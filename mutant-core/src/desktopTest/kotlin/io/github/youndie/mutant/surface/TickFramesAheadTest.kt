package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-75: under a running clock a resting body's tick frame is built ahead, off the frame, and taken at its tick; what
 * it draws is what a frame built in the tick draws, tick for tick.
 */
@OptIn(ExperimentalTestApi::class)
class TickFramesAheadTest {
    private class Count {
        var ahead = 0
        var inFrame = 0
    }

    private fun ComposeUiTest.card(
        offFrame: Boolean,
        count: Count,
        clock: MutantClock = MutantClock(),
    ) {
        // Before the content: the kit's clock asks for frames for ever, so the content never goes idle.
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalMutantBuildOffFrame provides offFrame,
                // The frame alone: drawing a resting body from an image (B-82) is held by BodyAsImageTest.
                LocalMutantBodyAsImage provides false,
                LocalMutantFrameProbe provides { ahead, _ -> if (ahead) count.ahead++ else count.inFrame++ },
            ) {
                ProvideMutantClock(clock) {
                    Box(Modifier.size(360.dp, 120.dp).testTag("stage")) {
                        MutantSurface(MutantBody.Card, "card/b75", Modifier.size(328.dp, 80.dp)) {
                            BasicText("Кассета 07", Modifier.padding(16.dp).mutantCleanZone())
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.pixels(): List<Color> =
        onNodeWithTag("stage").captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    /**
     * Two seconds of clock in whole frames, the background given real time between frames as a window gives it,
     * and a picture at every frame a tick fell on (the clock steps every 125 ms).
     */
    private fun ComposeUiTest.ticks(): List<List<Color>> {
        val shots = ArrayList<List<Color>>()
        repeat(120) { i ->
            mainClock.advanceTimeByFrame()
            Thread.sleep(4)
            if (i % 8 == 7) shots += pixels()
        }
        return shots
    }

    @Test
    fun `once the field cache settles, every resting tick is taken from the frame built ahead`() =
        runComposeUiTest {
            val count = Count()
            card(offFrame = true, count)
            ticks()
            // The first ticks fill the cache's four boil phases in the frame; from then on every tick is taken.
            assertTrue(
                count.ahead >= 10,
                "ticks taken from the frame built ahead: ${count.ahead}, in the frame ${count.inFrame}",
            )
        }

    @Test
    fun `a body building its frames ahead draws exactly what one building them in the tick draws`() {
        fun run(offFrame: Boolean): Pair<List<List<Color>>, Count> {
            val count = Count()
            var shots = emptyList<List<Color>>()
            runComposeUiTest {
                card(offFrame, count)
                shots = ticks()
            }
            return shots to count
        }
        val (ahead, used) = run(offFrame = true)
        val (inFrame, none) = run(offFrame = false)
        assertEquals(0, none.ahead, "nothing is built ahead with the off-frame builds turned off")
        assertTrue(used.ahead >= 10, "and with them on, the ticks compared were built ahead: ${used.ahead}")
        // The first frame of an off-frame body draws no body yet (B-50); every tick after it is compared.
        for (i in 1 until ahead.size) assertEquals(inFrame[i], ahead[i], "tick ${i + 1}")
    }

    @Test
    fun `a clock that jumps past the tick prepared draws its own time, not the prepared one`() {
        fun run(offFrame: Boolean): List<Color> {
            var shot = emptyList<Color>()
            runComposeUiTest {
                val clock = MutantClock()
                card(offFrame, Count(), clock)
                ticks()
                // Three seconds on at once, as a seek or a resumed screen does: the tick prepared is not this one.
                clock.setTime(clock.nowMs + 3_000)
                mainClock.advanceTimeByFrame()
                shot = pixels()
            }
            return shot
        }
        assertEquals(run(offFrame = false), run(offFrame = true))
    }
}
