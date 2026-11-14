package io.github.youndie.mutant.components

import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.icons.MutantIcons
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-24: the navigation bar the page draws — bar, pill, icons, badge and labels — in both themes. */
class NavigationBarParityTest {
    /** The page's four destinations, the second with its count. */
    internal val items =
        listOf(
            MutantNavigationItem("Эфир", MutantIcons.Home),
            MutantNavigationItem("Очередь", MutantIcons.Schedule, badge = "3"),
            MutantNavigationItem("Поиск", MutantIcons.Search),
            MutantNavigationItem("Настройка", MutantIcons.Settings),
        )

    @Test
    fun `the navigation bar matches the page, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantNavigationBar_Rest_$theme"
            val c =
                ComponentParity.split("MutantNavigationBar_Rest", theme, TEXT_INK) { m, hidden ->
                    NavigationBarBody(items, 0, {}, m, "navbar", "navpill", hidden)
                }
            lines += "$stem: $c"
            val items = items(stem, c, lines)
            if (c.body > (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS) || !c.contentHolds || !items) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    /**
     * Each item part by part — its icon box (with the pill and the badge) and its label — where the page's ink lies and
     * in its colour, ink being what the cell adds over its bare twin: one extent over four items would hide a word out
     * of place.
     */
    private fun items(
        stem: String,
        c: ComponentParity.Split,
        lines: MutableList<String>,
    ): Boolean {
        val r = c.result
        val bare = c.bare!!
        val s =
            io.github.youndie.mutant.DesignParity
                .reference(stem)["slot"]!!
                .jsonObject
        val origin = (s["origin"] as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.int }
        val bx = s["ox"]!!.jsonPrimitive.int - origin[0] + c.placed.first
        val by = s["oy"]!!.jsonPrimitive.int - origin[1] + c.placed.second
        val cell = s["w"]!!.jsonPrimitive.int / 4
        // The icon box 11 dp down, 30 tall; the label from 3 dp under it to the zone's bottom.
        val parts = listOf("icon" to (by + 11 until by + 41), "label" to (by + 44 until by + 60))
        var ok = true
        for (i in 0 until 4) {
            val xs = bx + i * cell until bx + (i + 1) * cell
            for ((name, ys) in parts) {
                fun inPart(p: Int) = p % r.width in xs && p / r.width in ys
                val kit =
                    extentOf(r.width, r.height) {
                        inPart(it) &&
                            channelGap(r.actual[it], bare.actual[it]) > TEXT_INK
                    }
                val page =
                    extentOf(r.width, r.height) {
                        inPart(it) &&
                            channelGap(r.reference[it], bare.reference[it]) > TEXT_INK
                    }
                val zone = (0 until r.width * r.height).filter(::inPart)
                val kitInk = r.actual[zone.maxBy { channelGap(r.actual[it], bare.actual[it]) }]
                val pageInk = r.reference[zone.maxBy { channelGap(r.reference[it], bare.reference[it]) }]
                val colour = channelGap(kitInk, pageInk)
                val part = "$stem/$i/$name"
                val holds =
                    kit != null && page != null && kit.near(page, px = 1, topPx = TOP_RESIDUAL[part] ?: 0) &&
                        colour <= (COLOUR_RESIDUAL[part] ?: 16)
                if (!holds) {
                    ok = false
                    lines += "  $stem item $i $name: kit $kit page $page colour $colour"
                }
            }
        }
        return ok
    }

    private companion object {
        val RESIDUAL = emptyMap<String, Int>()

        /**
         * A top row apart, measured on 2026-11-14: the ascender of «ф» and the breve of «й» lie on the ink threshold in
         * «Эфир», where the kit's light-on-dark mask crosses it a row above Chrome's; the labels without them lie
         * exactly, and in «Копия» all four do.
         */
        val TOP_RESIDUAL =
            mapOf(
                "MutantNavigationBar_Rest_Efir/0/label" to 1,
                "MutantNavigationBar_Rest_Efir/3/label" to 1,
            )

        /**
         * The badge's «3» in Handjet 14 never covers a pixel whole in either renderer, and Chrome's dark-on-light mask
         * is the darker (its strongest pixel 48 against the kit's 92 in «Эфир»), in both themes, as the badge is the page's
         * literal amber and black in both: the inkiest pixel is no measure of the colour
         * there. Its plate and strokes stand where the page's do. Measured on 2026-11-14.
         */
        val COLOUR_RESIDUAL =
            mapOf("MutantNavigationBar_Rest_Efir/1/icon" to 44, "MutantNavigationBar_Rest_Kopiya/1/icon" to 44)
    }
}
