package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-168 (F-drawer-06): through a run every row's words read on what is drawn under them. A bare twin (`hidden`, the same
 * keys and clock) gives the ground under each glyph pixel; each pixel's ink is the colour, of the two the words take,
 * that best explains it as a blend over that ground. On the page's 340 ms timer «Эфир»'s pale words stood on the
 * passing lime at 1.06:1 and the black ones left behind on the dark body at 1.46:1.
 */
@OptIn(ExperimentalTestApi::class)
class DrawerWordsOnSlimeTest {
    private val items =
        listOf("Эфир", "Очередь", "Поиск", "Настройка").mapIndexed { i, n -> MutantDrawerItem("CH 0${i + 1}", n) }

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val x = a.luminance()
        val y = b.luminance()
        return (max(x, y) + .05f) / (min(x, y) + .05f)
    }

    /** The ink of [inks] and its coverage that best explain [a] as a blend over [g]. */
    private fun fit(
        a: Color,
        g: Color,
        inks: List<Color>,
    ): Pair<Color, Double> =
        inks
            .map { ink ->
                val d = listOf(ink.red - g.red, ink.green - g.green, ink.blue - g.blue)
                val e = listOf(a.red - g.red, a.green - g.green, a.blue - g.blue)
                val den = d.sumOf { (it * it).toDouble() }
                val dot = d.zip(e).sumOf { (p, q) -> (p * q).toDouble() }
                val k = if (den == 0.0) 0.0 else (dot / den).coerceIn(0.0, 1.0)
                Triple(ink, k, d.zip(e).sumOf { (p, q) -> abs(q - k * p) })
            }.minBy { it.third }
            .let { it.first to it.second }

    @Test
    fun `every row's words read on the slime under them through a run across the drawer`() =
        runComposeUiTest {
            val colors = MutantPalettes.Efir
            var chosen by mutableIntStateOf(2)
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(colors) {
                        MutantScreenClock {
                            Row(Modifier.background(colors.page)) {
                                for ((tag, hidden) in listOf("real" to false, "bare" to true)) {
                                    Box(Modifier.size(360.dp, 320.dp).testTag(tag)) {
                                        DrawerFrame(
                                            true,
                                            {},
                                            items,
                                            chosen,
                                            {},
                                            Modifier,
                                            "КАНАЛЫ",
                                            null,
                                            "drawer",
                                            hidden = hidden,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            chosen = 0
            val inks = listOf(colors.text, colors.onPrimary)
            val unreadable = mutableListOf<String>()
            repeat(50) { frame ->
                mainClock.advanceTimeByFrame()
                val real = onNodeWithTag("real").captureToImage().toPixelMap()
                val bare = onNodeWithTag("bare").captureToImage().toPixelMap()
                for (i in items.indices) {
                    val ratios = mutableListOf<Float>()
                    // The row's words: its 180 dp from the window's 12, inside the row's 42 dp, 46 dp apart.
                    for (x in 12 until 192) {
                        for (y in 48 + 46 * i until 48 + 46 * i + 42) {
                            val a = real[x, y]
                            val g = bare[x, y]
                            if (abs(a.red - g.red) + abs(a.green - g.green) + abs(a.blue - g.blue) < .1f) continue
                            val (ink, k) = fit(a, g, inks)
                            if (k >= .6) ratios += contrast(ink, g)
                        }
                    }
                    if (ratios.size < 5) continue
                    val p10 = ratios.sorted()[ratios.size / 10]
                    if (p10 < 3f) unreadable += "frame $frame, row $i: ${"%.2f".format(p10)}:1"
                }
            }
            assertEquals(
                emptyList(),
                unreadable,
                "words whose tenth percentile reads under 3:1 on what lies under them",
            )
        }
}
