package io.github.youndie.mutant.gen

/**
 * A closed, smoothed outline as `pathBuf` and `pathOf` emit it: per loop a move to the midpoint of its
 * last and first points, then one quadratic per point through the midpoints, then close. Coordinates are
 * kept in whole tenths of a dp, exactly as the JS rounds them into its SVG string, so the port draws the
 * same numbers and [toSvg] reproduces the string.
 *
 * Each loop is `[mx, my, c1x, c1y, e1x, e1y, c2x, …]` in tenths.
 */
internal class GenPath(
    val loops: List<IntArray>,
) {
    val isEmpty: Boolean get() = loops.isEmpty()

    /** The SVG path data `mutant-generator.js` would return for the same input. */
    fun toSvg(): String =
        buildString {
            for (loop in loops) {
                append('M').append(tenths(loop[0])).append(',').append(tenths(loop[1]))
                var i = 2
                while (i < loop.size) {
                    append('Q').append(tenths(loop[i])).append(',').append(tenths(loop[i + 1]))
                    append(' ').append(tenths(loop[i + 2])).append(',').append(tenths(loop[i + 3]))
                    i += 4
                }
                append('Z')
            }
        }

    operator fun plus(other: GenPath): GenPath = GenPath(loops + other.loops)

    companion object {
        val Empty: GenPath = GenPath(emptyList())

        /** `Math.round(v * 10)` — the tenths the JS writes. */
        fun toTenths(v: Double): Int = jsRound(v * 10).toInt()

        /** A number of tenths as JS prints `Math.round(v * 10) / 10`: `14`, `14.3`, `-0.5`, never `-0`. */
        fun tenths(t: Int): String {
            if (t == 0) return "0"
            val neg = t < 0
            val a = if (neg) -t else t
            val whole = a / 10
            val frac = a % 10
            return (if (neg) "-" else "") + whole + (if (frac != 0) ".$frac" else "")
        }
    }
}

/** Accumulates loops of points into a [GenPath], the way `pathOf` smooths them. */
internal class GenPathBuilder {
    private val loops = ArrayList<IntArray>()

    /**
     * Adds one smoothed loop through [xs], [ys] (`n` points): points closer than [minStep] in
     * Manhattan distance to the last kept one are dropped; fewer than three kept points add nothing.
     */
    fun addLoop(
        xs: DoubleArray,
        ys: DoubleArray,
        count: Int,
        minStep: Double,
    ) {
        val kx = DoubleArray(count)
        val ky = DoubleArray(count)
        var n = 0
        for (i in 0 until count) {
            val x = xs[i]
            val y = ys[i]
            if (n == 0 || kotlin.math.abs(x - kx[n - 1]) + kotlin.math.abs(y - ky[n - 1]) > minStep) {
                kx[n] = x
                ky[n] = y
                n++
            }
        }
        if (n < 3) return
        val out = IntArray(2 + 4 * n)
        out[0] = GenPath.toTenths((kx[n - 1] + kx[0]) / 2)
        out[1] = GenPath.toTenths((ky[n - 1] + ky[0]) / 2)
        for (i in 0 until n) {
            val k = if (i + 1 == n) 0 else i + 1
            out[2 + 4 * i] = GenPath.toTenths(kx[i])
            out[3 + 4 * i] = GenPath.toTenths(ky[i])
            out[4 + 4 * i] = GenPath.toTenths((kx[i] + kx[k]) / 2)
            out[5 + 4 * i] = GenPath.toTenths((ky[i] + ky[k]) / 2)
        }
        loops += out
    }

    fun build(): GenPath = GenPath(loops.toList())
}
