package io.github.youndie.mutant.bench

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.surface.BodyPart
import io.github.youndie.mutant.surface.LocalMutantOutlineAsFill
import io.github.youndie.mutant.surface.LocalMutantSkipBodyParts
import io.github.youndie.mutant.theme.MutantTheme
import java.lang.management.ManagementFactory
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.system.exitProcess

/**
 * B-78: what each part of a body costs Skia's render thread. A real window draws three columns of the Library
 * screen's bodies (a field, four chips, two cards and a button each) with no Tape, and one part at a time is left out
 * of every body's draw. The render thread's CPU per frame is read as B-76's bench reads it, with its control: a fixed
 * load of tessellated paths must add the same on a blank page and over the bodies. Scenes rotate each round; round 1
 * and each scene's first second are not counted.
 *
 * `./gradlew :mutant-core:bodyRenderBench --args="<commit> [rounds] [scene-ms] [Scene,Scene]"`, stdout into `logs/b-78/`.
 */
private var rounds = 6
private var sceneMs = 8_000L
private const val SETTLE_MS = 1_000L
private const val RENDER_THREAD = "skiko-dispatcher-to-block-on"
private const val LOAD_PATHS = 1_200
private const val LOAD_POINTS = 120

private var scenes: List<Scene> = emptyList()

private class Scene(
    val name: String,
    val bodies: Boolean,
    val load: Boolean = false,
    val skip: Set<BodyPart> = emptySet(),
    val stroked: Boolean = false,
)

private val allScenes: List<Scene> =
    listOf(
        Scene("Blank", bodies = false),
        Scene("BlankLoad", bodies = false, load = true),
        Scene("Bodies", bodies = true),
        Scene("BodiesLoad", bodies = true, load = true),
        // B-79: the outline stroked on the render thread every frame, as before.
        Scene("BodiesStroked", bodies = true, stroked = true),
    ) + BodyPart.entries.map { Scene("No$it", bodies = true, skip = setOf(it)) }

/** The CPU time of every render thread seen, a finished one at its last reading; sampled every 5 ms. */
private object RenderThread {
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
        }, "b78-render-sampler").apply { isDaemon = true }.start()
    }
}

/** Frames drawn and the render CPU over one counted scene. */
private class Reading(
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

/** The Library screen's bodies, one column. */
@Composable
private fun LibraryBodies(column: Int) {
    Column(Modifier.size(360.dp, 720.dp).padding(16.dp)) {
        MutantTextField("", {}, Modifier.fillMaxWidth(), placeholder = "Find a recording", key = "field$column")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Tapes", "Noise", "Kept").forEachIndexed { i, label ->
                MutantChip(label, i == 1, {}, key = "chip$column$i")
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
            MutantCard(Modifier.fillMaxWidth(), onClick = {}, key = "cardA$column") {
                MutantCardTitle("Untitled")
                MutantCardSubtitle("Side A")
            }
            MutantCard(Modifier.fillMaxWidth(), onClick = {}, key = "cardB$column") {
                MutantCardTitle("Night, Tuesday")
                MutantCardSubtitle("Recorded over")
            }
        }
        Spacer(Modifier.height(24.dp))
        MutantButton("Play all", {}, Modifier.fillMaxWidth(), key = "button$column")
    }
}

@Composable
private fun SceneContent(scene: Scene) {
    MutantTheme {
        CompositionLocalProvider(
            LocalMutantSkipBodyParts provides scene.skip,
            LocalMutantOutlineAsFill provides !scene.stroked,
        ) {
            Box(Modifier.fillMaxSize().background(MutantTheme.colors.page)) {
                if (scene.bodies) {
                    Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        for (c in 0 until 3) LibraryBodies(c)
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
    RenderThread.start()
    var show: (Int) -> Unit = {}
    lateinit var window: ComposeWindow
    SwingUtilities.invokeAndWait {
        window = ComposeWindow()
        window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        window.title = "mutant-ui B-78"
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
    println("# B-78 body render bench")
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
    val readings = HashMap<Pair<Int, String>, Reading>()
    for (round in 1..rounds) {
        for (i in scenes.indices) {
            val index = (i + round - 1) % scenes.size
            SwingUtilities.invokeAndWait { show(index) }
            Thread.sleep(SETTLE_MS)
            val f0 = frames
            val r0 = RenderThread.cumulativeNs
            val t0 = System.nanoTime()
            Thread.sleep(sceneMs)
            val reading = Reading(frames - f0, RenderThread.cumulativeNs - r0, System.nanoTime() - t0)
            readings[round to scenes[index].name] = reading
            println(
                "round $round ${scenes[index].name}: ${reading.frames} frames, ${f2(
                    reading.perFrameMs,
                )} ms render/frame",
            )
        }
    }
    println()
    println("scene            frames/s  render/frame  vs Bodies   (counted rounds 2..$rounds, mean; per round)")
    val counted = 2..rounds

    fun of(name: String) = counted.mapNotNull { readings[it to name] }
    val bodies = of("Bodies").map { it.perFrameMs }.average()
    for (s in scenes) {
        val rs = of(s.name)
        val per = rs.map { it.perFrameMs }
        val mean = per.average()
        println(
            "%-16s %8.1f  %10s ms  %8s ms   %s".format(
                s.name,
                rs.map { it.fps }.average(),
                f2(mean),
                if (s.bodies) f2(mean - bodies) else "",
                per.joinToString(" ") { f2(it) },
            ),
        )
    }

    fun added(
        with: String,
        without: String,
    ) = counted.map { readings[it to with]!!.perFrameMs - readings[it to without]!!.perFrameMs }
    println()
    println("control, what the load adds per frame per counted round:")
    println("  on a blank page    ${added("BlankLoad", "Blank").joinToString { f2(it) }} ms")
    println("  over the bodies    ${added("BodiesLoad", "Bodies").joinToString { f2(it) }} ms")
    println("render threads seen: ${RenderThread.threadsSeen}")
    SwingUtilities.invokeAndWait { window.dispose() }
    exitProcess(0)
}
