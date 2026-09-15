package com.raebae.ytdl.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest

/**
 * Liquid glass switch: a glass thumb rides a tinted track. Dragging moves
 * the thumb with the finger, releasing springs to the nearest side, a tap
 * flips the value. Idle the thumb is frosted glass; while pressed it turns
 * into a lens with chromatic aberration and refracts the track's accent
 * fill traveling underneath.
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = glassColors()
    val appBackdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    // Geometry: the thumb must stay inside the track at both extremes —
    // travel is track width minus thumb width minus both paddings.
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 26.dp
    val thumbPadding = 2.dp
    val thumbTravel = with(density) {
        (trackWidth - thumbSize - thumbPadding * 2).toPx()
    }

    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    var moved by remember { mutableStateOf(false) }

    val drag = rememberGlassDragState(
        initialValue = fraction,
        valueRange = 0f..1f,
        initialScale = 1f,
        pressedScale = 1.5f,
        onDrag = { deltaPx ->
            if (deltaPx != 0f) moved = true
            val delta = if (isLtr) deltaPx else -deltaPx
            fraction = (fraction + delta / thumbTravel).fastCoerceIn(0f, 1f)
        },
        onDragStopped = {
            val target = if (targetValue >= 0.5f) 1f else 0f
            fraction = target
            if (moved) {
                moved = false
                onCheckedChange(target == 1f)
            } else {
                // a press without movement is a tap: flip
                val flipped = if (target == 1f) 0f else 1f
                fraction = flipped
                animateTo(flipped)
                onCheckedChange(flipped == 1f)
            }
        }
    )
    // external state changes animate the thumb
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (fraction != target) {
            fraction = target
            drag.animateTo(target)
        }
    }
    // fraction changes from the drag update the spring directly
    LaunchedEffect(drag) {
        snapshotFlow { fraction }
            .collectLatest { drag.updateValue(it) }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier.size(trackWidth, trackHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track: capsule whose color lerps gray → accent, recorded into
        // trackBackdrop so the pressed thumb can refract the accent fill
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind { drawRect(lerp(colors.controlTrack, accent, drag.value)) }
                .size(trackWidth, trackHeight)
        )

        // Thumb: CONTROL-recipe glass over the app backdrop combined with a
        // scaled copy of the track; shifted along the track by drag.value
        Box(
            Modifier
                .graphicsLayer {
                    val padding = thumbPadding.toPx()
                    translationX = if (isLtr) {
                        lerp(padding, padding + thumbTravel, drag.value)
                    } else {
                        -lerp(padding, padding + thumbTravel, drag.value)
                    }
                }
                .semantics {
                    role = Role.Switch
                    this[SemanticsProperties.ToggleableState] = ToggleableState(checked)
                    onClick {
                        onCheckedChange(!checked)
                        true
                    }
                }
                .pointerInput(drag) { drag.pointerHandler().invoke(this) }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        appBackdrop ?: emptyBackdrop(),
                        rememberBackdrop(trackBackdrop) { drawTrack ->
                            val progress = drag.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) { drawTrack() }
                        }
                    ),
                    shape = { CircleShape },
                    effects = {
                        val p = drag.pressProgress
                        blur(8.dp.toPx() * (1f - p))
                        lens(5.dp.toPx() * p, 10.dp.toPx() * p, chromaticAberration = true)
                    },
                    highlight = {
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = 0.6f + drag.pressProgress * 0.4f
                        )
                    },
                    shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.15f)) },
                    innerShadow = {
                        val p = drag.pressProgress
                        InnerShadow(radius = 4.dp * p, alpha = p)
                    },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val v = (drag.velocity / 50f).coerceIn(-0.2f, 0.2f)
                        scaleX /= 1f - v * 0.75f
                        scaleY *= 1f - v * 0.25f
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 1f - drag.pressProgress * 0.12f))
                    }
                )
                .size(thumbSize)
        )
    }
}
