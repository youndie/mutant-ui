package io.github.youndie.mutant.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.youndie.mutant.tokens.MutantMotion

/**
 * The boil's heat (the engine's `frame`): rising to 1 in 320 ms while pressed, cooling to 0 in 480 ms after,
 * from wherever it was. A pure function of the clock's time and the last press or release; only [on] is state. Under
 * the pause a release cools at once ([hold]).
 *
 * A mouse over the body heats only its tremble (B-193, the designer's answer 16): [trembleAt] rises to [HOVER_HEAT]
 * at the same rates, while [at], the heat the fill, the bubbles, the held drops and the running mosaic follow, stays a
 * press's alone.
 */
@androidx.compose.runtime.Stable
internal class HeatMachine {
    var on: Boolean by mutableStateOf(false)
        private set

    /** A mouse over the body (B-63): its outline trembles as at [HOVER_HEAT], under a press's full boil (B-193). */
    var hovered: Boolean by mutableStateOf(false)
        private set

    /** One heat ramping towards its target: rising at the press's rate, cooling at the release's. */
    private class Ramp {
        private var from = 0.0
        private var since = 0.0

        fun at(
            now: Double,
            target: Double,
        ): Double =
            if (from <= target) {
                minOf(target, from + (now - since) / MutantMotion.BOIL_OVER_IN_MS)
            } else {
                maxOf(target, from - (now - since) / MutantMotion.BOIL_OVER_OUT_MS)
            }

        /** Starts a new ramp from where the one towards [target] stands at [now]. */
        fun restart(
            now: Double,
            target: Double,
        ) {
            from = at(now, target)
            since = now
        }

        /** Stands at [target] from [now]: nothing left to ramp. */
        fun land(
            now: Double,
            target: Double,
        ) {
            from = target
            since = now
        }
    }

    private val press = Ramp()
    private val hover = Ramp()

    private val pressTarget: Double get() = if (on) 1.0 else 0.0
    private val hoverTarget: Double get() = if (hovered) HOVER_HEAT else 0.0

    /** Counts the impulses ([impulse]), so that what reads the heat is told when one lands between ticks. */
    private var impulses by mutableIntStateOf(0)

    /** A press's heat at [now]: the boil's fill, its bubbles, its held drops and its running mosaic. */
    fun at(now: Double): Double {
        impulses
        return press.at(now, pressTarget)
    }

    /** The hover's heat at [now], for the outline's tremble alone (B-193). */
    fun hoverAt(now: Double): Double = hover.at(now, hoverTarget)

    /** The heat the outline trembles at: a press's, or the hover's where it is the higher (B-193). */
    fun trembleAt(now: Double): Double = maxOf(at(now), hoverAt(now))

    /** Whether the pause holds (D7): a heat above where it is going then lands there at once (B-176). */
    private var held = false

    /**
     * Follows the pause from composition. While it holds, a release or a leave cools the body in the same frame, and a
     * heat still cooling when it starts lands: the body shows released for as long as the pause lasts, and has nothing to
     * cool when it lifts. A press under it does not boil, as the clock stands: the surface shows it as a still frame at heat
     * 1 ([mutantPausedPressFill], B-190); a press held across it keeps its heat (B-176). A hover under it shows its
     * finished tremble at once, and a leave takes it at once (D24, B-193).
     */
    fun hold(
        paused: Boolean,
        now: Double,
    ) {
        // Only as the pause starts: a release under it settles itself, and this reads no state in later compositions.
        if (paused && !held) settle(now)
        held = paused
    }

    private fun settle(now: Double) {
        val t = pressTarget
        if (at(now) > t) press.land(now, t)
        hover.land(now, hoverTarget)
    }

    fun press(now: Double) {
        if (on) return
        press.restart(now, pressTarget)
        on = true
    }

    fun release(now: Double) {
        if (!on) return
        press.restart(now, pressTarget)
        on = false
        if (held) settle(now)
    }

    /**
     * A heat impulse (the designer's answers 4 and 39, B-201): the press's heat stands at [k] at [now], where it stood
     * lower, and from there goes where the press is going at its rates, cooling at the release's when nothing presses.
     * Under the pause, nothing: the impulse's finished frame is the body as it was (D24).
     */
    fun impulse(
        now: Double,
        k: Double,
    ) {
        if (held || at(now) >= k) return
        press.land(now, k)
        impulses++
    }

    fun hover(now: Double) {
        if (hovered) return
        hover.restart(now, hoverTarget)
        hovered = true
        if (held) hover.land(now, hoverTarget)
    }

    fun leave(now: Double) {
        if (!hovered) return
        hover.restart(now, hoverTarget)
        hovered = false
        if (held) hover.land(now, hoverTarget)
    }
}

/**
 * B-190: the fill a pressed body shows under the pause (D24, the designer's answer 12): heat 1's amber mix, at once and
 * still, on the rest frame's shape with no bubbles and no tremble. Taken at time 0, where the boil's flicker is off.
 */
internal fun mutantPausedPressFill(
    fill: androidx.compose.ui.graphics.Color,
    colors: io.github.youndie.mutant.tokens.MutantColors,
): androidx.compose.ui.graphics.Color = boilFill(fill, colors, 1.0, 0.0)

/**
 * The heat a hovered body's outline trembles at (the designer's answer 16, B-193): heat 0.25's tremble, with no amber in
 * the fill and no bubbles. B-63 had chosen a simmer of 0.3 that warmed the fill too.
 */
internal const val HOVER_HEAT: Double = .25
