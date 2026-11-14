package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.Extent
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.illustration.IllustrationParts
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-29: the Components page's empty state — the cassette on the page's `illus` stage, its title and its two lines —
 * in both themes. The bodies against the bare twin's; the label, the reels and the words by where their ink lies.
 */
class EmptyStateParityTest {
    private fun cell(
        stem: String,
        theme: String,
        parts: IllustrationParts,
    ): DesignParity.Result {
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        val slots = ComponentParity.stageSlots("MutantEmptyState_Stage_$theme")
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(LocalMutantTextRaster provides ChromeText) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        Box(Modifier.fillMaxSize().background(ground)) {
                            ComponentParity.ProvideSlots(slots) {
                                EmptyStateBody(
                                    MutantIllustrationKind.Cassette,
                                    "Здесь пока пусто",
                                    Modifier.width(328.dp),
                                    "Запишите первую кассету, и она появится в эфире.",
                                    null,
                                    stage = "illus",
                                    parts = parts,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** The cassette's label and reels, the title, and the text's two lines: columns, rows. */
    private val parts =
        listOf(
            "label" to Pair(56 until 272, 14 until 52),
            "reels" to Pair(56 until 272, 66 until 106),
            "title" to Pair(0 until 328, 172 until 203),
            "line1" to Pair(0 until 328, 203 until 224),
            "line2" to Pair(0 until 328, 224 until 243),
        )

    @Test
    fun `the empty state matches the page - cassette, title and text - in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantEmptyState_Cassette_$theme"
            val full = cell(stem, theme, IllustrationParts.All)
            val bare = cell("MutantEmptyState_CassetteBare_$theme", theme, IllustrationParts.Bodies)
            lines += "$stem: bare ${bare.pixels} px; full $full"
            var ok = bare.pixels <= (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
            for ((name, area) in parts) {
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
                // The art's label and reels are held as the illustration's own parity holds them, within a pixel (B-14);
                // the words below it as text is held, the top row exactly (B-52).
                val topPx = if (name == "label" || name == "reels") 1 else 0
                if (kit == null || page == null || !kit.near(page, px = 1, topPx = topPx) || colour > 16) ok = false
            }
            if (!ok) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-14 and never to be raised: «Копия»'s speckle under the print slip, a few
         * phases recurring on its 40 dp grid across the cassette (research §1.15). The illustration's own parity runs in
         * «Эфир» only; here «Эфир» reads 3 px.
         */
        val RESIDUAL = mapOf("MutantEmptyState_Cassette_Kopiya" to 70)
    }
}
