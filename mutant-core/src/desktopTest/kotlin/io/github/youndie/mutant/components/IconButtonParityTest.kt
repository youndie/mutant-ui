package io.github.youndie.mutant.components

import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.icons.MutantIcons
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-16: every IconButton and FAB cell the page draws, body and content, in both themes. */
class IconButtonParityTest {
    private val cells =
        listOf(
            "IconButton_Rest",
            "IconButton_Pressed",
            "IconButton_Disabled",
            "Fab_Rest",
            "Fab_Pressed",
            "Fab_Extended",
        )

    internal fun split(
        cell: String,
        theme: String,
    ): ComponentParity.Split {
        val disabled = cell.endsWith("Disabled")
        return ComponentParity.split(
            "Mutant$cell",
            theme,
            if (disabled) ComponentParity.WEAK_INK else TEXT_INK,
        ) { m, hidden ->
            val heat = if (cell.endsWith("Pressed")) 1.0 else null
            when {
                cell.startsWith("IconButton") -> {
                    // The page's key for all three: `iconbtn`.
                    if (heat == null && !hidden) {
                        MutantIconButton(MutantIcons.Share, null, {}, m, enabled = !disabled, key = "iconbtn")
                    } else {
                        ActionBody(
                            ActionKind.Icon,
                            MutantIcons.Share,
                            null,
                            null,
                            {},
                            m,
                            !disabled,
                            "iconbtn",
                            null,
                            heat,
                            hidden,
                        )
                    }
                }

                cell == "Fab_Extended" -> {
                    if (!hidden) {
                        MutantExtendedFab("Запись", MutantIcons.Add, {}, m, key = "fab-x")
                    } else {
                        ActionBody(
                            ActionKind.Fab,
                            MutantIcons.Add,
                            null,
                            "Запись",
                            {},
                            m,
                            true,
                            "fab-x",
                            null,
                            null,
                            true,
                        )
                    }
                }

                else -> {
                    if (heat == null && !hidden) {
                        MutantFab(MutantIcons.Add, null, {}, m, key = "fab")
                    } else {
                        ActionBody(ActionKind.Fab, MutantIcons.Add, null, null, {}, m, true, "fab", null, heat, hidden)
                    }
                }
            }
        }
    }

    @Test
    fun `every icon button and FAB the page draws matches it, body and content, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in cells) {
                val stem = "Mutant${cell}_$theme"
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
         * Over the floor, measured on 2026-11-14 and never to be raised. «Копия»'s moving bodies: the speckle under the
         * print slip, as the button's (research §1.15). The disabled icon button in «Копия»: the print theme's dot
         * pattern clipped at the body's rounded left corners, single pixels up to 50 levels apart.
         */
        val RESIDUAL =
            mapOf(
                "MutantIconButton_Pressed_Kopiya" to 6,
                "MutantIconButton_Disabled_Kopiya" to 7,
                "MutantFab_Rest_Kopiya" to 8,
                "MutantFab_Extended_Kopiya" to 17,
            )
    }
}
