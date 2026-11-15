package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.HANDJET_ROWS_ABOVE
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-30: the v1 page's two sliders. */
class SliderParityTest {
    private class Cell(
        val name: String,
        val label: String,
        val width: Int,
        val left: Int,
        val value: String,
        val enabled: Boolean,
    )

    private val cells =
        listOf(
            // The page's handles stand at 174 of 266 and 52 of 186, the values it writes in them +3 and −4.
            Cell("Rest", "ТРЕКИНГ", 300, 174, "+3", true),
            Cell("Disabled", "ЧЁТКОСТЬ", 220, 52, "−4", false),
        )

    private fun render(
        cell: Cell,
        theme: String,
        bare: Boolean,
    ): DesignParity.Result {
        val stem = "MutantSlider_${cell.name}${if (bare) "Bare" else ""}_$theme"
        val ref = DesignParity.reference(stem)
        val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(
                    LocalMutantStage provides "v1-slider",
                    LocalMutantTextRaster provides ChromeText,
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                SliderBody(
                                    fraction = cell.left.toFloat() / (cell.width - 34),
                                    valueText = cell.value,
                                    onFraction = {},
                                    step = {},
                                    modifier = Modifier.width(cell.width.dp),
                                    label = cell.label,
                                    rangeText = "−10 … +10",
                                    enabled = cell.enabled,
                                    key = "slider-thumb",
                                    interactionSource = null,
                                    hidden = bare,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The words over their bare twin: where their ink lies (every side within a pixel, the top row exactly, B-74's
     * allowance on a host that needs it) and its colour (the inkiest pixel within 16 a channel).
     */
    private fun word(
        r: DesignParity.Result,
        b: DesignParity.Result,
        threshold: Int,
        x: IntRange,
        y: IntRange,
        above: Int,
        aliased: Boolean = false,
    ): Pair<Boolean, String> {
        fun inked(
            px: IntArray,
            bare: IntArray,
        ) = extentOf(r.width, r.height) { i ->
            i % r.width in x && i / r.width in y &&
                channelGap(px[i], bare[i]) > threshold
        }

        fun ink(
            px: IntArray,
            bare: IntArray,
        ) = (0 until r.width * r.height)
            .filter { it % r.width in x && it / r.width in y }
            .maxBy { channelGap(px[it], bare[it]) }
            .let { px[it] }
        val kit = inked(r.actual, b.actual)
        val page = inked(r.reference, b.reference)
        val colour = channelGap(ink(r.actual, b.actual), ink(r.reference, b.reference))
        // Drawn without antialiasing (ALIASED_DISABLED_VALUE), a glyph's partial pixels go whole: its sides reach a
        // pixel further and its inkiest pixel is the ink itself, so the colour says nothing.
        val side = if (aliased) 2 else 1
        val ok =
            kit != null && page != null && kit.near(page, px = side, topPx = 0, above = above) &&
                (aliased || colour <= ComponentParity.INK_COLOUR)
        return ok to
            "kit $kit page $page colour $colour (#${ink(
                r.actual,
                b.actual,
            ).toString(16)} against #${ink(r.reference, b.reference).toString(16)})"
    }

    @Test
    fun `the page's sliders - channel, fill, ticks and handle whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in cells) {
                val r = render(cell, theme, false)
                val b = render(cell, theme, true)
                // Without its words, the slider is held whole to the body's floor.
                val bareOk = b.pixels <= (RESIDUAL[b.stem] ?: ComponentParity.FLOOR_PIXELS)
                // The grey words of the disabled one add less over the ground than text's 64.
                val threshold = if (cell.enabled) TEXT_INK else ComponentParity.WEAK_INK
                val half = r.width / 2
                val words =
                    listOf(
                        "label" to
                            word(
                                r,
                                b,
                                threshold,
                                0 until half,
                                0 until HEADER,
                                HANDJET_ROWS_ABOVE,
                            ),
                        "range" to word(r, b, threshold, half until r.width, 0 until HEADER, HANDJET_ROWS_ABOVE),
                        "value" to
                            word(
                                r,
                                b,
                                ComponentParity.WEAK_INK,
                                (cell.left - 2) until (cell.left + 36),
                                HEADER until r.height,
                                HANDJET_ROWS_ABOVE,
                                aliased = !cell.enabled && ALIASED_DISABLED_VALUE,
                            ),
                    )
                lines +=
                    "${r.stem}: bare ${b.pixels} px (max ${b.maxChannel}); " +
                    words.joinToString("; ") { (n, w) -> "$n ${w.second}" }
                if (!bareOk || words.any { !it.second.first }) off += r.stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /** The header's rows: its 23 dp line and the 2 dp under it. */
        const val HEADER = 25

        /**
         * On macOS 26 (the hosted `macos-latest`, the host [HANDJET_ROWS_ABOVE] names) the fixture draws the disabled
         * handle's value without antialiasing — every glyph pixel the ink or the body, seen on PR #18's runner images
         * on 2026-11-15 — while the header and `+3` on the amber handle stay smooth. Allowed for that value on that host
         * alone, by the owner's decision on 2026-11-15; the exact form runs on this Mac before every merge, and why
         * the runner drops the antialiasing is B-85's.
         */
        val ALIASED_DISABLED_VALUE = HANDJET_ROWS_ABOVE > 0

        /**
         * Over the floor, measured on 2026-11-15 and never to be raised: the disabled handle in «Копия», the print
         * theme's dot pattern clipped at the round body's edge, single pixels up to 50 levels apart on the outline — as
         * the disabled icon button's.
         */
        val RESIDUAL = mapOf("MutantSlider_DisabledBare_Kopiya" to 16)
    }
}
