package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.drawTrackingDivider
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/** What a [MutantButton] is doing: at rest, working (the seek bands), or done (the clean frame, held). */
public enum class MutantButtonState {
    Idle,

    /** The seek bands run over the body (`seek`, 900 ms a cycle); the button takes no click. */
    Loading,

    /** The clean frame, static, with the check before the label, until the caller changes the state. */
    Success,
}

/**
 * The primary action: an acid plate whose lower edge runs (body `btn`). Pressed, it boils; the action fires
 * on release. [state] shows work and its end; disabled, the body loses its signal. The label is one line, cut
 * with an ellipsis where the width runs out; the button is 48 dp tall, the touch target, whatever its width.
 */
@Composable
public fun MutantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: MutantButtonState = MutantButtonState.Idle,
    icon: ImageVector? = null,
    key: String = text,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MutantTheme.colors
    ButtonBody(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        state = state,
        icon = icon ?: if (state == MutantButtonState.Success) MutantIcons.Done else null,
        key = key,
        interactionSource = interactionSource,
        label = colors.onPrimary,
        outlined = false,
    )
}

/**
 * A secondary action: the background's colour inside an accent outline, with the misregistered shadow and a
 * shorter drip (`over` 14). Pressed, it boils; disabled, it loses its signal and its label greys.
 */
@Composable
public fun MutantOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    key: String = text,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MutantTheme.colors
    ButtonBody(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        state = MutantButtonState.Idle,
        icon = icon,
        key = key,
        interactionSource = interactionSource,
        label = if (enabled) colors.textAccent else colors.textDisabled,
        outlined = true,
    )
}

/**
 * The quietest action: no body, the accent label over a tracking divider as wide as the label. Disabled, the
 * label greys (the design draws no disabled text button; research D15).
 */
@Composable
public fun MutantTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    key: String = text,
    interactionSource: MutableInteractionSource? = null,
) {
    TextButtonBody(text, onClick, modifier, enabled, key, interactionSource)
}

/** [MutantTextButton]; [dividerWidth] stands for a fixture's fixed cell (the page's 76 dp), null under the label. */
@Composable
internal fun TextButtonBody(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    dividerWidth: androidx.compose.ui.unit.Dp? = null,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val seed = "${LocalMutantCassette.current}/${LocalMutantStage.current}/div$key"
    Column(
        modifier
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = TOUCH)
            .height(TOUCH)
            .width(IntrinsicSize.Max)
            .padding(horizontal = TEXT_PADDING),
        verticalArrangement = Arrangement.spacedBy(TEXT_GAP, Alignment.CenterVertically),
    ) {
        Label(text, if (enabled) colors.textAccent else colors.textDisabled, Modifier)
        Canvas(
            (
                if (dividerWidth !=
                    null
                ) {
                    Modifier.width(dividerWidth)
                } else {
                    Modifier.fillMaxWidth()
                }
            ).height(DIVIDER),
        ) {
            drawTrackingDivider(
                seed,
                size.width / density,
                2f,
                colors,
            )
        }
    }
}

/** The filled and outlined buttons; [heat] holds a fixture's boil (the page's `data-heat`), null in the kit. */
@Composable
internal fun ButtonBody(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    state: MutantButtonState,
    icon: ImageVector?,
    key: String,
    interactionSource: MutableInteractionSource?,
    label: Color,
    outlined: Boolean,
    heat: Double? = null,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val loading = state == MutantButtonState.Loading
    val body =
        when {
            !enabled -> BodyState.Disabled
            loading -> BodyState.Loading
            state == MutantButtonState.Success -> BodyState.Success
            else -> BodyState.Rest
        }
    MutantSurface(
        MutantBody.Btn,
        key,
        modifier
            .defaultMinSize(minWidth = TOUCH, minHeight = TOUCH)
            .height(TOUCH)
            .semantics { if (loading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .clickable(source, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        fill = if (outlined) colors.background else null,
        shadow = if (outlined && enabled) colors.misregister else null,
        state = body,
        focused = focused && keyboard,
        interactionSource = source,
        heat = heat,
        profile = if (outlined) MutantBody.Btn.profile.with(overhang = OUTLINED_OVERHANG) else MutantBody.Btn.profile,
        stroke = if (outlined && enabled) colors.textAccent else null,
    ) {
        Row(
            Modifier.padding(horizontal = PADDING),
            horizontalArrangement = Arrangement.spacedBy(ICON_GAP, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) MutantIcon(icon, null, Modifier.mutantCleanZone(), tint = label)
            Label(text, label, Modifier.weight(1f, fill = false))
        }
    }
}

/** A button's label: Rubik 500 at 15, on Blink's line box (research §1.14), one line. */
@Composable
private fun Label(
    text: String,
    color: Color,
    modifier: Modifier,
) {
    BasicText(
        text,
        modifier.mutantCleanZone(),
        style =
            TextStyle(
                fontFamily = LocalMutantFonts.current?.text,
                fontWeight = FontWeight.Medium,
                fontSize = LABEL_SIZE.sp,
                color = color,
                platformStyle = LocalMutantTextRaster.current,
            ).cssLine(MutantFaceMetrics.Rubik),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The touch target and the body's height. */
private val TOUCH = 48.dp

/** The label's distance from the body's sides (the design draws fixed widths; research D15). */
private val PADDING = 24.dp

/** Between an icon and the label (the success cell's `gap: 8px`). */
private val ICON_GAP = 8.dp

/** The text button: its label 12 dp in, the divider 5 dp under it, 2 dp tall (the page's cell). */
private val TEXT_PADDING = 12.dp
private val TEXT_GAP = 5.dp
private val DIVIDER = 2.dp

/** The outlined button's drip (`data-over="14"`). */
private const val OUTLINED_OVERHANG = 14.0

/** The label's size, sp: the page's `font-size: 15px; font-weight: 500`. */
private const val LABEL_SIZE = 15
