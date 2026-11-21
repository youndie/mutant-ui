package io.github.youndie.mutant.gen

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
    /**
     * Cells added on every side past the engine's grid (B-97): index `i` stands at `x0 + (i - pad) * step`, so the
     * engine's cells keep their positions to the bit.
     */
    val pad: Int = 0,
) {
    /**
     * Whether the body reaches the grid's edge (a value inside it on the border): its contour is then cut there, and a
     * frame built off the frame's thread is not taken (B-97).
     */
    val cut: Boolean by lazy {
        var hit = false
        for (i in 0 until nx) if (values[i] < 0 || values[(ny - 1) * nx + i] < 0) hit = true
        for (j in 0 until ny) if (values[j * nx] < 0 || values[j * nx + nx - 1] < 0) hit = true
        hit
    }
}

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
     * The cells this body's grid is padded by past the engine's (B-97): 0, the engine's grid, until a frame built on the
     * frame's thread found the body at the grid's edge. It only grows, and only there.
     */
    var pad: Int = 0

    /**
     * Whether a resting frame of [g] would only read this cache: its static layers built and, where the body boils
     * at rest, all four boil phases. A frame may then be built on another thread while the frame's own thread reads
     * the cache too (B-75); before that, the first frames fill it, on the frame's thread alone.
     */
    fun settledFor(g: Instance): Boolean {
        val st = static[gridKey(g, pad)] ?: return false
        return REST_BOIL * g.slot.calm == 0.0 || st.boil.size == BOIL_PHASES
    }
}

/** The boil phases a body trembles through, 0…3 ([boilPhase]). */
private const val BOIL_PHASES = 4

private fun gridStep(g: Instance) = if (g.slot.w > 200) 1.5 else .9

/**
 * B-97: how a body's grid grows past the engine's. The engine's grid reaches 7 dp past the slot's sides, 13 dp above
 * and 18 dp below its drips' reach. A tall, wobbly body such as the bottom sheet bulged further: its contour was cut at
 * the grid's edge, the march left open chains, and their joins crossed the body, a wedge-shaped hole under the even-odd
 * fill. A body found at the edge gets [PAD_DP] more on every side, then twice that, up to [MAX_PAD_DP]. The engine's
 * cells keep their positions, so a body that never reaches the edge draws as the engine does, to the bit.
 */
private const val PAD_DP = 8.0
private const val MAX_PAD_DP = 32.0

private fun gridKey(
    g: Instance,
    pad: Int,
): Long {
    val el = g.slot
    val s = gridStep(g)
    val nx = ceil((el.w + 14) / s).toInt() + 1 + 2 * pad
    val ny = ceil((el.h + el.over + 31) / s).toInt() + 1 + 2 * pad
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
    steadyWebs: Boolean = false,
    grow: Boolean = true,
): DistanceField {
    val s = gridStep(g)
    var pad = cache.pad
    while (true) {
        val fd = fieldOn(g, cache, prims, t, rough, boilPhase, boil, heat, boil0, steadyWebs, pad)
        // Off the frame's thread the cache is only read: a cut frame is returned as it is and not taken (B-97).
        if (!fd.cut || !grow || pad * s >= MAX_PAD_DP) return fd
        pad = if (pad == 0) ceil(PAD_DP / s).toInt() else 2 * pad
        cache.pad = pad
    }
}

/** The field on the engine's grid with [pad] cells more on every side (B-97). */
private fun fieldOn(
    g: Instance,
    cache: FieldCache,
    prims: List<Primitive>,
    t: Double,
    rough: Double,
    boilPhase: Int,
    boil: Double,
    heat: Double,
    boil0: Double,
    steadyWebs: Boolean,
    pad: Int,
): DistanceField {
    val el = g.slot
    val s = gridStep(g)
    val x0 = el.ox - 7
    val y0 = el.oy - 13
    val nx = ceil((el.w + 14) / s).toInt() + 1 + 2 * pad
    val ny = ceil((el.h + el.over + 31) / s).toInt() + 1 + 2 * pad
    val n = nx * ny
    val st =
        cache.static.getOrPut(gridKey(g, pad)) {
            fieldStatic(g, nx, ny, x0, y0, s, pad)
        }
    val bk =
        if (boil != 0.0) {
            st.boil.getOrPut(boilPhase) {
                val k = FloatArray(n)
                for (j in 0 until ny) {
                    for (i in 0 until nx) {
                        k[j * nx + i] =
                            valueNoise(
                                (x0 + (i - pad) * s) * .16 + boilPhase * 41.3,
                                (y0 + (j - pad) * s) * .16 + boilPhase * 17.9,
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
        val a = max(0.0, floor((p[0] - rr - x0) / s) + pad).toInt()
        val b = min((nx - 1).toDouble(), ceil((p[0] + rr - x0) / s) + pad).toInt()
        for (i in a..b) cols[i] += p
    }
    for (i in 0 until nx) {
        val x = x0 + (i - pad) * s
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
            val y = y0 + (j - pad) * s
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
            // B-84: where two shapes merge (a drip into the body, a web between two drips), the blend weight of the
            // merge, 0 outside it and 1 where both are equally near.
            var web = 0.0
            for (p in c) {
                val rr = max(p[4], p[5]) + p[6] + 1
                if (y < min(p[1], p[3]) - rr || y > max(p[1], p[3]) + rr) continue
                val cap = unequalCapsule(x, y, p[0], p[1], p[2], p[3], p[4], p[5])
                if (steadyWebs) web = max(web, max(p[6] - abs(d - cap), 0.0) / p[6])
                d = smoothMin(d, cap, p[6])
            }
            val tremble =
                if (bk != null) {
                    (if (heat != 0.0) boil0 + (boil - boil0) * clamp01(1.7 - 2 * (y - el.oy) / el.h) else boil) *
                        bk[idx].toDouble()
                } else {
                    0.0
                }
            // A web's saddle is flat, so the boil's tremble opened and shut it at every phase; damped there, it
            // follows its drips (B-84).
            val kept = if (web > 0) (1 - web).pow(STEADY_POWER) else 1.0
            f[idx] = (d + rough * st.noise[idx].toDouble() + tremble * kept).toFloat()
        }
    }
    val q = st.quantPairs
    if (q != null) {
        val tmp = FloatArray(q.size / 2) { f[q[2 * it + 1]] }
        for (k in tmp.indices) f[q[2 * k]] = tmp[k]
    }
    return DistanceField(f, nx, ny, x0, y0, s, pad)
}

/**
 * How much of the boil's tremble a web keeps (B-84): `(1 − h)²`, h the blend weight of the merge it lies in. Measured
 * over 60 s of every golden body with drips, between drips standing still: the web's depth jumped 3 dp or more at 6 450
 * of 61 963 ticks as the engine draws it, at 246 with no boil at all, and at 444 with this; a power of 1 left 730.
 */
private const val STEADY_POWER: Double = 2.0

private fun fieldStatic(
    g: Instance,
    nx: Int,
    ny: Int,
    x0: Double,
    y0: Double,
    s: Double,
    pad: Int,
): FieldStatic {
    val el = g.slot
    val n = nx * ny
    val a = FloatArray(n)
    val b = FloatArray(n) { Float.NaN }
    // B-149: the same values as the engine's per-cell calls, each computed once. A lattice corner's hash serves every
    // cell around it, and a term of the row alone or of the column alone serves the whole row or column.
    val edge = LatticeRows(nx) { i -> (x0 + (i - pad) * s) * .8 }
    for (j in 0 until ny) {
        for (i in 0 until nx) {
            val x = x0 + (i - pad) * s
            val y = y0 + (j - pad) * s
            val ly = y - el.oy
            a[j * nx + i] = (.55 * edge.noise(x * .8, y * .8) + .3 * valueNoise(x * 2.1 + 17, y * 2.1 + 17)).toFloat()
            if (ly >= el.h * .5) b[j * nx + i] = bodyDistance(x - el.ox, ly, g, 0.0).toFloat()
        }
    }
    val v1 = FloatArray(n)
    val v2 = FloatArray(n)
    val warp1 = LatticeRows(nx) { i -> (x0 + (i - pad) * s - el.ox) * .03 + g.shape[7] }
    val warp2 = LatticeRows(nx) { i -> (x0 + (i - pad) * s - el.ox) * .03 + g.shape[7] + 31 }
    for (j in 0 until ny) {
        for (i in 0 until nx) {
            val lx = x0 + (i - pad) * s - el.ox
            val ly = y0 + (j - pad) * s - el.oy
            v1[j * nx + i] = warp1.noise(lx * .03 + g.shape[7], ly * .05).toFloat()
            v2[j * nx + i] = warp2.noise(lx * .03 + g.shape[7] + 31, ly * .05 + 9).toFloat()
        }
    }
    val xw = FloatArray(n)
    val yw = FloatArray(n)
    val b0 = FloatArray(n)
    val sh = g.shape
    val w = el.w
    val h = el.h
    // The column's terms: the horizontal warp's swing and the top's wave, each a function of the column alone.
    val swing = DoubleArray(nx)
    val wave = DoubleArray(nx)
    for (i in 0 until nx) {
        val px = x0 + (i - pad) * s - el.ox
        val u = clamp01(px / w)
        swing[i] =
            sh[2] * (u - .5) * 2 + sh[4] *
            sin(
                PI * u * sh[5] + sh[6],
            )
        wave[i] = topWave(g, clamp(px, 0.0, w), 0.0)
    }
    for (j in 0 until ny) {
        val py = y0 + (j - pad) * s - el.oy
        val v = clamp01(py / h)
        val m = clamp01(1 - py / (h * .5))
        val bulge = sin(PI * v)
        for (i in 0 until nx) {
            val idx = j * nx + i
            val px = x0 + (i - pad) * s - el.ox
            xw[idx] = (px + (if (px < w / 2) sh[0] else -sh[1]) * bulge + sh[3] * v1[idx].toDouble()).toFloat()
            yw[idx] = (py - swing[i] * m + sh[3] * .7 * v2[idx].toDouble()).toFloat()
            b0[idx] = roundedRectDistance(g, xw[idx].toDouble(), yw[idx].toDouble() - wave[i] * m).toFloat()
        }
    }
    var pairs: IntArray? = null
    if (g.mosaic.isNotEmpty()) {
        val pr = ArrayList<Int>()
        for (blk in g.mosaic) {
            val bx = blk.x
            val by = blk.y
            val bs = blk.size
            val ci = clamp(jsRound((el.ox + bx + bs / 2 - x0) / s) + pad, 0.0, (nx - 1).toDouble()).toInt()
            val cj = clamp(jsRound((el.oy + by + bs / 2 - y0) / s) + pad, 0.0, (ny - 1).toDouble()).toInt()
            var j = max(0.0, ceil((el.oy + by - y0) / s) + pad).toInt()
            while (j < ny && y0 + (j - pad) * s < el.oy + by + bs) {
                var i = max(0.0, ceil((el.ox + bx - x0) / s) + pad).toInt()
                while (i < nx && x0 + (i - pad) * s < el.ox + bx + bs) {
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

/**
 * B-149: [valueNoise] over a grid walked row by row, its lattice corners hashed once. The noise's x for the grid's
 * column `i` is [xOf]`(i)` in every row, so the corners a row needs are known from the columns, and a row whose lattice
 * row is the one before's, or the next, takes the hashes already made. The value is [valueNoise]'s to the bit: the same
 * expression on the same corners' hashes.
 */
internal class LatticeRows(
    nx: Int,
    xOf: (Int) -> Double,
) {
    private val ix0 = floor(xOf(0))
    private val span = (floor(xOf(nx - 1)) - ix0).toInt() + 2
    private var low = DoubleArray(span) { Double.NaN }
    private var high = DoubleArray(span) { Double.NaN }
    private var iyNow = Double.NaN

    private fun row(iy: Double) {
        if (iy == iyNow) return
        if (iy == iyNow + 1) {
            val reuse = low
            low = high
            high = reuse.also { it.fill(Double.NaN) }
        } else {
            low.fill(Double.NaN)
            high.fill(Double.NaN)
        }
        iyNow = iy
    }

    private fun DoubleArray.at(
        ix: Double,
        iy: Double,
    ): Double {
        val k = (ix - ix0).toInt()
        val got = this[k]
        if (!got.isNaN()) return got
        return latticeHash(ix, iy).also { this[k] = it }
    }

    /** [valueNoise]`(x, y)`, [x] being `xOf` of one of the grid's columns. */
    fun noise(
        x: Double,
        y: Double,
    ): Double {
        val ix = floor(x)
        val iy = floor(y)
        row(iy)
        val fx = x - ix
        val fy = y - iy
        val ux = fx * fx * (3 - 2 * fx)
        val uy = fy * fy * (3 - 2 * fy)
        val a = low.at(ix, iy)
        val b = low.at(ix + 1, iy)
        val c = high.at(ix, iy + 1)
        val d = high.at(ix + 1, iy + 1)
        return (a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy) * 2 - 1
    }
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
        px[id] = (fd.x0 + ((ax - fd.pad) + (bx - ax) * t) * fd.step).toFloat()
        py[id] = (fd.y0 + ((ay - fd.pad) + (by - ay) * t) * fd.step).toFloat()
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
            val ci = clamp(jsRound((sx / (b - a) - fd.x0) / fd.step) + fd.pad, 0.0, (fd.nx - 1).toDouble()).toInt()
            val cj = clamp(jsRound((sy / (b - a) - fd.y0) / fd.step) + fd.pad, 0.0, (fd.ny - 1).toDouble()).toInt()
            val width = if (fd.values[cj * fd.nx + ci].toDouble() > lines.iso) f32(min(xb - xa, yb - ya)) else 1e9
            if (width < minHole) continue
        }
        val xs = DoubleArray(b - a) { lines.xs[a + it].toDouble() }
        val ys = DoubleArray(b - a) { lines.ys[a + it].toDouble() }
        builder.addLoop(xs, ys, b - a, minStep)
    }
    return builder.build()
}
