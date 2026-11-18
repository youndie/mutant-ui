package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Rect
import io.github.youndie.mutant.gen.GenPath
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * B-129: the signed distance to a path's curve on a grid over a body's box, negative inside, computed once a tick for the
 * body shader to read bilinearly. Exact within [band] of the curve (the distance to its nearest segment, inside by the
 * path's fill rule), [band] beyond it. [texels] is an opaque RGBA texture, the value in red and green (16 bits over
 * −[band]…[band]); a shader reads a texel premultiplied by its alpha, so the alpha carries nothing.
 */
internal class DistanceGrid(
    val texels: ByteArray,
    val nx: Int,
    val ny: Int,
    val originX: Float,
    val originY: Float,
    val band: Float,
)

/** The grid's step, dp. */
internal const val GRID_DP: Float = .75f

/** Segments a quadratic of a [GenPath] is cut into: under 0.03 dp off the curve at a mosaic step's sharpest bend. */
internal const val SEGMENTS_A_QUADRATIC: Int = 3

/**
 * The signed distance to [path]'s curve over [box], exact within [band]. [nonZero] fills as the flow lines are filled;
 * otherwise even-odd, as the contour and the highlight are.
 */
internal fun distanceGrid(
    path: GenPath,
    box: Rect,
    band: Float,
    nonZero: Boolean = false,
): DistanceGrid {
    val s = flatten(path)
    val n = s.size / 4
    val ox = box.left
    val oy = box.top
    val nx = max(2, ceil(box.width / GRID_DP).toInt() + 1)
    val ny = max(2, ceil(box.height / GRID_DP).toInt() + 1)
    val dist = FloatArray(nx * ny) { band }
    // The distance: each segment to the nodes within band of it.
    for (k in 0 until n) {
        val ax = s[4 * k]
        val ay = s[4 * k + 1]
        val bx = s[4 * k + 2]
        val by = s[4 * k + 3]
        val i0 = max(0, floor((min(ax, bx) - band - ox) / GRID_DP).toInt())
        val i1 = min(nx - 1, ceil((max(ax, bx) + band - ox) / GRID_DP).toInt())
        val j0 = max(0, floor((min(ay, by) - band - oy) / GRID_DP).toInt())
        val j1 = min(ny - 1, ceil((max(ay, by) + band - oy) / GRID_DP).toInt())
        val dx = bx - ax
        val dy = by - ay
        val ll = max(dx * dx + dy * dy, 1e-12f)
        for (j in j0..j1) {
            val py = oy + j * GRID_DP - ay
            var o = j * nx + i0
            for (i in i0..i1) {
                val px = ox + i * GRID_DP - ax
                val t = ((px * dx + py * dy) / ll).coerceIn(0f, 1f)
                val ex = px - dx * t
                val ey = py - dy * t
                val d2 = ex * ex + ey * ey
                if (d2 < dist[o] * dist[o]) dist[o] = sqrt(d2)
                o++
            }
        }
    }
    // Inside: each row's crossings, left to right, by the fill rule.
    val rows = Array(ny) { ArrayList<Float>() }
    val dirs = Array(ny) { ArrayList<Int>() }
    for (k in 0 until n) {
        val ay = s[4 * k + 1]
        val by = s[4 * k + 3]
        if (ay == by) continue
        val lo = min(ay, by)
        val hi = max(ay, by)
        // Rows whose line y crosses [lo, hi): (ay > y) != (by > y).
        val j0 = max(0, ceil((lo - oy) / GRID_DP).toInt())
        val j1 = min(ny - 1, floor((hi - oy) / GRID_DP).toInt())
        for (j in j0..j1) {
            val y = oy + j * GRID_DP
            if ((ay > y) == (by > y)) continue
            val ax = s[4 * k]
            val bx = s[4 * k + 2]
            rows[j] += ax + (y - ay) / (by - ay) * (bx - ax)
            dirs[j] += if (by > ay) 1 else -1
        }
    }
    val texels = ByteArray(nx * ny * 4)
    for (j in 0 until ny) {
        val xs = rows[j]
        val order = xs.indices.sortedBy { xs[it] }
        var w = 0
        var q = 0
        for (i in 0 until nx) {
            val x = ox + i * GRID_DP
            while (q < order.size && xs[order[q]] < x) {
                w += if (nonZero) dirs[j][order[q]] else 1
                q++
            }
            val inside = if (nonZero) w != 0 else w % 2 != 0
            val o = j * nx + i
            val v = if (inside) -dist[o] else dist[o]
            val u = ((v / band * .5f + .5f) * 65535f).roundToInt().coerceIn(0, 65535)
            texels[4 * o] = (u shr 8).toByte()
            texels[4 * o + 1] = (u and 255).toByte()
            texels[4 * o + 3] = 255.toByte()
        }
    }
    return DistanceGrid(texels, nx, ny, ox, oy, band)
}

/** [path]'s loops as segments x0, y0, x1, y1, in the order the path runs, each quadratic cut [SEGMENTS_A_QUADRATIC] times. */
private fun flatten(path: GenPath): FloatArray {
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
            var lx = px
            var ly = py
            for (k in 1..SEGMENTS_A_QUADRATIC) {
                val t = k.toFloat() / SEGMENTS_A_QUADRATIC
                val u = 1 - t
                val qx = u * u * px + 2 * u * t * cx + t * t * ex
                val qy = u * u * py + 2 * u * t * cy + t * t * ey
                s += lx
                s += ly
                s += qx
                s += qy
                lx = qx
                ly = qy
            }
            px = ex
            py = ey
            i += 4
        }
        // `close()`: back to the loop's start, where the last quadratic did not end there.
        if (px != sx || py != sy) {
            s += px
            s += py
            s += sx
            s += sy
        }
    }
    return s.toFloatArray()
}
