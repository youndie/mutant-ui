package io.github.youndie.mutant.bench

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.LocalMutantTapeSkip
import io.github.youndie.mutant.surface.TapePart
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantTheme
import java.lang.management.ManagementFactory
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.system.exitProcess

/**
 * B-77: what each part of the Tape costs Skia's render thread. A real window draws the Demo page's four Tapes — one
 * over the window, one over each of three 360 × 720 dp screens — over plain ground, and one part at a time is left out
 * of every Tape; one Tape alone over the window beside them. The render thread's CPU per frame is read as B-76's bench
 * reads it, with its control: a fixed load of tessellated paths must add the same on a blank page and over the Tapes.
 * Scenes rotate each round; round 1 and each scene's first second are not counted.
 *
 * `./gradlew :mutant-core:tapeRenderBench --args="<commit> [rounds] [scene-ms] [TapeScene,TapeScene]"`, stdout into `logs/b-77/`.
 */
private var rounds = 6
private var sceneMs = 8_000L
private const val SETTLE_MS = 1_000L
private const val RENDER_THREAD = "skiko-dispatcher-to-block-on"
private const val LOAD_PATHS = 1_200
private const val LOAD_POINTS = 120

private class TapeScene(
    val name: String,
    val tapes: Int,
    val load: Boolean = false,
    val skip: Set<TapePart> = emptySet(),
)

private var scenes: List<TapeScene> = emptyList()

private val allScenes: List<TapeScene> =
    listOf(
        TapeScene("Blank", tapes = 0),
        TapeScene("BlankLoad", tapes = 0, load = true),
        TapeScene("Tape1", tapes = 1),
        TapeScene("Tape4", tapes = 4),
        TapeScene("Tape4Load", tapes = 4, load = true),
    ) + TapePart.entries.map { TapeScene("Tape4No$it", tapes = 4, skip = setOf(it)) }

/** The CPU time of every render thread seen, a finished one at its last reading; sampled every 5 ms. */
private object TapeRenderThread {
    private val bean = ManagementFactory.getThreadMXBean()
    private val last = HashMap<Long, Long>()

    @Volatile var cumulativeNs = 0L
        private set

    @Volatile var threadsSeen = 0
        private set

    fun start() {
        Thread({
            while (true) {
                for (info in bean.getThreadInfo(bean.allThreadIds)) {
                    if (info == null || info.threadName != RENDER_THREAD) continue
                    val cpu = bean.getThreadCpuTime(info.threadId)
                    if (cpu >= 0) last[info.threadId] = cpu
                }
                threadsSeen = last.size
                cumulativeNs = last.values.sum()
                Thread.sleep(5)
            }
        }, "b77-render-sampler").apply { isDaemon = true }.start()
    }
}

private class TapeReading(
    val frames: Int,
    val renderNs: Long,
    val wallNs: Long,
) {
    val perFrameMs get() = renderNs / 1e6 / frames
    val fps get() = frames / (wallNs / 1e9)
}

@Volatile private var frames = 0

@Composable
private fun Ticker() {
    var n by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                n++
                frames++
            }
        }
    }
    Box(Modifier.size(1.dp).drawBehind { drawRect(if (n % 2 == 0) Color.Black else Color.White) })
}

@Composable
private fun Load() {
    val paths =
        remember {
            val rnd = java.util.Random(76)
            List(LOAD_PATHS) {
                Path().apply {
                    val cx = 40f + rnd.nextFloat() * 1180
                    val cy = 40f + rnd.nextFloat() * 700
                    moveTo(cx, cy)
                    repeat(LOAD_POINTS) { lineTo(cx + rnd.nextFloat() * 80 - 40, cy + rnd.nextFloat() * 80 - 40) }
                    close()
                }
            }
        }
    Canvas(Modifier.fillMaxSize()) { for (p in paths) drawPath(p, Color(0x33FFAA00)) }
}

@Composable
private fun SceneContent(scene: TapeScene) {
    MutantTheme {
        CompositionLocalProvider(LocalMutantTapeSkip provides scene.skip) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MutantTheme.colors.page)
                    .then(if (scene.tapes > 0) Modifier.mutantTape() else Modifier),
            ) {
                if (scene.tapes > 1) {
                    Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        repeat(scene.tapes - 1) {
                            Box(Modifier.size(360.dp, 720.dp).background(MutantTheme.colors.background).mutantTape())
                        }
                    }
                }
                Ticker()
                if (scene.load) Load()
            }
        }
    }
}

private fun f2(v: Double) = "%.2f".format(v)

fun main(args: Array<String>) {
    val commit = args.getOrNull(0) ?: "unknown"
    args.getOrNull(1)?.let { rounds = it.toInt() }
    args.getOrNull(2)?.let { sceneMs = it.toLong() }
    val only = args.getOrNull(3)?.split(',')
    scenes = allScenes.filter { only == null || it.name in only }
    TapeRenderThread.start()
    var show: (Int) -> Unit = {}
    lateinit var window: ComposeWindow
    SwingUtilities.invokeAndWait {
        window = ComposeWindow()
        window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        window.title = "mutant-ui B-77"
        window.isAlwaysOnTop = true
        window.setSize(1280, 900)
        window.setLocation(40, 40)
        window.setContent {
            var index by remember { mutableStateOf(0) }
            show = { index = it }
            key(index) { SceneContent(scenes[index]) }
        }
        window.isVisible = true
    }
    Thread.sleep(2_000)

    fun sh(vararg cmd: String) =
        ProcessBuilder(*cmd)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
    println("# B-77 Tape render bench")
    println("# commit $commit")
    println("# host ${sh("sysctl", "-n", "hw.model")}, cpu ${sh("sysctl", "-n", "machdep.cpu.brand_string")}")
    println(
        "# os ${System.getProperty(
            "os.name",
        )} ${System.getProperty("os.version")}, jvm ${System.getProperty("java.runtime.version")}",
    )
    println("# power ${sh("pmset", "-g", "batt").lines().lastOrNull()?.trim()}")
    println(
        "# window ${window.width}x${window.height}, rounds $rounds (round 1 not counted), scene ${sceneMs / 1000} s",
    )
    val readings = HashMap<Pair<Int, String>, TapeReading>()
    for (round in 1..rounds) {
        for (i in scenes.indices) {
            val index = (i + round - 1) % scenes.size
            SwingUtilities.invokeAndWait { show(index) }
            Thread.sleep(SETTLE_MS)
            val f0 = frames
            val r0 = TapeRenderThread.cumulativeNs
            val t0 = System.nanoTime()
            Thread.sleep(sceneMs)
            val reading = TapeReading(frames - f0, TapeRenderThread.cumulativeNs - r0, System.nanoTime() - t0)
            readings[round to scenes[index].name] = reading
            println(
                "round $round ${scenes[index].name}: ${reading.frames} frames, ${f2(
                    reading.perFrameMs,
                )} ms render/frame",
            )
        }
    }
    println()
    println("scene            frames/s  render/frame  vs Tape4   (counted rounds 2..$rounds, mean; per round)")
    val counted = 2..rounds

    fun of(name: String) = counted.mapNotNull { readings[it to name] }
    val four = of("Tape4").map { it.perFrameMs }.average()
    for (s in scenes) {
        val rs = of(s.name)
        val per = rs.map { it.perFrameMs }
        val mean = per.average()
        println(
            "%-16s %8.1f  %10s ms  %8s ms   %s".format(
                s.name,
                rs.map { it.fps }.average(),
                f2(mean),
                if (s.tapes == 4) f2(mean - four) else "",
                per.joinToString(" ") { f2(it) },
            ),
        )
    }

    fun added(
        with: String,
        without: String,
    ) = counted.mapNotNull { r ->
        val a = readings[r to with] ?: return@mapNotNull null
        val b = readings[r to without] ?: return@mapNotNull null
        a.perFrameMs - b.perFrameMs
    }
    println()
    println("control, what the load adds per frame per counted round:")
    println("  on a blank page    ${added("BlankLoad", "Blank").joinToString { f2(it) }} ms")
    println("  over the Tapes     ${added("Tape4Load", "Tape4").joinToString { f2(it) }} ms")
    println("render threads seen: ${TapeRenderThread.threadsSeen}")
    SwingUtilities.invokeAndWait { window.dispose() }
    exitProcess(0)
}
