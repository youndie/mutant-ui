package io.github.youndie.mutant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * A composable against one of the Chrome references (`snapshots/design/`): rendered at density 1 at the
 * reference's size, a pixel differing when a channel is more than 16 apart, the diff written to
 * `build/parity/<stem>_DIFF.png` beside the kit's `_ACTUAL.png` to be looked at.
 */
internal object DesignParity {
    private val dir = File("src/desktopTest/snapshots/design")
    private val manifest by lazy { Json.parseToJsonElement(File(dir, "manifest.json").readText()).jsonObject }

    fun reference(stem: String): JsonObject =
        manifest["references"]!!.jsonArray.map { it.jsonObject }.single { it["stem"]!!.jsonPrimitive.content == stem }

    /** A CSS `rgb(…)` or `rgba(…)` as a colour. */
    fun rgb(s: String): Color {
        val v = Regex("""[\d.]+""").findAll(s).map { it.value.toFloat() }.toList()
        return Color(v[0] / 255, v[1] / 255, v[2] / 255, v.getOrElse(3) { 1f })
    }

    class Result(
        val stem: String,
        val pixels: Int,
        val percent: Double,
    ) {
        override fun toString(): String = "$stem: ${"%.3f".format(percent)} %, $pixels px"
    }

    @OptIn(ExperimentalTestApi::class)
    fun compare(
        stem: String,
        content: @Composable () -> Unit,
    ): Result {
        val ref = reference(stem)
        val w = ref["w"]!!.jsonPrimitive.int
        val h = ref["h"]!!.jsonPrimitive.int
        val bitmap = Bitmap.makeFromImage(Image.makeFromEncoded(File(dir, "$stem.png").readBytes()))
        var differ = 0
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Box(Modifier.size(w.dp, h.dp).testTag("parity")) { content() }
                }
            }
            val shot = onNodeWithTag("parity").captureToImage()
            val actual = shot.toPixelMap()
            val diff = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val a = actual[x, y]
                    val c = bitmap.getColor(x, y)
                    val m =
                        maxOf(
                            abs((a.red * 255).toInt() - (c shr 16 and 0xFF)),
                            abs((a.green * 255).toInt() - (c shr 8 and 0xFF)),
                            abs((a.blue * 255).toInt() - (c and 0xFF)),
                        )
                    if (m > 16) differ++
                    diff.setRGB(x, y, if (m > 16) 0xFF2020 else (c and 0xFFFFFF).let { (it shr 2) and 0x3F3F3F })
                }
            }
            File("build/parity").mkdirs()
            ImageIO.write(shot.toAwtImage(), "png", File("build/parity/${stem}_ACTUAL.png"))
            ImageIO.write(diff, "png", File("build/parity/${stem}_DIFF.png"))
        }
        return Result(stem, differ, 100.0 * differ / (w * h))
    }
}
