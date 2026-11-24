package io.github.youndie.mutant.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.DesignParity
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-14: every icon of the page at 48 and at 16 dp against the Icons page, drawn by [MutantIcon] in the colour the page's
 * cell gives it over the ground behind the cell, at whole pixels as Chrome snaps an `<svg>` (drawn at the
 * cell's fraction of a pixel instead, every icon reads worse: 216 px for Menu at 48).
 *
 * 25 of the 32 cells match to the pixel. The rest is the two renderers on the same strokes, read on each
 * `_DIFF` (research §1.13): only partial-coverage pixels, never a stroke moved — and held to what was measured,
 * never raised.
 *
 * B-195: the kit's own icons, which the page does not draw, are its known entries (research D20), each with the item
 * that added it; [PlayIconTest] holds Play to its own golden.
 */
class IconParityTest {
    @Test
    fun `the sixteen icons are the page's in its order, then the kit's own`() {
        assertEquals(
            listOf(
                "Menu",
                "Back",
                "Close",
                "Search",
                "Home",
                "Add",
                "Delete",
                "Edit",
                "Share",
                "Refresh",
                "Settings",
                "Done",
                "Error",
                "Warning",
                "Info",
                "Schedule",
                "Play",
            ),
            MutantIcons.all.map { it.first },
        )
        assertEquals(MutantIconPaths.all.map { it.first }, MutantIcons.all.map { it.first })
        assertEquals(mapOf("Play" to "B-195"), MutantIconPaths.kit, "the kit's own icons and their items")
        assertEquals(
            MutantIconPaths.kit.keys.toList(),
            MutantIcons.all.map { it.first }.takeLast(MutantIconPaths.kit.size),
        )
    }

    @Test
    fun `a known entry is one the page does not draw`() {
        // The Icons page's references are its sixteen; one rendered under a kit icon's name would mean the page drew it.
        val stems = DesignParity.stems().filter { it.startsWith("Icon_") }
        for (name in MutantIconPaths.kit.keys) {
            assertTrue(
                stems.none { it.startsWith("Icon_$name") },
                "$name has a reference",
            )
        }
        assertEquals(32, stems.size, "the page's sixteen icons at 48 and 16 dp")
    }

    @Test
    fun `every icon matches the page at 48 and 16 dp`() {
        val results =
            MutantIcons.all.filter { it.first !in MutantIconPaths.kit }.flatMap { (name, icon) ->
                listOf(48, 16).map { size ->
                    val stem = "Icon_$name${size}_Efir"
                    val ref = DesignParity.reference(stem)
                    val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
                    val ink = DesignParity.rgb(ref["color"]!!.jsonPrimitive.content)
                    DesignParity.compare(stem) {
                        Box(
                            Modifier.fillMaxSize().background(ground),
                        ) { MutantIcon(icon, null, tint = ink, size = size.dp) }
                    }
                }
            }
        println(results.joinToString("\n"))
        val over = results.filter { it.pixels > (RESIDUAL[it.stem] ?: 0) }
        assertTrue(over.isEmpty(), "over the measured residual: $over\n" + results.joinToString("\n"))
    }

    private companion object {
        /**
         * The cells that do not match to the pixel, measured on 2026-11-14, and why. Arcs: Compose turns an SVG arc
         * into cubics where Chrome's Skia draws conics; with Skia's own arcs Settings reads 0 and Search 3.
         * Coverage: the same straight strokes, Schedule's circle byte for byte Error's (which reads 0), and the two
         * renderers still anti-alias them differently — the pencil's 1 dp gap 105 against 74–88 per channel.
         */
        val RESIDUAL =
            mapOf(
                "Icon_Search16_Efir" to 9, // arcs
                "Icon_Settings48_Efir" to 6, // arcs
                "Icon_Settings16_Efir" to 5, // arcs
                "Icon_Edit48_Efir" to 18, // coverage in the pencil's gap
                "Icon_Refresh48_Efir" to 58, // arcs and coverage
                "Icon_Refresh16_Efir" to 9, // arcs and coverage
                "Icon_Schedule48_Efir" to 27, // coverage at the circle's top
            )
    }
}
