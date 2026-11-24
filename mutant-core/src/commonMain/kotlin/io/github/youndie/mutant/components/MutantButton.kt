package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.TrackingDivider
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.inkOnBody
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** What a [MutantButton] is doing: at rest, working (the seek bands), or done (the clean frame, held). */
public enum class MutantButtonState {
    Idle,

    /**
     * The seek bands run over the body (`seek`, 900 ms a cycle); the button keeps keyboard focus but takes no click, and
     * its body shows no press and no hover.
     */
    Loading,

    /** The clean frame, static, with the check before the label, until the caller changes the state. */
    Success,
}

/**
 * The primary action: an acid plate whose lower edge runs (body `btn`). Pressed, it boils; the action fires
 * on release. [state] shows work and its end; disabled, the body loses its signal. The label is one line, cut
 * with an ellipsis where the width runs out; the button is 48 dp tall, the touch target, whatever its width.
 *
 * Given no width, it is as wide as the widest of its states, Success's check before the label included, in every
 * state: no change of [state], of [enabled] or a press moves its width or regenerates its body (B-203). A new [text] or
 * [icon] is a new measurement; a width from the caller wins.
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
        // B-203: without an icon of the caller's, Success adds the check; every other state keeps its room.
        checkRoom = icon == null,
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
 * The quietest action: no body, the accent label over a tracking divider as wide as the label. Pressed, the label drops
 * 1 dp and a solid 2 dp line in its colour stands in the divider's place; released, it is at rest in the next frame, and
 * under reduce motion the same (B-192). Disabled, the label greys (the design draws no disabled text button; research
 * D15).
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
    val focused by source.collectIsFocusedAsState()
    // Read where it is drawn and placed, not here: a press changes no composition (B-192).
    val pressed = source.collectIsPressedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val seed = "${LocalMutantCassette.current}/${LocalMutantStage.current}/div$key"
    val label = if (enabled) colors.textAccent else colors.textDisabled
    val ink = inkOnBody(label)
    Column(
        modifier
            .mutantHand(enabled)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            // It has no body to draw them on: the brackets stand at the corners of its 48 dp target (B-57).
            .drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(1f, 1f, size.width / density - 1, size.height / density - 1, colors.focus)
                    }
                }
            }.defaultMinSize(minWidth = TOUCH)
            .heightIn(min = TOUCH)
            .width(IntrinsicSize.Max)
            .padding(horizontal = TEXT_PADDING),
        verticalArrangement = Arrangement.spacedBy(TEXT_GAP, Alignment.CenterVertically),
    ) {
        ButtonLabel(text, label, Modifier, inner = Modifier.labelPressDrop(pressed))
        // A tracking divider as the top app bar's is: the screen's keyframe loss takes it with the bodies (B-198). Pressed,
        // the solid line in the label's ink stands in its place (B-192).
        TrackingDivider(
            seed,
            DIVIDER.value,
            colors,
            (if (dividerWidth != null) Modifier.width(dividerWidth) else Modifier.fillMaxWidth())
                .height(DIVIDER)
                .textButtonPressLine(pressed, ink),
        )
    }
}

/**
 * B-192, the designer's answer 15: pressed, the label's ink stands [PRESS_DROP] lower. Moved where it is placed, inside
 * the label's clean zone, so the zone, and with it the mosaic of a body under the button, stays where it is at rest.
 */
internal fun Modifier.labelPressDrop(pressed: State<Boolean>): Modifier =
    offset { IntOffset(0, if (pressed.value) PRESS_DROP.roundToPx() else 0) }

/** B-192: pressed, a solid line in the label's [ink] instead of the tracking divider drawn under it, as tall as its slot. */
private fun Modifier.textButtonPressLine(
    pressed: State<Boolean>,
    ink: Color,
): Modifier =
    drawWithContent {
        if (pressed.value) drawRect(ink, Offset.Zero, Size(size.width, size.height)) else drawContent()
    }

/**
 * The filled and outlined buttons; [heat] holds a fixture's boil (the page's `data-heat`), null in the kit. [checkRoom]:
 * a state of this button shows the check where [icon] is null now, so the content keeps the check's room in every
 * state (B-203). [probe] is the surface's, for tests only.
 */
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
    checkRoom: Boolean = false,
    probe: ((io.github.youndie.mutant.gen.Instance) -> Unit)? = null,
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
    // Busy, it takes no click, so its body shows neither a press nor the mouse (B-168).
    val busy = rememberUpdatedState(loading)
    val heard = remember(source) { QuietWhileBusy(source, busy) }
    MutantSurface(
        MutantBody.Btn,
        key,
        modifier
            .defaultMinSize(minWidth = TOUCH, minHeight = TOUCH)
            .heightIn(min = TOUCH)
            .semantics {
                if (loading) {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    // Busy: a reader is told it cannot be pressed now, while focus stays on it.
                    disabled()
                }
            }
            // Loading keeps the button enabled, so keyboard focus stays on it through the work and its end (B-62); it
            // takes no click while it lasts, and its semantics say it is busy; the arrow, not the hand, says so too.
            .mutantHand(enabled && !loading)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button) { if (!loading) onClick() },
        fill = if (outlined) colors.background else null,
        shadow = if (outlined && enabled) colors.misregister else null,
        state = body,
        focused = focused && keyboard,
        interactionSource = heard,
        heat = heat,
        profile = if (outlined) MutantBody.Btn.profile.with(overhang = OUTLINED_OVERHANG) else MutantBody.Btn.profile,
        stroke = if (outlined && enabled) colors.textAccent else null,
        probe = probe,
    ) {
        // B-203: the check Success adds keeps its room in every other state.
        val room = if (checkRoom && icon == null) BUTTON_ICON_SIZE + ICON_GAP else 0.dp
        Row(
            Modifier.buttonRoom(room).padding(horizontal = PADDING),
            horizontalArrangement = Arrangement.spacedBy(ICON_GAP, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) MutantIcon(icon, null, Modifier.mutantCleanZone(), tint = label, size = BUTTON_ICON_SIZE)
            ButtonLabel(text, label, Modifier.weight(1f, fill = false))
        }
    }
}

/**
 * B-203: the content as wide as it is with [room] more, so a state that adds an icon finds its width already there and
 * the body keeps its size; a width from the caller (the surface's constraints) still wins. The widest state is
 * measured from the content itself, so a new label is a new measurement.
 */
private fun Modifier.buttonRoom(room: Dp): Modifier =
    if (room <= 0.dp) {
        this
    } else {
        layout { measurable, constraints ->
            val widest = measurable.maxIntrinsicWidth(constraints.maxHeight) + room.roundToPx()
            val width = widest.coerceIn(constraints.minWidth, constraints.maxWidth)
            val placeable = measurable.measure(constraints.copy(minWidth = width))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
    }

/**
 * What a button's body hears of its interactions (B-168). While it is busy (Loading) it takes no click, so it shows no
 * press and no hover: a press or a hover under way when the work starts lets go, and a mouse still over it when the work
 * ends trembles again (B-193). Focus is read from the button's own source, not from this.
 */
private class QuietWhileBusy(
    private val source: InteractionSource,
    private val busy: State<Boolean>,
) : InteractionSource {
    override val interactions: Flow<Interaction> =
        channelFlow {
            var over: HoverInteraction.Enter? = null
            launch {
                source.interactions.collect {
                    when (it) {
                        is HoverInteraction.Enter -> over = it
                        is HoverInteraction.Exit -> over = null
                    }
                    if (!busy.value || (it !is PressInteraction.Press && it !is HoverInteraction.Enter)) send(it)
                }
            }
            snapshotFlow { busy.value }.drop(1).collect { now ->
                if (now) {
                    send(PressInteraction.Cancel(PressInteraction.Press(Offset.Zero)))
                    over?.let { send(HoverInteraction.Exit(it)) }
                } else {
                    over?.let { send(it) }
                }
            }
        }
}

/** A button's label: Rubik 500 at 15 (a chip's at 13), on Blink's line box (research §1.14), one line. */
@Composable
internal fun ButtonLabel(
    text: String,
    color: Color,
    modifier: Modifier,
    size: Int = LABEL_SIZE,
    other: Color = MutantTheme.colors.onPrimary,
    inner: Modifier = Modifier,
) {
    MutantText(
        text,
        // [inner] moves the ink inside its clean zone (the text button's press, B-192).
        modifier.mutantCleanZone().then(inner),
        style = buttonLabelStyle(size, inkOnBody(color, other)),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A button's or a chip's label: Rubik [size]/500 on Blink's line. */
@Composable
internal fun buttonLabelStyle(
    size: Int,
    color: Color,
): TextStyle =
    TextStyle(
        fontFamily = LocalMutantFonts.current?.text,
        fontWeight = FontWeight.Medium,
        fontSize = size.sp,
        color = color,
        platformStyle = LocalMutantTextRaster.current,
    ).cssLine(MutantFaceMetrics.Rubik)

/** The touch target and the body's height. */
private val TOUCH = 48.dp

/** The label's distance from the body's sides (the design draws fixed widths; research D15). */
private val PADDING = 24.dp

/** An icon before the label (the success cell's check). */
private val BUTTON_ICON_SIZE = 24.dp

/** Between an icon and the label (the success cell's `gap: 8px`). */
private val ICON_GAP = 8.dp

/** The text button: its label 12 dp in, the divider 5 dp under it, 2 dp tall (the page's cell). */
private val TEXT_PADDING = 12.dp
private val TEXT_GAP = 5.dp
private val DIVIDER = 2.dp

/** How far a pressed label drops, the text button's and the snackbar action's (the designer's answer 15, B-192). */
internal val PRESS_DROP = 1.dp

/** The outlined button's drip (`data-over="14"`). */
private const val OUTLINED_OVERHANG = 14.0

/** The label's size, sp: the page's `font-size: 15px; font-weight: 500`. */
private const val LABEL_SIZE = 15
