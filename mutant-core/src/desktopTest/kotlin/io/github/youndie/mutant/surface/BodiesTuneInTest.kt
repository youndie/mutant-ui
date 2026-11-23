package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchResultLoading
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-179, the designer's answer 1: a body tunes in as it appears — a 16 dp mosaic over its slot, an 8 dp mosaic, the
 * finished body, 320 ms in three steps, its words only on the third — and steps out as it leaves, the 8 dp mosaic, then
 * nothing, by 160 ms; under reduce motion it shows finished at once and leaves at once; the badge switches on whole.
 *
 * The test clock moves in 16 ms frames from the frame the change is composed in (0 ms), so the AC's 0, 107, 213 and 320
 * ms are read at 0 and 96 (the 16 dp mosaic), 112 and 208 (the 8 dp one), 224 and 320 (finished). Every reading counts
 * pixels over an area with margins far from its bound, never a few edge pixels: what CI's rasteriser may move.
 */
@OptIn(ExperimentalTestApi::class)
class BodiesTuneInTest {
    private class Case(
        val name: String,
        /** The words whose unmerged nodes bound the glyphs. */
        val words: List<String>,
        /** The colours the words are drawn in. */
        val inks: (MutantColors) -> List<Color>,
        /** One body: its mosaic's blocks are read on the grid its own picture starts at. */
        val single: Boolean,
        /** The rows of the stage read, dp from its top: the tooltip's plate above its anchor, not the anchor. */
        val rows: Int = Int.MAX_VALUE,
        /** Whether it steps out when hidden; a hand-over brings back what it replaced in one frame instead. */
        val leaves: Boolean = true,
        /** Frames between the change and the component's own (the tooltip follows a focus moved by an effect). */
        val lag: Int = 0,
        /** When it has left the composition, ms: the snackbar's ride down (200 ms) outlasts its 160 ms of steps. */
        val goneBy: Int = 176,
        val height: Int = 160,
        val content: @Composable (on: Boolean) -> Unit,
    )

    private val card =
        Case("card", listOf("Ночной эфир", "Сторона A"), { listOf(it.text, it.textSecondary) }, single = true) { on ->
            MutantAppear(on) {
                MutantCard(Modifier.width(300.dp), key = "c1") {
                    MutantCardTitle("Ночной эфир")
                    MutantCardSubtitle("Сторона A")
                }
            }
        }

    /** The cocoon hands over to its card: the cocoon goes in one frame, the card tunes in. */
    private val handover =
        Case("cocoon → card", listOf("Ночной эфир"), { listOf(it.text) }, single = true, leaves = false) { on ->
            if (on) {
                MutantAppear { MutantCard(Modifier.width(300.dp), key = "c2") { MutantCardTitle("Ночной эфир") } }
            } else {
                MutantCocoon(Modifier.size(300.dp, 80.dp))
            }
        }

    private val empty =
        Case("empty state", listOf("Пусто"), { listOf(it.text) }, single = false, height = 420) { on ->
            MutantAppear(on) { MutantEmptyState(MutantIllustrationKind.Cassette, "Пусто") }
        }

    /** The results take the loading row's place. */
    private val search =
        Case(
            "search result",
            listOf("Ночь на вторник"),
            { listOf(it.text, it.textAccent) },
            single = true,
            leaves = false,
        ) { on ->
            if (on) {
                MutantAppear { MutantSearchResult("Ночь на вторник", "ночь", leadingIcon = MutantIcons.Schedule) }
            } else {
                MutantSearchResultLoading()
            }
        }

    private val tooltip =
        Case("tooltip", listOf("ЗАПИСЬ"), { listOf(it.onNoSignal) }, single = true, rows = 88, lag = 1) { on ->
            val f = remember { FocusRequester() }
            val focus = LocalFocusManager.current
            LaunchedEffect(on) { if (on) f.requestFocus() else focus.clearFocus() }
            Box(Modifier.padding(top = 80.dp, start = 100.dp)) {
                MutantTooltipBox("ЗАПИСЬ") { MutantIconButton(MutantIcons.Add, "add", {}, Modifier.focusRequester(f)) }
            }
        }

    private val snackbar =
        Case("snackbar", listOf("Добавлено"), { listOf(it.onNoSignal) }, single = true, goneBy = 208) { on ->
            MutantSnackbar("Добавлено", Modifier.width(300.dp), visible = on)
        }

    private class Shot(
        val ms: Int,
        val px: PixelMap,
        val glyphs: Int,
        val composed: Boolean,
    )

    private var on by mutableStateOf(false)

    /** Every frame from the change to [frames] × 16 ms later, after showing and after hiding. */
    private fun ComposeUiTest.strip(
        case: Case,
        colors: MutantColors,
        d: Float,
        reduce: Boolean = false,
        frames: Int = 22,
    ): Pair<List<Shot>, List<Shot>> {
        mainClock.autoAdvance = false
        on = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors = colors, reduceMotion = reduce) {
                    Box(Modifier.testTag("stage").size(360.dp, case.height.dp).background(colors.background)) {
                        Box(Modifier.padding(16.dp)) { case.content(on) }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1000)

        var seen = emptyList<Rect>()

        fun shot(ms: Int): Shot {
            // The stage stands at the window's origin, and its picture takes in what popups draw over it.
            val px = onNodeWithTag("stage").captureToImage().toPixelMap().crop((case.rows * d).toInt())
            val rects =
                case.words.flatMap { w ->
                    onAllNodesWithText(w, substring = true, useUnmergedTree = true).fetchSemanticsNodes().map {
                        it.boundsInWindow
                    }
                }
            // A leaving body answers no reader: its words are looked for where they stood.
            if (rects.isNotEmpty()) seen = rects
            return Shot(ms, px, glyphs(px, seen, case.inks(colors)), rects.isNotEmpty())
        }
        on = true
        val shown = List(frames + case.lag) { i -> mainClock.advanceTimeByFrame().let { shot((i - case.lag) * 16) } }
        on = false
        val left = List(14 + case.lag) { i -> mainClock.advanceTimeByFrame().let { shot((i - case.lag) * 16) } }
        return shown.drop(case.lag) to left.drop(case.lag)
    }

    /** The top [rows] rows of this picture. */
    private fun PixelMap.crop(rows: Int): PixelMap {
        if (rows >= height) return this
        val w = width
        val buffer = IntArray(w * rows) { i -> this[i % w, i / w].toArgb() }
        return PixelMap(buffer, w, rows, 0, w)
    }

    /** Whether the pixel at [x], [y] differs from one beside it: an edge, which a fractional offset blends. */
    private fun PixelMap.edge(
        x: Int,
        y: Int,
    ): Boolean {
        val c = this[x, y]
        return (x > 0 && !near(c, this[x - 1, y], .02f)) || (x < width - 1 && !near(c, this[x + 1, y], .02f)) ||
            (y > 0 && !near(c, this[x, y - 1], .02f)) || (y < height - 1 && !near(c, this[x, y + 1], .02f))
    }

    /** Pixels inside [rects] in one of [inks]: the words' glyphs as drawn. */
    private fun glyphs(
        px: PixelMap,
        rects: List<Rect>,
        inks: List<Color>,
    ): Int =
        rects.sumOf { r ->
            var n = 0
            for (y in max(0, r.top.toInt()) until minOf(px.height, r.bottom.toInt())) {
                for (x in max(0, r.left.toInt()) until minOf(px.width, r.right.toInt())) {
                    if (inks.any { near(px[x, y], it, .08f) }) n++
                }
            }
            n
        }

    private fun near(
        a: Color,
        b: Color,
        within: Float,
    ) = abs(a.red - b.red) < within && abs(a.green - b.green) < within && abs(a.blue - b.blue) < within

    /** Whether [c] is the stage's [bg], or the clear of a popup's root around what it draws. */
    private fun ground(
        c: Color,
        bg: Color,
    ) = c.alpha < .5f || near(c, bg, .02f)

    /** Pixels in the outline's ink: a finished body's, never a mosaic's. */
    private fun inked(
        px: PixelMap,
        colors: MutantColors,
    ): Int = (0 until px.height).sumOf { y -> (0 until px.width).count { x -> near(px[x, y], colors.ink, .04f) } }

    /** The bounds of what is drawn over the background, or null when nothing is. */
    private fun drawn(
        px: PixelMap,
        bg: Color,
    ): IntArray? {
        var x0 = Int.MAX_VALUE
        var y0 = Int.MAX_VALUE
        var x1 = -1
        var y1 = -1
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                if (!ground(px[x, y], bg)) {
                    x0 = minOf(x0, x)
                    y0 = minOf(y0, y)
                    x1 = max(x1, x)
                    y1 = max(y1, y)
                }
            }
        }
        return if (x1 < 0) null else intArrayOf(x0, y0, x1, y1)
    }

    /** Pixels other than the background, and the share of them in a mosaic's paints (the fills and the cocoon's). */
    private fun paintShare(
        px: PixelMap,
        colors: MutantColors,
    ): Pair<Int, Double> {
        val paints =
            listOf(
                colors.primary,
                colors.surface,
                colors.surfaceVariant,
                colors.phosphor,
                colors.magenta,
                colors.noSignal,
                colors.amber,
                colors.cocoon1,
                colors.cocoon2,
            )
        var other = 0
        var painted = 0
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val c = px[x, y]
                if (ground(c, colors.background)) continue
                other++
                // A block's edge at a fractional offset (a riding snackbar) blends two paints: the share reads within.
                if (px.edge(x, y)) {
                    painted++
                } else if (paints.any { near(c, it, .02f) }) {
                    painted++
                }
            }
        }
        return other to if (other == 0) 0.0 else painted.toDouble() / other
    }

    /**
     * The share of the whole [side]-pixel cells, laid from where the picture starts, that are one colour 2 px in from
     * their edges, the background the slot's rounding leaves and the blended edges aside.
     */
    private fun uniformCells(
        px: PixelMap,
        bounds: IntArray,
        side: Int,
        bg: Color,
    ): Double {
        val (x0, y0, x1, y1) = bounds.toList()
        val cols = (x1 - x0 + 1) / side
        val rows = (y1 - y0 + 1) / side
        var cells = 0
        var uniform = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                cells++
                var ref: Color? = null
                var same = true
                for (y in y0 + r * side + 2 until y0 + (r + 1) * side - 2) {
                    for (x in x0 + c * side + 2 until x0 + (c + 1) * side - 2) {
                        val p = px[x, y]
                        if (ground(p, bg) || px.edge(x, y)) continue
                        if (ref == null) {
                            ref = p
                        } else if (!near(p, ref, .02f)) {
                            same = false
                        }
                    }
                }
                if (same) uniform++
            }
        }
        return if (cells == 0) 0.0 else uniform.toDouble() / cells
    }

    private fun check(
        case: Case,
        colors: MutantColors,
        d: Float,
    ) = runComposeUiTest {
        val (shown, left) = strip(case, colors, d)
        val coarse = shown.filter { it.ms <= 96 }
        val fine = shown.filter { it.ms in 112..208 }
        val whole = shown.filter { it.ms >= 224 }
        val tag = "${case.name} at density $d"
        for (s in coarse + fine) {
            assertEquals(0, s.glyphs, "$tag: no glyph before the third step, ${s.ms} ms")
            val (n, share) = paintShare(s.px, colors)
            assertTrue(n > 400 * d * d, "$tag: a mosaic stands at ${s.ms} ms ($n px)")
            assertTrue(share > .97, "$tag: only the mosaic's paints at ${s.ms} ms: $share")
            assertEquals(0, inked(s.px, colors), "$tag: no outline at ${s.ms} ms")
        }
        for (s in whole) {
            assertTrue(s.glyphs > 20 * d * d, "$tag: the words at ${s.ms} ms (${s.glyphs} px)")
            assertTrue(inked(s.px, colors) > 100 * d, "$tag: the finished body, outlined, at ${s.ms} ms")
        }
        if (case.single) {
            fun cells(
                s: Shot,
                side: Int,
            ) = uniformCells(s.px, drawn(s.px, colors.background)!!, (side * d).toInt(), colors.background)
            for (s in coarse) assertTrue(cells(s, 16) > .95, "$tag: 16 dp blocks at ${s.ms} ms: ${cells(s, 16)}")
            for (s in fine) {
                assertTrue(cells(s, 8) > .95, "$tag: 8 dp blocks at ${s.ms} ms: ${cells(s, 8)}")
                assertTrue(cells(s, 16) < .8, "$tag: finer than 16 dp at ${s.ms} ms: ${cells(s, 16)}")
            }
        }
        if (!case.leaves) return@runComposeUiTest
        // Leaving: the 8 dp mosaic, then nothing, by 160 ms, then gone from the composition.
        for (s in left.filter { it.ms <= 64 }) {
            assertEquals(0, s.glyphs, "$tag: leaving shows no word, ${s.ms} ms")
            val (n, share) = paintShare(s.px, colors)
            assertTrue(n > 400 * d * d && share > .97, "$tag: leaving at the 8 dp mosaic, ${s.ms} ms: $n, $share")
            if (case.single) {
                val c = uniformCells(s.px, drawn(s.px, colors.background)!!, (8 * d).toInt(), colors.background)
                assertTrue(c > .95, "$tag: leaving's 8 dp blocks at ${s.ms} ms: $c")
            }
        }
        for (s in left.filter { it.ms in 80..144 }) {
            assertEquals(0, paintShare(s.px, colors).first, "$tag: nothing drawn at ${s.ms} ms of leaving")
        }
        for (s in left.filter { it.ms >= case.goneBy }) assertTrue(!s.composed, "$tag: gone by ${s.ms} ms")
    }

    @Test
    fun `a card tunes in and steps out`() {
        check(card, MutantPalettes.Efir, 1f)
        check(card, MutantPalettes.Kopiya, 2f)
    }

    @Test
    fun `a cocoon hands over to its card`() = check(handover, MutantPalettes.Kopiya, 1f)

    @Test
    fun `an empty state tunes in and steps out`() {
        check(empty, MutantPalettes.Efir, 1f)
        check(empty, MutantPalettes.Kopiya, 2f)
    }

    @Test
    fun `a search result tunes in from its loading row and steps out`() = check(search, MutantPalettes.Efir, 2f)

    @Test
    fun `the tooltip tunes in and steps out`() {
        check(tooltip, MutantPalettes.Efir, 1f)
        check(tooltip, MutantPalettes.Kopiya, 2f)
    }

    @Test
    fun `the snackbar tunes in as it rides in, its words on the third step, and steps out`() {
        check(snackbar, MutantPalettes.Efir, 1f)
        check(snackbar, MutantPalettes.Kopiya, 2f)
    }

    @Test
    fun `under reduce motion the finished frame shows at once and leaving removes at once`() {
        for (case in listOf(card, empty, tooltip, snackbar)) {
            runComposeUiTest {
                val (shown, left) = strip(case, MutantPalettes.Efir, 1f, reduce = true, frames = 3)
                // The first frame is the finished one; a body built off the frame may draw from the next (B-50).
                val first = shown[1]
                assertTrue(first.glyphs > 20, "${case.name}: the words at once (${first.glyphs} px)")
                assertTrue(inked(first.px, MutantPalettes.Efir) > 100, "${case.name}: the finished body, outlined")
                assertTrue(!left[1].composed, "${case.name}: gone at once")
                assertEquals(0, paintShare(left[1].px, MutantPalettes.Efir).first, "${case.name}: nothing left")
            }
        }
    }

    @Test
    fun `a leaving content stays composed for its 160 ms, then leaves the composition`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var alive = false
            setContent {
                MutantTheme {
                    MutantAppear(on) {
                        androidx.compose.runtime.DisposableEffect(Unit) {
                            alive = true
                            onDispose { alive = false }
                        }
                        MutantCard(Modifier.width(300.dp)) { MutantCardTitle("Ночной эфир") }
                    }
                }
            }
            on = true
            mainClock.advanceTimeBy(1000)
            assertTrue(alive, "shown")
            on = false
            val strip = List(12) { mainClock.advanceTimeByFrame().let { alive } }
            assertTrue(strip.take(10).all { it }, "composed through 144 ms: $strip")
            assertTrue(!strip.last(), "gone by 176 ms: $strip")
        }

    @Test
    fun `a new message in a standing snackbar tunes it in again, the new words on the third step`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var message by mutableStateOf("Добавлено")
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box(Modifier.testTag("stage").size(360.dp, 120.dp).background(MutantPalettes.Efir.background)) {
                            MutantSnackbar(message, Modifier.width(300.dp))
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1000)
            message = "Удалено"

            fun words(ms: Int): Int {
                val px = onNodeWithTag("stage").captureToImage().toPixelMap()
                val r =
                    onAllNodesWithText(
                        "Удалено",
                        useUnmergedTree = true,
                    ).fetchSemanticsNodes().map { it.boundsInRoot }
                return glyphs(px, r, listOf(MutantPalettes.Efir.onNoSignal)).also { println("$ms $it") }
            }
            val strip = List(16) { i -> mainClock.advanceTimeByFrame().let { words(i * 16) } }
            assertTrue(strip.take(14).all { it == 0 }, "no new word before 224 ms: $strip")
            assertTrue(strip.last() > 20, "the new words on the third step: $strip")
        }

    @Test
    fun `the badge switches on whole in its first frame`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    MutantTheme {
                        Box(Modifier.testTag("stage").size(120.dp, 60.dp).background(MutantPalettes.Efir.background)) {
                            if (on) MutantBadge("НОВОЕ", Modifier.padding(16.dp))
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1000)
            on = true
            mainClock.advanceTimeByFrame()
            val first = onNodeWithTag("stage").captureToImage().toPixelMap()
            mainClock.advanceTimeBy(320)
            val later = onNodeWithTag("stage").captureToImage().toPixelMap()
            var differ = 0
            for (y in 0 until first.height) for (x in 0 until first.width) if (first[x, y] != later[x, y]) differ++
            assertEquals(0, differ, "the first frame is the finished picture")
            val ink =
                (0 until first.height).sumOf { y ->
                    (0 until first.width).count { x -> near(first[x, y], Color(0xFF05060C), .05f) }
                }
            assertTrue(ink > 200, "its words and ring drawn in the first frame: $ink")
        }
}
