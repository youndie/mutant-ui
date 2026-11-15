package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
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
 * B-47: the later page's carousel as it stands — the view with the current card and its calm neighbours, the dots
 * and the pill pointer — in both themes: bodies by the bare twin within the floor, every word where the page puts it.
 */
class CarouselParityTest {
    private val items =
        listOf(
            MutantCarouselItem("Без подписи №3", "Сторона A · 46 мин"),
            MutantCarouselItem("Эфир, ночь на вторник", "Записано поверх · 12 мин", "CH 03 · SP"),
            MutantCarouselItem("Настроечная таблица", "Копия 4-го поколения"),
        )

    private fun cell(
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val stem = "MutantCarousel_Rest${if (hidden) "Bare" else ""}_$theme"
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(
                    LocalMutantTextRaster provides ChromeText,
                    LocalMutantStage provides "l-carousel",
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                CarouselBody(
                                    items,
                                    Modifier.width(400.dp),
                                    rememberPagerState(initialPage = 1) { items.size },
                                    null,
                                    "car",
                                    "cd",
                                    // The view's stage by the kit's own rule, `<stage>-view`: the page's `l-carousel-view`.
                                    hidden = hidden,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** The current card's words and the next card's start; the card before shows no word in its 70 dp. */
    private val words =
        listOf(
            Triple("current", Pair(84 until 304, 8 until 128), false),
            Triple("next", Pair(318 until 400, 8 until 128), false),
        )

    @Test
    fun `the carousel as the page stands it - cards and pointer whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantCarousel_Rest_$theme"
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
                if (kit == null || page == null ||
                    !kit.near(page, px = 1, topPx = TOP_RESIDUAL["$stem/$name"] ?: 0, above = above) ||
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
         * Over the floor, measured on 2026-11-15 and never to be raised: in «Копия» the speckle under the print slip on
         * its 40 dp grid, single pixels over the current card and the pointer (research §1.15).
         */
        val RESIDUAL = mapOf("MutantCarousel_Rest_Kopiya" to 69)

        /**
         * A top row apart, measured on 2026-11-15: the current card's title in «Эфир», light on dark, its top row on the
         * ink threshold — over it in the kit, under it on the page (B-24's cause, as the pull-to-refresh's list). In
         * «Копия» it lies exactly.
         */
        val TOP_RESIDUAL = mapOf("MutantCarousel_Rest_Efir/current" to 1)
    }
}
