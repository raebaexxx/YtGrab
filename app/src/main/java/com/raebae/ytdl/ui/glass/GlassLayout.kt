package com.raebae.ytdl.ui.glass

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout contract between the floating glass bars and scrollable content.
 *
 * The screens dropped [androidx.compose.material3.Scaffold], so nothing
 * consumes the system insets for them. These helpers fold the real window
 * insets (status bar, navigation bar) into the bar footprints, so list
 * content starts just below the floating top bar and its last item clears
 * the floating bottom bar on every device — not just on one with a stock
 * status bar height.
 */

private val barMargin = 12.dp

/** Extra breathing room so content isn't flush against a bar. */
private val contentGap = 16.dp

/** Height of the floating top bar capsule (mirrors [GlassTopBar]). */
val GlassTopBarHeight: Dp = GlassDimensionsDefault.topBarHeight

/**
 * Status-bar inset plus the top bar's vertical margins (8dp top + 8dp
 * bottom around the capsule) — everything above the first content item.
 */
@Composable
fun GlassTopContentInset(): Dp {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return status + GlassTopBarHeight + barMargin
}

/**
 * Navigation-bar inset plus the bottom bar footprint (bar + margins), used
 * as the bottom `contentPadding` of scrolling content so the last item can
 * be scrolled fully clear of the floating bar.
 */
@Composable
fun GlassBottomContentInset(): Dp {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return nav + GlassDimensionsDefault.barHeight + barMargin + contentGap
}

/**
 * Bottom inset without the extra breathing [contentGap] — for elements that
 * already sit inside a padded column (e.g. the Playlist download button).
 */
@Composable
fun GlassBottomBarInset(): Dp {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return nav + GlassDimensionsDefault.barHeight + barMargin
}

