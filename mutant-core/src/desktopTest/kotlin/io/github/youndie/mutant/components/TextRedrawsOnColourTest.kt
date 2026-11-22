package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-169: a word whose colour alone changes — the style's or a span's — draws the new colour in the first frame after the
 * change, with the clock held and nothing else redrawing its layer, and without laying the words out again (B-164).
 */
@OptIn(ExperimentalTestApi::class)
class TextRedrawsOnColourTest {
    @Test
    fun `a word whose style colour alone changes draws it in the next frame without a remeasure`() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            var tint by mutableStateOf(RED)
            var laidOut = 0
            val onLayout: (TextLayoutResult) -> Unit = { laidOut++ }
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    Box(Modifier.testTag("t").padding(8.dp)) {
                        MutantText("Слово", style = TextStyle(color = tint, fontSize = 32.sp), onTextLayout = onLayout)
                    }
                }
            }
            settle()
            assertTrue(count(shot(), RED) > 50, "the word is drawn red before the change")
            val before = laidOut
            for ((turn, to) in listOf(BLUE, RED, BLUE).withIndex()) {
                val from = if (to == BLUE) RED else BLUE
                tint = to
                mainClock.advanceTimeByFrame()
                val px = shot()
                assertEquals(0, count(px, from), "turn ${turn + 1}: no pixel of the old colour in the first frame")
                assertTrue(count(px, to) > 50, "turn ${turn + 1}: the new colour drawn in the first frame")
            }
            assertEquals(before, laidOut, "a change of colour alone measures nothing again")
        }
    }

    @Test
    fun `a span whose colour alone changes draws it in the next frame`() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            var tint by mutableStateOf(RED)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    Box(Modifier.testTag("t").padding(8.dp)) {
                        MutantText(
                            buildAnnotatedString {
                                append("найдено ")
                                withStyle(SpanStyle(color = tint)) { append("слово") }
                            },
                            style = TextStyle(color = GREY, fontSize = 32.sp),
                        )
                    }
                }
            }
            settle()
            assertTrue(count(shot(), RED) > 50, "the span is drawn red before the change")
            val grey = count(shot(), GREY)
            for ((turn, to) in listOf(BLUE, RED, BLUE).withIndex()) {
                val from = if (to == BLUE) RED else BLUE
                tint = to
                mainClock.advanceTimeByFrame()
                val px = shot()
                assertEquals(
                    0,
                    count(px, from),
                    "turn ${turn + 1}: no pixel of the span's old colour in the first frame",
                )
                assertTrue(count(px, to) > 50, "turn ${turn + 1}: the span's new colour drawn in the first frame")
                assertEquals(grey, count(px, GREY), "turn ${turn + 1}: the words around the span keep their colour")
            }
        }
    }

    @Test
    fun `a text button disabled in place greys its label in the next frame, and turns back enabled`() {
        for (reduceMotion in listOf(true, false)) {
            for (palette in listOf(MutantPalettes.Efir, MutantPalettes.Kopiya)) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    var enabled by mutableStateOf(true)
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(D)) {
                            MutantTheme(palette, reduceMotion = reduceMotion) {
                                MutantScreenClock {
                                    Box(
                                        Modifier.testTag("t").padding(8.dp),
                                    ) { MutantTextButton("Сбросить", {}, enabled = enabled) }
                                }
                            }
                        }
                    }
                    settle()
                    val word = onNodeWithText("Сбросить", useUnmergedTree = true).assertIsDisplayed().getBoundsInRoot()
                    val box =
                        intArrayOf(
                            (word.left.value * D).toInt(),
                            (word.top.value * D).toInt(),
                            (word.right.value * D).toInt(),
                            (word.bottom.value * D).toInt(),
                        )
                    val at = "reduce motion $reduceMotion, ${if (palette == MutantPalettes.Efir) "Эфир" else "Копия"}"
                    assertTrue(count(shot(), palette.textAccent, box) > 50, "$at: the label is in the accent enabled")
                    for ((turn, on) in listOf(false, true, false).withIndex()) {
                        val from = if (on) palette.textDisabled else palette.textAccent
                        val to = if (on) palette.textAccent else palette.textDisabled
                        enabled = on
                        mainClock.advanceTimeByFrame()
                        val px = shot()
                        assertEquals(0, count(px, from, box), "$at, turn ${turn + 1}: no pixel of the old colour")
                        assertTrue(count(px, to, box) > 50, "$at, turn ${turn + 1}: the new colour in the first frame")
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.settle() =
        repeat(10) {
            mainClock.advanceTimeByFrame()
        }

    private fun ComposeUiTest.shot(): PixelMap = onNodeWithTag("t").captureToImage().toPixelMap()

    /** The opaque pixels of [c] — a glyph's solid ink, not its anti-aliased edge — inside [box] (all of [px] by default). */
    private fun count(
        px: PixelMap,
        c: Color,
        box: IntArray = intArrayOf(0, 0, px.width, px.height),
    ): Int {
        var n = 0
        for (y in maxOf(0, box[1]) until minOf(px.height, box[3])) {
            for (x in maxOf(0, box[0]) until minOf(px.width, box[2])) {
                val p = px[x, y]
                if (p.alpha > .9f && abs(p.red - c.red) + abs(p.green - c.green) + abs(p.blue - c.blue) < .06f) n++
            }
        }
        return n
    }

    private companion object {
        const val D = 2f
        val RED = Color(0xFFE0182B)
        val BLUE = Color(0xFF1A3CE0)
        val GREY = Color(0xFF7A7A7A)
    }
}
