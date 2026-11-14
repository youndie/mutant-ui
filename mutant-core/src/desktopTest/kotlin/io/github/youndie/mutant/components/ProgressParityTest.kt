package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-27: the list stage's two progress rows at the engine's still time (tick 40): the determinate track with its value,
 * and the running band with the reel. The tracks, segments, band and reel are held pixel by pixel; the value's digits
 * by where their ink lies and their colour.
 */
class ProgressParityTest {
    private fun cell(
        stem: String,
        theme: String,
        content: @Composable () -> Unit,
    ): DesignParity.Result {
        val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(LocalMutantTextRaster provides ChromeText) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        Box(Modifier.fillMaxSize().background(ground)) { content() }
                    }
                }
            }
        }
    }

    @Test
    fun `both progress rows match the page, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val det =
                cell("MutantProgress_Determinate_$theme", theme) {
                    MutantLinearProgress(.42f, Modifier.fillMaxWidth())
                }
            // The value's digits stand past the track's end; the track and its segments before it.
            val g = det.reference[0]
            val split = 328 - 48
            var track = 0
            for (i in 0 until det.width * det.height) {
                if (i % det.width < split && channelGap(det.actual[i], det.reference[i]) > 16) track++
            }

            fun digits(px: IntArray) =
                extentOf(det.width, det.height) {
                    it % det.width >= split &&
                        channelGap(px[it], g) > TEXT_INK
                }
            val kit = digits(det.actual)
            val page = digits(det.reference)
            val inkiest = { px: IntArray ->
                px[(0 until det.width * det.height).filter { it % det.width >= split }.maxBy { channelGap(px[it], g) }]
            }
            val colour = channelGap(inkiest(det.actual), inkiest(det.reference))
            lines += "Determinate_$theme: track $track px; digits kit $kit page $page colour $colour"
            val trackOver = track > (RESIDUAL["Determinate_$theme"] ?: ComponentParity.FLOOR_PIXELS)
            val digitsOff = kit == null || page == null || !kit.near(page, 1, 0) || colour > 16
            if (trackOver || digitsOff) off += "Determinate_$theme"
            val ind =
                cell("MutantProgress_Indeterminate_$theme", theme) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MutantLinearProgress(Modifier.weight(1f))
                        MutantCircularProgress()
                    }
                }
            lines += "Indeterminate_$theme: ${ind.pixels} px"
            if (ind.pixels >
                (RESIDUAL["Indeterminate_$theme"] ?: ComponentParity.FLOOR_PIXELS)
            ) {
                off += "Indeterminate_$theme"
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over the floor, measured on 2026-11-14 and never to be raised. The determinate fill ends at 42 % of a track
         * whose width Chrome keeps fractional (the row less its value's width), one anti-aliased column 12 rows tall.
         * The reel: Chrome rasterises its `border-radius` circles and turns the holes as a layer, the kit draws its
         * circles turned — their edges apart, the band beside it exact (and a reel stood at the element's 0.19 px reads
         * 131 and 178: Chrome draws it whole).
         */
        val RESIDUAL =
            mapOf(
                "Determinate_Efir" to 12,
                "Determinate_Kopiya" to 12,
                "Indeterminate_Efir" to 43,
                "Indeterminate_Kopiya" to 63,
            )
    }
}
