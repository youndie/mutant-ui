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

/**
 * How long a finger's press on a body inside a scrolling container waits before its heat starts (B-189, the designer's
 * answer 11): Android's tap timeout, `ViewConfiguration.getTapTimeout()`, which Compose's own `clickable` waits there on
 * Android only. A gesture that becomes a scroll within it never heats the body.
 */
internal const val HEAT_HOLD_IN_SCROLL_MS: Long = 100L

/**
 * Whether Compose's `clickable` already holds a finger's press inside a scrolling container for the tap timeout
 * (`TapIndicationDelay`): on Android only. It is 0 on the desktop, iOS and in the browser (CMP 1.12.1), where the
 * surface holds the heat itself ([MutantPressHold]); on Android a second hold would double it.
 */
internal expect val composeHoldsPressInScroll: Boolean

/**
 * What a surface knows of a press when it arrives (B-189): whether a finger (any pointer but a mouse) is down on the
 * body, and whether the body stands inside a scrolling container. Compose reads the latter through its own gesture
 * nodes (`hasWaitingParent`, an ancestor drag waiting for the slop), which are internal to foundation, as is the
 * `isInScrollableContainer` that came before them; the surface reads it from what is public: an item of a lazy layout
 * ([inLazyItem], Compose's `LocalPinnableContainer`), or a layout showing less than its content along an axis
 * ([mutantInsideScroll]), as `verticalScroll` and `horizontalScroll` do once the content overflows them. A mouse and a
 * key never start a scroll (a scrollable's drag takes no mouse), so their press heats at once, as Compose's does.
 */
internal class MutantPressHold {
    /** The body's coordinates, set where it is placed. */
    var coords: androidx.compose.ui.layout.LayoutCoordinates? = null

    /** The body stands in an item of a lazy layout (a lazy list, a grid, a pager). */
    var inLazyItem: Boolean = false

    /** A finger (any pointer but a mouse) is down on the body: read in the pointer's first pass, before any press. */
    var finger: Boolean = false

    /** Whether a press arriving now waits [HEAT_HOLD_IN_SCROLL_MS] before it heats the body. */
    fun holds(): Boolean =
        !composeHoldsPressInScroll && finger &&
            (inLazyItem || coords?.takeIf { it.isAttached }?.mutantInsideScroll() == true)
}

/**
 * Whether these coordinates stand inside a container that can scroll: an ancestor showing less than its content along
 * an axis, as `verticalScroll` and `horizontalScroll` lay theirs out once it overflows them. A container whose content
 * fits cannot scroll, and its press is not held. The same reading as field-ui's B-93.
 */
internal fun androidx.compose.ui.layout.LayoutCoordinates.mutantInsideScroll(): Boolean {
    var child = this
    var parent = child.parentCoordinates
    while (parent != null) {
        if (child.size.height > parent.size.height || child.size.width > parent.size.width) return true
        child = parent
        parent = child.parentCoordinates
    }
    return false
}

/** Tells [hold] whether a finger is down on the body. It consumes nothing and shares the pointer with its siblings. */
internal fun androidx.compose.ui.Modifier.mutantPressHoldWatch(hold: MutantPressHold): androidx.compose.ui.Modifier =
    this then MutantPressHoldElement(hold)

private class MutantPressHoldElement(
    val hold: MutantPressHold,
) : androidx.compose.ui.node.ModifierNodeElement<MutantPressHoldNode>() {
    override fun create() = MutantPressHoldNode(hold)

    override fun update(node: MutantPressHoldNode) {
        node.hold = hold
    }

    override fun equals(other: Any?) = other is MutantPressHoldElement && other.hold === hold

    override fun hashCode() = hold.hashCode()
}

private class MutantPressHoldNode(
    var hold: MutantPressHold,
) : androidx.compose.ui.Modifier.Node(),
    androidx.compose.ui.node.PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: androidx.compose.ui.input.pointer.PointerEvent,
        pass: androidx.compose.ui.input.pointer.PointerEventPass,
        bounds: androidx.compose.ui.unit.IntSize,
    ) {
        if (pass != androidx.compose.ui.input.pointer.PointerEventPass.Initial) return
        hold.finger =
            pointerEvent.changes.any { it.pressed && it.type != androidx.compose.ui.input.pointer.PointerType.Mouse }
    }

    override fun onCancelPointerInput() {
        hold.finger = false
    }

    override fun sharePointerInputWithSiblings(): Boolean = true
}
