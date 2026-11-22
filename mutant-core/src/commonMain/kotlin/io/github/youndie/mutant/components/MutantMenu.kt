package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.VenomShapes
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.drawVenomFill
import io.github.youndie.mutant.surface.drawVenomInk
import io.github.youndie.mutant.surface.gooLayer
import io.github.youndie.mutant.surface.mutantCleanZone
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
 */
@Composable
public fun MutantDropdown(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: String = "menu",
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        DropdownAnchor(label, "$key-anchor") { expanded = !expanded }
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
 * Up and Down move the keyboard's focus from row to row, wrapping, Enter or Space picks, Escape and a press outside go
 * to [onDismissRequest].
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
    key: String = "menu",
) {
    if (!expanded) return
    val dismiss by rememberUpdatedState(onDismissRequest)
    val dy =
        with(
            androidx.compose.ui.platform.LocalDensity.current,
        ) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
    Popup(
        offset = dy,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        MenuBody(
            items,
            selectedIndex,
            onSelect,
            modifier.onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (it.key != Key.Escape) return@onPreviewKeyEvent false
                dismiss()
                true
            },
            key,
            "$key-pill",
        )
    }
}

/** The anchor: the page's acid chip, its label (Rubik 13/500) and the chevron 6 dp after it, in black. */
@Composable
internal fun DropdownAnchor(
    label: String,
    key: String,
    hidden: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MutantTheme.colors
    val ink = if (hidden) Color.Transparent else colors.onPrimary
    ChipLike(key, onClick) {
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
            focused = focused && keyboard,
            interactionSource = source,
        ) {
            Box(Modifier.heightIn(min = ANCHOR_HEIGHT.dp), contentAlignment = Alignment.CenterStart) { content() }
        }
    }
}

/** The menu's body and rows, drawn in place; [hidden] draws its words and mark transparent for a bare twin. */
@Composable
internal fun MenuBody(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    key: String,
    pillKey: String,
    hidden: Boolean = false,
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
    val ink = remember { WordInk() }
    // Where the lime lies at run progress p, as a mask in the body's pixels: the venom's fat shapes through the goo, as
    // the fill recorded them this frame, its thin ones, and the window once it shows, springing about its middle.
    val slime: DrawScope.(Double, VenomShapes) -> Unit = { p, v ->
        drawLayer(fillGoo)
        scale(density, density, Offset.Zero) {
            for (t in v.thin) drawPath(t.toPath(evenOdd = false), Color.Black)
        }
        if (v.full && shown in ink.tops.indices) {
            val l = ink.inset.toFloat()
            val t = ink.tops[shown].toFloat()
            val w = ink.rowW.toFloat()
            val h = ink.heights[shown].toFloat()
            val spring = venomSpring(p, v.vertical)
            scale(spring?.first ?: 1f, spring?.second ?: 1f, Offset(l + w / 2, t + h / 2)) {
                drawRoundRect(Color.Black, Offset(l, t), Size(w, h), CornerRadius(PILL_RADIUS.toFloat() * density))
            }
        }
    }
    val focus = remember(items.size) { List(items.size) { FocusRequester() } }
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
                    },
                ) {
                    if (chosen) {
                        MutantSurface(
                            MutantBody.Pill,
                            pillKey,
                            Modifier.fillMaxSize().mutantCleanZone(),
                            calm = PILL_CALM,
                            profile = MutantBody.Pill.profile.with(radius = PILL_RADIUS),
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
                    MenuRow(text, i, run, shown, word, hidden, moves, ink, slime) { select(i) }
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
    ink: WordInk,
    slime: DrawScope.(Double, VenomShapes) -> Unit,
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
            .selectable(index == selectedIndex, source, indication = null, role = Role.DropdownList) {
                if (index != selectedIndex) onPick()
            }.drawWithContent {
                // Through a run the row is drawn twice: its word black where the slime lies under it, in the text's
                // colour elsewhere, so it turns as the lime reaches it (B-168; the page turns the words on a 340 ms
                // timer, which leaves pale words on the passing lime and a black one on the dark body).
                val now = if (selectedIndex < 0 || hidden) null else run.now(clock, selectedIndex)
                if (now == null || index !in ink.tops.indices) return@drawWithContent drawContent()
                val bounds = Rect(Offset.Zero, size)
                val canvas = drawContext.canvas
                for (pass in listOf(WordPass.OffSlime, WordPass.OnSlime)) {
                    ink.pass = pass
                    canvas.saveLayer(bounds, Paint())
                    drawContent()
                    canvas.saveLayer(
                        bounds,
                        Paint().apply {
                            blendMode = if (pass == WordPass.OnSlime) BlendMode.DstIn else BlendMode.DstOut
                        },
                    )
                    translate(-ink.inset.toFloat(), -ink.tops[index].toFloat()) { slime(now.first, now.second) }
                    canvas.restore()
                    canvas.restore()
                }
                ink.pass = WordPass.Rest
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
            Modifier.mutantCleanZone(),
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = {
                when {
                    hidden -> Color.Transparent
                    ink.pass == WordPass.OnSlime -> colors.onPrimary
                    ink.pass == WordPass.OffSlime -> colors.text
                    index == selectedIndex -> colors.onPrimary
                    else -> colors.text
                }
            },
        )
    }
}

/** Which colour the words take in the draw under way: their own at rest, or one side of the slime through a run. */
private enum class WordPass { Rest, OffSlime, OnSlime }

/** The pass the words' colour reads, set by each row's draw around its passes, and where the rows stand, in pixels. */
private class WordInk {
    var pass = WordPass.Rest
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
