package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
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
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.LocalMutantTextZone
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-44: the later page's search bar as it stands — the focused field with its caret and clear icon, the suggestions,
 * a result, the divider and the loading row — in both themes: bodies, caret and cocoons by the bare twin within the
 * floor, every word, the caret and every icon where the page puts it.
 */
@OptIn(ExperimentalTestApi::class)
class SearchBarParityTest {
    private fun cell(
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val stem = "MutantSearchBar_Focused${if (hidden) "Bare" else ""}_$theme"
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(
                    LocalMutantTextRaster provides ChromeText,
                    LocalMutantStage provides "l-search",
                    // The caret's block where the page stands it (B-186's departure, research D20).
                    LocalMutantTextFieldAsDesigned provides true,
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                SearchBarBody(
                                    "ночь",
                                    {},
                                    Modifier.width(328.dp),
                                    "ПОИСК",
                                    null,
                                    null,
                                    "Очистить",
                                    true,
                                    "search",
                                    null,
                                    focusedAs = true,
                                    hidden = hidden,
                                ) {
                                    SuggestionsBody(
                                        listOf("ночь на вторник", "ночной эфир"),
                                        {},
                                        Modifier,
                                        null,
                                        true,
                                        hidden,
                                    )
                                    Column {
                                        ListItemBody(
                                            if (hidden) {
                                                androidx.compose.ui.text
                                                    .AnnotatedString("Эфир, ночь на вторник")
                                            } else {
                                                searchMatch(
                                                    "Эфир, ночь на вторник",
                                                    "ночь",
                                                    MutantTheme.colors.textAccent,
                                                )
                                            },
                                            Modifier,
                                            "Записано поверх · 12 мин",
                                            MutantIcons.icon("Play", "M8 5l11 7-11 7z"),
                                            null,
                                            null,
                                            null,
                                            true,
                                            "sr1",
                                            null,
                                            hidden = hidden,
                                            rowHeight = 60.dp,
                                        )
                                        MutantDivider(key = "0")
                                        MutantSearchResultLoading()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** The words and icons, each by the part of the crop it lies in; Handjet's may stand a row higher (B-74). */
    private val words =
        listOf(
            Triple("prefix", Pair(0 until 60, 0 until 52), true),
            Triple("query", Pair(60 until 124, 0 until 52), false),
            Triple("clear", Pair(296 until 328, 0 until 52), false),
            Triple("chips", Pair(0 until 328, 60 until 120), false),
            Triple("tile", Pair(0 until 44, 130 until 194), false),
            Triple("result", Pair(48 until 328, 130 until 194), false),
        )

    @Test
    fun `the search bar as the page stands it - bodies whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantSearchBar_Focused_$theme"
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

    /**
     * B-168: the page's bar, words hidden, drawn by the clean zone the kit measures itself and by the one the engine
     * measured. The engine counts every `svg` in the slot, the clear icon's included; drawn by its own zone without it,
     * the body ran its edge mosaic under the cross, 570 pixels of the bar in «Эфир».
     */
    @Test
    fun `the bar's own clean zone takes in the clear icon, as the page's does`() {
        val lines = ArrayList<String>()
        var worst = 0
        for (theme in listOf("Efir", "Kopiya")) {
            val page = bar(theme, ownZone = false)
            val own = bar(theme, ownZone = true)
            assertTrue(page.size == own.size && page.isNotEmpty(), "the bar was not drawn")
            val differ = page.indices.count { channelGap(page[it], own[it]) > ComponentParity.INK_COLOUR }
            lines += "$theme: ${page.size} pixels of the bar, $differ apart"
            worst = maxOf(worst, differ)
        }
        println(lines.joinToString("\n"))
        assertTrue(worst == 0, lines.joinToString("\n"))
    }

    /** The bar's 328 x 52 pixels, `0xRRGGBB`, as the page stands it; by its own clean zone when [ownZone]. */
    private fun bar(
        theme: String,
        ownZone: Boolean,
    ): IntArray {
        val stem = "MutantSearchBar_FocusedBare_$theme"
        val slots = ComponentParity.stageSlots(stem)
        val out = IntArray(328 * 52)
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(colors = ComponentParity.palette(theme)) {
                        CompositionLocalProvider(
                            LocalMutantTextRaster provides ChromeText,
                            LocalMutantStage provides "l-search",
                        ) {
                            ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                                ComponentParity.ProvideSlots(slots) {
                                    val engine = LocalMutantTextZone.current
                                    CompositionLocalProvider(
                                        LocalMutantTextZone provides {
                                            if (ownZone && it == "search") null else engine?.invoke(it)
                                        },
                                    ) {
                                        Box(Modifier.size(360.dp, 80.dp).testTag("bar")) {
                                            SearchBarBody(
                                                "ночь",
                                                {},
                                                Modifier.width(328.dp),
                                                "ПОИСК",
                                                null,
                                                null,
                                                "Очистить",
                                                true,
                                                "search",
                                                null,
                                                focusedAs = true,
                                                hidden = true,
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val shot = onNodeWithTag("bar").captureToImage().toPixelMap()
            for (y in 0 until 52) {
                for (x in 0 until 328) {
                    val c = shot[x, y]
                    out[y * 328 + x] =
                        ((c.red * 255).roundToInt() shl 16) or ((c.green * 255).roundToInt() shl 8) or
                        (c.blue * 255).roundToInt()
                }
            }
        }
        return out
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-15 and never to be raised: in «Копия» six single pixels of the speckle under
         * the print slip on its 40 dp grid, over the result's tile (research §1.15).
         */
        val RESIDUAL = mapOf("MutantSearchBar_Focused_Kopiya" to 6)
    }
}
