package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
 * B-28: the dialog demo's frame — two still cards behind the scrim, the pause band, the clean window with its title,
 * text and actions — in both themes. Everything but the words is held against the bare twin's; the words one by one.
 */
class DialogParityTest {
    private fun cell(
        stem: String,
        theme: String,
        hidden: Boolean,
    ): DesignParity.Result {
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        // Where the engine stood each body: the cards on `dialogdemo`, the window and its button on `dialogwin`.
        val cards = ComponentParity.stageSlots("MutantDialog_Card_$theme")
        val window = ComponentParity.stageSlots("MutantDialog_Window_$theme")
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(LocalMutantTextRaster provides ChromeText) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        Box(Modifier.fillMaxSize().background(ground)) {
                            // The page's frame: 328 × 262, its corners rounded 12.
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))) {
                                Background(cards, hidden)
                                Window(window, hidden)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Background(
        slots: Map<String, Any>,
        hidden: Boolean,
    ) {
        CompositionLocalProvider(LocalMutantStage provides "dialogdemo", LocalCardHidden provides hidden) {
            ComponentParity.ProvideSlots(slots) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
                    MutantCard(Modifier.width(328.dp), key = "card") {
                        MutantCardTitle("Без подписи №3")
                        MutantCardSubtitle("Сторона A · 46 мин")
                    }
                    MutantCard(Modifier.width(328.dp), key = "card-b") {
                        MutantCardTitle("Эфир, ночь на вторник")
                        MutantCardSubtitle("Записано поверх · 12 мин")
                    }
                }
            }
        }
    }

    @Composable
    private fun Window(
        slots: Map<String, Any>,
        hidden: Boolean,
    ) {
        CompositionLocalProvider(LocalMutantStage provides "dialogwin") {
            ComponentParity.ProvideSlots(slots) {
                DialogFrame(
                    true,
                    "Стереть запись?",
                    {},
                    Modifier,
                    "Плёнку можно будет записать заново.",
                    null,
                    {
                        MutantDialogAction("Отмена", {})
                        MutantDialogAction("Стереть", {}, primary = true)
                    },
                    playIn = false,
                    windowTop = 52.dp,
                    hidden = hidden,
                )
            }
        }
    }

    /** The words of the window, each where the page's ink lies and in its colour. */
    private val words =
        listOf(
            "title" to Pair(44 until 304, 64 until 98),
            "text" to Pair(44 until 304, 98 until 152),
            "dismiss" to Pair(140 until 196, 160 until 210),
            "confirm" to Pair(196 until 304, 160 until 210),
        )

    @Test
    fun `the dialog matches the page - scrim, band, cards, window and words - in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantDialog_Open_$theme"
            val full = cell(stem, theme, false)
            val bare = cell("MutantDialog_OpenBare_$theme", theme, true)
            lines += "$stem: bare ${bare.pixels} px; full $full"
            var ok = bare.pixels <= (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
            for ((name, area) in words) {
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
                val topPx = TOP_RESIDUAL["$stem/$name"] ?: 0
                // The title is Handjet: a row higher on macOS 26 alone (B-74); the text and the actions are Rubik.
                val above = if (name == "title") HANDJET_ROWS_ABOVE else 0
                val placed = kit != null && page != null && kit.near(page, px = 1, topPx = topPx, above = above)
                if (!placed || colour > 16) ok = false
            }
            if (!ok) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-14 and never to be raised: in «Копия» the speckle under the print slip on
         * the button's 40 dp grid and its outline over its fill (research §1.15, §1.13); in «Эфир» one highlight pixel
         * on the button and the pause band's light edge where it crosses a card's drip, a row of 6.
         */
        val RESIDUAL = mapOf("MutantDialog_Open_Efir" to 7, "MutantDialog_Open_Kopiya" to 11)

        /**
         * A top row apart, measured on 2026-11-14. The actions, both themes: the page lays its row at 112.6 dp (two lines
         * of 20.3 above it), the engine paints the button's body at the slot's rounded 113, and the words stay at the
         * fraction a row above; the kit lays body and words together, the body where the engine's is. The title in
         * «Эфир»: the light-on-dark mask on the threshold (B-24's cause); in «Копия» it lies exactly.
         */
        val TOP_RESIDUAL =
            mapOf(
                "MutantDialog_Open_Efir/title" to 1,
                "MutantDialog_Open_Efir/dismiss" to 1,
                "MutantDialog_Open_Efir/confirm" to 1,
                "MutantDialog_Open_Kopiya/dismiss" to 1,
                "MutantDialog_Open_Kopiya/confirm" to 1,
            )
    }
}
