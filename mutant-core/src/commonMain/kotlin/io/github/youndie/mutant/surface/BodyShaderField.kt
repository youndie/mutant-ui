package io.github.youndie.mutant.surface

import io.github.youndie.mutant.gen.DistanceField
import io.github.youndie.mutant.gen.GenPath
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The highlight's iso-line, as `frame` marches it. */
internal const val HIGHLIGHT_ISO: Float = -3.4f

/**
 * B-130: [field] as the body shader reads it, an opaque RGBA texture: its value in red and green (16 bits over ±32), and
 * in blue whether the node is within reach of a crossing of the contour (1: two cells, for the outline) and of the
 * highlight (2: one cell), so a pixel far from both reads one texel. A shader reads a texel premultiplied by its alpha,
 * so the alpha carries nothing.
 */
internal fun fieldTexels(field: DistanceField): ByteArray {
    val nx = field.nx
    val ny = field.ny
    val v = field.values
    val near = IntArray(nx * ny)

    fun mark(
        iso: Float,
        reach: Int,
        bit: Int,
    ) {
        for (j in 0 until ny - 1) {
            for (i in 0 until nx - 1) {
                val o = j * nx + i
                val sa = v[o] < iso
                if (sa == (v[o + 1] < iso) && sa == (v[o + nx] < iso) && sa == (v[o + nx + 1] < iso)) continue
                for (jj in max(0, j - reach)..min(ny - 1, j + 1 + reach)) {
                    for (ii in max(0, i - reach)..min(nx - 1, i + 1 + reach)) {
                        near[jj * nx + ii] =
                            near[jj * nx + ii] or bit
                    }
                }
            }
        }
    }
    mark(0f, 2, 1)
    mark(HIGHLIGHT_ISO, 1, 2)
    val bytes = ByteArray(nx * ny * 4)
    for (k in 0 until nx * ny) {
        val u = ((v[k].coerceIn(-32f, 32f) + 32f) / 64f * 65535f).roundToInt()
        bytes[4 * k] = (u shr 8).toByte()
        bytes[4 * k + 1] = (u and 255).toByte()
        bytes[4 * k + 2] = near[k].toByte()
        bytes[4 * k + 3] = 255.toByte()
    }
    return bytes
}

/**
 * B-131: a body's flow lines for its shader, their segments sorted into bins of [FLOW_BIN_DP] over the lines' box, so a
 * pixel reads its own bin's few. Two opaque RGBA textures, the data in red, green and blue: [bins], a texel a bin (the
 * first entry in two bytes; the entry count plus 32 · (winding + 4) at the bin's centre, by the non-zero rule as the paths
 * fill the lines); [segs], three texels an entry (its two ends in the order the path runs, each coordinate in two bytes,
 * [FLOW_SCALE] to the dp from the box's origin).
 */
internal class FlowBins(
    val bins: ByteArray,
    val binsW: Int,
    val binsH: Int,
    val segs: ByteArray,
    val rows: Int,
    val originX: Float,
    val originY: Float,
)

/** Texels a row of the flow segments' texture, a multiple of three. */
internal const val FLOW_W: Int = 1023

/** Units of a flow coordinate a dp. */
internal const val FLOW_SCALE: Float = 64f

/** The side of a flow bin, dp. */
internal const val FLOW_BIN_DP: Float = 2f

/** The entries a flow bin holds at most: its count shares a byte with the winding. */
internal const val MAX_FLOW_ENTRIES: Int = 31

/** Segments a quadratic of a flow line is cut into: its points lie close, so two hold the curve within a few hundredths of a dp. */
private const val SEGMENTS_A_QUADRATIC: Int = 2

/**
 * [path]'s segments binned, each listed in every bin within [reach] of it; null where the path is empty, or a bin would
 * hold more than [MAX_FLOW_ENTRIES], or a centre's winding is out of −4…3.
 */
internal fun flowBins(
    path: GenPath,
    reach: Float,
): FlowBins? {
    if (path.isEmpty) return null
    var n = 0
    val parts = path.loops.map { flattenLoop(it) }
    for (p in parts) n += p.size / 4
    val s = FloatArray(4 * n)
    var at = 0
    for (p in parts) {
        p.copyInto(s, at)
        at += p.size
    }
    var x0 = Float.MAX_VALUE
    var y0 = Float.MAX_VALUE
    var x1 = -Float.MAX_VALUE
    var y1 = -Float.MAX_VALUE
    for (k in 0 until n) {
        x0 = min(x0, min(s[4 * k], s[4 * k + 2]))
        y0 = min(y0, min(s[4 * k + 1], s[4 * k + 3]))
        x1 = max(x1, max(s[4 * k], s[4 * k + 2]))
        y1 = max(y1, max(s[4 * k + 1], s[4 * k + 3]))
    }
    val ox = x0 - reach - 1
    val oy = y0 - reach - 1
    val bw = max(1, ((x1 + reach + 1 - ox) / FLOW_BIN_DP).toInt() + 1)
    val bh = max(1, ((y1 + reach + 1 - oy) / FLOW_BIN_DP).toInt() + 1)

    fun range(
        lo: Float,
        hi: Float,
        o: Float,
        count: Int,
    ): IntRange =
        max(0, kotlin.math.floor((lo - reach - o) / FLOW_BIN_DP).toInt())..min(
            count - 1,
            kotlin.math.floor((hi + reach - o) / FLOW_BIN_DP).toInt(),
        )

    val counts = IntArray(bw * bh)
    for (k in 0 until n) {
        for (j in range(min(s[4 * k + 1], s[4 * k + 3]), max(s[4 * k + 1], s[4 * k + 3]), oy, bh)) {
            for (i in range(min(s[4 * k], s[4 * k + 2]), max(s[4 * k], s[4 * k + 2]), ox, bw)) counts[j * bw + i]++
        }
    }
    if ((counts.maxOrNull() ?: 0) > MAX_FLOW_ENTRIES) return null
    val start = IntArray(bw * bh + 1)
    for (b in 0 until bw * bh) start[b + 1] = start[b] + counts[b]
    val total = start[bw * bh]
    if (total > 65535) return null
    val entries = IntArray(total)
    val fill = start.copyOf()
    for (k in 0 until n) {
        for (j in range(min(s[4 * k + 1], s[4 * k + 3]), max(s[4 * k + 1], s[4 * k + 3]), oy, bh)) {
            for (i in range(min(s[4 * k], s[4 * k + 2]), max(s[4 * k], s[4 * k + 2]), ox, bw)) {
                entries[fill[j * bw + i]++] =
                    k
            }
        }
    }
    // The winding at each bin's centre, by a ray to the right (up through it +1, down −1): each row's crossings found
    // once, sorted, and summed from the right.
    val bins = ByteArray(bw * bh * 4)
    val xs = FloatArray(n)
    val ds = IntArray(n)
    val order = IntArray(n)
    for (j in 0 until bh) {
        val y = oy + (j + .5f) * FLOW_BIN_DP
        var m = 0
        for (k in 0 until n) {
            val ay = s[4 * k + 1]
            val by = s[4 * k + 3]
            if ((ay > y) == (by > y)) continue
            val ax = s[4 * k]
            val bx = s[4 * k + 2]
            xs[m] = ax + (y - ay) / (by - ay) * (bx - ax)
            ds[m] = if (by > ay) 1 else -1
            order[m] = m
            m++
        }
        // Insertion sort: a row crosses a few lines.
        for (a in 1 until m) {
            val v = order[a]
            var b = a - 1
            while (b >= 0 && xs[order[b]] > xs[v]) {
                order[b + 1] = order[b]
                b--
            }
            order[b + 1] = v
        }
        var w = 0
        for (q in 0 until m) w += ds[q]
        var q = 0
        for (i in 0 until bw) {
            val x = ox + (i + .5f) * FLOW_BIN_DP
            while (q < m && xs[order[q]] <= x) {
                w -= ds[order[q]]
                q++
            }
            if (w < -4 || w > 3) return null
            val b = j * bw + i
            bins[4 * b] = (start[b] shr 8).toByte()
            bins[4 * b + 1] = (start[b] and 255).toByte()
            bins[4 * b + 2] = (counts[b] + 32 * (w + 4)).toByte()
            bins[4 * b + 3] = 255.toByte()
        }
    }
    val rows = max(1, (total * 3 + FLOW_W - 1) / FLOW_W)
    val segs = ByteArray(FLOW_W * rows * 4) { if (it % 4 == 3) 255.toByte() else 0 }
    val slots = intArrayOf(0, 1, 2, 4, 5, 6, 8, 9)
    for (e in 0 until total) {
        val k = entries[e]
        val t = 12 * e
        for (c in 0 until 4) {
            val u = ((s[4 * k + c] - (if (c % 2 == 0) ox else oy)) * FLOW_SCALE).roundToInt().coerceIn(0, 65535)
            segs[t + slots[2 * c]] = (u shr 8).toByte()
            segs[t + slots[2 * c + 1]] = (u and 255).toByte()
        }
    }
    return FlowBins(bins, bw, bh, segs, rows, ox, oy)
}

/** One loop of a [GenPath] as segments x0, y0, x1, y1 in its order, closed back to its start. */
private fun flattenLoop(loop: IntArray): FloatArray {
    val quads = (loop.size - 2) / 4
    val out = FloatArray(4 * (quads * SEGMENTS_A_QUADRATIC + 1))
    var o = 0
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
            out[o++] = lx
            out[o++] = ly
            out[o++] = qx
            out[o++] = qy
            lx = qx
            ly = qy
        }
        px = ex
        py = ey
        i += 4
    }
    if (px != sx || py != sy) {
        out[o++] = px
        out[o++] = py
        out[o++] = sx
        out[o++] = sy
    }
    return out.copyOf(o)
}
