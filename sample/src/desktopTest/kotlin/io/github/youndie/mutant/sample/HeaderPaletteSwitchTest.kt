package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-175 (F-chip-02): the header's palette switch pours no chip. From the switch's first frame the chosen palette's chip
 * reads its word on the new palette's fill, and «RU», which the switch does not touch, shows the new palette's fill —
 * both ways, at density 2, every frame read from the drawn page with the clock advanced by hand. The Tape is off
 * («Чистый сигнал»): its noise over the words is no part of what a pour draws.
 */
@OptIn(ExperimentalTestApi::class)
class HeaderPaletteSwitchTest {
    @Test
    fun `the header's chips stand in the new palette from the switch's first frame, both ways`() {
        val s = SampleStrings(SampleLanguage.English)
        val out = ArrayList<String>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            val settings = SampleSettings(reduceMotion = false, cleanSignal = true)
            setContent { CompositionLocalProvider(LocalDensity provides Density(D)) { MutantUiSample(settings) } }
            repeat(WARM_UP) { mainClock.advanceTimeByFrame() }
            for (i in 1..2 * FRAMES) {
                if (i == 1 || i == FRAMES + 1) settings.print = !settings.print
                mainClock.advanceTimeByFrame()
                val px = onNodeWithTag("sample-root").captureToImage().toPixelMap()
                val chosen = if (settings.print) s.themeKopiya else s.themeEfir
                val old = if (settings.print) MutantPalettes.Efir else MutantPalettes.Kopiya
                val word = wordContrast(px, box(chosen))
                val ru = box("RU")
                val ruOld =
                    count(
                        px,
                        Rect(ru.left - 12 * D, ru.top - 6 * D, ru.right + 12 * D, ru.bottom + 6 * D),
                        old.surfaceVariant,
                    )
                if (word < WORDS || ruOld > 0) {
                    out += "frame $i, ${if (settings.print) "to «Копия»" else "to «Эфир»"}: «$chosen» " +
                        "${"%.2f".format(word)}:1, $ruOld px of the old fill in «RU»"
                }
            }
        }
        // Before: «Copy» 1.81:1 for 3 frames, «RU» showed «Эфир»'s fill for 160 ms; back, «On air» 6.16:1 at its worst.
        // Now 16.68:1 and 9.75:1 from the switch's frame, no old fill; the bound is 4.5:1.
        assertTrue(out.isEmpty(), out.joinToString("\n"))
    }

    /**
     * The header's word [text], in pixels: the topmost of its kind drawn (the page below has a «Copy» caption too, and
     * a node off the page has empty bounds).
     */
    private fun ComposeUiTest.box(text: String): Rect =
        onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .filter { it.width > 0 && it.height > 0 }
            .minBy { it.top }

    /** The word's strongest ink against the ground 6 dp left of its box, at its middle. */
    private fun wordContrast(
        px: PixelMap,
        box: Rect,
    ): Float {
        val ground = px[(box.left - 6 * D).roundToInt(), box.center.y.roundToInt()].copy(alpha = 1f)
        var best = 1f
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                best = maxOf(best, contrast(px[x, y].copy(alpha = 1f), ground))
            }
        }
        return best
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (maxOf(la, lb) + .05f) / (minOf(la, lb) + .05f)
    }

    private fun count(
        px: PixelMap,
        r: Rect,
        c: Color,
    ): Int {
        var n = 0
        for (y in maxOf(0, r.top.roundToInt()) until minOf(px.height, r.bottom.roundToInt())) {
            for (x in maxOf(0, r.left.roundToInt()) until minOf(px.width, r.right.roundToInt())) {
                val p = px[x, y]
                if (abs(p.red - c.red) + abs(p.green - c.green) + abs(p.blue - c.blue) < .12f) n++
            }
        }
        return n
    }

    private companion object {
        const val D = 2f
        const val WARM_UP = 24

        /** 448 ms: past a 32 dp chip's 420 ms pour. */
        const val FRAMES = 28
        const val WORDS = 4.5f
    }
}
