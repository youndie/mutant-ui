package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B-189: which press waits [HEAT_HOLD_IN_SCROLL_MS] before it heats its body, decided in common code and run on every
 * test target (the desktop, the browser through Karma, iOS where its tests run). Android is held by Compose itself
 * and is built, not run.
 */
class PressHoldTest {
    /** A chain of layouts, the first the body's, each placed in the next. */
    private class Coords(
        override val size: IntSize,
        override val parentCoordinates: LayoutCoordinates?,
    ) : LayoutCoordinates {
        override val providedAlignmentLines: Set<AlignmentLine> get() = emptySet()
        override val parentLayoutCoordinates: LayoutCoordinates? get() = parentCoordinates
        override val isAttached: Boolean get() = true

        override fun windowToLocal(relativeToWindow: Offset) = relativeToWindow

        override fun localToWindow(relativeToLocal: Offset) = relativeToLocal

        override fun localToRoot(relativeToLocal: Offset) = relativeToLocal

        override fun localPositionOf(
            sourceCoordinates: LayoutCoordinates,
            relativeToSource: Offset,
        ) = relativeToSource

        override fun localBoundingBoxOf(
            sourceCoordinates: LayoutCoordinates,
            clipBounds: Boolean,
        ) = Rect.Zero

        override fun get(alignmentLine: AlignmentLine) = AlignmentLine.Unspecified
    }

    private fun chain(vararg sizes: Pair<Int, Int>): LayoutCoordinates =
        sizes.reversed().fold(null as LayoutCoordinates?) { parent, (w, h) -> Coords(IntSize(w, h), parent) }!!

    private val card = 260 to 80

    /** A card in a column of 640 shown through a viewport of 240: `verticalScroll`'s layout. */
    private val inScroll = chain(card, 260 to 640, 300 to 240, 1024 to 768)

    /** A card in a column that fits its box. */
    private val outside = chain(card, 260 to 80, 300 to 240, 1024 to 768)

    @Test
    fun `the hold is the tap timeout and Compose holds nothing itself off Android`() {
        assertEquals(100L, HEAT_HOLD_IN_SCROLL_MS)
        assertFalse(composeHoldsPressInScroll, "Compose's TapIndicationDelay is 0 on this target")
    }

    @Test
    fun `a layout showing less than its content is a scroll and one that fits it is not`() {
        assertTrue(inScroll.mutantInsideScroll(), "a column taller than its viewport")
        assertTrue(chain(card, 900 to 80, 300 to 80).mutantInsideScroll(), "a row wider than its viewport")
        assertFalse(outside.mutantInsideScroll(), "everything fits")
    }

    @Test
    fun `a finger press in a scroll or a lazy item is held and a mouse or key press or one outside is not`() {
        val hold = MutantPressHold()
        hold.coords = inScroll
        assertFalse(hold.holds(), "no finger down: a mouse or a key")
        hold.finger = true
        assertTrue(hold.holds(), "a finger in a scroll")
        hold.coords = outside
        assertFalse(hold.holds(), "a finger outside any scroll")
        hold.inLazyItem = true
        assertTrue(hold.holds(), "a finger in a lazy list's item")
        hold.coords = null
        assertTrue(hold.holds(), "a lazy item before it is placed")
        hold.inLazyItem = false
        assertFalse(hold.holds(), "nothing known of the body")
    }
}
