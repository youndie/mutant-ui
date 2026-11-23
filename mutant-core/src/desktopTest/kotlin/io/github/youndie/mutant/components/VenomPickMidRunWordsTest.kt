package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
 * B-172: while a run settles on the way to a later pick, the window stands where that run lands, not on the pick, and
 * the words' mask must stand there with it. Each word is read as `SegmentedWordsOnSlimeTest`, `MenuWordsOnSlimeTest`
 * and `DrawerWordsOnSlimeTest` read it: a bare twin (`hidden`, the same keys and clock) gives the ground under each
 * glyph pixel, each pixel's ink is the colour of the two the words take that best explains it as a blend over that
 * ground (core pixels, coverage at least .6), and a word's tenth percentile reads at least 3:1 on what lies under it.
 *
 * Picked 0 → 2, then again: 1 at 160 ms (the run plays back to 0 and runs to 1), 1 at 400 ms (it lands on 2, the window
 * springs there, and runs to 1) and 0 at 608 ms (the window shown on 2 plays back to 0). Every frame from the second
 * pick until the last run has landed, the clock held and moved a 16 ms frame at a time.
 */
@OptIn(ExperimentalTestApi::class)
class VenomPickMidRunWordsTest {
    private val cases = listOf(160L to 1, 400L to 1, 608L to 0)

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

    /**
     * The words under 3:1 through each case, for [face]: its words' boxes in the capture ([words], x and y ranges), and
     * how far a pixel must differ from the bare twin to count as a glyph's ([ink], as its words test sets it).
     */
    private fun unreadable(
        face: String,
        words: List<Pair<IntRange, IntRange>>,
        ink: Float,
    ): List<String> {
        val colors = MutantPalettes.Efir
        val inks = listOf(colors.text, colors.onPrimary)
        val out = mutableListOf<String>()
        var worst = Float.MAX_VALUE
        for ((at, to) in cases) {
            runComposeUiTest {
                var chosen by mutableIntStateOf(0)
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(colors) {
                            // The drawer on a screen's clock of its own, as `DrawerWordsOnSlimeTest` sets it.
                            VenomPickScreenClock(face == "drawer") {
                                // The twins one under the other for the segmented button, side by side for the others,
                                // as their words tests stand them: a body's drips hang into a twin beside it.
                                VenomPickTwins(face == "segmented", Modifier.background(colors.page)) {
                                    for ((tag, hidden) in listOf("real" to false, "bare" to true)) {
                                        when (face) {
                                            "segmented" -> {
                                                Box(Modifier.width(280.dp).testTag(tag)) {
                                                    SegmentedBody(
                                                        listOf("SP · 2 h", "LP · 4 h", "EP · 6 h"),
                                                        chosen,
                                                        {},
                                                        Modifier,
                                                        true,
                                                        "seg",
                                                        "seg/pill",
                                                        hidden,
                                                    )
                                                }
                                            }

                                            "menu" -> {
                                                Box(Modifier.testTag(tag).padding(bottom = 30.dp)) {
                                                    MenuBody(
                                                        listOf("ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ", "СБРОС"),
                                                        chosen,
                                                        {},
                                                        Modifier,
                                                        "menu",
                                                        "menu-pill",
                                                        hidden,
                                                    )
                                                }
                                            }

                                            else -> {
                                                Box(Modifier.size(360.dp, 320.dp).testTag(tag)) {
                                                    DrawerFrame(
                                                        true,
                                                        {},
                                                        listOf("Эфир", "Очередь", "Поиск", "Настройка")
                                                            .mapIndexed { i, n -> MutantDrawerItem("CH 0${i + 1}", n) },
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
                    }
                }
                // The drawer slides in first.
                mainClock.advanceTimeBy(if (face == "drawer") 1_000 else 200)
                var t = 0L
                while (t < at + 1_200) {
                    if (t == 0L) chosen = 2
                    if (t == at) chosen = to
                    mainClock.advanceTimeByFrame()
                    t += 16
                    if (t <= at) continue
                    val real = onNodeWithTag("real").captureToImage().toPixelMap()
                    val bare = onNodeWithTag("bare").captureToImage().toPixelMap()
                    words.forEachIndexed { i, (xs, ys) ->
                        val ratios = mutableListOf<Float>()
                        for (x in xs) {
                            for (y in ys) {
                                val a = real[x, y]
                                val g = bare[x, y]
                                if (abs(a.red - g.red) + abs(a.green - g.green) + abs(a.blue - g.blue) < ink) continue
                                val (c, k) = fit(a, g, inks)
                                if (k >= .6) ratios += contrast(c, g)
                            }
                        }
                        if (ratios.size < 5) return@forEachIndexed
                        val p10 = ratios.sorted()[ratios.size / 10]
                        worst = min(worst, p10)
                        if (p10 < 3f) out += "$to at $at ms: $t ms, word $i ${"%.2f".format(p10)}:1"
                    }
                }
            }
        }
        println("B-172 $face: the worst word's tenth percentile through a settle, ${"%.2f".format(worst)}:1")
        return out
    }

    @Test
    fun `the segmented button's words read on the slime while a run settles`() {
        val cell = (280 - 8) / 3f
        val words =
            (0 until 3).map { i ->
                ((4 + i * cell).toInt() until (4 + (i + 1) * cell).toInt()) to (10 until 38)
            }
        assertEquals(emptyList(), unreadable("segmented", words, .35f), "words under 3:1 on what lies under them")
    }

    @Test
    fun `the menu's words read on the slime while a run settles`() {
        // From the ladder's first step to the row's end, inside the row's 36 dp.
        val words = (0 until 4).map { i -> (6 + 30 until 6 + 204) to (6 + 36 * i until 6 + 36 * (i + 1)) }
        assertEquals(emptyList(), unreadable("menu", words, .1f), "words under 3:1 on what lies under them")
    }

    @Test
    fun `the drawer's words read on the slime while a run settles`() {
        // The row's 180 dp from the window's 12, inside the row's 42 dp, 46 dp apart.
        val words = (0 until 4).map { i -> (12 until 192) to (48 + 46 * i until 48 + 46 * i + 42) }
        assertEquals(emptyList(), unreadable("drawer", words, .1f), "words under 3:1 on what lies under them")
    }
}

/** [content] on a screen's clock of its own where [own] says, else on the theme's. */
@Composable
private fun VenomPickScreenClock(
    own: Boolean,
    content: @Composable () -> Unit,
) {
    if (own) MutantScreenClock(content) else content()
}

/** [content] in a column where [stacked] says, else in a row. */
@Composable
private fun VenomPickTwins(
    stacked: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (stacked) Column(modifier) { content() } else Row(modifier) { content() }
}
