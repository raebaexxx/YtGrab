package com.raebae.ytdl.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule

/**
 * Feedback-layer glass: snackbars, progress bars and the spinner.
 * Feedback floats above content but below the chrome, so it uses light
 * recipes — readable, quiet, never competing with the bars.
 */

/**
 * Drop-in glass snackbar host: renders an existing [SnackbarHostState] in
 * glass style so screens keep their plain M3 snackbar logic.
 */
@Composable
fun GlassSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val colors = glassColors()
        val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
        Box(
            Modifier
                .padding(bottom = 8.dp)
                .drawGlass(
                    backdrop = backdrop,
                    recipe = Recipe.CHROME,
                    shape = Capsule(),
                    colors = colors
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Determinate or indeterminate progress in a glass capsule track.
 * [progress] returns null for the indeterminate sweep, 0..1 otherwise.
 * The fill is an accent capsule centered inside the clipped track; the
 * indeterminate stub travels the full track width.
 */
@Composable
fun GlassProgressBar(
    progress: () -> Float?,
    modifier: Modifier = Modifier
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val accent = MaterialTheme.colorScheme.primary
    val fraction = progress()

    val sweep = rememberInfiniteTransition(label = "glass-progress")
    val sweepPosition by sweep.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val animatedFraction by animateFloatAsState(
        targetValue = fraction?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = tween(350),
        label = "glass-progress-fill"
    )

    Box(
        modifier
            .height(20.dp)
            .fillMaxWidth()
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CONTENT,
                shape = Capsule(),
                colors = colors
            )
            .clip(Capsule())
    ) {
        Box(Modifier.matchParentSize().padding(3.dp)) {
            if (fraction != null) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight()
                        .clip(Capsule())
                        .drawBehind { drawRect(accent.copy(alpha = 0.75f)) }
                )
            } else {
                BoxWithConstraints(Modifier.matchParentSize()) {
                    val trackWidth = constraints.maxWidth.toFloat()
                    Box(
                        Modifier
                            .fillMaxWidth(0.35f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val stubWidth = trackWidth * 0.35f
                                translationX =
                                    (trackWidth + stubWidth) * sweepPosition - stubWidth
                            }
                            .clip(Capsule())
                            .drawBehind { drawRect(accent.copy(alpha = 0.75f)) }
                    )
                }
            }
        }
    }
}

/**
 * Glass spinner: a quiet content-glass disc with an accent dot orbiting
 * its rim — replaces the M3 CircularProgressIndicator in loading states.
 */
@Composable
fun GlassSpinner(
    modifier: Modifier = Modifier
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val accent = MaterialTheme.colorScheme.primary

    val rotation = rememberInfiniteTransition(label = "glass-spinner")
    val angle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "angle"
    )

    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(40.dp)
                .drawGlass(
                    backdrop = backdrop,
                    recipe = Recipe.CONTENT,
                    shape = CircleShape,
                    colors = colors
                )
        )
        // Rotating container carries the accent dot at its top edge, so the
        // dot orbits the rim around the disc center.
        Box(
            Modifier
                .size(40.dp)
                .graphicsLayer { rotationZ = angle }
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .drawBehind { drawRect(accent.copy(alpha = 0.85f)) }
            )
        }
    }
}
