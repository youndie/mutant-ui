package io.github.youndie.mutant.gen

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * `{F, nx, ny, x0, y0, s}`: the signed-distance field of one instance on a grid of step [step] dp,
 * negative inside. Values are `Float`, as the JS keeps them in a `Float32Array` (research §1.4).
 */
internal class DistanceField(
    val values: FloatArray,
    val nx: Int,
    val ny: Int,
    val x0: Double,
    val y0: Double,
    val step: Double,
)

/**
 * The static layers of one instance's field on one grid — edge noise, the lower body, the warped
 * coordinates, the body without the top wave, the mosaic's quantisation pairs — kept per instance as the
 * JS keeps them in `this._N` (the cache key there is the host's instance id and the grid size).
 */
internal class FieldStatic(
    val noise: FloatArray,
    val lowerBody: FloatArray,
    val warpX: FloatArray,
    val warpY: FloatArray,
    val body0: FloatArray,
    val quantPairs: IntArray?,
) {
    val boil = HashMap<Int, FloatArray>()
}

/** Per-instance field caches, keyed by grid size. */
internal class FieldCache {
    val static = HashMap<Long, FieldStatic>()

    /**
     * Whether a resting frame of [g] would only read this cache: its static layers built and, where the body boils
     * at rest, all four boil phases. A frame may then be built on another thread while the frame's own thread reads
     * the cache too (B-75); before that, the first frames fill it, on the frame's thread alone.
     */
    fun settledFor(g: Instance): Boolean {
        val st = static[gridKey(g)] ?: return false
        return REST_BOIL * g.slot.calm == 0.0 || st.boil.size == BOIL_PHASES
    }
}

/** The boil phases a body trembles through, 0…3 ([boilPhase]). */
private const val BOIL_PHASES = 4

private fun gridStep(g: Instance) = if (g.slot.w > 200) 1.5 else .9

private fun gridKey(g: Instance): Long {
    val el = g.slot
    val s = gridStep(g)
    val nx = ceil((el.w + 14) / s).toInt() + 1
    val ny = ceil((el.h + el.over + 31) / s).toInt() + 1
    return nx.toLong() shl 32 or ny.toLong()
}

/**
 * `field`: the field at time [t]: the body and the primitives [prims] joined by smooth minimum, the edge
 * noise ([rough]) and the per-tick tremble ([boilPhase], [boil]; [heat] is the press's heat and [boil0]
 * the resting boil). Under a mosaic block the value is taken from the block's centre.
 */
internal fun distanceField(
    g: Instance,
    cache: FieldCache,
    prims: List<Primitive>,
    t: Double,
    rough: Double,
    boilPhase: Int,
    boil: Double,
    heat: Double,
    boil0: Double,
): DistanceField {
    val el = g.slot
    val s = gridStep(g)
    val x0 = el.ox - 7
    val y0 = el.oy - 13
    val nx = ceil((el.w + 14) / s).toInt() + 1
    val ny = ceil((el.h + el.over + 31) / s).toInt() + 1
    val n = nx * ny
    val st =
        cache.static.getOrPut(gridKey(g)) {
            fieldStatic(g, nx, ny, x0, y0, s)
        }
    val bk =
        if (boil != 0.0) {
            st.boil.getOrPut(boilPhase) {
                val k = FloatArray(n)
                for (j in 0 until ny) {
                    for (i in 0 until nx) {
                        k[j * nx + i] =
                            valueNoise(
                                (x0 + i * s) * .16 + boilPhase * 41.3,
                                (y0 + j * s) * .16 + boilPhase * 17.9,
                            ).toFloat()
                    }
                }
                k
            }
        } else {
            null
        }
    val f = FloatArray(n)
    val hh2 = el.h * .5
    val cols = Array(nx) { ArrayList<Primitive>() }
    for (p in prims) {
        val rr = max(p[4], p[5]) + p[6] + 1
        val a = max(0.0, floor((p[0] - rr - x0) / s)).toInt()
        val b = min((nx - 1).toDouble(), ceil((p[0] + rr - x0) / s)).toInt()
        for (i in a..b) cols[i] += p
    }
    for (i in 0 until nx) {
        val x = x0 + i * s
        val lx = x - el.ox
        val tyx =
            if (heat != 0.0) {
                topWave(g, clamp(lx, 0.0, el.w), t * (1 + 2 * heat)) * (1 + 1.5 * heat)
            } else {
                topWave(g, clamp(lx, 0.0, el.w), t)
            }
        val c = cols[i]
        var cm = -1e9
        for (p in c) cm = max(cm, max(p[1], p[3]) + max(p[4], p[5]) + p[6] + 14)
        for (j in 0 until ny) {
            val y = y0 + j * s
            val idx = j * nx + i
            val bc = st.lowerBody[idx].toDouble()
            val b0 = st.body0[idx].toDouble()
            if (b0 < -12.5 || (b0 > 14 && y > cm)) {
                f[idx] = b0.toFloat()
                continue
            }
            var d =
                if (!bc.isNaN()) {
                    bc
                } else {
                    roundedRectDistance(
                        g,
                        st.warpX[idx].toDouble(),
                        st.warpY[idx].toDouble() - tyx * clamp01(1 - (y - el.oy) / hh2),
                    )
                }
            for (p in c) {
                val rr = max(p[4], p[5]) + p[6] + 1
                if (y < min(p[1], p[3]) - rr || y > max(p[1], p[3]) + rr) continue
                d = smoothMin(d, unequalCapsule(x, y, p[0], p[1], p[2], p[3], p[4], p[5]), p[6])
            }
            val tremble =
                if (bk != null) {
                    (if (heat != 0.0) boil0 + (boil - boil0) * clamp01(1.7 - 2 * (y - el.oy) / el.h) else boil) *
                        bk[idx].toDouble()
                } else {
                    0.0
                }
            f[idx] = (d + rough * st.noise[idx].toDouble() + tremble).toFloat()
        }
    }
    val q = st.quantPairs
    if (q != null) {
        val tmp = FloatArray(q.size / 2) { f[q[2 * it + 1]] }
        for (k in tmp.indices) f[q[2 * k]] = tmp[k]
    }
    return DistanceField(f, nx, ny, x0, y0, s)
}

private fun fieldStatic(
    g: Instance,
    nx: Int,
    ny: Int,
    x0: Double,
    y0: Double,
    s: Double,
): FieldStatic {
    val el = g.slot
    val n = nx * ny
    val a = FloatArray(n)
    val b = FloatArray(n) { Float.NaN }
    for (j in 0 until ny) {
        for (i in 0 until nx) {
            val x = x0 + i * s
            val y = y0 + j * s
            val ly = y - el.oy
            a[j * nx + i] = (.55 * valueNoise(x * .8, y * .8) + .3 * valueNoise(x * 2.1 + 17, y * 2.1 + 17)).toFloat()
            if (ly >= el.h * .5) b[j * nx + i] = bodyDistance(x - el.ox, ly, g, 0.0).toFloat()
        }
    }
    val v1 = FloatArray(n)
    val v2 = FloatArray(n)
    for (j in 0 until ny) {
        for (i in 0 until nx) {
            val lx = x0 + i * s - el.ox
            val ly = y0 + j * s - el.oy
            v1[j * nx + i] = valueNoise(lx * .03 + g.shape[7], ly * .05).toFloat()
            v2[j * nx + i] = valueNoise(lx * .03 + g.shape[7] + 31, ly * .05 + 9).toFloat()
        }
    }
    val xw = FloatArray(n)
    val yw = FloatArray(n)
    val b0 = FloatArray(n)
    val sh = g.shape
    val w = el.w
    val h = el.h
    for (j in 0 until ny) {
        for (i in 0 until nx) {
            val idx = j * nx + i
            val px = x0 + i * s - el.ox
            val py = y0 + j * s - el.oy
            val u = clamp01(px / w)
            val v = clamp01(py / h)
            val m = clamp01(1 - py / (h * .5))
            xw[idx] = (px + (if (px < w / 2) sh[0] else -sh[1]) * sin(PI * v) + sh[3] * v1[idx].toDouble()).toFloat()
            yw[idx] =
                (
                    py - (
                        sh[2] * (u - .5) * 2 + sh[4] *
                            sin(
                                PI * u * sh[5] + sh[6],
                            )
                    ) * m + sh[3] * .7 * v2[idx].toDouble()
                ).toFloat()
            b0[idx] =
                roundedRectDistance(
                    g,
                    xw[idx].toDouble(),
                    yw[idx].toDouble() - topWave(g, clamp(px, 0.0, w), 0.0) * m,
                ).toFloat()
        }
    }
    var pairs: IntArray? = null
    if (g.mosaic.isNotEmpty()) {
        val pr = ArrayList<Int>()
        for (blk in g.mosaic) {
            val bx = blk.x
            val by = blk.y
            val bs = blk.size
            val ci = clamp(jsRound((el.ox + bx + bs / 2 - x0) / s), 0.0, (nx - 1).toDouble()).toInt()
            val cj = clamp(jsRound((el.oy + by + bs / 2 - y0) / s), 0.0, (ny - 1).toDouble()).toInt()
            var j = max(0.0, ceil((el.oy + by - y0) / s)).toInt()
            while (j < ny && y0 + j * s < el.oy + by + bs) {
                var i = max(0.0, ceil((el.ox + bx - x0) / s)).toInt()
                while (i < nx && x0 + i * s < el.ox + bx + bs) {
                    pr += j * nx + i
                    pr += cj * nx + ci
                    i++
                }
                j++
            }
        }
        pairs = pr.toIntArray()
    }
    return FieldStatic(a, b, xw, yw, b0, pairs)
}

/** `march` → the closed polylines of the iso-line, as `Float` points the way the JS buffers keep them. */
internal class IsoLines(
    val xs: FloatArray,
    val ys: FloatArray,
    /** Start of each loop in [xs] / [ys], plus the end. */
    val starts: IntArray,
    val field: DistanceField,
    val iso: Double,
) {
    val count: Int get() = starts.size - 1
}

/**
 * `march`: marching squares over the field at level [iso]. Rows `skipFrom until skipTo` are skipped: the
 * highlight is needed only near the top and below the bottom edge. The order points are created and
 * linked in, and the order loops are traced in, are the JS's, so the path strings agree.
 */
internal fun march(
    fd: DistanceField,
    iso: Double,
    skipFrom: Int = 0,
    skipTo: Int = 0,
): IsoLines {
    val f = fd.values
    val nx = fd.nx
    val ny = fd.ny
    val n = nx * ny
    val edge = IntArray(n * 2)
    val px = FloatArray(n * 2)
    val py = FloatArray(n * 2)
    val nb = IntArray(n * 4)
    var np = 0

    fun ep(
        k: Int,
        a: Double,
        b: Double,
        ax: Int,
        ay: Int,
        bx: Int,
        by: Int,
    ): Int {
        val id0 = edge[k]
        if (id0 != 0) return id0 - 1
        val t = (a - iso) / (a - b)
        val id = np++
        px[id] = (fd.x0 + (ax + (bx - ax) * t) * fd.step).toFloat()
        py[id] = (fd.y0 + (ay + (by - ay) * t) * fd.step).toFloat()
        nb[2 * id] = -1
        nb[2 * id + 1] = -1
        edge[k] = id + 1
        return id
    }

    fun link(
        p: Int,
        q: Int,
    ) {
        if (nb[2 * p] < 0) nb[2 * p] = q else nb[2 * p + 1] = q
        if (nb[2 * q] < 0) nb[2 * q] = p else nb[2 * q + 1] = p
    }
    for (j in 0 until ny - 1) {
        if (j >= skipFrom && j < skipTo) continue
        var o = j * nx
        for (i in 0 until nx - 1) {
            val a = f[o].toDouble()
            val b = f[o + 1].toDouble()
            val c = f[o + nx + 1].toDouble()
            val d = f[o + nx].toDouble()
            val sa = a < iso
            val sb = b < iso
            val sc = c < iso
            val sd = d < iso
            if (!(sa == sb && sb == sc && sc == sd)) {
                val oo = o

                fun top() = ep(oo * 2, a, b, i, j, i + 1, j)

                fun right() = ep((oo + 1) * 2 + 1, b, c, i + 1, j, i + 1, j + 1)

                fun bottom() = ep((oo + nx) * 2, d, c, i, j + 1, i + 1, j + 1)

                fun left() = ep(oo * 2 + 1, a, d, i, j, i, j + 1)
                val cs = (if (sa) 8 else 0) or (if (sb) 4 else 0) or (if (sc) 2 else 0) or (if (sd) 1 else 0)
                when (cs) {
                    1, 14 -> {
                        link(left(), bottom())
                    }

                    2, 13 -> {
                        link(bottom(), right())
                    }

                    3, 12 -> {
                        link(left(), right())
                    }

                    4, 11 -> {
                        link(top(), right())
                    }

                    6, 9 -> {
                        link(top(), bottom())
                    }

                    7, 8 -> {
                        link(left(), top())
                    }

                    5 -> {
                        if ((a + b + c + d) / 4 < iso) {
                            link(left(), top())
                            link(bottom(), right())
                        } else {
                            link(top(), right())
                            link(left(), bottom())
                        }
                    }

                    10 -> {
                        if ((a + b + c + d) / 4 < iso) {
                            link(top(), right())
                            link(left(), bottom())
                        } else {
                            link(left(), top())
                            link(bottom(), right())
                        }
                    }
                }
            }
            o++
        }
    }
    val seen = BooleanArray(np)
    val ox = FloatArray(np)
    val oy = FloatArray(np)
    val starts = ArrayList<Int>()
    var no = 0
    for (pass in 0 until 2) {
        for (k in 0 until np) {
            if (seen[k] || (pass == 0 && nb[2 * k + 1] >= 0)) continue
            val st = no
            var prev = -1
            var cur = k
            while (cur >= 0 && !seen[cur]) {
                seen[cur] = true
                ox[no] = px[cur]
                oy[no] = py[cur]
                no++
                val n0 = nb[2 * cur]
                val n1 = nb[2 * cur + 1]
                val nxt = if (n0 != prev) n0 else n1
                prev = cur
                cur = nxt
            }
            if (no - st > 2 && starts.size < MAX_LOOPS) starts += st else no = st
        }
    }
    starts += no
    return IsoLines(ox, oy, starts.toIntArray(), fd, iso)
}

/** The JS keeps at most `LS.length - 1` = 4095 loops. */
private const val MAX_LOOPS = 4095

/**
 * `pathBuf`: the loops of [lines] smoothed into a [GenPath], points closer than [minStep] dropped. With
 * [minHole], an inner loop (its centre outside the body) narrower than that is left out.
 */
internal fun smoothPath(
    lines: IsoLines,
    minStep: Double,
    minHole: Double = 0.0,
): GenPath {
    val builder = GenPathBuilder()
    val fd = lines.field
    for (l in 0 until lines.count) {
        val a = lines.starts[l]
        val b = lines.starts[l + 1]
        if (minHole != 0.0) {
            var sx = 0.0
            var sy = 0.0
            var xa = 1e9
            var xb = -1e9
            var ya = 1e9
            var yb = -1e9
            for (i in a until b) {
                val x = lines.xs[i].toDouble()
                val y = lines.ys[i].toDouble()
                sx += x
                sy += y
                if (x < xa) xa = x
                if (x > xb) xb = x
                if (y < ya) ya = y
                if (y > yb) yb = y
            }
            val ci = clamp(jsRound((sx / (b - a) - fd.x0) / fd.step), 0.0, (fd.nx - 1).toDouble()).toInt()
            val cj = clamp(jsRound((sy / (b - a) - fd.y0) / fd.step), 0.0, (fd.ny - 1).toDouble()).toInt()
            val width = if (fd.values[cj * fd.nx + ci].toDouble() > lines.iso) f32(min(xb - xa, yb - ya)) else 1e9
            if (width < minHole) continue
        }
        val xs = DoubleArray(b - a) { lines.xs[a + it].toDouble() }
        val ys = DoubleArray(b - a) { lines.ys[a + it].toDouble() }
        builder.addLoop(xs, ys, b - a, minStep)
    }
    return builder.build()
}
