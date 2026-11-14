package io.github.youndie.mutant.surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
 * B-10: a resting body drawn by the kit's renderer against the Components page's, every text on the stage
 * made transparent. The slot is built from what the engine built (`manifest.json`): its place on the stage,
 * key, zones, fill, highlight, and the outline's width on that frame, which the engine seeds with its slot
 * count. A pixel differs when a channel is more than 16 apart. The diff of each is written to
 * `build/parity/` to be looked at.
 */
@OptIn(ExperimentalTestApi::class)
class BodyParityTest {
    private val dir = File("src/desktopTest/snapshots/design")
    private val manifest = Json.parseToJsonElement(File(dir, "manifest.json").readText()).jsonObject

    private fun hexColor(s: String): Color = Color(0xFF000000 or s.removePrefix("#").toLong(16))

    private fun rgb(s: String): Color {
        val (r, g, b) =
            Regex("""\d+""")
                .findAll(s)
                .map { it.value.toInt() }
                .take(3)
                .toList()
        return Color(r, g, b)
    }

    private fun parity(stem: String): Double {
        val ref =
            manifest["references"]!!.jsonArray.map { it.jsonObject }.single {
                it["stem"]!!.jsonPrimitive.content ==
                    stem
            }
        val s = ref["slot"]!!.jsonObject

        fun d(k: String) = s[k]!!.jsonPrimitive.double

        fun b(k: String) = d(k) != 0.0

        fun arr(k: String) = s[k]!!.jsonArray.map { it.jsonPrimitive.double }.toDoubleArray()
        val slot =
            Slot(
                ox = d("ox"),
                oy = d("oy"),
                w = d("w"),
                h = d("h"),
                r = d("r"),
                over = d("over"),
                dripShare = d("cf"),
                wobble = d("wob"),
                round = b("round"),
                calm = d("calm"),
                dead = b("dead"),
                marks = b("marks"),
                edge = b("edge"),
                textZone = arr("tz"),
                cleanZone = arr("cz"),
            )
        val gen = MutantGenerator(cassette = "5")
        val g = gen.instance(slot, s["key"]!!.jsonPrimitive.content)
        val frame = gen.frame(g, FieldCache(), 5000.0)
        val colors = MutantPalettes.Efir
        val fill = hexColor(s["fill"]!!.jsonPrimitive.content)
        val look =
            BodyLook(
                fill = fill,
                highlight = hexColor(s["hl"]!!.jsonPrimitive.content),
                ink = hexColor(s["stroke"]!!.jsonPrimitive.content),
                outline = d("drawnOutline").toFloat(),
                slip = d("slip").toFloat(),
                shadow = (s["shadow"] as? JsonPrimitive)?.takeIf { it.isString }?.let { hexColor(it.content) },
            )
        val dead = if (slot.dead) gen.deadPixels(g) else null
        val origin = s["origin"]!!.jsonArray.map { it.jsonPrimitive.double.toFloat() }
        val background = rgb(s["background"]!!.jsonPrimitive.content)
        val speckle = Speckle.of("5", s["stage"]!!.jsonPrimitive.content, .35 * slot.calm)
        val paths = BodyPaths.of(frame)
        val image = Image.makeFromEncoded(File(dir, "$stem.png").readBytes())
        val bitmap = Bitmap.makeFromImage(image)
        var percent = 0.0
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Canvas(Modifier.size(image.width.dp, image.height.dp)) {
                        drawRect(background)
                        translate(-origin[0], -origin[1]) {
                            drawBody(g, paths, look, colors, speckle, Offset.Zero, dead)
                        }
                    }
                }
            }
            val shot = onRoot().captureToImage()
            val actual = shot.toPixelMap()
            val diff =
                java.awt.image.BufferedImage(
                    image.width,
                    image.height,
                    java.awt.image.BufferedImage.TYPE_INT_RGB,
                )
            var differ = 0
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val a = actual[x, y]
                    val c = bitmap.getColor(x, y)
                    val m =
                        maxOf(
                            abs((a.red * 255).toInt() - (c shr 16 and 0xFF)),
                            abs((a.green * 255).toInt() - (c shr 8 and 0xFF)),
                            abs((a.blue * 255).toInt() - (c and 0xFF)),
                        )
                    if (m > 16) differ++
                    diff.setRGB(x, y, if (m > 16) 0xFF2020 else (c and 0xFFFFFF).let { ((it shr 2) and 0x3F3F3F) })
                }
            }
            percent = 100.0 * differ / (image.width * image.height)
            File("build/parity").mkdirs()
            ImageIO.write(shot.toAwtImage(), "png", File("build/parity/${stem}_ACTUAL.png"))
            ImageIO.write(diff, "png", File("build/parity/${stem}_DIFF.png"))
        }
        return percent
    }

    @Test
    fun `a resting button, card and field match the page's within the body's measured parity`() {
        val lines = ArrayList<String>()
        var worst = 0.0
        for (stem in listOf("Button_RestBody_Efir", "Card_RestBody_Efir", "Field_RestBody_Efir")) {
            val p = parity(stem)
            worst = maxOf(worst, p)
            lines += "$stem: ${"%.3f".format(p)} %"
        }
        println(lines.joinToString("\n"))
        assertTrue(worst <= BODY_PARITY_PERCENT, lines.joinToString("\n"))
    }

    private companion object {
        /** Measured 2026-11-14 (research §1.10). */
        const val BODY_PARITY_PERCENT = 0.02
    }
}
