package io.github.youndie.mutant.components

import io.github.youndie.mutant.TEXT_INK
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-19: the switch rows the page draws, track, thumb, echo and label, in both themes. */
class SwitchParityTest {
    // The page's keys: track and thumb, `switch` and `thumb` off and disabled, `switch2` and `thumb2` on.
    private val cells =
        listOf(
            listOf("Off", "Чистый сигнал", "switch", "thumb"),
            listOf("On", "Потеря кадра", "switch2", "thumb2"),
            listOf("Disabled", "Disabled", "switch", "thumb"),
        )

    internal fun split(
        cell: List<String>,
        theme: String,
    ): ComponentParity.Split {
        val (name, label, key, thumb) = cell
        val disabled = name == "Disabled"
        // The page's disabled switch is on.
        val on = name != "Off"
        val threshold = if (disabled) ComponentParity.WEAK_INK else TEXT_INK
        // The row is 48 tall around the 28 dp track.
        return ComponentParity.split(
            "MutantSwitch_$name",
            theme,
            threshold,
            fit = true,
            outset = 10,
            labelBeside = true,
        ) { m, hidden ->
            if (!hidden) {
                SwitchBody(on, {}, m, label, !disabled, key, thumb, null)
            } else {
                SwitchBody(on, {}, m, label, !disabled, key, thumb, null, hidden = true)
            }
        }
    }

    @Test
    fun `every switch row the page draws matches it, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (cell in cells) {
                val stem = "MutantSwitch_${cell[0]}_$theme"
                val c = split(cell, theme)
                lines += "$stem: $c"
                // Inside the track's clean zone are only the thumb and its echo, held like a body.
                val marksOver = c.content > ComponentParity.FLOOR_PIXELS
                val bodyOver = c.body > (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
                if (bodyOver || marksOver || !c.contentHolds || !c.labelHolds) off += stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        val RESIDUAL = emptyMap<String, Int>()
    }
}
