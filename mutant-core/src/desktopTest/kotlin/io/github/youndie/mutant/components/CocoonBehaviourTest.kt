package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-22: the cocoon's mosaic, its flicker on the scene's tick, the pause, and what a consumer gives it. */
@OptIn(ExperimentalTestApi::class)
class CocoonBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    @Test
    fun `the mosaic is 16 dp blocks inside an edge of 8 dp ones, in the four paints`() {
        val m = CocoonMosaic("1/card/coc0", "card0").apply { at(362, 80, null) }
        val b = m.blocks
        val n = m.paints.size
        assertTrue(n > 0)
        for (i in 0 until n) {
            val (x, y, s) = Triple(b[i * 3], b[i * 3 + 1], b[i * 3 + 2])
            // A cell is an edge one within 16 of the left or top, or 32 of the right or bottom.
            val cx = x - x % 16
            val cy = y - y % 16
            val edge = cx < 16 || cy < 16 || cx + 32 > 362 || cy + 32 > 80
            assertEquals(if (edge) 8 else 16, s, "block $i at $x, $y")
            assertTrue(x % s == 0 && y % s == 0, "on the grid: $x, $y")
            assertTrue(m.paints[i] in 0..3)
        }
        // An 80 dp card: rows 16, 32 and 48 are inner (48 + 32 is not past 80), 0 and 64 edge; 20 of 23 columns inner.
        val inner = (0 until n).count { b[it * 3 + 2] == 16 }
        assertTrue(inner in 50..60, "inner blocks: $inner of 60 places, 94 % drawn")
    }

    @Test
    fun `a tick repaints 6 percent of the blocks, two at least, and the paints build up`() {
        val a = CocoonMosaic("1/card/coc0", "card0").apply { at(362, 80, null) }
        val laid = a.paints.copyOf()
        a.at(362, 80, 40)
        val once = a.paints.copyOf()
        val changed = laid.indices.count { laid[it] != once[it] }
        assertTrue(changed in 1..ceil(laid.size * .06).toInt(), "changed $changed of ${laid.size}")
        a.at(362, 80, 40)
        assertContentEquals(once, a.paints, "a tick is applied once")
        a.at(362, 80, 41)
        // Tick 41 over tick 40 is not tick 41 alone: the page's blocks keep what earlier ticks painted.
        val fresh = CocoonMosaic("1/card/coc0", "card0").apply { at(362, 80, 41) }
        assertFalse(a.paints.contentEquals(fresh.paints), "built up, not redrawn")
        // A thin one changes two blocks a tick: 6 % of a few is less.
        val thin = CocoonMosaic("1/card/coc2", "card2").apply { at(40, 16, null) }
        val before = thin.paints.copyOf()
        thin.at(40, 16, 7)
        assertTrue(before.indices.count { before[it] != thin.paints[it] } <= 2)
        // A small cocoon, eight 8 dp quarters at most, 6 % of which is under one: still two repaints a tick, so over
        // many ticks one changes two blocks.
        val least = CocoonMosaic("1/card/coc9", "card9").apply { at(32, 16, null) }
        assertTrue(least.paints.size in 3..8, "quarters: ${least.paints.size}")
        val most =
            (0L until 60L).maxOf { t ->
                val was = least.paints.copyOf()
                least.at(32, 16, t)
                was.indices.count { was[it] != least.paints[it] }
            }
        assertEquals(2, most, "two blocks repainted on some tick")
    }

    @Test
    fun `a new size lays the blocks out again without re-applying the tick it stands at`() {
        val m = CocoonMosaic("1/card/coc0", "card0").apply { at(362, 80, 40) }
        m.at(200, 80, 40)
        val laid = CocoonMosaic("1/card/coc0", "card0").apply { at(200, 80, null) }
        assertContentEquals(laid.paints, m.paints)
    }

    @Test
    fun `each key flickers its own way, and one key alike`() {
        val a = CocoonMosaic("1/card/coc0", "card0").apply { at(362, 80, 40) }
        val b = CocoonMosaic("1/card/coc1", "card1").apply { at(362, 80, 40) }
        val c = CocoonMosaic("1/card/coc0", "card0").apply { at(362, 80, 40) }
        assertFalse(a.paints.contentEquals(b.paints))
        assertContentEquals(a.paints, c.paints)
    }

    private fun frames(
        reduceMotion: Boolean,
        cleanSignal: Boolean = false,
    ): Int {
        var changes = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion, cleanSignal = cleanSignal) {
                        Box { MutantCocoon(Modifier.size(362.dp, 80.dp).testTag("c")) }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            var last = pixels("c")
            repeat(8) {
                mainClock.advanceTimeBy(125)
                val now = pixels("c")
                if (now != last) changes++
                last = now
            }
        }
        return changes
    }

    @Test
    fun `it flickers on the scene's tick, and stands when paused or under clean signal`() {
        assertTrue(frames(reduceMotion = false) >= 6, "running: a change nearly every tick")
        assertEquals(0, frames(reduceMotion = true), "paused")
        assertEquals(0, frames(reduceMotion = false, cleanSignal = true), "clean signal")
    }

    @Test
    fun `it reads as indeterminate progress`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { MutantTheme { MutantCocoon(Modifier.size(40.dp).testTag("c")) } }
            onNodeWithTag("c").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
        }

    @Test
    fun `it takes the size it is given and none of its own, and draws inside it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Column(Modifier.testTag("all")) {
                            MutantCocoon(Modifier.testTag("none"))
                            MutantCocoon(Modifier.width(0.dp).height(16.dp))
                            MutantCocoon(Modifier.width(120.dp).height(0.dp))
                            Box(Modifier.size(100.dp, 30.dp).testTag("box")) {
                                MutantCocoon(Modifier.size(100.dp, 30.dp))
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val none = onNodeWithTag("none").fetchSemanticsNode().boundsInRoot
            assertEquals(0f, none.width)
            assertEquals(0f, none.height)
            pixels("all")
            // Rounded to 10: the corner pixel stays the ground, the middle of the edge is painted.
            val px = onNodeWithTag("box").captureToImage().toPixelMap()
            assertEquals(0f, px[0, 0].alpha, "the corner")
            assertNotEquals(0f, px[50, 0].alpha + px[50, 1].alpha, "the top edge")
        }
}
