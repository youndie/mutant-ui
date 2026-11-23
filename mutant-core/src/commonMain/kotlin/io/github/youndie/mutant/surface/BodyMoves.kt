package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.pourFront
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * A request to slide a body home from [dx] dp, the old place, the previous request's home; a new [id] starts a new
 * slide. A request while a slide runs starts from where the body is drawn, its stretch included (B-171). [run] is the
 * slide's state where its holder reads it too (the navigation bar's pill, the calendar's drop); null, the body keeps
 * its own.
 */
internal class SlideRequest(
    val id: Any,
    val dx: Double,
    /** Down from the old place to the new one, dp: a calendar's day in another row (B-93). */
    val dy: Double = 0.0,
    val run: MutantSlideRun? = null,
)

/**
 * A body's slide home (`slid`): from [dx], [dy] dp at [since], eased home by `1 − (1 − p)²`, stretched along its run by
 * `sin πp`. Re-aimed while it runs, it starts the new run from where the body is drawn and eases the stretch it shows
 * down to the new run's own, so nothing jumps in the re-aim's frame (B-171).
 */
internal class MutantSlideRun {
    var dx: Double = 0.0
        private set
    var dy: Double = 0.0
        private set
    var since: Double = Double.NEGATIVE_INFINITY
        private set

    /** The stretch shown when the run was re-aimed, as a change of each axis's scale from 1; it eases out by `1 − p`. */
    private var carryX = 0.0
    private var carryY = 0.0

    /** The run's progress now, 1 when done or under reduce motion. */
    fun progress(
        now: Double,
        instant: Boolean,
        steps: Boolean,
    ): Double = if (instant) 1.0 else stepped(now - since, MutantMotion.SLIDE_MS, steps)

    /** A body that does not run: it stands home, [dx] and [dy] kept for the next re-aim to start from. */
    fun land(
        dx: Double,
        dy: Double,
    ) {
        this.dx = dx
        this.dy = dy
        since = Double.NEGATIVE_INFINITY
        carryX = 0.0
        carryY = 0.0
    }

    /** A new run from [dx], [dy] dp away (from the old home), started at [now] from where the body is drawn then. */
    fun aim(
        dx: Double,
        dy: Double,
        now: Double,
        instant: Boolean,
        steps: Boolean,
    ) {
        val p = progress(now, instant, steps)
        if (p < 1) {
            val left = (1 - p) * (1 - p)
            val sx = stretchX(p)
            val sy = stretchY(p)
            this.dx = dx + this.dx * left
            this.dy = dy + this.dy * left
            carryX = sx
            carryY = sy
        } else {
            this.dx = dx
            this.dy = dy
            carryX = 0.0
            carryY = 0.0
        }
        since = now
    }

    /** Whether the run is more down than across: it then stretches tall (B-93). */
    val down: Boolean get() = abs(dy) > abs(dx)

    /** How far from home the body is drawn across at [p], dp, unrounded: `dx · (1 − p)²`. */
    fun offsetX(p: Double): Double = if (p >= 1) 0.0 else dx * (1 - (1 - (1 - p) * (1 - p)))

    /** How far from home the body is drawn down at [p], dp, unrounded. */
    fun offsetY(p: Double): Double = if (p >= 1) 0.0 else dy * (1 - (1 - (1 - p) * (1 - p)))

    /** The change of the horizontal scale from 1 at [p]: the run's own `sin πp` stretch, or what is left of the carried one. */
    fun stretchX(p: Double): Double = if (p >= 1) 0.0 else slideStretchJoin(own(p, !down), carryX * (1 - p))

    /** The change of the vertical scale from 1 at [p]. */
    fun stretchY(p: Double): Double = if (p >= 1) 0.0 else slideStretchJoin(own(p, down), carryY * (1 - p))

    /** The run's own stretch on an axis at [p]: `.4 sin πp` along the run, `−.18 sin πp` across it (the engine's). */
    private fun own(
        p: Double,
        along: Boolean,
    ): Double {
        val q = sin(PI * p)
        return if (along) .4 * q else -.18 * q
    }
}

/**
 * One axis's stretch from the run's own [own] and the carried [carried]: the larger where both stretch the same way (so
 * a re-aim never stretches past the design's 1.4), their sum where they pull apart. Which applies stays the same through
 * a run, so the result is continuous; with nothing carried it is [own] to the bit.
 */
private fun slideStretchJoin(
    own: Double,
    carried: Double,
): Double =
    if (own * carried >= 0) {
        if (abs(own) >= abs(carried)) own else carried
    } else {
        own + carried
    }

/**
 * A colour shown above a pour's front frozen at [p]: the band an interrupted pour had poured when the fill changed again
 * (B-171). Its front's path and its depth under the label are kept for the body's instance they were made for.
 */
internal class MutantPourBand(
    val color: Color,
    val p: Double,
) {
    private var madeFor: Instance? = null
    private var path: Path? = null
    private var depth = 0.0

    private fun make(g: Instance) {
        if (madeFor === g) return
        val front = MutantGenerator().pourFront(g, p)
        path = front.toPath()
        depth = front.depthUnderLabel(g.slot.ox, g.slot.oy, g.slot.w)
        madeFor = g
    }

    fun path(g: Instance): Path {
        make(g)
        return path!!
    }

    fun depthUnderLabel(g: Instance): Double {
        make(g)
        return depth
    }
}

/**
 * The picture a pour runs down over: [base] under all, each band's colour above its front, the later bands over the
 * earlier (B-171). A pour from a resting body runs over its plain fill, a band-less picture.
 */
internal class MutantPourUnder(
    val base: Color,
    val bands: List<MutantPourBand> = emptyList(),
) {
    /** The colour under a label at the middle of [g]'s height: the latest band whose front has passed it, else the base. */
    fun underLabel(g: Instance?): Color {
        if (g == null) return base
        for (i in bands.indices.reversed()) {
            if (bands[i].depthUnderLabel(g) >= g.slot.h / 2) return bands[i].color
        }
        return base
    }
}

/**
 * At most this many bands under a pour: a fifth change inside one pour's time merges the oldest band into the base, the
 * one jump left (B-171).
 */
private const val POUR_BANDS_MAX = 4

/** Follows a body's fill and slide requests from composition without writing state; a change starts a move then. */
internal class BodyMoves {
    private var fill: Color? = null
    private var slideId: Any? = null
    private val ownRun = MutantSlideRun()

    /** What the running pour pours over; null before the first change. */
    var pourUnder: MutantPourUnder? = null
        private set
    var pourSince: Double = Double.NEGATIVE_INFINITY
        private set

    /** The slide: the request's own run where it brings one, else the body's. */
    var slideRun: MutantSlideRun = ownRun
        private set

    fun follow(
        newFill: Color,
        slide: SlideRequest?,
        now: Double,
        h: Double? = null,
        steps: Boolean = false,
        instant: Boolean = false,
    ) {
        val old = fill
        if (old != null && old != newFill) {
            // B-171: the new fill pours over the picture the body shows, not over one plain colour: a change while a
            // pour runs keeps what that pour had poured, a band of its fill above its front where it stood.
            val under = pourUnder
            val p =
                when {
                    under == null || instant -> 1.0

                    // Not yet drawn: the pour has not started, and the body shows what it poured over.
                    h == null -> 0.0

                    else -> stepped(now - pourSince, pourDuration(h), steps)
                }
            pourUnder =
                when {
                    under == null || p >= 1 -> {
                        MutantPourUnder(old)
                    }

                    p <= 0 -> {
                        under
                    }

                    else -> {
                        // A band whose front the pour has passed is covered: the front only runs down as p grows.
                        val bands = under.bands.filter { it.p > p } + MutantPourBand(old, p)
                        if (bands.size > POUR_BANDS_MAX) {
                            MutantPourUnder(bands[0].color, bands.drop(1))
                        } else {
                            MutantPourUnder(under.base, bands)
                        }
                    }
                }
            pourSince = now
        }
        fill = newFill
        if (slide != null && slide.id != slideId) {
            val run = slide.run ?: ownRun
            if (slideId == null || run !== slideRun) {
                // The first request: the body stands where it is laid out.
                run.land(slide.dx, slide.dy)
            } else {
                run.aim(slide.dx, slide.dy, now, instant, steps)
            }
            slideRun = run
            slideId = slide.id
        }
    }
}
