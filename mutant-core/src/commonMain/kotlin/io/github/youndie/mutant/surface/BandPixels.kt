package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor

/**
 * The keyframe loss's band as pixels in this module's memory (B-158, B-144's route A): [width] px wide, rows [top] to
 * [bottom], premultiplied RGBA, a pixel an `Int` (red in its low byte, alpha in its high). The bodies are painted once
 * into one bitmap and read out; the blocks' drags, the clearing and the tints are copies and sums over the array; and
 * the band is handed to Skia as one image when it is next drawn after a change.
 *
 * In the browser Skia's raster pipeline runs a pixel at a time, and a drag drawn by it cost tens of milliseconds a
 * step (B-142…B-145); a Kotlin byte copy goes into Skiko's memory in one block since B-157.
 *
 * Its arithmetic is Skia's, pixel for pixel (`BandPixelsTest`): source-over and screen round with Skia's `div255`. A
 * rectangle's edge at a fraction of a pixel takes the pixels whose centres it holds, where Skia covers the edge pixel
 * partly: a block's edge in the browser, a pixel at a time.
 */
internal class BandPixels(
    override val width: Int,
    private val top: Int,
    private val bottom: Int,
) : LossPicture {
    private val height = maxOf(0, bottom - top)
    internal val px = IntArray(width * height)
    private var image: ImageBitmap? = null

    override fun paint(
        density: androidx.compose.ui.unit.Density,
        direction: androidx.compose.ui.unit.LayoutDirection,
        height: Int,
        block: DrawScope.() -> Unit,
    ) {
        if (this.height == 0 || width == 0) return
        // Drawn once, into one bitmap the band's size, where the strips drew it once a strip.
        val bitmap = ImageBitmap(width, this.height)
        CanvasDrawScope().draw(density, direction, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
            translate(0f, -top.toFloat()) { block() }
        }
        val read = IntArray(width * this.height)
        readImagePixels(bitmap, read)
        // Painted over what the band holds, as a strip's canvas would.
        for (i in px.indices) px[i] = over(read[i], px[i])
        image = null
    }

    /** [piece]'s pixels ([width] × [height], as [readImagePixels] reads them) drawn over the band at [x], [y] (B-159). */
    fun overlay(
        piece: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ) {
        for (r in 0 until height) {
            val row = y + r - top
            if (row < 0 || row >= this.height) continue
            for (c in 0 until width) {
                val col = x + c
                if (col < 0 || col >= this.width) continue
                val i = row * this.width + col
                px[i] = over(piece[r * width + c], px[i])
            }
        }
        image = null
    }

    /** [color] over the rectangle (its edges at pixel centres, as [drawRect]'s), without Skia (B-163). */
    fun fill(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: androidx.compose.ui.graphics.Color,
    ) {
        drawRect(left, top, right, bottom, Paint().apply { this.color = color })
    }

    override fun copy(): BandPixels = BandPixels(width, top, bottom).also { px.copyInto(it.px) }

    override fun draw(scope: DrawScope) {
        if (height == 0 || width == 0) return
        val img = image ?: imageOfPixels(px, width, height).also { image = it }
        scope.drawImage(img, Offset(0f, top.toFloat()))
    }

    override fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
    ) {
        val x0 = edge(left).coerceIn(0, width)
        val x1 = edge(right).coerceIn(0, width)
        val y0 = (edge(top) - this.top).coerceIn(0, height)
        val y1 = (edge(bottom) - this.top).coerceIn(0, height)
        if (x1 <= x0 || y1 <= y0) return
        when (paint.blendMode) {
            BlendMode.Clear -> {
                for (y in y0 until y1) px.fill(0, y * width + x0, y * width + x1)
            }

            BlendMode.Screen -> {
                val c = paint.color
                val a = c.alpha
                val s = rgba(c.red * a, c.green * a, c.blue * a, a)
                for (y in y0 until y1) for (i in y * width + x0 until y * width + x1) px[i] = screen(s, px[i])
            }

            else -> {
                val c = paint.color
                val a = c.alpha
                val s = rgba(c.red * a, c.green * a, c.blue * a, a)
                for (y in y0 until y1) for (i in y * width + x0 until y * width + x1) px[i] = over(s, px[i])
            }
        }
        image = null
    }

    override fun drawPart(
        from: LossPicture,
        at: IntOffset,
        size: IntSize,
        to: IntOffset,
        paint: Paint,
    ) {
        from as BandPixels
        val x0 = maxOf(at.x, 0)
        val x1 = minOf(at.x + size.width, from.width)
        val y0 = maxOf(at.y, from.top)
        val y1 = minOf(at.y + size.height, from.bottom)
        val w = x1 - x0
        val h = y1 - y0
        if (w <= 0 || h <= 0) return
        // Read out whole first, as drawing a bitmap onto itself reads it.
        val part = IntArray(w * h)
        for (r in 0 until h) {
            val row = (y0 + r - from.top) * from.width
            from.px.copyInto(part, r * w, row + x0, row + x1)
        }
        val dx = to.x + x0 - at.x
        val dy = to.y + y0 - at.y
        val copy = paint.blendMode == BlendMode.Src
        for (r in 0 until h) {
            val y = dy + r - top
            if (y < 0 || y >= height) continue
            for (c in 0 until w) {
                val x = dx + c
                if (x < 0 || x >= width) continue
                val i = y * width + x
                px[i] = if (copy) part[r * w + c] else over(part[r * w + c], px[i])
            }
        }
        image = null
    }

    private companion object {
        /** The pixel a rectangle's edge at [v] px stops before: the pixels whose centres lie inside it. */
        fun edge(v: Float): Int = floor(v + .5f).toInt()

        fun rgba(
            r: Float,
            g: Float,
            b: Float,
            a: Float,
        ): Int = q(r) or (q(g) shl 8) or (q(b) shl 16) or (q(a) shl 24)

        fun q(v: Float): Int = (v * 255f + .5f).toInt().coerceIn(0, 255)

        /** Skia's `div255`: (x + 128) · 257 >> 16. */
        fun div255(x: Int): Int = ((x + 128) * 257) ushr 16

        /** Premultiplied source over destination. */
        fun over(
            s: Int,
            d: Int,
        ): Int {
            val sa = s ushr 24
            if (sa == 255) return s
            if (sa == 0) return d
            val k = 255 - sa
            return overChannel(s, d, k, 0) or overChannel(s, d, k, 8) or overChannel(s, d, k, 16) or
                overChannel(s, d, k, 24)
        }

        fun overChannel(
            s: Int,
            d: Int,
            k: Int,
            shift: Int,
        ): Int = minOf(((s ushr shift) and 255) + div255(((d ushr shift) and 255) * k), 255) shl shift

        /** Premultiplied screen: s + d − s·d. */
        fun screen(
            s: Int,
            d: Int,
        ): Int = screenChannel(s, d, 0) or screenChannel(s, d, 8) or screenChannel(s, d, 16) or screenChannel(s, d, 24)

        fun screenChannel(
            s: Int,
            d: Int,
            shift: Int,
        ): Int {
            val sv = (s ushr shift) and 255
            val dv = (d ushr shift) and 255
            return minOf(sv + dv - div255(sv * dv), 255) shl shift
        }
    }
}

/** Whether the keyframe loss keeps its band as [BandPixels] here: the browser (B-158); elsewhere its strips. */
internal expect val lossBandInMemory: Boolean

/** [image]'s pixels into [into], premultiplied RGBA a pixel an `Int`, red in the low byte (B-158). */
internal expect fun readImagePixels(
    image: ImageBitmap,
    into: IntArray,
)

/** An immutable image of [px] (as [readImagePixels] reads them), [width] × [height] (B-158). */
internal expect fun imageOfPixels(
    px: IntArray,
    width: Int,
    height: Int,
): ImageBitmap
