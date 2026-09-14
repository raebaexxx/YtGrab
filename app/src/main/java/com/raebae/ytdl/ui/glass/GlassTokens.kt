package com.raebae.ytdl.ui.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material palette of the app's liquid glass design system.
 *
 * The app follows the hybrid material model of iOS 26: heavy lens glass only
 * on floating chrome (bars, dialogs, buttons), a quieter frosted glass for
 * content surfaces (cards, rows) and plain theme colors for text.
 *
 * Accent roles (accent tint/surface, selection) are derived from
 * [MaterialTheme.colorScheme.primary] by [GlassTheme], so glass follows the
 * app theme mode and Material You dynamic color instead of hardcoding a hue.
 */
data class GlassColors(
    /** Surface overlay for heavy floating chrome (bars, dialogs). */
    val chromeSurface: Color,
    /** Surface overlay for quiet content surfaces (cards, rows). */
    val contentSurface: Color,
    /** Surface overlay for a selected content surface (chips, cards). */
    val contentSurfaceSelected: Color,
    /** Hue-shift tint of accent glass (drawn with BlendMode.Hue). */
    val accentTint: Color,
    /** Readability overlay of accent glass, on top of the tint. */
    val accentSurface: Color,
    /** Track color of control glass (switches, progress). */
    val controlTrack: Color
)

/** Geometry tokens that keep all glass elements in one shape family. */
data class GlassDimensions(
    val barHeight: Dp,
    val barDropletHeight: Dp,
    val topBarHeight: Dp,
    val buttonHeight: Dp,
    val cardCornerRadius: Dp,
    val dialogCornerRadius: Dp
)

val GlassDimensionsDefault = GlassDimensions(
    barHeight = 64.dp,
    barDropletHeight = 56.dp,
    topBarHeight = 56.dp,
    buttonHeight = 52.dp,
    cardCornerRadius = 20.dp,
    dialogCornerRadius = 28.dp
)

/**
 * Never crashes: a component composed outside [GlassTheme] (a preview, a
 * dialog window) only downgrades to static light glass instead of taking
 * the app down.
 */
val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        chromeSurface = Color(0xFFFAFAFA).copy(alpha = 0.40f),
        contentSurface = Color(0xFFFAFAFA).copy(alpha = 0.45f),
        contentSurfaceSelected = Color(0xFF3560E0).copy(alpha = 0.12f),
        accentTint = Color(0xFF3560E0),
        accentSurface = Color(0xFF3560E0).copy(alpha = 0.30f),
        controlTrack = Color(0xFF787880).copy(alpha = 0.18f)
    )
}

@Composable
fun glassColors(): GlassColors = LocalGlassColors.current

/**
 * Provides the glass palette for the subtree. Neutral surfaces are static
 * per light/dark; accent roles are taken from the current Material color
 * scheme so dynamic color reaches the glass too. Dark mode is detected from
 * the scheme background, which already follows the app's theme setting.
 */
@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val accent = scheme.primary
    val colors = if (dark) {
        GlassColors(
            chromeSurface = Color(0xFF16171B).copy(alpha = 0.40f),
            contentSurface = Color(0xFF1C1D22).copy(alpha = 0.45f),
            contentSurfaceSelected = accent.copy(alpha = 0.16f),
            accentTint = accent,
            accentSurface = accent.copy(alpha = 0.30f),
            controlTrack = Color(0xFF8A8A93).copy(alpha = 0.28f)
        )
    } else {
        GlassColors(
            chromeSurface = Color(0xFFFAFAFA).copy(alpha = 0.40f),
            contentSurface = Color(0xFFFAFAFA).copy(alpha = 0.45f),
            contentSurfaceSelected = accent.copy(alpha = 0.10f),
            accentTint = accent,
            accentSurface = accent.copy(alpha = 0.28f),
            controlTrack = Color(0xFF787880).copy(alpha = 0.16f)
        )
    }
    CompositionLocalProvider(LocalGlassColors provides colors, content = content)
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
