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

    /** The tick's dark pixels inside the box (rows 17–31 of the 48 dp row, columns 4–18): the pour is never this dark. */
    private fun ComposeUiTest.tick(): Int =
        onNodeWithTag("c").captureToImage().toPixelMap().let { px ->
            (4 until 18).sumOf { x -> (17 until 31).count { y -> px[x, y].let { it.red + it.green + it.blue < .25f } } }
        }

    private fun melt(reduceMotion: Boolean): Triple<Int, Int, Int> {
        var start = 0
        var mid = 0
        var end = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) { Box { MutantCheckbox(on, {}, Modifier.testTag("c")) } }
                }
            }
            mainClock.advanceTimeByFrame()
            start = tick()
            on = true
            // The melt reveals from the top in 83 ms steps; the tick's top is a third of the way down the box.
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(240)
            mid = tick()
            mainClock.advanceTimeBy(400)
            end = tick()
        }
        return Triple(start, mid, end)
    }

    @Test
    fun `the mark melts in over 330 ms, and lands at once when paused`() {
        val (start, mid, end) = melt(reduceMotion = false)
        assertEquals(0, start, "no tick unchecked")
        assertTrue(mid in 1 until end, "partly revealed at 256 ms: $mid of $end")
        val (_, pMid, pEnd) = melt(reduceMotion = true)
        assertTrue(pMid > 0 && pMid == pEnd, "paused: whole at once: $pMid, $pEnd")
    }

    /** The burnt blocks' pixels (ink at 50 and 25 % on the acid) in the box's bottom right corner, rows 29–33. */
    private fun ComposeUiTest.blocks(): Int =
        onNodeWithTag("c").captureToImage().toPixelMap().let { px ->
            (12 until 20).sumOf { x ->
                (29 until 33).count { y -> px[x, y].let { it.green in .25f..0.7f && it.green > it.red * 1.2f } }
            }
        }

    /** The tick's dark pixels after unchecking: at once, 112 ms in (the melt's first step) and 500 ms in. */
    private fun meltOut(
        reduceMotion: Boolean,
        blocks: MutableList<Int> = ArrayList(),
    ): List<Int> {
        val counts = ArrayList<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(true)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) { Box { MutantCheckbox(on, {}, Modifier.testTag("c")) } }
                }
            }
            mainClock.advanceTimeBy(600)
            counts += tick()
            blocks += blocks()
            on = false
            mainClock.advanceTimeByFrame()
            counts += tick()
            blocks += blocks()
            mainClock.advanceTimeBy(96)
            counts += tick()
            mainClock.advanceTimeBy(400)
            counts += tick()
        }
        return counts
    }

    @Test
    fun `unchecked, the mark melts out over 330 ms, and goes at once when paused`() {
        // B-168: the tick was tinted away the moment the box was unchecked, so its melt-out ran over nothing.
        val blocks = ArrayList<Int>()
        val (whole, first, step, end) = meltOut(reduceMotion = false, blocks)
        assertTrue(whole > 0, "a tick while checked")
        assertEquals(whole, first, "the first frame after unchecking still shows the whole tick")
        assertTrue(blocks[0] > 0 && blocks[1] == blocks[0], "the blocks stay for the melt's first frame: $blocks")
        assertTrue(step in 1 until whole, "partly melted 112 ms in: $step of $whole")
        assertEquals(0, end, "gone once melted out")
        val (pWhole, pFirst, _, pEnd) = meltOut(reduceMotion = true)
        assertTrue(pWhole > 0 && pFirst == 0 && pEnd == 0, "paused: gone at once: $pWhole, $pFirst, $pEnd")
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
