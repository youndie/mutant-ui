package io.github.youndie.mutant.gen

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-149: [LatticeRows] is [valueNoise] to the bit over a grid walked row by row, whatever the grid's step against the
 * lattice: corners shared along a row, rows on the same lattice row, the next one, or further.
 */
class LatticeRowsTest {
    private fun walk(
        nx: Int,
        ny: Int,
        x0: Double,
        y0: Double,
        s: Double,
        fx: Double,
        fy: Double,
        cx: Double,
        cy: Double,
    ) {
        val rows = LatticeRows(nx) { i -> (x0 + i * s) * fx + cx }
        for (j in 0 until ny) {
            for (i in 0 until nx) {
                val x = (x0 + i * s) * fx + cx
                val y = (y0 + j * s) * fy + cy
                val want = valueNoise(x, y)
                val got = rows.noise(x, y)
                assertEquals(want.toRawBits(), got.toRawBits(), "($i, $j) at ($x, $y): $want, got $got")
            }
        }
    }

    @Test
    fun `the noise of every cell is valueNoise's to the bit at the field's own steps`() {
        // A wide body's grid of 1.5 dp and a narrow one's of 0.9, against the edge's 0.8 and the warp's 0.03 × 0.05.
        for (s in listOf(1.5, .9)) {
            walk(230, 100, -7.0, -13.0, s, .8, .8, 0.0, 0.0)
            walk(230, 100, -7.0, -13.0, s, .03, .05, 3.7, 0.0)
            walk(230, 100, -7.0, -13.0, s, .03, .05, 34.7, 9.0)
        }
    }

    @Test
    fun `the noise is valueNoise's to the bit at steps that skip lattice rows and columns`() {
        val r = Random(149)
        repeat(40) {
            val s = .2 + r.nextDouble() * 4
            val f = .01 + r.nextDouble() * 3
            walk(
                40,
                30,
                r.nextDouble() * 200 - 100,
                r.nextDouble() * 200 - 100,
                s,
                f,
                f * (.5 + r.nextDouble()),
                r.nextDouble() * 50,
                r.nextDouble() * 50,
            )
        }
    }
}
