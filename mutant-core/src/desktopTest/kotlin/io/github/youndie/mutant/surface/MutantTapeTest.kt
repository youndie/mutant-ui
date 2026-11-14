package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
 * reduce motion, steps on the scene's tick while running, and draws in dp at any density. Its pixels against
 * the engine are [BodyParityTest]'s drift frames.
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

    @Test
    fun `a running Tape changes on the scene's tick and not between`() {
        val clock = MutantClock(fps = 24.0)
        val shots = ArrayList<List<Color>>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        ProvideMutantClock(clock) {
                            Box(
                                Modifier
                                    .size(250.dp, 130.dp)
                                    .testTag("s")
                                    .background(Color.Black)
                                    .mutantTape(),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            fun shot() =
                onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
                    List(px.width * px.height) { px[it % px.width, it / px.width] }
                }
            // Stand just past a scene tick, then step under 125 ms at a resting tick of 24 a second.
            repeat(10) { if (clock.nowMs % MutantClock.SCENE_TICK_MS > 20) mainClock.advanceTimeByFrame() }
            val tick = clock.sceneTick
            shots += shot()
            repeat(5) { mainClock.advanceTimeByFrame() }
            assertEquals(tick, clock.sceneTick, "still inside the tick at ${clock.nowMs}")
            shots += shot()
            // Bounded: a tick that never publishes fails here instead of hanging the test JVM.
            repeat(10) { if (clock.sceneTick == tick) mainClock.advanceTimeByFrame() }
            assertNotEquals(tick, clock.sceneTick, "the scene's tick moved by ${clock.nowMs}")
            shots += shot()
        }
        assertEquals(shots[0], shots[1], "the 24 fps resting tick passed, the scene's did not")
        assertNotEquals(shots[1], shots[2], "the scene's tick passed")
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
