package io.github.youndie.mutant.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Where a body stands against the screen (B-87): [SHOWN] when its box, [OFF_SCREEN_REACH] wider all round, meets every
 * box it is laid out in — each layout and layout modifier up to the window; [OFF_WINDOW] when it misses the window
 * itself; [OFF_BOX] when it misses only a box inside it, which may or may not clip it. [flips] counts the changes
 * [place] saw, and invalidates the body's drawing.
 */
@androidx.compose.runtime.Stable
internal class BodyView {
    var flips by mutableIntStateOf(0)
        private set
    private var where = SHOWN

    /** After a layout: a change of where the body stands asks for its drawing again. */
    fun place(
        at: androidx.compose.ui.layout.LayoutCoordinates,
        reach: Float,
    ) {
        val now = at.standing(reach)
        // B-86: scrolled past the part of a tall body its image covers, the body draws again, as vectors until the next
        // tick's image covers where it now stands.
        val uncovered = drawn?.let { !it.covers(windowIn(at)) } ?: false
        if (now != where || uncovered) {
            where = now
            drawn = null
            flips++
        }
    }

    /** The image the body last drew, or null when it drew vectors (B-86). */
    var drawn: RasterisedBody? = null

    /** At a draw: where the body stands now, read from where it was last laid out. */
    fun whereNow(
        at: androidx.compose.ui.layout.LayoutCoordinates?,
        reach: Float,
    ): Int = (if (at == null || !at.isAttached) SHOWN else at.standing(reach)).also { where = it }
}

/**
 * The window's bounds in [at]'s own pixels (B-86): what of a box can show, whatever the boxes between clip. Null when
 * the box is no longer attached.
 */
internal fun windowIn(at: LayoutCoordinates): Rect? {
    if (!at.isAttached) return null
    var root = at
    while (true) root = root.parentCoordinates ?: break
    val w = root.size.width.toFloat()
    val h = root.size.height.toFloat()
    val corners = listOf(Offset.Zero, Offset(w, 0f), Offset(0f, h), Offset(w, h)).map { at.localPositionOf(root, it) }
    return Rect(corners.minOf { it.x }, corners.minOf { it.y }, corners.maxOf { it.x }, corners.maxOf { it.y })
}

/**
 * What a tall body rasterises ahead (B-86): the window, half its height and half its width more on each side, in the
 * surface's pixels, so a tick's image still covers the screen after a scroll of half a window.
 */
internal fun bandAround(window: Rect): Rect =
    Rect(
        window.left - window.width * BAND_SPARE,
        window.top - window.height * BAND_SPARE,
        window.right + window.width * BAND_SPARE,
        window.bottom + window.height * BAND_SPARE,
    )

/** How much of the window, on each side, a tall body's image covers past it (B-86). */
private const val BAND_SPARE = .5f

internal const val SHOWN = 0
internal const val OFF_BOX = 1
internal const val OFF_WINDOW = 2

/** Where this box, [reach] wider all round, stands against each box it is laid out in, up to the window. */
internal fun androidx.compose.ui.layout.LayoutCoordinates.standing(reach: Float): Int {
    var c = this
    var a = Offset(-reach, -reach)
    var b = Offset(size.width + reach, size.height + reach)
    var missed = false
    while (true) {
        val p = c.parentCoordinates ?: return if (missed) OFF_BOX else SHOWN
        a = p.localPositionOf(c, a)
        b = p.localPositionOf(c, b)
        val w = p.size.width + reach
        val h = p.size.height + reach
        val outside =
            maxOf(a.x, b.x) < -reach || maxOf(a.y, b.y) < -reach || minOf(a.x, b.x) > w || minOf(a.y, b.y) > h
        if (outside && p.parentCoordinates == null) return OFF_WINDOW
        missed = missed || outside
        c = p
    }
}
