package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A picture [width] px wide, kept as horizontal strips [stripHeight] px tall from [top] to [bottom]; transparent
 * elsewhere (B-88). Compose draws a bitmap by making an image of the whole of it, so a block dragged within one big
 * bitmap copied the whole picture; drawn from strips, a part copies only the strips it lies in. Every operation is the
 * one it stands for on one bitmap, pixel for pixel: the strips meet at whole pixels, and a part is read out whole
 * before it is drawn, as drawing a bitmap onto itself reads it.
 */
internal class StripPicture(
    val width: Int,
    private val top: Int,
    private val bottom: Int,
    private val stripHeight: Int,
) {
    private val tops = IntArray(maxOf(0, (bottom - top + stripHeight - 1) / stripHeight)) { top + it * stripHeight }
    private val strips = List(tops.size) { ImageBitmap(width, minOf(stripHeight, bottom - tops[it])) }
    private val canvases = strips.map { Canvas(it) }

    /**
     * Draws [block] into every strip, as into one bitmap [width] × [height] px: each strip draws it moved up to its own
     * place, and clips it at its edges.
     */
    fun paint(
        density: androidx.compose.ui.unit.Density,
        direction: androidx.compose.ui.unit.LayoutDirection,
        height: Int,
        block: DrawScope.() -> Unit,
    ) {
        val scope =
            androidx.compose.ui.graphics.drawscope
                .CanvasDrawScope()
        val size =
            androidx.compose.ui.geometry
                .Size(width.toFloat(), height.toFloat())
        for (i in strips.indices) {
            scope.draw(density, direction, canvases[i], size) {
                translate(0f, -tops[i].toFloat()) { block() }
            }
        }
    }

    /** A copy, strip for strip. */
    fun copy(): StripPicture =
        StripPicture(width, top, bottom, stripHeight).also { c ->
            val paint = Paint()
            for (i in strips.indices) c.canvases[i].drawImage(strips[i], Offset.Zero, paint)
        }

    /** Draws the picture at the scope's origin. */
    fun draw(scope: DrawScope) {
        for (i in strips.indices) scope.drawImage(strips[i], Offset(0f, tops[i].toFloat()))
    }

    /** As `drawRect` on the one bitmap: on each strip it meets, which clips it at its edges. */
    fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
    ) {
        for (i in strips.indices) {
            val t = tops[i]
            if (bottom <= t || top >= t + strips[i].height) continue
            canvases[i].drawRect(left, top - t, right, bottom - t, paint)
        }
    }

    /**
     * As `drawImageRect(from, at, size, to, size, paint)` on the one bitmap, [from] this picture or another of the same
     * strips: the source's part inside [from] is read out first, then drawn onto every strip the destination meets.
     */
    fun drawPart(
        from: StripPicture,
        at: IntOffset,
        size: IntSize,
        to: IntOffset,
        paint: Paint,
    ) {
        val x0 = maxOf(at.x, 0)
        val x1 = minOf(at.x + size.width, from.width)
        val y0 = maxOf(at.y, from.top)
        val y1 = minOf(at.y + size.height, from.bottom)
        val w = x1 - x0
        val h = y1 - y0
        if (w <= 0 || h <= 0) return
        val part = ImageBitmap(w, h)
        val read = Canvas(part)
        val copy = Paint()
        for (i in from.strips.indices) {
            val t = from.tops[i]
            val a = maxOf(y0, t)
            val b = minOf(y1, t + from.strips[i].height)
            if (b <= a) continue
            read.drawImageRect(
                from.strips[i],
                IntOffset(x0, a - t),
                IntSize(w, b - a),
                IntOffset(0, a - y0),
                IntSize(
                    w,
                    b - a,
                ),
                copy,
            )
        }
        val dx = to.x + x0 - at.x
        val dy = to.y + y0 - at.y
        for (i in strips.indices) {
            val t = tops[i]
            val a = maxOf(dy, t)
            val b = minOf(dy + h, t + strips[i].height)
            if (b <= a) continue
            canvases[i].drawImageRect(
                part,
                IntOffset(0, a - dy),
                IntSize(w, b - a),
                IntOffset(dx, a - t),
                IntSize(
                    w,
                    b - a,
                ),
                paint,
            )
        }
    }
}
