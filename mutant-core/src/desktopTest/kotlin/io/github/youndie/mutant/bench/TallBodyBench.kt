package io.github.youndie.mutant.bench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.surface.LocalMutantFrameProbe
import io.github.youndie.mutant.surface.LocalMutantRasterProbe
import io.github.youndie.mutant.theme.MutantTheme
import java.lang.management.ManagementFactory
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.system.exitProcess

/**
 * B-69: what a tall body costs per tick in a real window. One resting card 360 dp wide and 200, 1 000 or 3 000 dp
 * tall stands in a scrolling column on a running clock; a blank page is the control. Per scene: the whole process's
 * CPU a second (every thread: the frame, the builds ahead, the rasterising, the render thread), the ticks the body
 * built, the render thread's CPU a frame and the main thread's busy share. A tick's cost is the process CPU a second
 * above the blank page's, divided by the ticks built that second. Scenes rotate each round; round 1 and each scene's
 * first second are not counted.
 *
 * `./gradlew :mutant-core:tallBodyBench --args="<commit> [rounds] [scene-ms]"`, stdout into `logs/b-69/`.
 */
private var rounds = 6
private var sceneMs = 8_000L
private const val SETTLE_MS = 1_000L
private const val RENDER_THREAD = "skiko-dispatcher-to-block-on"

private val heights = listOf(0, 200, 1_000, 3_000)

private fun sceneName(h: Int) = if (h == 0) "Blank" else "Card$h"

@Volatile private var frames = 0

private val built =
    java.util.concurrent.atomic
        .AtomicInteger()
private val rasterNs =
    java.util.concurrent.atomic
        .AtomicLong()

private val mainBusyNs =
    java.util.concurrent.atomic
        .AtomicLong()

private class TallTimingQueue : java.awt.EventQueue() {
    override fun dispatchEvent(event: java.awt.AWTEvent) {
        val t0 = System.nanoTime()
        try {
            super.dispatchEvent(event)
        } finally {
            mainBusyNs.addAndGet(System.nanoTime() - t0)
        }
    }
}

/** The render thread's CPU, as B-76's bench reads it. */
private object TallRenderThread {
    private val bean = ManagementFactory.getThreadMXBean()
    private val last = HashMap<Long, Long>()

    @Volatile var cumulativeNs = 0L
        private set

    fun start() {
        Thread({
            while (true) {
                for (info in bean.getThreadInfo(bean.allThreadIds)) {
                    if (info == null || info.threadName != RENDER_THREAD) continue
                    val cpu = bean.getThreadCpuTime(info.threadId)
                    if (cpu >= 0) last[info.threadId] = cpu
                }
                cumulativeNs = last.values.sum()
                Thread.sleep(5)
            }
        }, "b69-render-sampler").apply { isDaemon = true }.start()
    }
}

private val os = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean

@Composable
private fun TallTicker() {
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
private fun TallScene(h: Int) {
    MutantTheme {
        CompositionLocalProvider(
            // A frame drawn from a tick's build: in the frame or ahead of it.
            LocalMutantFrameProbe provides { _, _, _ -> built.incrementAndGet() },
            LocalMutantRasterProbe provides { ns, _ -> rasterNs.addAndGet(ns) },
        ) {
            Box(Modifier.fillMaxSize().background(MutantTheme.colors.page)) {
                if (h > 0) {
                    Box(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                        MutantCard(Modifier.size(360.dp, h.dp), key = "tall$h") {
                            MutantCardTitle("Untitled")
                            MutantCardSubtitle("Side A")
                        }
                    }
                }
                TallTicker()
            }
        }
    }
}

private fun f2t(v: Double) = "%.2f".format(v)

private class TallReading(
    val frames: Int,
    val built: Int,
    val cpuNs: Long,
    val renderNs: Long,
    val rasterNs: Long,
    val mainNs: Long,
    val wallNs: Long,
) {
    val seconds get() = wallNs / 1e9
    val cpuMsPerSecond get() = cpuNs / 1e6 / seconds
    val builtPerSecond get() = built / seconds
}

fun main(args: Array<String>) {
    val commit = args.getOrNull(0) ?: "unknown"
    args.getOrNull(1)?.let { rounds = it.toInt() }
    args.getOrNull(2)?.let { sceneMs = it.toLong() }
    java.awt.Toolkit
        .getDefaultToolkit()
        .systemEventQueue
        .push(TallTimingQueue())
    TallRenderThread.start()
    var show: (Int) -> Unit = {}
    lateinit var window: ComposeWindow
    SwingUtilities.invokeAndWait {
        window = ComposeWindow()
        window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        window.title = "mutant-ui B-69"
        window.isAlwaysOnTop = true
        window.setSize(600, 900)
        window.setLocation(40, 40)
        window.setContent {
            var index by remember { mutableStateOf(0) }
            show = { index = it }
            key(index) { TallScene(heights[index]) }
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
    println("# B-69 tall body bench")
    println("# commit $commit")
    println("# host ${sh("sysctl", "-n", "hw.model")}, cpu ${sh("sysctl", "-n", "machdep.cpu.brand_string")}")
    println(
        "# os ${System.getProperty(
            "os.name",
        )} ${System.getProperty("os.version")}, jvm ${System.getProperty("java.runtime.version")}",
    )
    println(
        "# window ${window.width}x${window.height}, rounds $rounds (round 1 not counted), scene ${sceneMs / 1000} s",
    )
    val readings = HashMap<Pair<Int, Int>, TallReading>()
    for (round in 1..rounds) {
        for (i in heights.indices) {
            val index = (i + round - 1) % heights.size
            SwingUtilities.invokeAndWait { show(index) }
            Thread.sleep(SETTLE_MS)
            val f0 = frames
            val b0 = built.get()
            val c0 = os.processCpuTime
            val r0 = TallRenderThread.cumulativeNs
            val x0 = rasterNs.get()
            val m0 = mainBusyNs.get()
            val t0 = System.nanoTime()
            Thread.sleep(sceneMs)
            val reading =
                TallReading(
                    frames - f0,
                    built.get() - b0,
                    os.processCpuTime - c0,
                    TallRenderThread.cumulativeNs - r0,
                    rasterNs.get() - x0,
                    mainBusyNs.get() - m0,
                    System.nanoTime() - t0,
                )
            readings[round to heights[index]] = reading
            println(
                "round $round ${sceneName(heights[index])}: ${reading.frames} frames, ${reading.built} body frames, " +
                    "cpu ${f2t(
                        reading.cpuMsPerSecond,
                    )} ms/s, render ${f2t(reading.renderNs / 1e6 / reading.frames)} ms/frame, " +
                    "raster ${f2t(reading.rasterNs / 1e6 / reading.seconds)} ms/s, " +
                    "main busy ${"%.0f".format(100.0 * reading.mainNs / reading.wallNs)} %",
            )
        }
    }
    println()
    println(
        "scene      cpu ms/s  above blank  body frames/s  cost per tick ms   (counted rounds 2..$rounds, mean; per round)",
    )
    val counted = 2..rounds
    for (h in heights) {
        val rs = counted.mapNotNull { readings[it to h] }
        val perTick =
            counted.mapNotNull { r ->
                val a = readings[r to h] ?: return@mapNotNull null
                val blank = readings[r to 0] ?: return@mapNotNull null
                if (h == 0 || a.built == 0) null else (a.cpuMsPerSecond - blank.cpuMsPerSecond) / a.builtPerSecond
            }
        val blankMean = counted.mapNotNull { readings[it to 0] }.map { it.cpuMsPerSecond }.average()
        println(
            "%-10s %8s  %11s  %13s  %16s   %s".format(
                sceneName(h),
                f2t(rs.map { it.cpuMsPerSecond }.average()),
                if (h == 0) "" else f2t(rs.map { it.cpuMsPerSecond }.average() - blankMean),
                f2t(rs.map { it.builtPerSecond }.average()),
                if (perTick.isEmpty()) "" else f2t(perTick.average()),
                perTick.joinToString(" ") { f2t(it) },
            ),
        )
    }
    SwingUtilities.invokeAndWait { window.dispose() }
    exitProcess(0)
}
