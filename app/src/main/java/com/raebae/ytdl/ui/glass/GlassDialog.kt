package com.raebae.ytdl.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Liquid glass dialog: a floating CHROME panel instead of the M3
 * AlertDialog. The panel refracts the app behind it; content inside keeps
 * using content-recipe components (chips, rows). When no backdrop is
 * reachable (a dialog renders in its own window) the core renderer
 * degrades to a translucent surface automatically.
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
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colors = glassColors()
        val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier
                    .widthIn(max = 420.dp)
                    .drawGlass(
                        backdrop = backdrop,
                        recipe = Recipe.CHROME,
                        shape = RoundedCornerShape(GlassDimensionsDefault.dialogCornerRadius),
                        colors = colors
                    )
                    .padding(24.dp)
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
                    Box(
                        Modifier
                            .align(Alignment.End)
                            .padding(end = 4.dp)
                    ) {
                        GlassChip(
                            selected = false,
                            onClick = onDismissClick,
                            label = {
                                Text(
                                    dismissText,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
