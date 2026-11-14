package io.github.youndie.mutant.components

import androidx.compose.runtime.CompositionLocalProvider
import io.github.youndie.mutant.TEXT_INK
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-21: the card at rest and disabled, body, dead pixels and texts, in both themes. */
class CardParityTest {
    internal fun split(
        cell: String,
        theme: String,
    ): ComponentParity.Split {
        val disabled = cell == "Disabled"
        return ComponentParity.split(
            "MutantCard_$cell",
            theme,
            if (disabled) ComponentParity.WEAK_INK else TEXT_INK,
        ) { m, hidden ->
            CompositionLocalProvider(LocalCardHidden provides hidden) {
                MutantCard(m, enabled = !disabled) {
                    MutantCardTitle("Без подписи №3")
                    MutantCardSubtitle("Сторона A · 46 мин")
                }
            }
        }
    }

    @Test
    fun `the card at rest and disabled matches the page, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in listOf("Rest", "Disabled")) {
                val stem = "MutantCard_${cell}_$theme"
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
         * Over the floor, measured on 2026-11-14 and never to be raised: the speckle under «Копия»'s print slip, the same
         * few phases recurring in every 40 dp tile of a card 362 dp wide (the button's, research §1.15).
         */
        val RESIDUAL = mapOf("MutantCard_Rest_Kopiya" to 57)
    }
}
