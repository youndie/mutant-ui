package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-35: the v1 page's bottom sheet screen — the still card under the scrim, the tear line, the sheet with its grabber,
 * and the page's own content on it (a title, two rows, a button) — in both themes. Everything but the words is held
 * against the bare twin's; the words one by one.
 */
class SheetParityTest {
    private fun cell(
        stem: String,
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val card = ComponentParity.stageSlots("MutantSheet_Card_$theme")
        val window = ComponentParity.stageSlots("MutantSheet_Window_$theme")
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(LocalMutantTextRaster provides ChromeText) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        Box(Modifier.fillMaxSize().background(ground)) {
                            // The page's screen: 328 × 308, its corners rounded 12, the page's colour.
                            Box(
                                Modifier
                                    .size(328.dp, 308.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MutantTheme.colors.page),
                            ) {
                                CompositionLocalProvider(
                                    LocalMutantStage provides "v1-sheet-bg",
                                    LocalCardHidden provides hidden,
                                ) {
                                    ComponentParity.ProvideSlots(card) {
                                        MutantCard(
                                            Modifier.padding(start = 12.dp, top = 14.dp).width(304.dp),
                                            key = "card",
                                        ) {
                                            MutantCardTitle("Без подписи №3")
                                            MutantCardSubtitle("Сторона A · 46 мин")
                                        }
                                    }
                                    // The page's screen stage finds the sheet stage's tear line too
                                    // (`querySelectorAll` reaches into the nested stage) and draws it again with its own
                                    // seed, under the scrim: half the blue shows in the sheet's line's gaps. It belongs
                                    // to the page's screen, not to the sheet.
                                    MutantDivider(Modifier.padding(top = 102.dp), key = "0")
                                }
                                // The page's column squeezes its content into the room (B-184's departure, research
                                // D20): the kit gives each its own height and scrolls.
                                CompositionLocalProvider(
                                    LocalMutantStage provides "v1-sheet-win",
                                    LocalMutantBottomSheetAsDesigned provides true,
                                ) {
                                    ComponentParity.ProvideSlots(window) {
                                        SheetFrame(
                                            true,
                                            {},
                                            Modifier,
                                            null,
                                            WindowInsets(0),
                                            "sheet",
                                            { PageContent(hidden) },
                                            playIn = false,
                                            height = 208.dp,
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

    /** The page's sheet content: a title, two rows of a mode and its time, and a button. */
    @Composable
    private fun PageContent(hidden: Boolean) {
        val colors = MutantTheme.colors
        val fonts = LocalMutantFonts.current
        val raster = LocalMutantTextRaster.current

        fun ink(c: Color) = if (hidden) Color.Transparent else c
        // `line-height: 1.1`: a 28.6 dp line under Handjet 26's own 29, whose half-leading Blink floors to -1, the
        // baseline a pixel up. cssLine clamps a leading that negative (its ratio would be 2.1), so the fixture lays the
        // page's line itself: the face's own line, a pixel up, in a 28.6 dp box.
        Box(Modifier.height(28.6.dp)) {
            BasicText(
                "Скорость записи",
                Modifier.offset(y = (-1).dp),
                style =
                    TextStyle(
                        fontFamily = fonts?.display,
                        fontSize = 26.sp,
                        color = ink(colors.text),
                        platformStyle = raster,
                    ).cssLine(MutantFaceMetrics.Handjet),
            )
        }
        for ((mode, time) in listOf("SP" to "2 ЧАСА", "LP" to "4 ЧАСА")) {
            // `align-items: center` in a 32 dp row: Chrome puts a line at the half pixel and paints it on the one
            // above (Handjet 20's 23 dp line 4.5 down, at 4), where Compose's centring rounds up.
            Row(
                Modifier.fillMaxWidth().height(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                BasicText(
                    mode,
                    Modifier.padding(top = 7.dp),
                    style =
                        TextStyle(
                            fontFamily = fonts?.text,
                            fontSize = 15.sp,
                            color = ink(colors.text),
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Rubik),
                )
                BasicText(
                    time,
                    Modifier.padding(top = 4.dp),
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = 20.sp,
                            color = ink(colors.textOsd),
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Handjet),
                )
            }
        }
        // The page's button: a `btn` of 112 × 40 with drips of 8, 2 dp further down.
        Box(Modifier.fillMaxWidth().padding(top = 2.dp), contentAlignment = Alignment.CenterEnd) {
            MutantSurface(
                MutantBody.Btn,
                "sheet-ok",
                Modifier.size(112.dp, 40.dp),
                profile = MutantBody.Btn.profile.with(overhang = 8.0),
            ) {
                Box(Modifier.size(112.dp, 40.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        "Готово",
                        Modifier.mutantCleanZone(),
                        style =
                            TextStyle(
                                fontFamily = fonts?.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = ink(colors.onPrimary),
                                platformStyle = raster,
                            ).cssLine(MutantFaceMetrics.Rubik),
                    )
                }
            }
        }
    }

    /** The words, each where the page's ink lies and in its colour; the Handjet ones may stand a row higher on macOS 26. */
    private val words =
        listOf(
            Triple("card", Pair(24 until 220, 20 until 80), false),
            Triple("title", Pair(22 until 306, 134 until 172), true),
            Triple("sp", Pair(22 until 90, 176 until 208), false),
            Triple("2h", Pair(200 until 306, 176 until 208), true),
            Triple("lp", Pair(22 until 90, 216 until 248), false),
            Triple("4h", Pair(200 until 306, 216 until 248), true),
            Triple("ok", Pair(200 until 300, 264 until 292), false),
        )

    @Test
    fun `the sheet screen matches the page - scrim, card, tear line, sheet and words - in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantSheet_Open_$theme"
            val full = cell(stem, theme, false)
            val bare = cell("MutantSheet_OpenBare_$theme", theme, true)
            lines += "$stem: bare ${bare.pixels} px; full $full"
            var ok = bare.pixels <= (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
            for ((name, area, handjet) in words) {
                val (xs, ys) = area

                fun inPart(p: Int) = p % full.width in xs && p / full.width in ys

                fun ext(
                    px: IntArray,
                    under: IntArray,
                ): Extent? =
                    extentOf(full.width, full.height) { inPart(it) && channelGap(px[it], under[it]) > THRESHOLD }
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
                val placed = kit != null && page != null && kit.near(page, px = 1, topPx = 0, above = above)
                if (!placed || colour > ComponentParity.INK_COLOUR) ok = false
            }
            if (!ok) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /** What a word adds over its bare twin to count: the card's words are dimmed by the scrim. */
        const val THRESHOLD = TEXT_INK / 2

        /**
         * Over the floor, measured on 2026-11-15 and never to be raised: in «Копия» the speckle under the print slip on
         * its 40 dp grid, over the sheet and the button (research §1.15).
         */
        val RESIDUAL = mapOf("MutantSheet_Open_Kopiya" to 40)
    }
}
