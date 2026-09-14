package com.raebae.ytdl.ui.glass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Layout contract between the floating glass bars and scrollable content.
 * Lists use these as contentPadding so items actually travel under the
 * glass and get refracted by it, without the bars ever hiding content.
 */

private val barMargin = 12.dp

/**
 * Bottom padding for content sliding under the floating glass bottom bar
 * (bar height + two bar margins + breathing room).
 */
val GlassBarBottomPadding: Dp =
    GlassDimensionsDefault.barHeight + barMargin * 2 + 16.dp

/**
 * Top padding for content sliding under the floating glass top bar.
 * The status bar inset is handled by AppRoot's statusBarsPadding on the
 * bars themselves; list content already starts below it via window insets,
 * so this only covers the top bar + margins.
 */
val GlassBarTopPadding: Dp =
    GlassDimensionsDefault.topBarHeight + barMargin + 8.dp
