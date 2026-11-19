package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * B-143, B-145: the operations of [StripPicture] land as on the one bitmap it stands for, pixel for pixel: blocks
 * dragged within it, some over the ones before, opaque ones (copied as pixels since B-145) and ones with transparent
 * pixels (drawn), and a tint. The shapes painted into it are not compared (a strip rasterises a
 * shape's edge moved up to its place, a channel off by one), only what is done to it after.
 */
class StripPictureTest {
    private val strip = 42

    /** The picture as one bitmap: what every [StripPicture] operation stands for. */
    private class OneBitmap(
        val width: Int,
        val height: Int,
    ) {
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
    }

    private fun paintBoth(
        width: Int,
        height: Int,
        seed: Int,
    ): Pair<StripPicture, OneBitmap> {
        val pic = StripPicture(width, 0, height, strip)
        val one = OneBitmap(width, height)
        val block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {
            val r = Random(seed)
            // Opaque ground under the left of it, none under the right: blocks with no transparent pixel, blocks with
            // some, and blocks across the edge between them.
            drawRect(Color(0xFF203040), size = Size(width * .6f, height.toFloat()))
            repeat(60) {
                val c = Color(r.nextInt(256), r.nextInt(256), r.nextInt(256), 60 + r.nextInt(196))
                drawCircle(c, 4f + r.nextFloat() * 40f, Offset(r.nextFloat() * width, r.nextFloat() * height))
            }
        }
        pic.paint(Density(1f), LayoutDirection.Ltr, height, block)
        // The one bitmap starts from the strips' own pixels, so what is compared is the operations after, not how a
        // shape's edge rasterises moved into a strip.
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            one.canvas,
            Size(width.toFloat(), height.toFloat()),
        ) { pic.draw(this) }
        return pic to one
    }

    /** A step's blocks: 8–24 px, dragged up to 8 px, some of them overlapping the ones before. */
    private fun blocks(
        width: Int,
        height: Int,
        seed: Int,
        n: Int,
    ): List<Pair<IntOffset, IntOffset>> {
        val r = Random(seed)
        return List(n) {
            val at = IntOffset(r.nextInt(width - 24), r.nextInt(height - 24))
            at to IntOffset(at.x + r.nextInt(-8, 9), at.y + r.nextInt(-8, 9))
        }
    }

    private fun StripPicture.pixels(height: Int): IntArray {
        val out = ImageBitmap(width, height)
        val scope =
            androidx.compose.ui.graphics.drawscope
                .CanvasDrawScope()
        scope.draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(out),
            Size(width.toFloat(), height.toFloat()),
        ) { draw(this) }
        return IntArray(width * height).also { out.readPixels(it) }
    }

    @Test
    fun `blocks dragged within the strips land as on one bitmap, every pixel`() {
        val (w, h) = 300 to 200
        val (pic, one) = paintBoth(w, h, seed = 7)
        val paint = Paint()
        val size = IntSize(20, 14)
        for ((at, to) in blocks(w, h, seed = 11, n = 300)) {
            pic.drawPart(pic, at, size, to, paint)
            // One bitmap drawn onto itself reads its part whole before drawing it.
            val part = ImageBitmap(size.width, size.height)
            Canvas(part).drawImageRect(one.bitmap, at, size, IntOffset.Zero, size, Paint())
            one.canvas.drawImageRect(part, IntOffset.Zero, size, to, size, paint)
        }
        val tint =
            Paint().apply {
                color = Color(0x66FF00FF)
                blendMode = BlendMode.Screen
            }
        pic.drawRect(10f, 30f, 250f, 120f, tint)
        one.canvas.drawRect(10f, 30f, 250f, 120f, tint)
        val expected = IntArray(w * h).also { one.bitmap.readPixels(it) }
        assertContentEquals(expected, pic.pixels(h), "the strips are not the one bitmap")
    }
}
