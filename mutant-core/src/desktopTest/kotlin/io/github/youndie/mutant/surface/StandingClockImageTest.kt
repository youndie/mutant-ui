package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-83: a clock that stands has no next tick to rasterise a body for, so a body that first draws under one is
 * rasterised with its first build and draws that image from its first frame, never its vectors.
 */
@OptIn(ExperimentalTestApi::class)
class StandingClockImageTest {
    /** Every drawn frame of the card, as whether it was drawn from an image. */
    private fun ComposeUiTest.frames(
        clock: MutantClock,
        over: @Composable () -> Unit = {},
    ): List<Boolean> {
        val drawn = ArrayList<Boolean>()
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme {
                    ProvideMutantClock(clock) {
                        Box(Modifier.size(360.dp, 400.dp)) {
                            CompositionLocalProvider(LocalMutantFrameProbe provides { _, image, _ -> drawn += image }) {
                                MutantSurface(MutantBody.Card, "card/b83", Modifier.size(328.dp, 80.dp)) {
                                    BasicText("Кассета 07", Modifier.padding(16.dp).mutantCleanZone())
                                }
                            }
                            over()
                        }
                    }
                }
            }
        }
        repeat(30) {
            mainClock.advanceTimeByFrame()
            // The background is given real time between frames, as a window gives it.
            Thread.sleep(4)
        }
        return drawn
    }

    private fun assertAllImages(drawn: List<Boolean>) {
        assertTrue(drawn.isNotEmpty(), "the card drew")
        assertEquals(List(drawn.size) { true }, drawn, "every frame drawn from an image, the first included")
    }

    @Test
    fun `under a clock held from the start the body draws its image from its first frame`() =
        runComposeUiTest { assertAllImages(frames(MutantClock().apply { holds++ })) }

    @Test
    fun `under a clock paused from the start the body draws its image from its first frame`() =
        runComposeUiTest { assertAllImages(frames(MutantClock().apply { paused = true })) }

    @Test
    fun `behind a dialog open from the start the screen's body draws its image from its first frame`() =
        runComposeUiTest {
            assertAllImages(
                frames(MutantClock()) {
                    MutantDialog(true, "Стереть запись?", {}) { MutantDialogAction("Да", {}) }
                },
            )
        }

    @Test
    fun `under a running clock the first ticks still draw vectors, so the probe can tell them`() =
        runComposeUiTest {
            val drawn = frames(MutantClock())
            assertTrue(drawn.isNotEmpty() && !drawn.first(), "the first frame under a running clock: $drawn")
        }
}
