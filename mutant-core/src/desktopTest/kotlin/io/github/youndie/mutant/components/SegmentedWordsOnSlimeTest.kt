package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-168 (F-segmented-01): through a run every word reads on what is drawn under it. A bare twin (`hidden`, the same
 * keys and clock) gives the ground under each glyph pixel; each pixel's ink is the colour, of the two the words take,
 * that best explains it as a blend over that ground. On a timer, as the page turns them, «Эфир»'s pale words stood on
 * the passing lime at 1.06:1 and the black one on the dark body at 1.8:1.
 */
@OptIn(ExperimentalTestApi::class)
class SegmentedWordsOnSlimeTest {
    private val options = listOf("SP · 2 h", "LP · 4 h", "EP · 6 h")

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val x = a.luminance()
        val y = b.luminance()
        return (max(x, y) + .05f) / (min(x, y) + .05f)
    }

    @Test
    fun `every word reads on the slime under it through a run across the button`() =
        runComposeUiTest {
            val colors = MutantPalettes.Efir
            var chosen by mutableIntStateOf(0)
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(colors) {
                        Column(Modifier.background(colors.page)) {
                            for ((tag, hidden) in listOf("real" to false, "bare" to true)) {
                                Box(Modifier.width(280.dp).testTag(tag)) {
                                    SegmentedBody(options, chosen, {}, Modifier, true, "seg", "seg/pill", hidden)
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(200)
            chosen = 2
            val inks = listOf(colors.text, colors.onPrimary)
            val unreadable = mutableListOf<String>()
            repeat(46) { frame ->
                mainClock.advanceTimeByFrame()
                val real = onNodeWithTag("real").captureToImage().toPixelMap()
                val bare = onNodeWithTag("bare").captureToImage().toPixelMap()
                val cell = (280 - 8) / 3f
                for (i in 0 until 3) {
                    val ratios = mutableListOf<Float>()
                    for (x in (4 + i * cell).toInt() until (4 + (i + 1) * cell).toInt()) {
                        for (y in 10 until 38) {
                            val a = real[x, y]
                            val g = bare[x, y]
                            if (abs(a.red - g.red) + abs(a.green - g.green) + abs(a.blue - g.blue) < .35f) continue
                            // The ink and its coverage that best explain the pixel over its ground; core pixels only.
                            val fit =
                                inks
                                    .map { ink ->
                                        val d = listOf(ink.red - g.red, ink.green - g.green, ink.blue - g.blue)
                                        val e = listOf(a.red - g.red, a.green - g.green, a.blue - g.blue)
                                        val den = d.sumOf { (it * it).toDouble() }
                                        val k =
                                            if (den ==
                                                0.0
                                            ) {
                                                0.0
                                            } else {
                                                (
                                                    d.zip(e).sumOf { (p, q) ->
                                                        (p * q).toDouble()
                                                    } / den
                                                ).coerceIn(0.0, 1.0)
                                            }
                                        Triple(ink, k, d.zip(e).sumOf { (p, q) -> abs(q - k * p) })
                                    }.minBy { it.third }
                            if (fit.second >= .6) ratios += contrast(fit.first, g)
                        }
                    }
                    if (ratios.size < 5) continue
                    val p10 = ratios.sorted()[ratios.size / 10]
                    if (p10 < 3f) unreadable += "frame $frame, word $i: ${"%.2f".format(p10)}:1"
                }
            }
            assertEquals(
                emptyList(),
                unreadable,
                "words whose tenth percentile reads under 3:1 on what lies under them",
            )
        }
}
