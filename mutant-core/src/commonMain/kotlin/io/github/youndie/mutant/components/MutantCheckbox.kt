package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.mutantMelt
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/**
 * A checkbox (body `box`, 22 × 22) in a 48 dp row with its [label]: the row is the touch target. Checked, the acid
 * fill pours in and the mark — a tick and two burnt blocks — melts in; unchecked, it melts out. Disabled, the body
 * loses its signal and the label greys.
 */
@Composable
public fun MutantCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    key: String = label ?: "check",
    interactionSource: MutableInteractionSource? = null,
) {
    CheckboxBody(checked, onCheckedChange, modifier, label, enabled, key, interactionSource)
}

/**
 * A radio button (body `dot`, 22 × 22) in a 48 dp row with its [label]: selected, a red recording dot melts in with
 * a faint echo up and to the right; the red never flashes. Disabled, the dot greys.
 */
@Composable
public fun MutantRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    key: String = label ?: "radio",
    interactionSource: MutableInteractionSource? = null,
) {
    RadioBody(selected, onClick, modifier, label, enabled, key, interactionSource)
}

/** [MutantCheckbox]; [hidden] draws the mark and the label transparent for a fixture's bare twin. */
@Composable
internal fun CheckboxBody(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    label: String?,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    // A bare twin hides the tick (drawn in the text's `currentColor` on the page) and keeps the blocks (a background).
    val tick = if (hidden) Color.Transparent else colors.onPrimary
    val ink = colors.onPrimary
    ControlRow(
        modifier.toggleable(
            checked,
            source,
            indication = null,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        label,
        enabled,
        hidden,
    ) {
        Control(MutantBody.Box, key, if (checked) colors.primary else null, enabled, source) {
            // A disabled box draws its mark as it stands (the page's disabled cell); a live one melts it.
            val melt = if (enabled) Modifier.mutantMelt(checked) else Modifier
            val shown = checked
            MutantIcon(
                remember { MutantIcons.stroked(MutantIcons.Done, MARK_LINE) },
                null,
                melt.then(if (shown) Modifier.mutantCleanZone() else Modifier),
                tint = if (shown) tick else Color.Transparent,
                size = MARK,
            )
            if (enabled) {
                // Two burnt blocks in the corner (the page's `right: 2px` and `6px`, `bottom: 2px`, 4 dp each in 22).
                Block(16, 16, .5f, ink, if (enabled) Modifier.mutantMelt(checked) else Modifier, shown)
                Block(12, 16, .25f, ink, if (enabled) Modifier.mutantMelt(checked) else Modifier, shown)
            }
        }
    }
}

@Composable
private fun BoxScope.Block(
    x: Int,
    y: Int,
    alpha: Float,
    ink: Color,
    melt: Modifier,
    shown: Boolean,
) {
    Canvas(
        Modifier
            .align(Alignment.TopStart)
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .offset(x.dp, y.dp)
            .size(BLOCK)
            .then(melt),
    ) { if (shown) drawRect(ink, alpha = alpha) }
}

/** [MutantRadioButton]; [hidden] draws the dot and the label transparent for a fixture's bare twin. */
@Composable
internal fun RadioBody(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    label: String?,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    // A bare twin hides the dot (kept clean, `data-keep`, on the page) and keeps its echo.
    val red = if (hidden) Color.Transparent else RECORD
    ControlRow(
        modifier.selectable(
            selected,
            source,
            indication = null,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        label,
        enabled,
        hidden,
    ) {
        Control(MutantBody.Dot, key, null, enabled, source) {
            if (!enabled) {
                // The disabled dot: the page's grey, as it stands.
                if (selected) {
                    Dot(
                        6,
                        6,
                        if (hidden) Color.Transparent else colors.textDisabled,
                        1f,
                        Modifier.mutantCleanZone(),
                    )
                }
            } else {
                // The echo, offset up and right, and the dot itself, each melting in and out.
                Dot(9, 6, RECORD, ECHO, Modifier.mutantMelt(selected))
                Dot(
                    6,
                    6,
                    red,
                    1f,
                    Modifier.mutantMelt(selected).then(if (selected) Modifier.mutantCleanZone() else Modifier),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Dot(
    x: Int,
    y: Int,
    color: Color,
    alpha: Float,
    modifier: Modifier,
) {
    Canvas(
        Modifier
            .align(Alignment.TopStart)
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .offset(x.dp, y.dp)
            .size(DOT)
            .then(modifier),
    ) { drawCircle(color, alpha = alpha) }
}

/** A control's body, 22 × 22, its content centred and kept clean. */
@Composable
private fun Control(
    body: MutantBody,
    key: String,
    fill: Color?,
    enabled: Boolean,
    source: MutableInteractionSource,
    content: @Composable BoxScope.() -> Unit,
) {
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    MutantSurface(
        body,
        key,
        Modifier.size(SIDE),
        fill = fill,
        state = if (enabled) BodyState.Rest else BodyState.Disabled,
        focused = focused && keyboard,
        interactionSource = source,
    ) {
        Box(Modifier.size(SIDE), contentAlignment = Alignment.Center) { content() }
    }
}

/** The 48 dp row of a control and its label, 12 dp apart: the whole row is the touch target. */
@Composable
internal fun ControlRow(
    modifier: Modifier,
    label: String?,
    enabled: Boolean,
    hidden: Boolean,
    control: @Composable () -> Unit,
) {
    val colors = MutantTheme.colors
    Row(
        modifier.defaultMinSize(minWidth = ROW).height(ROW),
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        if (label != null) {
            val color =
                when {
                    hidden -> Color.Transparent
                    enabled -> colors.text
                    else -> colors.textDisabled
                }
            BasicText(
                label,
                Modifier.weight(1f, fill = false),
                style =
                    TextStyle(
                        fontFamily = LocalMutantFonts.current?.text,
                        fontSize = LABEL_SIZE.sp,
                        color = color,
                        platformStyle = LocalMutantTextRaster.current,
                    ).cssLine(MutantFaceMetrics.Rubik),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The row, the touch target. */
private val ROW = 48.dp

/** Between the control and its label (the page's `gap: 12px`). */
private val GAP = 12.dp

/** The control's body. */
private val SIDE = 22.dp

/** The tick: 18 dp, a 3 dp line (the page's `stroke-width="3"`). */
private val MARK = 18.dp
private const val MARK_LINE = 3f

/** A burnt block, 4 dp. */
private val BLOCK = 4.dp

/** The radio's dot and its echo, 10 dp; the echo at 30 %. */
private val DOT = 10.dp
private const val ECHO = .3f

/**
 * The recording dot's red: the page's literal `#ff3b30` in both themes (the «Эфир» danger), not «Копия»'s own.
 */
private val RECORD = Color(0xFFFF3B30)

/** The label's size, sp (the page's grid, `font-size: 15px`, weight 400). */
private const val LABEL_SIZE = 15
