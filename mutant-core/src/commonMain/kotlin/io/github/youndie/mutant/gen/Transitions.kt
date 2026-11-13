package io.github.youndie.mutant.gen

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * The geometry drawn outside the resting frame (B-04): the corruption blocks of an error (`errBlocks`),
 * the front of a new colour pouring down (`pourPath`) and the venom that moves a selection
 * (`venomShapes`). Ported 1:1 like the rest of the generator; the JS returns SVG fragments, the port the
 * shapes in them.
 */

/** The colours of a corruption block, by the theme's role. */
internal enum class ErrorBlockColor { Danger, Ink, Background }

/** One corruption block of an error, in scene coordinates. */
internal class ErrorBlock(
    val x: Double,
    val y: Double,
    val size: Double,
    val color: ErrorBlockColor,
)

/** `errBlocks`: fourteen 4 or 8 dp blocks along the body's perimeter. */
internal fun MutantGenerator.errorBlocks(g: Instance): List<ErrorBlock> {
    val el = g.slot
    val w = el.w
    val h = el.h
    val rnd = Mulberry32(g.seed, "err")
    val out = ArrayList<ErrorBlock>()
    for (i in 0 until 14) {
        val bs = if (rnd.next() < .4) 8.0 else 4.0
        val side = floor(rnd.next() * 4).toInt()
        val u = rnd.next()
        val x =
            if (side < 2) {
                jsRound(u * (w - bs) / 4) * 4
            } else if (side == 2) {
                0.0
            } else {
                w - bs
            }
        val y =
            if (side == 0) {
                0.0
            } else if (side == 1) {
                h - bs
            } else {
                jsRound(u * (h - bs) / 4) * 4
            }
        val c = rnd.next()
        val color =
            if (c <
                .45
            ) {
                ErrorBlockColor.Danger
            } else if (c < .75) {
                ErrorBlockColor.Ink
            } else {
                ErrorBlockColor.Background
            }
        out += ErrorBlock(el.ox + x, el.oy + y, bs, color)
    }
    return out
}

/**
 * The front of a pour at progress p: a polyline from left to right, in tenths of a dp as the JS rounds
 * it, then down to [bottom] and back to [left]. The JS writes [bottom] and [left] unrounded.
 */
internal class PourFront(
    /** `x, y` pairs in tenths. */
    val points: IntArray,
    val rightTenths: Int,
    val bottom: Double,
    val left: Double,
)

/** `pourPath`: the front of a new colour running down from the top at p = 0…1, its drips running ahead. */
internal fun MutantGenerator.pourFront(
    g: Instance,
    p: Double,
): PourFront {
    val el = g.slot
    val ox = el.ox
    val oy = el.oy
    val w = el.w
    val h = el.h
    val rnd = Mulberry32(g.seed, "pour")
    val n = max(2.0, jsRound(w / 13)).toInt()
    val f =
        Array(n) {
            doubleArrayOf(
                rnd.next() * w,
                1.6 + 3.2 * rnd.next(),
                (5 + 16 * rnd.next()) * max(.5, min(1.0, h / 40)),
                .6 + .8 * rnd.next(),
            )
        }
    val bot = oy + h + el.over + 34
    val base = oy - 10 + p * (bot - oy + 12)
    val st = if (w > 120) 2.0 else 1.0
    val pts = ArrayList<Int>()
    var x = -12.0
    while (x <= w + 12) {
        var y = base + 1.6 * sin(x * .21 + p * 9)
        for (q in f) {
            val u = (x - q[0]) / q[1]
            if (u > -3 && u < 3) y += q[2] * min(1.0, p * 2.4 * q[3]) * exp(-u * u)
        }
        pts += GenPath.toTenths(ox + x)
        pts += GenPath.toTenths(min(bot, y))
        x += st
    }
    return PourFront(pts.toIntArray(), GenPath.toTenths(ox + w + 12), bot, ox - 12)
}

/** Where a selection came from and where it was clicked, relative to the new slot (`fx`). */
internal class VenomFrom(
    /** The old position minus the new one. */
    val dx: Double,
    val dy: Double,
    /** The click point from the centre of the new slot. */
    val px: Double = 0.0,
    val py: Double = 0.0,
)

/** One fat shape of the venom: the fat tentacle, its tail, or one of the two masses. */
internal sealed class VenomPart {
    internal class Stroke(
        val path: GenPath,
    ) : VenomPart()

    internal class Circle(
        val cx: Double,
        val cy: Double,
        val r: Double,
    ) : VenomPart()

    internal class Rect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val radius: Double,
    ) : VenomPart()
}

/**
 * The venom at one moment: [fat] — the fat tentacle and both masses, whose joins are softened by a blur
 * and an alpha threshold (`SF`); [thin] — the thin tentacles and spikes, left sharp (`S`); [full] — the new
 * pill is whole; [vertical] — the move is vertical.
 */
internal class VenomShapes(
    val fat: List<VenomPart>,
    val thin: List<GenPath>,
    val full: Boolean,
    val vertical: Boolean,
)

/**
 * `venomShapes`: the selection's move at p = 0…1 for the new slot [ox], [oy], [w], [h], [r] and [key].
 * Tentacles shoot from the old pill (one fat one to the click), the mass flows along them, the old pill
 * shrinks and the new one grows from where the fat one caught; at the end they let go and retract.
 */
internal fun MutantGenerator.venomShapes(
    ox: Double,
    oy: Double,
    w: Double,
    h: Double,
    r: Double,
    key: String,
    from: VenomFrom,
    p: Double,
): VenomShapes {
    val cx = ox + w / 2
    val cy = oy + h / 2
    val eo = { v: Double -> 1 - (1 - clamp01(v)).pow(2) }
    val ei = { v: Double -> clamp01(v) * clamp01(v) }
    val mx = -from.dx
    val my = -from.dy
    val vert = abs(my) >= abs(mx)
    val sg = if ((if (vert) my else mx) < 0) -1.0 else 1.0
    val ax = if (vert) 0.0 else sg
    val ay = if (vert) sg else 0.0
    val ha = (if (vert) h else w) / 2
    val hp = (if (vert) w else h) / 2
    val d0 = abs(if (vert) my else mx).let { if (it == 0.0) 1.0 else it }
    val ex = cx + from.dx + ax * ha
    val ey = cy + from.dy + ay * ha
    val pt = { s: Double, c: Double -> doubleArrayOf(ex + ax * s + abs(ay) * c, ey + ay * s + abs(ax) * c) }
    val rnd = Mulberry32("$cassette/$key", "venom")
    val gap = max(0.0, d0 - 2 * ha)
    val lerp = { a: Double, b: Double, t: Double -> a + (b - a) * t }
    val kd = 1 - max(5.0, ha * (if (vert) .24 else .5)) / (2 * ha)
    var pd = .8
    var lo = .15
    for (i in 0 until 12) {
        val mid = (lo + pd) / 2
        if (smoothstep(.15, .8, mid) < kd) lo = mid else pd = mid
    }
    val k = smoothstep(.15, .8, p)
    val full = p >= .8
    val n = if (vert) max(3.0, min(6.0, jsRound(hp / 18))).toInt() else 3
    val lead = floor(rnd.next() * n).toInt()
    val rr = min(r, hp)
    val hs = 1 - (if (vert) .8 else .45) * k * k
    val fat = ArrayList<VenomPart>()
    val thin = ArrayList<GenPath>()
    val km = if (vert) min(hp * .6, 30.0) else 8.0
    val kc = max(-hp + km, min(hp - km, if (vert) from.px else from.py))
    val ks = max(gap + ha * .5, min(gap + 1.5 * ha, gap + ha + sg * (if (vert) from.py else from.px)))
    for (i in 0 until n + 3) {
        val jit = rnd.next()
        val q = rnd.next()
        val r1 = rnd.next()
        val r2 = rnd.next()
        val r3 = rnd.next()
        val r4 = rnd.next()
        val isFat = i == lead
        val spike = i >= n
        val straight = vert && !isFat && !spike && (i == (lead + 1) % n || i == (lead + 2) % n)
        val b =
            if (isFat) {
                min(if (vert) 11 + 5 * q else hp * .45, ha * .8)
            } else if (spike) {
                1.6 + 1.2 * q
            } else {
                1.8 +
                    2.2 * q * q
            }
        val slot = if (spike) -1 + 2 * jit else -1 + 2 * (i + .5 + (jit - .5) * .7) / n
        val cr0 = slot * max(0.0, hp - rr - b)
        val cr = if (isFat) lerp(cr0, kc, .6) else cr0
        val ct =
            if (isFat) {
                kc
            } else if (straight || spike) {
                cr * .92
            } else {
                max(-hp + 8, min(hp - 8, lerp(cr, kc, .45) + (r1 - .5) * hp * .7))
            }
        val st = if (isFat) ks else gap + ha * (.7 + .6 * r2)
        val dl = if (isFat) 0.0 else .03 + .14 * r3
        val pa = if (spike) 9.0 else dl + (if (isFat) .2 else .16 + .1 * r4)
        if (p <= dl) continue
        val sw =
            if (straight) {
                0.0
            } else {
                (if (r1 < .5) -1.0 else 1.0) * (if (spike) 3.0 else 5 + 10 * r4) *
                    (if (vert) 1.0 else .5)
            }
        val ph = r2 * 6.28
        val on = p >= pa
        val hu =
            if (spike) {
                (.3 + .3 * r4) * eo((p - dl) / .12) * (1 - ei((p - dl - .16) / .22))
            } else if (on) {
                1.0
            } else {
                eo((p - dl) / (pa - dl))
            }
        val tu = if (spike) 0.0 else clamp01((p - pd) / ((if (isFat) .96 else min(.96, pd + .12)) - pd))
        var mass: DoubleArray? = null // e, ah, ch, sc, cc
        if (isFat && on && !full) {
            val e = smoothstep(0.0, 1.0, (p - pa) / (.8 - pa))
            val ah = lerp(min(ha, b * .9), ha, e)
            val ch = lerp(min(hp, b * 1.3), hp, e)
            mass =
                doubleArrayOf(
                    e,
                    ah,
                    ch,
                    max(gap + ah, min(gap + 2 * ha - ah, lerp(st, gap + ha, e))),
                    max(-hp + ch, min(hp - ch, lerp(ct, 0.0, smoothstep(.35, 1.0, e)))),
                )
        }
        if (hu - tu > .04) {
            val th = if (on) 1 + (if (isFat) .3 else .9) * sin(PI * clamp01((p - pa) / max(.05, pd - pa))) else 1.0
            val count = if (isFat) 32 else 8
            val nEdge = if (mass != null) mass[3] - mass[1] else st
            val rf = if (isFat && on) min(26.0, min(ha * 1.3, nEdge * .6)) else 0.0
            val arc = {
                d: Double,
                rad: Double,
                ->
                rad - sqrt(max(0.0, rad * rad - (rad - max(0.0, min(rad, d))).pow(2)))
            }
            val extO = 2 * ha * (1 - k)
            val rfO = min(rf, extO * .8)
            var cA = 0.0
            var cZ = 0.0
            val rT = if (isFat && tu > 0) b * th * (.94 - .35 * tu) / 2 else 0.0
            val tq = min(hu, tu + rT / (st + 3))
            val c = ArrayList<DoubleArray>()
            val wd = ArrayList<Double>()
            for (j in 0..count) {
                val v = j.toDouble() / count
                val u = tq + (hu - tq) * v
                val sj = lerp(-3.0, st, u)
                val cj =
                    lerp(cr * hs, ct, if (isFat) smoothstep(0.0, 1.0, u) else u) +
                        sw * sin(PI * u).pow(2) * (1 + .5 * sin(2 * PI * u + ph))
                var wj =
                    b * th *
                        (
                            if (!on) {
                                .1 + 1.9 * (1 - v).pow(2)
                            } else if (tu > 0) {
                                (if (isFat) .75 - .35 * tu + .19 * (1 - v) else .4 + 1.2 * (1 - tu) * (1 - v)) +
                                    1.6 * v.pow(1.5)
                            } else if (isFat) {
                                .75 + 1.6 * (1 - k) * (1 - v).pow(1.5) + 1.6 * v.pow(1.5)
                            } else {
                                .4 + 1.8 * abs(2 * v - 1).pow(3)
                            }
                        )
                wj = min(wj, max(.6, gap + 2 * ha - 1 - sj))
                if (isFat) wj = min(wj, max(b * .5, 2 * (hp - abs(cj) - 1)))
                if (rf > 1.5) {
                    if (mass !=
                        null
                    ) {
                        wj = min(wj + 2 * arc(nEdge - sj, rf), max(wj, 2 * (mass[2] - abs(cj - mass[4]) - .5)))
                    }
                    if (tu == 0.0 && p < pd) wj = min(wj + 2 * arc(sj, rfO), max(wj, 2 * (hp * hs - abs(cj) - .5)))
                    if (sj > nEdge) wj = min(wj, max(.6, gap + 2 * ha - 1 - sj))
                }
                if (j == 0) cA = cj
                cZ = cj
                c += pt(sj, cj)
                wd += wj
            }
            if (tu == 0.0) {
                c.add(0, pt(-max(1.0, min(12.0, extO - 1)), cA))
                wd.add(0, wd[0])
            }
            if (isFat && on) {
                c += pt(min(gap + 2 * ha - 3, max(st + min(8.0, ha * .5), nEdge + 4)), cZ)
                wd += wd[wd.size - 1]
            }
            val stroke = taperPath(c, wd)
            if (isFat) fat += VenomPart.Stroke(stroke) else thin += stroke
            if (rT > 1) fat += VenomPart.Circle(c[0][0], c[0][1], min(rT, wd[1] / 2))
        }
        if (mass != null) {
            val a = pt(mass[3] - mass[1], mass[4] - mass[2])
            val bq = pt(mass[3] + mass[1], mass[4] + mass[2])
            val w0 = abs(a[0] - bq[0])
            val h0 = abs(a[1] - bq[1])
            val rx =
                min(
                    lerp(min(mass[1], mass[2]), min(r, min(ha, hp)), smoothstep(.65, 1.0, mass[0])),
                    min(
                        w0 / 2,
                        h0 / 2,
                    ),
                )
            fat += VenomPart.Rect(min(a[0], bq[0]), min(a[1], bq[1]), w0, h0, rx)
        }
    }
    val ext = 2 * ha * (1 - k)
    if (p < pd) {
        val a = pt(-ext, -hp * hs)
        val bq = pt(0.0, hp * hs)
        val w0 = abs(a[0] - bq[0])
        val h0 = abs(a[1] - bq[1])
        fat += VenomPart.Rect(min(a[0], bq[0]), min(a[1], bq[1]), w0, h0, min(r, min(w0 / 2, h0 / 2)))
    }
    return VenomShapes(fat, thin, full, vert)
}

/** `taper` over a list of axis points, as one [GenPath]. */
internal fun taperPath(
    points: List<DoubleArray>,
    widths: List<Double>,
): GenPath {
    val builder = GenPathBuilder()
    taperInto(
        builder,
        DoubleArray(points.size) { points[it][0] },
        DoubleArray(points.size) { points[it][1] },
        widths.toDoubleArray(),
    )
    return builder.build()
}
