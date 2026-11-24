package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.HeatMachine
import io.github.youndie.mutant.surface.LocalMutantMoshScene
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A modal bottom sheet (v1): a frame risen from below. A `blob` of the surface's colour (r 22, a top edge that waves,
 * wobble 1.6) wider than the screen by 8 dp a side and running past its foot, a grabber at its top, and a tear line
 * (the tracking divider) 2 dp under the window's top, above the sheet. Behind it a scrim darkens the screen and holds
 * its clock, as a dialog does.
 *
 * The design draws it open; its motion is designed here (D15): it rises from below over 330 ms (ease-out²) with the
 * scrim coming in, and sinks in 200 ms, both on every frame and in the kit's 83 ms steps under `steppedMotion`; under
 * reduce motion it is there at once. A sink during the rise, or a rise during the sink, goes on from where the sheet
 * stands. A finger or a mouse drags it down from anywhere on it, 1:1 from the pointer's first move, and its body boils
 * while dragged (B-183, the designer's answer 5): let go past 40 % of its height or faster than 800 dp/s down, it calls
 * [onDismissRequest] and goes on down; otherwise it settles back in 330 ms on the slide curve. Pulled up past open it
 * moves 0.3 of the pointer, at most 24 dp. The scrim is as dark as the sheet is up, rising, sinking or dragged. A click
 * on the scrim, Escape or the system back gesture calls [onDismissRequest]. Focus
 * moves into the sheet as it opens and stays there, a Tab on its controls only, coming round from the last to the
 * first and with Shift+Tab the other way; once it has sunk focus goes to [returnFocusTo], or with none is left to the
 * window. As it starts to sink, it and what is on it answer no press and no key.
 *
 * The sheet keeps its content above [windowInsets]; its body runs on under them. It is at most as tall as the screen,
 * and scrolls a content taller than that itself (B-184, the designer's answer 30): by a finger, the wheel, the page keys
 * and the focus moving onto a control below its foot; scrolled, the content is cut under the grabber. Each child keeps its
 * own height; one that cannot say it before it is measured (a lazy list) is given the room and scrolls itself, so a
 * `LazyColumn` works in it, and the sheet goes on where it stops. With the content at its top a pull down is the sheet's
 * drag; elsewhere a finger scrolls the content, and a finger's scroll that brings it to its top goes on as the sheet's
 * drag, let go as one. A wheel's scroll never pulls it, and a mouse, which scrolls nothing by a drag, drags it only with
 * the content at its top. Whether a lazy list is at its top is read from its scrolls, so one moved by its own state in
 * code is not seen until it scrolls.
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
    // B-183: dragged shut by a finger or a mouse, and boiling while dragged (the designer's answer 5).
    val drag = remember { PanelDrag() }
    val heat = remember { HeatMachine() }
    drag.over = PANEL_DRAG_OVER_DP * LocalDensity.current.density
    ProvideMutantClock(own) {
        val clock = readMutantClock()
        follow.follow(visible, clock.nowMs)
        val p = if (reduce) 1.0 else stepped(clock.nowMs - follow.since, if (visible) RISE_MS else SINK_MS, steps)
        val shown = if (visible) p else 1 - p
        // Ease-out²: the sheet rises fast and settles.
        val rise = (1 - (1 - shown) * (1 - shown)).toFloat()
        drag.scripted = 1 - rise
        // How far down the window stands, a share of its height: where a drag puts it, or the rise.
        val down = drag.follow(visible, clock.nowMs, still = reduce || clock.frozen, steps) ?: (1 - rise)
        if (p < 1 || drag.gliding) clock.frame
        SideEffect { if (drag.taken) heat.press(clock.nowMs) else heat.release(clock.nowMs) }
        // A drag's close glides on past the sink's end; the sheet stays until it is over.
        val gone = !visible && p >= 1 && !drag.closing
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
            SheetScene(visible, onDismissRequest, modifier, windowInsets, key, content, down, height, drag, heat)
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
    down: Float,
    height: Dp?,
    drag: PanelDrag,
    heat: HeatMachine,
) {
    val colors = MutantTheme.colors
    val dismissState = rememberUpdatedState(onDismissRequest)
    val dismiss by dismissState
    val open = rememberUpdatedState(visible)
    val towardsClosed = remember { mutableStateOf<(Offset) -> Float>({ o -> o.y }) }
    // B-184: the sheet scrolls its content, and a finger's pull at the content's top is the sheet's drag (answer 30).
    val scroll = remember { SheetScroll() }
    val atTop = remember(scroll) { { scroll.atTop } }
    val overPull = remember(scroll) { { scroll.overPull } }
    val pull = remember(drag, scroll) { SheetPull(drag, scroll, open, dismissState) }
    pull.density = LocalDensity.current.density
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Whether the sheet itself holds focus (as it opens, and where Shift+Tab from its first control would stop): it shows
    // nothing there.
    var onSelf by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) focus.requestFocus() }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = visible) { dismiss() }
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
                // As dark as the sheet is up, rising or dragged (the designer's answer 5).
                .graphicsLayer { alpha = (1 - down).coerceIn(0f, 1f) }
                .background(SCRIM)
                .focusProperties { canFocus = false }
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = visible) { dismiss() },
        )
        SheetWindow(
            key,
            windowInsets,
            height,
            heat,
            scroll,
            Modifier
                .align(Alignment.BottomStart)
                // The window drags it down (B-183); the scrim above it dismisses on a click. With its content scrolled
                // away from the top a press is the content's, and a finger's scroll the content hands back at its top
                // pulls the sheet (B-184).
                .sheetFinger(scroll)
                .nestedScroll(pull)
                .panelDrag(drag, open, dismissState, towardsClosed, atTop, overPull)
                .onSizeChanged { drag.size = it.height.toFloat() }
                .graphicsLayer { translationY = down * size.height }
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
    heat: HeatMachine,
    scroll: SheetScroll,
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
                heatMachine = heat,
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
                    SheetContent(scroll, Modifier.mutantCleanZone(), content)
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

/**
 * The sheet's own scroll of its content (B-184, the designer's answer 30): how far it stands from the top, px, and how far
 * it can go. Whether the content is at its top also reads the scrolls inside it ([inner]): a lazy list or a
 * `verticalScroll` of the consumer's scrolled away from its top keeps the sheet's drag off. Only scrolls are seen; a list
 * moved by its own state in code is not, until a scroll passes through.
 */
@Stable
internal class SheetScroll {
    /** How far the content is scrolled, px. */
    var offset by mutableFloatStateOf(0f)

    /** How far it can be scrolled, px: its height past the room. Written by its layout. */
    var max by mutableIntStateOf(0)

    /** A finger (or a pen) presses the sheet now: only its scrolls pull the sheet, never a wheel's or a key's. */
    var finger = false

    /** No scroll inside has been seen away from its top since the last one that reached it. */
    private var innerTop = true

    /** A scroll inside has been seen: the content scrolls by itself. */
    private var innerScrolls = false

    /** The content does not scroll, as far as has been seen: a pull up lifts the sheet past open (B-183). */
    val overPull: Boolean get() = max <= 0 && !innerScrolls

    /** The content stands at its top: a pull down is the sheet's. */
    val atTop: Boolean get() = offset <= 0f && innerTop

    val state =
        ScrollableState { delta ->
            val whole = offset + delta
            val to = whole.coerceIn(0f, max.toFloat())
            // Unclamped, all of it, exactly, as `ScrollState` answers: Compose's bring-into-view stops at a consumption a
            // float's rounding short of what it asked, and a focused control stood below the foot (B-184).
            val used = if (to == whole) delta else to - offset
            offset = to
            used
        }

    /** Sits between the sheet's scroll and its content: it sees what the content's own scrolls did, and takes nothing. */
    val inner =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Consumed up the content is away from its top; left over down, it is at it (cmp-ui-component §4).
                innerScrolls = true
                if (consumed.y < 0f) innerTop = false
                if (available.y > 0f) innerTop = true
                return Offset.Zero
            }
        }
}

/**
 * Above the sheet's scroll: what a finger's scroll leaves over down at the content's top pulls the sheet, 1:1 (B-184), and
 * a scroll back up takes the sheet back to open before the content moves. The fling at the lift lets it go as a release
 * does: past 40 % or faster than 800 dp/s down it is dismissed, otherwise it settles back (B-183). A wheel, the keys and a
 * fling's own glide never pull it: a wheel has no letting go.
 */
private class SheetPull(
    private val drag: PanelDrag,
    private val scroll: SheetScroll,
    private val open: State<Boolean>,
    private val dismiss: State<() -> Unit>,
) : NestedScrollConnection {
    var density = 1f

    /** The content's scrolls hold the sheet now. */
    private var pulling = false

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!pulling || available.y >= 0f) return Offset.Zero
        val at = drag.heldAt ?: return Offset.Zero
        if (at <= 0f) return Offset.Zero
        val back = max(available.y, -at)
        drag.pull(back)
        return Offset(0f, back)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (available.y <= 0f || source != NestedScrollSource.UserInput || !scroll.finger || !open.value) {
            return Offset.Zero
        }
        pulling = true
        drag.pull(available.y)
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!pulling) return Velocity.Zero
        pulling = false
        val at = drag.heldAt
        if (at == null || at <= 0f) {
            // Back at open: the content flings on.
            drag.drop()
            return Velocity.Zero
        }
        if (drag.release(available.y, density)) dismiss.value()
        return available
    }
}

/** Keeps [SheetScroll.finger]: whether a finger or a pen presses the sheet, seen before anything on it. */
private fun Modifier.sheetFinger(scroll: SheetScroll): Modifier =
    pointerInput(scroll) {
        awaitPointerEventScope {
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                scroll.finger = e.changes.any { it.pressed && it.type != PointerType.Mouse }
            }
        }
    }

/**
 * The sheet's content, a column [GAP] apart that the sheet scrolls itself (B-184, the designer's answer 30), by a finger,
 * the wheel, the page keys and the focus moving onto a control below its foot. Not a `verticalScroll`, which gives its
 * content no height limit, so a lazy list in it would throw: each child is measured to its own height, and a child that
 * cannot say it before it is measured (a lazy list, a `BoxWithConstraints`) is given the room, a screenful, and scrolls
 * itself; the sheet goes on where it stops. A weighted child shares what room the others leave, as in a `Column`;
 * `alignBy` has no effect here. Cut at its top only while scrolled away from it, so a content at rest is drawn as before.
 */
@Composable
private fun SheetContent(
    scroll: SheetScroll,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrolls = scroll.max > 0
    val squeezed = LocalMutantBottomSheetAsDesigned.current
    val reverse = ScrollableDefaults.reverseDirection(LocalLayoutDirection.current, Orientation.Vertical, false)
    Layout(
        content = { SheetColumnScope.content() },
        modifier =
            modifier
                .drawWithContent {
                    if (scroll.offset > 0f) {
                        // Scrolled, what runs above the content's top is cut there, under the grabber. Across and below,
                        // the content's drawing runs free (a press's echo, a body's drips, the window's foot), as at rest.
                        clipRect(-size.width, 0f, 2 * size.width, 2 * size.height + scroll.max) {
                            this@drawWithContent.drawContent()
                        }
                    } else {
                        drawContent()
                    }
                }.then(
                    if (scrolls) {
                        Modifier.semantics {
                            verticalScrollAxisRange = ScrollAxisRange({ scroll.offset }, { scroll.max.toFloat() })
                            scrollBy { _, y -> scroll.state.dispatchRawDelta(y) != 0f }
                        }
                    } else {
                        Modifier
                    },
                ).scrollable(scroll.state, Orientation.Vertical, enabled = scrolls, reverseDirection = reverse)
                // The viewport, as `verticalScroll` lays its own out: the column below says its whole height, which is
                // past the room when it scrolls, and is moved up by the scroll.
                .layout { m, c ->
                    val p = m.measure(c)
                    val whole = p.measuredHeight
                    scroll.max = (whole - p.height).coerceAtLeast(0)
                    layout(p.width, p.height) {
                        val at = scroll.offset.roundToInt().coerceIn(0, scroll.max)
                        // A placeable taller than its constraints is centred on them; its top goes where the scroll says.
                        p.place(0, (whole - p.height) / 2 - at)
                    }
                }.nestedScroll(scroll.inner),
    ) { measurables, c ->
        val room = c.maxHeight
        val gap = GAP.roundToPx()
        val data = measurables.map { it.parentData as? SheetColumnData }
        val across = Constraints(maxWidth = c.maxWidth)
        // A child's own height at this width, or -1 for one that cannot say before it is measured (a lazy list).
        val natural =
            measurables.mapIndexed { i, m ->
                if ((data[i]?.weight ?: 0f) > 0f) {
                    0
                } else {
                    try {
                        m.maxIntrinsicHeight(c.maxWidth)
                    } catch (_: IllegalStateException) {
                        -1
                    } catch (_: IllegalArgumentException) {
                        -1
                    }
                }
            }
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        var used = gap * (measurables.size - 1).coerceAtLeast(0)
        measurables.forEachIndexed { i, m ->
            if ((data[i]?.weight ?: 0f) > 0f) return@forEachIndexed
            val bound =
                when {
                    room == Constraints.Infinity -> Constraints.Infinity

                    // The page's column: each child given what room the ones above it left, a tall one squeezed.
                    squeezed -> (room - used + gap * (measurables.size - 1 - i)).coerceAtLeast(0)

                    natural[i] < 0 -> room

                    else -> max(natural[i], room)
                }
            val p = m.measure(across.copy(maxHeight = bound))
            placeables[i] = p
            used += p.height
        }
        val weight = data.sumOf { (it?.weight ?: 0f).toDouble() }.toFloat()
        val left = if (room == Constraints.Infinity) 0 else room - used
        measurables.forEachIndexed { i, m ->
            val d = data[i] ?: return@forEachIndexed
            if (d.weight <= 0f) return@forEachIndexed
            val p =
                if (left > 0) {
                    val share = (left * d.weight / weight).roundToInt()
                    m.measure(across.copy(minHeight = if (d.fill) share else 0, maxHeight = share))
                } else {
                    // No room left over: it takes its own height, and the sheet scrolls.
                    m.measure(across.copy(maxHeight = if (room == Constraints.Infinity) room else max(room, 0)))
                }
            placeables[i] = p
            used += p.height
        }
        val width = placeables.maxOf { it?.width ?: 0 }.coerceIn(c.minWidth, c.maxWidth)
        // Its whole height, past the room when it scrolls: the viewport above takes the room and moves it.
        layout(width, used.coerceAtLeast(c.minHeight)) {
            var y = 0
            placeables.forEachIndexed { i, p ->
                if (p == null) return@forEachIndexed
                val x = (data[i]?.align ?: Alignment.Start).align(p.width, width, layoutDirection)
                p.place(x, y)
                y += p.height + gap
            }
        }
    }
}

/**
 * B-184's departure from the page, for the parity cells (research D20's registry): true gives each child of the sheet's
 * content what room the ones above it left, as the page's column does, so the page's content, 2 dp taller than its room
 * in the kit, has its button squeezed and its word a row higher. The kit gives each child its own height and scrolls (the
 * designer's answer 30).
 */
internal val LocalMutantBottomSheetAsDesigned = staticCompositionLocalOf { false }

/** What a child of the sheet's content asks of it through [SheetColumnScope]. */
private class SheetColumnData(
    val align: Alignment.Horizontal? = null,
    val weight: Float = 0f,
    val fill: Boolean = true,
)

private data class SheetColumnDataModifier(
    val align: Alignment.Horizontal? = null,
    val weight: Float? = null,
    val fill: Boolean = true,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any {
        val was = parentData as? SheetColumnData ?: SheetColumnData()
        return SheetColumnData(align ?: was.align, weight ?: was.weight, if (weight != null) fill else was.fill)
    }
}

/** The sheet's content is a column: `align` and `weight` as in a `Column`; `alignBy` has no effect. */
private object SheetColumnScope : ColumnScope {
    override fun Modifier.weight(
        weight: Float,
        fill: Boolean,
    ): Modifier {
        require(weight > 0f) { "invalid weight $weight; must be greater than zero" }
        return then(SheetColumnDataModifier(weight = weight, fill = fill))
    }

    override fun Modifier.align(alignment: Alignment.Horizontal): Modifier =
        then(SheetColumnDataModifier(align = alignment))

    override fun Modifier.alignBy(alignmentLine: VerticalAlignmentLine): Modifier = this

    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}
