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
import io.github.youndie.mutant.Extent
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

/**
 * B-45: the later page's time picker as it stands, the hours chosen, in both themes.
 */
class TimePickerParityTest {
    private fun cell(
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val stem = "MutantTimePicker_Hours${if (hidden) "Bare" else ""}_$theme"
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(
                    LocalMutantTextRaster provides ChromeText,
                    LocalMutantStage provides "l-date",
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                TimePickerBody(21, 30, { _, _ -> }, Modifier, "", "", "tp", hidden)
                            }
                        }
                    }
                }
            }
        }
    }

    private val words =
        listOf(
            Triple("hours", Pair(0 until 76, 0 until 72), true),
            Triple("colon", Pair(80 until 102, 0 until 72), true),
            Triple("minutes", Pair(105 until 181, 0 until 72), true),
        )

    @Test
    fun `as the page stands it - bodies whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantTimePicker_Hours_$theme"
            val full = cell(theme, false)
            val bare = cell(theme, true)
            lines += "$stem: bare ${bare.pixels} px (max ${bare.maxChannel}); full $full"
            var ok = bare.pixels <= (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
            for ((name, area, handjet) in words) {
                val (xs, ys) = area

                fun inPart(p: Int) = p % full.width in xs && p / full.width in ys

                fun ext(
                    px: IntArray,
                    under: IntArray,
                ): Extent? =
                    extentOf(full.width, full.height) { inPart(it) && channelGap(px[it], under[it]) > TEXT_INK }
                val kit = ext(full.actual, bare.actual)
                val page = ext(full.reference, bare.reference)
                val zone = (0 until full.width * full.height).filter(::inPart)
                val colour =
                    channelGap(
                        full.actual[zone.maxBy { channelGap(full.actual[it], bare.actual[it]) }],
                        full.reference[zone.maxBy { channelGap(full.reference[it], bare.reference[it]) }],
                    )
                lines += "  $name: kit $kit page $page colour $colour"
                val above = if (handjet) HANDJET_ROWS_ABOVE else 0
                if (kit == null || page == null ||
                    !kit.near(page, px = 1, topPx = TOP_RESIDUAL["$stem/$name"] ?: 0, above = above) ||
                    colour > ComponentParity.INK_COLOUR
                ) {
                    ok = false
                }
            }
            if (!ok) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-15 and never to be raised: in «Копия» the speckle under the print slip on
         * its 40 dp grid, single pixels over both fields (research §1.15).
         */
        val RESIDUAL = mapOf("MutantTimePicker_Hours_Kopiya" to 20)

        /**
         * A top row apart, measured on 2026-11-15, the bottom rows exact: the digits' round tops on the ink threshold.
         * Black on acid, the hours' top row is under it in the kit and over it on the page, in both themes; light on
         * dark, the minutes' the other way in «Эфир» (B-24's cause).
         */
        val TOP_RESIDUAL =
            mapOf(
                "MutantTimePicker_Hours_Efir/hours" to 1,
                "MutantTimePicker_Hours_Kopiya/hours" to 1,
                "MutantTimePicker_Hours_Efir/minutes" to 1,
            )
    }
}
