package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-191, the designer's answers 13 and 14: under the pause the indeterminate band stands centred on the track, so the
 * track is never empty; under `steppedMotion` the band and the reel step at the 125 ms tick, 12 dp and 30° a tick; by
 * default they move on every frame at that speed (D22).
 */
@OptIn(ExperimentalTestApi::class)
class ProgressPauseAndStepsTest {
    private val acid = MutantPalettes.Efir.primary
    private val black = Color(0xFF05060C)

    /** How much of [c] is the solid acid over the track's black, 0…1, by the green channel (the widest apart). */
    private fun acidness(c: Color) = ((c.green - black.green) / (acid.green - black.green)).coerceIn(0f, 1f)

    /** The columns of the middle row whose pixel is the solid acid, all but a few hundredths. */
    private fun solid(px: PixelMap): List<Int> = (0 until px.width).filter { acidness(px[it, px.height / 2]) > .95f }

    /**
     * Where the band's solid half ends, in pixels from the track's start, to a fraction of a pixel: the last solid column
     * and the share of acid in the column after it, so an edge between pixels reads where it stands.
     */
    private fun solidEnd(px: PixelMap): Float {
        val last = solid(px).last()
        val next = if (last + 1 < px.width) acidness(px[last + 1, px.height / 2]) else 0f
        return last + 1 + next
    }

    /**
     * The angles of the reel's three holes about its centre, degrees: the centroid of each run of connected black pixels
     * inside the spool — an area each, not an edge.
     */
    private fun holeAngles(px: PixelMap): List<Double> {
        val cx = px.width / 2.0
        val cy = px.height / 2.0
        val r = px.width / 2.0 * 0.85
        val dark = HashSet<Pair<Int, Int>>()
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                if (hypot(x + .5 - cx, y + .5 - cy) < r && px[x, y].red < .5f) dark += x to y
            }
        }
        val holes = ArrayList<List<Pair<Int, Int>>>()
        while (dark.isNotEmpty()) {
            val seed = dark.first()
            dark -= seed
            val hole = arrayListOf(seed)
            var i = 0
            while (i < hole.size) {
                val (x, y) = hole[i++]
                for ((nx, ny) in listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
                    if (dark.remove(nx to ny)) hole += nx to ny
                }
            }
            holes += hole
        }
        assertEquals(3, holes.size, "three holes")
        return holes.map { h ->
            atan2(h.sumOf { it.second + .5 - cy } / h.size, h.sumOf { it.first + .5 - cx } / h.size) * 180 / PI
        }
    }

    /** [d] degrees brought into −180…180. */
    private fun wrap(d: Double): Double = ((d % 360 + 540) % 360) - 180

    /** The reel's turn from [a] to [b], degrees, clockwise: each hole matched to the nearest one, the holes 112° apart. */
    private fun turn(
        a: List<Double>,
        b: List<Double>,
    ): Double = a.map { x -> b.map { y -> wrap(y - x) }.minBy { abs(it) } }.average()

    /** The widths in [all] that do not show the band centred under the pause, at density [d], [batch] at a time. */
    private fun offCentre(
        d: Float,
        all: List<Int>,
        batch: Int,
    ): List<String> {
        val off = ArrayList<String>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var first by mutableIntStateOf(0)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(d)) {
                    MutantTheme(
                        colors = if (d == 1f) MutantPalettes.Efir else MutantPalettes.Kopiya,
                        reduceMotion = true,
                    ) {
                        Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in first until minOf(first + batch, all.size)) {
                                Box { MutantLinearProgress(Modifier.width(all[i].dp).testTag("w${all[i]}")) }
                            }
                        }
                    }
                }
            }
            while (first < all.size) {
                mainClock.advanceTimeBy(100)
                for (i in first until minOf(first + batch, all.size)) {
                    val w = all[i]
                    val px = onNodeWithTag("w$w").captureToImage().toPixelMap()
                    assertEquals((w * d).toInt(), px.width, "$w dp at $d")
                    val cols = solid(px)
                    if (cols.isEmpty()) {
                        off += "$w dp at $d: no solid acid"
                        continue
                    }
                    // Centred, the band's middle — where its solid half starts — stands at the track's middle, and the
                    // solid half reaches 48 dp on, or the track's end where the track is under 96 dp.
                    val start = cols.first().toFloat()
                    val end = solidEnd(px)
                    val wantStart = px.width / 2f
                    val wantEnd = minOf(px.width.toFloat(), px.width / 2f + 48 * d)
                    if (abs(start - wantStart) > 1.5f || abs(end - wantEnd) > 1.5f) {
                        off += "$w dp at $d: solid $start..$end, want $wantStart..$wantEnd"
                    }
                }
                first += batch
            }
        }
        return off
    }

    @Test
    fun `under reduce motion the band stands centred on the track at every width from 48 to 600 dp`() {
        val off = offCentre(1f, (48..600).toList(), 30) + offCentre(2f, (48..500 step 13).toList(), 15)
        assertTrue(off.isEmpty(), "not centred: $off")
    }

    @Test
    fun `stepped, the band and the reel step 12 dp and 30 degrees a 125 ms tick, by default every frame`() {
        class Run(
            val ends: List<Float>,
            val holes: List<List<Double>>,
        )

        fun run(stepped: Boolean): Run {
            val ends = ArrayList<Float>()
            val holes = ArrayList<List<Double>>()
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    // At density 2, so the holes' centroids are read from four times the pixels.
                    CompositionLocalProvider(LocalDensity provides Density(2f)) {
                        MutantTheme(steppedMotion = stepped) {
                            Column {
                                MutantLinearProgress(Modifier.width(400.dp).testTag("band"))
                                MutantCircularProgress(Modifier.testTag("reel"))
                            }
                        }
                    }
                }
                // A second in, the band's solid half ends inside the 400 dp track, and stays there for the next second.
                mainClock.advanceTimeBy(1000)
                repeat(64) {
                    mainClock.advanceTimeByFrame()
                    ends += solidEnd(onNodeWithTag("band").captureToImage().toPixelMap()) / 2f
                    holes += holeAngles(onNodeWithTag("reel").captureToImage().toPixelMap())
                }
            }
            return Run(ends, holes)
        }

        // 64 frames of 16 ms: 1024 ms, 8.2 ticks, so 8 tick boundaries pass.
        val stepped = run(stepped = true)
        val moves = stepped.ends.zipWithNext { a, b -> b - a }
        val turns = stepped.holes.zipWithNext { a, b -> turn(a, b) }
        val bandAt = moves.indices.filter { abs(moves[it]) > .25f }
        val reelAt = turns.indices.filter { abs(wrap(turns[it])) > 1.5 }
        assertEquals(8, bandAt.size, "the band moves at the 8 ticks only: $moves")
        assertEquals(bandAt, reelAt, "the reel turns at the same frames as the band")
        assertTrue(bandAt.all { abs(moves[it] - 12f) < .5f }, "12 dp a tick: ${bandAt.map { moves[it] }}")
        assertTrue(reelAt.all { abs(turns[it] - 30) < 2 }, "30° a tick: ${reelAt.map { turns[it] }}")
        // Ticks 125 ms apart: about 7.8 frames of 16 ms between steps, so 7 or 8.
        assertTrue(bandAt.zipWithNext { a, b -> b - a }.all { it in 7..8 }, "a tick apart: $bandAt")

        val every = run(stepped = false)
        val everyMoves = every.ends.zipWithNext { a, b -> b - a }
        val everyTurns = every.holes.zipWithNext { a, b -> turn(a, b) }
        // 12 dp and 30° a 125 ms tick spread over its frames: 1.536 dp and 3.84° every 16 ms.
        assertTrue(everyMoves.all { abs(it - 1.536f) < .35f }, "the band every frame: $everyMoves")
        assertTrue(everyTurns.all { abs(it - 3.84) < 1.2 }, "the reel every frame: $everyTurns")
        // The same speed: over the whole run as far as the steps went, give or take the part-tick before the first one.
        val travel = every.ends.last() - every.ends.first()
        assertTrue(abs(travel - 63 * 1.536f) < 1f, "the band's travel at 96 dp a second: $travel")
    }
}
