package io.github.youndie.mutant.gen

import io.github.youndie.mutant.gen.goldens.genGoldens
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-84: between two drips standing still, the web's depth must not jump with the boil. Measured at each adjacent
 * pair's midline over 60 s of 125 ms ticks, on every golden body with drips.
 */
class SteadyWebsTest {
    private class Count(
        var ticks: Int = 0,
        var jumps: Int = 0,
    )

    private fun measure(steady: Boolean): Count {
        val n = Count()
        for (g0 in genGoldens.distinctBy { it.key }) {
            val s = g0.slot
            if (s[5] <= 0 || g0.still) continue
            val slot =
                Slot(
                    s[0],
                    s[1],
                    s[2],
                    s[3],
                    s[4],
                    s[5],
                    s[6],
                    s[7],
                    s[8] != 0.0,
                    s[9],
                    s[10] != 0.0,
                    s[11] != 0.0,
                    s[12] != 0.0,
                    s.copyOfRange(13, 17),
                    s.copyOfRange(17, 21),
                )
            val gen = MutantGenerator(cassette = "5")
            val g = gen.instance(slot, g0.key, g0.still)
            val cache = FieldCache()
            val pairs = g.drips.zipWithNext()
            val last = DoubleArray(pairs.size)

            fun pose(
                d: Drip,
                t: Double,
            ) = gen.dripState(d, t, slot.h, g.k)
            for (tick in 0 until 480) {
                val t = tick * 125.0
                val f = gen.frame(g, cache, t, 0.0, pressed = false, steadyWebs = steady).field
                for ((pi, pr) in pairs.withIndex()) {
                    val i = Math.round((slot.ox + (pr.first.x + pr.second.x) / 2 - f.x0) / f.step).toInt()
                    var j = Math.round((slot.oy + slot.h - f.y0) / f.step).toInt()
                    while (j < f.ny && f.values[j * f.nx + i] < 0) j++
                    val depth = f.y0 + j * f.step - (slot.oy + slot.h)
                    if (tick > 0) {
                        val still =
                            listOf(pr.first, pr.second).all { d ->
                                val a = pose(d, t - 125.0)
                                val b = pose(d, t)
                                abs(b.length - a.length) < 1 && abs(b.tipRadius - a.tipRadius) < .5
                            }
                        if (still) {
                            n.ticks++
                            if (abs(depth - last[pi]) >= 3.0) n.jumps++
                        }
                    }
                    last[pi] = depth
                }
            }
        }
        return n
    }

    @Test
    fun `a web between still drips does not jump with the boil`() {
        val engine = measure(steady = false)
        val steady = measure(steady = true)
        println("WEB engine ${engine.jumps}/${engine.ticks} steady ${steady.jumps}/${steady.ticks}")
        assertTrue(engine.jumps * 20 > engine.ticks, "the engine's webs jump: ${engine.jumps} of ${engine.ticks}")
        assertTrue(steady.jumps * 100 < steady.ticks, "steady webs jump ${steady.jumps} of ${steady.ticks} still ticks")
    }
}
