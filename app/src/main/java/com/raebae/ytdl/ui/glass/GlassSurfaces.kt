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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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

/** Glass card — the base container of content sections. Content is clipped
 * to the card shape so images never poke out of the rounded corners. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = glassColors()
    val backdrop = com.raebae.ytdl.ui.LocalAppBackdrop.current
    val shape = RoundedCornerShape(GlassDimensionsDefault.cardCornerRadius)
    Box(
        modifier
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CONTENT,
                shape = shape,
                colors = colors
            )
            .clip(shape)
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
    val shape = RoundedCornerShape(GlassDimensionsDefault.cardCornerRadius)

    Box(
        modifier
            .glassPress(scope, true, press, onClick)
            .drawGlass(
                backdrop = backdrop,
                recipe = Recipe.CONTENT,
                shape = shape,
                colors = colors,
                pressed = { press.value },
                accentOverlay = selected
            ) {
                val scale = lerp(1f, 0.98f, press.value)
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
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
    // A selected chip is a filled droplet of accent glass with onPrimary
    // text (iOS tinted-selection); an unselected one is quiet content glass.
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier
            .glassPress(scope, true, press, onClick)
            .drawGlass(
                backdrop = backdrop,
                recipe = if (selected) Recipe.ACCENT else Recipe.CONTENT,
                shape = Capsule(),
                colors = colors,
                pressed = { press.value }
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
 * [onSubmit] wires the keyboard's Done action to the primary screen action.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onSubmit: (() -> Unit)? = null,
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
            .padding(start = 20.dp, end = if (trailing != null) 8.dp else 20.dp),
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
                keyboardOptions = if (onSubmit != null) {
                    KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    )
                } else {
                    KeyboardOptions.Default
                },
                keyboardActions = if (onSubmit != null) {
                    KeyboardActions(
                        onDone = { onSubmit() }
                    )
                } else {
                    KeyboardActions.Default
                },
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
