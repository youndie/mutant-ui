package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-181, the designer's answer 3: a body disabled loses its signal in three steps over 300 ms — its fill desaturating a
 * third at each, a tear band rolling up over it, «нет сигнала» on the third step under the band — and «нет сигнала»
 * alone from 300 ms; enabled, it tunes in with B-179's steps; focus in and out change in one frame; under reduce motion
 * the finished frame shows at once.
 *
 * The test clock moves in 16 ms frames from the frame the change is composed in (0 ms): the steps are read at 0–96 ms,
 * 112–192 ms and 208–288 ms, the finished look from 304 ms. Every reading counts pixels over the body, with bounds far
 * from what a few anti-aliased edge pixels could move: CI's rasteriser differs from this Mac's.
 */
@OptIn(ExperimentalTestApi::class)
class SignalLossTest {
    private var enabled by mutableStateOf(true)
    private var focused by mutableStateOf(false)

    private class Shot(
        val ms: Int,
        val px: PixelMap,
    )

    /** The frames after each change, the first of each the frame before it (-16 ms), and the button's slot. */
    private class Strip(
        val runs: List<List<Shot>>,
        val slot: Rect,
    ) {
        val shots get() = runs[0]
    }

    /**
     * Composes the button, lets it settle, then for each of [changes] shows a frame before it and every frame after it,
     * to [frames].
     */
    private fun strip(
        colors: MutantColors,
        d: Float,
        reduce: Boolean = false,
        startEnabled: Boolean = true,
        frames: Int = 25,
        vararg changes: () -> Unit,
    ): Strip {
        var out: Strip? = null
        runComposeUiTest { out = strip(colors, d, reduce, startEnabled, frames, changes.toList()) }
        return out!!
    }

    private fun ComposeUiTest.strip(
        colors: MutantColors,
        d: Float,
        reduce: Boolean,
        startEnabled: Boolean,
        frames: Int,
        changes: List<() -> Unit>,
    ): Strip {
        mainClock.autoAdvance = false
        enabled = startEnabled
        focused = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors = colors, reduceMotion = reduce) {
                    val f = remember { FocusRequester() }
                    val focus = LocalFocusManager.current
                    androidx.compose.runtime.LaunchedEffect(
                        focused,
                    ) { if (focused) f.requestFocus() else focus.clearFocus() }
                    Box(Modifier.testTag("stage").size(220.dp, 100.dp).background(colors.background)) {
                        Box(Modifier.padding(20.dp)) {
                            MutantButton("Записать", {}, Modifier.testTag("b").focusRequester(f), enabled = enabled)
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1000)

        fun shot(ms: Int) = Shot(ms, onNodeWithTag("stage").captureToImage().toPixelMap())
        val runs =
            changes.map { change ->
                val before = shot(-16)
                change()
                listOf(before) + List(frames) { i -> mainClock.advanceTimeByFrame().let { shot(i * 16) } }
            }
        return Strip(runs, onNodeWithTag("b").fetchSemanticsNode().boundsInRoot)
    }

    private fun near(
        a: Color,
        b: Color,
        within: Float = .02f,
    ) = abs(a.red - b.red) < within && abs(a.green - b.green) < within && abs(a.blue - b.blue) < within

    /** Pixels in [r] of [px] near [c]. */
    private fun count(
        px: PixelMap,
        r: Rect,
        c: Color,
        within: Float = .02f,
    ): Int {
        var n = 0
        for (y in r.top.toInt() until minOf(px.height, r.bottom.toInt())) {
            for (x in r.left.toInt() until minOf(px.width, r.right.toInt())) if (near(px[x, y], c, within)) n++
        }
        return n
    }

    /** Pixels of the whole picture that differ between [a] and [b] by more than a tenth in a channel. */
    private fun diff(
        a: PixelMap,
        b: PixelMap,
    ): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        for (y in 0 until a.height) for (x in 0 until a.width) if (!near(a[x, y], b[x, y], .1f)) out += x to y
        return out
    }

    private fun lossCheck(
        colors: MutantColors,
        d: Float,
    ) {
        val (shots, r) = strip(colors, d, changes = arrayOf({ enabled = false })).let { it.shots to it.slot }
        val tag = "density $d"
        val fill = colors.primary
        val dis = disabledFill(fill, highlightFor(fill, colors), colors)
        val step = List(2) { mixColor(fill, dis, signalLossShare(it), 0.0) }
        val area = r.width * r.height

        fun at(ms: Int) = shots.first { it.ms == ms }.px

        // The rest body before; at every step's frames the fill of that step, and no other.
        assertTrue(count(at(-16), r, fill) > area * .2, "$tag: the rest fill before")
        for (s in shots.filter { it.ms in 0..96 }) {
            assertTrue(count(s.px, r, step[0]) > area * .2, "$tag: a third desaturated at ${s.ms} ms")
            assertTrue(count(s.px, r, step[1]) < area * .01, "$tag: not two thirds at ${s.ms} ms")
            assertTrue(count(s.px, r, fill) < area * .01, "$tag: the rest fill gone at ${s.ms} ms")
        }
        for (s in shots.filter { it.ms in 112..192 }) {
            assertTrue(count(s.px, r, step[1]) > area * .2, "$tag: two thirds desaturated at ${s.ms} ms")
            assertTrue(count(s.px, r, step[0]) < area * .01, "$tag: not a third at ${s.ms} ms")
            assertTrue(count(s.px, r, dis) < area * .05, "$tag: still the slime at ${s.ms} ms")
        }
        for (s in shots.filter { it.ms >= 208 }) {
            assertTrue(count(s.px, r, dis) > area * .1, "$tag: «нет сигнала» at ${s.ms} ms")
            assertTrue(count(s.px, r, step[1]) < area * .01, "$tag: no slime at ${s.ms} ms")
        }
        // The tear band: it stands at the slot's bottom, then its middle, then its top; the third step is «нет сигнала»
        // under the band at the top, and from 300 ms «нет сигнала» alone, still.
        val third = shots.filter { it.ms in 208..288 }
        val done = shots.filter { it.ms >= 304 }
        for (s in third) {
            assertTrue(
                diff(third[0].px, s.px).size < 20 * d * d,
                "$tag: the third step stands at ${s.ms} ms",
            )
        }
        for (s in done) assertTrue(diff(done[0].px, s.px).size < 20 * d * d, "$tag: finished and still at ${s.ms} ms")
        val tear = diff(third[0].px, done[0].px)
        assertTrue(tear.size > 60 * d * d, "$tag: the third step differs from the finished look: ${tear.size} px")
        val high = tear.count { (_, y) -> y < r.top + r.height / 2 }
        assertTrue(high > tear.size * .8, "$tag: the third step's band at the slot's top: $high of ${tear.size}")
        // The finished look is the disabled button's own, as composed disabled.
        val finished = done[0].px
        val composed = strip(colors, d, startEnabled = false, frames = 2, changes = arrayOf({})).shots.last().px
        assertTrue(diff(finished, composed).size < 20 * d * d, "$tag: «нет сигнала» from 300 ms")
    }

    @Test
    fun `a button disabled loses its signal in three steps and stands at no signal from 300 ms`() {
        lossCheck(MutantPalettes.Efir, 1f)
        lossCheck(MutantPalettes.Kopiya, 2f)
    }

    /** Whether the pixel at [x], [y] differs from one beside it: an edge, which the slot's rounding blends. */
    private fun PixelMap.edge(
        x: Int,
        y: Int,
    ): Boolean {
        val c = this[x, y]
        return (x > 0 && !near(c, this[x - 1, y])) || (x < width - 1 && !near(c, this[x + 1, y])) ||
            (y > 0 && !near(c, this[x, y - 1])) || (y < height - 1 && !near(c, this[x, y + 1]))
    }

    /**
     * The share of the whole [side]-pixel cells, laid from the slot's corner, that are one colour 2 px in from their
     * edges, the background its rounding leaves and the blended edges aside.
     */
    private fun uniformCells(
        px: PixelMap,
        r: Rect,
        side: Int,
        bg: Color,
    ): Double {
        val x0 = r.left.toInt()
        val y0 = r.top.toInt()
        val cols = r.width.toInt() / side
        val rows = r.height.toInt() / side
        var cells = 0
        var uniform = 0
        for (rr in 0 until rows) {
            for (c in 0 until cols) {
                cells++
                var ref: Color? = null
                var same = true
                for (y in y0 + rr * side + 2 until y0 + (rr + 1) * side - 2) {
                    for (x in x0 + c * side + 2 until x0 + (c + 1) * side - 2) {
                        val p = px[x, y]
                        if (near(p, bg) || px.edge(x, y)) continue
                        if (ref == null) {
                            ref = p
                        } else if (!near(p, ref)) {
                            same = false
                        }
                    }
                }
                if (same) uniform++
            }
        }
        return uniform.toDouble() / cells
    }

    private fun enableCheck(
        colors: MutantColors,
        d: Float,
    ) {
        val (shots, r) =
            strip(colors, d, startEnabled = false, changes = arrayOf({ enabled = true })).let {
                it.shots to
                    it.slot
            }
        val tag = "density $d"
        val area = r.width * r.height
        // The slot's own mosaic: its blocks in the fill and the cocoon's paints, no outline, no word.
        val paints = listOf(colors.primary, colors.cocoon1, colors.cocoon2)
        for (s in shots.filter { it.ms in 0..208 }) {
            val painted = paints.sumOf { count(s.px, r, it) }
            assertTrue(painted > area * .9, "$tag: the tune-in's mosaic at ${s.ms} ms ($painted of $area)")
            assertEquals(0, count(s.px, r, colors.ink, .04f), "$tag: no outline or word at ${s.ms} ms")
            val side = if (s.ms <= 96) 16 else 8
            val cells = uniformCells(s.px, r, (side * d).toInt(), colors.background)
            assertTrue(cells > .95, "$tag: $side dp blocks at ${s.ms} ms: $cells")
            if (side == 8) {
                val coarse = uniformCells(s.px, r, (16 * d).toInt(), colors.background)
                assertTrue(coarse < .8, "$tag: finer than 16 dp at ${s.ms} ms: $coarse")
            }
        }
        for (s in shots.filter { it.ms >= 224 }) {
            assertTrue(count(s.px, r, colors.ink, .04f) > 100 * d, "$tag: the finished body at ${s.ms} ms")
            assertTrue(count(s.px, r, colors.primary) > area * .2, "$tag: its fill at ${s.ms} ms")
        }
    }

    @Test
    fun `a button enabled tunes in with the tune-in's steps`() {
        enableCheck(MutantPalettes.Efir, 1f)
        enableCheck(MutantPalettes.Kopiya, 2f)
    }

    /** Pixels in the focus colour around [r]: the brackets, 7 dp outside the slot. */
    private fun brackets(
        px: PixelMap,
        r: Rect,
        d: Float,
        colors: MutantColors,
    ): Int = count(px, Rect(r.left - 10 * d, r.top - 10 * d, r.right + 10 * d, r.bottom + 10 * d), colors.focus, .04f)

    private fun focusCheck(
        colors: MutantColors,
        d: Float,
    ) {
        // The focus is moved by an effect, a frame after the change: the frame it lands in is the one read.
        val run = strip(colors, d, frames = 12, changes = arrayOf({ focused = true }, { focused = false }))
        val into = run.runs[0]
        val r = run.slot
        val tag = "density $d"
        val first = into.drop(1).first { brackets(it.px, r, d, colors) > 0 }
        val full = brackets(first.px, r, d, colors)
        assertTrue(full > 60 * d, "$tag: the brackets whole in focus's first frame ($full px)")
        assertTrue(count(first.px, r, colors.ink, .04f) > 100 * d, "$tag: the body drawn whole in focus's first frame")
        for (s in into.filter { it.ms > first.ms }) {
            assertEquals(full, brackets(s.px, r, d, colors), "$tag: the brackets unchanged at ${s.ms} ms")
            assertTrue(count(s.px, r, colors.ink, .04f) > 100 * d, "$tag: the body whole at ${s.ms} ms")
        }
        val out = run.runs[1].drop(1).map { it.px }
        val gone = out.indexOfFirst { brackets(it, r, d, colors) == 0 }
        assertTrue(gone in 0..1, "$tag: the brackets gone in focus out's first frame ($gone)")
        for (px in out.drop(gone)) {
            assertEquals(0, brackets(px, r, d, colors), "$tag: no bracket after focus out")
            assertTrue(count(px, r, colors.ink, .04f) > 100 * d, "$tag: the body whole after focus out")
        }
    }

    @Test
    fun `focus in and out still change in one frame`() {
        focusCheck(MutantPalettes.Efir, 1f)
        focusCheck(MutantPalettes.Kopiya, 2f)
    }

    private fun reduceCheck(
        colors: MutantColors,
        d: Float,
    ) {
        val off = strip(colors, d, reduce = true, changes = arrayOf({ enabled = false })).shots
        val tag = "density $d"
        for (s in off.drop(1)) {
            assertTrue(diff(off.last().px, s.px).size < 20 * d * d, "$tag: «нет сигнала» finished at ${s.ms} ms")
        }
        val composed =
            strip(
                colors,
                d,
                reduce = true,
                startEnabled = false,
                frames = 2,
                changes = arrayOf({}),
            ).shots.last().px
        assertTrue(diff(off[1].px, composed).size < 20 * d * d, "$tag: «нет сигнала» in the change's frame")
        val run = strip(colors, d, reduce = true, startEnabled = false, changes = arrayOf({ enabled = true }))
        val on = run.shots
        val r = run.slot
        for (s in on.drop(1)) {
            assertTrue(count(s.px, r, colors.ink, .04f) > 100 * d, "$tag: the body finished at ${s.ms} ms")
            assertTrue(diff(on.last().px, s.px).size < 20 * d * d, "$tag: enabled finished at ${s.ms} ms")
        }
    }

    @Test
    fun `under reduce motion the finished frame shows at once`() {
        reduceCheck(MutantPalettes.Efir, 1f)
        reduceCheck(MutantPalettes.Kopiya, 2f)
    }
}
