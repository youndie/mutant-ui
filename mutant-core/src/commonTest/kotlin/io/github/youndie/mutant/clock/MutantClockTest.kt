package io.github.youndie.mutant.clock

import io.github.youndie.mutant.gen.boilPhase
import io.github.youndie.mutant.gen.goldens.goldenClockSteps
import io.github.youndie.mutant.gen.goldens.goldenClockTicks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-07: the kit clock steps, quantises and trembles as the engine does. The expected values are the
 * engine's own expressions, read out of `mutant-engine.js` and evaluated in Node (`GoldenClock.kt`), at
 * every tick boundary of the first six seconds give or take a fraction of a millisecond.
 */
class MutantClockTest {
    @Test
    fun `the boil phase and both ticks match the engine around every boundary`() {
        val rows = goldenClockTicks
        assertTrue(rows.size / 6 > 400, "rows: ${rows.size / 6}")
        for (i in rows.indices step 6) {
            val t = rows[i]
            assertEquals(rows[i + 1].toInt(), boilPhase(t, hot = false), "rest phase at $t")
            assertEquals(rows[i + 2].toInt(), boilPhase(t, hot = true), "hot phase at $t")
            val clock = MutantClock(startMs = t, frozen = true)
            assertEquals(rows[i + 3], clock.restTimeMs(), "resting tick at $t")
            assertEquals(rows[i + 4], clock.hotTimeMs(), "hot tick at $t")
            assertEquals(
                rows[i + 5],
                MutantClock(fps = 24.0, startMs = t, frozen = true).restTimeMs(),
                "24 fps tick at $t",
            )
            // The same ticks reached by moving a running clock, the path every frame takes.
            val moved = MutantClock().apply { setTime(t) }
            assertEquals(rows[i + 3], moved.restTimeMs(), "resting tick of a moved clock at $t")
            assertEquals(rows[i + 4], moved.hotTimeMs(), "hot tick of a moved clock at $t")
        }
    }

    @Test
    fun `a frame steps by the engine's dt clamped to a second and 16 ms first`() {
        val rows = goldenClockSteps
        for (i in rows.indices step 3) {
            val now = rows[i]
            val last = rows[i + 1]
            val clock = MutantClock()
            if (last != 0.0) clock.onFrame((last * 1e6).toLong())
            val before = clock.nowMs
            clock.onFrame((now * 1e6).toLong())
            assertEquals(rows[i + 2], clock.nowMs - before, 1e-6, "dt at now $now, last $last")
        }
    }

    @Test
    fun `speed scales the step and a pause stops time without losing the frame`() {
        val clock = MutantClock(speed = 2.0)
        clock.onFrame(0L)
        assertEquals(32.0, clock.nowMs, 1e-9, "the first frame's 16 ms at speed 2")
        clock.paused = true
        clock.onFrame(100_000_000L)
        assertEquals(32.0, clock.nowMs, 1e-9, "paused")
        clock.paused = false
        clock.onFrame(116_000_000L)
        assertEquals(64.0, clock.nowMs, 1e-9, "resumed with the frame's own 16 ms, not the pause's")
    }

    @Test
    fun `a frozen clock never moves`() {
        val clock = MutantClock(startMs = STILL_MS, frozen = true)
        clock.onFrame(0L)
        clock.onFrame(1_000_000_000L)
        assertEquals(STILL_MS, clock.nowMs)
        assertEquals(40L, clock.restTick)
    }
}
