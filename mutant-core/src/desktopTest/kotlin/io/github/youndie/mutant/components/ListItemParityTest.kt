package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.icons.MutantIcons
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-25: the list stage's two rows — tile, words, the trailing time or badge — and the dividers under them, in both
 * themes. The tile is the slot; the words beside it are held one by one, where the page's ink lies and in its colour.
 */
class ListItemParityTest {
    /** The page's play triangle: list content, not one of the design's icons. */
    private val play = MutantIcons.icon("Play", "M8 5l11 7-11 7z")

    private fun split(
        cell: String,
        theme: String,
    ): ComponentParity.Split {
        val badge = cell == "Badge"
        // The row is 64 tall around the 40 dp tile.
        return ComponentParity.split(
            "MutantListItem_$cell",
            theme,
            TEXT_INK,
            fit = true,
            outset = 12,
            labelBeside = true,
        ) { m, hidden ->
            // The second row's cell reaches 2 dp up into the first row's divider.
            Column(
                m
                    .width(328.dp)
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .offset(y = if (badge) (-2).dp else 0.dp),
            ) {
                if (badge) MutantDivider(key = "0")
                ListItemBody(
                    if (badge) "Эфир, ночь на вторник" else "Настроечная таблица",
                    Modifier,
                    if (badge) "Записано поверх" else "Копия 4-го поколения",
                    play,
                    if (badge) null else "12:04",
                    if (badge) ({ BadgeBody("НОВОЕ", Modifier, compact = false, hidden = hidden) }) else null,
                    null,
                    true,
                    if (badge) "tile2" else "tile1",
                    null,
                    hidden,
                )
                MutantDivider(key = if (badge) "1" else "0")
            }
        }
    }

    /** The parts beside the tile and the dividers: (verdict lines, whether all holds). */
    private fun parts(
        stem: String,
        c: ComponentParity.Split,
        lines: MutableList<String>,
    ): Boolean {
        val r = c.result
        val bare = c.bare!!
        val s = DesignParity.reference(stem)["slot"]!!.jsonObject
        val origin = (s["origin"] as JsonArray).map { it.jsonPrimitive.int }
        val left = s["ox"]!!.jsonPrimitive.int - origin[0] + c.placed.first
        val top = s["oy"]!!.jsonPrimitive.int - origin[1] - 12 + c.placed.second
        var ok = true
        // The words column from the tile's 40 and its 12 dp gap to the trailing part's 70 dp at the row's end.
        val words =
            listOf(
                "headline" to Pair(left + 52 until left + 258, top + 10 until top + 33),
                "supporting" to Pair(left + 52 until left + 258, top + 33 until top + 52),
                "trailing" to Pair(left + 258 until left + 328, top until top + 64),
            )
        for ((name, area) in words) {
            val (xs, ys) = area

            fun inPart(p: Int) = p % r.width in xs && p / r.width in ys
            val kit = extentOf(r.width, r.height) { inPart(it) && channelGap(r.actual[it], bare.actual[it]) > TEXT_INK }
            val page =
                extentOf(r.width, r.height) {
                    inPart(it) &&
                        channelGap(r.reference[it], bare.reference[it]) > TEXT_INK
                }
            val zone = (0 until r.width * r.height).filter(::inPart)
            val colour =
                channelGap(
                    r.actual[zone.maxBy { channelGap(r.actual[it], bare.actual[it]) }],
                    r.reference[zone.maxBy { channelGap(r.reference[it], bare.reference[it]) }],
                )
            val part = "$stem/$name"
            val holds =
                kit != null && page != null && kit.near(page, px = 1, topPx = TOP_RESIDUAL[part] ?: 0) &&
                    colour <= (COLOUR_RESIDUAL[part] ?: 16)
            lines += "  $name: kit $kit page $page colour $colour"
            if (!holds) ok = false
        }
        // The dividers, whole: under the row, and over it in the second row's cell.
        var dividers = 0
        for (i in 0 until r.width * r.height) {
            val x = i % r.width
            val y = i / r.width
            if (x < left || x >= left + 328) continue
            if ((y in top + 64 until top + 66 || y in top - 2 until top) &&
                channelGap(r.actual[i], r.reference[i]) > 16
            ) {
                dividers++
            }
        }
        // What the bare twins keep beside the tile — the badge's plate and ring — against each other.
        var kept = 0
        for (i in 0 until r.width * r.height) {
            val x = i % r.width
            val y = i / r.width
            if (x < left + 52 || x >= left + 328 || y !in top until top + 64) continue
            if (channelGap(bare.actual[i], bare.reference[i]) > 16) kept++
        }
        lines += "  dividers $dividers px; kept beside the tile $kept px"
        return ok && dividers <= ComponentParity.FLOOR_PIXELS && kept <= ComponentParity.FLOOR_PIXELS
    }

    @Test
    fun `both list rows the page draws match it, tile, words and dividers, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in listOf("Time", "Badge")) {
                val stem = "MutantListItem_${cell}_$theme"
                val c = split(cell, theme)
                lines += "$stem: $c"
                val words = parts(stem, c, lines)
                // Inside the tile's clean zone only the icon, held like the icons, to the floor.
                val iconOver = c.content > ComponentParity.FLOOR_PIXELS
                val bodyOver = c.body > (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
                if (bodyOver || iconOver || !c.contentHolds || !words) off += stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        val RESIDUAL = emptyMap<String, Int>()

        /**
         * A top row apart, measured on 2026-11-14: the ascender of «б» in «таблица» lies on the ink threshold in «Эфир»,
         * where the kit's light-on-dark mask crosses it a row above Chrome's (the navigation bar's «ф» and «й», B-24);
         * the second headline, without one, lies exactly, and in «Копия» both do.
         */
        val TOP_RESIDUAL = mapOf("MutantListItem_Time_Efir/headline" to 1)
        val COLOUR_RESIDUAL = emptyMap<String, Int>()
    }
}
