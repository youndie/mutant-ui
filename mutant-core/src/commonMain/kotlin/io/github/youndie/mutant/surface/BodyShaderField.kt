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
 * B-131: a body's flow lines for its shader, loop by loop: [segs], an opaque RGBA texture of three texels a segment (its
 * two ends, each coordinate in two bytes, [FLOW_SCALE] to the dp from [originX], [originY]); [boxes], each loop's box
 * grown by a dp; [ranges], each loop's first segment and count.
 */
internal class FlowSegments(
    val segs: ByteArray,
    val rows: Int,
    val originX: Float,
    val originY: Float,
    val boxes: FloatArray,
    val ranges: FloatArray,
    val loops: Int,
)

/** Texels a row of the flow texture, a multiple of three. */
internal const val FLOW_W: Int = 1023

/** Units of a flow coordinate a dp. */
internal const val FLOW_SCALE: Float = 64f

/** The loops and the segments a loop the shader reads at most. */
internal const val MAX_FLOW_LOOPS: Int = 8
internal const val MAX_FLOW_SEGS: Int = 192

/** Segments a quadratic of a flow line is cut into: its points lie close, so two hold the curve within a few hundredths of a dp. */
private const val SEGMENTS_A_QUADRATIC: Int = 2

/** [path]'s loops as [FlowSegments], or null where there are more than the shader reads. */
internal fun flowSegments(path: GenPath): FlowSegments? {
    if (path.loops.size > MAX_FLOW_LOOPS) return null
    val perLoop = path.loops.map { flattenLoop(it) }
    if (perLoop.any { it.size / 4 > MAX_FLOW_SEGS }) return null
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    for (s in perLoop) {
        for (k in s.indices step 2) {
            minX = min(minX, s[k])
            minY = min(minY, s[k + 1])
        }
    }
    val ox = if (minX == Float.MAX_VALUE) 0f else minX - 1
    val oy = if (minY == Float.MAX_VALUE) 0f else minY - 1
    val total = perLoop.sumOf { it.size / 4 }
    val rows = max(1, (total * 3 + FLOW_W - 1) / FLOW_W)
    val segs = ByteArray(FLOW_W * rows * 4) { if (it % 4 == 3) 255.toByte() else 0 }
    val boxes = FloatArray(4 * MAX_FLOW_LOOPS)
    val ranges = FloatArray(2 * MAX_FLOW_LOOPS)
    val slots = intArrayOf(0, 1, 2, 4, 5, 6, 8, 9)

    fun put(
        hi: Int,
        lo: Int,
        v: Float,
        o: Float,
    ) {
        val u = ((v - o) * FLOW_SCALE).roundToInt().coerceIn(0, 65535)
        segs[hi] = (u shr 8).toByte()
        segs[lo] = (u and 255).toByte()
    }
    var e = 0
    for ((l, s) in perLoop.withIndex()) {
        var x0 = Float.MAX_VALUE
        var y0 = Float.MAX_VALUE
        var x1 = -Float.MAX_VALUE
        var y1 = -Float.MAX_VALUE
        ranges[2 * l] = e.toFloat()
        ranges[2 * l + 1] = (s.size / 4).toFloat()
        for (k in 0 until s.size / 4) {
            val t = 12 * e
            for (c in 0 until 4) put(t + slots[2 * c], t + slots[2 * c + 1], s[4 * k + c], if (c % 2 == 0) ox else oy)
            x0 = min(x0, min(s[4 * k], s[4 * k + 2]))
            y0 = min(y0, min(s[4 * k + 1], s[4 * k + 3]))
            x1 = max(x1, max(s[4 * k], s[4 * k + 2]))
            y1 = max(y1, max(s[4 * k + 1], s[4 * k + 3]))
            e++
        }
        boxes[4 * l] = x0 - 1
        boxes[4 * l + 1] = y0 - 1
        boxes[4 * l + 2] = x1 + 1
        boxes[4 * l + 3] = y1 + 1
    }
    return FlowSegments(segs, rows, ox, oy, boxes, ranges, perLoop.size)
}

/** One loop of a [GenPath] as segments x0, y0, x1, y1 in its order, closed back to its start. */
private fun flattenLoop(loop: IntArray): FloatArray {
    val s = ArrayList<Float>()
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
    if (px != sx || py != sy) {
        s += px
        s += py
        s += sx
        s += sy
    }
    return s.toFloatArray()
}
