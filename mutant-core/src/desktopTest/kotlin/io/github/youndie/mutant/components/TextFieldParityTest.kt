package io.github.youndie.mutant.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.CompositionLocalProvider
import io.github.youndie.mutant.TEXT_INK
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-20: every text field the page draws — rest, focused, error, disabled — body and texts, in both themes. */
class TextFieldParityTest {
    // The page's cells: prefix, value, placeholder.
    private val cells =
        listOf(
            listOf("Rest", "ПОИСК", "", "Найти запись"),
            listOf("Focused", "ПОИСК", "Ночь на вт", "Найти запись"),
            listOf("Error", "КАНАЛ", "99", ""),
            listOf("Disabled", "ПОИСК", "", "Найти запись"),
        )

    internal fun split(
        cell: List<String>,
        theme: String,
    ): ComponentParity.Split {
        val (name, prefix, value, placeholder) = cell
        val disabled = name == "Disabled"
        val threshold = if (disabled) ComponentParity.WEAK_INK else TEXT_INK
        return ComponentParity.split("MutantField_$name", theme, threshold) { m, hidden ->
            // All four cells share the page's key `field`; the focused one is held focused, as the page draws it, its
            // block where the page stands it (B-186's departure, research D20).
            CompositionLocalProvider(LocalMutantTextFieldAsDesigned provides true) {
                TextFieldBody(
                    value,
                    {},
                    m,
                    prefix,
                    placeholder.takeIf { it.isNotEmpty() },
                    !disabled,
                    name == "Error",
                    KeyboardOptions.Default,
                    "field",
                    null,
                    focusedAs = name == "Focused",
                    hidden = hidden,
                )
            }
        }
    }

    @Test
    fun `every text field the page draws matches it, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in cells) {
                val stem = "MutantField_${cell[0]}_$theme"
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
         * Over the floor, measured on 2026-11-14 and never to be raised: the speckle under «Копия»'s print slip, single
         * pixels repeating every 40 dp along a row (the button's, research §1.15), and a few on the error outline's edge.
         */
        val RESIDUAL = mapOf("MutantField_Rest_Kopiya" to 20, "MutantField_Error_Kopiya" to 29)
    }
}
