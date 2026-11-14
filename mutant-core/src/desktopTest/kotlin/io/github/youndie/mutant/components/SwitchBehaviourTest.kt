package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-19: what the switch does, and what a consumer does to it. Clocks are driven by hand. */
@OptIn(ExperimentalTestApi::class)
class SwitchBehaviourTest {
    @Test
    fun `a click on the row switches it, and the semantics say so`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(false)
            setContent { MutantTheme { MutantSwitch(on, { on = it }, Modifier.testTag("s"), label = "Чистый сигнал") } }
            onNodeWithTag("s").assertIsOff()
            onNodeWithText("Чистый сигнал").performClick()
            mainClock.advanceTimeByFrame()
            assertTrue(on)
            onNodeWithTag("s").assertIsOn()
        }

    /** The thumb's centre of mass along the track: the mean x of its phosphor pixels in the track's rows. */
    private fun ComposeUiTest.thumbX(): Float =
        onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
            val xs = (0 until 60).flatMap { x -> (13 until 35).filter { y -> px[x, y].isPhosphor() }.map { x } }
            xs.average().toFloat()
        }

    private fun Color.isPhosphor() = red > .85f && green > .9f && blue > .85f

    private fun slide(reduceMotion: Boolean): List<Float> {
        val xs = ArrayList<Float>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) { Box { MutantSwitch(on, {}, Modifier.testTag("s")) } }
                }
            }
            mainClock.advanceTimeByFrame()
            xs += thumbX()
            on = true
            mainClock.advanceTimeByFrame()
            xs += thumbX()
            mainClock.advanceTimeBy(160)
            xs += thumbX()
            mainClock.advanceTimeBy(600)
            xs += thumbX()
        }
        return xs
    }

    @Test
    fun `switched on, the thumb slides right over time, and lands at once when paused`() {
        val (off, first, mid, on) = slide(reduceMotion = false)
        // The mean of near-white pixels counts the bodies' highlights too: it moves about 16 of the thumb's 24 dp.
        assertTrue(on - off > 12, "the thumb travels right: $off → $on")
        assertTrue(first < off + 6 && mid in (off + 2)..(on - 2), "it slides, not jumps: $off, $first, $mid, $on")
        val (pOff, pFirst, _, pOn) = slide(reduceMotion = true)
        assertTrue(pFirst - pOff > 12 && kotlin.math.abs(pFirst - pOn) < 1, "paused, at once: $pOff, $pFirst, $pOn")
    }

    @Test
    fun `the row is the 48 dp target, disabled it takes no click`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks = 0
            setContent {
                MutantTheme {
                    Column {
                        MutantSwitch(false, { clicks++ }, Modifier.testTag("a"))
                        MutantSwitch(true, { clicks++ }, Modifier.testTag("d"), label = "Нельзя", enabled = false)
                    }
                }
            }
            onNodeWithTag("a").assertHeightIsEqualTo(48.dp)
            onNodeWithTag("d").assertHeightIsEqualTo(48.dp).assertIsNotEnabled().performClick()
            assertEquals(0, clicks)
        }

    @Test
    fun `no width, a long label and a doubled font neither throw nor leave the row`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Чистый сигнал для долгих сеансов и чувствительных к мерцанию"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column {
                            MutantSwitch(true, {}, Modifier.width(0.dp).testTag("zero"), label = long)
                            MutantSwitch(false, {}, Modifier.width(180.dp).testTag("long"), label = long)
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("long").assertIsDisplayed().assertHeightIsEqualTo(48.dp)
            onNodeWithTag("long").captureToImage()
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                assertTrue(node.boundsInRoot.height <= 48f, "label ${node.boundsInRoot}")
            }
        }
}
