package io.github.youndie.mutant.gen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-05: the floor of the body — what Chrome and Compose make of one outline. Chrome draws the vendored
 * generator's contour at t = 5000 (`scripts/design-references.mjs`, `Body_*`), Compose draws the port's
 * contour of the same instance the same way: the fill clipped by the contour (even-odd), the ink stroke
 * with round joins, nothing else. A pixel differs when any channel is more than 16 apart (viddik's
 * design-parity rule). Every later parity number of a body is read against this.
 */
@OptIn(ExperimentalTestApi::class)
class BodyFloorTest {
    private class Probe(
        val name: String,
        val type: String,
        val key: String,
        val w: Double,
        val h: Double,
        val r: Double,
        val over: Double,
        val dripShare: Double,
        val outline: Float,
        val marks: Boolean,
        val edge: Boolean,
        val wobble: Double,
        val round: Boolean,
        val content: DoubleArray?,
    )

    private class Theme(
        val name: String,
        val background: Color,
        val ink: Color,
        val fills: Map<String, Color>,
    )

    // As scripts/design-references.mjs builds them: the generator's `static K` and `static TH`.
    private val probes =
        listOf(
            Probe(
                "Button",
                "btn",
                "btn/a",
                200.0,
                48.0,
                12.0,
                24.0,
                .55,
                3f,
                true,
                true,
                1.0,
                false,
                doubleArrayOf(52.0, 15.0, 96.0, 18.0),
            ),
            Probe(
                "Card",
                "card",
                "card/a",
                328.0,
                80.0,
                10.0,
                24.0,
                .4,
                3f,
                true,
                true,
                1.0,
                false,
                doubleArrayOf(16.0, 14.0, 200.0, 36.0),
            ),
            Probe("Dot", "dot", "dot/a", 22.0, 22.0, 11.0, 0.0, 0.0, 2f, false, false, .35, true, null),
        )
    private val themes =
        listOf(
            Theme(
                "Efir",
                Color(0xFF10142A),
                Color(0xFF05060C),
                mapOf("btn" to Color(0xFFB6FF1A), "card" to Color(0xFF232B4A), "dot" to Color(0xFF2F3960)),
            ),
            Theme(
                "Kopiya",
                Color(0xFFEFE9D9),
                Color(0xFF14110F),
                mapOf("btn" to Color(0xFFB6FF1A), "card" to Color(0xFFE3DBC6), "dot" to Color(0xFFD6CCB2)),
            ),
        )

    private fun contour(
        p: Probe,
        key: String = p.key,
    ): GenPath {
        val tz = p.content?.let { doubleArrayOf(it[0] - 6, it[1] - 2, it[2] + 12, it[3] + 4) }
        val slot =
            Slot(
                ox = 20.0,
                oy = 20.0,
                w = p.w,
                h = p.h,
                r = minOf(p.r, p.w / 2, p.h / 2),
                over = p.over,
                dripShare = p.dripShare,
                wobble = p.wobble,
                round = p.round,
                calm = 1.0,
                dead = false,
                marks = p.marks,
                edge = p.edge,
                textZone = tz ?: doubleArrayOf(-99.0, -99.0, 0.0, 0.0),
                cleanZone =
                    tz?.let { doubleArrayOf(it[0] - 2, it[1] - 2, it[2] + 4, it[3] + 4) }
                        ?: doubleArrayOf(0.0, 0.0, 0.0, 0.0),
            )
        val gen = MutantGenerator(cassette = "5")
        return gen.frame(gen.instance(slot, key), FieldCache(), 5000.0).contour
    }

    private fun GenPath.toComposePath(): Path =
        Path().apply {
            fillType = PathFillType.EvenOdd
            for (loop in loops) {
                moveTo(loop[0] / 10f, loop[1] / 10f)
                var i = 2
                while (i < loop.size) {
                    quadraticTo(loop[i] / 10f, loop[i + 1] / 10f, loop[i + 2] / 10f, loop[i + 3] / 10f)
                    i += 4
                }
                close()
            }
        }

    /** The share of differing pixels, in percent, between the reference and Compose's drawing. */
    private fun floor(
        p: Probe,
        theme: Theme,
        outline: Float = p.outline,
        key: String = p.key,
    ): Double {
        val ref = File("src/desktopTest/snapshots/design/Body_${p.name}_${theme.name}.png")
        val image = Image.makeFromEncoded(ref.readBytes())
        val bitmap = Bitmap.makeFromImage(image)
        val path = contour(p, key).toComposePath()
        var percent = 0.0
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Canvas(Modifier.size(image.width.dp, image.height.dp)) {
                        drawRect(theme.background)
                        drawPath(path, theme.fills.getValue(p.type))
                        drawPath(path, theme.ink, style = Stroke(width = outline, join = StrokeJoin.Round))
                    }
                }
            }
            val actual = onRoot().captureToImage().toPixelMap()
            assertEquals(image.width, actual.width, "${p.name} width")
            assertEquals(image.height, actual.height, "${p.name} height")
            var differ = 0
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val a = actual[x, y]
                    val b = bitmap.getColor(x, y)
                    val db = abs((a.blue * 255).toInt() - (b and 0xFF))
                    val dg = abs((a.green * 255).toInt() - (b shr 8 and 0xFF))
                    val dr = abs((a.red * 255).toInt() - (b shr 16 and 0xFF))
                    if (maxOf(dr, dg, db) > 16) differ++
                }
            }
            percent = 100.0 * differ / (image.width * image.height)
        }
        return percent
    }

    @Test
    fun `the port's outline sits on Chrome's within the floor for every probe and theme`() {
        val lines = ArrayList<String>()
        var worst = 0.0
        for (p in probes) {
            for (theme in themes) {
                val f = floor(p, theme)
                worst = maxOf(worst, f)
                lines += "Body_${p.name}_${theme.name}: ${"%.3f".format(f)} %"
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(
            worst <= FLOOR_PERCENT,
            "worst ${"%.3f".format(worst)} % over the floor $FLOOR_PERCENT %:\n${lines.joinToString("\n")}",
        )
    }

    @Test
    fun `the floor separates a wrong outline from a right one`() {
        val button = probes.first()
        val efir = themes.first()
        val thicker = floor(button, efir, outline = button.outline * 1.2f)
        val wrongSeed = floor(button, efir, key = "btn/b")
        println(
            "Body_Button_Efir: outline +20 % ${"%.3f".format(thicker)} %, another key ${"%.3f".format(wrongSeed)} %",
        )
        assertTrue(thicker > FLOOR_PERCENT * 2, "an outline 20 % thicker reads ${"%.3f".format(thicker)} %")
        assertTrue(wrongSeed > FLOOR_PERCENT * 2, "another instance's outline reads ${"%.3f".format(wrongSeed)} %")
    }

    private companion object {
        /** Measured on 2026-11-14 (research §1.8): the worst probe and theme, rounded up. */
        const val FLOOR_PERCENT = 0.02
    }
}
