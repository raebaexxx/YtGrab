package com.raebae.ytdl.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Native liquid glass components of the design system.
 *
 * Components resolve their own backdrop from
 * [LocalAppBackdrop][com.raebae.ytdl.ui.LocalAppBackdrop], palette from
 * [glassColors] and dimensions from [GlassDimensionsDefault], so call sites
 * never branch on backdrop availability or theme. Without a backdrop
 * (previews, dialog windows) the glass degrades to a translucent surface.
 *
 * Recipes follow the iOS 26 hybrid material model:
 * - [Recipe.CHROME] floating chrome (bars, dialogs, icon buttons): vibrancy + blur + lens
 * - [Recipe.CONTENT] content surfaces (cards, chips, fields): vibrancy + light blur, no lens
 * - [Recipe.ACCENT] primary actions (buttons): light blur + lens + accent tint
 * - [Recipe.CONTROL] small controls (toggle, progress): pressed state swaps blur for lens
 */

enum class Recipe { CHROME, CONTENT, ACCENT, CONTROL }

/**
 * Core glass renderer: draws the app backdrop through [shape] with the
 * effect stack of [recipe] plus its readability surface. Degrades to a
 * plain translucent surface when [backdrop] is null. [layerBlock]
 * transforms the drawn layer only — the backdrop stays unscaled, which is
 * what makes the element read as real glass when it moves or stretches.
 */
internal fun Modifier.drawGlass(
    backdrop: Backdrop?,
    recipe: Recipe,
    shape: Shape,
    colors: GlassColors,
    pressed: () -> Float = { 0f },
    accentOverlay: Boolean = false,
    layerBlock: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {}
): Modifier {
    fun DrawScope.surfaceOverlay() {
        val p = pressed()
        when (recipe) {
            Recipe.CHROME -> drawRect(colors.chromeSurface)
            Recipe.CONTENT -> {
                if (accentOverlay) {
                    drawRect(colors.accentTint, blendMode = BlendMode.Hue)
                    drawRect(colors.contentSurfaceSelected)
                } else {
                    drawRect(colors.contentSurface)
                }
            }
            Recipe.ACCENT -> {
                drawRect(colors.accentTint, blendMode = BlendMode.Hue)
                drawRect(colors.accentSurface)
            }
            Recipe.CONTROL -> {
                drawRect(colors.controlTrack)
                if (p > 0f) drawRect(Color.White.copy(alpha = p))
            }
        }
        // Sheen + rim: without something to refract (flat app background),
        // these two are what make a surface read as glass, not a fill.
        if (recipe != Recipe.CONTROL) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to colors.glassSheen,
                    0.45f to Color.Transparent,
                    endY = size.height
                )
            )
            val rimPath = Path().apply {
                addOutline(shape.createOutline(size, layoutDirection, this@surfaceOverlay))
            }
            drawPath(
                path = rimPath,
                color = colors.glassEdge,
                style = Stroke(width = 1.5f.dp.toPx())
            )
        }
    }

    return if (backdrop == null) {
        drawBehind { surfaceOverlay() }
    } else {
        drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                when (recipe) {
                    Recipe.CHROME -> {
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    }
                    Recipe.CONTENT -> {
                        blur(6.dp.toPx())
                        // A gentle lens so content glass refracts and catches
                        // a rim of light even over a flat background — this is
                        // what stops cards from reading as flat tinted rects.
                        lens(4.dp.toPx(), 12.dp.toPx())
                    }
                    Recipe.ACCENT -> {
                        blur(2.dp.toPx())
                        lens(12.dp.toPx(), 24.dp.toPx())
                    }
                    Recipe.CONTROL -> {
                        val p = pressed()
                        blur(8.dp.toPx() * (1f - p))
                        lens(10.dp.toPx() * p, 14.dp.toPx() * p, chromaticAberration = true)
                    }
                }
            },
            highlight = {
                when (recipe) {
                    Recipe.CONTENT -> Highlight.Ambient.copy(alpha = 0.75f)
                    else -> Highlight.Ambient.copy(alpha = 0.5f)
                }
            },
            shadow = {
                when (recipe) {
                    // Elevation is what separates same-tint glass from the
                    // background (white pill on white page in light theme).
                    Recipe.CHROME -> Shadow(radius = 20.dp, offset = DpOffset.Zero, color = Color.Black, alpha = 0.18f)
                    Recipe.CONTENT -> Shadow(radius = 10.dp, offset = DpOffset.Zero, color = Color.Black, alpha = 0.10f)
                    Recipe.ACCENT -> Shadow(radius = 14.dp, offset = DpOffset.Zero, color = Color.Black, alpha = 0.20f)
                    Recipe.CONTROL -> null
                }
            },
            innerShadow = {
                val p = pressed()
                when {
                    p > 0f -> InnerShadow(radius = 8.dp * p, alpha = p)
                    recipe == Recipe.CONTENT -> InnerShadow(radius = 3.dp, alpha = 0.4f)
                    else -> null
                }
            },
            layerBlock = layerBlock,
            onDrawSurface = { surfaceOverlay() }
        )
    }
}

/**
 * Press gesture for static glass controls: runs a 0..1 spring [press]
 * while the element is touched and invokes [onClick] on a confirmed tap.
 * Observes gestures without consuming them so parents still scroll.
 */
internal fun Modifier.glassPress(
    scope: CoroutineScope,
    enabled: Boolean,
    press: Animatable<Float, AnimationVector1D>,
    onClick: (() -> Unit)?
): Modifier {
    val spec = spring<Float>(0.5f, 300f, 0.001f)
    return this
        .semantics { if (onClick != null) role = Role.Button }
        .pointerInput(scope, enabled, onClick) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!enabled) return@awaitEachGesture
                scope.launch { press.animateTo(1f, spec) }
                val up = waitForUpOrCancellation()
                scope.launch { press.animateTo(0f, spec) }
                if (up != null && onClick != null) onClick()
            }
        }
}

/**
 * Accent pill button — the primary action of a screen. Light blur + lens
 * over an accent-tinted surface; swells 4dp while pressed.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val (press, scope) = rememberGlassPress()

    Row(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .glassPress(scope, enabled, press, if (enabled) onClick else null)
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.ACCENT,
                shape = Capsule(),
                colors = colors,
                pressed = { press.value }
            ) {
                val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, press.value)
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = GlassDimensionsDefault.buttonHeight)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Circular 44dp glass icon button from the chrome recipe. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val (press, scope) = rememberGlassPress()

    Box(
        modifier
            .glassPress(scope, true, press, onClick)
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CHROME,
                shape = CircleShape,
                colors = colors,
                pressed = { press.value }
            ) {
                val scale = lerp(1f, 0.92f, press.value)
                scaleX = scale
                scaleY = scale
            }
            .size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}
