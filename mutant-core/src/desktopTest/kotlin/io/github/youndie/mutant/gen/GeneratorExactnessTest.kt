package io.github.youndie.mutant.gen

import io.github.youndie.mutant.gen.goldens.genGoldens
import io.github.youndie.mutant.gen.goldens.goldenPour
import io.github.youndie.mutant.gen.goldens.goldenVenom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-03: on the JVM the port reproduces every golden path string byte for byte, not only within the
 * handoff's 0.1 dp. The common test allows 0.1 dp because `sin` and `exp` may differ in the last bit
 * between platforms; measured on 2026-11-14, the JVM, the iOS simulator and wasm all come out exact.
 * Exactness is what holds the details a 0.1 dp tolerance cannot see: V8's `Math.hypot`, the Float32
 * storage of the field, JS rounding. If this fails on another host while the common test passes, the
 * host's maths differs: a finding about the host, not a defect of the port.
 */
class GeneratorExactnessTest {
    @Test
    fun `every golden path is byte-identical on the JVM`() {
        val gen = MutantGenerator(cassette = "5")
        val differ = ArrayList<String>()
        for (golden in genGoldens) {
            val s = golden.slot
            val slot =
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
                )
            val g = gen.instance(slot, golden.key, golden.still)
            val cache = FieldCache()
            for (f in golden.frames) {
                val frame = gen.frame(g, cache, f.t, f.heat)
                if (frame.contour.toSvg() != f.contour) differ += "${golden.name} @${f.t} contour"
                if (frame.highlight.toSvg() != f.highlight) differ += "${golden.name} @${f.t} highlight"
                if (frame.flowLines.toSvg() != f.flowLines) differ += "${golden.name} @${f.t} flow lines"
            }
        }
        assertEquals(emptyList(), differ, "paths that are within tolerance but not byte-identical")
    }

    @Test
    fun `every venom shape and pour front is exact on the JVM`() {
        val gen = MutantGenerator(cassette = "5")
        val differ = ArrayList<String>()
        for (case in goldenVenom) {
            val s = case.slot
            for (f in case.frames) {
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
                for ((want, part) in f.fat.zip(got.fat)) {
                    val same =
                        when (part) {
                            is VenomPart.Stroke -> {
                                want.path == part.path.toSvg()
                            }

                            is VenomPart.Circle -> {
                                want.circle!!.toList() == listOf(part.cx, part.cy, part.r).map(GoldenAssert::toFixed1)
                            }

                            is VenomPart.Rect -> {
                                want.rect!!.toList() ==
                                    listOf(
                                        part.x,
                                        part.y,
                                        part.width,
                                        part.height,
                                        part.radius,
                                    ).map(GoldenAssert::toFixed1)
                            }
                        }
                    if (!same) differ += "${case.name} @${f.p} fat"
                }
                for ((want, path) in f.thin.zip(got.thin)) {
                    if (want.path !=
                        path.toSvg()
                    ) {
                        differ += "${case.name} @${f.p} thin"
                    }
                }
            }
        }
        for ((name, frames) in goldenPour) {
            val golden = genGoldens.single { it.name == name }
            val s = golden.slot
            val g =
                gen.instance(
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
            for (f in frames) {
                val front = gen.pourFront(g, f.p)
                val got =
                    (front.points.toList() + front.rightTenths).map { it / 10.0 } +
                        listOf(front.bottom, front.left, front.bottom)
                if (got != f.numbers.toList()) differ += "$name @${f.p} pour"
            }
        }
        assertEquals(emptyList(), differ, "transition shapes that are within tolerance but not exact")
    }
}
