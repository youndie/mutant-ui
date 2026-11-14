package io.github.youndie.mutant.gen

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The instance half of `MutantGenerator`: everything decided once per slot and key (`genItem` and what
 * it calls), and the per-frame primitives of the drips (`prims`, `dyn`, `bubbles`). The field and the
 * outline are in `Field.kt`. Every function keeps the JS's structure and the order of its random calls.
 */
internal class MutantGenerator(
    /** `props.seed`: the cassette seed every instance key is prefixed with. */
    val cassette: String = "5",
    /** `props.density`: the flow lines' density. */
    val density: Double = 1.0,
) {
    /** `genItem`: the instance for [slot] and [key]. [still] is the engine's `stat` (disabled or clean): no mosaic, no flow lines. */
    fun instance(
        slot: Slot,
        key: String,
        still: Boolean = false,
    ): Instance {
        val sd = "$cassette/$key"
        val rnd = Mulberry32(sd, "grime")
        val tau = 6.283
        val w = slot.w
        val h = slot.h
        val r = slot.r
        val over = slot.over
        val cf = slot.dripShare
        val k = max(.5, min(1.0, min(h / 48, w / 110)))
        val rb = if (over > 0) min(r, max(4.0, r * .45)) else r
        val wq = k * slot.wobble * (.3 + .7 * slot.calm)
        val wave =
            doubleArrayOf(
                min(2.6, .8 + h * .035) * slot.wobble * slot.calm,
                24 + 20 * rnd.next(),
                rnd.next() * tau,
                rnd.next() * tau,
                tau / (6000 + 4000 * rnd.next()),
            )
        val lw = 34 + 40 * rnd.next()
        val lp = rnd.next() * tau
        val cm = min(w, h) / 2
        val rc = { f: Double -> if (slot.round) f else min(cm, f * (.5 + 1.3 * rnd.next())) }
        val sg = { if (rnd.next() < .5) -1.0 else 1.0 }
        val shape = DoubleArray(12)
        shape[0] = (1.5 + 3.5 * rnd.next()) * wq
        shape[1] = (1.5 + 3.5 * rnd.next()) * wq
        shape[2] = sg().let { it * (1.5 + 2.5 * rnd.next()) * wq }
        shape[3] = 2.6 * wq
        shape[4] = sg().let { it * (1.5 + 2.5 * rnd.next()) * wq }
        shape[5] = 1 + floor(rnd.next() * 3)
        shape[6] = rnd.next() * tau
        shape[7] = rnd.next() * 50
        shape[8] = rc(r)
        shape[9] = rc(r)
        shape[10] = rc(rb)
        shape[11] = rc(rb)
        val drips = ArrayList<Drip>()
        if (over > 0) {
            var x = rb + 1.5 + rnd.next() * 3
            for (i in 0 until 80) {
                val q = rnd.next()
                val s = rnd.next()
                val u = rnd.next()
                val gp = rnd.next()
                val p = rnd.next()
                val o = rnd.next()
                val a = rnd.next()
                val ty = rnd.next()
                val thin = ty < .25
                val b = max(1.9, (if (thin) 2 + q else 3.4 + 4.6 * q * q) * k)
                val cx = x + b * 1.45
                if (cx + b * 1.45 > w - rb - 1) break
                val mod = .5 + .5 * sin(tau * cx / lw + lp)
                val drip = a < cf
                var len = if (thin) over * (.55 + .45 * s) else over * (.2 + .8 * mod.pow(.7) * (.3 + .7 * s))
                len = max(b * 2.2 + 1.5, min(if (drip) over * .74 else over, len))
                drips +=
                    Drip(
                        x = cx,
                        halfWidth = b,
                        maxLength = len,
                        restLength = max(b * 1.8 + 1, len * (.5 + .2 * u)),
                        period = 8000 + 6000 * p,
                        offset = o,
                        u = u,
                        drips = drip,
                    )
                x = cx + b * 1.45 + (.8 + 9 * gp * gp * gp) * k
            }
        }
        val g = Instance(slot, k, rb, wave, shape, drips, sd)
        val inside = { px: Double, py: Double, margin: Double ->
            bodyDistance(px, py, g, topWave(g, clamp(px, 0.0, w), 0.0)) < -margin
        }
        if (slot.dead) g.deadZone = doubleArrayOf(w, if (Mulberry32(sd, "mark:dead").next() < .5) 0.0 else h, 62.0)
        if (slot.edge && w >= 40 && !still) edgeMosaic(g)
        if (slot.marks && !still) placeFlowLines(g, rnd, inside, density * slot.calm)
        return g
    }

    /** `edgeMos`: up to three rows of 4 and 8 dp blocks along one side of the body. A still body (disabled) draws it too. */
    internal fun edgeMosaic(g: Instance) {
        val slot = g.slot
        val w = slot.w
        val h = slot.h
        val cz = slot.cleanZone
        val r = min(slot.r, h / 2)
        val rnd = Mulberry32(g.seed, "edge")
        val blocks = ArrayList<Block>()
        val tiles = ArrayList<MosaicTile>()
        val hit = { x: Double, y: Double, a: Double ->
            cz[2] > 0 && x < cz[0] + cz[2] && x + a > cz[0] && y < cz[1] + cz[3] && y + a > cz[1]
        }
        val sv = rnd.next()
        val side =
            if (h - 2 * r >= 20 &&
                sv > .6
            ) {
                (if (sv > .8) MosaicSide.Left else MosaicSide.Right)
            } else {
                MosaicSide.Top
            }
        g.mosaicSide = side
        val top = side == MosaicSide.Top
        val len = (if (top) w else h) - 2 * r
        val segL = max(12.0, len * (if (top) .4 + .25 * rnd.next() else .9))
        val s0 = r + rnd.next() * max(0.0, len - segL)
        val chance = doubleArrayOf(.95, .7, .35)
        var p = 0.0
        while (p < segL) {
            val bs = if (abs(p / segL - .5) < .22) 8.0 else 4.0
            val a = jsRound((s0 + p) / 4) * 4
            for (j in 0 until 3) {
                val v = rnd.next()
                val c = rnd.next()
                val x =
                    when (side) {
                        MosaicSide.Top -> a
                        MosaicSide.Left -> j * bs
                        MosaicSide.Right -> w - (j + 1) * bs
                    }
                val y = if (top) j * bs else a
                if (v < chance[j] && !hit(x, y, bs)) {
                    blocks += Block(x, y, bs)
                    if (j == 0) {
                        var q = 0.0
                        while (q < bs) {
                            blocks +=
                                when (side) {
                                    MosaicSide.Top -> Block(a + q, -4.0, 4.0)
                                    MosaicSide.Left -> Block(-4.0, a + q, 4.0)
                                    MosaicSide.Right -> Block(w, a + q, 4.0)
                                }
                            q += 4
                        }
                    }
                    val opacity =
                        if (c < .25) {
                            .5
                        } else if (c < .45) {
                            .28
                        } else if (c < .75) {
                            .38
                        } else {
                            .62
                        }
                    tiles += MosaicTile(slot.ox + x, slot.oy + y, bs, black = c < .45, opacity = opacity)
                }
            }
            p += bs
        }
        g.mosaic = blocks
        g.mosaicTiles = tiles
    }

    /** `deadPx`: the dead pixels of the card's dirty corner, 8, 4 and 2 dp, thinning with distance from the corner. */
    fun deadPixels(g: Instance): DeadPixels {
        val slot = g.slot
        val w = slot.w
        val h = slot.h
        val cz = slot.cleanZone
        val rnd = Mulberry32(g.seed, "mark:dead")
        val x0 = cz[0] + cz[2] + 2
        val out = ArrayList<DeadPixel>()
        val rect = { x: Double, y: Double, q: Double, c: DeadPixelColor ->
            out +=
                DeadPixel(slot.ox + x, slot.oy + y, q, c)
        }
        val bright = arrayOf(DeadPixelColor.Green, DeadPixelColor.Magenta, DeadPixelColor.Cyan, DeadPixelColor.White)
        val top = rnd.next() < .5
        val cy0 = if (top) 0.0 else h
        val palette =
            arrayOf(DeadPixelColor.Cocoon1, DeadPixelColor.Surface2, DeadPixelColor.Cocoon2, DeadPixelColor.Ink)
        val pick = {
            val v = rnd.next()
            val u = rnd.next()
            if (v < .06) {
                DeadPixelColor.Green
            } else if (v < .1) {
                DeadPixelColor.Magenta
            } else {
                palette[floor(u * 4).toInt()]
            }
        }
        val g8 = ceil(x0 / 8) * 8
        val g4 = ceil(x0 / 4) * 4
        var y = 0.0
        while (y < h) {
            var x = g8
            while (x < w) {
                val d = jsHypot(x + 4 - w, y + 4 - cy0)
                val v = rnd.next()
                val c = pick()
                if (d < 34 && v < .85) rect(x, y, 8.0, c)
                x += 8
            }
            y += 8
        }
        y = 0.0
        while (y < h) {
            var x = g4
            while (x < w) {
                val d = jsHypot(x + 2 - w, y + 2 - cy0)
                val v = rnd.next()
                val c = pick()
                if (d >= 34 && d < 58 && v < .42) rect(x, y, 4.0, c)
                x += 4
            }
            y += 4
        }
        for (n in 0 until 18) {
            val a = rnd.next()
            val b = rnd.next()
            val c = rnd.next()
            val x = floor(x0 + a * (w - x0))
            val yy = floor(b * h)
            val d = jsHypot(x - w, yy - cy0)
            if (d >= 52 && d < 92) rect(x, yy, 2.0, bright[floor(c * 4).toInt()])
        }
        val ly = slot.oy + jsRound(if (top) 22 + rnd.next() * 26 else h - 22 - rnd.next() * 26) + .5
        val lx = slot.ox + x0 + rnd.next() * 16
        return DeadPixels(out, lineX1 = lx, lineX2 = lx + 20 + rnd.next() * 30, lineY = ly)
    }

    /** `marks`: places the flow lines inside the body, outside the clean zone, the mosaic and the dead pixels, never overlapping. */
    private fun placeFlowLines(
        g: Instance,
        rnd: Mulberry32,
        inside: (Double, Double, Double) -> Boolean,
        dens: Double,
    ) {
        val slot = g.slot
        val w = slot.w
        val h = slot.h
        val tz = slot.textZone
        val sc = max(.72, g.k)
        val dz = g.deadZone
        val mz = g.mosaic
        val hitT = { x: Double, y: Double, m: Double ->
            (x > tz[0] - m && x < tz[0] + tz[2] + m && y > tz[1] - m && y < tz[1] + tz[3] + m) ||
                (dz != null && jsHypot(x - dz[0], y - dz[1]) < dz[2] + m) ||
                mz.any { b -> x > b.x - m && x < b.x + b.size + m && y > b.y - m && y < b.y + b.size + m }
        }
        val boxes = ArrayList<DoubleArray>()
        val lines = ArrayList<FlowLine>()
        val place = place@{ m: FlowLine ->
            val c = flowLineAt(m, 1.0).points
            if (!inside(m.x, m.y, 2.4) ||
                c.any { p -> hitT(p[0], p[1], 2.6) || !inside(p[0], p[1], 2.4) }
            ) {
                return@place false
            }
            var xa = 1e9
            var xb = -1e9
            var ya = 1e9
            var yb = -1e9
            for (p in c) {
                xa = min(xa, p[0])
                xb = max(xb, p[0])
                ya = min(ya, p[1])
                yb = max(yb, p[1])
            }
            ya = min(ya, m.y - 1)
            if (boxes.any { b ->
                    xa < b[1] + 3.5 && xb > b[0] - 3.5 && ya < b[3] + 3 && yb > b[2] - 3
                }
            ) {
                return@place false
            }
            boxes += doubleArrayOf(xa, xb, ya, yb)
            m.period = 5000 + 4500 * rnd.next()
            m.offset = rnd.next()
            m.noiseSeed = rnd.next() * 90
            lines += m
            true
        }
        val n = max(2.0, jsRound(w / (30 * sc) * dens)).toInt()
        val sw = w / n
        for (i in 0 until n) {
            val t0 = rnd.next()
            for (tr in 0 until 24) {
                val t = t0 + tr * .035
                val x = (i + .15 + .7 * rnd.next()) * sw
                val dir = if (rnd.next() < .5) -1.0 else 1.0
                val m: FlowLine
                if (t < .4) {
                    val a = (2.6 + 1.2 * rnd.next()) * sc
                    val sl = 4 * sc
                    val len = min(h - 14 - sl, (13 + 12 * rnd.next()) * sc)
                    val dx = (rnd.next() - .5) * 14 * sc
                    val y = 3 + rnd.next() * max(1.0, h - len - a - 8 - sl)
                    val hk = (3 + 4 * rnd.next()) * sc
                    val fl = if (rnd.next() < .5) 2 * sc else 0.0
                    m = FlowLine(FlowLineKind.Hook, x - (dx + dir * a * 2) / 2, y, len, dx, a, hk, dir, fl, sl)
                } else if (t < .75) {
                    val a = (2.2 + 1.2 * rnd.next()) * sc
                    val sl = 3 * sc
                    val y = 4 + rnd.next() * max(1.0, h - 14 - sl)
                    val len = (3 + 5 * rnd.next()) * sc
                    val hk = (3 + 4.5 * rnd.next()) * sc
                    m = FlowLine(FlowLineKind.Hook, x - dir * a, y, len, 0.0, a, hk, dir, 1.8 * sc, sl)
                } else {
                    val sl = 2 * sc
                    val y = 4 + rnd.next() * max(1.0, h - 9 - sl)
                    val wd = (7 + 7 * rnd.next()) * sc
                    val dp = (1.6 + 1.4 * rnd.next()) * sc
                    m = FlowLine(FlowLineKind.Arc, x, y, slide = sl, width = wd, depth = dp)
                }
                if (place(m)) break
            }
        }
        g.flowLines = lines
    }

    /** `dyn`: one drip at time t — length, tip radius, neck — and the drop it lets go, if any. */
    internal fun dripState(
        d: Drip,
        t: Double,
        h: Double,
        k: Double,
    ): DripState {
        val b = d.halfWidth
        val s = 2.2 + .9 * b
        val tau = 6.283
        if (!d.drips) {
            val ph = (t / (d.period * 1.7) + d.offset) % 1
            return DripState(d.maxLength * (.88 + .12 * (.5 + .5 * sin(tau * ph))), b * 1.08, .9)
        }
        val period = d.period
        val hang = 2200 + 2200 * d.u
        val pinch = 1000.0
        val recoil = 1000.0
        val grow = period - hang - pinch - recoil
        val ph = (t + d.offset * period) % period
        val r2 = b * 1.45
        val lh = d.maxLength + s * .25
        val yc0 = h + lh - r2
        if (ph < grow) {
            val u = ph / grow
            val e = u * u * (.35 + .65 * u)
            return DripState(
                d.restLength + (d.maxLength - d.restLength) * e,
                b * (1 + .45 * smoothstep(.3, 1.0, u)),
                .9 - .14 * smoothstep(.4, 1.0, u),
            )
        }
        if (ph < grow + hang) {
            val v = (ph - grow) / hang
            val sw = sin(v * tau * 1.5) * .6 * (1 - v)
            return DripState(d.maxLength + s * .25 * v + sw, r2, .76 - .08 * v)
        }
        if (ph < grow + hang + pinch) {
            val v = (ph - grow - hang) / pinch
            val e = v * v
            return DripState(
                lh - r2 * 1.2 * e,
                r2 + (b * .55 - r2) * e,
                .68 - .12 * e,
                yc0 + s * 1.1 * e,
                r2 * (1 - .08 * e),
            )
        }
        val w = ph - grow - hang - pinch
        val dist = .5 * .00055 * w * w
        val sc = max(0.0, 1 - max(0.0, dist - 10 * k) / (8 * k))
        val ls = lh - r2 * 1.2
        val q = 1 - exp(-w / 90)
        val tipLength = d.restLength + (ls - d.restLength) * exp(-w / 160) * cos(w / 90)
        return if (sc > .05) {
            DripState(tipLength, b * (.55 + .45 * q), .56 + .34 * q, yc0 + s * 1.1 + dist, r2 * .92 * sc)
        } else {
            DripState(tipLength, b * (.55 + .45 * q), .56 + .34 * q)
        }
    }

    /** `prims`: the drips' and drops' capsules at time t. [noDrop] is the pressed mode: a drop never lets go, the drip waits on the edge of it. */
    fun primitives(
        g: Instance,
        t: Double,
        noDrop: Boolean,
    ): MutableList<Primitive> {
        val slot = g.slot
        val ox = slot.ox
        val oy = slot.oy
        val h = slot.h
        val k = g.k
        val out = ArrayList<Primitive>()
        for (d in g.drips) {
            var tt = t
            var hold = false
            if (noDrop && d.drips) {
                val lim = d.period - 2000 - 2200 - 2200 * d.u + 2200 + 2200 * d.u - 1
                val ph = (t + d.offset * d.period) % d.period
                if (ph > lim && ph < lim + 1001) tt = t - (ph - lim)
                hold = true
            }
            val s = dripState(d, tt, h, k)
            val x = d.x + ox
            val kd = 4 * k
            val y1 = h - d.halfWidth
            val y2 = h + s.length - s.tipRadius
            val nr = d.halfWidth * s.neck
            if (y2 - y1 <= abs(s.tipRadius - d.halfWidth) + .1) {
                val rr = max(d.halfWidth, s.tipRadius)
                out += doubleArrayOf(x, oy + y1, x, oy + y1, rr, rr, kd)
            } else {
                val yn = min(y2 - s.tipRadius * 1.1, y2 - abs(s.tipRadius - nr) - .1)
                if (yn - y1 > abs(d.halfWidth - nr) + .1) {
                    out += doubleArrayOf(x, oy + y1, x, oy + yn, d.halfWidth, nr, kd)
                    out += doubleArrayOf(x, oy + yn, x, oy + y2, nr, s.tipRadius, kd)
                } else {
                    out += doubleArrayOf(x, oy + y1, x, oy + y2, d.halfWidth, s.tipRadius, kd)
                }
            }
            if (s.dropY != null && !hold) {
                val r = s.dropRadius!!
                out += doubleArrayOf(x, oy + s.dropY, x, oy + s.dropY, r, r, 2.4 * k)
            }
        }
        return out
    }

    /** `bubbles`: the boil's bubbles on the top edge at time t and heat [heat], as round primitives. */
    fun bubbles(
        g: Instance,
        t: Double,
        heat: Double,
    ): List<Primitive> {
        val slot = g.slot
        val out = ArrayList<Primitive>()
        val n = max(3.0, jsRound(slot.w / 20)).toInt()
        for (i in 0 until n) {
            val rnd = Mulberry32(g.seed, "bub$i")
            val per = 360 + 440 * rnd.next()
            val off = rnd.next()
            val cycle = floor(t / per + off)
            val ph = (t / per + off) % 1
            val q = Mulberry32(g.seed, "bb$i/${cycle.toLong()}")
            val x = slot.r * .6 + q.next() * (slot.w - slot.r * 1.2)
            val rm = (2.4 + 3.8 * q.next()) * max(.7, g.k) * heat
            val r = if (ph < .8) rm * (.35 + .65 * ph / .8) else 0.0
            if (r > .9) {
                val y = slot.oy + 1.5 - r * .55
                out += doubleArrayOf(slot.ox + x, y, slot.ox + x, y, r, r, 2.6)
            }
        }
        return out
    }

    /** `markPath`: every flow line at time t, each growing, sliding down and fading on its own cycle; [boilPhase] 0…3 and [boil] tremble them. */
    fun flowLinePath(
        g: Instance,
        t: Double,
        boilPhase: Int,
        boil: Double,
    ): GenPath {
        val ox = g.slot.ox
        val oy = g.slot.oy
        val wm = 2.1 * max(.72, g.k)
        val bk = boilPhase.toDouble()
        val builder = GenPathBuilder()
        for (m in g.flowLines) {
            val u = (t / m.period + m.offset) % 1
            val e = smoothstep(0.0, .08, u) * (1 - smoothstep(.86, 1.0, u))
            if (e < .08) continue
            val shape = flowLineAt(m, u)
            val c = shape.points
            val cum = DoubleArray(c.size)
            for (i in 1 until c.size) cum[i] = cum[i - 1] + jsHypot(c[i][0] - c[i - 1][0], c[i][1] - c[i - 1][1])
            val tot = cum[c.size - 1]
            val wk = wm * e * (1 + .14 * boil * valueNoise(m.noiseSeed + bk * 3.7, 5.0))
            val widths =
                DoubleArray(c.size) { i ->
                    wk *
                        (
                            .26 + .74 * smoothstep(0.0, 3.2, min(cum[i], tot - cum[i])) +
                                .22 * exp(-((cum[i] - cum[shape.bend]) / 2.4).pow(2))
                        )
                }
            val xs =
                DoubleArray(c.size) { i ->
                    c[i][0] + ox +
                        boil * .55 * valueNoise(i * .3 + m.noiseSeed + bk * 7.3, bk * 3.1)
                }
            val ys =
                DoubleArray(c.size) { i ->
                    c[i][1] + oy +
                        boil * .4 * valueNoise(i * .3 + m.noiseSeed + 40 + bk * 5.9, 1.7)
                }
            taperInto(builder, xs, ys, widths)
        }
        return builder.build()
    }

    /** `markAt`: a flow line's axis at phase u, with the index of its bend. */
    internal fun flowLineAt(
        m: FlowLine,
        u: Double,
    ): FlowLineShape {
        val q = .35 + .65 * smoothstep(0.0, .75, u)
        val y = m.y + m.slide * u
        return if (m.kind == FlowLineKind.Arc) {
            arc(m.x, y, m.width, m.depth * q)
        } else {
            hook(m.x, y, m.length * q, m.dx * q, m.a, m.hook * (.4 + .6 * (q - .35) / .65), m.dir, m.flick)
        }
    }

    /** `mkHook`. */
    private fun hook(
        x: Double,
        y: Double,
        len: Double,
        dx: Double,
        a: Double,
        hk: Double,
        dir: Double,
        fl: Double,
    ): FlowLineShape {
        val c = ArrayList<DoubleArray>()
        val n = max(3.0, jsRound(len / 1.1)).toInt()
        if (fl != 0.0) {
            c += doubleArrayOf(x - dir * fl, y - fl * .45)
            c += doubleArrayOf(x - dir * fl * .35, y - fl * .08)
        }
        for (i in 0..n) {
            val u = i.toDouble() / n
            c += doubleArrayOf(x + dx * u * u * (3 - 2 * u), y + len * u)
        }
        val bx = x + dx
        val by = y + len
        var ib = 0
        for (i in 1..8) {
            val q = PI * i / 8
            c += doubleArrayOf(bx + dir * a * (1 - cos(q)), by + a * 1.15 * sin(q))
            if (i == 4) ib = c.size - 1
        }
        val m = max(2.0, jsRound(hk / 1.1)).toInt()
        for (i in 1..m) {
            val u = i.toDouble() / m
            c += doubleArrayOf(bx + dir * a * (2 + .45 * u * u), by - hk * u)
        }
        return FlowLineShape(c, ib)
    }

    /** `mkArc`. */
    private fun arc(
        x: Double,
        y: Double,
        wd: Double,
        dp: Double,
    ): FlowLineShape {
        val c = ArrayList<DoubleArray>()
        val n = max(6.0, jsRound(wd / 1.1)).toInt()
        for (i in 0..n) {
            val u = i.toDouble() / n
            c += doubleArrayOf(x - wd / 2 + wd * u, y + dp * sin(PI * u) - dp * .55 * abs(2 * u - 1).pow(4))
        }
        return FlowLineShape(c, n shr 1)
    }
}

/** A drip at one moment (`dyn`'s `tip` and `drop`). */
internal class DripState(
    val length: Double,
    val tipRadius: Double,
    val neck: Double,
    val dropY: Double? = null,
    val dropRadius: Double? = null,
)

/** A flow line's axis and the index of its bend (`{ C, ib }`). */
internal class FlowLineShape(
    val points: List<DoubleArray>,
    val bend: Int,
)

/** `taper`: an ink stroke of varying width along the axis, as one closed smoothed loop. */
internal fun taperInto(
    builder: GenPathBuilder,
    xs: DoubleArray,
    ys: DoubleArray,
    widths: DoubleArray,
) {
    val n = xs.size
    val lx = DoubleArray(2 * n)
    val ly = DoubleArray(2 * n)
    for (i in 0 until n) {
        val a = max(0, i - 1)
        val b = min(n - 1, i + 1)
        val dx = xs[b] - xs[a]
        val dy = ys[b] - ys[a]
        val l = jsHypot(dx, dy).let { if (it == 0.0 || it.isNaN()) 1.0 else it }
        val nx = -dy / l
        val ny = dx / l
        val hw = widths[i] / 2
        lx[i] = xs[i] + nx * hw
        ly[i] = ys[i] + ny * hw
        lx[2 * n - 1 - i] = xs[i] - nx * hw
        ly[2 * n - 1 - i] = ys[i] - ny * hw
    }
    builder.addLoop(lx, ly, 2 * n, 0.0)
}
