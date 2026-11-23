package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.LocalMutantMoshScene
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody

/**
 * A modal bottom sheet (v1): a frame risen from below. A `blob` of the surface's colour (r 22, a top edge that waves,
 * wobble 1.6) wider than the screen by 8 dp a side and running past its foot, a grabber at its top, and a tear line
 * (the tracking divider) 2 dp under the window's top, above the sheet. Behind it a scrim darkens the screen and holds
 * its clock, as a dialog does.
 *
 * The design draws it open; its motion is designed here (D15): it rises from below over 330 ms (ease-out²) with the
 * scrim coming in, and sinks in 200 ms, both on every frame and in the kit's 83 ms steps under `steppedMotion`; under
 * reduce motion it is there at once. A sink during the rise, or a rise during the sink, goes on from where the sheet
 * stands. It is not dragged. A click on the scrim, Escape or the system back gesture calls [onDismissRequest]. Focus
 * moves into the sheet as it opens and stays there, a Tab on its controls only, coming round from the last to the
 * first and with Shift+Tab the other way; once it has sunk focus goes to [returnFocusTo], or with none is left to the
 * window. As it starts to sink, it and what is on it answer no press and no key.
 *
 * The sheet keeps its content above [windowInsets]; its body runs on under them. It is at most as tall as the screen: a
 * content taller than that is given the room left and scrolls if it is made to (`verticalScroll`, a lazy list), or
 * runs off the screen's foot.
 */
@Composable
public fun MutantBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    key: String = "sheet",
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetFrame(visible, onDismissRequest, modifier, returnFocusTo, windowInsets, key, content)
}

/**
 * [MutantBottomSheet]; [playIn] false stands it open from the first frame, [height] fixes the window's height (a
 * fixture's still, at the page's place), [hidden] hides the grabber's partner words for a bare twin (none of its own).
 */
@Composable
internal fun SheetFrame(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    returnFocusTo: FocusRequester?,
    windowInsets: WindowInsets,
    key: String,
    content: @Composable ColumnScope.() -> Unit,
    playIn: Boolean = true,
    height: Dp? = null,
) {
    val reduce = LocalMutantReduceMotion.current
    val screen = readMutantClock()
    // The sheet's own clock: the screen's stands while it shows, and the sheet and what is on it still move.
    val own = remember { MutantClock(startMs = screen.nowMs, frozen = screen.frozen) }
    SideEffect { own.paused = reduce }
    // Given both durations, a sink during the rise goes down from where the sheet stands, and a rise during the sink
    // goes up from there, instead of restarting each from its end (B-168).
    val follow =
        remember { DialogFollow(visible, if (playIn) own.nowMs else Double.NEGATIVE_INFINITY, RISE_MS, SINK_MS) }
    val steps = LocalMutantSteppedMotion.current
    ProvideMutantClock(own) {
        val clock = readMutantClock()
        follow.follow(visible, clock.nowMs)
        val p = if (reduce) 1.0 else stepped(clock.nowMs - follow.since, if (visible) RISE_MS else SINK_MS, steps)
        if (p < 1) clock.frame
        val shown = if (visible) p else 1 - p
        val gone = !visible && p >= 1
        // Held while anything of it shows: the screen behind stands still.
        DisposableEffect(screen, gone) {
            if (!gone) screen.holds++
            onDispose { if (!gone) screen.holds-- }
        }
        // Focus goes back once the sheet is gone, not as it starts to sink: through the sink the scrim still covers the
        // screen, and an Enter pressed then would reach the opener under it and open the sheet again (B-168).
        ReturnFocus(!gone, returnFocusTo)
        if (gone) return@ProvideMutantClock
        // The sheet floats over the screen, so the screen's keyframe loss does not take its body: a loss held mid-run by
        // the sheet would cut its band out of the sheet for as long as it shows (B-98's rule, B-168).
        CompositionLocalProvider(LocalMutantMoshScene provides null) {
            SheetScene(visible, onDismissRequest, modifier, windowInsets, key, content, shown.toFloat(), height)
        }
    }
}

@Composable
private fun SheetScene(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    windowInsets: WindowInsets,
    key: String,
    content: @Composable ColumnScope.() -> Unit,
    shown: Float,
    height: Dp?,
) {
    val colors = MutantTheme.colors
    val dismiss by rememberUpdatedState(onDismissRequest)
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Whether the sheet itself holds focus (as it opens, and where Shift+Tab from its first control would stop): it shows
    // nothing there.
    var onSelf by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) focus.requestFocus() }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = visible) { dismiss() }
    // Ease-out²: the sheet rises fast and settles.
    val rise = 1 - (1 - shown) * (1 - shown)
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            // Focus stays in here while it is open: what is under the scrim cannot be reached by the keys.
            .focusProperties { onExit = { if (visible) cancelFocusChange() } }
            .focusGroup()
            .focusRequester(focus)
            .onPreviewKeyEvent {
                when {
                    // Sinking, it answers no key: an Enter on the control that closed it would fire it again (B-168).
                    !visible -> {
                        true
                    }

                    it.type == KeyEventType.KeyDown && it.key == Key.Escape -> {
                        dismiss()
                        true
                    }

                    // Tab past the last control comes round to the first, Shift+Tab past the first to the last, as in the
                    // dialog (B-57): never onto the sheet itself, which shows nothing (B-168).
                    it.type == KeyEventType.KeyDown && it.key == Key.Tab -> {
                        val back = it.isShiftPressed
                        if (!focusManager.moveFocus(if (back) FocusDirection.Previous else FocusDirection.Next) ||
                            onSelf
                        ) {
                            sheetFocusRound(focus, focusManager, back)
                        }
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
            // Nor a press: what is on it stops answering as it starts to sink, as the scrim does (B-168).
            .pointerInput(visible) {
                if (!visible) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            }.onFocusChanged { onSelf = it.isFocused }
            .focusable(),
    ) {
        // The scrim: it takes every click above the sheet, and a click on it dismisses. Not a Tab stop.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown }
                .background(SCRIM)
                .focusProperties { canFocus = false }
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = visible) { dismiss() },
        )
        SheetWindow(
            key,
            windowInsets,
            height,
            Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer { translationY = (1 - rise) * size.height }
                .semantics {
                    dismiss {
                        dismiss()
                        true
                    }
                },
            content,
        )
    }
}

/**
 * The window: the tear line 2 dp down, and the sheet's body from 14 dp, 8 dp past each side and [BLEED] past the
 * window's foot. Its height is the content's, or [height] for a fixture.
 */
@Composable
private fun SheetWindow(
    key: String,
    windowInsets: WindowInsets,
    height: Dp?,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MutantTheme.colors
    val insets = windowInsets.asPaddingValues()
    Layout(
        content = {
            MutantSurface(
                MutantBody.Blob,
                key,
                // Its own clicks: none reaches the scrim under it. A pointer target only, not a `clickable`: a press on
                // a clickable asks for focus, and the body took it, a Tab stop that shows nothing (B-57, B-168).
                Modifier.pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
                fill = colors.surface,
                // The page's `data-r="22" data-over="0" data-wob="1.6" data-marks="0"`.
                profile = MutantBody.Blob.profile.with(radius = RADIUS, overhang = 0.0, wobble = WOBBLE, marks = false),
            ) {
                Column(
                    Modifier.padding(
                        start = SIDE_PAD,
                        end = SIDE_PAD,
                        top = TOP_PAD,
                        bottom = FOOT_PAD + BLEED + insets.calculateBottomPadding(),
                    ),
                    verticalArrangement =
                        androidx.compose.foundation.layout.Arrangement
                            .spacedBy(GAP),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(GRABBER_W, GRABBER_H)
                            .mutantCleanZone()
                            .background(GRABBER, RoundedCornerShape(GRABBER_RADIUS)),
                    )
                    Column(
                        Modifier.mutantCleanZone(),
                        verticalArrangement =
                            androidx.compose.foundation.layout.Arrangement
                                .spacedBy(GAP),
                    ) {
                        content()
                    }
                }
            }
            // The tear line over the body, as the engine draws a stage's dividers after its bodies: the sheet's
            // mosaic rises past its top edge and under the line.
            MutantDivider()
        },
        modifier = modifier,
    ) { measurables, c ->
        val w = c.maxWidth
        val side = SIDE_BLEED.roundToPx()
        val top = BODY_TOP.roundToPx()
        val bleed = BLEED.roundToPx()
        val body =
            if (height != null) {
                measurables[0].measure(Constraints.fixed(w + 2 * side, height.roundToPx() - top + bleed))
            } else {
                // At most as tall as the screen: a content that scrolls learns its room (with none it threw), and a
                // taller one runs off the foot with its top kept, where it was centred and cut at both ends (B-168).
                val room =
                    if (c.hasBoundedHeight) {
                        (c.maxHeight - top + bleed).coerceAtLeast(
                            0,
                        )
                    } else {
                        Constraints.Infinity
                    }
                measurables[0].measure(Constraints(minWidth = w + 2 * side, maxWidth = w + 2 * side, maxHeight = room))
            }
        val line = measurables[1].measure(Constraints.fixed(w, LINE_H.roundToPx()))
        layout(w, body.height - bleed + top) {
            body.place(-side, top)
            line.place(0, LINE_TOP.roundToPx())
        }
    }
}

/**
 * Takes focus round the sheet's [sheet] group from its other end: to its first control going forward, to its last going
 * [back]. Compose gives no way to name them, so it steps from the sheet itself; with no control it stays there.
 */
private fun sheetFocusRound(
    sheet: FocusRequester,
    focusManager: FocusManager,
    back: Boolean,
) {
    sheet.requestFocus()
    if (!focusManager.moveFocus(FocusDirection.Next)) return
    if (back) {
        var steps = 0
        while (steps++ < SHEET_FOCUS_STEPS && focusManager.moveFocus(FocusDirection.Next)) {
            continue
        }
    }
}

/** The most controls [sheetFocusRound] steps over to reach the last: a bound, not a number of the design. */
private const val SHEET_FOCUS_STEPS = 256

/** The page's scrim: `rgba(5, 6, 12, .5)`. */
private val SCRIM = Color(0x8005060C)

/** The grabber: 44 × 5, r 3, the page's literal `#05060c` in both themes. */
private val GRABBER = Color(0xFF05060C)
private val GRABBER_W = 44.dp
private val GRABBER_H = 5.dp
private val GRABBER_RADIUS = 3.dp

/** The window: the tear line 2 dp down and 2 dp tall; the body from 14 dp, 8 dp past each side, 26 dp past the foot. */
private val LINE_TOP = 2.dp
private val LINE_H = 2.dp
private val BODY_TOP = 14.dp
private val SIDE_BLEED = 8.dp
private val BLEED = 26.dp

/** Inside the body: 12 dp from its top, 30 from its sides, 8 between the parts; 12 under the content (designed here). */
private val TOP_PAD = 12.dp
private val SIDE_PAD = 30.dp
private val GAP = 8.dp
private val FOOT_PAD = 12.dp

/** The body: `data-r="22" data-wob="1.6"`. */
private const val RADIUS = 22.0
private const val WOBBLE = 1.6

/** Rising and sinking (designed here): the kit's slide time in, the dialog's close time out. */
private const val RISE_MS = 330.0
private const val SINK_MS = 200.0
