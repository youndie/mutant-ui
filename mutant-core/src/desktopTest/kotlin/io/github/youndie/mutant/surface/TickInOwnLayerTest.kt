package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
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
 * B-135: a body's ticks record its own layer and nothing beside it. Drawn into the layer around it, every tick recorded
 * that whole layer again — on the components page every paragraph and pattern swatch of a scrolling column, seen or not.
 */
@OptIn(ExperimentalTestApi::class)
class TickInOwnLayerTest {
    @Test
    fun `a body's ticks do not redraw what is drawn beside it`() =
        runComposeUiTest {
            var beside = 0
            var ticks = 0
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalMutantFrameProbe provides { _, _, _ -> ticks++ },
                ) {
                    ProvideMutantClock(MutantClock()) {
                        Column {
                            Box(
                                Modifier.size(100.dp, 40.dp).drawBehind {
                                    beside++
                                    drawRect(Color.Gray)
                                },
                            )
                            MutantSurface(MutantBody.Card, "card/b135-layer", Modifier.size(328.dp, 80.dp)) {}
                        }
                    }
                }
            }
            repeat(4) { mainClock.advanceTimeByFrame() }
            val before = beside
            val ticked = ticks
            repeat(60) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(4)
            }
            assertTrue(ticks - ticked >= 6, "the body ticked ${ticks - ticked} times in a second")
            assertEquals(
                before,
                beside,
                "the box beside the body was drawn again ${beside - before} times while it ticked",
            )
        }
}
