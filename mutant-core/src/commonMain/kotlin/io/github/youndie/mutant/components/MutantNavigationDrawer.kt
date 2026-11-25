package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.VenomShapes
import io.github.youndie.mutant.surface.HeatMachine
import io.github.youndie.mutant.surface.LocalMutantMoshScene
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.drawVenomFill
import io.github.youndie.mutant.surface.drawVenomInk
import io.github.youndie.mutant.surface.gooLayer
import io.github.youndie.mutant.surface.inkOnBody
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.mutantPlaceKey
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.surface.toPath
import io.github.youndie.mutant.surface.venomSpring
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantMotion
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/** One destination of a [MutantNavigationDrawer]: its channel [code] (`CH 01`) and its [label]. */
public class MutantDrawerItem(
    public val code: String,
    public val label: String,
)

/**
 * A modal navigation drawer (v1), the channels' side menu: a `bar` of the surface's colour standing in from the
 * screen's start, its fill slipped 3 dp off its outline over a misregistered shadow, its corners square and run off the
 * screen's start and top by 10 dp; behind it a scrim darkens the screen and holds its clock, as a dialog does.
 *
 * In it, 26 dp down, the [title] in the on-screen-display amber (Handjet 20, tracked 1) and the [items], 42 dp rows 4
 * dp apart: the channel's code (Handjet 20, tracked 1) and its label (Rubik 15) 12 dp after it. The [selectedIndex]
 * stands in a clean window (body `pill`, radius 8), its words in black; a new one is reached by venom, as the menu's
 * (B-33). Through a run a word is black where the lime lies under it and in the text's colour elsewhere, so it turns as
 * the slime reaches it (the page turns the words 340 ms in). A row is its whole target, 42 dp tall: an owner-named exception to the 48 dp rule (research D19).
 *
 * The design draws it open; its motion is designed here (D15): it slides in from the start over 330 ms (ease-out², on
 * every frame, in the kit's transition steps under `steppedMotion`) with the scrim coming in, and out in 200 ms; turned
 * round mid-way it goes on from where it stands, and under reduce motion it is there at once. A finger or a mouse drags it
 * towards the start from the window or the scrim, 1:1 from the pointer's first move, and its body boils while dragged
 * (B-183, the designer's answer 5): let go past 40 % of its width or faster than 800 dp/s towards the start, it calls
 * [onDismissRequest] and goes on out; otherwise it settles back in 330 ms on the slide curve. Pulled past open it moves
 * 0.3 of the pointer, at most 24 dp. The scrim is as dark as the window is open, sliding or dragged. A closed drawer is
 * not composed, so it is not pulled out from the edge. A click on the scrim, Escape or the system back gesture calls
 * [onDismissRequest]; from then on no row answers. Focus moves into the drawer as it opens, Up or Down there takes it to
 * the chosen row and then from row to row; Tab steps the rows, coming round from the last to the first and with
 * Shift+Tab the other way, and never leaves the drawer while it is open. Once the drawer has gone focus goes to
 * [returnFocusTo].
 *
 * [key] seeds its body: by default its place in the composition and its body type (B-206), so two unkeyed ones are
 * two bodies and one keeps its body whatever its words or state; in a list, give it from the item's id.
 */
@Composable
public fun MutantNavigationDrawer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MutantDrawerItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = "КАНАЛЫ",
    returnFocusTo: FocusRequester? = null,
    key: String = mutantPlaceKey("drawer"),
) {
    DrawerFrame(visible, onDismissRequest, items, selectedIndex, onSelect, modifier, title, returnFocusTo, key)
}

/** [MutantNavigationDrawer]; [playIn] false stands it open from the first frame (a fixture's still). */
@Composable
internal fun DrawerFrame(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MutantDrawerItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    title: String?,
    returnFocusTo: FocusRequester?,
    key: String,
    playIn: Boolean = true,
    hidden: Boolean = false,
) {
    val reduce = LocalMutantReduceMotion.current
    val screen = readMutantClock()
    // The drawer's own clock: the screen's stands while it shows, and the drawer still moves.
    val own = remember { MutantClock(startMs = screen.nowMs, frozen = screen.frozen) }
    SideEffect { own.paused = reduce }
    // Given its two durations, a close during the slide in (or an open during the slide out) goes on from where the
    // window stands instead of starting over from the other end (B-168).
    val follow = remember { DialogFollow(visible, if (playIn) own.nowMs else Double.NEGATIVE_INFINITY, IN_MS, OUT_MS) }
    val pick = remember { PickHold(selectedIndex) }
    val steps = LocalMutantSteppedMotion.current
    // B-183: dragged shut by a finger or a mouse, and boiling while dragged (the designer's answer 5).
    val drag = remember { PanelDrag() }
    val heat = remember { HeatMachine() }
    drag.over = PANEL_DRAG_OVER_DP * LocalDensity.current.density
    ProvideMutantClock(own) {
        val clock = readMutantClock()
        // B-102: a pick that closes the drawer holds it until the venom run has landed on the new row and lingered, then
        // the drawer goes; a close with no pick goes at once, and under reduce motion everything is at once.
        pick.follow(selectedIndex, clock.nowMs)
        val holding = !visible && !reduce && clock.nowMs < pick.pickedAt + MutantMotion.VENOM_MS + LINGER_MS
        if (holding) clock.frame
        val showing = visible || holding
        follow.follow(showing, clock.nowMs)
        val p = if (reduce) 1.0 else stepped(clock.nowMs - follow.since, if (showing) IN_MS else OUT_MS, steps)
        val shown = (if (showing) p else 1 - p).toFloat()
        val slide = 1 - (1 - shown) * (1 - shown)
        drag.scripted = 1 - slide
        // How far towards closed the window stands, a share of its width: where a drag puts it, or the slide.
        val closed = drag.follow(visible, clock.nowMs, still = reduce || clock.frozen, steps) ?: (1 - slide)
        if (p < 1 || drag.gliding) clock.frame
        SideEffect { if (drag.taken) heat.press(clock.nowMs) else heat.release(clock.nowMs) }
        // A drag's close glides on past the slide out's end; the drawer stays until it is over.
        val gone = !showing && p >= 1 && !drag.closing
        pick.showing = !gone
        DisposableEffect(screen, gone) {
            if (!gone) screen.holds++
            onDispose { if (!gone) screen.holds-- }
        }
        // Focus goes back once the drawer is gone, not as it starts to close: through the exit the scrim still covers the
        // screen, and an Enter pressed then would reach the opener under it and open the drawer again (B-168).
        ReturnFocus(!gone, returnFocusTo)
        if (gone) return@ProvideMutantClock
        val dismissState = rememberUpdatedState(onDismissRequest)
        val dismiss by dismissState
        val focus = remember { FocusRequester() }
        val rows = remember(items.size) { List(items.size) { FocusRequester() } }
        LaunchedEffect(visible) { if (visible) focus.requestFocus() }
        NavigationBackHandler(
            rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = visible,
        ) { dismiss() }
        // It comes in from the screen's start: the left edge, or the right one right to left.
        val from = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1 else -1
        val open = rememberUpdatedState(visible)
        // Towards closed is towards the screen's start.
        val towardsClosed = rememberUpdatedState<(Offset) -> Float> { o -> from * o.x }
        Box(
            modifier
                .fillMaxSize()
                .clipToBounds()
                // Focus stays in here while it is open; Tab past the last row comes round to the first, and Shift+Tab
                // from the drawer itself to the last, as in the dialog (B-207).
                .focusProperties {
                    onExit = {
                        if (visible) {
                            when (requestedFocusDirection) {
                                FocusDirection.Next -> rows.firstOrNull()?.requestFocus()
                                FocusDirection.Previous -> rows.lastOrNull()?.requestFocus()
                                else -> null
                            } ?: cancelFocusChange()
                        }
                    }
                }.focusGroup()
                .focusRequester(focus)
                .onPreviewKeyEvent {
                    if (visible && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        dismiss()
                        true
                    } else {
                        false
                    }
                }
                // The drawer itself holds the keys as it opens; Up or Down there goes to the chosen row (or the first),
                // where on a row they move from row to row (B-168).
                .onKeyEvent {
                    val arrow = it.key == Key.DirectionDown || it.key == Key.DirectionUp
                    if (!visible || it.type != KeyEventType.KeyDown || !arrow) return@onKeyEvent false
                    rows.getOrNull(if (selectedIndex in items.indices) selectedIndex else 0)?.requestFocus()
                    true
                }.focusable()
                // The window and the scrim alike drag it towards the start (B-183).
                .panelDrag(drag, open, dismissState, towardsClosed),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // The scrim is as dark as the window is open, sliding or dragged (the designer's answer 5).
                    .graphicsLayer { alpha = (1 - closed).coerceIn(0f, 1f) }
                    .background(SCRIM)
                    .focusProperties { canFocus = false }
                    .clickable(
                        remember { MutableInteractionSource() },
                        indication = null,
                        enabled = visible,
                    ) { dismiss() },
            )
            // The window floats over the screen, so the screen's keyframe loss does not take its bodies: a loss held
            // mid-run by the drawer would cut its band out of the window for as long as it is open (B-98's rule, B-168).
            CompositionLocalProvider(LocalMutantMoshScene provides null) {
                DrawerWindow(
                    items,
                    selectedIndex,
                    onSelect,
                    title,
                    key,
                    hidden,
                    visible,
                    rows,
                    heat = heat,
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .onSizeChanged { drag.size = it.width.toFloat() }
                            .graphicsLayer { translationX = from * closed * size.width }
                            .semantics {
                                dismiss {
                                    dismiss()
                                    true
                                }
                            },
                )
            }
        }
    }
}

/** The drawer's window: its body run off the start and the top by 10 dp, the title and the rows in it. */
@Composable
internal fun DrawerWindow(
    items: List<MutantDrawerItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    title: String?,
    key: String,
    hidden: Boolean,
    open: Boolean,
    focus: List<FocusRequester>,
    modifier: Modifier,
    heat: HeatMachine? = null,
) {
    val colors = MutantTheme.colors
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val cassette = LocalMutantCassette.current
    val chosen = selectedIndex in items.indices
    val run = remember { VenomRun(selectedIndex, PILL_RADIUS) }
    run.steps = LocalMutantSteppedMotion.current
    val top = TOP + (if (title != null) TITLE + GAP else 0f)
    // The rows stand 22 dp from the body's start: its left edge, or its right one right to left.
    val rowX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) BODY_WIDTH - START - ROW_WIDTH else START
    run.key = "$key-pill"
    run.cassette = cassette
    run.slot =
        { i ->
            doubleArrayOf(rowX.toDouble(), (top + i * (ROW + GAP)).toDouble(), ROW_WIDTH.toDouble(), ROW.toDouble())
        }
    run.follow(selectedIndex, clock.nowMs, still = reduce || clock.frozen || clock.paused || clock.holds > 0)
    val select by rememberUpdatedState(onSelect)
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val code =
        TextStyle(
            fontFamily = fonts?.display,
            fontSize = CODE_SIZE.sp,
            letterSpacing = TRACKING.sp,
            platformStyle = raster,
        ).cssLine(MutantFaceMetrics.Handjet)
    val label =
        TextStyle(
            fontFamily = fonts?.text,
            fontSize = LABEL_SIZE.sp,
            platformStyle = raster,
        ).cssLine(MutantFaceMetrics.Rubik)
    val inkGoo = rememberGraphicsLayer()
    val fillGoo = rememberGraphicsLayer()
    val words = remember { DrawerWordInk() }
    // Where the lime lies at run progress p, as a mask in the body's pixels: the venom's fat shapes through the goo, as
    // the fill recorded them this frame, its thin ones, and the window once it shows, springing about its middle, on
    // the row the run lands on (a run settling on the way to a later pick lands short of it, B-172).
    val slime: DrawScope.(Double, VenomShapes) -> Unit = { p, v ->
        drawLayer(fillGoo)
        scale(density, density, Offset.Zero) {
            for (t in v.thin) drawPath(t.toPath(evenOdd = false), Color.Black)
        }
        val at = run.window
        if (v.full && at in words.tops.indices) {
            val l = words.start.toFloat()
            val t = words.tops[at].toFloat()
            val w = words.rowW.toFloat()
            val h = words.heights[at].toFloat()
            val spring = venomSpring(p, v.vertical)
            scale(spring?.first ?: 1f, spring?.second ?: 1f, Offset(l + w / 2, t + h / 2)) {
                drawRoundRect(Color.Black, Offset(l, t), Size(w, h), CornerRadius(PILL_RADIUS.toFloat() * density))
            }
        }
    }
    Layout(
        content = {
            MutantSurface(
                MutantBody.Bar,
                key,
                // Its own clicks: none reaches the scrim under it. A pointer target only, not a `clickable`: a clickable is
                // a Tab stop and takes focus on a press, and its body shows neither (B-168, the dialog's window).
                Modifier
                    .focusGroup()
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
                fill = colors.surface,
                slip = SLIP,
                shadow = colors.misregister,
                profile = MutantBody.Bar.profile.with(radius = 0.0, marks = false, mosaic = false),
                heatMachine = heat,
            ) {
                Layout(
                    content = {
                        Canvas(Modifier.fillMaxSize()) {
                            val v = run.now(clock, selectedIndex)?.second ?: return@Canvas
                            gooLayer(inkGoo, density)
                            drawVenomInk(
                                v,
                                colors.ink,
                                MutantBody.Pill.profile.outline
                                    .toFloat(),
                                inkGoo,
                                density,
                            )
                        }
                        Box(
                            Modifier.graphicsLayer {
                                val now = run.now(clock, selectedIndex)
                                val v = now?.second
                                alpha = if (v != null && !v.full) 0f else 1f
                                val spring = if (now != null && v!!.full) venomSpring(now.first, v.vertical) else null
                                scaleX = spring?.first ?: 1f
                                scaleY = spring?.second ?: 1f
                                // A run settling on the way to a later pick shows the window where it lands (B-172).
                                val at = run.window
                                translationY =
                                    if (now == null || at == selectedIndex || at !in words.tops.indices ||
                                        selectedIndex !in words.tops.indices
                                    ) {
                                        0f
                                    } else {
                                        (words.tops[at] - words.tops[selectedIndex]).toFloat()
                                    }
                            },
                        ) {
                            if (chosen) {
                                MutantSurface(
                                    MutantBody.Pill,
                                    "$key-pill",
                                    Modifier.fillMaxSize().mutantCleanZone(),
                                    calm = PILL_CALM,
                                    profile = MutantBody.Pill.profile.with(radius = PILL_RADIUS),
                                ) {}
                            }
                        }
                        Canvas(Modifier.fillMaxSize()) {
                            val v = run.now(clock, selectedIndex)?.second ?: return@Canvas
                            gooLayer(fillGoo, density)
                            drawVenomFill(v, colors.primary, fillGoo, density)
                        }
                        if (title != null) {
                            MutantText(
                                title,
                                Modifier.mutantCleanZone(),
                                // On a body boiling amber while dragged, the amber title turns dark (B-183).
                                style = code.copy(color = if (hidden) Color.Transparent else inkOnBody(colors.textOsd)),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        items.forEachIndexed { i, item ->
                            val moves =
                                Modifier.focusRequester(focus[i]).onPreviewKeyEvent {
                                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    val to =
                                        when {
                                            it.key == Key.DirectionDown -> (i + 1) % items.size

                                            it.key == Key.DirectionUp -> (i - 1 + items.size) % items.size

                                            // Shift+Tab on the first row would stop on the drawer itself, which shows
                                            // nothing: round to the last row instead, as the dialog does (B-207).
                                            it.key == Key.Tab && it.isShiftPressed && i == 0 -> items.lastIndex

                                            else -> return@onPreviewKeyEvent false
                                        }
                                    focus[to].requestFocus()
                                    true
                                }
                            DrawerRow(item, i, run, selectedIndex, code, label, hidden, open, moves, words, slime) {
                                select(i)
                            }
                        }
                    },
                ) { measurables, c ->
                    val d = density
                    val w = (BODY_WIDTH * d).roundToInt()
                    val h = c.maxHeight
                    val start = (START * d).roundToInt()
                    val rowW = (ROW_WIDTH * d).roundToInt()
                    val gap = (GAP * d).roundToInt()
                    val ink = measurables[0].measure(Constraints.fixed(w, h))
                    val fill = measurables[2].measure(Constraints.fixed(w, h))
                    val head = if (title != null) measurables[3] else null
                    val titleP = head?.measure(Constraints(maxWidth = rowW, minHeight = (TITLE * d).roundToInt()))
                    val rows =
                        measurables.drop(if (title != null) 4 else 3).map {
                            it.measure(
                                Constraints(minWidth = rowW, maxWidth = rowW, minHeight = (ROW * d).roundToInt()),
                            )
                        }
                    var y = (TOP * d).roundToInt() + (titleP?.let { it.height + gap } ?: 0)
                    val tops = rows.map { r -> y.also { y += r.height + gap } }
                    words.start = if (layoutDirection == LayoutDirection.Rtl) w - start - rowW else start
                    words.rowW = rowW
                    words.tops = tops.toIntArray()
                    words.heights = rows.map { it.height }.toIntArray()
                    val pill =
                        if (chosen) {
                            measurables[1].measure(Constraints.fixed(rowW, rows[selectedIndex].height))
                        } else {
                            measurables[1].measure(Constraints.fixed(0, 0))
                        }
                    layout(w, h) {
                        ink.place(0, 0)
                        if (chosen) pill.placeRelative(start, tops[selectedIndex])
                        fill.place(0, 0)
                        titleP?.placeRelative(start, (TOP * d).roundToInt())
                        rows.forEachIndexed { i, r -> r.placeRelative(start, tops[i]) }
                    }
                }
            }
        },
        modifier,
    ) { measurables, c ->
        // The body runs off the window's start (the screen's, either way) and top by 10 dp and past its foot by as much.
        val off = (RUN_OFF * density).roundToInt()
        val h = if (c.hasBoundedHeight) c.maxHeight else (BODY_MIN * density).roundToInt()
        val body = measurables[0].measure(Constraints.fixed((BODY_WIDTH * density).roundToInt(), h + 2 * off))
        layout(body.width - off, h) { body.placeRelative(-off, -off) }
    }
}

@Composable
private fun DrawerRow(
    item: MutantDrawerItem,
    index: Int,
    run: VenomRun,
    selectedIndex: Int,
    code: TextStyle,
    label: TextStyle,
    hidden: Boolean,
    open: Boolean,
    moves: Modifier,
    words: DrawerWordInk,
    slime: DrawScope.(Double, VenomShapes) -> Unit,
    onPick: () -> Unit,
) {
    val colors = MutantTheme.colors
    val clock = readMutantClock()
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    // The words off the slime: the text's colour, dark on a body boiling amber while dragged (B-183).
    val plain = inkOnBody(colors.text)

    // A lambda, not a reference to a local function: two references to `ink` compare equal whatever selection they
    // captured, so the words' node kept the first one and coloured the rows by the selection it was composed with (B-103).
    // Through a run the words take the colour of the side of the slime the pass draws; at rest the chosen row's are black.
    val ink = {
        when {
            hidden -> Color.Transparent
            words.pass == DrawerWordPass.OnSlime -> colors.onPrimary
            words.pass == DrawerWordPass.OffSlime -> plain
            index == selectedIndex -> colors.onPrimary
            else -> plain
        }
    }
    Row(
        moves
            .pointerInput(index) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        val p = e.changes.firstOrNull() ?: continue
                        if (p.pressed && !p.previousPressed) {
                            run.pressAt(
                                (p.position.x - size.width / 2f) / density,
                                (p.position.y - size.height / 2f) / density,
                            )
                        }
                    }
                }
            }.mutantHand(true)
            // A closing drawer answers no row: through B-102's hold and through the exit a press picks nothing.
            .selectable(index == selectedIndex, source, enabled = open, indication = null, role = Role.Tab) {
                if (index != selectedIndex) onPick()
            }.drawWithContent {
                // Through a run the row is drawn twice: its words black where the slime lies under them, in the text's
                // colour elsewhere, so they turn as the lime reaches them (B-168; the page turns the words on a 340 ms
                // timer, which left pale words on the passing lime and a black one on the dark body, 1.06:1).
                val now = if (hidden) null else run.now(clock, selectedIndex)
                if (now == null || index !in words.tops.indices) return@drawWithContent drawContent()
                val bounds = Rect(Offset.Zero, size)
                val canvas = drawContext.canvas
                for (pass in listOf(DrawerWordPass.OffSlime, DrawerWordPass.OnSlime)) {
                    words.pass = pass
                    canvas.saveLayer(bounds, Paint())
                    drawContent()
                    canvas.saveLayer(
                        bounds,
                        Paint().apply {
                            blendMode = if (pass == DrawerWordPass.OnSlime) BlendMode.DstIn else BlendMode.DstOut
                        },
                    )
                    translate(-words.start.toFloat(), -words.tops[index].toFloat()) { slime(now.first, now.second) }
                    canvas.restore()
                    canvas.restore()
                }
                words.pass = DrawerWordPass.Rest
            }.drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(1f, 1f, size.width / density - 1, size.height / density - 1, colors.focus)
                    }
                }
            }.padding(horizontal = ROW_PADDING.dp),
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MutantText(item.code, Modifier.mutantCleanZone(), style = code, maxLines = 1, softWrap = false, color = ink)
        MutantText(
            item.label,
            Modifier.weight(1f, fill = false).mutantCleanZone(),
            style = label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = ink,
        )
    }
}

/** The scrim over the screen (`rgba(5, 6, 12, .5)`). */
private val SCRIM = Color(0x8005060C)

/** The body (`width: 212px`, run off by `left: -10px; top: -10px`) and the screen's height given none (`308px`). */
private const val BODY_WIDTH = 212
private const val RUN_OFF = 10
private const val BODY_MIN = 308

/** The body's padding (`26px 10px 0 22px`), the column's gap (4), the title's line (28) and a row (`180 × 42`). */
private const val TOP = 26f
private const val START = 22
private const val GAP = 4f
private const val TITLE = 28f
private const val ROW = 42f
private const val ROW_WIDTH = 180

/** A row's padding (`0 12px`) and the gap between code and label (`gap: 12px`). */
private const val ROW_PADDING = 12
private const val ROW_GAP = 12

/** The code and title (Handjet `20px`, tracked 1) and the label (`15px`), sp. */
private const val CODE_SIZE = 20
private const val TRACKING = 1f
private const val LABEL_SIZE = 15

/** The body's slip (`data-slip="3"`); the window (`data-r="8" data-calm=".2"`). */
private const val SLIP = 3.0
private const val PILL_RADIUS = 8.0
private const val PILL_CALM = .2

/**
 * When the drawer took its last pick, and whether it was on screen at the last composition: a pick counts only on a
 * drawer the user could see, so a selection changed elsewhere while it is gone does not bring it back (B-102).
 */
private class PickHold(
    var at: Int,
) {
    var pickedAt = Double.NEGATIVE_INFINITY
    var showing = false

    fun follow(
        index: Int,
        nowMs: Double,
    ) {
        if (index == at) return
        at = index
        if (showing) pickedAt = nowMs
    }
}

/** Which colour the words take in the draw under way: their own at rest, or one side of the slime through a run. */
private enum class DrawerWordPass { Rest, OffSlime, OnSlime }

/** The pass the words' colour reads, set by each row's draw around its passes, and where the rows stand, in pixels. */
private class DrawerWordInk {
    var pass = DrawerWordPass.Rest
    var start = 0
    var rowW = 0
    var tops = IntArray(0)
    var heights = IntArray(0)
}

/** After the venom run lands, how long the drawer stands with the pill on its new row before it goes (D15, B-102). */
private const val LINGER_MS = 160.0

/** In and out (designed here, D15): as the sheet's rise and sink. */
private const val IN_MS = 330.0
private const val OUT_MS = 200.0
