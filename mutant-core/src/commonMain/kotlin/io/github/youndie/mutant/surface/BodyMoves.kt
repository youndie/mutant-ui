package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Color

/** A request to slide a body home from [dx] dp; a new [id] starts a new slide. */
internal class SlideRequest(
    val id: Any,
    val dx: Double,
    /** Down from the old place to the new one, dp: a calendar's day in another row (B-93). */
    val dy: Double = 0.0,
)

/** Follows a body's fill and slide requests from composition without writing state; a change starts a move then. */
internal class BodyMoves {
    private var fill: Color? = null
    private var slideId: Any? = null
    var pourFrom: Color? = null
        private set
    var pourSince: Double = Double.NEGATIVE_INFINITY
        private set
    var slideDx: Double = 0.0
        private set
    var slideDy: Double = 0.0
        private set
    var slideSince: Double = Double.NEGATIVE_INFINITY
        private set

    fun follow(
        newFill: Color,
        slide: SlideRequest?,
        now: Double,
        h: Double? = null,
        steps: Boolean = false,
    ) {
        val old = fill
        if (old != null && old != newFill) {
            // A change back within a pour's first half pours from what still covers most of the body, the pour's own
            // old fill, not from the fill it was pouring in: a body never jumps to a colour it was not showing (B-94).
            val from = pourFrom
            val early = from != null && h != null && stepped(now - pourSince, pourDuration(h), steps) < .5
            pourFrom = if (early) from else old
            pourSince = now
        }
        fill = newFill
        if (slide != null && slide.id != slideId) {
            if (slideId != null || slideSince == Double.NEGATIVE_INFINITY) {
                slideDx = slide.dx
                slideDy = slide.dy
                slideSince = if (slideId == null) Double.NEGATIVE_INFINITY else now
            }
            slideId = slide.id
        }
    }
}
