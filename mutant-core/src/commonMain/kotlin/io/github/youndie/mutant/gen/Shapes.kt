package io.github.youndie.mutant.gen

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * `ucap`: the signed distance from (px, py) to an unequal capsule from (ax, ay) with radius ra to
 * (bx, by) with radius rb. Drips, necks, drops and bubbles are all made of it.
 */
internal fun unequalCapsule(
    px0: Double,
    py0: Double,
    ax: Double,
    ay: Double,
    bx: Double,
    by: Double,
    ra: Double,
    rb: Double,
): Double {
    val px = px0 - ax
    val py = py0 - ay
    val bxr = bx - ax
    val byr = by - ay
    val h = bxr * bxr + byr * byr
    if (h < 1e-4) return jsHypot(px, py) - ra
    val qx = abs((px * byr - py * bxr) / h)
    val qy = (px * bxr + py * byr) / h
    val b = ra - rb
    val cx = sqrt(max(h - b * b, 1e-4))
    val k = cx * qy - b * qx
    val n = qx * qx + qy * qy
    if (k < 0) return sqrt(h * n) - ra
    if (k > cx) return sqrt(h * (n + 1 - 2 * qy)) - rb
    return cx * qx + b * qy - ra
}

/** `ty`: the top edge's offset at x and time t, two travelling sines from the instance's wave. */
internal fun topWave(
    g: Instance,
    x: Double,
    t: Double,
): Double {
    val w = g.wave
    val amplitude = w[0]
    val lambda = w[1]
    val p0 = w[2]
    val p1 = w[3]
    val ws = w[4]
    val tau = 6.2832
    return -amplitude *
        (.2 + .6 * sin(tau * x / lambda + p0 + t * ws) + .25 * sin(tau * x / (lambda * .53) + p1 - t * ws * .7))
}

/**
 * `bodyD`: the signed distance to the instance's body with no cache — the slot's rounded rectangle bent
 * by the shape and by [topOffset] at the top. Used to place flow lines and for the lower half of the
 * field.
 */
internal fun bodyDistance(
    px: Double,
    py: Double,
    g: Instance,
    topOffset: Double,
): Double {
    val w = g.slot.w
    val h = g.slot.h
    val s = g.shape
    val u = clamp01(px / w)
    val v = clamp01(py / h)
    val m = clamp01(1 - py / (h * .5))
    var x = px + (if (px < w / 2) s[0] else -s[1]) * sin(PI * v)
    var y = py - (topOffset + s[2] * (u - .5) * 2 + s[4] * sin(PI * u * s[5] + s[6])) * m
    x += s[3] * valueNoise(px * .03 + s[7], py * .05)
    y += s[3] * .7 * valueNoise(px * .03 + s[7] + 31, py * .05 + 9)
    return roundedRectDistance(g, x, y)
}

/** `rrD`: the signed distance to the slot's rectangle with the four corner radii `sh[8…11]`. */
internal fun roundedRectDistance(
    g: Instance,
    x: Double,
    y: Double,
): Double {
    val w = g.slot.w
    val h = g.slot.h
    val s = g.shape
    val cx = x - w / 2
    val cy = y - h / 2
    val rr = if (cy > 0) (if (cx > 0) s[10] else s[11]) else (if (cx > 0) s[9] else s[8])
    val qx = abs(cx) - w / 2 + rr
    val qq = abs(cy) - h / 2 + rr
    return min(max(qx, qq), 0.0) + jsHypot(max(qx, 0.0), max(qq, 0.0)) - rr
}
