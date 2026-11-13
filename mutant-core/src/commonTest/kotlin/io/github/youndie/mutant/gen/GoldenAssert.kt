package io.github.youndie.mutant.gen

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.fail

/** The comparisons every generator golden test uses (B-03, B-04). */
internal object GoldenAssert {
    /** Numbers of the instance: to 1e-9, relative above 1. */
    fun near(
        want: Double,
        got: Double,
        what: String,
    ) {
        if (abs(want - got) > 1e-9 * max(1.0, abs(want))) fail("$what: expected $want, got $got")
    }

    fun numbers(
        want: DoubleArray,
        got: DoubleArray,
        what: String,
    ) {
        assertEquals(want.size, got.size, "$what: count")
        for (i in want.indices) near(want[i], got[i], "$what[$i]")
    }

    /** Numbers the JS rounded to tenths: within 0.1, the handoff's tolerance between platforms. */
    fun tenths(
        want: DoubleArray,
        got: DoubleArray,
        what: String,
    ) {
        assertEquals(want.size, got.size, "$what: count")
        for (i in want.indices) {
            if (abs(want[i] - got[i]) > .1 + 1e-9) fail("$what[$i]: expected ${want[i]}, got ${got[i]}")
        }
    }

    /** The same commands in the same order, every coordinate within 0.1 dp. */
    fun path(
        want: String,
        got: String,
        what: String,
    ) {
        val a = tokens(want)
        val b = tokens(got)
        assertEquals(a.map { it.first }, b.map { it.first }, "$what: command structure")
        for (i in a.indices) {
            if (abs(a[i].second - b[i].second) >
                .1 + 1e-9
            ) {
                fail("$what: token $i is ${b[i].second}, expected ${a[i].second}")
            }
        }
    }

    /** Commands as `'M'`/`'Q'`/`'Z'` with 0, numbers as `'#'` with their value. */
    fun tokens(path: String): List<Pair<Char, Double>> =
        Regex("""[MQZ]|-?\d+(?:\.\d+)?""")
            .findAll(path)
            .map {
                val v = it.value
                if (v[0] in "MQZ") v[0] to 0.0 else '#' to v.toDouble()
            }.toList()

    /** `Number.prototype.toFixed(1)` as a number: half away from zero on the magnitude. */
    fun toFixed1(v: Double): Double {
        val t = jsRound(abs(v) * 10) / 10
        return if (v < 0) -t else t
    }
}
