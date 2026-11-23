package io.github.youndie.mutant.components

import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.VenomFrom
import io.github.youndie.mutant.gen.VenomShapes
import io.github.youndie.mutant.gen.venomShapes
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A selection window's run by venom (the segmented button's, B-31; the menu's, B-33; the drawer's, B-34): the slots it
 * moves between, each given by the component as the engine places it, in dp ([slot]: left, top, width, height), and the
 * press that chose each. Read from draw, layer and colour lambdas; the clock's frame is read only while a run lasts, so
 * a still component asks for no frames.
 *
 * A pick during a run goes on from where the slime stands (B-172), never from the slot picked before, as the page does
 * (its `bindPicks` reads the pill's `offsetLeft`, already at the last pick, and the window jumps there whole). The run
 * under way settles at its nearer end, by what is left to play: it plays back to where it came from, or on to where it
 * was going, and from there a new run goes to the latest pick. A pick of where the run came from is the run played
 * back; of where it goes, the run going on. Every frame drawn is a frame of a run from rest, at its own moment: the
 * generator's `venomShapes` is asked for no shape it does not draw.
 */
internal class VenomRun(
    private var at: Int,
    private val radius: Double,
) {
    var key = ""
    var cassette = ""
    var slot: (Int) -> DoubleArray = { DoubleArray(4) }

    /** Whether the run steps at 24 a second as the design draws it, or moves on every frame (B-100); set from composition. */
    var steps: Boolean = false

    /** Where the slime stands when no run plays: the last index it settled on, or -1. */
    private var stands = at

    /** The run that plays: from slot [a] to slot [b] (none while [a] is -1), at progress [p0] when [t0], going [dir]. */
    private var a = -1
    private var b = -1
    private var p0 = 0.0
    private var t0 = Double.NaN
    private var dir = 1

    /** The press of the run that plays, and the one the latest pick was made by, dp from the slot's middle. */
    private var runPx = 0.0
    private var runPy = 0.0
    private var pickPx = 0.0
    private var pickPy = 0.0
    private var pressX = 0f
    private var pressY = 0f

    private var cachedP = Double.NaN
    private var cached: VenomShapes? = null

    /** The press that chose the slot, dp from its middle: the mass goes there. */
    fun pressAt(
        x: Float,
        y: Float,
    ) {
        pressX = x
        pressY = y
    }

    /**
     * From composition: a new index starts a run from where the slime stands, unless the clock stands. During a run it
     * settles the run at its nearer end first: what is left to play back to its start (and a run from there unless that
     * is the pick) against what is left to play on to its end (and a run from there unless that is the pick).
     */
    fun follow(
        to: Int,
        nowMs: Double,
        still: Boolean,
    ) {
        if (to == at) return
        at = to
        pickPx = pressX.toDouble()
        pickPy = pressY.toDouble()
        if (still || to < 0) {
            a = -1
            stands = to
            return
        }
        settle(nowMs)
        if (a < 0) {
            // From no window, the window arrives at once.
            if (stands >= 0) play(stands, to, 0.0, 1, nowMs, pickPx, pickPy) else stands = to
            return
        }
        val p = progress(nowMs - t0)
        val back = p + (if (a == to) 0 else 1)
        val on = 1 - p + (if (b == to) 0 else 1)
        when {
            // Not under way yet: the run starts over toward the pick, from the same place.
            back < on && p == 0.0 && a != to -> play(a, to, 0.0, 1, nowMs, pickPx, pickPy)

            back < on -> play(a, b, p, -1, nowMs, runPx, runPy)

            else -> play(a, b, p, 1, nowMs, runPx, runPy)
        }
    }

    private fun play(
        from: Int,
        to: Int,
        p: Double,
        direction: Int,
        nowMs: Double,
        x: Double,
        y: Double,
    ) {
        a = from
        b = to
        p0 = p
        dir = direction
        t0 = nowMs
        runPx = x
        runPy = y
        cachedP = Double.NaN
    }

    /** The progress [e] ms after [t0], stepped at 24 frames a second where [steps] says, from [p0] in [dir]. */
    private fun progress(e: Double): Double {
        val ms = maxOf(0.0, e)
        val q = (if (steps) floor(ms / VENOM_STEP_MS) * VENOM_STEP_MS else ms) / MutantMotion.VENOM_MS
        return (p0 + dir * q).coerceIn(0.0, 1.0)
    }

    /** How long the run that plays lasts from [t0]: what is left to its end, in whole steps where [steps] says. */
    private fun length(): Double {
        val left = (if (dir > 0) 1 - p0 else p0) * MutantMotion.VENOM_MS
        return if (steps) ceil(left / VENOM_STEP_MS - STEP_EPSILON) * VENOM_STEP_MS else left
    }

    /** Ends the runs over by [nowMs]: the slime stands at the end reached, and a run to the latest pick starts there. */
    private fun settle(nowMs: Double) {
        while (a >= 0) {
            val length = length()
            if (nowMs - t0 < length) return
            val end = t0 + length
            stands = if (dir > 0) b else a
            a = -1
            if (at >= 0 && at != stands) play(stands, at, 0.0, 1, end, pickPx, pickPy)
        }
    }

    private fun running(clock: MutantClock): Boolean {
        if (a < 0) return false
        clock.frame
        settle(clock.nowMs)
        return a >= 0
    }

    /**
     * The run's progress with its shapes; null outside a run. [to] is the index the caller was composed for: a lambda
     * that captures it is made anew when a pick changes it, so it reads the clock again.
     */
    fun now(
        clock: MutantClock,
        to: Int,
    ): Pair<Double, VenomShapes>? {
        if (to != at || !running(clock)) return null
        val p = progress(clock.nowMs - t0)
        if (p != cachedP || cached == null) {
            val n = slot(b)
            val o = slot(a)
            cached =
                MutantGenerator(cassette).venomShapes(
                    n[0],
                    n[1],
                    n[2],
                    n[3],
                    radius,
                    key,
                    VenomFrom(o[0] - n[0], o[1] - n[1], runPx, runPy),
                    p,
                )
            cachedP = p
        }
        return p to cached!!
    }

    /**
     * The slot the window stands on: the end the run under way goes to (it shows there once the venom has filled it),
     * or where the slime stands. Read after [now] in the same lambda. It differs from the pick only while a run settles
     * on the way to it.
     */
    val window: Int get() = if (a >= 0) b else stands
}

/** The venom steps at 24 frames a second (the engine's 41.67 ms). */
private const val VENOM_STEP_MS = 41.67

/** A run's length in steps is a whole number up to rounding: what lies under this is not one more step. */
private const val STEP_EPSILON = 1e-6
