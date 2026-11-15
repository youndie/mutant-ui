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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-36: the v1 page's three pull-to-refresh frames — the short tongue while pulling, the long boiling one while
 * refreshing, the clean pill when done — each with the page's screen around it, in both themes.
 */
class PullToRefreshParityTest {
    private enum class Frame(
        val stage: String,
    ) {
        Pulling("v1-ptr-a"),
        Refreshing("v1-ptr-b"),
        Done("v1-ptr-c"),
    }

    private fun cell(
        frame: Frame,
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val stem = "MutantPullToRefresh_${frame.name}${if (hidden) "Bare" else ""}_$theme"
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(
                    LocalMutantTextRaster provides ChromeText,
                    LocalMutantStage provides frame.stage,
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                Screen(frame, hidden)
                            }
                        }
                    }
                }
            }
        }
    }

    /** The page's screen: 300 × 210, rounded 12, 16 dp in; the header, its divider, and the component. */
    @Composable
    private fun Screen(
        frame: Frame,
        hidden: Boolean,
    ) {
        val colors = MutantTheme.colors
        val fonts = LocalMutantFonts.current
        val raster = LocalMutantTextRaster.current

        fun ink(c: Color) = if (hidden) Color.Transparent else c
        Column(
            Modifier
                .size(300.dp, 210.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.page)
                .padding(horizontal = 16.dp),
        ) {
            // Handjet 24's 27 dp line in a 44 dp row, centred: 8.5 dp down, painted at 9.
            Box(Modifier.height(44.dp)) {
                BasicText(
                    "Эфир",
                    Modifier.padding(top = 9.dp),
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = 24.sp,
                            color = ink(colors.text),
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Handjet),
                )
            }
            PullToRefreshBody(
                refreshing = frame == Frame.Refreshing,
                onRefresh = {},
                modifier = Modifier,
                refreshingLabel = "ОБНОВЛЯЕМ",
                doneLabel = "ОБНОВЛЕНО",
                key = if (frame == Frame.Done) "ptr" else "ptr",
                content = {
                    if (frame == Frame.Done) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                BasicText(
                                    "Настроечная таблица",
                                    style =
                                        TextStyle(
                                            fontFamily = fonts?.text,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ink(colors.text),
                                            platformStyle = raster,
                                        ).cssLine(MutantFaceMetrics.Rubik),
                                )
                                BasicText(
                                    "Копия 4-го поколения",
                                    style =
                                        TextStyle(
                                            fontFamily = fonts?.text,
                                            fontSize = 13.sp,
                                            color = ink(colors.textSecondary),
                                            platformStyle = raster,
                                        ).cssLine(MutantFaceMetrics.Rubik),
                                )
                            }
                            MutantDivider(key = "1")
                            BasicText(
                                "Эфир, ночь на вторник",
                                style =
                                    TextStyle(
                                        fontFamily = fonts?.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ink(colors.text),
                                        platformStyle = raster,
                                    ).cssLine(MutantFaceMetrics.Rubik),
                            )
                        }
                    } else {
                        val widths = if (frame == Frame.Pulling) listOf(.72f, .48f, .64f) else listOf(.72f, .48f)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            widths.forEachIndexed { i, w ->
                                MutantCocoon(Modifier.fillMaxWidth(w).height(16.dp), key = "$i")
                            }
                        }
                    }
                },
                pull = if (frame == Frame.Pulling) 40f else null,
                done = frame == Frame.Done,
                heat = if (frame == Frame.Refreshing) 1.0 else null,
                hidden = hidden,
            )
        }
    }

    /** Each frame's words: the header (Handjet), and the tongue's, the pill's or the list's. */
    private fun words(frame: Frame): List<Triple<String, Pair<IntRange, IntRange>, Boolean>> =
        listOf(Triple("header", Pair(0 until 300, 0 until 44), true)) +
            when (frame) {
                Frame.Pulling -> {
                    emptyList()
                }

                Frame.Refreshing -> {
                    listOf(Triple("tongue", Pair(88 until 212, 44 until 80), true))
                }

                Frame.Done -> {
                    listOf(
                        Triple("pill", Pair(98 until 202, 56 until 88), true),
                        Triple("list", Pair(16 until 300, 98 until 210), false),
                    )
                }
            }

    @Test
    fun `the page's three frames - screen, tongue or pill whole, the words where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (frame in Frame.entries) {
                val stem = "MutantPullToRefresh_${frame.name}_$theme"
                val full = cell(frame, theme, false)
                val bare = cell(frame, theme, true)
                lines += "$stem: bare ${bare.pixels} px (max ${bare.maxChannel}); full $full"
                var ok = bare.pixels <= (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
                for ((name, area, handjet) in words(frame)) {
                    val (xs, ys) = area

                    fun inPart(p: Int) = p % full.width in xs && p / full.width in ys

                    fun ext(
                        px: IntArray,
                        under: IntArray,
                    ): Extent? =
                        extentOf(full.width, full.height) {
                            inPart(it) &&
                                channelGap(px[it], under[it]) > TEXT_INK
                        }
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
                    val topPx = TOP_RESIDUAL["$stem/$name"] ?: 0
                    if (kit == null || page == null || !kit.near(page, px = 1, topPx = topPx, above = above) ||
                        colour > ComponentParity.INK_COLOUR
                    ) {
                        ok = false
                    }
                }
                if (!ok) off += stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-15 and never to be raised: in «Копия» the speckle under the print slip on
         * its 40 dp grid, over the tongue (research §1.15).
         */
        val RESIDUAL = mapOf("MutantPullToRefresh_Pulling_Kopiya" to 6, "MutantPullToRefresh_Refreshing_Kopiya" to 19)

        /**
         * A top row apart, measured on 2026-11-15: the list's first line in «Эфир», light on dark, its top row on the
         * ink threshold — over it in the kit, under it on the page (B-24's cause, as the dialog's title). In «Копия» it
         * lies exactly.
         */
        val TOP_RESIDUAL = mapOf("MutantPullToRefresh_Done_Efir/list" to 1)
    }
}
