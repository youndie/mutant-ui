package io.github.youndie.mutant.bench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.gen.moshPlan
import io.github.youndie.mutant.surface.KeyframeLossBody
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.system.exitProcess

/**
 * B-88: what a step of the keyframe loss costs on the JVM. A Demo screen's size (360 × 720 dp at 2×), eight cards
 * filling it; the loss frozen at 95 % of its run renders every step in one frame, at 0.1 % one step, and the
 * difference over the steps between is a step's cost. Arms alternate, round 1 not counted. Headless, Skia's raster.
 *
 * `./gradlew :mutant-core:keyframeLossBench --args="<commit> [rounds]"`, stdout into `logs/b-88/`.
 */
fun main(args: Array<String>) {
    val commit = args.getOrNull(0) ?: "?"
    val rounds = args.getOrNull(1)?.toInt() ?: 12
    val plan = moshPlan("5", 1, 360, 720)
    val steps = (0.95 * plan.durationMs / 125).toInt()
    println("# B-88 keyframe loss bench, commit $commit")
    println(
        "# ${System.getProperty(
            "os.name",
        )} ${System.getProperty(
            "os.version",
        )}, ${System.getProperty("java.vm.name")} ${System.getProperty("java.version")}",
    )
    println(
        "# 360 x 720 dp at 2x, loss 1 of seed 5: ${plan.rows} rows, ${plan.runs.size} blocks, ${steps + 1} steps at 95 %",
    )
    val perStep = ArrayList<Double>()
    for (round in 1..rounds) {
        val one = renderMs(0.001)
        val all = renderMs(0.95)
        val step = (all - one) / steps
        println(
            "round $round: one step ${"%.1f".format(
                one,
            )} ms, ${steps + 1} steps ${"%.1f".format(all)} ms, a step ${"%.2f".format(step)} ms",
        )
        if (round > 1) perStep += step
    }
    perStep.sort()
    println(
        "# a step, counted rounds: median ${"%.2f".format(
            perStep[perStep.size / 2],
        )} ms, min ${"%.2f".format(perStep.first())}, max ${"%.2f".format(perStep.last())}",
    )
    exitProcess(0)
}

/** The time of the frame that starts a loss frozen at [p]: its snapshot and every step up to [p]. */
private fun renderMs(p: Double): Double {
    val scene =
        ImageComposeScene(720, 1_440, Density(2f)) {
            MutantTheme {
                ProvideMutantClock(MutantClock(startMs = 1_000.0, frozen = true)) {
                    KeyframeLossBody(Modifier.fillMaxSize(), MutantTheme.colors.page, p, {
                        Column(
                            Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            repeat(8) {
                                MutantSurface(MutantBody.Card, "card$it", Modifier.fillMaxWidth().height(72.dp)) {}
                            }
                        }
                    })
                }
            }
        }
    // The bodies draw first; the loss starts once they have (`startFrozen` waits a frame or more for them).
    var t = 0L
    var ms = 0.0
    repeat(6) {
        val t0 = System.nanoTime()
        scene.render(t)
        val dt = (System.nanoTime() - t0) / 1e6
        if (dt > ms) ms = dt
        t += 16_000_000
    }
    scene.close()
    return ms
}
