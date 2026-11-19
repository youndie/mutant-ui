package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.Foundation.NSHomeDirectory
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import kotlin.time.TimeSource

/**
 * B-138: what a frame costs on iOS, per scene, in the simulator — the Mac's CPU and GPU through it, never a phone's. Per
 * frame: the interval between two frames' starts (this bench's own frame callback) and *work*, from that callback to
 * the end of the main thread's whole frame: a block put on the main queue at the frame's start runs only once the
 * display link's callback, which composes, lays out, draws and encodes the frame, has returned. Scenes rotate each round;
 * round 1 and each scene's first second are not counted. The summary goes to stdout, every frame into the app's
 * `Documents/frames.csv`, then `DONE`.
 *
 * The end of the root's draw is no end of the frame: the root's draw runs only when the root is invalidated, and even
 * redrawn every frame it closed before the nested layers' work in some rounds and not in others (B-138's first two
 * runs). It is kept as a second column, `draw_ns`, to show where it would have been blind. The control is
 * [Scene.Blank] with [CONTROL_MS] of busy-waiting in the draw of a layer nested in a layer, where the scenes' own
 * drawing sits, and the recorder must read it in every round.
 */
@Composable
internal fun IosBench(
    rounds: Int,
    sceneMs: Long,
    only: List<String>?,
) {
    val scenes = only?.map { Scene.valueOf(it) } ?: Scene.entries
    val recorder = remember { Recorder((sceneMs - SETTLE_MS) / 1e3) }
    var current by remember { mutableStateOf<Pair<Int, Scene>?>(null) }
    LaunchedEffect(Unit) {
        println("B-138 iOS bench: rounds $rounds, scene ${sceneMs}ms, settle ${SETTLE_MS}ms, control ${CONTROL_MS}ms")
        for (round in 1..rounds) {
            for (scene in scenes) {
                current = round to scene
                recorder.begin(round, scene)
                delay(SETTLE_MS)
                recorder.counting = true
                delay(sceneMs - SETTLE_MS)
                recorder.counting = false
                println(recorder.line(round, scene))
            }
        }
        current = null
        recorder.summary(scenes, rounds)
        recorder.writeFrames()
        println("DONE")
    }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { recorder.frameStart() }
        }
    }
    Box(
        Modifier.fillMaxSize().drawWithContent {
            drawContent()
            recorder.drawEnd()
        },
    ) {
        current?.let { (round, scene) -> key(round, scene) { SceneContent(scene) } }
    }
}

private const val SETTLE_MS = 1_000L
private const val CONTROL_MS = 10L

private enum class Scene {
    TenCards8,
    Blank,
    ComponentsRest,
    ComponentsClean,
    ComponentsReduce,
    DemoPage,
    DemoLibrary,
    DemoRecording,
    DemoSettings,
    Control,
}

@Composable
private fun SceneContent(scene: Scene) {
    val strings = remember { SampleStrings(SampleLanguage.English) }
    when (scene) {
        Scene.TenCards8 -> {
            TenCards8()
        }

        Scene.Blank -> {
            TenCards8(cards = false)
        }

        Scene.Control -> {
            TenCards8(cards = false, burnMs = CONTROL_MS)
        }

        Scene.ComponentsRest -> {
            MutantUiSample(remember { SampleSettings() }, rememberScrollState())
        }

        Scene.ComponentsClean -> {
            MutantUiSample(remember { SampleSettings(cleanSignal = true) }, rememberScrollState())
        }

        Scene.ComponentsReduce -> {
            MutantUiSample(remember { SampleSettings(reduceMotion = true) }, rememberScrollState())
        }

        Scene.DemoPage -> {
            MutantUiSample(remember { SampleSettings(page = SamplePage.Demo) }, rememberScrollState())
        }

        Scene.DemoLibrary, Scene.DemoRecording, Scene.DemoSettings -> {
            MutantTheme {
                Box(Modifier.fillMaxSize().background(MutantTheme.colors.page).padding(24.dp)) {
                    DemoScreenFrame(
                        when (scene) {
                            Scene.DemoLibrary -> DemoScreen.Library
                            Scene.DemoRecording -> DemoScreen.Recording
                            else -> DemoScreen.Settings
                        },
                        strings,
                    )
                }
            }
        }
    }
}

/** bench-android's `TenCards8`, as the browser's B-108 scene composes it: every frame drawn, on the theme's clock. */
@Composable
private fun TenCards8(
    cards: Boolean = true,
    burnMs: Long = 0,
) {
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { frame = it } }
    val read = { frame }
    MutantTheme(reduceMotion = false) {
        Box(Modifier.fillMaxSize().background(MutantTheme.colors.background).mutantTape()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (cards) {
                    repeat(10) { i ->
                        MutantCard(Modifier.size(328.dp, 80.dp), key = "card/b49/$i") {
                            MutantCardTitle("Кассета ${(i + 1).toString().padStart(2, '0')}")
                            MutantCardSubtitle("Запись ${12 + i}:0$i")
                        }
                    }
                }
            }
            Box(Modifier.fillMaxSize().graphicsLayer { }) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer { }.drawBehind {
                        read()
                        if (burnMs > 0) busyWait(burnMs)
                    },
                )
            }
        }
    }
}

private val origin = TimeSource.Monotonic.markNow()

private fun nowNs(): Long = origin.elapsedNow().inWholeNanoseconds

private fun busyWait(ms: Long) {
    val until = nowNs() + ms * 1_000_000
    @Suppress("ControlFlowWithEmptyBody")
    while (nowNs() < until) {
    }
}

private class Frame(
    val round: Int,
    val scene: Scene,
    val startNs: Long,
    val intervalNs: Long,
    val workNs: Long,
    val drawNs: Long,
)

/** Every call on the main thread. */
private class Recorder(
    private val countedSeconds: Double,
) {
    var counting = false
    private var round = 0
    private var scene = Scene.Blank
    private var start = -1L
    private var drawn = -1L
    private var lastStart = -1L
    val frames = ArrayList<Frame>()

    fun begin(
        round: Int,
        scene: Scene,
    ) {
        this.round = round
        this.scene = scene
        lastStart = -1
        start = -1
    }

    fun frameStart() {
        val begun = nowNs()
        start = begun
        drawn = -1
        dispatch_async(dispatch_get_main_queue()) { frameEnd(begun) }
    }

    fun drawEnd() {
        if (start >= 0) drawn = nowNs() - start
    }

    private fun frameEnd(begun: Long) {
        val end = nowNs()
        // Every frame is kept, a late one too; only the draw column needs the next frame not to have begun.
        if (counting) {
            val draw = if (begun == start) drawn else -1
            frames += Frame(round, scene, begun, if (lastStart < 0) -1 else begun - lastStart, end - begun, draw)
        }
        lastStart = begun
    }

    fun line(
        round: Int,
        scene: Scene,
    ): String {
        val mine = frames.filter { it.round == round && it.scene == scene }
        return "round $round ${scene.name}: ${row(mine)}"
    }

    fun summary(
        scenes: List<Scene>,
        rounds: Int,
    ) {
        println("pooled, rounds 2..$rounds:")
        println("scene | frames/s | work p50 | work p95 | interval p50 | interval p95 | root draw p50 (ms)")
        for (s in scenes) println("${s.name} | ${row(frames.filter { it.scene == s && it.round > 1 }, rounds - 1)}")
    }

    private fun row(
        mine: List<Frame>,
        roundsPooled: Int = 1,
    ): String {
        if (mine.isEmpty()) return "idle (no frame drawn)"
        val seconds = roundsPooled * countedSeconds
        val work = mine.map { it.workNs / 1e6 }.sorted()
        val interval = mine.mapNotNull { f -> f.intervalNs.takeIf { it > 0 }?.let { it / 1e6 } }.sorted()
        val draw = mine.mapNotNull { f -> f.drawNs.takeIf { it >= 0 }?.let { it / 1e6 } }.sorted()
        return "${fmt(mine.size / seconds)} | ${fmt(q(work, .5))} | ${fmt(q(work, .95))} | " +
            "${fmt(q(interval, .5))} | ${fmt(q(interval, .95))} | ${fmt(q(draw, .5))}"
    }

    @OptIn(ExperimentalForeignApi::class)
    fun writeFrames() {
        val path = NSHomeDirectory() + "/Documents/frames.csv"
        val f = fopen(path, "w") ?: return
        fputs("round,scene,start_ns,interval_ns,work_ns,draw_ns\n", f)
        for (fr in frames) {
            fputs(
                "${fr.round},${fr.scene},${fr.startNs},${fr.intervalNs},${fr.workNs},${fr.drawNs}\n",
                f,
            )
        }
        fclose(f)
        println("frames: $path")
    }
}

private fun q(
    sorted: List<Double>,
    p: Double,
): Double = if (sorted.isEmpty()) Double.NaN else sorted[((sorted.size - 1) * p).toInt()]

private fun fmt(v: Double): String = if (v.isNaN()) "-" else ((v * 10).toLong() / 10.0).toString()
