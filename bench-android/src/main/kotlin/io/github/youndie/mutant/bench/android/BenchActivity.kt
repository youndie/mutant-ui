package io.github.youndie.mutant.bench.android

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.gen.goldens.GoldenInstance
import io.github.youndie.mutant.gen.goldens.genGoldens
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantTheme
import java.io.BufferedWriter
import java.io.File
import kotlin.math.abs

private const val TAG = "MutantBench"

/** The screen scenes of B-49's pre-registration, in their round-1 order; each round rotates it by one. */
private enum class Scene { Blank, TenCards12, TenCards8, List100, Spin5 }

private const val METRICS_HEADER =
    "scene,round,since_ns,vsync_ns,input_ns,animation_ns,layout_ns,draw_ns,sync_ns,command_ns,swap_ns,total_ns,gpu_ns," +
        "deadline_ns,first"

/** The control's known cost, inside a layer nested in another. */
private const val SPIN_NS = 5_000_000L

/** The list's scripted scroll, dp a second. */
private const val SCROLL_DP_PER_S = 1_500f

/** B-06's instance protocol: calls a round, arms interleaved per call, rounds (round 1 discarded), warm-up a case. */
private const val CALLS = 200
private const val INSTANCE_ROUNDS = 5
private const val WARMUP_NS = 3_000_000_000L
private const val CONTROL_NS = 200_000L

/**
 * B-49's host. `bench`: [Scene]s for `rounds` of `seconds` each, Android's per-frame metrics into `metrics.csv` and a
 * line a scene into `scenes.csv`; then B-06's instance and warm-frame arms per case into `instance.txt`. `show`: one
 * scene on screen, for a look. It logs `DONE` when its files are closed.
 */
class BenchActivity : ComponentActivity() {
    @Volatile private var scene = "start"

    @Volatile private var round = -1

    @Volatile private var since = 0L

    private var metrics: BufferedWriter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // The brief's target is 60 fps, and the panel can do more.
        window.attributes = window.attributes.apply { preferredRefreshRate = 60f }
        val dir = getExternalFilesDir(null)!!
        val commit = intent.getStringExtra("commit") ?: "unknown"
        when (intent.getStringExtra("mode") ?: "bench") {
            "bench" -> bench(dir, commit)
            "show" -> setContent { SceneOf(Scene.valueOf(intent.getStringExtra("scene") ?: "TenCards12")) {} }
            "instance" -> instanceOnly(dir, commit)
        }
    }

    private fun header(
        out: BufferedWriter,
        commit: String,
    ) {
        val shown = display
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        out.appendLine(
            "# device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT}",
        )
        out.appendLine("# build: ${Build.FINGERPRINT}")
        out.appendLine(
            "# display: ${shown?.mode?.physicalWidth}x${shown?.mode?.physicalHeight} at ${shown?.refreshRate} Hz, " +
                "density ${resources.displayMetrics.density}",
        )
        out.appendLine("# cores: ${Runtime.getRuntime().availableProcessors()}, debuggable: $debuggable")
        out.appendLine("# commit: $commit")
    }

    private fun thermal() = getSystemService(PowerManager::class.java).currentThermalStatus

    private fun bench(
        dir: File,
        commit: String,
    ) {
        val rounds = intent.getIntExtra("rounds", 6)
        val seconds = intent.getIntExtra("seconds", 10)
        val scenes = File(dir, "scenes.csv").bufferedWriter()
        header(scenes, commit)
        scenes.appendLine("# rounds: $rounds, seconds a scene: $seconds")
        scenes.appendLine("scene,round,frames,scrolled_px,thermal_start,thermal_end")
        val out = File(dir, "metrics.csv").bufferedWriter()
        header(out, commit)
        metrics = out
        out.appendLine(METRICS_HEADER)
        val thread = HandlerThread("frame-metrics").apply { start() }
        val listener =
            Window.OnFrameMetricsAvailableListener { _, m, _ ->
                val line = metricsLine(m)
                synchronized(out) { metrics?.appendLine(line) }
            }
        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        val plan =
            (1..rounds).flatMap { r ->
                val order = Scene.entries
                List(order.size) { i -> r to order[(i + r - 1) % order.size] }
            }
        setContent {
            var at by remember { mutableIntStateOf(0) }
            if (at >= plan.size) {
                LaunchedEffect(Unit) {
                    window.removeOnFrameMetricsAvailableListener(listener)
                    synchronized(out) {
                        out.close()
                        metrics = null
                    }
                    scenes.close()
                    Thread {
                        instances(File(dir, "instance.txt"), commit)
                        Log.i(TAG, "DONE")
                        runOnUiThread { finish() }
                    }.start()
                }
            } else {
                val (r, s) = plan[at]
                key(at) {
                    val t0 = thermal()
                    var frames by remember { mutableIntStateOf(0) }
                    var scrolled by remember { mutableLongStateOf(0L) }
                    LaunchedEffect(Unit) {
                        scene = s.name
                        round = r
                        since = System.nanoTime()
                        val end = since + seconds * 1_000_000_000L
                        while (System.nanoTime() < end) withFrameNanos { frames++ }
                        scenes.appendLine("${s.name},$r,$frames,$scrolled,$t0,${thermal()}")
                        scenes.flush()
                        at++
                    }
                    SceneOf(s) { scrolled += it }
                }
            }
        }
    }

    private fun instanceOnly(
        dir: File,
        commit: String,
    ) {
        setContent { SceneOf(Scene.Blank) {} }
        Thread {
            instances(File(dir, "instance.txt"), commit)
            Log.i(TAG, "DONE")
            runOnUiThread { finish() }
        }.start()
    }

    private fun metricsLine(m: FrameMetrics): String {
        val vsync = m.getMetric(FrameMetrics.VSYNC_TIMESTAMP)
        return "$scene,$round,${vsync - since},$vsync,${m.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)}," +
            "${m.getMetric(FrameMetrics.ANIMATION_DURATION)},${m.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)}," +
            "${m.getMetric(FrameMetrics.DRAW_DURATION)},${m.getMetric(FrameMetrics.SYNC_DURATION)}," +
            "${m.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)},${m.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)}," +
            "${m.getMetric(FrameMetrics.TOTAL_DURATION)},${m.getMetric(FrameMetrics.GPU_DURATION)}," +
            "${m.getMetric(FrameMetrics.DEADLINE)},${m.getMetric(FrameMetrics.FIRST_DRAW_FRAME)}"
    }

    /** B-06's arms on the phone, by B-06's protocol, printed as `fieldBench` prints them. */
    private fun instances(
        file: File,
        commit: String,
    ) {
        val out = file.bufferedWriter()
        header(out, commit)
        out.appendLine(
            "# calls $CALLS per arm per round, arms interleaved per call, rounds $INSTANCE_ROUNDS (round 1 discarded), " +
                "warm-up ${WARMUP_NS / 1_000_000_000} s per case",
        )
        val cases = genGoldens.map { Case(it) }
        for (c in cases) {
            val end = System.nanoTime() + WARMUP_NS
            while (System.nanoTime() < end) for ((_, arm) in arms) arm(c)
        }
        val card = cases.single { it.golden.name == "card-a" }
        val controlArms = listOf(arms[1], "warm+200us" to { c: Case -> warm(c).also { spin(CONTROL_NS) } })
        for (r in 1..INSTANCE_ROUNDS) {
            out.appendLine("# round $r thermal ${thermal()}")
            for (c in cases) {
                val cells = round(c, arms)
                out.appendLine(
                    "round $r ${c.golden.name} " + cells.joinToString(" ") { (n, v) -> "$n=${"%.1f".format(v)}" },
                )
            }
            val (w, ctl) = round(card, controlArms).map { it.second }
            out.appendLine(
                "round $r control card-a warm=${"%.1f".format(w)} warm+200us=${"%.1f".format(ctl)} " +
                    "delta=${"%.1f".format(ctl - w)}",
            )
            out.appendLine("round $r checksum $checksum")
            out.flush()
        }
        out.appendLine("# end thermal ${thermal()}")
        out.close()
    }
}

/** One scene on the screen, under the kit's theme; [scrolled] is told the list's scroll, px. */
@Composable
private fun SceneOf(
    scene: Scene,
    scrolled: (Long) -> Unit,
) {
    // Every scene asks for a frame each vsync and reads it in a draw, as the control's layer must; only Spin5 spins.
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { frame = it } }
    MutantTheme(reduceMotion = false) {
        val clock = remember { MutantClock(fps = if (scene == Scene.TenCards8) 8.0 else 12.0) }
        ProvideMutantClock(clock) {
            val ground = Modifier.fillMaxSize().background(MutantTheme.colors.background)
            Box(if (scene == Scene.Blank) ground else ground.mutantTape()) {
                when (scene) {
                    Scene.Blank -> {}

                    Scene.TenCards12, Scene.TenCards8, Scene.Spin5 -> {
                        TenCards()
                    }

                    Scene.List100 -> {
                        List100(scrolled)
                    }
                }
                Box(Modifier.fillMaxSize().graphicsLayer { }) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer { }.drawBehind {
                            frame
                            if (scene == Scene.Spin5) {
                                val until = System.nanoTime() + SPIN_NS
                                while (System.nanoTime() < until) {
                                    // The control: a known cost, by the clock.
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TenCards() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(10) { i ->
            MutantCard(Modifier.size(328.dp, 80.dp), key = "card/b49/$i") {
                MutantCardTitle("Кассета ${(i + 1).toString().padStart(2, '0')}")
                MutantCardSubtitle("Запись ${12 + i}:0$i")
            }
        }
    }
}

@Composable
private fun List100(scrolled: (Long) -> Unit) {
    val state = rememberLazyListState()
    val pxPerNs = with(LocalDensity.current) { SCROLL_DP_PER_S.dp.toPx() } / 1e9f
    LaunchedEffect(Unit) {
        var down = true
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val by = (now - last) * pxPerNs * (if (down) 1 else -1)
                    val moved = state.dispatchRawDelta(by)
                    scrolled(abs(moved).toLong())
                    if (abs(moved) < abs(by) / 2) down = !down
                }
                last = now
            }
        }
    }
    // As the demo's Library lays its rows out: a leading icon, whose tile is a drawn body, a time, a divider under each.
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), state = state) {
        items(100) { i ->
            MutantListItem(
                "Кассета ${(i + 1).toString().padStart(3, '0')}",
                Modifier.fillMaxWidth(),
                supporting = "Запись ${i % 24}:${(i * 7 % 60).toString().padStart(2, '0')}",
                leadingIcon = MutantIcons.Home,
                trailingText = "12:04",
                key = "tile/b49/$i",
            )
            MutantDivider(key = "b49/$i")
        }
    }
}

private var checksum = 0L

private class Case(
    val golden: GoldenInstance,
) {
    val gen = MutantGenerator(cassette = "5")
    val slot = slotOf(golden)
    val instance: Instance = gen.instance(slot, golden.key, golden.still)
    val cache = FieldCache()
    var tick = 0
}

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

private fun fold(f: io.github.youndie.mutant.gen.BodyFrame) {
    checksum += f.contour.loops.sumOf { it.size } + f.highlight.loops.size + f.flowLines.loops.size
    checksum += f.field.values[f.field.values.size / 2].toRawBits()
}

private fun warm(c: Case) = fold(c.gen.frame(c.instance, c.cache, 5000.0 + 125.0 * (c.tick++ % 64)))

private fun spin(ns: Long) {
    val end = System.nanoTime() + ns
    var n = 0L
    while (System.nanoTime() < end) n++
    checksum += n and 1L
}

/** B-06's `instance` and `warm` arms, as `fieldBench` defines them. */
private val arms: List<Pair<String, (Case) -> Unit>> =
    listOf(
        "instance" to { c ->
            c.gen.instance(c.slot, c.golden.key, c.golden.still).also { checksum += it.drips.size + it.flowLines.size }
        },
        "warm" to { c -> warm(c) },
    )

private fun round(
    case: Case,
    arms: List<Pair<String, (Case) -> Unit>>,
): List<Pair<String, Double>> {
    val ns = Array(arms.size) { LongArray(CALLS) }
    for (i in 0 until CALLS) {
        for ((a, arm) in arms.withIndex()) {
            val t0 = System.nanoTime()
            arm.second(case)
            ns[a][i] = System.nanoTime() - t0
        }
    }
    return arms.mapIndexed { a, (name, _) ->
        ns[a].sort()
        name to ns[a][CALLS / 2] / 1000.0
    }
}
