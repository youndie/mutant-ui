package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-137: the edge mosaic past the contour, a departure behind `MutantTheme(mosaicPastContour = true)`. Off, a body is
 * drawn as the design draws it; on, only the pixels under the edge row and the blocks outside change, the blocks
 * outside never touch the slot or the clean zone, and the contact sheets of both are written to `build/b-137/`.
 */
class MosaicPastContourTest {
    private val gen = MutantGenerator(cassette = "5")

    private fun slot(
        body: MutantBody,
        w: Double,
        h: Double,
    ): Slot {
        val p = body.profile
        // A label's zone in the middle, as a button's measured one stands.
        val tz = doubleArrayOf(w / 2 - 40, h / 2 - 10, 80.0, 20.0)
        return Slot(
            ox = 20.0,
            oy = 30.0,
            w = w,
            h = h,
            r = minOf(p.radius, w / 2, h / 2),
            over = p.overhang,
            dripShare = p.dripShare,
            wobble = p.wobble,
            round = p.round,
            calm = 1.0,
            dead = false,
            marks = p.marks,
            edge = p.mosaic,
            textZone = tz,
            cleanZone = doubleArrayOf(tz[0] - 2, tz[1] - 2, tz[2] + 4, tz[3] + 4),
        )
    }

    private val keys = (0 until 24).map { "button$it" }

    @Test
    fun `the blocks past the contour stand outside the slot and clear of the clean zone`() {
        var spilled = 0
        for (body in listOf(MutantBody.Btn, MutantBody.Card, MutantBody.Fab)) {
            for (key in keys) {
                val s =
                    slot(
                        body,
                        if (body ==
                            MutantBody.Fab
                        ) {
                            56.0
                        } else {
                            220.0
                        },
                        if (body == MutantBody.Card) 120.0 else 56.0,
                    )
                val g = gen.instance(s, key, false)
                val inside = Rect(s.ox.toFloat(), s.oy.toFloat(), (s.ox + s.w).toFloat(), (s.oy + s.h).toFloat())
                val cz = s.cleanZone
                val clean =
                    Rect(
                        (s.ox + cz[0]).toFloat(),
                        (s.oy + cz[1]).toFloat(),
                        (s.ox + cz[0] + cz[2]).toFloat(),
                        (
                            s.oy +
                                cz[1] +
                                cz[3]
                        ).toFloat(),
                    )
                for (t in g.spillTiles) {
                    val r = rectOf(t.x, t.y, t.size)
                    assertTrue(
                        r.intersect(inside).isEmptyOrNegative(),
                        "$body $key: a block past the contour at $r inside the slot",
                    )
                    assertTrue(
                        r.intersect(clean).isEmptyOrNegative(),
                        "$body $key: a block past the contour in the clean zone",
                    )
                    assertTrue(t.row < 0)
                }
                spilled += g.spillTiles.size
            }
        }
        println("B-137: $spilled blocks past the contour over ${3 * keys.size} bodies")
        assertTrue(spilled > 0, "no body put a block past its contour")
    }

    @Test
    fun `switched on, only the pixels under the edge row and the blocks past the contour change`() {
        var changed = 0
        for (key in keys.take(8)) {
            val s = slot(MutantBody.Btn, 220.0, 56.0)
            val g = gen.instance(s, key, false)
            val off = render(g, pastContour = false)
            val on = render(g, pastContour = true)
            // Where the blocks drawn over the outline stand, a pixel wider for the rasteriser's edge.
            val allowed =
                (g.mosaicTiles.filter { it.row == 0 } + g.spillTiles).map {
                    rectOf(it.x - 1, it.y - 1, it.size + 2)
                }
            for (i in off.indices) {
                if (off[i] == on[i]) continue
                val x = i % W + .5f
                val y = i / W + .5f
                assertTrue(
                    allowed.any { it.contains(Offset(x, y)) },
                    "$key: pixel ($x, $y) changed away from the mosaic",
                )
                changed++
            }
        }
        println("B-137: $changed pixels changed over 8 buttons at density 1")
        assertTrue(changed > 0, "the switch changed nothing")
    }

    @Test
    fun `the theme draws as the design by default`() {
        val g = gen.instance(slot(MutantBody.Btn, 220.0, 56.0), "button", false)
        assertEquals(render(g, pastContour = false).toList(), render(g, null).toList())
    }

    @Test
    fun `contact sheets of both, two themes at densities 1 and 2_625`() {
        val out = File("build/b-137").apply { mkdirs() }
        for (theme in listOf("Efir" to MutantPalettes.Efir, "Kopiya" to MutantPalettes.Kopiya)) {
            for (d in listOf(1f, 2.625f)) {
                val png = sheet(theme.second, d)
                File(out, "sheet-${theme.first.lowercase()}-${if (d == 1f) "1x" else "2.625x"}.png").writeBytes(png)
            }
        }
    }

    private fun sheet(
        colors: MutantColors,
        d: Float,
    ): ByteArray {
        val wDp = 760
        val hDp = 470
        val scene =
            ImageComposeScene((wDp * d).toInt(), (hDp * d).toInt(), Density(d)) {
                Row(
                    Modifier.fillMaxSize().background(colors.page).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                ) {
                    for (past in listOf(false, true)) {
                        MutantTheme(colors = colors, mosaicPastContour = past) {
                            ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                                Column(Modifier.width(336.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                                    BasicText(
                                        if (past) "B-137: past the contour" else "As designed",
                                        style = TextStyle(color = colors.text, fontSize = 14.sp),
                                    )
                                    Content()
                                }
                            }
                        }
                    }
                }
            }
        // The bodies are built off the first frame (B-75) and drawn from a later one.
        var image: org.jetbrains.skia.Image? = null
        repeat(6) { image = scene.render(it * 16_000_000L) }
        scene.close()
        return image!!.encodeToData(EncodedImageFormat.PNG)!!.bytes
    }

    @Composable
    private fun Content() {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            MutantButton("Воспроизвести", {}, key = "button")
            MutantButton("В очередь", {}, key = "queue")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            MutantFab(MutantIcons.Add, "Добавить", {}, key = "fab")
            MutantFab(MutantIcons.Edit, "Изменить", {}, key = "fab-b")
            MutantButton("Перемотать", {}, key = "rewind")
        }
        MutantCard(Modifier.width(320.dp), key = "card") {
            MutantCardTitle("Кассета 5")
            MutantCardSubtitle("Запись с эфира, 1987")
        }
        MutantTextField("", {}, Modifier.width(320.dp), placeholder = "Название записи", key = "field")
    }

    private fun render(
        g: Instance,
        pastContour: Boolean?,
    ): IntArray {
        val colors = MutantPalettes.Efir
        val frame = gen.frame(g, FieldCache(), STILL_MS, 0.0)
        val look =
            BodyLook(
                fill = colors.primary,
                highlight = highlightFor(colors.primary, colors),
                ink = colors.ink,
                outline = 3f,
            )
        val img = ImageBitmap(W, H)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(img), Size(W.toFloat(), H.toFloat())) {
            drawRect(colors.page)
            scale(1f, Offset.Zero) {
                val paths = BodyPaths.of(frame)
                val speckle = Speckle.of("5", "button", .35)
                if (pastContour == null) {
                    drawBody(g, paths, look, colors, speckle, Offset.Zero)
                } else {
                    drawBody(g, paths, look, colors, speckle, Offset.Zero, pastContour = pastContour)
                }
            }
        }
        return IntArray(W * H).also { img.readPixels(it) }
    }

    private fun rectOf(
        x: Double,
        y: Double,
        size: Double,
    ): Rect = Rect(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat())

    private fun Rect.isEmptyOrNegative(): Boolean = width <= 0 || height <= 0

    private companion object {
        const val W = 260
        const val H = 140
    }
}
