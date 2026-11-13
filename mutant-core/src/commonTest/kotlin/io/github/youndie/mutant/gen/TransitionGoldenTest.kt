package io.github.youndie.mutant.gen

import io.github.youndie.mutant.gen.goldens.GoldenVenomPart
import io.github.youndie.mutant.gen.goldens.genGoldens
import io.github.youndie.mutant.gen.goldens.goldenErrorBlocks
import io.github.youndie.mutant.gen.goldens.goldenPour
import io.github.youndie.mutant.gen.goldens.goldenVenom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * B-04: the geometry outside the resting frame — the venom of a selection, the pour of a new colour, the
 * corruption blocks of an error — is what `mutant-generator.js` emits, on every target. Paths and the
 * numbers the JS rounds to tenths are held to 0.1 dp; unrounded numbers to 1e-9.
 */
class TransitionGoldenTest {
    private val gen = MutantGenerator(cassette = "5")

    private fun instance(name: String): Instance {
        val golden = genGoldens.single { it.name == name }
        val s = golden.slot
        return gen.instance(
            Slot(
                ox = s[0],
                oy = s[1],
                w = s[2],
                h = s[3],
                r = s[4],
                over = s[5],
                dripShare = s[6],
                wobble = s[7],
                round = s[8] != 0.0,
                calm = s[9],
                dead = s[10] != 0.0,
                marks = s[11] != 0.0,
                edge = s[12] != 0.0,
                textZone = s.copyOfRange(13, 17),
                cleanZone = s.copyOfRange(17, 21),
            ),
            golden.key,
            golden.still,
        )
    }

    @Test
    fun `the venom matches the JS at every 24th of its run for each case`() {
        assertEquals(3, goldenVenom.size, "venom cases")
        for (case in goldenVenom) {
            for (f in case.frames) {
                val at = "${case.name} @${f.p}"
                val s = case.slot
                val got =
                    gen.venomShapes(
                        s[0],
                        s[1],
                        s[2],
                        s[3],
                        s[4],
                        case.key,
                        VenomFrom(case.from[0], case.from[1], case.from[2], case.from[3]),
                        f.p,
                    )
                assertEquals(f.full, got.full, "$at full")
                assertEquals(f.vertical, got.vertical, "$at vertical")
                assertEquals(f.fat.size, got.fat.size, "$at fat shapes")
                for ((i, pair) in f.fat.zip(got.fat).withIndex()) checkPart(pair.first, pair.second, "$at fat[$i]")
                assertEquals(f.thin.size, got.thin.size, "$at thin strokes")
                for ((i, pair) in f.thin.zip(got.thin).withIndex()) {
                    GoldenAssert.path(pair.first.path!!, pair.second.toSvg(), "$at thin[$i]")
                }
            }
        }
    }

    private fun checkPart(
        want: GoldenVenomPart,
        got: VenomPart,
        what: String,
    ) {
        when (got) {
            is VenomPart.Stroke -> {
                GoldenAssert.path(want.path ?: fail("$what: expected a ${kind(want)}"), got.path.toSvg(), what)
            }

            is VenomPart.Circle -> {
                GoldenAssert.tenths(
                    want.circle ?: fail("$what: expected a ${kind(want)}"),
                    doubleArrayOf(got.cx, got.cy, got.r).map(GoldenAssert::toFixed1).toDoubleArray(),
                    what,
                )
            }

            is VenomPart.Rect -> {
                GoldenAssert.tenths(
                    want.rect ?: fail("$what: expected a ${kind(want)}"),
                    doubleArrayOf(
                        got.x,
                        got.y,
                        got.width,
                        got.height,
                        got.radius,
                    ).map(GoldenAssert::toFixed1).toDoubleArray(),
                    what,
                )
            }
        }
    }

    private fun kind(p: GoldenVenomPart) =
        if (p.path != null) {
            "path"
        } else if (p.circle != null) {
            "circle"
        } else {
            "rect"
        }

    @Test
    fun `the pour front matches the JS at every eighth of its run`() {
        assertTrue(goldenPour.size >= 5, "pour cases")
        for ((name, frames) in goldenPour) {
            val g = instance(name)
            for (f in frames) {
                val front = gen.pourFront(g, f.p)
                val tenths = (front.points.toList() + front.rightTenths).map { it / 10.0 }
                val want = f.numbers
                // The JS writes M x,y L x,y … L right,bottom L left,bottom Z: tenths, then the raw bottom and left.
                GoldenAssert.tenths(want.copyOfRange(0, tenths.size), tenths.toDoubleArray(), "$name @${f.p} front")
                GoldenAssert.numbers(
                    want.copyOfRange(tenths.size, want.size),
                    doubleArrayOf(front.bottom, front.left, front.bottom),
                    "$name @${f.p} bottom, left",
                )
            }
        }
    }

    @Test
    fun `the error blocks match the JS`() {
        assertTrue(goldenErrorBlocks.size >= 5, "error block cases")
        for ((name, want) in goldenErrorBlocks) {
            val got =
                gen
                    .errorBlocks(
                        instance(name),
                    ).flatMap { listOf(it.x, it.y, it.size, it.color.ordinal.toDouble()) }
            GoldenAssert.numbers(want, got.toDoubleArray(), "$name error blocks")
        }
    }
}
