package io.github.youndie.mutant.components

import io.github.youndie.mutant.TEXT_INK
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-18: the checkbox and radio rows the page draws, body, mark and label, in both themes. */
class CheckboxParityTest {
    private val cells =
        listOf(
            Triple("Checkbox_Unchecked", "Шум головки", "check"),
            Triple("Checkbox_Checked", "Автотрекинг", "check2"),
            Triple("Checkbox_Disabled", "Disabled", "check"),
            Triple("Radio_Unselected", "LP · 4 часа", "radio"),
            Triple("Radio_Selected", "SP · 2 часа", "radio2"),
            Triple("Radio_Disabled", "Disabled", "radio"),
        )

    internal fun split(
        cell: String,
        label: String,
        key: String,
        theme: String,
    ): ComponentParity.Split {
        val disabled = cell.endsWith("Disabled")
        // The page's disabled checkbox is checked, its disabled radio selected; the other two cells are the pair.
        val on = cell.endsWith("Checked") || cell.endsWith("Selected") || disabled
        val threshold = if (disabled) ComponentParity.WEAK_INK else TEXT_INK
        // The row is 48 tall around the 22 dp control.
        return ComponentParity.split(
            "Mutant$cell",
            theme,
            threshold,
            fit = true,
            outset = 13,
            labelBeside = true,
        ) { m, hidden ->
            if (cell.startsWith("Checkbox")) {
                if (!hidden) {
                    MutantCheckbox(on, {}, m, label, enabled = !disabled, key = key)
                } else {
                    CheckboxBody(on, {}, m, label, !disabled, key, null, hidden = true)
                }
            } else {
                if (!hidden) {
                    MutantRadioButton(on, {}, m, label, enabled = !disabled, key = key)
                } else {
                    RadioBody(on, {}, m, label, !disabled, key, null, hidden = true)
                }
            }
        }
    }

    @Test
    fun `every checkbox and radio row the page draws matches it, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for ((cell, label, key) in cells) {
                val stem = "Mutant${cell}_$theme"
                val c = split(cell, label, key, theme)
                lines += "$stem: $c"
                if (c.body > (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS) || !c.contentHolds ||
                    !c.labelHolds
                ) {
                    off += stem
                }
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        val RESIDUAL = emptyMap<String, Int>()
    }
}
