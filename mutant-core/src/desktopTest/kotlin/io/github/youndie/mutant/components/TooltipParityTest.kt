package io.github.youndie.mutant.components

import io.github.youndie.mutant.HANDJET_ROWS_ABOVE
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-32: the v1 page's two tooltip plates, the plain one and the one with two lines. */
class TooltipParityTest {
    private val cells =
        listOf(
            listOf("Plain", "ПОДЕЛИТЬСЯ", null, "tip"),
            listOf("Rich", "REC · 0:42:17", "ОСТАЛОСЬ 3 МИН", "tip-rich"),
        )

    @Test
    fun `the page's tooltip plates - the plate whole, its words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for ((name, text, supporting, key) in cells) {
                val c =
                    ComponentParity.split("MutantTooltip_$name", theme, TEXT_INK) { m, hidden ->
                        TooltipPlate(text!!, supporting, key!!, m, hidden)
                    }
                val stem = "MutantTooltip_${name}_$theme"
                // The words: every side within a pixel, the top row exactly (a row higher on macOS 26, B-74).
                val words =
                    c.kit != null && c.page != null &&
                        c.kit.near(c.page, px = 1, topPx = 0, above = HANDJET_ROWS_ABOVE) &&
                        channelGap(c.kitInk, c.pageInk) <= ComponentParity.INK_COLOUR
                val bodyOk = c.body <= (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
                lines += "$stem: $c"
                if (!words || !bodyOk) off += stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        val RESIDUAL = emptyMap<String, Int>()
    }
}
