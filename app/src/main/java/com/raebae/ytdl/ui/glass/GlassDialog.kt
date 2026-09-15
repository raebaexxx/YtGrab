package com.raebae.ytdl.ui.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

/**
 * Liquid glass dialog rendered as an INLINE overlay (not a separate window):
 * the panel stays in the screen's composition, so [drawGlass] samples the
 * same CONTENT backdrop as every other in-screen surface and the render
 * graph can never become cyclic. The scrim darkens what is behind, the
 * panel floats above it in chrome glass.
 *
 * Place as the last child of a full-size [Box] to cover the screen.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onDismissRequest)

    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var panelShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { panelShown = true }

    // Taps on the scrim (outside the panel) dismiss; taps inside the panel
    // are left to the panel's own controls.
    val dismissOnScrim: (androidx.compose.ui.geometry.Offset) -> Unit = { position ->
        if (!panelBounds.contains(position)) onDismissRequest()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .pointerInput(dismissOnScrim) {
                detectTapGestures(onTap = dismissOnScrim)
            }
    ) {
        AnimatedVisibility(
            visible = panelShown,
            enter = fadeIn() + scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.7f)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 24.dp)
                    .drawGlass(
                        backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current,
                        recipe = Recipe.CHROME,
                        shape = RoundedCornerShape(GlassDimensionsDefault.dialogCornerRadius),
                        colors = glassColors()
                    )
                    .clip(RoundedCornerShape(GlassDimensionsDefault.dialogCornerRadius))
                    .padding(24.dp)
                    .onGloballyPositioned { coords -> panelBounds = coords.boundsInRoot() }
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                content()
                if (dismissText != null && onDismissClick != null) {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.align(Alignment.CenterHorizontally)) {
                        GlassChip(
                            selected = false,
                            onClick = onDismissClick,
                            label = { Text(dismissText) }
                        )
                    }
                }
            }
        }
    }
}
