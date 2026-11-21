package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-50, B-149: under a running clock a body's first instance and static layers are built on another thread, the frame's
 * bodies side by side, and the body's first frame waits for its own: it is drawn in its first frame, the picture a body
 * built in its frame draws. Where the frame cannot wait (the browser, here the seam), the first frame draws a stand-in
 * and the body comes from the next. A frozen clock draws in its first frame, as before.
 */
@OptIn(ExperimentalTestApi::class)
class FirstFrameOffTheFrameTest {
    private class Card(
        var width: Int = 328,
    ) {
        var instance: Instance? = null
        var builds = 0
    }

    private var standIns = 0

    private fun ComposeUiTest.card(
        offFrame: Boolean,
        clock: MutantClock = MutantClock(),
        card: Card = Card(),
        waits: Boolean? = null,
    ): Card {
        var width by mutableStateOf(card.width)
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalMutantBuildOffFrame provides offFrame,
                LocalMutantFirstFrameWaits provides waits,
                LocalMutantStandInProbe provides { standIns++ },
                // The first build alone: drawing a resting body from an image (B-82) is held by BodyAsImageTest.
                LocalMutantBodyAsImage provides false,
            ) {
                ProvideMutantClock(clock) {
                    Box(Modifier.size(360.dp, 120.dp).testTag("stage")) {
                        MutantSurface(MutantBody.Card, "card/b50", Modifier.size(width.dp, 80.dp), probe = {
                            if (it !== card.instance) card.builds++
                            card.instance = it
                        }) {
                            BasicText("Кассета 07", Modifier.padding(16.dp).mutantCleanZone())
                        }
                    }
                }
            }
        }
        resize = { width = it }
        return card
    }

    private var resize: (Int) -> Unit = {}

    private fun ComposeUiTest.pixels() =
        onNodeWithTag("stage").captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < .04f

    /** Pixels of the card's fill: the body drawn. */
    private fun List<Color>.fill() = count { near(it, MutantPalettes.Efir.surface) }

    @Test
    fun `under a running clock the body is drawn in its first frame, built once`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val card = card(offFrame = true)
            mainClock.advanceTimeByFrame()
            // (A capture renders a frame of its own in the test runtime, so this frame is read through the probes.)
            assertNotNull(card.instance, "built for the frame that first knows the slot, and drawn in it")
            assertEquals(0, standIns, "no stand-in where the frame waits")
            repeat(20) { mainClock.advanceTimeByFrame() }
            assertEquals(1, card.builds, "built once")
            assertTrue(pixels().fill() > 5000, "the body drawn: ${pixels().fill()} px of its fill")
        }

    @Test
    fun `where the frame cannot wait, the first frame draws a stand-in and the next the body`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val card = card(offFrame = true, waits = false)
            mainClock.advanceTimeByFrame()
            assertNull(card.instance, "nothing built in the frame that first knows the slot")
            assertEquals(1, standIns, "the stand-in drawn in that frame")
            mainClock.advanceTimeByFrame()
            assertNotNull(card.instance, "taken in the next frame")
            assertTrue(pixels().fill() > 5000, "the body drawn: ${pixels().fill()} px of its fill")
            repeat(20) { mainClock.advanceTimeByFrame() }
            assertEquals(1, card.builds, "built once")
            assertEquals(1, standIns, "a stand-in in the first frame alone")
        }

    @Test
    fun `every frame, the first included, is what a body built in its frame draws`() {
        fun frames(offFrame: Boolean): List<List<Color>> {
            var shots = emptyList<List<Color>>()
            runComposeUiTest {
                mainClock.autoAdvance = false
                card(offFrame)
                shots = List(12) { mainClock.advanceTimeByFrame().let { pixels() } }
            }
            return shots
        }
        val off = frames(offFrame = true)
        val inFrame = frames(offFrame = false)
        assertTrue(inFrame[0].fill() > 5000, "built in its frame, the body draws at once")
        for (i in off.indices) assertEquals(inFrame[i], off[i], "frame $i")
    }

    @Test
    fun `a frozen clock draws the body in its first frame`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val card = card(offFrame = true, clock = MutantClock(startMs = 5000.0, frozen = true))
            mainClock.advanceTimeByFrame()
            assertNotNull(card.instance)
            assertTrue(pixels().fill() > 5000, "drawn in the first frame")
        }

    @Test
    fun `a later rebuild, a new size, is drawn in its own frame`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val card = card(offFrame = true)
            repeat(3) { mainClock.advanceTimeByFrame() }
            val first = card.instance
            resize(300)
            mainClock.advanceTimeByFrame()
            assertTrue(card.instance !== first, "rebuilt for the new size")
            assertEquals(300.0, card.instance!!.slot.w)
            assertTrue(pixels().fill() > 5000, "never a frame without its body")
        }
}
