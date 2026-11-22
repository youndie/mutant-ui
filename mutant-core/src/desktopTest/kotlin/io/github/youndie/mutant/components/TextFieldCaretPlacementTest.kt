package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-168: where the text field's caret stands against the text as drawn — past the end of a value longer than the field,
 * inside it, and on the frame a letter is typed. Read from pixels at density 1, the caret lit
 * (reduce motion stands it lit).
 */
@OptIn(ExperimentalTestApi::class)
class TextFieldCaretPlacementTest {
    private val text = MutantPalettes.Efir.text

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) < .05f && abs(a.green - b.green) < .05f && abs(a.blue - b.blue) < .05f

    /** The columns holding a run of at least 18 text-coloured pixels: the caret (a glyph of Rubik 15 is shorter). */
    private fun PixelMap.caret(): List<Int> =
        (0 until width).filter { x ->
            var run = 0
            var best = 0
            for (y in 0 until height) {
                run = if (near(this[x, y], text)) run + 1 else 0
                best = maxOf(best, run)
            }
            best >= 18
        }

    /** The columns holding any pixel of [colour] that are not the caret's. */
    private fun PixelMap.ink(colour: Color): List<Int> {
        val caret = caret().toSet()
        return (0 until width).filter { x -> x !in caret && (0 until height).any { y -> near(this[x, y], colour) } }
    }

    private fun ComposeUiTest.shot(): PixelMap = onNodeWithTag("f").captureToImage().toPixelMap()

    private fun ComposeUiTest.press(key: Key) {
        onNode(hasSetTextAction()).performKeyInput { pressKey(key) }
        mainClock.advanceTimeByFrame()
    }

    private val long = "Night to Tuesday from the start of side one, unlabelled, to the end"

    @Test
    fun `past the end of a value longer than the field, the block stands clear of the last letter`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        Box {
                            MutantTextField(long, {}, Modifier.width(240.dp).focusRequester(focus).testTag("f"))
                        }
                    }
                }
            }
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val px = shot()
            val caret = px.caret()
            assertEquals(9, caret.size, "the block: $caret")
            val last = px.ink(text).filter { it < caret.first() }.max()
            assertTrue(
                caret.first() - last - 1 >= 6,
                "the gap between the last letter's ink ($last) and the block (${caret.first()}): the page's 8 dp",
            )
            assertTrue(caret.last() < 240 - 14, "the block inside the field's padding: ${caret.last()}")
            // The text scrolled off the line's start is cut there, at the 14 dp padding.
            val spilt = px.ink(text).filter { it < 14 }
            assertTrue(spilt.isEmpty(), "the value's ink in the padding before its line: $spilt")
        }

    @Test
    fun `inside a value longer than the field, the bar stands at the insertion point`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        Box {
                            MutantTextField(long, {}, Modifier.width(240.dp).focusRequester(focus).testTag("f"))
                        }
                    }
                }
            }
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val block = shot().caret().first()
            // Ten letters back from the end: "the end" and its spaces, some 50 dp of text.
            repeat(10) { press(Key.DirectionLeft) }
            val px = shot()
            val bar = px.caret()
            assertEquals(2, bar.size, "the bar: $bar")
            assertTrue(block - 8 - bar.last() >= 40, "the bar ${bar.first()} stands ten letters before the end, $block")
            val ink = px.ink(text)
            assertTrue(ink.any { it in bar.last() + 1..bar.last() + 4 }, "a letter right after the bar: $bar, $ink")
            assertTrue(ink.any { it in bar.first() - 4 until bar.first() }, "and one right before it: $bar, $ink")
        }

    @Test
    fun `on the frame a letter is typed, the block stands past it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var value by mutableStateOf("Nig")
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        Box {
                            MutantTextField(
                                value,
                                { value = it },
                                Modifier.width(240.dp).focusRequester(focus).testTag("f"),
                            )
                        }
                    }
                }
            }
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            val gaps = ArrayList<Int>()
            for (letter in "ht to Tuesday") {
                onNode(hasSetTextAction()).performTextInput(letter.toString())
                mainClock.advanceTimeByFrame()
                val px = shot()
                val caret = px.caret()
                val last = px.ink(text).filter { it < caret.first() }.maxOrNull() ?: continue
                gaps += caret.first() - last - 1
            }
            assertEquals("Night to Tuesday", value)
            assertTrue(gaps.isNotEmpty() && gaps.all { it >= 6 }, "the gap before the block, each first frame: $gaps")
        }
}
