package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/**
 * A text field: a line of the on-screen menu (body `field`, 52 dp tall). [prefix] is the OSD label before the
 * value (Handjet, the OSD colour), [placeholder] stands in while the value is empty. Focused, the damage drops to 30 %,
 * the corner brackets lock on and a block caret blinks after the text; [isError] corrupts the body (the red offset
 * outline and blocks); disabled, it loses its signal and both texts grey. The text and the caret are never moved by
 * the body. One line.
 */
@Composable
public fun MutantTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    key: String = prefix ?: placeholder ?: "field",
    interactionSource: MutableInteractionSource? = null,
) {
    TextFieldBody(
        value,
        onValueChange,
        modifier,
        prefix,
        placeholder,
        enabled,
        isError,
        keyboardOptions,
        key,
        interactionSource,
    )
}

/** [MutantTextField]; [focusedAs] holds a fixture's focus, [hidden] draws the texts and caret transparent. */
@Composable
internal fun TextFieldBody(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    prefix: String?,
    placeholder: String?,
    enabled: Boolean,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    key: String,
    interactionSource: MutableInteractionSource?,
    focusedAs: Boolean? = null,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val focused = focusedAs ?: isFocused
    val fonts = LocalMutantFonts.current

    fun tint(c: Color) = if (hidden) Color.Transparent else c
    val textColor = tint(if (enabled) colors.text else colors.textDisabled)
    val valueStyle =
        TextStyle(
            fontFamily = fonts?.text,
            fontSize = VALUE_SIZE.sp,
            color = textColor,
            platformStyle = LocalMutantTextRaster.current,
        ).cssLine(MutantFaceMetrics.Rubik)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    MutantSurface(
        MutantBody.Field,
        key,
        modifier
            .defaultMinSize(minWidth = MIN_WIDTH)
            .height(HEIGHT)
            .semantics { if (isError) error("") },
        state =
            when {
                !enabled -> BodyState.Disabled
                isError -> BodyState.Error
                else -> BodyState.Rest
            },
        focused = focused,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = PADDING),
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefix != null) {
                BasicText(
                    prefix,
                    Modifier.mutantCleanZone(),
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = PREFIX_SIZE.sp,
                            color = tint(if (enabled) colors.textOsd else colors.textDisabled),
                            platformStyle = LocalMutantTextRaster.current,
                        ).cssLine(MutantFaceMetrics.Handjet),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    BasicText(
                        placeholder,
                        Modifier.mutantCleanZone(),
                        style =
                            valueStyle.copy(
                                color = tint(if (enabled) colors.textSecondary else colors.textDisabled),
                            ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value,
                    onValueChange,
                    // Only the value marks the clean zone: an empty field's box is the placeholder's.
                    (if (value.isEmpty()) Modifier else Modifier.mutantCleanZone()).fillMaxWidth(),
                    enabled = enabled,
                    textStyle = valueStyle,
                    keyboardOptions = keyboardOptions,
                    singleLine = true,
                    interactionSource = source,
                    // The design's caret is a block after the text, drawn below; the field's own is hidden.
                    cursorBrush = SolidColor(Color.Transparent),
                    onTextLayout = { layout = it },
                )
                if (focused && enabled) Caret(layout, value, tint(colors.text))
            }
        }
    }
}

/**
 * The block caret (the page's 9 × 20 `data-mu-anim="blink"`): after the text, 8 dp from its end, shown five scene
 * ticks in eight — 625 ms in each second — and standing under reduce motion. It never leaves the field: past the
 * field's end it stops at its edge.
 */
@Composable
private fun Caret(
    layout: TextLayoutResult?,
    value: String,
    color: Color,
) {
    val clock = readMutantClock()
    val still = LocalMutantReduceMotion.current
    // The end of the text the layout holds: on the frame a value changes, the layout is still the last one's, and
    // asking it about the new value's end throws.
    val end =
        layout?.let {
            val n = it.layoutInput.text.length
            if (n == 0) 0f else it.getHorizontalPosition(n, true)
        } ?: 0f
    val gap = if (value.isEmpty()) 0.dp else GAP
    Canvas(
        Modifier
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minWidth = 0))
                // At the text's end, but never past the field's.
                val x = (end + gap.toPx()).roundToInt().coerceAtMost(constraints.maxWidth - p.width).coerceAtLeast(0)
                layout(constraints.maxWidth, p.height) { p.place(x, 0) }
            }.size(CARET_W, CARET_H)
            .mutantCleanZone(),
    ) {
        val shown = still || clock.sceneTick % BLINK_PERIOD < BLINK_ON
        if (shown) {
            drawRect(
                color,
                size =
                    androidx.compose.ui.geometry
                        .Size(CARET_W.toPx(), CARET_H.toPx()),
            )
        }
    }
}

/** The field's height (the page's `height: 52px`) and the width it takes given none (research D15). */
private val HEIGHT = 52.dp
private val MIN_WIDTH = 200.dp

/** The texts from the sides and from each other (the page's `padding: 0 14px; gap: 8px`). */
private val PADDING = 14.dp
private val GAP = 8.dp

/** The OSD prefix (Handjet 20) and the value (Rubik 15), sp. */
private const val PREFIX_SIZE = 20
private const val VALUE_SIZE = 15

/** The block caret, and its blink: on for 5 of every 8 scene ticks (the engine's `fi % 8 < 5`). */
private val CARET_W = 9.dp
private val CARET_H = 20.dp
private const val BLINK_PERIOD = 8L
private const val BLINK_ON = 5L
