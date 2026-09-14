package com.raebae.ytdl.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Liquid motion of the glass design system.
 *
 * Interactive glass swells when pressed, follows the finger with a spring
 * and settles with a squash & stretch driven by drag velocity. This state
 * implements the feel once; every draggable glass element (tab droplet,
 * toggle thumb) moves the same way.
 */
class GlassDragState internal constructor(
    private val scope: CoroutineScope,
    initialValue: Float,
    private val valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    private val initialScale: Float,
    private val pressedScale: Float,
    private val onDrag: GlassDragState.(dragAmountPx: Float) -> Unit,
    private val onDragStopped: GlassDragState.() -> Unit
) {
    private val valueAnim = Animatable(initialValue.coerceIn(valueRange), visibilityThreshold)
    private val velocityAnim = Animatable(0f, 5f)
    private val pressAnim = Animatable(0f, 0.001f)
    private val scaleXAnim = Animatable(initialScale, 0.001f)
    private val scaleYAnim = Animatable(initialScale, 0.001f)
    private val velocityTracker = VelocityTracker()

    private val valueSpec = spring<Float>(1f, 1000f, visibilityThreshold)
    private val velocitySpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressSpec = spring(1f, 1000f, 0.001f)
    private val scaleSpecX = spring(0.6f, 250f, 0.001f)
    private val scaleSpecY = spring(0.7f, 250f, 0.001f)

    val value: Float get() = valueAnim.value
    val targetValue: Float get() = valueAnim.targetValue
    val pressProgress: Float get() = pressAnim.value
    val scaleX: Float get() = scaleXAnim.value
    val scaleY: Float get() = scaleYAnim.value
    val velocity: Float get() = velocityAnim.value

    fun press() {
        velocityTracker.resetTracking()
        scope.launch {
            launch { pressAnim.animateTo(1f, pressSpec) }
            launch { scaleXAnim.animateTo(pressedScale, scaleSpecX) }
            launch { scaleYAnim.animateTo(pressedScale, scaleSpecY) }
        }
    }

    fun release() {
        scope.launch {
            // hold the press until the value spring is nearly settled so the
            // squash lands together with the move, then let go
            if (valueAnim.value != valueAnim.targetValue) {
                val settle = (valueRange.endInclusive - valueRange.start) * 0.02f
                snapshotFlow { valueAnim.value }
                    .filter { abs(it - valueAnim.targetValue) < settle }
                    .first()
            }
            launch { pressAnim.animateTo(0f, pressSpec) }
            launch { scaleXAnim.animateTo(initialScale, scaleSpecX) }
            launch { scaleYAnim.animateTo(initialScale, scaleSpecY) }
        }
    }

    /** Move the target with the finger by [deltaPx], [pxPerUnit] scales it. */
    fun dragBy(deltaPx: Float, pxPerUnit: Float) {
        if (pxPerUnit <= 0f) return
        updateValue(targetValue + deltaPx / pxPerUnit)
        trackVelocity()
    }

    /** Move the target straight to [target] (direct manipulation). */
    fun updateValue(target: Float) {
        val coerced = target.coerceIn(valueRange.start, valueRange.endInclusive)
        scope.launch {
            launch { valueAnim.animateTo(coerced, valueSpec) { trackVelocity() } }
        }
    }

    /**
     * Spring to [target] with the liquid pick-up: briefly swell, fly and
     * settle — used for state changes that did not come from a drag.
     */
    fun animateTo(target: Float) {
        val coerced = target.coerceIn(valueRange.start, valueRange.endInclusive)
        scope.launch {
            press()
            launch { valueAnim.animateTo(coerced, valueSpec) }
            if (velocity != 0f) launch { velocityAnim.animateTo(0f, velocitySpec) }
            release()
        }
    }

    private fun trackVelocity() {
        velocityTracker.addPosition(System.currentTimeMillis(), Offset(value, 0f))
        val rangeWidth = valueRange.endInclusive - valueRange.start
        if (rangeWidth <= 0f) return
        val normalized = velocityTracker.calculateVelocity().x / rangeWidth
        scope.launch { velocityAnim.animateTo(normalized, velocitySpec) }
    }

    /**
     * Pointer handler for a glass element: observes drags without consuming
     * them (parents still scroll), drives press/velocity/scale animations.
     */
    fun pointerHandler(): suspend PointerInputScope.() -> Unit = {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val down = awaitFirstDown(requireUnconsumed = false)
            press()
            onDrag(0f)
            var released = false
            while (!released) {
                val event = awaitPointerEvent()
                val active = event.changes.firstOrNull { it.id == down.id }
                    ?: event.changes.firstOrNull { it.pressed }
                    ?: break
                if (!active.pressed) {
                    released = true
                } else {
                    val delta = active.positionChange()
                    if (delta != Offset.Zero) onDrag(delta.x)
                }
            }
            onDragStopped()
            release()
        }
    }
}

@Composable
fun rememberGlassDragState(
    initialValue: Float = 0f,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    visibilityThreshold: Float = 0.001f,
    initialScale: Float = 1f,
    pressedScale: Float = 1.2f,
    onDrag: GlassDragState.(dragAmountPx: Float) -> Unit = {},
    onDragStopped: GlassDragState.() -> Unit = {}
): GlassDragState {
    val scope = rememberCoroutineScope()
    return remember(scope, initialValue, valueRange, initialScale, pressedScale) {
        GlassDragState(
            scope = scope,
            initialValue = initialValue,
            valueRange = valueRange,
            visibilityThreshold = visibilityThreshold,
            initialScale = initialScale,
            pressedScale = pressedScale,
            onDrag = onDrag,
            onDragStopped = onDragStopped
        )
    }
}

/**
 * Squash & stretch of a moving glass element: fast motion stretches along
 * the motion axis and compresses across it. Returns (scaleX, scaleY).
 */
fun motionSquash(velocity: Float, normalize: Float = 10f): Pair<Float, Float> {
    val v = (velocity / normalize).coerceIn(-0.2f, 0.2f)
    return 1f / (1f - v * 0.75f) to 1f - v * 0.25f
}

/**
 * Press feedback of a static glass control (buttons, cards, chips): runs a
 * 0..1 spring while pressed and invokes [onClick] on a confirmed tap. The
 * gesture is observed, not consumed, so parents keep scrolling.
 */
@Composable
fun rememberGlassPress(): Pair<Animatable<Float, AnimationVector1D>, CoroutineScope> {
    val press = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    return press to scope
}
