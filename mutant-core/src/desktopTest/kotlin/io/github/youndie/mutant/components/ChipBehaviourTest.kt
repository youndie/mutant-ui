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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-17: the chip toggles, pours, and turns its label after the pour; what a consumer does to it. */
@OptIn(ExperimentalTestApi::class)
class ChipBehaviourTest {
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
    fun `a click toggles it, and the semantics say so`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(false)
            setContent { MutantTheme { MutantChip("Избранное", on, { on = !on }, Modifier.testTag("c")) } }
            onNodeWithTag("c").assertIsNotSelected().performClick()
            mainClock.advanceTimeByFrame()
            assertTrue(on)
            onNodeWithTag("c").assertIsSelected()
        }

    /** The darkest pixel of the label's band: the ink once the label has turned, the text's light colour before. */
    private fun ComposeUiTest.labelInk(): Float =
        onNodeWithTag("c").captureToImage().toPixelMap().let { px ->
            (0 until px.width)
                .flatMap { x ->
                    (20 until 28).map { y -> px[x, y] }
                }.minOf { it.red + it.green + it.blue }
        }

    private fun turns(reduceMotion: Boolean): List<Float> {
        val inks = ArrayList<Float>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(
                        reduceMotion = reduceMotion,
                    ) { Box { MutantChip("Избранное", on, {}, Modifier.testTag("c")) } }
                }
            }
            mainClock.advanceTimeByFrame()
            on = true
            mainClock.advanceTimeByFrame()
            inks += labelInk()
            mainClock.advanceTimeBy(96)
            inks += labelInk()
            mainClock.advanceTimeBy(160)
            inks += labelInk()
        }
        return inks
    }

    @Test
    fun `selected, the label turns to the ink 200 ms after the change, at once when paused`() {
        val ink = MutantPalettes.Efir.onPrimary.let { it.red + it.green + it.blue }
        val (first, mid, late) = turns(reduceMotion = false)
        assertTrue(first > ink + .3f && mid > ink + .3f, "still light at 16 and 112 ms: $first, $mid")
        assertTrue(abs(late - ink) < .1f, "the ink by 272 ms: $late")
        val (now, _, _) = turns(reduceMotion = true)
        assertTrue(abs(now - ink) < .1f, "paused, at once: $now")
    }

    @Test
    fun `the target is 48 dp tall around a 32 dp body, and at least 48 wide`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { MutantTheme { MutantChip("Да", false, {}, Modifier.testTag("c")) } }
            onNodeWithTag("c").assertHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
        }

    @Test
    fun `given no width, a chip is as wide as its label, not its parent`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box(
                            Modifier.width(400.dp),
                        ) { MutantChip("Избранное", false, {}, Modifier.testTag("c")) }
                    }
                }
            }
            val w = onNodeWithTag("c").fetchSemanticsNode().boundsInRoot.width
            assertTrue(w in 100f..110f, "the label (about 72) and 16 dp each side: $w")
        }

    @Test
    fun `a disabled chip takes no click`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks = 0
            setContent {
                MutantTheme {
                    MutantChip(
                        "Избранное",
                        false,
                        { clicks++ },
                        Modifier.testTag("c"),
                        enabled = false,
                    )
                }
            }
            onNodeWithTag("c").assertIsNotEnabled().performClick()
            assertEquals(0, clicks)
        }

    @Test
    fun `no width, a long label and a doubled font neither throw nor leave the body`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Избранное с начала первой стороны без подписи"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column {
                            MutantChip(long, true, {}, Modifier.width(0.dp).testTag("zero"))
                            MutantChip(long, false, {}, Modifier.width(140.dp).testTag("long"))
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("long").assertIsDisplayed().assertHeightIsEqualTo(48.dp)
            pixels("long")
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                assertTrue(node.boundsInRoot.height <= 32f, "label ${node.boundsInRoot} inside the 32 dp body")
            }
        }
}
