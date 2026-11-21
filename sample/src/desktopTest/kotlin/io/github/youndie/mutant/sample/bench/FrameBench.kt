package io.github.youndie.mutant.sample.bench

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.sample.DemoScreen
import io.github.youndie.mutant.sample.DemoScreenFrame
import io.github.youndie.mutant.sample.MutantUiSample
import io.github.youndie.mutant.sample.SampleLanguage
import io.github.youndie.mutant.sample.SampleSettings
import io.github.youndie.mutant.sample.SampleStrings
import io.github.youndie.mutant.theme.MutantTheme
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.AWTEvent
import java.awt.Container
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.system.exitProcess

/**
 * B-72: what a frame of the sample costs on the JVM, in a real window on this Mac, per scene. Per frame it reads
 * the interval between two frames' starts, Compose's whole frame around the window's [SkikoRenderDelegate.onRender]
 * (frame callbacks, recomposition, layout, draw: *work*), and the AWT event that carried it (work plus Skia's flush
 * and the submit where they run on the event thread). Scenes rotate each round; round 1 and each scene's first second
 * are not counted. The control is the resting page with [CONTROL_MS] of busy-waiting added inside the measured span
 * of every frame, and the recorder must read it. A scene that asks for no frame (reduce motion stops the clock) is
 * reported as idle.
 *
 * `./gradlew :sample:frameBench --args="<commit> <out-dir> [rounds] [scene-ms] [Scene,Scene]"`: the summary on stdout, every frame into
 * `<out-dir>/frames.csv`. Not part of `check`; it opens a window on top for about ten minutes.
 */
private var rounds = 6
private var sceneMs = 10_000L
private const val SETTLE_MS = 1_000L
private const val CONTROL_MS = 10L
private const val WHEEL_EVERY_MS = 32L
private const val WHEEL_FLIP_MS = 2_500L

private enum class Scene(
    val wheel: Boolean = false,
) {
    ComponentsRest,
    ComponentsWheel(wheel = true),
    DemoPage,
    DemoLibrary,
    DemoRecording,
    DemoSettings,
    ComponentsClean,
    ComponentsReduce,
    Control,

    // B-76: the render thread. A blank page and the Demo page each with and without a fixed drawing load (the
    // control: what the load adds to a blank page it must add to the Demo page), and the Demo page without the Tape.
    Blank,
    BlankLoad,
    DemoPageTicked,
    DemoPageLoad,
    DemoPageClean,

    // The three screens alone in a row, no header and no page Tape: with the screens' rounded clip and without.
    DemoRow,
    DemoRowNoClip,
}

private class Frame(
    val startNs: Long,
    val workNs: Long,
    var eventNs: Long = -1,
    /** The render thread's CPU time when this frame started, so the next frame's minus it is this interval's (B-76). */
    val renderCpuNs: Long = -1,
)

/**
 * Skiko replays the frame's picture on a thread of its own, and its dispatcher may replace that thread. A sampler
 * on a thread of the bench's own adds up, every [SAMPLE_MS], the CPU time of every thread of that name it has seen,
 * a finished one at its last reading (B-76).
 */
private object RenderThread {
    private val bean =
        java.lang.management.ManagementFactory
            .getThreadMXBean()
    private val last = HashMap<Long, Long>()

    @Volatile var cumulativeNs = 0L
        private set

    @Volatile var threadsSeen = 0
        private set

    fun start() {
        Thread({
            while (true) {
                val ids = bean.allThreadIds
                for (info in bean.getThreadInfo(ids)) {
                    if (info == null || info.threadName != RENDER_THREAD) continue
                    val cpu = bean.getThreadCpuTime(info.threadId)
                    if (cpu >= 0) last[info.threadId] = cpu
                }
                threadsSeen = last.size
                cumulativeNs = last.values.sum()
                Thread.sleep(SAMPLE_MS)
            }
        }, "b76-render-sampler").apply { isDaemon = true }.start()
    }

    fun cpuNs(): Long = if (threadsSeen == 0) -1 else cumulativeNs
}

private const val SAMPLE_MS = 5L

private const val RENDER_THREAD = "skiko-dispatcher-to-block-on"

/** What the event thread writes and the driver reads; every write happens on the event thread. */
private class Recorder {
    @Volatile var scene: Scene? = null

    @Volatile var counting = false
    val frames = ArrayList<Frame>()
    private var inEvent = false
    private var eventFrame: Frame? = null

    fun rendered(
        start: Long,
        end: Long,
    ) {
        if (!counting) return
        val f = Frame(start, end - start, renderCpuNs = RenderThread.cpuNs())
        frames += f
        if (inEvent) eventFrame = f
    }

    fun eventStarted() {
        inEvent = true
        eventFrame = null
    }

    fun eventEnded(ns: Long) {
        inEvent = false
        eventFrame?.eventNs = ns
        eventFrame = null
    }
}

private val recorder = Recorder()

/** Times every AWT event, so the one that carried a frame is known with its flush. */
private class TimingQueue : EventQueue() {
    override fun dispatchEvent(event: AWTEvent) {
        recorder.eventStarted()
        val t0 = System.nanoTime()
        try {
            super.dispatchEvent(event)
        } finally {
            recorder.eventEnded(System.nanoTime() - t0)
        }
    }
}

/** A monotonic millisecond: the bench paces itself and stamps its wheel events by it, never by the wall clock. */
private fun nowMs() = System.nanoTime() / 1_000_000

private fun busyWait(ms: Long) {
    val until = System.nanoTime() + ms * 1_000_000
    while (System.nanoTime() < until) Thread.onSpinWait()
}

/**
 * B-148: the Components scenes show the page's default group, Foundations, alone, and the Demo page scenes its default
 * screen, Library, alone.
 */
@Composable
private fun SceneContent(
    scene: Scene,
    scroll: ScrollState,
) {
    val strings = remember { SampleStrings(SampleLanguage.English) }
    when (scene) {
        Scene.ComponentsRest, Scene.ComponentsWheel -> {
            MutantUiSample(remember { SampleSettings() }, scroll)
        }

        Scene.ComponentsClean -> {
            MutantUiSample(remember { SampleSettings(cleanSignal = true) }, scroll)
        }

        Scene.ComponentsReduce -> {
            MutantUiSample(remember { SampleSettings(reduceMotion = true) }, scroll)
        }

        Scene.DemoPage -> {
            MutantUiSample(remember { SampleSettings(page = io.github.youndie.mutant.sample.SamplePage.Demo) }, scroll)
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

        // The resting page, the busy-wait added inside the measured span by the render delegate.
        Scene.Control -> {
            MutantUiSample(remember { SampleSettings() }, scroll)
        }

        Scene.Blank, Scene.BlankLoad -> {
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics
                        .Color(0xFF10142A),
                ),
            ) {
                Ticker()
                if (scene == Scene.BlankLoad) DrawingLoad()
            }
        }

        Scene.DemoPageTicked, Scene.DemoPageLoad -> {
            Box(Modifier.fillMaxSize()) {
                MutantUiSample(
                    remember { SampleSettings(page = io.github.youndie.mutant.sample.SamplePage.Demo) },
                    scroll,
                )
                Ticker()
                if (scene == Scene.DemoPageLoad) DrawingLoad()
            }
        }

        Scene.DemoRow, Scene.DemoRowNoClip -> {
            Box(Modifier.fillMaxSize().background(MutantTheme.colors.page)) {
                MutantTheme {
                    androidx.compose.foundation.layout.Row(
                        Modifier.padding(24.dp),
                        horizontalArrangement =
                            androidx.compose.foundation.layout.Arrangement
                                .spacedBy(32.dp),
                    ) {
                        for (screen in DemoScreen.entries) {
                            DemoScreenFrame(
                                screen,
                                strings,
                                roundedClip =
                                    scene == Scene.DemoRow,
                            )
                        }
                    }
                }
            }
        }

        Scene.DemoPageClean -> {
            MutantUiSample(
                remember { SampleSettings(page = io.github.youndie.mutant.sample.SamplePage.Demo, cleanSignal = true) },
                scroll,
            )
        }
    }
}

/** One pixel redrawn every frame, so a page with nothing moving still asks for frames (B-76). */
@Composable
private fun Ticker() {
    var n by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) androidx.compose.runtime.withFrameNanos { n++ } }
    Box(
        Modifier.size(1.dp).drawBehind {
            drawRect(
                if (n % 2 ==
                    0
                ) {
                    androidx.compose.ui.graphics.Color.Black
                } else {
                    androidx.compose.ui.graphics.Color.White
                },
            )
        },
    )
}

/**
 * A fixed drawing, recorded once and replayed every frame: [LOAD_PATHS] filled anti-aliased paths of
 * [LOAD_POINTS] points each, which Skia tessellates on the render thread's CPU (B-76's control).
 */
@Composable
private fun DrawingLoad() {
    val paths =
        remember {
            val rnd = java.util.Random(76)
            List(LOAD_PATHS) {
                androidx.compose.ui.graphics.Path().apply {
                    val cx = 40f + rnd.nextFloat() * 1180
                    val cy = 40f + rnd.nextFloat() * 700
                    moveTo(cx, cy)
                    repeat(LOAD_POINTS) { lineTo(cx + rnd.nextFloat() * 80 - 40, cy + rnd.nextFloat() * 80 - 40) }
                    close()
                }
            }
        }
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        for (path in paths) {
            drawPath(
                path,
                androidx.compose.ui.graphics
                    .Color(0x33FFAA00),
            )
        }
    }
}

private const val LOAD_PATHS = 1_200
private const val LOAD_POINTS = 120

private fun <T : java.awt.Component> Container.find(type: Class<T>): T? {
    for (c in components) {
        if (type.isInstance(c)) return type.cast(c)
        if (c is Container) c.find(type)?.let { return it }
    }
    return null
}

private fun pct(
    sorted: List<Double>,
    p: Double,
): Double = if (sorted.isEmpty()) Double.NaN else sorted[((sorted.size - 1) * p).toInt()]

private fun ms(ns: Long) = ns / 1e6

/** The render thread's CPU time per frame drawn, ms; NaN where the thread was not yet known. */
private fun renderPerFrame(frames: List<Frame>): Double {
    val f = frames.filter { it.renderCpuNs >= 0 }
    if (f.size < 2) return Double.NaN
    return ms(f.last().renderCpuNs - f.first().renderCpuNs) / (f.size - 1)
}

/** The render thread's CPU time over a scene's wall time, %; NaN where the thread was not yet known. */
private fun busyOf(frames: List<Frame>): Double {
    val f = frames.filter { it.renderCpuNs >= 0 }
    if (f.size < 2) return Double.NaN
    return 100.0 * (f.last().renderCpuNs - f.first().renderCpuNs) / (f.last().startNs - f.first().startNs)
}

/** The render thread's CPU between two frames' starts, ms; NaN where the thread was not yet known. */
private fun renderMs(
    a: Frame,
    b: Frame,
) = if (a.renderCpuNs < 0 || b.renderCpuNs < 0) Double.NaN else ms(b.renderCpuNs - a.renderCpuNs)

private fun f1(v: Double) = "%.2f".format(v)

fun main(args: Array<String>) {
    val commit = args.getOrNull(0) ?: "unknown"
    val out = File(args.getOrNull(1) ?: "build/b-72").apply { mkdirs() }
    args.getOrNull(2)?.let { rounds = it.toInt() }
    args.getOrNull(3)?.let { sceneMs = it.toLong() }
    val only = args.getOrNull(4)?.split(',')?.map { Scene.valueOf(it) }
    Toolkit.getDefaultToolkit().systemEventQueue.push(TimingQueue())
    RenderThread.start()

    val sceneState = AtomicReference<(Scene) -> Unit>()
    val scrollRef = AtomicReference<ScrollState>()
    lateinit var window: ComposeWindow
    SwingUtilities.invokeAndWait {
        window = ComposeWindow()
        window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        window.title = "mutant-ui B-72"
        window.isAlwaysOnTop = true
        window.setSize(1280, 900)
        window.setLocation(40, 40)
        window.setContent {
            var scene by remember { mutableStateOf(Scene.ComponentsRest) }
            var key by remember { mutableStateOf(0) }
            sceneState.set { s ->
                scene = s
                key++
            }
            androidx.compose.runtime.key(key) {
                val scroll = remember { ScrollState(0) }
                scrollRef.set(scroll)
                SceneContent(scene, scroll)
            }
        }
        window.isVisible = true
    }
    Thread.sleep(2_000)
    val layer = window.contentPane.find(SkiaLayer::class.java) ?: error("no SkiaLayer in the window")
    SwingUtilities.invokeAndWait {
        val inner = layer.renderDelegate ?: error("no render delegate")
        layer.renderDelegate =
            object : SkikoRenderDelegate {
                override fun onRender(
                    canvas: Canvas,
                    width: Int,
                    height: Int,
                    nanoTime: Long,
                ) {
                    val t0 = System.nanoTime()
                    if (recorder.scene == Scene.Control) busyWait(CONTROL_MS)
                    inner.onRender(canvas, width, height, nanoTime)
                    recorder.rendered(t0, System.nanoTime())
                }
            }
    }

    val device = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    val header =
        listOf(
            "# B-72 frame bench",
            "# commit $commit",
            "# host ${ProcessBuilder(
                "sysctl",
                "-n",
                "hw.model",
            ).start().inputStream.bufferedReader().readText().trim()}, " +
                "cpu ${ProcessBuilder(
                    "sysctl",
                    "-n",
                    "machdep.cpu.brand_string",
                ).start().inputStream.bufferedReader().readText().trim()}, " +
                "${Runtime.getRuntime().availableProcessors()} cores",
            "# os ${System.getProperty(
                "os.name",
            )} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}",
            "# jvm ${System.getProperty("java.vm.name")} ${System.getProperty("java.runtime.version")}",
            "# render ${layer.renderApi}, display ${device.displayMode.width}x${device.displayMode.height} @ " +
                "${device.displayMode.refreshRate} Hz, window ${window.width}x${window.height}, " +
                "scale ${window.graphicsConfiguration.defaultTransform.scaleX}",
            "# rounds $rounds (round 1 not counted), scene ${sceneMs / 1000} s, first ${SETTLE_MS / 1000} s not counted",
        )
    header.forEach(::println)
    val csv = out.resolve("frames.csv").bufferedWriter()
    csv.write(header.joinToString("\n") + "\nround,scene,frame,interval_ms,work_ms,event_ms,render_ms\n")

    data class Row(
        val round: Int,
        val scene: Scene,
        val frames: List<Frame>,
        val scrolled: Int,
    )
    val rows = ArrayList<Row>()
    val scenes = only ?: Scene.entries
    for (round in 1..rounds) {
        for (i in scenes.indices) {
            val scene = scenes[(i + round - 1) % scenes.size]
            SwingUtilities.invokeAndWait {
                recorder.counting = false
                recorder.frames.clear()
                recorder.scene = scene
                sceneState.get()(scene)
            }
            Thread.sleep(SETTLE_MS)
            SwingUtilities.invokeAndWait { recorder.counting = true }
            var travelled = 0
            var last = 0
            val until = nowMs() + sceneMs
            var down = true
            var flipAt = nowMs() + WHEEL_FLIP_MS
            while (nowMs() < until) {
                if (scene.wheel) {
                    if (nowMs() > flipAt) {
                        down = !down
                        flipAt += WHEEL_FLIP_MS
                    }
                    SwingUtilities.invokeAndWait {
                        val target = SwingUtilities.getDeepestComponentAt(window.contentPane, 640, 450) ?: layer
                        val p = SwingUtilities.convertPoint(window.contentPane, 640, 450, target)
                        Toolkit.getDefaultToolkit().systemEventQueue.postEvent(
                            MouseWheelEvent(
                                target,
                                MouseEvent.MOUSE_WHEEL,
                                nowMs(),
                                0,
                                p.x,
                                p.y,
                                0,
                                false,
                                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                                3,
                                if (down) 1 else -1,
                            ),
                        )
                        val now = scrollRef.get().value
                        travelled += kotlin.math.abs(now - last)
                        last = now
                    }
                    Thread.sleep(WHEEL_EVERY_MS)
                } else {
                    Thread.sleep(100)
                }
            }
            val frames = ArrayList<Frame>()
            SwingUtilities.invokeAndWait {
                recorder.counting = false
                frames += recorder.frames
                recorder.frames.clear()
            }
            rows += Row(round, scene, frames, travelled)
            for ((n, f) in frames.withIndex()) {
                val interval = if (n == 0) Double.NaN else ms(f.startNs - frames[n - 1].startNs)
                val render = if (n == 0) Double.NaN else renderMs(frames[n - 1], f)
                csv.write("$round,$scene,$n,${f1(interval)},${f1(ms(f.workNs))},${f1(ms(f.eventNs))},${f1(render)}\n")
            }
            println(
                "round $round ${scene.name}: ${frames.size} frames${if (scene.wheel) ", scrolled $travelled px" else ""}",
            )
        }
    }
    csv.close()

    println()
    println(
        "scene                 frames/s  interval p50  p95   >16.7%  >33%  | work p50   p95    p99    max  " +
            "| event p50  p95  | render/frame busy",
    )
    val counted = rows.filter { it.round > 1 }
    val summary = LinkedHashMap<Scene, String>()
    for (scene in scenes) {
        val all = counted.filter { it.scene == scene }
        val intervals =
            all.flatMap { r -> r.frames.zipWithNext { a, b -> ms(b.startNs - a.startNs) } }.sorted()
        val work = all.flatMap { r -> r.frames.map { ms(it.workNs) } }.sorted()
        val event = all.flatMap { r -> r.frames.filter { it.eventNs >= 0 }.map { ms(it.eventNs) } }.sorted()
        val renderFrame = all.map { renderPerFrame(it.frames) }.filter { !it.isNaN() }.average()
        val renderBusy = all.map { busyOf(it.frames) }.filter { !it.isNaN() }.average()
        val fps = all.sumOf { it.frames.size } / (all.size * sceneMs / 1000.0)
        val over16 = 100.0 * intervals.count { it > 16.7 } / intervals.size
        val over33 = 100.0 * intervals.count { it > 33.0 } / intervals.size
        val verdict =
            when {
                work.isEmpty() -> "IDLE (no frame asked)"
                pct(work, .5) > 16.7 || over33 > 5 -> "RED"
                pct(work, .95) <= 8 && over16 < 1 -> "GREEN"
                else -> "GREY"
            }
        val line =
            "%-20s %8.1f  %10s %6s %7.2f %5.2f  | %6s %6s %6s %6s | %6s %6s | %8s %4.0f%%  %s".format(
                scene.name,
                fps,
                f1(pct(intervals, .5)),
                f1(pct(intervals, .95)),
                over16,
                over33,
                f1(pct(work, .5)),
                f1(pct(work, .95)),
                f1(pct(work, .99)),
                f1(work.lastOrNull() ?: Double.NaN),
                f1(pct(event, .5)),
                f1(pct(event, .95)),
                f1(renderFrame),
                renderBusy,
                if (scene == Scene.Control) "(control)" else verdict,
            )
        summary[scene] = line
        println(line)
    }

    // B-76's control: what the fixed drawing adds to the render thread's CPU per frame on a blank page, it must add
    // on the Demo page too, round by round. (A share cannot: on the Demo page the thread is near saturation.)
    fun added(
        with: Scene,
        without: Scene,
    ) = (2..rounds).mapNotNull { r ->
        val a = counted.firstOrNull { it.round == r && it.scene == with } ?: return@mapNotNull null
        val b = counted.firstOrNull { it.round == r && it.scene == without } ?: return@mapNotNull null
        renderPerFrame(a.frames) - renderPerFrame(b.frames)
    }
    if (Scene.BlankLoad in scenes) {
        println()
        println("render control, what the load adds to the render thread's CPU per frame, per counted round:")
        println("  on a blank page    ${added(Scene.BlankLoad, Scene.Blank).joinToString { f1(it) }} ms")
        println(
            "  on the Demo page   ${added(
                Scene.DemoPageLoad,
                Scene.DemoPageTicked,
            ).joinToString { f1(it) }} ms",
        )
    }
    // The control: its work over the resting page's, per counted round, must read the busy-wait.
    val rest = counted.filter { it.scene == Scene.ComponentsRest }
    val control = counted.filter { it.scene == Scene.Control }

    fun p50(r: Row) = pct(r.frames.map { ms(it.workNs) }.sorted(), .5)
    val deltas =
        (2..rounds).mapNotNull { r ->
            val a = rest.firstOrNull { it.round == r } ?: return@mapNotNull null
            val b = control.firstOrNull { it.round == r } ?: return@mapNotNull null
            p50(b) - p50(a)
        }
    println()
    println("control: work p50 over ComponentsRest per counted round ${deltas.joinToString { f1(it) }} ms")
    println("         (busy-wait $CONTROL_MS ms)")
    val wheel = counted.filter { it.scene == Scene.ComponentsWheel }.map { it.scrolled }
    println("render threads seen: ${RenderThread.threadsSeen}")
    println("wheel: scrolled per counted round $wheel px")
    SwingUtilities.invokeAndWait { window.dispose() }
    exitProcess(0)
}
