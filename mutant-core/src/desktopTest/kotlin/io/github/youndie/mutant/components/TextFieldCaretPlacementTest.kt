package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
 * inside it, on the frame a letter is typed, and over a placeholder. Read from pixels at density 1, the caret lit
 * (reduce motion stands it lit). B-186: the block 2 dp past the last letter, and the line centred between two letters,
 * at density 1, 2 and 3.
 */
@OptIn(ExperimentalTestApi::class)
class TextFieldCaretPlacementTest {
    private val text = MutantPalettes.Efir.text
    private val secondary = MutantPalettes.Efir.textSecondary

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) < .05f && abs(a.green - b.green) < .05f && abs(a.blue - b.blue) < .05f

    /**
     * The columns holding a run of at least 18 text-coloured pixels a dp: the caret (a glyph of Rubik 15 is shorter).
     */
    private fun PixelMap.caret(density: Float = 1f): List<Int> =
        (0 until width).filter { x ->
            var run = 0
            var best = 0
            for (y in 0 until height) {
                run = if (near(this[x, y], text)) run + 1 else 0
                best = maxOf(best, run)
            }
            best >= 18 * density
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
                caret.first() - last - 1 >= 2,
                "the gap between the last letter's ink ($last) and the block (${caret.first()}): 2 dp (B-186)",
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
            // Centred on the insertion point (B-186), here after the space before «to»: the comma before that space.
            assertTrue(ink.any { it in bar.first() - 8 until bar.first() }, "and one before it: $bar, $ink")
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
            assertTrue(gaps.isNotEmpty() && gaps.all { it >= 2 }, "the gap before the block, each first frame: $gaps")
        }

    @Test
    fun `over a placeholder the caret is the bar, before its first letter`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        Box {
                            MutantTextField(
                                "",
                                {},
                                Modifier.width(240.dp).focusRequester(focus).testTag("f"),
                                prefix = "ПОИСК",
                                placeholder = "Найти запись",
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val first = shot().ink(secondary).filter { it > 20 }.min()
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val px = shot()
            val caret = px.caret()
            assertEquals(2, caret.size, "the bar: $caret")
            assertTrue(caret.last() < first, "the bar ${caret.last()} before the placeholder's first letter $first")
            assertEquals(first, px.ink(secondary).filter { it > 20 }.min(), "the placeholder's first letter uncovered")
        }

    /**
     * The field «Night» at [density], focused, the cursor [left] places back from the end: the columns of the caret,
     * and the runs of columns each letter inks, read as what changes when the words are drawn transparent.
     */
    private fun caretAndLetters(
        density: Float,
        left: Int,
    ): Pair<List<Int>, List<IntRange>> {
        var caret = emptyList<Int>()
        var letters = emptyList<IntRange>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var hidden by mutableStateOf(false)
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    MutantTheme(reduceMotion = true) {
                        Box {
                            TextFieldBody(
                                "Night",
                                {},
                                Modifier.width(240.dp).focusRequester(focus).testTag("f"),
                                null,
                                null,
                                true,
                                false,
                                KeyboardOptions.Default,
                                "field",
                                null,
                                hidden = hidden,
                            )
                        }
                    }
                }
            }
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            // The letters, with the block past them, in the block's rows: the line's.
            val shown = shot()
            hidden = true
            mainClock.advanceTimeByFrame()
            val bare = shot()
            val block = shown.caret(density)
            val rows = (0 until shown.height).filter { near(shown[block.first(), it], text) }
            val inked =
                // From inside the 14 dp padding, where the body's own edge stays out.
                ((10 * density).toInt() until block.first()).filter { x -> rows.any { y -> shown[x, y] != bare[x, y] } }
            letters =
                inked.fold(ArrayList<IntRange>()) { runs, x ->
                    if (runs.isNotEmpty() && runs.last().last == x - 1) {
                        runs[runs.size - 1] = runs.last().first..x
                    } else {
                        runs += x..x
                    }
                    runs
                }
            hidden = false
            mainClock.advanceTimeByFrame()
            repeat(left) { press(Key.DirectionLeft) }
            caret = shot().caret(density)
        }
        return caret to letters
    }

    @Test
    fun `at the end of the line the block stands 2 dp past the last letter`() {
        val found = ArrayList<String>()
        for (d in listOf(1f, 2f, 3f)) {
            val (block, letters) = caretAndLetters(d, 0)
            val gap = block.first() - letters.last().last - 1
            val clear = (2 * d).toInt()
            println("B-186 d$d: block ${block.first()}..${block.last()}, last ink ${letters.last().last}, gap $gap")
            // Past the last letter's box by 2 dp: its ink stops short of its box by at most 1 dp more.
            if (block.size != (9 * d).toInt() || gap !in clear..clear + d.toInt()) {
                found +=
                    "d$d: the block ${block.first()}..${block.last()}, the last ink ${letters.last().last}, gap $gap"
            }
        }
        assertTrue(found.isEmpty(), "a 9 dp block 2 dp past «Night» (B-186):\n" + found.joinToString("\n"))
    }

    @Test
    fun `inside the text the line stands centred between two letters, over no more than half its width of either`() {
        val found = ArrayList<String>()
        // Density 1 joins «h» and «t» into one run of ink; 2 and 3 part them, by 1 and 2 px.
        for (d in listOf(2f, 3f)) {
            val (bar, letters) = caretAndLetters(d, 1)
            if (letters.size != 5) {
                found += "d$d: the letters of «Night» read as ${letters.size} runs: $letters"
                continue
            }
            val h = letters[3]
            val t = letters[4]
            val half = d.toInt()
            val centre = (bar.first() + bar.last()) / 2f
            val gap = (h.last + t.first) / 2f
            val overH = bar.count { it in h }
            val overT = bar.count { it in t }
            println(
                "B-186 d$d: line ${bar.first()}..${bar.last()} centre $centre, h $h, t $t, gap centre $gap, over $overH/$overT",
            )
            if (bar.size != 2 * half || abs(centre - gap) > d / 2 || overH > half || overT > half) {
                found +=
                    "d$d: the line ${bar.first()}..${bar.last()} (centre $centre), «h» $h, «t» $t (gap centre $gap)"
                found += "  over «h» $overH, over «t» $overT"
            }
        }
        assertTrue(found.isEmpty(), "a 2 dp line centred in «Nigh|t» (B-186):\n" + found.joinToString("\n"))
    }
}
