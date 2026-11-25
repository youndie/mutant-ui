package io.github.youndie.mutant.clock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import kotlin.math.floor
import kotlin.math.min

/**
 * The kit clock (research D6): one per screen, in milliseconds, the engine's (`mutant-engine.js`,
 * `frame`). Every body is drawn from it and from nothing else; no component reads system time.
 *
 * Time moves by each frame's step, `min(1000, now − last)` and 16 ms on the first frame, times [speed],
 * and stands while [paused] — the kit's reduce motion (D7). Readers do not observe the time itself but
 * the ticks: [restTick] steps [fps] times a second, [hotTick] 1.5 times as often (a pressed body and a
 * transition), so a body recomposes and redraws once per tick, not once per frame.
 *
 * A clock built [frozen] at a time is the engine's `data-mu-t`: every slot still at that moment. Fixtures
 * and references draw at 5000 ms this way.
 */
@Stable
internal class MutantClock(
    /** The resting tick's rate (tokens `genome.tick`, 8…24 frames a second). */
    val fps: Double = REST_FPS,
    speed: Double = 1.0,
    startMs: Double = 0.0,
    /** Time never moves and no frame is asked for. */
    val frozen: Boolean = false,
) {
    var speed: Double by mutableDoubleStateOf(speed)
    var paused: Boolean by mutableStateOf(false)

    /**
     * Holds on the clock: while any stands (a dialog open over the screen, the brief's `pause`), time stands and no
     * frame is asked for, as under [paused], without touching the theme's own switch.
     */
    internal var holds: Int by mutableIntStateOf(0)

    /** The engine's `step`: one resting tick, ms. */
    val stepMs: Double get() = 1000.0 / fps

    private var time: Double = startMs
    private var lastNanos: Long = NO_FRAME
    private val rest = mutableLongStateOf(tickOf(startMs, stepMs))
    private val hot = mutableLongStateOf(tickOf(startMs, stepMs / 1.5))
    private val frames = mutableLongStateOf(0L)
    private val scene = mutableLongStateOf(tickOf(startMs, SCENE_TICK_MS))

    /** Readers of the clock that need frames; with none, the clock asks for none. */
    internal var readers: Int by mutableIntStateOf(0)

    /**
     * Readers that run through a hold: the Tape, which is the window's topmost layer and runs while a dialog holds the
     * screen (B-197, the designer's answer 23). With one, the clock keeps asking for frames while held, and only
     * [tapeMs] moves.
     */
    internal var tapeReaders: Int by mutableIntStateOf(0)

    private var tapeTime: Double = startMs
    private val tapeFrames = mutableLongStateOf(0L)
    private val tapeScene = mutableLongStateOf(tickOf(startMs, SCENE_TICK_MS))

    /** The time now, ms. Not observable: read the ticks to be told when the picture changes. */
    val nowMs: Double get() = time

    /** The resting tick's number, `floor(t / step)`. Observable. */
    val restTick: Long get() = rest.longValue

    /** The hot tick's number, `floor(t / (step / 1.5))`. Observable. */
    val hotTick: Long get() = hot.longValue

    /**
     * The scene's tick, `floor(t / 125)`, observable: the engine's `fi`, on which the Tape and the stage's own
     * animations step whatever the resting tick's rate (`stageStep`).
     */
    val sceneTick: Long get() = scene.longValue

    /**
     * The frame's number, observable: a running transition reads it, as the engine steps its transitions on
     * every frame from the moment of the event (`meltStep`, `drawItem`); a finished one reads nothing.
     */
    val frame: Long get() = frames.longValue

    /**
     * The Tape's time, ms (B-197): the clock's own, but moving through a hold as well, so it stands only under the pause
     * or frozen. It equals [nowMs] until a hold first stands, and runs ahead of it by every held moment after.
     */
    val tapeMs: Double get() = tapeTime

    /** The frame's number for the Tape, observable: it counts held frames too. */
    val tapeFrame: Long get() = tapeFrames.longValue

    /** The scene's tick of [tapeMs], `floor(tapeMs / 125)`, observable. */
    val tapeSceneTick: Long get() = tapeScene.longValue

    /** The engine's `ts`: the time a resting body is drawn at. Observable through [restTick]. */
    fun restTimeMs(): Double = restTick * stepMs

    /**
     * The time of the resting tick after this one, exactly as [restTimeMs] will read it then: the same product, not
     * `restTimeMs() + stepMs`, which at 12 or 24 ticks a second misses it in the last bit on a third of the ticks, so a
     * frame built ahead for it was never found (B-89).
     */
    fun nextRestTimeMs(): Double = (restTick + 1) * stepMs

    /** The engine's `tsH`: the time a pressed body or a transition is drawn at. Observable through [hotTick]. */
    fun hotTimeMs(): Double = hotTick * (stepMs / 1.5)

    /** Moves the clock to [ms] at once (a fixture, a test). */
    fun setTime(ms: Double) {
        time = ms
        tapeTime = ms
        publish()
    }

    /** One frame at [frameNanos]: the engine's step, unless frozen or paused. */
    fun onFrame(frameNanos: Long) {
        if (frozen) return
        val dt = if (lastNanos == NO_FRAME) FIRST_STEP_MS else min(MAX_STEP_MS, (frameNanos - lastNanos) / 1e6)
        lastNanos = frameNanos
        if (!paused) tapeTime += dt * speed
        tapeFrames.longValue++
        // Held, the clock runs only for the Tape: its own time and frame stand, as when it asked for no frames at all.
        if (holds == 0) {
            if (!paused) time += dt * speed
            frames.longValue++
        }
        publish()
    }

    /** Forgets the last frame, so the next one steps 16 ms, as after a pause the frames resume. */
    internal fun forgetFrame() {
        lastNanos = NO_FRAME
    }

    private fun publish() {
        val r = tickOf(time, stepMs)
        if (r != rest.longValue) rest.longValue = r
        val h = tickOf(time, stepMs / 1.5)
        if (h != hot.longValue) hot.longValue = h
        val f = tickOf(time, SCENE_TICK_MS)
        if (f != scene.longValue) scene.longValue = f
        val tf = tickOf(tapeTime, SCENE_TICK_MS)
        if (tf != tapeScene.longValue) tapeScene.longValue = tf
    }

    internal companion object {
        const val REST_FPS: Double = 8.0
        const val FIRST_STEP_MS: Double = 16.0
        const val MAX_STEP_MS: Double = 1000.0
        const val NO_FRAME: Long = Long.MIN_VALUE

        /** The scene's step, ms: fixed, unlike the resting tick (`stageStep`'s 125). */
        const val SCENE_TICK_MS: Double = 125.0

        fun tickOf(
            ms: Double,
            step: Double,
        ): Long = floor(ms / step).toLong()
    }
}

/** The screen's clock. A body outside any [ProvideMutantClock] draws frozen at 5000 ms. */
internal val LocalMutantClock = staticCompositionLocalOf { MutantClock(startMs = STILL_MS, frozen = true) }

/** The time a still slot is drawn at: the engine's `tFix` default, and every reference's. */
internal const val STILL_MS: Double = 5000.0

/**
 * Provides [clock] to [content] and drives it with the frame clock while anything reads it and it is
 * neither paused nor frozen: no frames are asked for otherwise.
 */
@Composable
internal fun ProvideMutantClock(
    clock: MutantClock = remember { MutantClock() },
    content: @Composable () -> Unit,
) {
    // Held, it runs on for a Tape that reads it (B-197).
    val running = !clock.frozen && !clock.paused && (clock.holds == 0 || clock.tapeReaders > 0) && clock.readers > 0
    LaunchedEffect(clock, running) {
        if (!running) return@LaunchedEffect
        clock.forgetFrame()
        while (true) withFrameNanos { clock.onFrame(it) }
    }
    CompositionLocalProvider(LocalMutantClock provides clock, content = content)
}

/** Marks the caller as a reader of the screen's clock while it is in the composition, and returns the clock. */
@Composable
internal fun readMutantClock(): MutantClock {
    val clock = LocalMutantClock.current
    DisposableEffect(clock) {
        clock.readers++
        onDispose { clock.readers-- }
    }
    return clock
}
