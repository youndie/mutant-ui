package io.github.youndie.mutant.surface

import io.github.youndie.mutant.gen.DistanceField
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
