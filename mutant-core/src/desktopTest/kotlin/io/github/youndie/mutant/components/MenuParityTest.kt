package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.Extent
import io.github.youndie.mutant.HANDJET_ROWS_ABOVE
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-33: the v1 page's menu as it stands — the anchor chip and the menu with its window on the first item — in both
 * themes: bodies by the bare twin within the floor, the words, the chevron and the play mark where the page puts them.
 */
class MenuParityTest {
    private val items = listOf("ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ", "СБРОС")

    private fun cell(
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val stem = "MutantMenu_Open${if (hidden) "Bare" else ""}_$theme"
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(
                    LocalMutantTextRaster provides ChromeText,
                    LocalMutantStage provides "v1-menu",
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                DropdownAnchor("Настройка", "menu-anchor", hidden) {}
                                MenuBody(items, 0, {}, Modifier.offset(y = 50.dp), "menu", "menu-pill", hidden)
                            }
                        }
                    }
                }
            }
        }
    }

    /** The anchor's word and chevron, the play mark, and the four rows' words; Handjet's may stand a row higher. */
    private val words =
        listOf(
            Triple("anchor", Pair(0 until 120, 0 until 32), false),
            Triple("mark", Pair(6 until 32, 56 until 92), false),
            Triple("rows", Pair(32 until 210, 56 until 200), true),
        )

    @Test
    fun `the menu as the page stands it - anchor, body and window whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantMenu_Open_$theme"
            val full = cell(theme, false)
            val bare = cell(theme, true)
            lines += "$stem: bare ${bare.pixels} px (max ${bare.maxChannel}); full $full"
            var ok = bare.pixels <= (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
            for ((name, area, handjet) in words) {
                val (xs, ys) = area

                fun inPart(p: Int) = p % full.width in xs && p / full.width in ys

                fun ext(
                    px: IntArray,
                    under: IntArray,
                ): Extent? =
                    extentOf(full.width, full.height) { inPart(it) && channelGap(px[it], under[it]) > TEXT_INK }
                val kit = ext(full.actual, bare.actual)
                val page = ext(full.reference, bare.reference)
                val zone = (0 until full.width * full.height).filter(::inPart)
                val colour =
                    channelGap(
                        full.actual[zone.maxBy { channelGap(full.actual[it], bare.actual[it]) }],
                        full.reference[zone.maxBy { channelGap(full.reference[it], bare.reference[it]) }],
                    )
                lines += "  $name: kit $kit page $page colour $colour"
                val above = if (handjet) HANDJET_ROWS_ABOVE else 0
                if (kit == null || page == null || !kit.near(page, px = 1, topPx = 0, above = above) ||
                    colour > ComponentParity.INK_COLOUR
                ) {
                    ok = false
                }
            }
            if (!ok) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-15 and never to be raised. Chrome draws the speckle as an SVG pattern,
         * rasterised once and resampled, where the kit draws each square exactly: single pixels on the speckle's 40 dp
         * grid, over the window in «Эфир», and over the body under «Копия»'s slip with two short runs of the window's
         * highlight at its top edge, as the segmented button's (research §1.15).
         */
        val RESIDUAL = mapOf("MutantMenu_Open_Efir" to 56, "MutantMenu_Open_Kopiya" to 124)
    }
}
