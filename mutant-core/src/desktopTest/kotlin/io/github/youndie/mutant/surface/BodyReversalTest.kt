package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-171: a body's transition changed again while it runs goes on from the picture it shows. For a pour, a slide and a
 * melt, the second change is swept over every frame of the first transition, at density 2, with the design's steps and
 * without; every frame is read from what is drawn. Each frame from the reversal on changes no more than the same
 * transition changes in a frame of its own, uninterrupted (its steps' size under `steppedMotion`), and the reversal's
 * own frame no more than an uninterrupted frame on every frame of the clock, or than the uninterrupted run's step on that
 * very frame: the picture goes on from where it stood.
 * The clock is the test's, advanced by hand; nothing waits on the wall clock.
 */
@OptIn(ExperimentalTestApi::class)
class BodyReversalTest {
    private val c = MutantPalettes.Efir

    /** A run: what the scene shows from each frame on (frame 0 is the rest before the first change), read [frames] frames. */
    private class Run(
        val events: List<Pair<Int, Int>>,
        val frames: Int,
    )

    /**
     * Each [runs]' readings, one per frame from its rest (index 0) on, the scene composed afresh for each run; [scene]
     * draws a phase, [read] reads a frame.
     */
    private fun <M> film(
        steps: Boolean,
        runs: List<Run>,
        read: (PixelMap) -> M,
        scene: @Composable (phase: Int) -> Unit,
    ): List<List<M>> {
        val out = ArrayList<List<M>>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var run by mutableIntStateOf(0)
            var phase by mutableIntStateOf(0)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    MutantTheme(reduceMotion = false, steppedMotion = steps) {
                        Box(Modifier.background(c.background).testTag("f")) { key(run) { scene(phase) } }
                    }
                }
            }
            for (r in runs) {
                run++
                phase = 0
                frames(WARM_UP)
                val m = ArrayList<M>()
                m += read(shot())
                for (i in 1..r.frames) {
                    r.events.firstOrNull { it.first == i }?.let { phase = it.second }
                    mainClock.advanceTimeByFrame()
                    m += read(shot())
                }
                out += m
            }
        }
        return out
    }

    private fun ComposeUiTest.frames(n: Int) = repeat(n) { mainClock.advanceTimeByFrame() }

    private fun ComposeUiTest.shot() = onNodeWithTag("f").captureToImage().toPixelMap()

    /** The largest change from one frame to the next in [m] from frame [from] on, by [delta], and where. */
    private fun <M> worst(
        m: List<M>,
        from: Int,
        delta: (M, M) -> Int,
        skip: (M, M) -> Boolean = { _, _ -> false },
    ): Pair<Int, Int> {
        var w = 0
        var at = -1
        for (i in from until m.size) {
            if (skip(m[i - 1], m[i])) continue
            val d = delta(m[i - 1], m[i])
            if (d > w) {
                w = d
                at = i
            }
        }
        return w to at
    }

    // The pour.

    private fun lime(px: PixelMap): BooleanArray =
        BooleanArray(px.width * px.height) { i ->
            val p = px[i % px.width, i / px.width]
            abs(p.red - c.primary.red) + abs(p.green - c.primary.green) + abs(p.blue - c.primary.blue) < .12f
        }

    private fun changed(
        a: BooleanArray,
        b: BooleanArray,
    ): Int = a.indices.count { a[it] != b[it] }

    private fun pour(
        body: MutantBody,
        w: Int,
        h: Int,
        steps: Boolean,
        every: Int,
    ) {
        val frames = (pourDuration(h.toDouble()) / FRAME).toInt() + 3
        val flips = (1..((pourDuration(h.toDouble()) - 1) / FRAME).toInt() step every).toList()
        // The control: a pour from rest, run to its end, then the pour back from rest.
        val runs =
            listOf(Run(listOf(1 to 1, frames + 1 to 2), 2 * frames)) +
                flips.map { Run(listOf(1 to 1, 1 + it to 2), it + frames) }
        val scene: @Composable (Int) -> Unit = { ph ->
            MutantSurface(
                body,
                body.name,
                Modifier.padding(20.dp).size(w.dp, h.dp),
                fill = if (ph == 1) c.primary else c.surfaceVariant,
            ) {}
        }
        val free = if (steps) film(false, runs.take(1), ::lime, scene)[0] else null
        val all = film(steps, runs, ::lime, scene)
        val own = worst(all[0], 1, ::changed).first
        val frame = worst(free ?: all[0], 1, ::changed).first
        val full = all[0][frames].count { it }
        val report =
            StringBuilder(
                "$body ${w}x$h stepped $steps: a frame of its own ≤ $own px, on every frame ≤ $frame, full $full\n",
            )
        var bad = false
        for ((k, flip) in flips.withIndex()) {
            val m = all[k + 1]
            val at = flip + 1
            val (after, where) = worst(m, at + 1, ::changed)
            val turn = changed(m[at - 1], m[at])
            // The reversal's frame may carry the step the pour takes there anyway (`steppedMotion`).
            val step = maxOf(frame, changed(all[0][at - 1], all[0][at]))
            report.append(
                "  reversed at ${flip * FRAME.toInt()} ms: its frame $turn (≤ $step), after ≤ $after (frame $where)\n",
            )
            if (turn > step * MARGIN || after > own * MARGIN) bad = true
            // Poured back: what lime is left is the mosaic's few lime squares, never a twentieth of the body.
            if (m.last().count { it } >
                full / 20
            ) {
                bad = true.also { report.append("  not poured back: ${m.last().count { it }} px\n") }
            }
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    @Test
    fun `a pour changed back at any moment pours over the picture it shows`() {
        for (steps in listOf(false, true)) pour(MutantBody.Chip, 112, 32, steps, every = 1)
    }

    @Test
    fun `a card's pour changed back at any moment pours over the picture it shows`() {
        for (steps in listOf(false, true)) pour(MutantBody.Card, 300, 88, steps, every = 2)
    }

    // The slide.

    /** The body's lime: its box, left, top, right, bottom in pixels, or null. */
    private fun box(px: PixelMap): IntArray? {
        var l = Int.MAX_VALUE
        var t = Int.MAX_VALUE
        var r = -1
        var b = -1
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val p = px[x, y]
                if (p.green > .6f && p.blue < .4f) {
                    if (x < l) l = x
                    if (x > r) r = x
                    if (y < t) t = y
                    if (y > b) b = y
                }
            }
        }
        return if (r < 0) null else intArrayOf(l, t, r, b)
    }

    private fun moved(
        a: IntArray?,
        b: IntArray?,
    ): Int =
        if (a == null ||
            b == null
        ) {
            Int.MAX_VALUE
        } else {
            maxOf(abs(a[0] + a[2] - b[0] - b[2]), abs(a[1] + a[3] - b[1] - b[3])) / 2
        }

    private fun stretched(
        a: IntArray?,
        b: IntArray?,
    ): Int =
        if (a == null ||
            b == null
        ) {
            Int.MAX_VALUE
        } else {
            maxOf(abs(a[2] - a[0] - b[2] + b[0]), abs(a[3] - a[1] - b[3] + b[1]))
        }

    private fun slide(steps: Boolean) {
        // Three homes: a switch's run across, and a calendar's run down and back across.
        val homes = listOf(20 to 20, 180 to 20, 100 to 90)
        val frames = (SLIDE_MS / FRAME).toInt() + 3
        val flips = (1..(SLIDE_MS / FRAME).toInt()).toList()
        val runs =
            listOf(Run(listOf(1 to 1, frames to 2, 2 * frames to 0), 3 * frames)) +
                flips.map { Run(listOf(1 to 1, 1 + it to 0), it + frames) } +
                flips.map { Run(listOf(1 to 1, 1 + it to 2), it + frames) }
        val scene: @Composable (Int) -> Unit = { ph ->
            // The slide request as a switch makes it: the id is the place, its offset the way from the place before.
            val last = remember { IntArray(1) }
            val from = homes[last[0]]
            val to = homes[ph]
            val req = remember { arrayOfNulls<SlideRequest>(1) }
            if (req[0] == null || req[0]!!.id != ph) {
                req[0] = SlideRequest(ph, (from.first - to.first).toDouble(), (from.second - to.second).toDouble())
                last[0] = ph
            }
            Box(Modifier.size(260.dp, 140.dp)) {
                MutantSurface(
                    MutantBody.Pill,
                    "pill",
                    Modifier.offset(to.first.dp, to.second.dp).size(56.dp, 30.dp),
                    slide = req[0],
                ) {}
            }
        }
        val free = if (steps) film(false, runs.take(1), ::box, scene)[0] else null
        val all = film(steps, runs, ::box, scene)
        val ownMove = worst(all[0], 1, ::moved).first
        val ownStretch = worst(all[0], 1, ::stretched).first
        val frameMove = worst(free ?: all[0], 1, ::moved).first
        val frameStretch = worst(free ?: all[0], 1, ::stretched).first
        val report =
            StringBuilder(
                "slide stepped $steps: a frame of its own moves ≤ $ownMove px, stretches ≤ $ownStretch; " +
                    "on every frame ≤ $frameMove, ≤ $frameStretch\n",
            )
        var bad = false
        for ((k, m) in all.drop(1).withIndex()) {
            val flip = flips[k % flips.size]
            val to = if (k < flips.size) "back" else "down"
            val at = flip + 1
            val move = moved(m[at - 1], m[at])
            val stretch = stretched(m[at - 1], m[at])
            // The re-aim's frame may carry the step the run takes there anyway (`steppedMotion`).
            val stepMove = maxOf(frameMove, moved(all[0][at - 1], all[0][at]))
            val stepStretch = maxOf(frameStretch, stretched(all[0][at - 1], all[0][at]))
            val afterMove = worst(m, at + 1, ::moved).first
            val afterStretch = worst(m, at + 1, ::stretched).first
            report.append(
                "  re-aimed $to at ${flip * FRAME.toInt()} ms: its frame moves $move, stretches $stretch; " +
                    "after ≤ $afterMove, ≤ $afterStretch\n",
            )
            if (move > stepMove + PX || stretch > stepStretch + PX || afterMove > ownMove + PX ||
                afterStretch > ownStretch + PX
            ) {
                bad = true
            }
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    @Test
    fun `a slide re-aimed at any moment goes on from where the body is drawn`() {
        for (steps in listOf(false, true)) slide(steps)
    }

    // The melt.

    /** The mark's white: its count, its top and its bottom row. */
    private fun mark(px: PixelMap): IntArray {
        var n = 0
        var t = -1
        var b = -1
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                if (px[x, y].red > .5f) {
                    n++
                    if (t < 0) t = y
                    b = y
                }
            }
        }
        return intArrayOf(n, t, b)
    }

    private fun inked(
        a: IntArray,
        b: IntArray,
    ): Int = abs(a[0] - b[0])

    /** How far the mark's edges move; a mark appearing or going counts nothing here, its ink does. */
    private fun edged(
        a: IntArray,
        b: IntArray,
    ): Int = if (a[0] == 0 || b[0] == 0) 0 else maxOf(abs(a[1] - b[1]), abs(a[2] - b[2]))

    private fun melt(steps: Boolean) {
        val frames = (MELT_MS / FRAME).toInt() + 3
        val flips = (1..(MELT_MS / FRAME).toInt()).toList()

        // Phase 1 is the first change, 2 the change back; [inFirst] says which way the first one goes.
        fun runs() =
            listOf(Run(listOf(1 to 1, frames to 2), 2 * frames)) +
                flips.map { Run(listOf(1 to 1, 1 + it to 2), it + frames) }
        val report = StringBuilder()
        var bad = false
        for (inFirst in listOf(true, false)) {
            val scene: @Composable (Int) -> Unit = { ph ->
                val on = if (inFirst) ph == 1 else ph != 1
                Box(Modifier.size(60.dp).background(Color.Black)) {
                    Box(
                        Modifier
                            .padding(18.dp)
                            .size(24.dp)
                            .mutantMelt(on)
                            .background(Color.White),
                    )
                }
            }
            val r = runs()
            val free = if (steps) film(false, r.take(1), ::mark, scene)[0] else null
            val all = film(steps, r, ::mark, scene)
            // The melt-out's own end, where what is left of the mark goes at once, is the design's: not a reversal.
            val end = { a: IntArray, b: IntArray -> a[0] > 0 && b[0] == 0 }
            val ownInk = worst(all[0], 1, ::inked, end).first
            val ownEdge = worst(all[0], 1, ::edged, end).first
            val frameInk = worst(free ?: all[0], 1, ::inked, end).first
            val frameEdge = worst(free ?: all[0], 1, ::edged, end).first
            report.append(
                "melt ${if (inFirst) "in" else "out"} stepped $steps: " +
                    "a frame of its own ≤ $ownInk px, edges ≤ $ownEdge; on every frame ≤ $frameInk, ≤ $frameEdge\n",
            )
            for ((k, flip) in flips.withIndex()) {
                val m = all[k + 1]
                val at = flip + 1
                val ink = inked(m[at - 1], m[at])
                val edge = edged(m[at - 1], m[at])
                val afterInk = worst(m, at + 1, ::inked).first
                val afterEdge = worst(m, at + 1, ::edged).first
                // The reversal's frame may carry the step the melt takes there anyway (`steppedMotion`).
                val stepInk = maxOf(frameInk, inked(all[0][at - 1], all[0][at]))
                val stepEdge = maxOf(frameEdge, edged(all[0][at - 1], all[0][at]))
                report.append(
                    "  reversed at ${flip * FRAME.toInt()} ms: its frame $ink px, edges $edge; after ≤ $afterInk, ≤ $afterEdge\n",
                )
                if (ink > stepInk * MARGIN + PX ||
                    edge > stepEdge + PX ||
                    afterInk > ownInk * MARGIN + PX ||
                    afterEdge > ownEdge + PX
                ) {
                    bad = true
                }
                val rest = if (inFirst) 0 else WHOLE
                assertEquals(rest, m.last()[0], "back at rest, reversed at frame $flip")
            }
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    @Test
    fun `a melt reversed at any moment retraces from where the mark stands`() {
        for (steps in listOf(false, true)) melt(steps)
    }

    private companion object {
        const val D = 2f
        const val FRAME = 16.0
        const val WARM_UP = 12

        /** The slide's and the melt's durations, ms (`motion.slide`, `motion.melt`). */
        const val SLIDE_MS = io.github.youndie.mutant.tokens.MutantMotion.SLIDE_MS
        const val MELT_MS = io.github.youndie.mutant.tokens.MutantMotion.MELT_IN_MS

        /** A frame across a reversal may change 10 % more than the transition's own largest frame: the boil's wobble. */
        const val MARGIN = 1.1

        /** And edges may move two pixels more: the outline's wobble at density 2. */
        const val PX = 2

        /** The whole mark, 24 dp at density 2. */
        const val WHOLE = 48 * 48
    }
}
