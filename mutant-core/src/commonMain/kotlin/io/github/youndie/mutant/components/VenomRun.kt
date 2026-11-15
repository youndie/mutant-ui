package io.github.youndie.mutant.components

import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.VenomFrom
import io.github.youndie.mutant.gen.VenomShapes
import io.github.youndie.mutant.gen.venomShapes
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.floor
import kotlin.math.min

/**
 * A selection window's run by venom (the segmented button's, B-31; the menu's, B-33): where it came from and when,
 * the press that chose it, and the slots it moves between, each given by the component as the engine places it, in dp
 * ([slot]: left, top, width, height). Read from draw, layer and colour lambdas; the clock's frame is read only while a
 * run lasts, so a still component asks for no frames.
 */
internal class VenomRun(
    private var at: Int,
    private val radius: Double,
) {
    var key = ""
    var cassette = ""
    var slot: (Int) -> DoubleArray = { DoubleArray(4) }
    private var from = -1
    private var t0 = Double.NaN
    private var px = 0f
    private var py = 0f
    private var cachedP = Double.NaN
    private var cached: VenomShapes? = null

    /** The press that chose the slot, dp from its middle: the mass goes there. */
    fun pressAt(
        x: Float,
        y: Float,
    ) {
        px = x
        py = y
    }

    /** From composition: a new index starts a run from the old one, unless the clock stands. */
    fun follow(
        to: Int,
        nowMs: Double,
        still: Boolean,
    ) {
        if (to == at) return
        from = if (still || at < 0) -1 else at
        t0 = nowMs
        at = to
        cachedP = Double.NaN
    }

    private fun elapsed(clock: MutantClock): Double? {
        if (from < 0) return null
        clock.frame
        return clock.nowMs - t0
    }

    /**
     * The run's progress, stepped at 24 frames a second, with its shapes; null outside a run. [to] is the index the
     * caller was composed for: a lambda that captures it is made anew when a run starts, so it reads the clock again.
     */
    fun now(
        clock: MutantClock,
        to: Int,
    ): Pair<Double, VenomShapes>? {
        if (to != at) return null
        val e = elapsed(clock) ?: return null
        val p = min(1.0, floor(maxOf(0.0, e) / VENOM_STEP_MS) * VENOM_STEP_MS / MutantMotion.VENOM_MS)
        if (p >= 1) {
            from = -1
            return null
        }
        if (p != cachedP || cached == null) {
            val n = slot(at)
            val o = slot(from)
            cached =
                MutantGenerator(cassette).venomShapes(
                    n[0],
                    n[1],
                    n[2],
                    n[3],
                    radius,
                    key,
                    VenomFrom(o[0] - n[0], o[1] - n[1], px.toDouble(), py.toDouble()),
                    p,
                )
            cachedP = p
        }
        return p to cached!!
    }

    /** Whether slot [i]'s word is on the window: the chosen one, from 340 ms into a run, the old one before. */
    fun labelOn(
        i: Int,
        clock: MutantClock,
        to: Int,
    ): Boolean {
        val e = elapsed(clock)
        return if (e != null && e < LABEL_SWITCH_MS) i == from else i == to
    }
}

/** The venom steps at 24 frames a second (the engine's 41.67 ms). */
private const val VENOM_STEP_MS = 41.67

/** The words change colour 340 ms into a run (`setTimeout(…, 340)`). */
private const val LABEL_SWITCH_MS = 340.0
