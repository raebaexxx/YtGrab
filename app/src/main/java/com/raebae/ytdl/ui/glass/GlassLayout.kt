package com.raebae.ytdl.ui.glass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Bottom padding for scrollable content that must slide under the floating
 * glass bottom bar (bar height 64dp + 12dp vertical margin + breathing room).
 * Lists use it as contentPadding so items actually travel under the glass
 * and get refracted by it.
 */
val GlassBarBottomPadding = 112.dp

/**
 * Strips the bottom part of a Scaffold contentPadding (the glass bar is not
 * part of the Scaffold, so bottom insets must not push the content up).
 */
fun scaffoldPaddingWithoutBottom(
    padding: PaddingValues,
    layoutDirection: LayoutDirection
): PaddingValues = PaddingValues(
    top = padding.calculateTopPadding(),
    start = padding.calculateStartPadding(layoutDirection),
    end = padding.calculateEndPadding(layoutDirection)
)
