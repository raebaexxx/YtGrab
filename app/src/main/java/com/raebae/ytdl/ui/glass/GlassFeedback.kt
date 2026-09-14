package com.raebae.ytdl.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
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
 * The fill is an accent capsule sliding inside the clipped track.
 */
@Composable
fun GlassProgressBar(
    progress: () -> Float?,
    modifier: Modifier = Modifier
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val accent = MaterialTheme.colorScheme.primary
    val determinate = progress() != null

    val sweep = rememberInfiniteTransition(label = "glass-progress")
    val sweepPosition by sweep.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
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
            .padding(4.dp)
    ) {
        // accent fill: full width by fraction when determinate, a traveling
        // stub when indeterminate
        val fraction = progress()
        val fillFraction = fraction?.coerceIn(0f, 1f) ?: 0.35f
        Box(
            Modifier
                .fillMaxWidth(fillFraction)
                .height(12.dp)
                .graphicsLayer {
                    if (!determinate) {
                        // the stub slides across the track
                        translationX = sweepPosition * size.width
                    }
                }
                .clip(Capsule())
                .drawBehind { drawRect(accent.copy(alpha = 0.55f)) }
        )
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
        // accent dot orbiting the disc rim
        Box(
            Modifier
                .size(10.dp)
                .graphicsLayer {
                    rotationZ = angle
                    translationX = 13.dp.toPx()
                }
                .clip(CircleShape)
                .drawBehind { drawRect(accent.copy(alpha = 0.6f)) }
        )
    }
}
