package io.github.youndie.mutant.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.youndie.mutant.tokens.MutantMotion

/**
 * The boil's heat (the engine's `frame`): rising to 1 in 320 ms while pressed, cooling to 0 in 480 ms after,
 * from wherever it was. A pure function of the clock's time and the last press or release; only [on] is state.
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
    }
}

/** A hovered body's heat (designed here, B-63): a simmer the eye catches, a third of a press's boil. */
internal const val HOVER_HEAT: Double = .3
