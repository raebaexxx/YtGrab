package com.raebae.ytdl.ui.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.shapes.Capsule/**
 * Content-surface glass components: cards, selectable cards, chips and
 * text fields. They use the CONTENT recipe (vibrancy + light blur, no
 * lens) — the content behind and inside them must stay readable, so they
 * read as a quiet glass layer in contrast to the floating chrome.
 */

/** Glass card — the base container of content sections. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    Box(
        modifier.drawGlass(
            backdrop = backdrop,
            recipe = Recipe.CONTENT,
            shape = RoundedCornerShape(GlassDimensionsDefault.cardCornerRadius),
            colors = colors
        )
    ) {
        content()
    }
}

/**
 * Selectable glass card for format/quality options and playlist entries.
 * A selected card carries the accent tint; press feedback squashes it
 * slightly toward the finger.
 */
@Composable
fun GlassSelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val (press, scope) = rememberGlassPress()

    Box(
        modifier
            .glassPress(scope, true, press, onClick)
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CONTENT,
                shape = RoundedCornerShape(GlassDimensionsDefault.cardCornerRadius),
                colors = colors,
                pressed = { press.value },
                accentOverlay = selected
            ) {
                val scale = lerp(1f, 0.98f, press.value)
                scaleX = scale
                scaleY = scale
            }
    ) {
        content()
    }
}

/**
 * Glass chip — a small selection pill (theme mode, codec, retry). Uses the
 * accent surface when selected, quiet content glass otherwise.
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val (press, scope) = rememberGlassPress()
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier
            .glassPress(scope, true, press, onClick)
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CONTENT,
                shape = Capsule(),
                colors = colors,
                pressed = { press.value },
                accentOverlay = selected
            ) {
                val scale = lerp(1f, 0.95f, press.value)
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(6.dp))
            }
            ProvideTextStyle(
                MaterialTheme.typography.labelLarge.copy(color = labelColor)
            ) {
                label()
            }
        }
    }
}

/**
 * Glass text field — a capsule with a [hint] and an optional [trailing]
 * icon inside. The caret carries the focus state; no focus ring on glass.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current

    Row(
        modifier
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CONTENT,
                shape = Capsule(),
                colors = colors
            )
            .defaultMinSize(minHeight = GlassDimensionsDefault.buttonHeight)
            .padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerField ->
                    Box(Modifier.fillMaxWidth()) {
                        if (value.isEmpty() && hint != null) {
                            Text(
                                hint,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerField()
                    }
                }
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}
