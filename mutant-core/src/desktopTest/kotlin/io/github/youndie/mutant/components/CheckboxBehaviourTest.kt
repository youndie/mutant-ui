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
import androidx.compose.ui.test.assertIsSelected
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-18: what the checkbox and the radio do, and what a consumer does to them. Clocks are driven by hand. */
@OptIn(ExperimentalTestApi::class)
class CheckboxBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) {
                px[
                    it % px.width, it /
                        px.width,
                ]
            }
        }

    @Test
    fun `a click anywhere on the row toggles the checkbox, and selects the radio`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var checked by mutableStateOf(false)
            var speed by mutableStateOf("LP")
            setContent {
                MutantTheme {
                    Column {
                        MutantCheckbox(checked, { checked = it }, Modifier.testTag("c"), label = "Автотрекинг")
                        MutantRadioButton(
                            speed == "LP",
                            { speed = "LP" },
                            Modifier.testTag("lp"),
                            label = "LP · 4 часа",
                        )
                        MutantRadioButton(
                            speed == "SP",
                            { speed = "SP" },
                            Modifier.testTag("sp"),
                            label = "SP · 2 часа",
                        )
                    }
                }
            }
            onNodeWithTag("c").assertIsOff()
            onNodeWithText("Автотрекинг").performClick()
            mainClock.advanceTimeByFrame()
            assertTrue(checked)
            onNodeWithTag("c").assertIsOn()
            onNodeWithText("SP · 2 часа").performClick()
            mainClock.advanceTimeByFrame()
            assertEquals("SP", speed)
            onNodeWithTag("sp").assertIsSelected()
        }

    private fun melt(reduceMotion: Boolean): Triple<List<Color>, List<Color>, List<Color>> {
        lateinit var start: List<Color>
        lateinit var mid: List<Color>
        lateinit var end: List<Color>
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) { Box { MutantCheckbox(on, {}, Modifier.testTag("c")) } }
                }
            }
            mainClock.advanceTimeByFrame()
            start = pixels("c")
            on = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(160)
            mid = pixels("c")
            mainClock.advanceTimeBy(400)
            end = pixels("c")
        }
        return Triple(start, mid, end)
    }

    @Test
    fun `the mark melts in over 330 ms, and lands at once when paused`() {
        val (start, mid, end) = melt(reduceMotion = false)
        assertNotEquals(start, mid, "under way at 176 ms")
        assertNotEquals(mid, end, "not yet whole at 176 ms")
        val (pStart, pMid, pEnd) = melt(reduceMotion = true)
        assertNotEquals(pStart, pMid, "paused: there at once")
        assertEquals(pMid, pEnd, "paused: and still")
    }

    @Test
    fun `the row is the 48 dp target, disabled it takes no click`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks = 0
            setContent {
                MutantTheme {
                    Column {
                        MutantCheckbox(false, { clicks++ }, Modifier.testTag("c"))
                        MutantRadioButton(false, { clicks++ }, Modifier.testTag("r"), label = "Нельзя", enabled = false)
                    }
                }
            }
            onNodeWithTag("c").assertHeightIsEqualTo(48.dp)
            onNodeWithTag("r").assertHeightIsEqualTo(48.dp).assertIsNotEnabled().performClick()
            assertEquals(0, clicks)
        }

    @Test
    fun `no width, a long label and a doubled font neither throw nor leave the row`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Автотрекинг по всей ленте от начала первой стороны"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column {
                            MutantCheckbox(true, {}, Modifier.width(0.dp).testTag("zero"), label = long)
                            MutantRadioButton(true, {}, Modifier.width(160.dp).testTag("long"), label = long)
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("long").assertIsDisplayed().assertHeightIsEqualTo(48.dp)
            pixels("long")
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                assertTrue(node.boundsInRoot.height <= 48f, "label ${node.boundsInRoot}")
            }
        }
}
