package io.github.youndie.mutant.bench

import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.gen.goldens.GoldenInstance
import io.github.youndie.mutant.gen.goldens.genGoldens

/**
 * B-06: the cost of the CPU field on the desktop JVM, by the protocol pre-registered in the item.
 * `./gradlew :mutant-core:fieldBench --args="<commit>"`, stdout into `logs/b-06/`. Not part of `check`.
 *
 * Arms per case, interleaved within a round: `instance` (`genItem`), `warm` (a frame with the instance's
 * field cache kept, t stepping a tick per call), `warm2` (the same again: the ruler), `hot` (heat 1, the
 * hot tick), `cold` (a frame with a fresh cache). The control: the card's warm frame plus a busy-wait of
 * exactly 200 µs. Every output is folded into a checksum, printed per round.
 */
private const val CALLS = 200
private const val ROUNDS = 5
private const val WARMUP_NS = 3_000_000_000L
private const val CONTROL_NS = 200_000L

private var checksum = 0L

private fun slotOf(g: GoldenInstance): Slot {
    val s = g.slot
    return Slot(
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
}

private class Case(
    val golden: GoldenInstance,
) {
    val gen = MutantGenerator(cassette = "5")
    val slot = slotOf(golden)
    val instance: Instance = gen.instance(slot, golden.key, golden.still)
    val cache = FieldCache()
    var tick = 0
}

private fun fold(f: io.github.youndie.mutant.gen.BodyFrame) {
    checksum += f.contour.loops.sumOf { it.size } + f.highlight.loops.size + f.flowLines.loops.size
    checksum += f.field.values[f.field.values.size / 2].toRawBits()
}

private fun spin(ns: Long) {
    val end = System.nanoTime() + ns
    var n = 0L
    while (System.nanoTime() < end) n++
    checksum += n and 1L
}

private val arms: List<Pair<String, (Case) -> Unit>> =
    listOf(
        "instance" to
            { c ->
                c.gen.instance(c.slot, c.golden.key, c.golden.still).also {
                    checksum +=
                        it.drips.size + it.flowLines.size
                }
            },
        "warm" to { c -> fold(c.gen.frame(c.instance, c.cache, 5000.0 + 125.0 * (c.tick++ % 64))) },
        "warm2" to { c -> fold(c.gen.frame(c.instance, c.cache, 5000.0 + 125.0 * (c.tick++ % 64))) },
        "hot" to { c -> fold(c.gen.frame(c.instance, c.cache, 9340.0 + 83.333 * (c.tick++ % 64), heat = 1.0)) },
        "cold" to { c -> fold(c.gen.frame(c.instance, FieldCache(), 5000.0)) },
    )

private fun medianMicros(
    case: Case,
    arm: (Case) -> Unit,
): Double {
    val ns = LongArray(CALLS)
    for (i in 0 until CALLS) {
        val t0 = System.nanoTime()
        arm(case)
        ns[i] = System.nanoTime() - t0
    }
    ns.sort()
    return ns[CALLS / 2] / 1000.0
}

fun main(args: Array<String>) {
    val commit = args.firstOrNull() ?: "unknown"
    val rt = Runtime.getRuntime()
    println("# B-06 field bench")
    println("# commit $commit")
    println(
        "# host ${System.getProperty(
            "os.name",
        )} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}, cpus ${rt.availableProcessors()}",
    )
    println(
        "# jvm ${System.getProperty(
            "java.vm.name",
        )} ${System.getProperty("java.runtime.version")}, max heap ${rt.maxMemory() / 1_048_576} MiB",
    )
    println(
        "# calls $CALLS per arm per round, rounds $ROUNDS (round 1 discarded), warm-up ${WARMUP_NS / 1_000_000_000} s per case",
    )
    val cases = genGoldens.map { Case(it) }
    for (c in cases) {
        val end = System.nanoTime() + WARMUP_NS
        while (System.nanoTime() < end) for ((_, arm) in arms) arm(c)
    }
    val card = cases.single { it.golden.name == "card-a" }
    val control: (Case) -> Unit = { c ->
        fold(c.gen.frame(c.instance, c.cache, 5000.0 + 125.0 * (c.tick++ % 64)))
        spin(CONTROL_NS)
    }
    for (round in 1..ROUNDS) {
        for (c in cases) {
            val cells = arms.map { (name, arm) -> name to medianMicros(c, arm) }
            println("round $round ${c.golden.name} " + cells.joinToString(" ") { (n, v) -> "$n=${"%.1f".format(v)}" })
        }
        val warm = medianMicros(card, arms[1].second)
        val ctl = medianMicros(card, control)
        println(
            "round $round control card-a warm=${"%.1f".format(
                warm,
            )} warm+200us=${"%.1f".format(ctl)} delta=${"%.1f".format(ctl - warm)}",
        )
        println("round $round checksum $checksum")
    }
}
