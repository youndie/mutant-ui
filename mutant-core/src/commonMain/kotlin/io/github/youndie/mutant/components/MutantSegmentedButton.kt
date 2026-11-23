package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.VenomFrom
import io.github.youndie.mutant.gen.VenomShapes
import io.github.youndie.mutant.gen.venomShapes
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
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
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A segmented button (v1): a deck's mode switch. The body flows (a `btn` of the second surface, drips of 14); the
 * chosen segment is a clean window, a lime `pill` (r 9, calm 0.2) with its label in black. A new [selectedIndex] moves
 * the window by venom: tentacles shoot from the old window to the new place, the mass flows along them and the new
 * window springs (720 ms at 24 frames a second). A word is black where the lime lies under it and in the text's colour
 * elsewhere, so it turns as the slime reaches it (the page turns the words 340 ms in). Under reduce motion, or a clock
 * that stands, it moves at once. An index outside [options] shows no window and runs no venom; a window shown again
 * arrives at once.
 *
 * A segment is 104 dp where the button is given no width. The whole button is one focus stop, and the arrows choose.
 * A label wider than its segment ends where the segment does, in an ellipsis. Disabled, the body loses its signal, no
 * window shows and the labels grey.
 */
@Composable
public fun MutantSegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    key: String = "segmented",
) {
    SegmentedBody(options, selectedIndex, onSelect, modifier, enabled, key, "$key/pill")
}

/** [MutantSegmentedButton]; [pillKey] is the window's own key (the page's `seg-pill`), [hidden] a bare twin's. */
@Composable
internal fun SegmentedBody(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    key: String,
    pillKey: String,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val n = options.size
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val cassette = LocalMutantCassette.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val chosen = enabled && selectedIndex in options.indices
    // The segment the window stands on, or -1: a disabled button and an index outside the options show none, and the
    // venom never runs to a slot that is not there. A window shown again arrives at once, with no run.
    val shown = if (chosen) selectedIndex else -1
    val run = remember { VenomRun(shown, PILL_RADIUS) }
    run.steps = LocalMutantSteppedMotion.current
    val geo = remember { SegmentGeometry() }
    run.key = pillKey
    run.cassette = cassette
    run.slot = geo::slot
    geo.inset = if (enabled) INSET else 0f
    // A clock that does not move (a still, reduce motion, a held screen) moves the window at once.
    run.follow(shown, clock.nowMs, still = reduce || clock.frozen || clock.paused || clock.holds > 0)
    val select by rememberUpdatedState(onSelect)
    val label =
        TextStyle(
            fontFamily = LocalMutantFonts.current?.text,
            fontSize = LABEL_SIZE.sp,
            fontWeight = FontWeight.Medium,
            platformStyle = LocalMutantTextRaster.current,
        ).cssLine(MutantFaceMetrics.Rubik)
    // The body is the page's 48 dp while a label's line fits its 40 dp; a taller one (font scale 2) grows it, keeping
    // the room around the line (designed here, D15).
    val px = LocalDensity.current
    val lineH =
        with(px) {
            rememberTextMeasurer()
                .measure("Ag", label)
                .size.height
                .toDp()
        }
    val height = maxOf(HEIGHT.dp, lineH + (HEIGHT - CONTENT_LINE).dp)
    val ink = remember { WordInk() }
    val inkGoo = rememberGraphicsLayer()
    val fillGoo = rememberGraphicsLayer()
    MutantSurface(
        MutantBody.Btn,
        key,
        modifier
            .width(DESIGN_CELL.dp * n.coerceAtLeast(1))
            .height(height)
            .selectableGroup()
            .onKeyEvent {
                if (!enabled || n == 0 || it.type != KeyEventType.KeyDown) return@onKeyEvent false
                val to =
                    when (it.key) {
                        Key.DirectionRight, Key.DirectionDown -> (selectedIndex + 1).coerceIn(0, n - 1)
                        Key.DirectionLeft, Key.DirectionUp -> (selectedIndex - 1).coerceIn(0, n - 1)
                        else -> return@onKeyEvent false
                    }
                if (to != selectedIndex) {
                    run.pressAt(0f, 0f)
                    select(to)
                }
                true
            }.focusable(enabled, source),
        fill = colors.surfaceVariant,
        state = if (enabled) BodyState.Rest else BodyState.Disabled,
        focused = focused && keyboard,
        // The page's segmented sets its drips and leaves out the marks; the disabled one sets nothing.
        profile =
            if (enabled) {
                MutantBody.Btn.profile.with(overhang = OVERHANG, dripShare = DRIP_SHARE, marks = false)
            } else {
                MutantBody.Btn.profile
            },
    ) {
        Layout(
            content = {
                // The venom's ink, under the window.
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
                // The window: shown once the venom has filled its place, springing after.
                Box(
                    Modifier.graphicsLayer {
                        val now = if (shown < 0) null else run.now(clock, shown)
                        val v = now?.second
                        alpha = if (v != null && !v.full) 0f else 1f
                        val spring = if (now != null && v!!.full) venomSpring(now.first, v.vertical) else null
                        scaleX = spring?.first ?: 1f
                        scaleY = spring?.second ?: 1f
                        // A run settling on the way to a later pick shows the window where it lands (B-172).
                        translationX = if (now == null) 0f else geo.shift(shown, run.window, density).toFloat()
                    },
                ) {
                    if (chosen) {
                        MutantSurface(
                            MutantBody.Pill,
                            pillKey,
                            // The window is a body inside the button: the engine keeps it in the button's clean zone.
                            Modifier.fillMaxSize().mutantCleanZone(),
                            calm = PILL_CALM,
                            profile = MutantBody.Pill.profile.with(radius = PILL_RADIUS),
                        ) {}
                    }
                }
                // The venom's fill, over the window.
                Canvas(Modifier.fillMaxSize()) {
                    if (shown < 0) return@Canvas
                    val v = run.now(clock, shown)?.second ?: return@Canvas
                    gooLayer(fillGoo, density)
                    drawVenomFill(v, colors.primary, fillGoo, density)
                }
                // The words, over the fill. Through a run each is drawn twice: black where the slime lies under it, in
                // the text's colour elsewhere, so a word turns as the lime reaches it (B-168; the page turns them on a
                // 340 ms timer, which leaves pale words on the passing lime and a black one on the dark body).
                Layout(
                    content = {
                        options.forEachIndexed { i, text ->
                            val on = i == selectedIndex
                            Box(
                                Modifier
                                    .focusProperties { canFocus = false }
                                    .pointerInput(i) {
                                        // Where the press lands, dp from the segment's middle: the venom's mass goes there.
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
                                    }.mutantHand(enabled)
                                    .selectable(
                                        on,
                                        interactionSource = null,
                                        indication = null,
                                        enabled = enabled,
                                        role = Role.Tab,
                                        onClick = { if (!on) select(i) },
                                    ),
                            ) {
                                SegmentLabel(text, label, geo, i, enabled) {
                                    when {
                                        hidden -> Color.Transparent
                                        !enabled -> colors.textDisabled
                                        ink.pass == WordPass.OnSlime -> colors.onPrimary
                                        ink.pass == WordPass.OffSlime -> colors.text
                                        i == shown -> colors.onPrimary
                                        else -> colors.text
                                    }
                                }
                            }
                        }
                    },
                    modifier =
                        Modifier.drawWithContent {
                            val now = if (shown < 0 || hidden) null else run.now(clock, shown)
                            if (now == null) return@drawWithContent drawContent()
                            val (p, v) = now
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
                                drawSlime(p, v, fillGoo, geo, geo.shift(shown, run.window, density))
                                canvas.restore()
                                canvas.restore()
                            }
                            ink.pass = WordPass.Rest
                        },
                ) { words, c ->
                    val d = density
                    val placed =
                        words.mapIndexed { i, m ->
                            val l = ((geo.inset + i * geo.cell) * d).roundToInt()
                            val r = ((geo.inset + (i + 1) * geo.cell) * d).roundToInt()
                            m.measure(Constraints.fixed((r - l).coerceAtLeast(0), c.maxHeight)) to l
                        }
                    layout(c.maxWidth, c.maxHeight) { placed.forEach { (p, l) -> p.place(l, 0) } }
                }
            },
        ) { measurables, c ->
            val w = c.maxWidth
            val h = c.maxHeight
            val d = density
            geo.cell = if (n > 0) (w / d - 2 * geo.inset) / n else 0f
            geo.height = h / d
            val inkLayer = measurables[0].measure(Constraints.fixed(w, h))
            val fill = measurables[2].measure(Constraints.fixed(w, h))
            // The window as the engine places it: the chosen segment's box, in whole pixels.
            val (pillX, pillW) = geo.box(selectedIndex, d)
            val pillTop = (geo.inset * d).roundToInt()
            val pill =
                measurables[1].measure(
                    Constraints.fixed(pillW.coerceAtLeast(0), (h - 2 * pillTop).coerceAtLeast(0)),
                )
            geo.pill = IntRect(pillX, pillTop, pillX + pillW.coerceAtLeast(0), h - pillTop)
            val words = measurables[3].measure(Constraints.fixed(w, h))
            layout(w, h) {
                inkLayer.place(0, 0)
                if (chosen) pill.place(pillX, pillTop)
                fill.place(0, 0)
                words.place(0, 0)
            }
        }
    }
}

/**
 * One segment's label where Chrome draws it. The segment's box is in whole pixels; the grid's `1fr` columns put the
 * label's glyphs at a fraction, centred. Vertically the enabled one stands in its 40 dp line (`line-height: 40px`) 4 dp
 * down, the disabled one in the whole row (`align-items: center`).
 */
@Composable
private fun SegmentLabel(
    text: String,
    style: TextStyle,
    run: SegmentGeometry,
    index: Int,
    enabled: Boolean,
    color: () -> Color,
) {
    var line by remember { mutableFloatStateOf(Float.NaN) }
    MutantText(
        text,
        Modifier
            // The line from the segment's left whatever the direction: the draw below centres it from there.
            .fillMaxWidth()
            .wrapContentWidth(AbsoluteAlignment.Left)
            .mutantCleanZone()
            .drawWithContent {
                val w = line
                if (w.isNaN()) return@drawWithContent drawContent()
                val d = density
                val cellLeft = (run.inset + index * run.cell) * d
                val x = cellLeft - cellLeft.roundToInt() + (run.cell * d - w) / 2
                // Blink floors a line box's half-leading; a grid centres an item at the half pixel and paints it on the
                // next one.
                val y =
                    if (enabled) {
                        floor((LINE * d - size.height) / 2) + run.inset * d
                    } else {
                        floor((run.height * d - size.height) / 2 + .5f)
                    }
                translate(x, y) { this@drawWithContent.drawContent() }
            },
        style = style,
        // A word wider than its segment ends in an ellipsis at the segment's end, so its line fits and is centred.
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        softWrap = false,
        color = color,
        onTextLayout = { line = it.getLineRight(0) - it.getLineLeft(0) },
    )
}

/** The segments' geometry the window moves over, in dp: the inset from the body and the width of a segment. */
private class SegmentGeometry {
    var inset = 0f
    var cell = 0f
    var height = 0f

    /** The window's box in pixels, as laid out. */
    var pill = IntRect.Zero

    /** The window's left and width in pixels for segment [i]: the engine's whole-pixel `offsetLeft`/`offsetWidth`. */
    fun box(
        i: Int,
        density: Float,
    ): Pair<Int, Int> {
        val l = ((inset + i * cell) * density).roundToInt()
        return l to (cell * density).roundToInt()
    }

    /** How far, in pixels, the window's box for segment [to] stands right of the one for segment [from]. */
    fun shift(
        from: Int,
        to: Int,
        density: Float,
    ): Int = if (from == to || from < 0 || to < 0) 0 else box(to, density).first - box(from, density).first

    /** Segment [i]'s slot as the venom reads it: the engine's whole-dp box. */
    fun slot(i: Int): DoubleArray {
        val top = inset.roundToInt().toDouble()
        return doubleArrayOf(
            (inset + i * cell).roundToInt().toDouble(),
            top,
            cell.roundToInt().toDouble(),
            height - 2 * top,
        )
    }
}

/** Which colour the words take in the draw under way: their own at rest, or one side of the slime through a run. */
private enum class WordPass { Rest, OffSlime, OnSlime }

/** The pass the words' colour reads; set by their holder's draw around each pass, read by each word's draw. */
private class WordInk {
    var pass = WordPass.Rest
}

/**
 * Where the lime lies at run progress [p], as a mask: the venom's fat shapes through the goo, as the fill recorded
 * them this frame ([fillGoo]), its thin ones, and the window once it shows, springing about its middle, [shift] pixels
 * right of the chosen segment's box where a run settles on the way to it.
 */
private fun DrawScope.drawSlime(
    p: Double,
    v: VenomShapes,
    fillGoo: GraphicsLayer,
    geo: SegmentGeometry,
    shift: Int,
) {
    drawLayer(fillGoo)
    scale(density, density, Offset.Zero) {
        for (t in v.thin) drawPath(t.toPath(evenOdd = false), Color.Black)
    }
    if (!v.full) return
    val box = geo.pill.translate(shift, 0)
    val spring = venomSpring(p, v.vertical)
    scale(spring?.first ?: 1f, spring?.second ?: 1f, box.center.toOffset()) {
        drawRoundRect(
            Color.Black,
            box.topLeft.toOffset(),
            box.size.toSize(),
            CornerRadius(PILL_RADIUS.toFloat() * density),
        )
    }
}

/** The page's segmented: 48 dp, 4 dp inside the body, a segment's line 40 dp; the label Rubik 14/500. */
private const val HEIGHT = 48
private const val INSET = 4f
private const val LINE = 40f
private const val LABEL_SIZE = 14

/** Rubik 14's own line (Blink's rounded ascent and descent): the room under the 48 dp body is kept around it. */
private const val CONTENT_LINE = 17

/** A segment where the button is given no width: the page's 312 dp over three. */
private const val DESIGN_CELL = 104

/** The body's drips (`data-over="14" data-cf=".4"`). */
private const val OVERHANG = 14.0
private const val DRIP_SHARE = .4

/** The window: `data-r="9" data-calm=".2"`. */
private const val PILL_RADIUS = 9.0
private const val PILL_CALM = .2
