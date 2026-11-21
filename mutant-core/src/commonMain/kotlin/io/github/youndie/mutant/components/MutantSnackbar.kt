package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A snackbar: the `bar` body in the no-signal blue with its deep misregistered shadow, 6 dp corners, 48 dp tall across
 * what it is given, [message] (Rubik 14) 16 dp from its start and [actionLabel] (Handjet 20, tracked 1, the on-blue
 * accent) at its end; [onAction] runs on the action's release. It reads as a polite live region.
 *
 * It enters (`osd-in`) as it is composed with [visible] true, or when [visible] becomes true: it rides up from its own
 * height and 8 dp below its place in 240 ms, stepped twelve times a second, eased out. Hidden (`osd-out`), it rides
 * back down in 200 ms, its action no longer answering from the moment it starts to leave, and once gone it is not
 * composed at all and [onHidden] is called. Under reduce motion both are at once. Whatever holds it at the screen's
 * bottom hides what has ridden off.
 */
@Composable
public fun MutantSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onHidden: () -> Unit = {},
    key: String = "snack",
) {
    SnackbarBody(message, modifier, visible, actionLabel, onAction, onHidden, key)
}

/**
 * [MutantSnackbar]; [hidden] draws its words transparent for a fixture's bare twin, and [playIn] false stands it at
 * its place from the first frame (a fixture's still, whose clock does not run). [probe] is told the body's instance
 * ([MutantSurface]'s): for tests only, never for a consumer.
 */
@Composable
internal fun SnackbarBody(
    message: String,
    modifier: Modifier,
    visible: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
    onHidden: () -> Unit,
    key: String,
    hidden: Boolean = false,
    playIn: Boolean = true,
    probe: ((Instance) -> Unit)? = null,
) {
    val colors = MutantTheme.colors
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val steps = LocalMutantSteppedMotion.current
    val ride = remember { SnackbarRide(visible, if (playIn) clock.nowMs else Double.NEGATIVE_INFINITY) }
    ride.follow(visible, clock.nowMs)
    val duration = if (visible) IN_MS else OUT_MS
    val elapsed = if (reduce) Double.POSITIVE_INFINITY else clock.nowMs - ride.since
    val p = stepped(elapsed, duration, steps)
    // While it rides it reads every frame, so composition follows it and it leaves the moment it is gone. Stepped, it
    // lands a step after its duration (250 ms for 200): asked by the time alone, it would stand at 83 % for ever.
    if (p < 1) clock.frame
    val gone = !visible && p >= 1
    val done by rememberUpdatedState(onHidden)
    LaunchedEffect(gone) { if (gone && ride.left) done() }
    if (gone) return
    // How far below its place it stands: all the way before it enters and after it leaves.
    val out = if (visible) (1 - p).pow(3) else 1 - (1 - p).pow(3)
    MutantSurface(
        MutantBody.Bar,
        key,
        modifier
            .fillMaxWidth()
            .heightIn(min = HEIGHT)
            .graphicsLayer { translationY = (out * (size.height + RIDE_BELOW.toPx())).toFloat() }
            .semantics { liveRegion = LiveRegionMode.Polite },
        fill = colors.noSignal,
        shadow = colors.shadowDeep,
        // The page's `data-r="6"` and `data-marks="0"`.
        profile = MutantBody.Bar.profile.with(radius = RADIUS, marks = false),
        probe = probe,
    ) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = HEIGHT).padding(
                start = SIDE,
                end =
                    if (actionLabel ==
                        null
                    ) {
                        SIDE
                    } else {
                        0.dp
                    },
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                message,
                Modifier.weight(1f, fill = false).mutantCleanZone(),
                style =
                    TextStyle(
                        fontFamily = LocalMutantFonts.current?.text,
                        fontSize = MESSAGE_SIZE.sp,
                        color = if (hidden) Color.Transparent else colors.onNoSignal,
                        platformStyle = LocalMutantTextRaster.current,
                    ).cssLine(MutantFaceMetrics.Rubik),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (actionLabel != null) SnackbarAction(actionLabel, onAction, visible, hidden)
        }
    }
}

/**
 * The action: its word 16 dp from the bar's end, its target the bar's height and at least 48 wide, and never more than
 * half the bar: a longer word ends in an ellipsis and the message keeps the rest (B-70).
 */
@Composable
private fun SnackbarAction(
    label: String,
    onAction: () -> Unit,
    enabled: Boolean,
    hidden: Boolean,
) {
    val colors = MutantTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        Modifier
            .mutantHand(enabled)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onAction)
            .layout { m, c ->
                // Measured before the weighted message, with the whole bar to take: half of it at most.
                val cap =
                    if (c.hasBoundedWidth) {
                        maxOf(
                            c.minWidth,
                            (c.maxWidth * ACTION_SHARE).roundToInt(),
                        )
                    } else {
                        c.maxWidth
                    }
                val p = m.measure(c.copy(maxWidth = cap))
                layout(p.width, p.height) { p.place(0, 0) }
            }.fillMaxHeight()
            .widthIn(min = TOUCH)
            .drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(1f, 1f, size.width / density - 1, size.height / density - 1, colors.focus)
                    }
                }
            }.padding(start = ACTION_GAP, end = SIDE),
        contentAlignment = Alignment.CenterEnd,
    ) {
        BasicText(
            label,
            Modifier.mutantCleanZone(),
            style =
                TextStyle(
                    fontFamily = LocalMutantFonts.current?.display,
                    fontSize = ACTION_SIZE.sp,
                    letterSpacing = ACTION_TRACKING.sp,
                    color = if (hidden) Color.Transparent else colors.onNoSignalAccent,
                    platformStyle = LocalMutantTextRaster.current,
                ).cssLine(MutantFaceMetrics.Handjet),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The share of the bar an action may take (B-70). */
private const val ACTION_SHARE = .5f

/**
 * Follows [visible] from composition without writing state: a change starts the ride then. It starts settled where it
 * is first composed visible — at the start of its entrance — and hidden, as gone.
 */
internal class SnackbarRide(
    private var shown: Boolean,
    now: Double,
) {
    var since: Double = if (shown) now else Double.NEGATIVE_INFINITY
        private set

    /** Whether it has been shown and has left since, so its leaving is reported once. */
    var left: Boolean = false
        private set

    fun follow(
        visible: Boolean,
        now: Double,
    ) {
        if (visible == shown) return
        shown = visible
        since = now
        left = !visible
    }
}

/** The bar (`height: 48px`), its corner (`data-r="6"`) and its sides (`padding: 0 16px`). */
private val HEIGHT = 48.dp
private const val RADIUS = 6.0
private val SIDE = 16.dp

/** The message (`font-size: 14px`) and the action (Handjet `20px`, `letter-spacing: 1px`), sp. */
private const val MESSAGE_SIZE = 14
private const val ACTION_SIZE = 20
private const val ACTION_TRACKING = 1

/** The action's target and the room it keeps before its word (designed here, D15). */
private val TOUCH = 48.dp
private val ACTION_GAP = 12.dp

/** The brief's `osd-in` and `osd-out`, and how far below its place it rides from (designed here, D15). */
private const val IN_MS = 240.0
private const val OUT_MS = 200.0
private val RIDE_BELOW = 8.dp
