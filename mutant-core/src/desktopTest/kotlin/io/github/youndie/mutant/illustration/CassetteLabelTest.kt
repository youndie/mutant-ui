package io.github.youndie.mutant.illustration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-194: the cassette's label is the app's word, and it fits the label's 164 dp (the designer's answer 19): at 20 where
 * it fits, at 16 where only that fits, and three rules with no word where neither does.
 */
@OptIn(ExperimentalTestApi::class)
class CassetteLabelTest {
    /** The size [word] is laid out at on the cassette, read from its text layout. */
    private fun ComposeUiTest.sizeOf(word: String): Float {
        val results = mutableListOf<TextLayoutResult>()
        val node = onNodeWithText(word, useUnmergedTree = true).fetchSemanticsNode()
        node.config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        return results
            .single()
            .layoutInput.style.fontSize.value
    }

    private fun cassette(
        word: String?,
        density: Density = Density(1f),
        check: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                MutantTheme(reduceMotion = true) {
                    Box(Modifier.size(216.dp, 132.dp).testTag("s")) {
                        if (word == null) {
                            MutantIllustration(MutantIllustrationKind.Cassette)
                        } else {
                            MutantIllustration(MutantIllustrationKind.Cassette, cassetteLabel = word)
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        check()
    }

    @Test
    fun `a word that fits draws at 20 and one wider than 164 dp at 20 draws at 16`() {
        // Their widths at 20 and 16, Handjet tracked 2 dp: БЕЗ ПОДПИСИ 108, UNTITLED 72; NO LABEL ON THIS TAPE 183, 155.
        cassette(null) { assertEquals(20f, sizeOf("БЕЗ ПОДПИСИ"), "the default") }
        cassette("UNTITLED") { assertEquals(20f, sizeOf("UNTITLED")) }
        cassette("NO LABEL ON THIS TAPE") { assertEquals(16f, sizeOf("NO LABEL ON THIS TAPE")) }
        // Measured in dp, whatever the density and the font scale, as the label draws.
        val dense = Density(2f, fontScale = 2f)
        cassette("UNTITLED", dense) { assertEquals(20f, sizeOf("UNTITLED"), "density 2") }
        cassette("NO LABEL ON THIS TAPE", dense) { assertEquals(16f, sizeOf("NO LABEL ON THIS TAPE"), "density 2") }
        // The empty state hands its word to the art.
        runComposeUiTest {
            setContent {
                MutantTheme(reduceMotion = true) {
                    MutantEmptyState(MutantIllustrationKind.Cassette, "Empty", cassetteLabel = "UNTITLED")
                }
            }
            assertEquals(20f, sizeOf("UNTITLED"), "in an empty state")
        }
    }

    /**
     * Inside the label (the art's 18, 14, 180 × 38), clear of its outline: rows 20…46, columns 40…176. A row is a rule
     * where nine in ten of its pixels are the ink; anything else of the ink there is a glyph.
     */
    private fun ComposeUiTest.labelInk(): Pair<List<Int>, Int> {
        val px = onNodeWithTag("s").captureToImage().toPixelMap()
        val columns = 40..176
        val dark = { x: Int, y: Int -> px[x, y].let { it.red < .2f && it.green < .2f && it.blue < .2f } }
        val rules = (20..46).filter { y -> columns.count { x -> dark(x, y) } >= columns.count() * 9 / 10 }
        val stray = (20..46).filter { it !in rules }.sumOf { y -> columns.count { x -> dark(x, y) } }
        return rules to stray
    }

    @Test
    fun `a word too wide at 16 draws three rules and no glyph`() {
        // 211 dp at 16.
        val long = "THIS TAPE HAS NO LABEL AT ALL"
        for (word in listOf(long, " ")) {
            cassette(word) {
                onAllNodesWithText(long, useUnmergedTree = true).assertCountEquals(0)
                val (rules, stray) = labelInk()
                // Three rules of 2 dp, 8 dp apart: rows 24–25, 32–33, 40–41 at density 1.
                assertEquals(listOf(24, 25, 32, 33, 40, 41), rules, "'$word'")
                assertTrue(stray < 20, "'$word': $stray pixels of ink off the rules")
            }
        }
        // The control: the design's word is no rule, and is a few hundred pixels of ink.
        cassette(null) {
            val (rules, stray) = labelInk()
            assertEquals(emptyList(), rules)
            assertTrue(stray > 200, "the default's glyphs: $stray")
        }
    }
}
