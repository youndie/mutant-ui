package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import io.github.youndie.mutant.gen.Instance

/**
 * A body rasterised at the device's pixels, and where its top-left pixel goes in the surface's own pixels. [whole] is
 * everything the body can ink, in the same pixels; the image covers all of it, or the part of it a band asked for (B-86).
 */
internal class RasterisedBody(
    val image: androidx.compose.ui.graphics.ImageBitmap,
    val offset: androidx.compose.ui.unit.IntOffset,
    val whole: Rect,
) {
    private val drawn =
        Rect(
            offset.x.toFloat(),
            offset.y.toFloat(),
            (offset.x + image.width).toFloat(),
            (offset.y + image.height).toFloat(),
        )

    /** Whether the image holds every pixel of the body inside [shown]; a null [shown] asks for the whole body. */
    fun covers(shown: Rect?): Boolean {
        val need = if (shown == null) whole else whole.intersect(shown)
        if (need.isEmpty) return true
        return need.left >= drawn.left && need.top >= drawn.top && need.right <= drawn.right &&
            need.bottom <= drawn.bottom
    }
}

/**
 * Draws [draw] (in the generator's scene coordinates) into an image covering everything a body can ink: its box,
 * the drips under it, the shadow, the slip, the outline's half width and the focus brackets (B-82). With a [band], in
 * the surface's pixels, only the rows and columns of that image inside it (B-86): the same pixels, cut on its grid.
 */
internal fun rasterisedBody(
    g: Instance,
    ox: Double,
    oy: Double,
    density: Float,
    direction: androidx.compose.ui.unit.LayoutDirection,
    band: Rect? = null,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
): RasterisedBody {
    val (left, top, right, bottom) = rasterBox(g)
    // On the surface's own pixel grid (B-115): the scene's point (ox, oy) is the surface's pixel (0, 0), and the image's
    // pixels are the surface's, so drawn at [at] nothing is resampled. Cut on whole scene units, a fractional density put
    // the image up to half a pixel off the body's vectors (2.9 % of pixels at 2.625).
    val ax = kotlin.math.floor((left - ox) * density).toInt()
    val ay = kotlin.math.floor((top - oy) * density).toInt()
    val w = kotlin.math.ceil((right - ox) * density).toInt() - ax
    val h = kotlin.math.ceil((bottom - oy) * density).toInt() - ay
    val at =
        androidx.compose.ui.unit
            .IntOffset(ax, ay)
    val whole = Rect(at.x.toFloat(), at.y.toFloat(), (at.x + w).toFloat(), (at.y + h).toFloat())
    // The band, on the whole image's pixel grid: whole pixels of it, so a cut image is the whole one's pixels.
    val x0 = band?.let { (kotlin.math.floor(it.left) - at.x).toInt().coerceIn(0, w) } ?: 0
    val y0 = band?.let { (kotlin.math.floor(it.top) - at.y).toInt().coerceIn(0, h) } ?: 0
    val x1 = band?.let { (kotlin.math.ceil(it.right) - at.x).toInt().coerceIn(x0, w) } ?: w
    val y1 = band?.let { (kotlin.math.ceil(it.bottom) - at.y).toInt().coerceIn(y0, h) } ?: h
    val image =
        androidx.compose.ui.graphics
            .ImageBitmap(maxOf(1, x1 - x0), maxOf(1, y1 - y0))
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit
            .Density(density),
        direction,
        androidx.compose.ui.graphics
            .Canvas(image),
        androidx.compose.ui.geometry
            .Size(image.width.toFloat(), image.height.toFloat()),
    ) {
        translate(-(x0 + ax).toFloat(), -(y0 + ay).toFloat()) {
            scale(density, Offset.Zero) { translate(-ox.toFloat(), -oy.toFloat()) { draw() } }
        }
    }
    return RasterisedBody(
        image,
        androidx.compose.ui.unit
            .IntOffset(at.x + x0, at.y + y0),
        whole,
    )
}

private const val RASTER_MARGIN = 20.0

/**
 * B-117: a body's still layers drawn once ([stillLayersImage]), kept for as long as its instance and the inputs of its
 * image are what they were. Built ahead off the frame's thread, so the three are held as one reference.
 */
internal class StillLayers {
    private var held: Triple<Instance, List<Any?>, StillImage>? = null

    fun of(
        g: Instance,
        inputs: List<Any?>,
        make: () -> StillImage,
    ): StillImage {
        val h = held
        if (h != null && h.first === g && h.second == inputs) return h.third
        return make().also { held = Triple(g, inputs, it) }
    }
}

/** Whether [surface] stands on whole pixels of the window, so an image on its grid is drawn without resampling (B-117). */
internal fun onWholePixels(surface: androidx.compose.ui.layout.LayoutCoordinates?): Boolean {
    if (surface == null || !surface.isAttached) return false
    val p = surface.localToWindow(Offset.Zero)
    return kotlin.math.abs(p.x - kotlin.math.round(p.x)) < 0.01f &&
        kotlin.math.abs(p.y - kotlin.math.round(p.y)) < 0.01f
}

/** What [rasterisedBody]'s image of [g] covers, in whole scene units. */
internal fun rasterBox(g: Instance): Rect {
    val el = g.slot
    return Rect(
        kotlin.math.floor(el.ox - RASTER_MARGIN).toFloat(),
        kotlin.math.floor(el.oy - RASTER_MARGIN - 4).toFloat(),
        kotlin.math.ceil(el.ox + el.w + RASTER_MARGIN).toFloat(),
        kotlin.math.ceil(el.oy + el.h + el.over + RASTER_MARGIN + 16).toFloat(),
    )
}
