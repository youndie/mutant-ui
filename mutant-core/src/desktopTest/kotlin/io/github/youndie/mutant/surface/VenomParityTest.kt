package io.github.youndie.mutant.surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.VenomFrom
import io.github.youndie.mutant.gen.venomShapes
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-12: the venom's soft joins as the kit draws them — a graphics layer blurred by 3.2 dp with the alpha matrix
 * `18 · a − 8.2` — against the engine's SVG filter in Chrome, for the three cases of the designer's golden.
 */
@OptIn(ExperimentalTestApi::class)
class VenomParityTest {
    private val dir = File("src/desktopTest/snapshots/design")
    private val manifest = Json.parseToJsonElement(File(dir, "manifest.json").readText()).jsonObject

    private fun hex(s: String) = Color(0xFF000000 or s.removePrefix("#").toLong(16))

    private fun parity(stem: String): Pair<Double, Int> {
        val ref =
            manifest["references"]!!.jsonArray.map { it.jsonObject }.single {
                it["stem"]!!.jsonPrimitive.content ==
                    stem
            }
        val v = ref["venom"]!!.jsonObject
        val el = v["el"]!!.jsonObject
        val fx = v["fx"] as JsonObject

        fun e(k: String) = el[k]!!.jsonPrimitive.double

        fun f(k: String) = fx[k]?.jsonPrimitive?.double ?: 0.0
        val shapes =
            MutantGenerator("5").venomShapes(
                e("ox"),
                e("oy"),
                e("w"),
                e("h"),
                e("r"),
                v["key"]!!.jsonPrimitive.content,
                VenomFrom(f("dx"), f("dy"), f("px"), f("py")),
                v["p"]!!.jsonPrimitive.double,
            )
        val image = Image.makeFromEncoded(File(dir, "$stem.png").readBytes())
        val bitmap = Bitmap.makeFromImage(image)
        val c = MutantPalettes.Efir
        var result = 0.0 to 0
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    val ink = rememberGraphicsLayer()
                    val fill = rememberGraphicsLayer()
                    Canvas(Modifier.size(image.width.dp, image.height.dp)) {
                        gooLayer(ink, density)
                        gooLayer(fill, density)
                        drawRect(c.background)
                        drawVenom(shapes, c.ink, hex(el["fill"]!!.jsonPrimitive.content), e("ow").toFloat(), ink, fill)
                    }
                }
            }
            val shot = onRoot().captureToImage()
            val px = shot.toPixelMap()
            var differ = 0
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val a = px[x, y]
                    val b = bitmap.getColor(x, y)
                    val m =
                        maxOf(
                            abs((a.red * 255).toInt() - (b shr 16 and 0xFF)),
                            abs((a.green * 255).toInt() - (b shr 8 and 0xFF)),
                            abs((a.blue * 255).toInt() - (b and 0xFF)),
                        )
                    if (m > 16) differ++
                }
            }
            File("build/parity").mkdirs()
            ImageIO.write(shot.toAwtImage(), "png", File("build/parity/${stem}_ACTUAL.png"))
            result = 100.0 * differ / (image.width * image.height) to differ
        }
        return result
    }

    @Test
    fun `the venom's soft joins match the engine's filter`() {
        val lines = ArrayList<String>()
        var worst = 0.0
        for (case in listOf("Menu", "Segmented", "Drawer")) {
            for (p in listOf(10, 30, 50, 70)) {
                val stem = "Venom_$case${p}_Efir"
                val (pc, n) = parity(stem)
                worst = maxOf(worst, pc)
                lines += "$stem: ${"%.3f".format(pc)} %, $n px"
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(worst <= VENOM_PERCENT, lines.joinToString("\n"))
    }

    private companion object {
        /** Measured 2026-11-14 (research §1.11): edge pixels only, the blur differing under the alpha threshold. */
        const val VENOM_PERCENT = 0.9
    }
}
