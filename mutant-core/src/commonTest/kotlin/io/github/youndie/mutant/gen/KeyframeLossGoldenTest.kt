package io.github.youndie.mutant.gen

import io.github.youndie.mutant.gen.goldens.goldenMosh
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-48: the keyframe loss's plan is the engine's `moshStart`, number for number. */
class KeyframeLossGoldenTest {
    @Test
    fun everyLossPlansTheEnginesBandAndBlocks() {
        for (g in goldenMosh) {
            val p = moshPlan("5", g.n, g.w, g.h)
            val at = "loss ${g.n} on ${g.w} x ${g.h}"
            assertEquals(g.y0, p.y0, "$at: y0")
            assertEquals(g.rows, p.rows, "$at: rows")
            assertEquals(g.durationMs, p.durationMs, "$at: duration")
            assertEquals(g.runs.size / 9, p.runs.size, "$at: blocks")
            p.runs.forEachIndexed { i, b ->
                val e = g.runs.copyOfRange(i * 9, i * 9 + 9)
                val got =
                    doubleArrayOf(
                        b.x.toDouble(),
                        b.y.toDouble(),
                        b.w.toDouble(),
                        b.h.toDouble(),
                        b.vx.toDouble(),
                        b.vy.toDouble(),
                        b.start,
                        if (b.gone) 1.0 else 0.0,
                        b.tint.toDouble(),
                    )
                assertEquals(e.toList(), got.toList(), "$at: block $i")
            }
        }
    }
}
