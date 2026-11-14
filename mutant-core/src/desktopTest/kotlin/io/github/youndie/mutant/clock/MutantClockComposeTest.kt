package io.github.youndie.mutant.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-07: under one screen's clock two bodies step on the same tick; a paused or frozen clock draws no new
 * frame; with no reader the clock asks for no frames.
 */
@OptIn(ExperimentalTestApi::class)
class MutantClockComposeTest {
    @Test
    fun `two readers on one screen draw the same tick on every frame`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val clock = MutantClock()
            val seen = List(2) { ArrayList<Long>() }
            setContent {
                ProvideMutantClock(clock) {
                    Column {
                        repeat(2) { i ->
                            val c = readMutantClock()
                            Canvas(Modifier.size(10.dp)) { seen[i] += c.restTick }
                        }
                    }
                }
            }
            repeat(40) { mainClock.advanceTimeByFrame() }
            assertEquals(seen[0], seen[1], "both readers drew the same ticks")
            assertTrue(seen[0].last() >= 4, "the clock moved: ${seen[0]}")
            // One draw per tick, not one per frame: 40 frames of 16 ms are about five ticks of 125 ms.
            assertTrue(seen[0].size <= 8, "draws: ${seen[0].size} for ${seen[0].last()} ticks")
        }

    @Test
    fun `a paused clock draws no new frame and resumes where it stopped`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val clock = MutantClock()
            var draws = 0
            setContent {
                ProvideMutantClock(clock) {
                    val c = readMutantClock()
                    Canvas(Modifier.size(10.dp)) {
                        c.restTick
                        draws++
                    }
                }
            }
            repeat(20) { mainClock.advanceTimeByFrame() }
            clock.paused = true
            mainClock.advanceTimeByFrame()
            val time = clock.nowMs
            val before = draws
            repeat(60) { mainClock.advanceTimeByFrame() }
            assertEquals(time, clock.nowMs, "time stood while paused")
            assertEquals(before, draws, "no draw while paused")
            clock.paused = false
            repeat(20) { mainClock.advanceTimeByFrame() }
            assertTrue(
                clock.nowMs > time && clock.nowMs < time + 400,
                "resumed from ${"%.1f".format(time)}: ${"%.1f".format(clock.nowMs)}",
            )
        }

    @Test
    fun `a frozen clock and a clock nobody reads ask for no frames`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val frozen = MutantClock(startMs = STILL_MS, frozen = true)
            val unread = MutantClock()
            var draws = 0
            setContent {
                ProvideMutantClock(unread) {}
                ProvideMutantClock(frozen) {
                    val c = readMutantClock()
                    Canvas(Modifier.size(10.dp)) {
                        c.restTick
                        draws++
                    }
                }
            }
            repeat(30) { mainClock.advanceTimeByFrame() }
            assertEquals(STILL_MS, frozen.nowMs)
            assertEquals(1, draws, "drawn once, at 5000 ms")
            assertEquals(0.0, unread.nowMs, "a clock with no reader did not run")
        }
}
