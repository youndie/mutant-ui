package io.github.youndie.mutant.surface

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
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
 * B-135: a body the shader draws, half out of a scrolling viewport and scrolled back and forth 30 px a frame, takes its
 * resting ticks from the frames built ahead, and the frame probe says so: it reported every shader tick as built in the
 * frame, whichever it was.
 */
@OptIn(ExperimentalTestApi::class)
class ScrolledShaderTicksAheadTest {
    private fun scrolled(onGpu: Boolean): Pair<Int, Int> {
        var ahead = 0
        var inFrame = 0
        var scrolledAhead = 0
        var scrolledInFrame = 0
        val scroll = ScrollState(0)
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalMutantBuildOffFrame provides true,
                    LocalMutantBodyOnGpu provides onGpu,
                    LocalMutantBodyAsImage provides false,
                    LocalMutantFrameProbe provides { a, _, _ -> if (a) ahead++ else inFrame++ },
                ) {
                    ProvideMutantClock(MutantClock()) {
                        // The card's top 40 dp in the viewport at first.
                        Box(Modifier.size(400.dp, 300.dp)) {
                            Column(Modifier.verticalScroll(scroll)) {
                                Spacer(Modifier.height(260.dp))
                                MutantSurface(
                                    MutantBody.Card,
                                    "card/b135",
                                    Modifier.padding(start = 24.dp).size(328.dp, 80.dp),
                                ) {}
                                Spacer(Modifier.height(900.dp))
                            }
                        }
                    }
                }
            }
            repeat(60) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(6)
            }
            val a0 = ahead
            val f0 = inFrame
            // A second of scrolling, 30 px a frame down then up: the part of the card the window shows changes every frame.
            repeat(60) {
                runOnUiThread { scroll.dispatchRawDelta(if (it % 20 < 10) 30f else -30f) }
                mainClock.advanceTimeByFrame()
                Thread.sleep(6)
            }
            scrolledAhead = ahead - a0
            scrolledInFrame = inFrame - f0
        }
        return scrolledAhead to scrolledInFrame
    }

    @Test
    fun `a scrolled body the shader draws takes its ticks from the frames built ahead`() {
        val (ahead, inFrame) = scrolled(onGpu = true)
        assertTrue(
            ahead >= 6,
            "ticks taken from the frames built ahead while it scrolled: $ahead, in the frame $inFrame",
        )
        assertEquals(0, inFrame, "ticks built in the frame while it scrolled (taken ahead: $ahead)")
    }
}
