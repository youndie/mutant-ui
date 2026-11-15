package io.github.youndie.mutant.components

import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.HANDJET_ROWS_ABOVE
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.extentOf
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-31: the v1 page's two segmented buttons. */
class SegmentedParityTest {
    private class Cell(
        val name: String,
        val options: List<String>,
        val enabled: Boolean,
    )

    private val cells =
        listOf(
            Cell("Rest", listOf("SP · 2 ч", "LP · 4 ч", "EP · 6 ч"), true),
            Cell("Disabled", listOf("SP · 2 ч", "LP · 4 ч"), false),
        )

    /** A word over its bare twin: where its ink lies (every side within a pixel, the top row exactly) and its colour. */
    private fun word(
        r: DesignParity.Result,
        b: DesignParity.Result,
        threshold: Int,
        x: IntRange,
    ): Pair<Boolean, String> {
        fun inked(
            px: IntArray,
            bare: IntArray,
        ) = extentOf(r.width, r.height) { i -> i % r.width in x && channelGap(px[i], bare[i]) > threshold }

        fun ink(
            px: IntArray,
            bare: IntArray,
        ) = (0 until r.width * r.height)
            .filter { it % r.width in x }
            .maxBy { channelGap(px[it], bare[it]) }
            .let { px[it] }
        val kit = inked(r.actual, b.actual)
        val page = inked(r.reference, b.reference)
        val colour = channelGap(ink(r.actual, b.actual), ink(r.reference, b.reference))
        val ok =
            kit != null && page != null && kit.near(page, px = 1, topPx = 0, above = 0) &&
                colour <= ComponentParity.INK_COLOUR
        return ok to "kit $kit page $page colour $colour"
    }

    @Test
    fun `the page's segmented buttons - body and window whole, each label where the page puts it`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in cells) {
                val threshold = if (cell.enabled) TEXT_INK else ComponentParity.WEAK_INK
                val c =
                    ComponentParity.split("MutantSegmented_${cell.name}", theme, threshold) { m, hidden ->
                        SegmentedBody(cell.options, 0, {}, m, cell.enabled, "seg", "seg-pill", hidden = hidden)
                    }
                val r = c.result
                val b = c.bare!!
                val bareOk = b.pixels <= (RESIDUAL[b.stem] ?: ComponentParity.FLOOR_PIXELS)
                // The crop starts 12 px left of the body; the segments lie 4 px in (none in the disabled one).
                val inset = if (cell.enabled) 4f else 0f
                val w = (if (cell.enabled) 312f else 208f) - 2 * inset
                val n = cell.options.size
                val words =
                    (0 until n).map { i ->
                        val from = (12 + c.placed.first + inset + i * w / n).toInt()
                        val to = (12 + c.placed.first + inset + (i + 1) * w / n).toInt()
                        word(r, b, threshold, from until to)
                    }
                lines += "${r.stem}: bare ${b.pixels} px (max ${b.maxChannel}); " +
                    words.joinToString("; ") { it.second } + "; placed ${c.placed}"
                if (!bareOk || words.any { !it.first }) off += r.stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-15 and never to be raised: Chrome draws the speckle as an SVG pattern,
         * rasterised once and resampled, where the kit draws each square exactly — single pixels on the speckle's 40 dp
         * grid, over the window in «Эфир» and over the body under «Копия»'s slip, as the button's (research §1.15).
         */
        val RESIDUAL = mapOf("MutantSegmented_RestBare_Efir" to 17, "MutantSegmented_RestBare_Kopiya" to 31)
    }
}
