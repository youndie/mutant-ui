package io.github.youndie.mutant.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody

/**
 * A filter chip (body `chip`, 32 dp tall in a 48 dp touch target). Unselected, its fill has slipped 2 dp off the
 * outline; selected, the acid fill pours in from the top, and the label turns to the ink as the pour's front passes
 * under its middle (B-94), not on a timer; unselected again, it turns back the same way. Under reduce motion the fill
 * and the label turn at once. Disabled, it loses its signal and its label greys.
 */
@Composable
public fun MutantChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    key: String = text,
    interactionSource: MutableInteractionSource? = null,
) {
    ChipBody(text, selected, onClick, modifier, enabled, key, interactionSource)
}

/** [MutantChip]; [heat] holds a fixture's boil, [hidden] draws the label transparent for a bare twin. */
@Composable
internal fun ChipBody(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    heat: Double? = null,
    hidden: Boolean = false,
    padding: androidx.compose.ui.unit.Dp = PADDING,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val instant = LocalMutantReduceMotion.current
    val label =
        when {
            hidden -> Color.Transparent

            !enabled -> colors.textDisabled

            // The label turns when the pour's front passes under it (B-94), not on a timer of its own: the engine's
            // `_muTc` turned it 200 ms late on the wall clock, while the pour runs on the screen's. Under reduce motion,
            // where nothing pours, it turns with the fill in the same frame.
            selected -> colors.onPrimary

            else -> colors.text
        }
    val other = if (selected) colors.text else colors.onPrimary
    Box(
        modifier
            .defaultMinSize(minWidth = TOUCH)
            .heightIn(min = TOUCH)
            // As wide as the label and its padding, or the width the caller gives; the body fills it.
            .width(IntrinsicSize.Max)
            .mutantHand(enabled)
            .selectable(
                selected,
                source,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MutantSurface(
            MutantBody.Chip,
            key,
            Modifier.fillMaxWidth().heightIn(min = HEIGHT),
            fill = if (selected) colors.primary else null,
            slip = if (selected) null else UNSELECTED_SLIP,
            state = if (enabled) BodyState.Rest else BodyState.Disabled,
            focused = focused && keyboard,
            interactionSource = source,
            heat = heat,
        ) {
            // The label starts 16 dp in, wherever the chip is wider than it (the page's flex row, no justify).
            Box(Modifier.padding(horizontal = padding), contentAlignment = Alignment.CenterStart) {
                ButtonLabel(text, label, Modifier, size = LABEL_SIZE, other = other)
            }
        }
    }
}

/** The touch target. */
private val TOUCH = 48.dp

/** The body's height and the label's distance from its sides (the page's `height: 32px; padding: 0 16px`). */
private val HEIGHT = 32.dp
private val PADDING = 16.dp

/** The unselected chip's slip, dp (the page's `data-slip="2"`). */
private const val UNSELECTED_SLIP = 2.0

/** The label's size, sp (the page's `font-size: 13px`). */
private const val LABEL_SIZE = 13
