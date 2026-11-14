package io.github.youndie.mutant.components

import io.github.youndie.mutant.TEXT_INK
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-17: every chip the page draws, body and label, in both themes. */
class ChipParityTest {
    private val cells = listOf("Unselected", "Selected", "Pressed", "Disabled")

    internal fun split(
        cell: String,
        theme: String,
    ): ComponentParity.Split {
        val disabled = cell == "Disabled"
        // The page stretches the first two chips to their captions' width; the others are as wide as their label.
        val fit = cell == "Pressed" || cell == "Disabled"
        return ComponentParity.split(
            "MutantChip_$cell",
            theme,
            if (disabled) ComponentParity.WEAK_INK else TEXT_INK,
            fit,
            // The chip's 48 dp target around its 32 dp body.
            outset = 8,
        ) { m, hidden ->
            // The page's keys: `chip` for all but the selected one, `chip2`; the pressed cell is selected and boiling.
            val selected = cell == "Selected" || cell == "Pressed"
            val key = if (cell == "Selected") "chip2" else "chip"
            if (!hidden && cell != "Pressed") {
                MutantChip("Избранное", selected, {}, m, enabled = !disabled, key = key)
            } else {
                ChipBody(
                    "Избранное",
                    selected,
                    {},
                    m,
                    !disabled,
                    key,
                    null,
                    if (cell ==
                        "Pressed"
                    ) {
                        1.0
                    } else {
                        null
                    },
                    hidden,
                )
            }
        }
    }

    @Test
    fun `every chip the page draws matches it, body and label, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in cells) {
                val stem = "MutantChip_${cell}_$theme"
                val c = split(cell, theme)
                lines += "$stem: $c"
                if (c.body > (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS) || !c.contentHolds) off += stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-14 and never to be raised: the selected chips in «Копия» carry the print
         * slip of 1.5 dp, and the speckle under it reads as the button's does (research §1.15).
         */
        val RESIDUAL = mapOf("MutantChip_Selected_Kopiya" to 10, "MutantChip_Pressed_Kopiya" to 10)
    }
}
