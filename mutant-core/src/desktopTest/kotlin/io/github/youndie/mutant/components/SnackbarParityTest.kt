package io.github.youndie.mutant.components

import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.extentOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-26: the list stage's snackbar — blue bar, deep shadow, message and action — in both themes. */
class SnackbarParityTest {
    /** The message and the action each where the page's ink lies and in its colour. */
    private fun words(
        stem: String,
        c: ComponentParity.Split,
        lines: MutableList<String>,
    ): Boolean {
        val r = c.result
        val bare = c.bare!!
        val s = DesignParity.reference(stem)["slot"]!!.jsonObject
        val origin = (s["origin"] as JsonArray).map { it.jsonPrimitive.int }
        val left = s["ox"]!!.jsonPrimitive.int - origin[0] + c.placed.first
        val top = s["oy"]!!.jsonPrimitive.int - origin[1] + c.placed.second
        var ok = true
        for ((name, xs) in listOf("message" to (left until left + 200), "action" to (left + 200 until left + 328))) {
            fun inPart(p: Int) = p % r.width in xs && p / r.width in top until top + 48
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
            lines += "  $name: kit $kit page $page colour $colour"
            val topPx = TOP_RESIDUAL["$stem/$name"] ?: 0
            if (kit == null || page == null || !kit.near(page, px = 1, topPx = topPx) || colour > 16) ok = false
        }
        return ok
    }

    @Test
    fun `the snackbar matches the page, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantSnackbar_Rest_$theme"
            val c =
                ComponentParity.split("MutantSnackbar_Rest", theme, TEXT_INK) { m, hidden ->
                    SnackbarBody("Добавлено в очередь", m, true, "ОТМЕНИТЬ", {}, {}, "snack", hidden, playIn = false)
                }
            lines += "$stem: $c"
            val words = words(stem, c, lines)
            if (c.body > (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS) || !c.contentHolds || !words) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        val RESIDUAL = emptyMap<String, Int>()

        /**
         * A top row apart, measured on 2026-11-14: the ascender of «б» in «Добавлено» lies on the ink threshold, and the
         * kit's light-on-dark mask crosses it a row above Chrome's — here in both themes, the message being on-blue on
         * the blue bar in each (B-24's and B-25's cause). Its bottom row, the baseline's side, is the page's.
         */
        val TOP_RESIDUAL = mapOf("MutantSnackbar_Rest_Efir/message" to 1, "MutantSnackbar_Rest_Kopiya/message" to 1)
    }
}
