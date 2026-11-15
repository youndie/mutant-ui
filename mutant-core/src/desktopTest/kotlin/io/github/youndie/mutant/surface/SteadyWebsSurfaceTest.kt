package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

/** B-84: a body on a running clock draws steady webs, in the frame and ahead of it; a still keeps the engine's field. */
@OptIn(ExperimentalTestApi::class)
class SteadyWebsSurfaceTest {
    private fun frames(clock: MutantClock): Pair<Int, Int> {
        var steady = 0
        var engine = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalMutantFrameProbe provides { _, _, s -> if (s) steady++ else engine++ },
                ) {
                    ProvideMutantClock(clock) {
                        Box(Modifier.size(360.dp, 120.dp)) {
                            MutantSurface(MutantBody.Card, "card/b84", Modifier.size(328.dp, 80.dp)) {}
                        }
                    }
                }
            }
            repeat(60) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(4)
            }
        }
        return steady to engine
    }

    @Test
    fun `a running body steadies its webs, a frozen one draws the engine's`() {
        val (runSteady, runEngine) = frames(MutantClock())
        val (stillSteady, stillEngine) = frames(MutantClock(startMs = 5_000.0, frozen = true))
        assertEquals(0, runEngine, "engine frames on a running clock ($runSteady steady)")
        assertEquals(0, stillSteady, "steady frames on a frozen clock ($stillEngine engine)")
        kotlin.test.assertTrue(runSteady > 0 && stillEngine > 0, "frames: running $runSteady, frozen $stillEngine")
    }
}
