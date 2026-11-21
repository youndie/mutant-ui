package io.github.youndie.mutant.gen

import kotlin.math.floor

/**
 * `hash`: FNV-1a over the string's UTF-16 code units (`charCodeAt`), 32 bits, unsigned. The root of
 * every seed.
 */
internal fun fnv1a(s: String): Long {
    var h = 0x811c9dc5.toInt()
    for (c in s) {
        h = h xor c.code
        h *= 0x01000193
    }
    return h.toLong() and 0xFFFFFFFFL
}

/**
 * `rng`: mulberry32 seeded by the hash of `"$seed/$stream"`. Streams are independent, so an extra call
 * in one never shifts another; the order of calls inside a stream is part of the contract.
 */
internal class Mulberry32(
    seed: String,
    stream: String,
) {
    private var a: Int = fnv1a("$seed/$stream").toInt()

    /** The next number in [0, 1). */
    fun next(): Double {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

/** `ss`: smoothstep(a, b, x), clamped to 0…1. */
internal fun smoothstep(
    a: Double,
    b: Double,
    x: Double,
): Double {
    val v = clamp01((x - a) / (b - a))
    return v * v * (3 - 2 * v)
}

/** `smin`: the smooth minimum of two distances with blend radius k; drips and bubbles join the body by it. */
internal fun smoothMin(
    a: Double,
    b: Double,
    k: Double,
): Double {
    val h = maxOf(k - kotlin.math.abs(a - b), 0.0) / k
    return minOf(a, b) - h * h * k * .25
}

/** `h21`: the lattice hash `fract(sin(127.1·x + 311.7·y) · 43758.5453)`, 0…1. */
internal fun latticeHash(
    x: Double,
    y: Double,
): Double {
    val s = jsSin(x * 127.1 + y * 311.7) * 43758.5453
    return s - floor(s)
}

/** `vn`: value noise on a unit lattice, −1…1. */
internal fun valueNoise(
    x: Double,
    y: Double,
): Double {
    val ix = floor(x)
    val iy = floor(y)
    val fx = x - ix
    val fy = y - iy
    val ux = fx * fx * (3 - 2 * fx)
    val uy = fy * fy * (3 - 2 * fy)
    val a = latticeHash(ix, iy)
    val b = latticeHash(ix + 1, iy)
    val c = latticeHash(ix, iy + 1)
    val d = latticeHash(ix + 1, iy + 1)
    return (a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy) * 2 - 1
}
