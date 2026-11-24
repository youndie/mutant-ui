package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-185, the designer's answers 7 and 29: the dialog's window is opaque from its first frame and tunes in (B-179's 16 dp
 * mosaic, 8 dp mosaic, then the window with its words, 320 ms in three steps) while the scrim darkens over 200 ms; it
 * steps out (the 8 dp mosaic, then nothing, by 160 ms) while the scrim lightens over 200 ms; and a dialog taller than the
 * screen stands 24 dp from every edge, its title and actions in view and its text scrolling.
 *
 * The test clock moves in 16 ms frames from the frame the change is composed in (0 ms). Every reading counts pixels over
 * an area with margins far from its bound (the window's inside 16 dp in from its slot, whole mosaic cells 2 px in from
 * their edges, a 32 dp patch of scrim), never a few edge pixels, which CI's rasteriser may move.
 */
@OptIn(ExperimentalTestApi::class)
class DialogOpaqueAndTallTest {
    private var open by mutableStateOf(false)

    private class Shot(
        val ms: Int,
        val px: PixelMap,
        /** The window's slot in pixels, or null once the dialog has left the composition. */
        val window: Rect?,
        /** Pixels of the title's ink inside the title's bounds. */
        val glyphs: Int,
    )

    /**
     * A screen of one [ground] colour under a dialog; every frame of the open, then of the close. [fromStart] composes it
     * open, as a screen that opens under its dialog does, its frames counted from the first one drawn.
     */
    private fun ComposeUiTest.strip(
        colors: MutantColors,
        d: Float,
        ground: Color,
        reduce: Boolean = false,
        primary: Boolean = true,
        fromStart: Boolean = false,
    ): Pair<List<Shot>, List<Shot>> {
        mainClock.autoAdvance = false
        open = fromStart
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors = colors, reduceMotion = reduce) {
                    Box(Modifier.size(360.dp, 400.dp).background(ground).testTag("screen")) {
                        MutantDialog(
                            open,
                            "Стереть запись?",
                            { open = false },
                            text = "Плёнку можно будет записать заново.",
                        ) {
                            MutantDialogAction("Отмена", { open = false })
                            MutantDialogAction("Стереть", {}, primary = primary)
                        }
                    }
                }
            }
        }
        if (!fromStart) mainClock.advanceTimeBy(500)
        var seen: Rect? = null

        fun shot(ms: Int): Shot {
            val px = onNodeWithTag("screen").captureToImage().toPixelMap()
            val window =
                onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsDialog))
                    .fetchSemanticsNodes()
                    .firstOrNull()
                    ?.boundsInRoot
            val title =
                onAllNodesWithText(
                    "Стереть запись?",
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().firstOrNull()?.boundsInRoot
            if (title != null) seen = title
            return Shot(ms, px, window, seen?.let { count(px, it) { c -> near(c, colors.text, .08f) } } ?: 0)
        }
        open = true
        val opening = List(22) { i -> mainClock.advanceTimeByFrame().let { shot(i * 16) } }
        open = false
        val closing = List(16) { i -> mainClock.advanceTimeByFrame().let { shot(i * 16) } }
        return opening to closing
    }

    private fun near(
        a: Color,
        b: Color,
        within: Float,
    ) = abs(a.red - b.red) < within && abs(a.green - b.green) < within && abs(a.blue - b.blue) < within

    private fun count(
        px: PixelMap,
        r: Rect,
        what: (Color) -> Boolean,
    ): Int {
        var n = 0
        for (y in maxOf(0, r.top.toInt()) until minOf(px.height, r.bottom.toInt())) {
            for (x in maxOf(0, r.left.toInt()) until minOf(px.width, r.right.toInt())) if (what(px[x, y])) n++
        }
        return n
    }

    /** The window's inside: its slot 16 dp in, clear of its rounded corners and its outline. */
    private fun Rect.inside(d: Float) = Rect(left + 16 * d, top + 16 * d, right - 16 * d, bottom - 16 * d)

    /** The window's slot above its actions' row (its foot's 64 dp), whose primary body lays a mosaic of its own. */
    private fun Rect.upper(d: Float) = Rect(left, top, right, bottom - 64 * d)

    /**
     * The mean brightness (r + g + b) of a patch of the screen beside the window, where only the ground and the scrim lie:
     * by default 32 dp square left of a 280 dp window; [x0] to [x1], [y0] to [y1] in dp.
     */
    private fun scrim(
        px: PixelMap,
        d: Float,
        x0: Int = 4,
        x1: Int = 36,
        y0: Int = 8,
        y1: Int = 40,
    ): Float {
        var sum = 0f
        var n = 0
        for (y in (y0 * d).toInt() until (y1 * d).toInt()) {
            for (x in (x0 * d).toInt() until (x1 * d).toInt()) {
                val c = px[x, y]
                sum += c.red + c.green + c.blue
                n++
            }
        }
        return sum / n
    }

    /** The share of whole [side]-pixel cells, laid from [r]'s origin, one colour 2 px in from their edges. */
    private fun uniformCells(
        px: PixelMap,
        r: Rect,
        side: Int,
    ): Double {
        val x0 = r.left.toInt()
        val y0 = r.top.toInt()
        val cols = (r.width.toInt()) / side
        val rows = (r.height.toInt()) / side
        var cells = 0
        var uniform = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                cells++
                val ref = px[x0 + col * side + 2, y0 + row * side + 2]
                var same = true
                for (y in y0 + row * side + 2 until y0 + (row + 1) * side - 2) {
                    for (x in x0 + col * side + 2 until x0 + (col + 1) * side - 2) {
                        if (!near(px[x, y], ref, .02f)) same = false
                    }
                }
                if (same) uniform++
            }
        }
        return uniform.toDouble() / cells
    }

    private fun opaqueAndTuned(
        colors: MutantColors,
        d: Float,
    ) = runComposeUiTest {
        val tag = "${if (colors == MutantPalettes.Efir) "Эфир" else "Копия"} at density $d"
        // The same dialog over a red screen and over a green one: wherever the window stands opaque, the two agree.
        val (red, redOut) = strip(colors, d, Color(0xFFE0_2020))
        val (green, greenOut) = strip(colors, d, Color(0xFF20_C040))
        for ((a, b) in (red + redOut).zip(green + greenOut)) {
            val w = a.window ?: continue
            if (a in redOut && a.ms >= 80) continue
            val inside = w.inside(d)
            val area = inside.width * inside.height
            val through = through(a, b, d)
            assertTrue(through <= area * .001f, "$tag: the screen reads through the window at ${a.ms} ms ($through px)")
        }
        // The window: the 16 dp mosaic to 96 ms, the 8 dp one to 208, then itself with its words.
        for (s in red) {
            assertTrue(s.window != null, "$tag: composed at ${s.ms} ms")
            when {
                s.ms <= 96 -> {
                    assertEquals(0, s.glyphs, "$tag: no word at ${s.ms} ms")
                    val cells = uniformCells(s.px, s.window.upper(d), (16 * d).toInt())
                    assertTrue(cells > .75, "$tag: 16 dp blocks at ${s.ms} ms: $cells")
                }

                s.ms <= 208 -> {
                    assertEquals(0, s.glyphs, "$tag: no word at ${s.ms} ms")
                    val fine = uniformCells(s.px, s.window.upper(d), (8 * d).toInt())
                    val coarse = uniformCells(s.px, s.window.upper(d), (16 * d).toInt())
                    assertTrue(fine > .85 && coarse < .5, "$tag: 8 dp blocks at ${s.ms} ms: $fine, 16 dp $coarse")
                }

                else -> {
                    assertTrue(s.glyphs > 60 * d * d, "$tag: the words at ${s.ms} ms (${s.glyphs} px)")
                }
            }
        }
        // The scrim darkens over 200 ms, every frame darker, and stands from there.
        val ground = scrim(red.first().px, d)
        val dark = red.map { ground - scrim(it.px, d) }
        val full = dark.last()
        assertTrue(full > .3f, "$tag: the scrim darkens the screen: $full")
        for (i in 1..12) assertTrue(dark[i] > dark[i - 1] + full * .03f, "$tag: darker at ${i * 16} ms: $dark")
        assertTrue(dark[11] < full * .97f, "$tag: not yet full at 176 ms: $dark")
        for (i in 13 until dark.size) assertEquals(full, dark[i], full * .01f, "$tag: full from 208 ms: $dark")
        // Leaving: the 8 dp mosaic to 64 ms, then nothing (the scrim alone where the window stood), gone by 208.
        for (s in redOut) {
            when {
                s.ms <= 64 -> {
                    assertEquals(0, s.glyphs, "$tag: no word leaving, ${s.ms} ms")
                    val fine = uniformCells(s.px, s.window!!.upper(d), (8 * d).toInt())
                    assertTrue(fine > .85, "$tag: leaving's 8 dp blocks at ${s.ms} ms: $fine")
                }

                s.ms <= 192 -> {
                    val inside = red.last().window!!.inside(d)
                    val shade = scrim(s.px, d)
                    val off =
                        count(s.px, inside) { c ->
                            abs(c.red + c.green + c.blue - shade) > .06f
                        }
                    assertEquals(0, off, "$tag: nothing of the window at ${s.ms} ms of leaving")
                }

                else -> {
                    assertTrue(s.window == null, "$tag: gone at ${s.ms} ms")
                }
            }
        }
        val light = redOut.map { ground - scrim(it.px, d) }
        for (i in 1..12) assertTrue(light[i] < light[i - 1] - full * .03f, "$tag: lighter at ${i * 16} ms: $light")
        assertTrue(light[11] > full * .03f, "$tag: not yet clear at 176 ms: $light")
        assertTrue(light[13] < full * .01f, "$tag: clear from 208 ms: $light")
    }

    /** The pixels of the window's inside where two renders over different screens differ: the screen read through. */
    private fun through(
        a: Shot,
        b: Shot,
        d: Float,
    ): Int {
        val inside = a.window!!.inside(d)
        return count(a.px, inside) { true } - countSame(a.px, b.px, inside)
    }

    @Test
    fun `composed open, the window tunes in opaque from its first frame too`() =
        runComposeUiTest {
            val (red, _) = strip(MutantPalettes.Efir, 1f, Color(0xFFE0_2020), fromStart = true)
            val (green, _) = strip(MutantPalettes.Efir, 1f, Color(0xFF20_C040), fromStart = true)
            for (i in 0 until red.size) {
                val (a, b) = red[i] to green[i]
                assertTrue(a.window != null, "composed at frame $i")
                val inside = a.window.inside(1f)
                val n = through(a, b, 1f)
                assertTrue(n <= inside.width * inside.height * .001f, "the screen reads through at frame $i ($n px)")
                // Counted from the first frame drawn, which the clock may already have moved 16 ms into the tune.
                if (i < 5) assertEquals(0, a.glyphs, "no word at frame $i")
                if (i >= 15) assertTrue(a.glyphs > 60, "the words at frame $i (${a.glyphs} px)")
            }
        }

    private fun countSame(
        a: PixelMap,
        b: PixelMap,
        r: Rect,
    ): Int {
        var n = 0
        for (y in maxOf(0, r.top.toInt()) until minOf(a.height, r.bottom.toInt())) {
            for (x in maxOf(0, r.left.toInt()) until minOf(a.width, r.right.toInt())) {
                if (near(a[x, y], b[x, y], .01f)) n++
            }
        }
        return n
    }

    @Test
    fun `the window rises opaque by tune-in and steps out while the scrim takes 200 ms each way`() {
        opaqueAndTuned(MutantPalettes.Efir, 1f)
        opaqueAndTuned(MutantPalettes.Kopiya, 2f)
    }

    @Test
    fun `under reduce motion the window and the scrim stand finished in the first frame and leave at once`() =
        runComposeUiTest {
            val (opening, closing) = strip(MutantPalettes.Efir, 1f, Color(0xFFE0_2020), reduce = true)
            val first = opening.first()
            assertTrue(first.glyphs > 60, "the words in the first frame (${first.glyphs} px)")
            val ground = 0xE0 / 255f + 2 * 0x20 / 255f
            assertTrue(ground - scrim(first.px, 1f) > .3f, "the scrim whole in the first frame")
            assertTrue(closing[1].window == null, "gone in the frame after the close")
        }

    @Test
    fun `a dialog taller than the screen stands 24 dp from every edge, title and actions in view, text scrolling`() {
        tall(MutantPalettes.Efir, 1f)
        tall(MutantPalettes.Kopiya, 2f)
    }

    private fun tall(
        colors: MutantColors,
        d: Float,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val tag = "${if (colors == MutantPalettes.Efir) "Эфир" else "Копия"} at density $d"
        val ground = Color(0xFFE0_2020)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors = colors) {
                    Box(Modifier.size(300.dp, 300.dp).background(ground).testTag("screen")) {
                        MutantDialog(true, "Стереть запись?", {}, text = "Плёнку можно стереть заново. ".repeat(30)) {
                            MutantDialogAction("Отмена", {})
                            MutantDialogAction("Стереть", {}, primary = true)
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1000)
        val window =
            onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.IsDialog),
            ).fetchSemanticsNodes().single().boundsInRoot
        val edge = 24 * d
        val side = 300 * d
        assertEquals(edge, window.top, .5f, "$tag: 24 dp from the top: $window")
        assertEquals(side - edge, window.bottom, .5f, "$tag: 24 dp from the foot: $window")
        assertEquals(edge, window.left, .5f, "$tag: 24 dp from the left: $window")
        assertEquals(side - edge, window.right, .5f, "$tag: 24 dp from the right: $window")
        // The margins above and below show the scrim over the screen alone: no outline, no corner, no fill of the window.
        val px = onNodeWithTag("screen").captureToImage().toPixelMap()
        // Left of the window, in its 24 dp margin, halfway down.
        val shade = scrim(px, d, 2, 20, 100, 200)
        for ((name, band) in listOf(
            "top" to Rect(0f, 0f, side, 18 * d),
            "foot" to Rect(0f, side - 18 * d, side, side),
        )) {
            val off = count(px, band) { c -> abs(c.red + c.green + c.blue - shade) > .06f }
            assertEquals(0, off, "$tag: the window reaches into the $name margin")
        }
        // The window's outline is whole: rows of its ink along its top and its foot.
        val ink = colors.ink
        val topRow =
            count(
                px,
                Rect(window.left + 16 * d, window.top, window.right - 16 * d, window.top + 3 * d),
            ) { near(it, ink, .1f) }
        val footRow =
            count(px, Rect(window.left + 16 * d, window.bottom - 3 * d, window.right - 16 * d, window.bottom)) {
                near(it, ink, .1f)
            }
        val run = (window.width - 32 * d) * .8f
        assertTrue(topRow > run && footRow > run, "$tag: the outline along the top ($topRow) and the foot ($footRow)")

        fun bounds(word: String) =
            onAllNodesWithText(word, substring = true, useUnmergedTree = true).fetchSemanticsNodes().first().let { n ->
                Rect(
                    n.positionInRoot,
                    n.size.let {
                        androidx.compose.ui.geometry
                            .Size(it.width.toFloat(), it.height.toFloat())
                    },
                )
            }
        val title = bounds("Стереть запись?")
        val cancel = bounds("Отмена")
        val body = bounds("Плёнку можно")
        assertTrue(title.top >= window.top && title.bottom <= window.bottom, "$tag: the title in the window: $title")
        assertTrue(
            cancel.top >= window.top && cancel.bottom <= window.bottom,
            "$tag: the actions in the window: $cancel",
        )
        assertTrue(body.bottom > cancel.top, "$tag: the text runs past the actions before scrolling: $body")
        assertTrue(
            count(px, cancel) { near(it, colors.textAccent, .08f) } > 40 * d * d,
            "$tag: the plain action's word drawn",
        )
        // Scrolled to its end, the text's last line stands above the actions; the title and the actions stay put.
        onNode(
            SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy),
        ).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 100_000f)
        }
        mainClock.advanceTimeBy(1000)
        val scrolled = bounds("Плёнку можно")
        assertTrue(scrolled.bottom <= bounds("Отмена").top, "$tag: the text's end above the actions: $scrolled")
        assertEquals(title.top, bounds("Стереть запись?").top, .5f, "$tag: the title pinned")
        assertEquals(cancel.top, bounds("Отмена").top, .5f, "$tag: the actions pinned")
    }
}
