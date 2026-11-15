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
 * B-46: the later page's data table as it stands — the OSD header, four rows with the second selected, the tracking
 * dividers — in both themes: bodies and dividers by the bare twin within the floor, every word and the tick where the
 * page puts them.
 */
class DataTableParityTest {
    private val rows =
        listOf(
            listOf("Без подписи №3", "46:00", "CH 03"),
            listOf("Эфир, ночь на вторник", "12:04", "CH 03"),
            listOf("Настроечная таблица", "02:10", "CH 01"),
            listOf("Нет сигнала", "00:42", "CH 99"),
        )

    private val columns =
        listOf(
            MutantTableColumn<List<String>>("ЗАПИСЬ") { it[0] },
            MutantTableColumn("ДЛИНА", 90.dp, MutantTableCellStyle.Osd) { it[1] },
            MutantTableColumn("КАНАЛ", 80.dp, MutantTableCellStyle.OsdSecondary) { it[2] },
        )

    private fun cell(
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val stem = "MutantDataTable_Selected${if (hidden) "Bare" else ""}_$theme"
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(
                    LocalMutantTextRaster provides ChromeText,
                    LocalMutantStage provides "l-table",
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                DataTableBody(
                                    rows,
                                    columns,
                                    Modifier.width(560.dp),
                                    setOf(1),
                                    { _, _ -> },
                                    true,
                                    "trow",
                                    hidden,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** The words and the tick, each by the part of the crop it lies in; Handjet's may stand a row higher (B-74). */
    private val words =
        listOf(
            Triple("header", Pair(40 until 560, 0 until 36), true),
            Triple("names", Pair(40 until 390, 38 until 238), false),
            Triple("lengths", Pair(390 until 480, 38 until 238), true),
            Triple("channels", Pair(480 until 560, 38 until 238), true),
            Triple("tick", Pair(0 until 40, 88 until 136), false),
        )

    @Test
    fun `the data table as the page stands it - bodies and dividers whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantDataTable_Selected_$theme"
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
                if (kit == null || page == null || !kit.near(page, px = 1, topPx = 0, above = above) ||
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
         * Over the floor, measured on 2026-11-15 and never to be raised: in «Копия» thirteen single pixels of the speckle
         * under the print slip on its 40 dp grid, over the four boxes (research §1.15).
         */
        val RESIDUAL = mapOf("MutantDataTable_Selected_Kopiya" to 13)
    }
}
