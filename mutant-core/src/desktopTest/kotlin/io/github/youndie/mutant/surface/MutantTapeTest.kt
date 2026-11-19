package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-13: the Tape's band passes on the engine's cycle, the Tape is off under «Чистый сигнал», stands still under
 * reduce motion, and draws in dp at any density. B-136: its band moves and its head's noise flickers on every frame, the
 * grain keeps the scene's tick, the content under it is not drawn again for it, and under the theme's stepped motion all
 * of it steps on the scene's tick. B-141: its frames record its own layer only, not the layer of whatever holds it. Its
 * pixels against the engine are [BodyParityTest]'s drift frames.
 */
@OptIn(ExperimentalTestApi::class)
class MutantTapeTest {
    @Test
    fun `the band enters at the bottom, leaves 30 dp above the top in 2100 ms, and returns every 7 s`() {
        assertEquals(130f, tapeBandTop(0.0, 130f))
        assertEquals(50f, tapeBandTop(1050.0, 130f), "halfway: 130 − 0.5 × 160")
        assertEquals(-30f, tapeBandTop(2099.99, 130f))
        assertNull(tapeBandTop(2100.0, 130f), "gone from 30 % of the cycle")
        assertNull(tapeBandTop(6999.0, 130f))
        assertEquals(130f, tapeBandTop(7000.0, 130f), "back at the bottom on the next cycle")
        assertEquals(tapeBandTop(1050.0, 130f), tapeBandTop(71050.0, 130f))
        var last = tapeBandTop(0.0, 800f)!!
        for (ms in 1 until 2100) {
            val top = tapeBandTop(ms.toDouble(), 800f)!!
            assertTrue(top <= last && last - top <= 1f, "steady at $ms: $last → $top")
            last = top
        }
    }

    private fun pixels(
        density: Float = 1f,
        ground: Color = MutantPalettes.Efir.background,
        content: @androidx.compose.runtime.Composable (Modifier) -> Unit,
        between: (androidx.compose.ui.test.ComposeUiTest.() -> Unit)? = null,
    ): List<List<Color>> {
        val shots = ArrayList<List<Color>>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    content(Modifier.size(250.dp, 130.dp).testTag("s").background(ground))
                }
            }
            mainClock.advanceTimeByFrame()

            fun shot() =
                onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
                    List(px.width * px.height) { px[it % px.width, it / px.width] }
                }
            shots += shot()
            if (between != null) {
                between()
                shots += shot()
            }
        }
        return shots
    }

    @Test
    fun `clean signal draws no Tape at all`() {
        val bare = pixels(content = { m -> MutantTheme(cleanSignal = true) { Box(m) } }).single()
        val clean = pixels(content = { m -> MutantTheme(cleanSignal = true) { Box(m.mutantTape()) } }).single()
        val taped = pixels(content = { m -> MutantTheme(cleanSignal = false) { Box(m.mutantTape()) } }).single()
        assertEquals(bare, clean, "the Tape under clean signal")
        assertNotEquals(bare, taped, "the Tape draws without clean signal")
    }

    private fun stepsOver3s(reduceMotion: Boolean): Boolean {
        val (first, later) =
            pixels(
                content = { m -> MutantTheme(reduceMotion = reduceMotion) { Box(m.mutantTape()) } },
                between = { repeat(3000 / 16) { mainClock.advanceTimeByFrame() } },
            )
        return first != later
    }

    @Test
    fun `the Tape stands still under reduce motion and steps while the clock runs`() {
        assertTrue(stepsOver3s(reduceMotion = false), "a running clock steps the grain and the noise")
        assertTrue(!stepsOver3s(reduceMotion = true), "paused, the Tape keeps its picture")
    }

    /**
     * One frame of a running Tape: its pixels; how many times so far the Tape itself was drawn, the content under it, the
     * node holding it (a draw before `mutantTape()` on that node) and a sibling beside it; the clock's time and tick.
     */
    private class TapeFrame(
        val px: List<Color>?,
        val draws: Int,
        val contentDraws: Int,
        val holderDraws: Int,
        val siblingDraws: Int,
        val ms: Double,
        val tick: Long,
    ) {
        /** Rows [from] until [to] of the 250 dp wide picture. */
        fun rows(
            from: Int,
            to: Int,
        ) = px!!.subList(from * 250, to * 250)
    }

    /**
     * Runs a 250 × 130 dp Tape, under a sibling of its size in a column, as the sample's Tape section stands in its page,
     * on a clock of 24 resting ticks a second from [startMs] for [frames] frames of 16 ms, with the theme's
     * [steppedMotion], and records each frame's [TapeFrame] counts; [capture] takes its pixels too (a separate run counts
     * draws, so a capture never draws the layer it counts).
     */
    private fun runTape(
        startMs: Double,
        frames: Int,
        steppedMotion: Boolean = false,
        capture: Boolean,
    ): List<TapeFrame> {
        val clock = MutantClock(fps = 24.0, startMs = startMs)
        var draws = 0
        var contentDraws = 0
        var holderDraws = 0
        var siblingDraws = 0
        val out = ArrayList<TapeFrame>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false, steppedMotion = steppedMotion) {
                        ProvideMutantClock(clock) {
                            CompositionLocalProvider(LocalMutantTapeProbe provides { draws++ }) {
                                Column {
                                    Box(Modifier.size(250.dp, 130.dp).drawBehind { siblingDraws++ })
                                    Box(
                                        Modifier
                                            .size(250.dp, 130.dp)
                                            .testTag("s")
                                            .background(Color.Black)
                                            .drawBehind { holderDraws++ }
                                            .mutantTape(),
                                    ) { Box(Modifier.matchParentSize().drawBehind { contentDraws++ }) }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            repeat(frames) {
                mainClock.advanceTimeByFrame()
                val px =
                    if (!capture) {
                        null
                    } else {
                        onNodeWithTag("s").captureToImage().toPixelMap().let { m ->
                            List(m.width * m.height) { m[it % m.width, it / m.width] }
                        }
                    }
                out += TapeFrame(px, draws, contentDraws, holderDraws, siblingDraws, clock.nowMs, clock.sceneTick)
            }
        }
        return out
    }

    @Test
    fun `away from the band the head's noise flickers on every frame and the grain on the scene's tick`() {
        // From 2200 ms the band is gone until 7000: the grain over the screen, the head's noise in its bottom 12 dp.
        val shots = runTape(startMs = 2200.0, frames = 40, capture = true)
        val draws = runTape(startMs = 2200.0, frames = 40, capture = false)
        var within = 0
        var across = 0
        for (k in 1 until shots.size) {
            val (a, b) = shots[k - 1] to shots[k]
            assertTrue(tapeBandTop(b.ms, 130f) == null, "the band is away at ${b.ms} ms")
            assertNotEquals(a.rows(118, 130), b.rows(118, 130), "the head's noise stood, ${a.ms} → ${b.ms} ms")
            assertTrue(draws[k].draws > draws[k - 1].draws, "the Tape not drawn again, ${a.ms} → ${b.ms} ms")
            if (a.tick == b.tick) {
                within++
                assertEquals(a.rows(0, 117), b.rows(0, 117), "the grain moved inside the tick ${a.tick}, ${a.ms} ms")
            } else {
                across++
                assertNotEquals(a.rows(0, 117), b.rows(0, 117), "the grain stood as the tick passed, ${a.ms} ms")
            }
        }
        assertTrue(within >= 20 && across >= 4, "frames inside a tick $within, ticks passed $across")
    }

    @Test
    fun `the content under the Tape is not drawn again for the Tape's frames`() {
        val draws = runTape(startMs = 2200.0, frames = 40, capture = false)
        assertTrue(
            draws.last().draws - draws.first().draws >= 39,
            "the Tape drawn on every frame: ${draws.map { it.draws }}",
        )
        assertEquals(draws.first().contentDraws, draws.last().contentDraws, "the content drawn again with the Tape")
    }

    @Test
    fun `the Tape's frames draw neither the node holding it nor its siblings again`() {
        // B-140: drawn in its holder's layer, a Tape inside a page's column recorded the whole column on every frame.
        for (startMs in listOf(200.0, 2200.0)) {
            val draws = runTape(startMs = startMs, frames = 40, capture = false)
            val (first, last) = draws.first() to draws.last()
            assertTrue(
                last.draws - first.draws >= 39,
                "from $startMs ms the Tape drawn on every frame: ${draws.map { it.draws }}",
            )
            assertEquals(
                first.holderDraws,
                last.holderDraws,
                "from $startMs ms the node holding the Tape drawn again with it",
            )
            assertEquals(first.siblingDraws, last.siblingDraws, "from $startMs ms a sibling drawn again with the Tape")
        }
    }

    @Test
    fun `while the band passes the Tape moves and is drawn on every frame`() {
        // From 200 ms to about 840 the band rises 1.2 dp a frame over 130 dp: every frame moves it by a whole dp or two.
        val shots = runTape(startMs = 200.0, frames = 40, capture = true)
        val draws = runTape(startMs = 200.0, frames = 40, capture = false)
        var within = 0
        for (k in 1 until shots.size) {
            val (a, b) = shots[k - 1] to shots[k]
            assertTrue(tapeBandTop(b.ms, 130f) != null, "the band still passes at ${b.ms} ms")
            assertNotEquals(a.px, b.px, "the band stood still, ${a.ms} → ${b.ms} ms")
            assertTrue(draws[k].draws > draws[k - 1].draws, "not drawn again, ${a.ms} → ${b.ms} ms")
            if (a.tick == b.tick) within++
        }
        assertTrue(within >= 20, "frames inside a scene tick: $within")
    }

    @Test
    fun `under stepped motion the band steps with the grain on the scene's tick`() {
        val shots = runTape(startMs = 200.0, frames = 40, steppedMotion = true, capture = true)
        var within = 0
        var across = 0
        for (k in 1 until shots.size) {
            val (a, b) = shots[k - 1] to shots[k]
            if (a.tick == b.tick) {
                within++
                assertEquals(a.px, b.px, "inside the tick ${a.tick}, ${a.ms} → ${b.ms} ms")
            } else {
                across++
                assertNotEquals(a.px, b.px, "the scene's tick passed, ${a.ms} → ${b.ms} ms")
            }
        }
        assertTrue(within >= 20 && across >= 4, "frames inside a tick $within, ticks passed $across")
    }

    @Test
    fun `scanlines darken one dp in three by 7·5 percent in Efir and are absent in Kopiya`() {
        for ((colors, alpha) in listOf(MutantPalettes.Efir to .075f, MutantPalettes.Kopiya to 0f)) {
            // At 3000 ms the band is away. On white, 7.5 % and 10 % are 6 levels apart; a row's most common colour is the ground under its scanline alone.
            val clock = MutantClock(startMs = 3000.0, frozen = true)
            val px =
                pixels(ground = Color.White, content = { m ->
                    MutantTheme(colors = colors) { ProvideMutantClock(clock) { Box(m.mutantTape()) } }
                }).single()

            fun common(row: Int) =
                (0 until 250)
                    .map { px[row * 250 + it] }
                    .groupingBy { it }
                    .eachCount()
                    .maxBy { it.value }
                    .key
            for (row in 0 until 9) {
                val a = if (row % 3 == 0) alpha else 0f
                val expected = 1f - a // a white ground
                assertTrue(
                    abs(common(row).red - expected) <= 1.5f / 255,
                    "row $row: ${common(row).red} against $expected",
                )
            }
        }
    }

    @Test
    fun `the band lands in dp at density 2`() {
        val clock = MutantClock(startMs = 1050.0, frozen = true)
        val px =
            pixels(density = 2f, content = { m ->
                MutantTheme(colors = MutantPalettes.Efir) {
                    ProvideMutantClock(clock) { Box(m.mutantTape()) }
                }
            }).single()
        val width = 500

        fun brightShare(row: Int) = (0 until width).count { px[row * width + it].green > .3f }.toDouble() / width
        // The band's top border is the 1 dp at y = 50: rows 100 and 101 at density 2, the row above it is not.
        assertTrue(
            brightShare(100) > .9 && brightShare(101) > .9,
            "border rows: ${brightShare(100)}, ${brightShare(101)}",
        )
        assertTrue(brightShare(99) < .3, "above the border: ${brightShare(99)}")
    }
}
