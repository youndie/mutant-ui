package io.github.youndie.mutant.gen

import io.github.youndie.mutant.gen.goldens.genGoldens
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
}
