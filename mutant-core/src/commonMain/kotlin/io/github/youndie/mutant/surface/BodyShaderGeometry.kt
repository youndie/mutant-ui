package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Rect
import io.github.youndie.mutant.gen.GenPath
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * B-128: a path's segments, sorted into the bins of a grid over a body's box, for the body shader to read the edge the
 * paths draw. Two opaque textures as RGBA bytes, the data in red, green and blue only (a shader reads a texel
 * premultiplied by its alpha): [bins], one texel a bin (the first entry's index in two bytes, the entry count plus 128
 * where the bin's centre is inside the path by the even-odd rule); [segs], three texels an entry (its two ends, each
 * coordinate in two bytes, [SEG_SCALE] to the dp from the grid's origin).
 */
internal class SegmentBins(
    val bins: ByteArray,
    val binsW: Int,
    val binsH: Int,
    val segs: ByteArray,
    val segsH: Int,
    val originX: Float,
    val originY: Float,
    /** The most entries in one bin; the shader reads at most [MAX_BIN_ENTRIES]. */
    val fullest: Int,
)

/** Segments a quadratic of a [GenPath] is cut into: under 0.03 dp off the curve at a mosaic step's sharpest bend. */
internal const val SEGMENTS_A_QUADRATIC: Int = 3

/** The side of a bin, dp. */
internal const val BIN_DP: Float = 2.5f

/** Texels a row of the segment texture; a multiple of three, so an entry's three texels share a row. */
internal const val SEGS_W: Int = 1023

/** Units of a segment's coordinate a dp. */
internal const val SEG_SCALE: Float = 64f

/** The entries the shader reads in one bin at most; under 128, as the count shares its byte with the inside flag. */
internal const val MAX_BIN_ENTRIES: Int = 96

/**
 * Bins [path]'s segments on a grid of [BIN_DP] over [box] (scene dp), each listed in every bin within [reach] of it, so
 * a pixel finds in its own bin every segment closer than [reach].
 */
internal fun segmentBins(
    path: GenPath,
    box: Rect,
    reach: Float,
): SegmentBins {
    // The path's segments, flattened: x0, y0, x1, y1.
    val s = ArrayList<Float>()
    for (loop in path.loops) {
        var px = loop[0] / 10f
        var py = loop[1] / 10f
        val sx = px
        val sy = py
        var i = 2
        while (i < loop.size) {
            val cx = loop[i] / 10f
            val cy = loop[i + 1] / 10f
            val ex = loop[i + 2] / 10f
            val ey = loop[i + 3] / 10f
            for (k in 1..SEGMENTS_A_QUADRATIC) {
                val t = k.toFloat() / SEGMENTS_A_QUADRATIC
                val u = 1 - t
                val qx = u * u * px + 2 * u * t * cx + t * t * ex
                val qy = u * u * py + 2 * u * t * cy + t * t * ey
                val ax = if (k == 1) px else s[s.size - 2]
                val ay = if (k == 1) py else s[s.size - 1]
                s += ax
                s += ay
                s += qx
                s += qy
            }
            px = ex
            py = ey
            i += 4
        }
        // `close()`: back to the loop's start, if the last quadratic did not end there.
        if (px != sx || py != sy) {
            s += px
            s += py
            s += sx
            s += sy
        }
    }
    val n = s.size / 4
    val ox = box.left
    val oy = box.top
    val bw = max(1, ceil(box.width / BIN_DP).toInt())
    val bh = max(1, ceil(box.height / BIN_DP).toInt())

    fun binRange(
        lo: Float,
        hi: Float,
        o: Float,
        count: Int,
    ): IntRange =
        max(0, floor((lo - reach - o) / BIN_DP).toInt())..min(count - 1, floor((hi + reach - o) / BIN_DP).toInt())

    val counts = IntArray(bw * bh)
    for (k in 0 until n) {
        val x0 = s[4 * k]
        val y0 = s[4 * k + 1]
        val x1 = s[4 * k + 2]
        val y1 = s[4 * k + 3]
        for (j in binRange(min(y0, y1), max(y0, y1), oy, bh)) {
            for (i in binRange(min(x0, x1), max(x0, x1), ox, bw)) counts[j * bw + i]++
        }
    }
    val start = IntArray(bw * bh + 1)
    for (b in 0 until bw * bh) start[b + 1] = start[b] + counts[b]
    val total = start[bw * bh]
    val entries = IntArray(total)
    val fill = start.copyOf()
    for (k in 0 until n) {
        val x0 = s[4 * k]
        val y0 = s[4 * k + 1]
        val x1 = s[4 * k + 2]
        val y1 = s[4 * k + 3]
        for (j in binRange(min(y0, y1), max(y0, y1), oy, bh)) {
            for (i in binRange(min(x0, x1), max(x0, x1), ox, bw)) entries[fill[j * bw + i]++] = k
        }
    }
    // Whether each bin's centre is inside, by the even-odd rule: the crossings of its row's centre line left of it.
    val bins = ByteArray(bw * bh * 4)
    val xs = FloatArray(n)
    for (j in 0 until bh) {
        val y = oy + (j + .5f) * BIN_DP
        var m = 0
        for (k in 0 until n) {
            val y0 = s[4 * k + 1]
            val y1 = s[4 * k + 3]
            if ((y0 > y) != (y1 > y)) {
                val x0 = s[4 * k]
                val x1 = s[4 * k + 2]
                xs[m++] = x0 + (y - y0) / (y1 - y0) * (x1 - x0)
            }
        }
        for (i in 0 until bw) {
            val x = ox + (i + .5f) * BIN_DP
            var c = 0
            for (q in 0 until m) if (xs[q] < x) c++
            val b = j * bw + i
            bins[4 * b] = (start[b] shr 8).toByte()
            bins[4 * b + 1] = (start[b] and 255).toByte()
            bins[4 * b + 2] = (min(127, counts[b]) + (if (c % 2 == 1) 128 else 0)).toByte()
            bins[4 * b + 3] = 255.toByte()
        }
    }
    val segsH = max(1, ceil(total * 3f / SEGS_W).toInt())
    val segs = ByteArray(SEGS_W * segsH * 4) { if (it % 4 == 3) 255.toByte() else 0 }

    fun put(
        hi: Int,
        lo: Int,
        v: Float,
        o: Float,
    ) {
        val u = ((v - o) * SEG_SCALE).roundToInt().coerceIn(0, 65535)
        segs[hi] = (u shr 8).toByte()
        segs[lo] = (u and 255).toByte()
    }
    // Entry e's texels 3e…3e + 2 hold its four coordinates in the bytes r g b, r g b, r g: (x0 y0) (x1 y1).
    val slots = intArrayOf(0, 1, 2, 4, 5, 6, 8, 9)
    for (e in 0 until total) {
        val k = entries[e]
        val t = 12 * e
        put(t + slots[0], t + slots[1], s[4 * k], ox)
        put(t + slots[2], t + slots[3], s[4 * k + 1], oy)
        put(t + slots[4], t + slots[5], s[4 * k + 2], ox)
        put(t + slots[6], t + slots[7], s[4 * k + 3], oy)
    }
    return SegmentBins(bins, bw, bh, segs, segsH, ox, oy, counts.maxOrNull() ?: 0)
}
