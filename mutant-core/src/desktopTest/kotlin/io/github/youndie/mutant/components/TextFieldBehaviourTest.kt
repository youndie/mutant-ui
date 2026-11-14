package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-20: what the text field does, and what a consumer does to it. Clocks are driven by hand. */
@OptIn(ExperimentalTestApi::class)
class TextFieldBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) {
                px[
                    it % px.width, it /
                        px.width,
                ]
            }
        }

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) < .05f && abs(a.green - b.green) < .05f && abs(a.blue - b.blue) < .05f

    /** The caret's left edge: the first column holding 18 text-coloured pixels in a run, or null when it is not shown. */
    private fun ComposeUiTest.caret(tag: String): Int? =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            val text = MutantPalettes.Efir.text
            (0 until px.width).firstOrNull { x ->
                var run = 0
                var best = 0
                for (y in 0 until px.height) {
                    run = if (near(px[x, y], text)) run + 1 else 0
                    best = maxOf(best, run)
                }
                best >= 18
            }
        }

    @Test
    fun `typing reaches the caller, and focus shows on the body`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var value by mutableStateOf("")
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box {
                            MutantTextField(
                                value,
                                { value = it },
                                Modifier.width(300.dp).testTag("f"),
                                prefix = "ПОИСК",
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val rest = pixels("f")
            onNode(hasSetTextAction()).performClick()
            mainClock.advanceTimeByFrame()
            onNode(hasSetTextAction()).performTextInput("Ночь")
            mainClock.advanceTimeByFrame()
            assertEquals("Ночь", value)
            assertNotEquals(rest, pixels("f"), "focused: brackets, calm, the caret")
        }

    private fun blink(reduceMotion: Boolean): List<Boolean> {
        val shown = ArrayList<Boolean>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) {
                        Box {
                            MutantTextField(
                                "Ночь на вт",
                                {},
                                Modifier.width(300.dp).focusRequester(focus).testTag("f"),
                            )
                        }
                    }
                }
            }
            runOnIdle { focus.requestFocus() }
            repeat(24) {
                mainClock.advanceTimeBy(125)
                shown += caret("f") != null
            }
        }
        return shown
    }

    @Test
    fun `the caret blinks five scene ticks in eight, and stands lit when paused`() {
        // Samples every 125 ms of test time, which frames of 16 ms do not divide: a tick is now and then seen twice
        // or not at all. What holds is the blink's shape: lit runs of five at most, dark runs of three, both seen.
        val running = blink(reduceMotion = false).joinToString("") { if (it) "T" else "F" }
        assertTrue("TTTTTT" !in running && "FFFF" !in running, "runs of at most 5 lit and 3 dark: $running")
        assertTrue("TTTTT" in running && "FFF" in running, "a whole lit and a whole dark run: $running")
        assertTrue(blink(reduceMotion = true).all { it }, "paused: always lit")
    }

    @Test
    fun `the caret stays inside the field however long the text`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        Box {
                            MutantTextField(
                                "Ночь на вторник с начала первой стороны без подписи и до конца",
                                {},
                                Modifier.width(200.dp).focusRequester(focus).testTag("f"),
                            )
                        }
                    }
                }
            }
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            val x = caret("f")
            assertTrue(x != null && x + 9 <= 200 - 14, "the caret inside the field's padding: $x")
        }

    @Test
    fun `a disabled field takes no input, an error field says so`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var value by mutableStateOf("")
            setContent {
                MutantTheme {
                    Column {
                        MutantTextField(value, { value = it }, Modifier.testTag("d"), enabled = false)
                        MutantTextField("99", {}, Modifier.testTag("e"), prefix = "КАНАЛ", isError = true)
                    }
                }
            }
            onNodeWithTag("d", useUnmergedTree = true).assertIsDisplayed()
            assertEquals("", value)
            onNodeWithTag("e").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        }

    @Test
    fun `no width, long text and a doubled font neither throw nor leave the field`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column {
                            MutantTextField(
                                "Ночь на вторник с начала первой стороны",
                                {},
                                Modifier.testTag("free"),
                                prefix = "ПОИСК",
                            )
                            MutantTextField("", {}, Modifier.width(0.dp).testTag("zero"), placeholder = "Найти запись")
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("free").assertIsDisplayed().assertHeightIsEqualTo(52.dp).assertWidthIsAtLeast(200.dp)
            pixels("free")
        }
}
