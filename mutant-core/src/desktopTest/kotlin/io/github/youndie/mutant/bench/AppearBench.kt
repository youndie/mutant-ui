package io.github.youndie.mutant.bench

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.surface.LocalMutantBuildOffFrame
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.tokens.MutantBody

/**
 * B-50: what the frame costs on the main thread when ten 328 × 80 cards appear in it, their first instances built
 * in that frame (`inFrame`, the behaviour before) or off it (`offFrame`). Per round and arm, interleaved: the frame
 * in which they appear (N), the next (N+1, after 16 ms of wall time, a frame's interval at 60 Hz), and a warm frame
 * five frames on, each as the whole frame (`advanceTimeByFrame`: composition, layout, draw and the test runtime's
 * render). Frame N's draw phase is timed alone too, around the cards' `drawContent`; later frames redraw only the
 * bodies' own nodes, which a parent's `drawContent` does not see. Rounds 2 to 6 are counted.
 * `./gradlew :mutant-core:appearBench --args="<commit>"`, stdout into `logs/b-50/`. Not part of `check`.
 */
private const val ROUNDS = 6
private const val CARDS = 10
private const val FRAME_INTERVAL_MS = 16L

@OptIn(ExperimentalTestApi::class)
private fun round(
    offFrame: Boolean,
    round: Int,
): LongArray {
    val out = LongArray(4)
    runComposeUiTest {
        mainClock.autoAdvance = false
        var shown by mutableStateOf(false)
        var drawNs = 0L
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalMutantBuildOffFrame provides offFrame) {
                ProvideMutantClock(androidx.compose.runtime.remember { MutantClock() }) {
                    Row(
                        Modifier.drawWithContent {
                            val t0 = System.nanoTime()
                            drawContent()
                            drawNs += System.nanoTime() - t0
                        },
                    ) {
                        if (shown) {
                            repeat(2) { c ->
                                Column {
                                    repeat(CARDS / 2) { r ->
                                        MutantSurface(
                                            MutantBody.Card,
                                            "card/r$round/$c$r",
                                            Modifier.size(328.dp, 80.dp),
                                        ) {
                                            BasicText("Кассета 0$r", Modifier.padding(16.dp).mutantCleanZone())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        repeat(5) { mainClock.advanceTimeByFrame() }

        fun frame(): Long {
            drawNs = 0L
            val t0 = System.nanoTime()
            mainClock.advanceTimeByFrame()
            return System.nanoTime() - t0
        }
        shown = true
        out[0] = frame()
        out[1] = drawNs
        Thread.sleep(FRAME_INTERVAL_MS)
        out[2] = frame()
        repeat(4) { mainClock.advanceTimeByFrame() }
        out[3] = frame()
    }
    return out
}

fun main(args: Array<String>) {
    val commit = args.firstOrNull() ?: "unknown"
    val rt = Runtime.getRuntime()
    println(
        "# B-50 appear bench, commit $commit, ${System.getProperty(
            "java.vm.name",
        )} ${System.getProperty("java.version")}, " +
            "${System.getProperty("os.name")} ${System.getProperty("os.arch")}, ${rt.availableProcessors()} cpus",
    )
    val cols = listOf("N frame", "N draw", "N+1 frame", "warm frame")
    println("round arm       " + cols.joinToString("  ") { it.padStart(10) } + "   (us)")
    val counted = mapOf(true to ArrayList<LongArray>(), false to ArrayList())
    for (r in 1..ROUNDS) {
        // Interleaved, the order alternating, so a drift of the machine falls on both arms.
        val arms = if (r % 2 == 0) listOf(true, false) else listOf(false, true)
        for (off in arms) {
            val x = round(off, r)
            val name = if (off) "offFrame" else "inFrame "
            println("$r     $name  " + x.joinToString("  ") { (it / 1000).toString().padStart(10) })
            if (r > 1) counted.getValue(off) += x
        }
    }
    println("median of rounds 2-$ROUNDS")
    for (off in listOf(false, true)) {
        val rows = counted.getValue(off)
        val med = LongArray(4) { i -> rows.map { it[i] }.sorted()[rows.size / 2] }
        println(
            "      ${if (off) "offFrame" else "inFrame "}  " +
                med.joinToString("  ") { (it / 1000).toString().padStart(10) },
        )
    }
}
