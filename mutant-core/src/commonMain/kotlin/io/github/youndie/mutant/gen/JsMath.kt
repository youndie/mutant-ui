package io.github.youndie.mutant.gen

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/*
 * The few places where JavaScript's arithmetic is not Kotlin's, ported as JavaScript defines them, so the
 * port draws what `mutant-generator.js` draws (research §1.4).
 */

/**
 * `Math.round`: the nearest integer, ties toward +∞. Not `kotlin.math.round` (ties to even), and not
 * `floor(x + 0.5)`, which is wrong for 0.49999999999999994.
 */
internal fun jsRound(x: Double): Double {
    val f = floor(x)
    return if (x - f >= 0.5) f + 1.0 else f
}

/**
 * `Math.hypot(a, b)` as V8 computes it: both values normalised by the larger one and summed with Kahan
 * compensation. The JVM's `Math.hypot` is a different algorithm and differs in the last bit.
 */
internal fun jsHypot(
    a: Double,
    b: Double,
): Double {
    if (a.isNaN() || b.isNaN()) return Double.NaN
    val x = abs(a)
    val y = abs(b)
    val max = if (y > x) y else x
    if (max == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY
    if (max == 0.0) return 0.0
    var sum = 0.0
    var compensation = 0.0
    for (v in doubleArrayOf(x, y)) {
        val n = v / max
        val summand = n * n - compensation
        val preliminary = sum + summand
        compensation = (preliminary - sum) - summand
        sum = preliminary
    }
    return sqrt(sum) * max
}

/** `Math.max(0, Math.min(1, v))`. */
internal fun clamp01(v: Double): Double = maxOf(0.0, minOf(1.0, v))

/** `Math.max(lo, Math.min(hi, v))`. */
internal fun clamp(
    v: Double,
    lo: Double,
    hi: Double,
): Double = maxOf(lo, minOf(hi, v))

/** A value as a `Float32Array` stores it and reads it back. */
internal fun f32(v: Double): Double = v.toFloat().toDouble()

/**
 * `Number.prototype.toFixed(digits)` as a number, for the few values the JS rounds that way before they
 * reach the picture (a speckle's position, the outline's width): the magnitude rounded half up.
 */
internal fun jsToFixed(
    v: Double,
    digits: Int,
): Double {
    var scale = 1.0
    repeat(digits) { scale *= 10 }
    val t = jsRound(abs(v) * scale) / scale
    return if (v < 0) -t else t
}
