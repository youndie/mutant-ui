package io.github.youndie.mutant.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.youndie.mutant.tokens.MutantMotion

/**
 * The boil's heat (the engine's `frame`): rising to 1 in 320 ms while pressed, cooling to 0 in 480 ms after,
 * from wherever it was. A pure function of the clock's time and the last press or release; only [on] is state. Under
 * the pause a release cools at once ([hold]).
 */
@androidx.compose.runtime.Stable
internal class HeatMachine {
    var on: Boolean by mutableStateOf(false)
        private set

    /** A mouse over the body (B-63): it simmers at [HOVER_HEAT], under a press's full boil. */
    var hovered: Boolean by mutableStateOf(false)
        private set
    private var from = 0.0
    private var since = 0.0

    private val target: Double get() =
        if (on) {
            1.0
        } else if (hovered) {
            HOVER_HEAT
        } else {
            0.0
        }

    /** The heat at [now]: rising towards the target at the press's rate, cooling towards it at the release's. */
    fun at(now: Double): Double {
        val t = target
        return if (from <= t) {
            minOf(t, from + (now - since) / MutantMotion.BOIL_OVER_IN_MS)
        } else {
            maxOf(t, from - (now - since) / MutantMotion.BOIL_OVER_OUT_MS)
        }
    }

    /** Whether the pause holds (D7): a heat above where it is going then lands there at once (B-176). */
    private var held = false

    /**
     * Follows the pause from composition. While it holds, a release or a leave cools the body in the same frame, and a
     * heat still cooling when it starts lands: the body shows released for as long as the pause lasts, and has nothing to
     * cool when it lifts. A press under it does not boil, as the clock stands: the surface shows it as a still frame at heat
     * 1 ([mutantPausedPressFill], B-190); a press held across it keeps its heat (B-176).
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
        val t = target
        if (at(now) > t) {
            from = t
            since = now
        }
    }

    fun press(now: Double) {
        if (on) return
        from = at(now)
        since = now
        on = true
    }

    fun release(now: Double) {
        if (!on) return
        from = at(now)
        since = now
        on = false
        if (held) settle(now)
    }

    fun hover(now: Double) {
        if (hovered) return
        from = at(now)
        since = now
        hovered = true
    }

    fun leave(now: Double) {
        if (!hovered) return
        from = at(now)
        since = now
        hovered = false
        if (held) settle(now)
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

/** A hovered body's heat (designed here, B-63): a simmer the eye catches, a third of a press's boil. */
internal const val HOVER_HEAT: Double = .3
