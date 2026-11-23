package io.github.youndie.mutant.surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
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
import io.github.youndie.mutant.gen.pourFront
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

    /** One body of a stage, as the engine recorded it, drawn as the kit draws it. */
    private fun bodyOf(
        s: JsonObject,
        stage: String,
    ): androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit {
        fun d(k: String) = s[k]!!.jsonPrimitive.double

        fun b(k: String) =
            (s[k] as? JsonPrimitive)?.content.let { it == "true" || (it?.toDoubleOrNull() ?: 0.0) != 0.0 }

        fun arr(k: String) = s[k]!!.jsonArray.map { it.jsonPrimitive.double }.toDoubleArray()

        fun obj(k: String) = (s[k] as? JsonObject)
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
        val still = b("still")
        val state =
            when (s["state"]!!.jsonPrimitive.content) {
                "loading" -> BodyState.Loading
                "clean" -> BodyState.Success
                "disabled" -> BodyState.Disabled
                "error" -> BodyState.Error
                else -> BodyState.Rest
            }
        val heat = (s["heat"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
        val g = gen.instance(slot, s["key"]!!.jsonPrimitive.content, still)
        if (still && slot.edge && slot.w >= 40) gen.edgeMosaic(g)
        val t = (s["t"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 5000.0
        val colors = MutantPalettes.Efir
        val fill = hexColor(s["fill"]!!.jsonPrimitive.content)
        val highlight = hexColor(s["hl"]!!.jsonPrimitive.content)
        if (still) {
            val ow = d("drawnOutline").toFloat()
            return if (state == BodyState.Disabled) {
                val patterns = StagePatterns.of("5", stage)
                ({ drawDisabledBody(g, fill, highlight, colors, ow, patterns, Offset.Zero) })
            } else {
                ({ drawCleanBody(g, fill, highlight, hexColor(s["stroke"]!!.jsonPrimitive.content), ow) })
            }
        }
        val frame = gen.frame(g, FieldCache(), t, heat)
        val look =
            BodyLook(
                fill = boilFill(fill, colors, heat, t),
                highlight = highlight,
                ink = hexColor(s["stroke"]!!.jsonPrimitive.content),
                outline = d("drawnOutline").toFloat(),
                slip = d("slip").toFloat(),
                shadow = (s["shadow"] as? JsonPrimitive)?.takeIf { it.isString }?.let { hexColor(it.content) },
                baseOutline = d("ow").toFloat(),
            )
        val dead = if (slot.dead) gen.deadPixels(g) else null
        val speckle = Speckle.of("5", stage, .35 * slot.calm)
        val paths = BodyPaths.of(frame)
        val decor = gen.decorOf(g, state, b("brackets"), heat, t, colors)
        val pour =
            obj("pour")?.let { p ->
                val at = p["p"]!!.jsonPrimitive.double
                if (at >=
                    1
                ) {
                    null
                } else {
                    BodyPour(gen.pourFront(g, at).toPath(), hexColor(p["from"]!!.jsonPrimitive.content))
                }
            }
        val slide = obj("slide")?.let { it["p"]!!.jsonPrimitive.double to it["dx"]!!.jsonPrimitive.double }
        return {
            if (slide != null) {
                slid(
                    g,
                    slide.first,
                    slide.second,
                ) { drawBody(g, paths, look, colors, speckle, Offset.Zero, dead, decor, pour) }
            } else {
                drawBody(g, paths, look, colors, speckle, Offset.Zero, dead, decor, pour)
            }
        }
    }

    private fun parity(stem: String): Pair<Double, Int> {
        val ref =
            manifest["references"]!!.jsonArray.map { it.jsonObject }.single {
                it["stem"]!!.jsonPrimitive.content ==
                    stem
            }
        val s = ref["slot"]!!.jsonObject
        val stage = s["stage"]!!.jsonPrimitive.content
        val bodies = ref["stageSlots"]!!.jsonArray.map { bodyOf(it.jsonObject, stage) }
        val clip =
            (ref["stageClip"] as? kotlinx.serialization.json.JsonArray)?.map {
                it.jsonPrimitive.double.toFloat()
            }
        val tape = ref["tape"] as? JsonObject
        val scene: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {
            bodies.forEach { it() }
            if (tape != null) {
                fun n(k: String) = tape[k]!!.jsonPrimitive.double.toFloat()
                translate(n("x"), n("y")) {
                    drawTape(stage, n("t").toDouble(), MutantPalettes.Efir, n("w"), n("h"))
                }
            }
        }
        val draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {
            if (clip == null) {
                scene()
            } else {
                // The stage clips its content to its rounded box (the spec page's frames).
                val box =
                    androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                0f,
                                0f,
                                clip[0],
                                clip[1],
                                androidx.compose.ui.geometry
                                    .CornerRadius(clip[2]),
                            ),
                        )
                    }
                clipPath(box) { scene() }
            }
        }
        val origin = s["origin"]!!.jsonArray.map { it.jsonPrimitive.double.toFloat() }
        val background = rgb(s["background"]!!.jsonPrimitive.content)
        val image = Image.makeFromEncoded(File(dir, "$stem.png").readBytes())
        val bitmap = Bitmap.makeFromImage(image)
        var percent = 0.0
        var pixels = 0
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Canvas(Modifier.size(image.width.dp, image.height.dp)) {
                        drawRect(background)
                        translate(-origin[0], -origin[1]) { draw() }
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
            pixels = differ
            File("build/parity").mkdirs()
            ImageIO.write(shot.toAwtImage(), "png", File("build/parity/${stem}_ACTUAL.png"))
            ImageIO.write(diff, "png", File("build/parity/${stem}_DIFF.png"))
        }
        return percent to pixels
    }

    private fun check(stems: List<String>) {
        val lines = ArrayList<String>()
        var worst = 0
        for (stem in stems) {
            val (p, n) = parity(stem)
            worst = maxOf(worst, n)
            lines += "$stem: ${"%.3f".format(p)} %, $n px"
        }
        println(lines.joinToString("\n"))
        assertTrue(worst <= FLOOR_PIXELS, lines.joinToString("\n"))
    }

    @Test
    fun `a resting button, card and field match the page's within the body's measured parity`() =
        check(listOf("Button_RestBody_Efir", "Card_RestBody_Efir", "Field_RestBody_Efir"))

    @Test
    fun `the spec page's pour, slide and boil frames match within the body's measured parity`() =
        check(
            listOf("0", "30", "60", "100").map { "Chip_Pour${it}Body_Efir" } +
                listOf("0", "33", "66", "100").map { "Check_Pour${it}Body_Efir" } +
                listOf("0", "33", "66", "100").map { "Switch_Slide${it}Body_Efir" } + "Button_Boil50Body_Efir",
        )

    @Test
    fun `every state the page draws matches within the body's measured parity`() =
        check(
            listOf(
                "Button_PressedBody_Efir",
                "Button_LoadingBody_Efir",
                "Button_SuccessBody_Efir",
                "Button_DisabledBody_Efir",
                "Field_FocusedBody_Efir",
                "Field_ErrorBody_Efir",
                "Field_DisabledBody_Efir",
                "Card_DisabledBody_Efir",
            ),
        )

    @Test
    fun `the Tape over a card matches the spec page's drift frames within the body's measured parity`() =
        check(listOf("3000", "7350", "8050", "8900").map { "Tape_Drift${it}_Efir" })

    private companion object {
        /**
         * The body's floor in pixels (research §1.8): the outline probes' worst, 0.017 % of 240 × 124, is 5 isolated
         * pixels. A cell of any size may differ on that many; a defect is clusters, hundreds of pixels.
         */
        const val FLOOR_PIXELS = 5
    }
}
