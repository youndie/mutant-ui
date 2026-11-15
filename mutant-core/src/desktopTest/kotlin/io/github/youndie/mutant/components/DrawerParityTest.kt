package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * B-34: the v1 page's navigation drawer screen as it stands — two still cards under the scrim and the drawer over them
 * with its window on the third channel — in both themes: bodies by the bare twin within the floor, the words where the
 * page puts them.
 */
class DrawerParityTest {
    private val items =
        listOf(
            MutantDrawerItem("CH 01", "Эфир"),
            MutantDrawerItem("CH 02", "Очередь"),
            MutantDrawerItem("CH 03", "Поиск"),
            MutantDrawerItem("CH 04", "Настройка"),
        )

    private fun cell(
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val stem = "MutantDrawer_Open${if (hidden) "Bare" else ""}_$theme"
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(LocalMutantTextRaster provides ChromeText) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                // The page's screen: 328 × 308, its corners rounded 12, the page's colour.
                                Box(
                                    Modifier
                                        .size(328.dp, 308.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MutantTheme.colors.page),
                                ) {
                                    CompositionLocalProvider(
                                        LocalMutantStage provides "v1-drawer-bg",
                                        LocalCardHidden provides hidden,
                                    ) {
                                        Column(
                                            Modifier.padding(start = 60.dp, top = 14.dp, end = 12.dp).fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(30.dp),
                                        ) {
                                            MutantCard(Modifier.fillMaxWidth().height(80.dp), key = "card") {
                                                MutantCardTitle("Без подписи №3")
                                                MutantCardSubtitle("Сторона A · 46 мин")
                                            }
                                            MutantCard(Modifier.fillMaxWidth().height(80.dp), key = "card-b") {
                                                MutantCardTitle("Эфир, ночь на вторник")
                                                MutantCardSubtitle("Записано поверх · 12 мин")
                                            }
                                        }
                                    }
                                    CompositionLocalProvider(LocalMutantStage provides "v1-drawer-win") {
                                        DrawerFrame(
                                            true,
                                            {},
                                            items,
                                            2,
                                            {},
                                            Modifier,
                                            "КАНАЛЫ",
                                            null,
                                            "drawer",
                                            playIn = false,
                                            hidden = hidden,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The title, the codes, the labels; the cards' words beyond the drawer, darkened by the scrim, from 210: at the
     * drawer's edge the first card's title ends a pixel later in the kit than on the page, its last pixel at 207.
     */
    private val words =
        listOf(
            Triple("title", Pair(10 until 120, 16 until 50), true),
            Triple("codes", Pair(20 until 70, 54 until 230), true),
            Triple("labels", Pair(74 until 190, 54 until 230), false),
            Triple("cards", Pair(210 until 328, 20 until 210), false),
        )

    @Test
    fun `the drawer screen as the page stands it - bodies whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantDrawer_Open_$theme"
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
         * rasterised once and resampled, where the kit draws each square exactly: single pixels on its 40 dp grid over
         * the window, as the menu's and the segmented button's (research §1.15).
         */
        val RESIDUAL = mapOf("MutantDrawer_Open_Efir" to 47, "MutantDrawer_Open_Kopiya" to 9)
    }
}
