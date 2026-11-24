package io.github.youndie.mutant.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A modal panel dragged shut by a finger or a mouse: the drawer and the bottom sheet (B-183, the designer's answer 5).
 *
 * Where the panel stands is a distance towards closed, in pixels: 0 open, [size] closed, below 0 pulled past open. Held,
 * it stands where the pointer puts it, 1:1 from the pointer's first move and unquantised, caught where it was drawn at the
 * press (mid-slide, mid-settle); past open it moves [PANEL_DRAG_RESIST] of the pointer, at most [PANEL_DRAG_OVER_DP].
 * Let go past [PANEL_DRAG_CLOSE_SHARE] of its size or faster than [PANEL_DRAG_CLOSE_SPEED] towards closed, it asks to be
 * dismissed ([closes]); a panel still visible after that settles back. Both glides take the slide token's 330 ms on the
 * slide curve, `(1 − p)²` of the way left, as the slider's handle (B-182), on every frame or in the hot tick's steps
 * under `steppedMotion` (D22), and land at once while the clock stands (D24). A panel turned to close while held or
 * settling closes from where it stands; reopened while it closes, it settles back from there.
 *
 * The gesture ([panelDrag]) reads the pointer itself, a finger and a mouse alike (cmp-ui-component §4): a mouse drag
 * scrolls nothing in Compose, and `draggable` would hold the panel through the slop and then jump by all of it.
 */
@Stable
internal class PanelDrag {
    /** The panel's extent along its way, px, from its layout. */
    var size = 0f

    /** [PANEL_DRAG_OVER_DP] in pixels, from composition. */
    var over = 0f

    /** Where the panel's scripted slide stands, a share of [size] towards closed, from composition. */
    var scripted = 0f

    /** Where the pointer puts the panel, px towards closed, while held; null when no pointer holds it. */
    private var held by mutableStateOf<Float?>(null)

    /** The pointer has moved past the slop: the drag is the gesture's, and the panel's body boils. */
    var taken by mutableStateOf(false)
        private set

    /** Where the pointer took it, as the pointer's own distance (before the resistance). */
    private var base = 0f

    /** Where it was last drawn, px towards closed. */
    private var drawn = 0f

    private var phase = PanelDragPhase.Idle
    private var from = 0f
    private var since = 0.0
    private var visible = true

    /** From the gesture: a press. The panel is caught where it was last drawn. */
    fun press() {
        val at = if (phase == PanelDragPhase.Idle) scripted * size else drawn
        base = if (at < 0) at / PANEL_DRAG_RESIST else at
    }

    /** From the gesture: the pointer [travel]led this far towards closed since the press, px. */
    fun move(travel: Float) {
        held = resist(base + travel)
    }

    fun take() {
        taken = true
    }

    /** Where a pointer or a content's scroll holds the panel, px towards closed; null when nothing holds it. */
    val heldAt: Float? get() = held

    /** How far the content's scrolls have pulled the panel since they took it, px towards closed (B-184). */
    private var pulled = 0f

    /**
     * From the sheet's nested scroll (B-184): the content at its top handed [delta] px of a finger's scroll towards closed
     * to the panel (negative: back towards open). The first pull catches the panel where it was drawn, as a press does,
     * and takes it: it follows 1:1 from there and boils.
     */
    fun pull(delta: Float) {
        if (held == null) {
            press()
            pulled = 0f
            take()
        }
        pulled += delta
        move(pulled)
    }

    /** From the gesture: let go, at [velocity] px/s towards closed. Whether the panel asks to close, at [density]. */
    fun release(
        velocity: Float,
        density: Float,
    ): Boolean {
        val at = held
        held = null
        val was = taken
        taken = false
        if (!was || at == null || size <= 0f) return false
        return at / size > PANEL_DRAG_CLOSE_SHARE || velocity > PANEL_DRAG_CLOSE_SPEED * density
    }

    /** From the gesture's end, however it ended: nothing holds the panel any more. */
    fun drop() {
        held = null
        taken = false
    }

    private fun resist(x: Float): Float = if (x >= 0) min(x, size) else max(-over, x * PANEL_DRAG_RESIST)

    private fun progress(
        now: Double,
        steps: Boolean,
    ): Double = stepped(now - since, MutantMotion.SLIDE_MS, steps)

    private fun glideAt(
        now: Double,
        steps: Boolean,
    ): Float {
        val target = if (phase == PanelDragPhase.Close) size else 0f
        val left = progress(now, steps).let { (1 - it) * (1 - it) }.toFloat()
        return target + (from - target) * left
    }

    private fun start(
        to: PanelDragPhase,
        now: Double,
    ) {
        phase = to
        from = drawn
        since = now
    }

    /**
     * From composition: where the panel stands at [now], a share of [size] towards closed, or null where its scripted
     * slide alone places it. [still] lands a glide at once.
     */
    fun follow(
        visible: Boolean,
        now: Double,
        still: Boolean,
        steps: Boolean,
    ): Float? {
        val h = held
        val turned = visible != this.visible
        this.visible = visible
        when {
            h != null && visible -> {
                phase = PanelDragPhase.Held
                drawn = h
            }

            phase == PanelDragPhase.Held -> {
                start(if (visible) PanelDragPhase.Settle else PanelDragPhase.Close, now)
            }

            turned && phase == PanelDragPhase.Settle && !visible -> {
                start(PanelDragPhase.Close, now)
            }

            turned && phase == PanelDragPhase.Close && visible -> {
                start(PanelDragPhase.Settle, now)
            }
        }
        if (phase == PanelDragPhase.Settle || phase == PanelDragPhase.Close) {
            if (still || progress(now, steps) >= 1) {
                phase = PanelDragPhase.Idle
            } else {
                drawn = glideAt(now, steps)
            }
        }
        if (phase == PanelDragPhase.Idle || size <= 0f) return null
        return drawn / size
    }

    /** A glide is under way: the panel reads every frame. */
    val gliding: Boolean get() = phase == PanelDragPhase.Settle || phase == PanelDragPhase.Close

    /** The panel is closing by a drag's glide: it stays composed until the glide is over. */
    val closing: Boolean get() = phase == PanelDragPhase.Close
}

private enum class PanelDragPhase { Idle, Held, Settle, Close }

/**
 * The gesture that drags [drag]: from any press on the node, a finger or a mouse, along one axis. [towardsClosed] turns
 * the pointer's move into a distance towards closed, px. Past the slop the gesture consumes the moves, so a press under it
 * (a row, the scrim) is cancelled. Moves are seen before the node's content: one past the slop towards closed is the
 * panel's before a scroll inside can take it (B-184); otherwise a move a child consumed first (a scroll inside) is the
 * child's, and the panel goes back. A press while [atTop] answers false (the panel's content scrolled away from its top)
 * leaves the whole gesture to the content; while [overPull] answers false (the content scrolls), a move towards open is
 * not followed until it is the panel's, so a scroll up does not lift the panel through the slop first. Let go past the
 * drag's limits, it calls [dismiss]. Only while [enabled].
 */
internal fun Modifier.panelDrag(
    drag: PanelDrag,
    enabled: State<Boolean>,
    dismiss: State<() -> Unit>,
    towardsClosed: State<(Offset) -> Float>,
    atTop: () -> Boolean = { true },
    overPull: () -> Boolean = { true },
): Modifier =
    pointerInput(drag, atTop, overPull) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!enabled.value || !atTop()) return@awaitEachGesture
            val tracker = VelocityTracker()
            tracker.addPointerInputChange(down)
            drag.press()
            val slop = viewConfiguration.touchSlop
            var closes = false
            try {
                while (true) {
                    // Seen before the content: past the slop towards closed, the drag is the panel's, and consumed here
                    // a scroll inside never starts (B-184).
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (!enabled.value) break
                    if (c.changedToUpIgnoreConsumed()) {
                        tracker.addPointerInputChange(c)
                        val v = tracker.calculateVelocity()
                        val along = towardsClosed.value(Offset(v.x, v.y))
                        closes = drag.release(if (along.isNaN()) 0f else along, density)
                        break
                    }
                    tracker.addPointerInputChange(c)
                    val travel = towardsClosed.value(c.position - down.position)
                    drag.move(if (travel < 0f && !drag.taken && !overPull()) 0f else travel)
                    if (!drag.taken && travel >= slop) drag.take()
                    if (!drag.taken) {
                        // Towards open, or inside the slop: the content sees it first, and a move it consumed is its own.
                        val seen = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull { it.id == down.id }
                        if (seen == null || seen.isConsumed) break
                        if (abs(travel) >= slop) {
                            drag.take()
                            drag.move(travel)
                        }
                    }
                    if (drag.taken) c.consume()
                }
            } finally {
                drag.drop()
            }
            if (closes) dismiss.value()
        }
    }

/** Let go past this share of its size, the panel closes (the designer's answer 5). */
internal const val PANEL_DRAG_CLOSE_SHARE = .4f

/** Let go faster than this towards closed, dp a second, the panel closes (the designer's answer 5). */
internal const val PANEL_DRAG_CLOSE_SPEED = 800f

/** Past open the panel moves this share of the pointer's move (the designer's answer 5). */
internal const val PANEL_DRAG_RESIST = .3f

/** Past open the panel goes no further than this, dp (the designer's answer 5). */
internal const val PANEL_DRAG_OVER_DP = 24f
