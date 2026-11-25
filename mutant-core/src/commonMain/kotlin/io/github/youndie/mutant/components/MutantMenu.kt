package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.VenomShapes
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.HeatMachine
import io.github.youndie.mutant.surface.LocalCleanZone
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.drawVenomFill
import io.github.youndie.mutant.surface.drawVenomInk
import io.github.youndie.mutant.surface.gooLayer
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.mutantPlaceKey
import io.github.youndie.mutant.surface.mutantTapeOver
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A dropdown: the acid anchor chip with its [label] and a chevron (body `chip`, 32 dp), and under it, 18 dp below,
 * the [MutantMenu] of [items] while it is open. A press on the anchor opens and closes it; a press on an item hands it
 * to [onSelect] and the window flows to it by venom, the menu staying open (the owner's choice, B-92: it closed once
 * the venom arrived). A press outside or Escape closes it. Opening and closing are at once (designed here, D15).
 *
 * [key] seeds its body: by default its place in the composition and its body type (B-206), so two unkeyed ones are
 * two bodies and one keeps its body whatever its words or state; in a list, give it from the item's id.
 */
@Composable
public fun MutantDropdown(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: String = mutantPlaceKey("menu"),
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        DropdownAnchor(label, "$key-anchor", open = expanded) { expanded = !expanded }
        MutantMenu(
            expanded,
            { expanded = false },
            items,
            selectedIndex,
            onSelect,
            key = key,
        )
    }
}

/**
 * A menu: a column of the on-screen menu (body `btn`, 216 dp, the surface's fill, radius 8, drips to 18 dp) in a popup
 * standing [offset] from its parent's start, its [items] 36 dp tall in Handjet 20, tracked 1, each a step further in
 * than the one above (30, 38, 46, 54 dp, the ladder repeating every four). The [selectedIndex] stands in a clean
 * window (body `pill`, radius 6) with a play mark, its word in black; a new one is reached by venom, as the segmented
 * button's (B-31). Through a run a word is black where the lime lies under it and in the text's colour elsewhere, so it
 * turns as the slime reaches it (the page turns the words 340 ms in). An index outside [items] shows no window and runs
 * no venom; a window shown again arrives at once.
 * A row is its whole target, 36 dp tall: an owner-named exception to the 48 dp rule (research D19).
 * The chosen row (or the first) takes the keys as the menu opens. Up and Down move the keyboard's focus from row to
 * row, wrapping, and the column scrolls to the row they reach; Enter or Space picks, Escape and a press outside go to
 * [onDismissRequest]. A press, Enter or Space on the chosen row hands it to [onSelect] too, so a caller that closes its
 * menu on a pick is closed by it; nothing pours, and the window gets a heat impulse of 0.8 (the designer's answer 39,
 * B-201; the page's menu ignored the press).
 *
 * Where the menu has no room under its anchor it opens above it, 18 dp clear of it for its drips, never over it; one
 * taller than the window stands 16 dp from its top and 34 dp from its foot (16 and the drips' 18), and its rows scroll
 * (the designer's answer 28, B-201). Where it fits neither under nor above, it keeps to those margins on the side with
 * more room. Across, it stands [offset] from its parent's start, moved in to fit the window.
 *
 * [key] seeds its body: by default its place in the composition and its body type (B-206), so two unkeyed ones are
 * two bodies and one keeps its body whatever its words or state; in a list, give it from the item's id.
 */
@Composable
public fun MutantMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, MENU_TOP.dp),
    key: String = mutantPlaceKey("menu"),
) {
    if (!expanded) return
    val dismiss by rememberUpdatedState(onDismissRequest)
    val dy =
        with(
            androidx.compose.ui.platform.LocalDensity.current,
        ) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val placement =
        remember(dy, density) {
            with(density) {
                MenuPlacement(dy, OVERHANG.dp.roundToPx(), MENU_EDGE_TOP.dp.roundToPx(), MENU_EDGE_FOOT.dp.roundToPx())
            }
        }
    Popup(
        popupPositionProvider = placement,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        MenuBody(
            items,
            selectedIndex,
            onSelect,
            // B-197: the window's Tape over the menu, in register with it (the designer's answer 22).
            Modifier.mutantTapeOver(MENU_TAPE_REACH.dp).then(modifier).menuWithinWindow().onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (it.key != Key.Escape) return@onPreviewKeyEvent false
                dismiss()
                true
            },
            key,
            "$key-pill",
            focusOnOpen = true,
            scrolls = true,
        )
    }
}

/** The anchor: the page's acid chip, its label (Rubik 13/500) and the chevron 6 dp after it, in black. */
@Composable
internal fun DropdownAnchor(
    label: String,
    key: String,
    hidden: Boolean = false,
    open: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MutantTheme.colors
    val ink = if (hidden) Color.Transparent else colors.onPrimary
    ChipLike(key, open, onClick) {
        Row(
            Modifier.padding(horizontal = ANCHOR_PADDING.dp),
            horizontalArrangement = Arrangement.spacedBy(ANCHOR_GAP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MutantText(
                label,
                Modifier.mutantCleanZone(),
                style = buttonLabelStyle(ANCHOR_SIZE, ink),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            MutantIcon(
                remember { MutantIcons.icon("Chevron", CHEVRON, CHEVRON_LINE) },
                null,
                Modifier.mutantCleanZone(),
                tint = ink,
                size = CHEVRON_SIZE.dp,
            )
        }
    }
}

/** The anchor's acid chip body, 32 dp tall in a 48 dp touch box, the whole box its target. */
@Composable
private fun ChipLike(
    key: String,
    open: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = MutantTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        Modifier
            .heightIn(min = ANCHOR_TOUCH.dp)
            .mutantHand(true)
            .selectable(false, source, indication = null, role = Role.DropdownList, onClick = onClick),
        contentAlignment = Alignment.TopStart,
    ) {
        MutantSurface(
            MutantBody.Chip,
            key,
            Modifier.heightIn(min = ANCHOR_HEIGHT.dp),
            fill = colors.primary,
            // While its menu is open the keys are the menu's, and only the row that takes them shows it (B-168).
            focused = focused && keyboard && !open,
            interactionSource = source,
        ) {
            Box(Modifier.heightIn(min = ANCHOR_HEIGHT.dp), contentAlignment = Alignment.CenterStart) { content() }
        }
    }
}

/**
 * The menu's body and rows, drawn in place; [hidden] draws its words and mark transparent for a bare twin. [scrolls]
 * puts the rows in a scrolling column, for a body that [modifier] may hold shorter than its rows (the popup's, B-201).
 * [plate] is the window's heat, which a press on the chosen row kicks.
 */
@Composable
internal fun MenuBody(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    key: String,
    pillKey: String,
    hidden: Boolean = false,
    focusOnOpen: Boolean = false,
    scrolls: Boolean = false,
    plate: HeatMachine = remember { HeatMachine() },
) {
    val colors = MutantTheme.colors
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val cassette = LocalMutantCassette.current
    val chosen = selectedIndex in items.indices
    // The row the window stands on, or none: an index outside the rows shows no window and runs no venom, and a window
    // shown again arrives at once (B-168, as the segmented button's).
    val shown = if (chosen) selectedIndex else -1
    val run = remember { VenomRun(shown, PILL_RADIUS) }
    run.steps = LocalMutantSteppedMotion.current
    run.key = pillKey
    run.cassette = cassette
    run.slot =
        { i -> doubleArrayOf(INSET.toDouble(), (INSET + i * ROW).toDouble(), ROW_WIDTH.toDouble(), ROW.toDouble()) }
    run.follow(shown, clock.nowMs, still = reduce || clock.frozen || clock.paused || clock.holds > 0)
    val select by rememberUpdatedState(onSelect)
    val word =
        TextStyle(
            fontFamily = LocalMutantFonts.current?.display,
            fontSize = WORD_SIZE.sp,
            letterSpacing = WORD_TRACKING.sp,
            platformStyle = LocalMutantTextRaster.current,
            // The row's `line-height: 36px`: Blink floors the half-leading, so the word stands where it puts it.
        ).cssLine(MutantFaceMetrics.Handjet, ROW.toFloat() / WORD_SIZE)
    val inkGoo = rememberGraphicsLayer()
    val fillGoo = rememberGraphicsLayer()
    val ink = remember { MenuWordInk() }
    // Where the lime lies at run progress p, as a mask in the body's pixels: the venom's fat shapes through the goo, as
    // the fill recorded them this frame, its thin ones, and the window once it shows, springing about its middle, on
    // the row the run lands on (a run settling on the way to a later pick lands short of it, B-172).
    val slime: DrawScope.(Double, VenomShapes) -> Unit = { p, v ->
        drawLayer(fillGoo)
        scale(density, density, Offset.Zero) {
            for (t in v.thin) drawPath(t.toPath(evenOdd = false), Color.Black)
        }
        val at = run.window
        if (v.full && at in ink.tops.indices) {
            val l = ink.inset.toFloat()
            val t = ink.tops[at].toFloat()
            val w = ink.rowW.toFloat()
            val h = ink.heights[at].toFloat()
            val spring = venomSpring(p, v.vertical)
            scale(spring?.first ?: 1f, spring?.second ?: 1f, Offset(l + w / 2, t + h / 2)) {
                drawRoundRect(Color.Black, Offset(l, t), Size(w, h), CornerRadius(PILL_RADIUS.toFloat() * density))
            }
        }
    }
    val focus = remember(items.size) { List(items.size) { FocusRequester() } }
    if (focusOnOpen) {
        // The popup takes the keys as it opens; its chosen row (or its first) takes them from it, so the arrows,
        // Enter and Escape answer from the start rather than after a Tab (B-168).
        LaunchedEffect(Unit) {
            withFrameNanos { }
            focus.getOrNull(if (chosen) selectedIndex else 0)?.requestFocus()
        }
    }
    val scroll = if (scrolls) rememberScrollState() else null
    val column: @Composable () -> Unit = {
        Layout(
            content = {
                Canvas(Modifier.fillMaxSize()) {
                    if (shown < 0) return@Canvas
                    val v = run.now(clock, shown)?.second ?: return@Canvas
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
                        val now = if (shown < 0) null else run.now(clock, shown)
                        val v = now?.second
                        alpha = if (v != null && !v.full) 0f else 1f
                        val spring = if (now != null && v!!.full) venomSpring(now.first, v.vertical) else null
                        scaleX = spring?.first ?: 1f
                        scaleY = spring?.second ?: 1f
                        // A run settling on the way to a later pick shows the window where it lands (B-172).
                        val at = run.window
                        translationY =
                            if (now == null || at == shown || at !in ink.tops.indices || shown !in ink.tops.indices) {
                                0f
                            } else {
                                (ink.tops[at] - ink.tops[shown]).toFloat()
                            }
                    },
                ) {
                    if (chosen) {
                        MutantSurface(
                            MutantBody.Pill,
                            pillKey,
                            Modifier.fillMaxSize().mutantCleanZone(),
                            calm = PILL_CALM,
                            profile = MutantBody.Pill.profile.with(radius = PILL_RADIUS),
                            heatMachine = plate,
                        ) {
                            Box(
                                Modifier.fillMaxSize().padding(start = MARK_START.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                MutantIcon(
                                    remember { playMark() },
                                    null,
                                    Modifier.mutantCleanZone(),
                                    tint = if (hidden) Color.Transparent else colors.onPrimary,
                                    size = MARK.dp,
                                )
                            }
                        }
                    }
                }
                Canvas(Modifier.fillMaxSize()) {
                    if (shown < 0) return@Canvas
                    val v = run.now(clock, shown)?.second ?: return@Canvas
                    gooLayer(fillGoo, density)
                    drawVenomFill(v, colors.primary, fillGoo, density)
                }
                items.forEachIndexed { i, text ->
                    // Up and Down move the keyboard's focus from row to row, wrapping at the ends.
                    val moves =
                        Modifier.focusRequester(focus[i]).onPreviewKeyEvent {
                            if (it.type != KeyEventType.KeyDown || items.isEmpty()) return@onPreviewKeyEvent false
                            val to =
                                when (it.key) {
                                    Key.DirectionDown -> (i + 1) % items.size
                                    Key.DirectionUp -> (i - 1 + items.size) % items.size
                                    else -> return@onPreviewKeyEvent false
                                }
                            focus[to].requestFocus()
                            true
                        }
                    MenuRow(text, i, run, shown, word, hidden, moves, ink, slime, scroll) {
                        // The chosen row answers too, with no pour: its window takes a heat impulse (B-201).
                        if (i == shown) plate.impulse(clock.nowMs, MENU_PLATE_IMPULSE)
                        select(i)
                    }
                }
            },
        ) { measurables, c ->
            val d = density
            val w = (WIDTH * d).roundToInt()
            val inset = (INSET * d).roundToInt()
            val rowW = (ROW_WIDTH * d).roundToInt()
            val rows =
                measurables.drop(3).map { m ->
                    m.measure(
                        Constraints(
                            minWidth = rowW,
                            maxWidth = rowW,
                            minHeight =
                                (
                                    ROW *
                                        d
                                ).roundToInt(),
                        ),
                    )
                }
            val h = inset * 2 + rows.sumOf { it.height }
            val inkLayer = measurables[0].measure(Constraints.fixed(w, h))
            val fill = measurables[2].measure(Constraints.fixed(w, h))
            var top = inset
            val tops = rows.map { r -> top.also { top += r.height } }
            ink.inset = inset
            ink.rowW = rowW
            ink.tops = tops.toIntArray()
            ink.heights = rows.map { it.height }.toIntArray()
            val pill =
                if (chosen) {
                    measurables[1].measure(Constraints.fixed(rowW, rows[selectedIndex].height))
                } else {
                    measurables[1].measure(Constraints.fixed(0, 0))
                }
            layout(w, h) {
                inkLayer.place(0, 0)
                if (chosen) pill.place(inset, tops[selectedIndex])
                fill.place(0, 0)
                rows.forEachIndexed { i, r -> r.place(inset, tops[i]) }
            }
        }
    }
    MutantSurface(
        MutantBody.Btn,
        key,
        modifier.width(WIDTH.dp).selectableGroup(),
        fill = colors.surface,
        profile =
            MutantBody.Btn.profile.with(
                radius = RADIUS,
                overhang = OVERHANG,
                dripShare = DRIP_SHARE,
                marks = false,
            ),
    ) {
        if (scroll != null) Box(Modifier.verticalScroll(scroll)) { column() } else column()
    }
}

@Composable
private fun MenuRow(
    text: String,
    index: Int,
    run: VenomRun,
    selectedIndex: Int,
    style: TextStyle,
    hidden: Boolean,
    moves: Modifier,
    ink: MenuWordInk,
    slime: DrawScope.(Double, VenomShapes) -> Unit,
    scroll: ScrollState?,
    onPick: () -> Unit,
) {
    val colors = MutantTheme.colors
    val clock = readMutantClock()
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        moves
            .pointerInput(index) {
                // Where the press lands, dp from the row's middle: the venom's mass goes there.
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
            .selectable(index == selectedIndex, source, indication = null, role = Role.DropdownList, onClick = onPick)
            .drawWithContent {
                // Through a run the row is drawn twice: its word black where the slime lies under it, in the text's
                // colour elsewhere, so it turns as the lime reaches it (B-168; the page turns the words on a 340 ms
                // timer, which leaves pale words on the passing lime and a black one on the dark body).
                val now = if (selectedIndex < 0 || hidden) null else run.now(clock, selectedIndex)
                if (now == null || index !in ink.tops.indices) return@drawWithContent drawContent()
                val bounds = Rect(Offset.Zero, size)
                val canvas = drawContext.canvas
                for (pass in listOf(MenuWordPass.OffSlime, MenuWordPass.OnSlime)) {
                    ink.pass = pass
                    canvas.saveLayer(bounds, Paint())
                    drawContent()
                    canvas.saveLayer(
                        bounds,
                        Paint().apply {
                            blendMode = if (pass == MenuWordPass.OnSlime) BlendMode.DstIn else BlendMode.DstOut
                        },
                    )
                    translate(-ink.inset.toFloat(), -ink.tops[index].toFloat()) { slime(now.first, now.second) }
                    canvas.restore()
                    canvas.restore()
                }
                ink.pass = MenuWordPass.Rest
            }.drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(1f, 1f, size.width / density - 1, size.height / density - 1, colors.focus)
                    }
                }
            }.padding(start = (STEP_START + STEP * (index % STEPS)).dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        MutantText(
            text,
            Modifier.menuCleanZone(scroll),
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = {
                when {
                    hidden -> Color.Transparent
                    ink.pass == MenuWordPass.OnSlime -> colors.onPrimary
                    ink.pass == MenuWordPass.OffSlime -> colors.text
                    index == selectedIndex -> colors.onPrimary
                    else -> colors.text
                }
            },
        )
    }
}

/**
 * A word's clean zone. In a scrolling column it is where the word stands with the column unscrolled, cut to the body:
 * the body is built for its first screenful of words and is not rebuilt as the rows scroll under it (B-201). Unscrolled
 * and inside the body, which is every menu that fits, that is the word's own bounds, as [mutantCleanZone] marks them.
 */
private fun Modifier.menuCleanZone(scroll: ScrollState?): Modifier =
    if (scroll == null) {
        mutantCleanZone()
    } else {
        composed {
            val registry = LocalCleanZone.current ?: return@composed this
            val id = remember { Any() }
            androidx.compose.runtime.DisposableEffect(registry) { onDispose { registry.bounds.remove(id) } }
            onGloballyPositioned { c ->
                val s = registry.surface ?: return@onGloballyPositioned
                if (!s.isAttached || !c.isAttached) return@onGloballyPositioned
                val at = s.localBoundingBoxOf(c, clipBounds = false).translate(0f, scroll.value.toFloat())
                val cut = at.intersect(Rect(0f, 0f, s.size.width.toFloat(), s.size.height.toFloat()))
                if (cut.isEmpty) {
                    registry.bounds.remove(id)
                } else if (registry.bounds[id] != cut) {
                    registry.bounds[id] = cut
                }
            }
        }
    }

/**
 * Holds the popup's body to the window less its margins, 16 dp at the top and 34 dp at the foot (the designer's answer
 * 28, B-201): a taller menu scrolls its rows.
 */
private fun Modifier.menuWithinWindow(): Modifier =
    layout { m, c ->
        val room =
            if (c.hasBoundedHeight) {
                (c.maxHeight - (MENU_EDGE_TOP + MENU_EDGE_FOOT).dp.roundToPx()).coerceAtLeast(c.minHeight)
            } else {
                Constraints.Infinity
            }
        val p = m.measure(c.copy(maxHeight = room))
        layout(p.width, p.height) { p.place(0, 0) }
    }

/**
 * Where the menu's popup stands (the designer's answer 28, B-201): [offset] under its anchor's top while it fits above
 * the window's foot margin; else on the side with more room, above its anchor [gap] clear of it (the drips hang there),
 * held inside both margins: a menu that fits above stands there whole. Across, [offset] from the anchor's start,
 * mirrored right to left, moved in to fit the window.
 */
private class MenuPlacement(
    private val offset: IntOffset,
    private val gap: Int,
    private val top: Int,
    private val foot: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val w = popupContentSize.width
        val h = popupContentSize.height
        val start =
            if (layoutDirection == LayoutDirection.Ltr) {
                anchorBounds.left + offset.x
            } else {
                anchorBounds.right - w - offset.x
            }
        val x = start.coerceIn(0, maxOf(0, windowSize.width - w))
        val below = anchorBounds.top + offset.y
        val above = anchorBounds.top - gap - h
        val floor = windowSize.height - foot
        val y =
            if (below + h <= floor) {
                below
            } else {
                // Above where it has more room there than under, as it has whenever it fits there; inside the margins.
                val up = anchorBounds.top - gap - top > floor - below
                (if (up) above else below).coerceIn(top, maxOf(top, floor - h))
            }
        return IntOffset(x, y)
    }
}

/** Which colour the words take in the draw under way: their own at rest, or one side of the slime through a run. */
private enum class MenuWordPass { Rest, OffSlime, OnSlime }

/** The pass the words' colour reads, set by each row's draw around its passes, and where the rows stand, in pixels. */
private class MenuWordInk {
    var pass = MenuWordPass.Rest
    var inset = 0
    var rowW = 0
    var tops = IntArray(0)
    var heights = IntArray(0)
}

/** The window's play mark: a filled triangle (`fill="currentColor"`), not one of the kit's stroked icons. */
private fun playMark(): androidx.compose.ui.graphics.vector.ImageVector =
    androidx.compose.ui.graphics.vector.ImageVector
        .Builder("Mutant.MenuPlay", 24.dp, 24.dp, 24f, 24f)
        .addPath(
            androidx.compose.ui.graphics.vector
                .addPathNodes(PLAY),
            fill =
                androidx.compose.ui.graphics
                    .SolidColor(Color.Black),
        ).build()

/** The menu (`width: 216px; padding: 6px`), its rows (`204 × 36`) and its place under the anchor (`top: 50px`). */
private const val WIDTH = 216
private const val INSET = 6
private const val ROW = 36
private const val ROW_WIDTH = 204
private const val MENU_TOP = 50

/** B-201, the designer's answer 28: the window's margins round a popup menu, the foot's holding its 18 dp drips. */
private const val MENU_EDGE_TOP = 16
private val MENU_EDGE_FOOT = MENU_EDGE_TOP + OVERHANG.toInt()

/** B-201, the designer's answer 39: the heat a press on the chosen row gives its window. */
private const val MENU_PLATE_IMPULSE = .8

/** The ladder: the first word 30 dp in, each next 8 dp further (`padding-left: 30px … 54px`), repeating every four. */
private const val STEP_START = 30
private const val STEP = 8
private const val STEPS = 4

/** The words: Handjet `20px`, tracked 1. */
private const val WORD_SIZE = 20
private const val WORD_TRACKING = 1f

/** The body: `data-r="8" data-over="18" data-cf=".5" data-marks="0"`. */
private const val RADIUS = 8.0
private const val OVERHANG = 18.0

/** How far the menu draws past its box, dp, for the Tape over it (B-197): drips of 18, the 4 dp shadow and the outline. */
private const val MENU_TAPE_REACH = 28
private const val DRIP_SHARE = .5

/** The window: `data-r="6" data-calm=".2"`; its play mark 14 dp, 8 dp in (`padding-left: 8px`). */
private const val PILL_RADIUS = 6.0
private const val PILL_CALM = .2
private const val MARK = 14
private const val MARK_START = 8
private const val PLAY = "M7 4l13 8-13 8z"

/** The anchor: 32 dp in a 48 dp target, `padding: 0 14px; gap: 6px`, Rubik 13/500, the chevron 16 dp stroked 3. */
private const val ANCHOR_HEIGHT = 32
private const val ANCHOR_TOUCH = 48
private const val ANCHOR_PADDING = 14
private const val ANCHOR_GAP = 6
private const val ANCHOR_SIZE = 13
private const val CHEVRON_SIZE = 16
private const val CHEVRON_LINE = 3f
private const val CHEVRON = "M6 9l6 6 6-6"
