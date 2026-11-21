package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-158: the keyframe loss's band kept as pixels in memory ([BandPixels], the browser's) does what one Skia bitmap does,
 * pixel for pixel: blocks dragged within it, some over the ones before, opaque and with transparent pixels, a tint
 * screened over it, rectangles cleared, and a part copied from another band. Rectangles here stand on whole pixels; at a
 * fraction of one, Skia covers the edge pixel partly and the band takes or leaves it whole.
 */
class BandPixelsTest {
    private val w = 300
    private val h = 200

    private val block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {
        val r = Random(7)
        // Opaque ground under the left of it, none under the right: blocks with no transparent pixel, blocks with some.
        drawRect(Color(0xFF203040), size = Size(w * .6f, h.toFloat()))
        repeat(60) {
            val c = Color(r.nextInt(256), r.nextInt(256), r.nextInt(256), 60 + r.nextInt(196))
            drawCircle(c, 4f + r.nextFloat() * 40f, Offset(r.nextFloat() * w, r.nextFloat() * h))
        }
    }

    private fun pixelsOf(image: ImageBitmap) = IntArray(w * h).also { readImagePixels(image, it) }

    private fun BandPixels.pixels(): IntArray {
        val out = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(out), Size(w.toFloat(), h.toFloat())) {
            draw(this)
        }
        return pixelsOf(out)
    }

    @Test
    fun `the band in memory does what one bitmap does, every pixel`() {
        val band = BandPixels(w, 0, h)
        band.paint(Density(1f), LayoutDirection.Ltr, h, block)
        val one = ImageBitmap(w, h)
        val canvas = Canvas(one)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) { block() }
        assertEquals(pixelsOf(one).toList(), band.pixels().toList(), "painted alike")
        val clean = band.copy()
        val cleanOne = ImageBitmap(w, h).also { Canvas(it).drawImage(one, Offset.Zero, Paint()) }

        val r = Random(11)
        val size = IntSize(20, 14)
        val paint = Paint()
        repeat(300) {
            val at = IntOffset(r.nextInt(w - 24), r.nextInt(h - 24))
            val to = IntOffset(at.x + r.nextInt(-8, 9), at.y + r.nextInt(-8, 9))
            band.drawPart(band, at, size, to, paint)
            val part = ImageBitmap(size.width, size.height)
            Canvas(part).drawImageRect(one, at, size, IntOffset.Zero, size, Paint())
            canvas.drawImageRect(part, IntOffset.Zero, size, to, size, paint)
        }
        val tint =
            Paint().apply {
                color = Color(0xFFFF2E93).copy(alpha = .3f)
                blendMode = BlendMode.Screen
            }
        band.drawRect(10f, 30f, 250f, 120f, tint)
        canvas.drawRect(10f, 30f, 250f, 120f, tint)
        val clear = Paint().apply { blendMode = BlendMode.Clear }
        band.drawRect(40f, 60f, 90f, 100f, clear)
        canvas.drawRect(40f, 60f, 90f, 100f, clear)
        // A clean zone restored from the clean copy, as the loss does.
        band.drawPart(clean, IntOffset(120, 20), IntSize(60, 40), IntOffset(120, 20), Paint())
        canvas.drawImageRect(
            cleanOne,
            IntOffset(120, 20),
            IntSize(60, 40),
            IntOffset(120, 20),
            IntSize(60, 40),
            Paint(),
        )

        assertEquals(pixelsOf(one).toList(), band.pixels().toList(), "the band is not the one bitmap")
    }
}
