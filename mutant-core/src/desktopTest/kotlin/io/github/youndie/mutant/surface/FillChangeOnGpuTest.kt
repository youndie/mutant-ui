package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-166: a body the shader draws, in the browser's way (its ticks built ahead, the latest one done drawn, its body kept
 * in the pool when it leaves), comes back with another fill — a chip chosen, the page around it composed anew — and draws
 * the new fill from its first frame back: it drew the old one again, from the brush built ahead before it left, until
 * its next tick's.
 */
@OptIn(ExperimentalTestApi::class)
class FillChangeOnGpuTest {
    private val old = Color(0xFF3B4580)
    private val new = Color(0xFFB6FF1A)

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < .08f

    @BeforeTest
    fun empty() = BodyPool.clear()

    @AfterTest
    fun emptyAgain() = BodyPool.clear()

    @Test
    fun `a body back from the pool with another fill draws it from its first frame`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var fill by mutableStateOf(old)
            var shown by mutableStateOf(true)
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalMutantFirstFrameWaits provides false,
                    LocalMutantPoolBodies provides true,
                    LocalMutantBodyOnGpu provides true,
                    LocalMutantBodyAsImage provides false,
                ) {
                    ProvideMutantClock(MutantClock()) {
                        Box(Modifier.size(200.dp, 120.dp).testTag("stage")) {
                            if (shown) {
                                MutantSurface(
                                    MutantBody.Chip,
                                    "chip/b166",
                                    Modifier.size(120.dp, 40.dp),
                                    fill = fill,
                                ) {}
                            }
                        }
                    }
                }
            }
            // Its ticks built ahead with the old fill, a build between frames each.
            repeat(60) {
                Thread.sleep(3)
                mainClock.advanceTimeByFrame()
            }
            shown = false
            mainClock.advanceTimeByFrame()
            fill = new
            shown = true
            var oldFrames = 0
            repeat(6) {
                mainClock.advanceTimeByFrame()
                if (near(onNodeWithTag("stage").captureToImage().toPixelMap()[60, 20], old)) oldFrames++
            }
            assertTrue(oldFrames == 0, "the old fill drawn in $oldFrames of its first 6 frames back")
        }
}
