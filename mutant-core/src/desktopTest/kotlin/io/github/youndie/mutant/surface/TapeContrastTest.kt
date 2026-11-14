package io.github.youndie.mutant.surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-13's contrast under the Tape, read off the kit's own drawing: the same Tape over a plain text colour and over
 * its ground, compared pixel by pixel at a tick where the band crosses the middle (research §1.5, R2).
 *
 * The design's `contrast-after-tape` is the band's body over both (with the scanlines in «Эфир»). Its figures
 * are rounded by hand: they sit up to 0.12 from the exact composite of the engine's own layers, and the kit's
 * 8-bit pixels move that composite by up to 0.08 more (B-13). So the kit is held to what the figures decide —
 * the same side of WCAG's 4.5:1 for every declared pair — and to no gap a wrong layer would make. What is
 * thinner than the band — its 1 dp edge, a grain dot, the head's noise for one tick — is printed for B-38,
 * not judged here.
 */
@OptIn(ExperimentalTestApi::class)
class TapeContrastTest {
    private val tokens =
        Json
            .parseToJsonElement(File("../reference/design-system/mutant-tokens.json").readText())
            .jsonObject["mutant"]!!
            .jsonObject

    private fun colour(
        c: MutantColors,
        name: String,
    ): Color =
        when (name) {
            "text" -> c.text
            "text-secondary" -> c.textSecondary
            "text-disabled" -> c.textDisabled
            "text-accent" -> c.textAccent
            "text-osd" -> c.textOsd
            "text-error" -> c.textError
            "background" -> c.background
            "surface" -> c.surface
            "surface-variant" -> c.surfaceVariant
            "on-primary" -> c.onPrimary
            "primary" -> c.primary
            "amber" -> c.amber
            "on-no-signal" -> c.onNoSignal
            "on-no-signal-accent" -> c.onNoSignalAccent
            "no-signal" -> c.noSignal
            else -> error("no colour named $name")
        }

    private fun lum(c: Color): Double {
        fun ch(v: Float) = if (v <= .04045f) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
        return .2126 * ch(c.red) + .7152 * ch(c.green) + .0722 * ch(c.blue)
    }

    private fun ratio(
        a: Color,
        b: Color,
    ): Double = (maxOf(lum(a), lum(b)) + .05) / (minOf(lum(a), lum(b)) + .05)

    /** The Tape at [t] over a plain [fill], as pixels, 250 × 130 dp at density 1. */
    private fun taped(
        fill: Color,
        colors: MutantColors,
        t: Double,
    ): List<Color> {
        var px: List<Color> = emptyList()
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Canvas(Modifier.size(W.dp, H.dp).testTag("c")) {
                        drawRect(fill)
                        drawTape("contrast", t, colors)
                    }
                }
            }
            val map = onNodeWithTag("c").captureToImage().toPixelMap()
            px = List(W * H) { map[it % W, it / W] }
        }
        return px
    }

    private class Reading(
        val body: Double,
        val worst: Double,
        val edge: Double,
    )

    private fun read(
        colors: MutantColors,
        fg: String,
        bg: String,
    ): Reading {
        val t = 1050.0 // the band's top at 50 dp: rows 50 (edge), 51…72 (body), 73 (edge)
        val text = taped(colour(colors, fg), colors, t)
        val ground = taped(colour(colors, bg), colors, t)
        val ratios = DoubleArray(W * H) { ratio(text[it], ground[it]) }
        // The band's body: the most common ground colour of each body row is the band (and its scanline) alone.
        var body = Double.MAX_VALUE
        for (y in 51..72) {
            val row = (0 until W).map { y * W + it }
            val common =
                row
                    .groupingBy { ground[it] }
                    .eachCount()
                    .maxBy { it.value }
                    .key
            row.filter { ground[it] == common }.forEach { body = minOf(body, ratios[it]) }
        }
        val edge = (0 until W).minOf { ratios[50 * W + it] }
        return Reading(body, ratios.min(), edge)
    }

    @Test
    fun `the band's body keeps every contrast the design declares after the Tape`() {
        val lines = ArrayList<String>()
        var off = 0.0
        val flipped = ArrayList<String>()
        for ((theme, colors) in listOf("efir" to MutantPalettes.Efir, "kopiya" to MutantPalettes.Kopiya)) {
            val declared = tokens["contrast-after-tape"]!!.jsonObject[theme]!!.jsonObject
            for ((pair, value) in declared) {
                val (fg, bg) = pair.split(" / ")
                val r = read(colors, fg, bg)
                val d = value.jsonPrimitive.double
                off = maxOf(off, abs(r.body - d))
                if ((r.body >= AA) != (d >= AA)) flipped += "$theme $pair"
                lines +=
                    "$theme $pair: declared $d, band ${"%.2f".format(r.body)}, " +
                    "edge ${"%.2f".format(r.edge)}, worst ${"%.2f".format(r.worst)}"
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(
            flipped.isEmpty(),
            "on the other side of 4.5:1 from the design: $flipped\n" + lines.joinToString("\n"),
        )
        assertTrue(off < GROSS, "largest gap $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        const val W = 250
        const val H = 130

        /** WCAG 2.2 AA for body text. */
        const val AA = 4.5

        /** Far above the 0.2 the two roundings make; the band's alpha off by 0.03 moves a pair by more. */
        const val GROSS = .5
    }
}
