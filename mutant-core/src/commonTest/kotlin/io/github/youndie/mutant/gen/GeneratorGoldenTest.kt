package io.github.youndie.mutant.gen

import io.github.youndie.mutant.gen.goldens.GoldenInstance
import io.github.youndie.mutant.gen.goldens.genGoldens
import io.github.youndie.mutant.gen.goldens.goldenHashes
import io.github.youndie.mutant.gen.goldens.goldenNoise
import io.github.youndie.mutant.gen.goldens.goldenStreams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-03: the port draws what `mutant-generator.js` draws, on every target. The goldens are emitted by the
 * vendored JS itself (`scripts/gen-goldens.mjs`). Hash, streams, noise and the instance's numbers are
 * held to 1e-9; a path is held to the same commands in the same order and every coordinate within
 * 0.1 dp, the handoff's own tolerance for `sin` and `exp` differing in the last bit between platforms.
 */
class GeneratorGoldenTest {
    private val gen = MutantGenerator(cassette = "5")

    @Test
    fun `FNV-1a matches the JS for every string including surrogate pairs`() {
        for ((s, h) in goldenHashes) assertEquals(h, fnv1a(s), "hash of \"$s\"")
    }

    @Test
    fun `every mulberry32 stream matches the JS for twelve draws`() {
        for ((seed, stream, want) in goldenStreams) {
            val r = Mulberry32(seed, stream)
            val got = DoubleArray(want.size) { r.next() }
            GoldenAssert.numbers(want, got, "$seed | $stream")
        }
    }

    @Test
    fun `the lattice hash and value noise match the JS`() {
        for (i in goldenNoise.indices step 4) {
            val x = goldenNoise[i]
            val y = goldenNoise[i + 1]
            GoldenAssert.near(goldenNoise[i + 2], latticeHash(x, y), "h21($x, $y)")
            GoldenAssert.near(goldenNoise[i + 3], valueNoise(x, y), "vn($x, $y)")
        }
    }

    @Test
    fun `every instance matches the JS's genItem`() {
        assertTrue(genGoldens.size >= 27, "goldens found: ${genGoldens.size}")
        for (golden in genGoldens) checkInstance(golden)
    }

    @Test
    fun `every frame's primitives and grid and contour and highlight and flow lines match the JS`() {
        val report = StringBuilder()
        var exact = 0
        var total = 0
        val cut = ArrayList<String>()
        for (golden in genGoldens) {
            val g = instanceOf(golden)
            val cache = FieldCache()
            for (f in golden.frames) {
                val at = "${golden.name} @${f.t} heat ${f.heat}"
                val frame = gen.frame(g, cache, f.t, f.heat)
                assertEquals(f.boilPhase, frame.boilPhase, "$at boil phase")
                GoldenAssert.near(f.boil, frame.boil, "$at boil")
                GoldenAssert.numbers(
                    f.primitives,
                    frame.primitives.flatMap { it.toList() }.toDoubleArray(),
                    "$at primitives",
                )
                val fd = frame.field
                // B-97: where the engine's field reaches its grid's edge, the engine's contour is cut there and the
                // port's grid has grown by whole cells past it; the origin and the step stay the engine's.
                val engineCut = gen.frame(g, FieldCache(), f.t, f.heat, grow = false).field.cut
                if (engineCut) cut += "${golden.name} @${f.t}"
                GoldenAssert.numbers(
                    f.grid,
                    doubleArrayOf(
                        (fd.nx - 2 * fd.pad).toDouble(),
                        (fd.ny - 2 * fd.pad).toDouble(),
                        fd.x0,
                        fd.y0,
                        fd.step,
                    ),
                    "$at grid",
                )
                for ((what, want, got) in listOf(
                    Triple("contour", f.contour, frame.contour.toSvg()),
                    Triple("highlight", f.highlight, frame.highlight.toSvg()),
                    Triple("flow lines", f.flowLines, frame.flowLines.toSvg()),
                )) {
                    total++
                    if (want == got) {
                        exact++
                    } else if (!(engineCut && what == "contour")) {
                        GoldenAssert.path(want, got, "$at $what")
                    }
                }
            }
        }
        assertEquals(B97_CUT_FRAMES, cut.toSet(), "golden frames whose engine field reaches its grid's edge")
        report.append("paths byte-identical: $exact of $total")
        println(report)
    }

    @Test
    fun `the vendored golden's resting button reproduces by a second route`() {
        // research §1.3: the designer's own vectors, button 200×48 key btn/01, t = 5000 and 5125.
        val g =
            gen.instance(
                Slot(
                    ox = 20.0,
                    oy = 20.0,
                    w = 200.0,
                    h = 48.0,
                    r = 12.0,
                    over = 24.0,
                    dripShare = .55,
                    wobble = 1.0,
                    round = false,
                    calm = 1.0,
                    dead = false,
                    marks = true,
                    edge = true,
                    textZone = doubleArrayOf(20.0, 14.0, 160.0, 18.0),
                    cleanZone = doubleArrayOf(18.0, 12.0, 164.0, 22.0),
                ),
                "btn/01",
            )
        GoldenAssert.numbers(
            doubleArrayOf(2.48, 27.7573, 2.4557, 5.9511, 0.0007),
            g.wave.map { round4(it) }.toDoubleArray(),
            "wave",
        )
        assertEquals(25, gen.primitives(g, 5000.0, false).size, "primitives at 5000")
    }

    private fun round4(v: Double) = jsRound(v * 1e4) / 1e4

    private fun instanceOf(golden: GoldenInstance): Instance {
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
        return gen.instance(slot, golden.key, golden.still)
    }

    private fun checkInstance(golden: GoldenInstance) {
        val at = golden.name
        val g = instanceOf(golden)
        GoldenAssert.near(golden.k, g.k, "$at k")
        GoldenAssert.near(golden.bottomRadius, g.bottomRadius, "$at rb")
        GoldenAssert.numbers(golden.wave, g.wave, "$at wave")
        GoldenAssert.numbers(golden.shape, g.shape, "$at shape")
        val drips =
            g.drips.flatMap {
                listOf(
                    it.x,
                    it.halfWidth,
                    it.maxLength,
                    it.restLength,
                    it.period,
                    it.offset,
                    it.u,
                    if (it.drips) 1.0 else 0.0,
                )
            }
        GoldenAssert.numbers(golden.drips, drips.toDoubleArray(), "$at drips")
        val lines =
            g.flowLines.flatMap {
                listOf(
                    if (it.kind == FlowLineKind.Arc) 1.0 else 0.0,
                    it.x,
                    it.y,
                    it.length,
                    it.dx,
                    it.a,
                    it.hook,
                    it.dir,
                    it.flick,
                    it.slide,
                    it.width,
                    it.depth,
                    it.period,
                    it.offset,
                    it.noiseSeed,
                )
            }
        GoldenAssert.numbers(golden.flowLines, lines.toDoubleArray(), "$at flow lines")
        GoldenAssert.numbers(
            golden.mosaic,
            g.mosaic.flatMap { listOf(it.x, it.y, it.size) }.toDoubleArray(),
            "$at mosaic",
        )
        val tiles = g.mosaicTiles.flatMap { listOf(it.x, it.y, it.size, if (it.black) 1.0 else 0.0, it.opacity) }
        GoldenAssert.numbers(golden.mosaicTiles, tiles.toDoubleArray(), "$at mosaic tiles")
        assertEquals(golden.mosaicSide, g.mosaicSide?.ordinal ?: -1, "$at mosaic side")
        GoldenAssert.numbers(golden.deadZone, g.deadZone ?: doubleArrayOf(), "$at dead zone")
        if (g.slot.dead) {
            val dp = gen.deadPixels(g)
            GoldenAssert.numbers(
                golden.deadPixels,
                dp.pixels.flatMap { listOf(it.x, it.y, it.size, it.color.ordinal.toDouble()) }.toDoubleArray(),
                "$at dead pixels",
            )
            // The JS prints the line's x's with toFixed(1).
            assertEquals(golden.deadLine[0], dp.lineX1, .05 + 1e-9, "$at dead line x1")
            assertEquals(golden.deadLine[1], dp.lineX2, .05 + 1e-9, "$at dead line x2")
            GoldenAssert.near(golden.deadLine[2], dp.lineY, "$at dead line y")
        }
    }
}

/**
 * B-97: the golden frames whose field reaches the engine's grid edge. The engine's contour is cut there; the port's
 * grid grows past it and closes the contour, so these contours are the port's own, not the engine's.
 */
internal val B97_CUT_FRAMES: Set<String> = setOf("fab-b @5000.0", "fab-b @12100.0", "fab-b @15990.0")
