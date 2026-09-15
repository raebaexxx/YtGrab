package com.raebae.ytdl.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** A tab of the glass bottom bar. */
data class GlassTab(
    val label: String,
    val icon: ImageVector
)

/**
 * iOS 26-style floating bottom tab bar. A capsule chrome panel carries the
 * tabs; the active tab is covered by a draggable glass droplet with lens
 * refraction and chromatic aberration that follows the finger and springs
 * to the nearest tab on release. The panel drifts slightly against the
 * drag and swells while touched.
 */
@Composable
fun GlassBottomBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    tabs: List<GlassTab>,
    modifier: Modifier = Modifier
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current ?: return
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val accent = MaterialTheme.colorScheme.primary
    val isDarkTheme = MaterialTheme.colorScheme.background.luminanceCompat() < 0.5f
    val tabsCount = tabs.size
    if (tabsCount < 2) return

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount
        }
        val scope = rememberCoroutineScope()

        var currentIndex by remember(selectedTabIndex) { mutableIntStateOf(selectedTabIndex()) }
        // sync external selection (navigation) into local state
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index -> if (index != currentIndex) currentIndex = index }
        }

        // panel sway: the bar drifts a little against the droplet drag.
        // The damped offset is shared by the panel and the droplet so they
        // never desynchronize on fast drags.
        val panelSway = remember { Animatable(0f) }
        val swayFraction =
            (panelSway.value / constraints.maxWidth.toFloat()).fastCoerceIn(-1f, 1f)
        val swayPx = with(density) { 4.dp.toPx() } *
            swayFraction.sign * EaseOut.transform(abs(swayFraction))

        val dragState = rememberGlassDragState(
            initialValue = selectedTabIndex().toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            initialScale = 1f,
            pressedScale = 78f / 56f,
            onDrag = { deltaPx ->
                val direction = if (isLtr) 1f else -1f
                updateValue(targetValue + direction * deltaPx / tabWidth)
                scope.launch { panelSway.snapTo(panelSway.value + deltaPx) }
            },
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                scope.launch { panelSway.animateTo(0f, spring(1f, 300f, 0.5f)) }
                if (target != currentIndex) {
                    currentIndex = target
                } else {
                    // released on the same tab: spring the droplet back
                    animateTo(target.toFloat())
                }
            }
        )
        // single source of truth for selection: move the droplet and commit
        // navigation — both for drag releases and for tab clicks
        LaunchedEffect(dragState) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    if (dragState.value != index.toFloat()) dragState.animateTo(index.toFloat())
                    onTabSelected(index)
                }
        }

        // Base panel: chrome glass, icons/labels drawn above the glass
        Row(
            Modifier
                .graphicsLayer { translationX = swayPx }
                .drawGlass(
                    backdrop = backdrop,
                    recipe = Recipe.CHROME,
                    shape = Capsule(),
                    colors = colors
                ) {
                    val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, dragState.pressProgress)
                    scaleX = scale
                    scaleY = scale
                }
                .height(GlassDimensionsDefault.barHeight)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                tabs.forEachIndexed { index, tab ->
                    TabContent(
                        tab = tab,
                        selected = index == currentIndex,
                        // the selected tab's glyph is repainted above the
                        // droplet (which would otherwise dim it through its
                        // surface); the slot stays reserved for layout
                        showContent = index != currentIndex,
                        onSelect = { if (index != currentIndex) currentIndex = index }
                    )
                }
            }
        )

        // Hidden accent-tinted copy of the tab row, exported into
        // tabsBackdrop so the droplet refracts accent-colored icons
        val tabsBackdrop = rememberLayerBackdrop()
        Row(
            Modifier
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer { translationX = swayPx }
                .drawGlass(
                    backdrop = backdrop,
                    recipe = Recipe.CHROME,
                    shape = Capsule(),
                    colors = colors
                )
                .height(GlassDimensionsDefault.barDropletHeight)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .graphicsLayer(colorFilter = ColorFilter.tint(accent)),
            verticalAlignment = Alignment.CenterVertically,
            content = { tabs.forEach { tab -> TabContent(tab, selected = true, interactive = false) } }
        )

        // The droplet: a draggable lens over the active tab
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = if (isLtr) {
                        dragState.value * tabWidth + swayPx
                    } else {
                        size.width - (dragState.value + 1f) * tabWidth + swayPx
                    }
                }
                .pointerInput(dragState) { dragState.pointerHandler().invoke(this) }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val p = dragState.pressProgress
                        lens(10.dp.toPx() * p, 14.dp.toPx() * p, chromaticAberration = true)
                    },
                    highlight = { Highlight.Default.copy(alpha = dragState.pressProgress) },
                    shadow = { Shadow(alpha = dragState.pressProgress) },
                    innerShadow = {
                        val p = dragState.pressProgress
                        InnerShadow(radius = 8.dp * p, alpha = p)
                    },
                    layerBlock = {
                        scaleX = dragState.scaleX
                        scaleY = dragState.scaleY
                        val v = (dragState.velocity / 10f).fastCoerceIn(-0.2f, 0.2f)
                        scaleX /= 1f - v * 0.75f
                        scaleY *= 1f - v * 0.25f
                    },
                    onDrawSurface = {
                        drawRect(
                            if (dragState.pressProgress < 0.5f) colors.chromeSurface
                            else colors.chromeSurface.copy(alpha = colors.chromeSurface.alpha * 0.9f)
                        )
                        drawRect(
                            if (isDarkTheme) {
                                Color.White.copy(alpha = 0.08f * dragState.pressProgress)
                            } else {
                                Color.Black.copy(alpha = 0.03f * dragState.pressProgress)
                            }
                        )
                    }
                )
                .height(GlassDimensionsDefault.barDropletHeight)
                .fillMaxWidth(1f / tabsCount)
        )

        // Selected-tab glyph repainted ABOVE the droplet: the droplet's
        // surface would otherwise darken it below legibility. Mirrors the
        // base panel's geometry exactly; purely decorative, so it neither
        // repeats semantics nor consumes touches.
        Row(
            Modifier
                .clearAndSetSemantics {}
                .graphicsLayer { translationX = swayPx }
                .height(GlassDimensionsDefault.barHeight)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                TabContent(
                    tab = tab,
                    selected = true,
                    interactive = false,
                    showContent = index == currentIndex
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminanceCompat(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

/**
 * Floating top title bar in the same capsule family as the bottom bar.
 * Content scrolls under it and gets refracted. Callers place it inside a
 * [statusBarsPadding][androidx.compose.foundation.layout.statusBarsPadding]
 * scope (AppRoot does) so it sits below the system status bar.
 */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current

    Row(
        modifier
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CHROME,
                shape = Capsule(),
                colors = colors
            )
            .height(GlassDimensionsDefault.topBarHeight)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        if (navigationIcon != null) {
            navigationIcon()
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (navigationIcon != null) 0.dp else 12.dp)
        )
        if (actions != null) {
            actions()
        }
    }
}

@Composable
private fun RowScope.TabContent(
    tab: GlassTab,
    selected: Boolean,
    onSelect: () -> Unit = {},
    interactive: Boolean = true,
    showContent: Boolean = true
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val clickableModifier = if (interactive) {
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            role = Role.Tab,
            onClick = onSelect
        )
    } else {
        Modifier
    }
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        if (showContent) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = tab.icon, contentDescription = tab.label, tint = contentColor)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
        }
    }
}
