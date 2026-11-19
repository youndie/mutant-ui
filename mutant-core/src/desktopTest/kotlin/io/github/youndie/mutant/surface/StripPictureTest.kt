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
import kotlin.test.assertTrue

/**
 * B-143: [StripPicture] is one bitmap kept as strips, pixel for pixel, and a block read out of it costs the block, not
 * the strips it lies in. A part was read by drawing a whole mutable strip, which Compose on Skiko turns into an image
 * of the whole strip on every draw: a running keyframe loss held a phone's frames at 110–220 ms (B-142).
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
            repeat(60) {
                val c = Color(r.nextInt(256), r.nextInt(256), r.nextInt(256), 60 + r.nextInt(196))
                drawCircle(c, 4f + r.nextFloat() * 40f, Offset(r.nextFloat() * width, r.nextFloat() * height))
            }
        }
        pic.paint(Density(1f), LayoutDirection.Ltr, height, block)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            one.canvas,
            Size(width.toFloat(), height.toFloat()),
            block,
        )
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
        val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
        scope.draw(Density(1f), LayoutDirection.Ltr, Canvas(out), Size(width.toFloat(), height.toFloat())) { draw(this) }
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
        val tint = Paint().apply { color = Color(0x66FF00FF); blendMode = BlendMode.Screen }
        pic.drawRect(10f, 30f, 250f, 120f, tint)
        one.canvas.drawRect(10f, 30f, 250f, 120f, tint)
        val expected = IntArray(w * h).also { one.bitmap.readPixels(it) }
        assertContentEquals(expected, pic.pixels(h), "the strips are not the one bitmap")
    }

    private fun stepNanos(
        width: Int,
        rounds: Int,
    ): Long {
        val h = 168
        val (pic, _) = paintBoth(width, h, seed = 3)
        // The same blocks in the same 300 px whatever the width: only the strips' width differs.
        val steps = blocks(300, h, seed = 5, n = 120)
        val paint = Paint()
        val size = IntSize(16, 16)
        var best = Long.MAX_VALUE
        repeat(rounds) {
            val t0 = System.nanoTime()
            for ((at, to) in steps) pic.drawPart(pic, at, size, to, paint)
            best = minOf(best, System.nanoTime() - t0)
        }
        return best
    }

    @Test
    fun `a step's blocks cost the blocks, not the width of the strips they lie in`() {
        stepNanos(400, 5)
        stepNanos(12_000, 2)
        val narrow = stepNanos(400, 9)
        val wide = stepNanos(12_000, 9)
        // Copying whole strips, 30 times the width costs about 30 times as much; copying the blocks, about the same.
        assertTrue(
            wide < narrow * 4,
            "120 blocks: ${narrow / 1000} µs on 400 px, ${wide / 1000} µs on 12000 px (×${wide.toDouble() / narrow})",
        )
    }
}
