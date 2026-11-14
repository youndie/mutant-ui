package io.github.youndie.mutant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** B-74: the row's allowance is named by the macOS version it is for, and it lets the top go up, never down. */
class HandjetRowsAboveTest {
    @Test
    fun `only macOS 26 gets a row, every other host is held exactly`() {
        assertEquals(1, handjetRowsAbove("Mac OS X", "26.7.2"))
        assertEquals(0, handjetRowsAbove("Mac OS X", "27.0.1"))
        assertEquals(0, handjetRowsAbove("Mac OS X", "28.0"))
        assertEquals(0, handjetRowsAbove("Mac OS X", "15.6"))
        assertEquals(0, handjetRowsAbove("Linux", "26.1"))
        assertEquals(0, handjetRowsAbove("Mac OS X", null))
    }

    @Test
    fun `the allowance lets the kit's top lie a row higher, not lower and not two`() {
        val page = Extent(top = 77, bottom = 94, left = 45, right = 166)

        fun kit(top: Int) = page.copy(top = top)
        assertFalse(kit(76).near(page, px = 1, topPx = 0), "exact without the allowance")
        assertTrue(kit(76).near(page, px = 1, topPx = 0, above = 1), "a row higher with it")
        assertFalse(kit(75).near(page, px = 1, topPx = 0, above = 1), "two rows higher")
        assertFalse(kit(78).near(page, px = 1, topPx = 0, above = 1), "a row lower")
        assertTrue(kit(75).near(page, px = 1, topPx = 1, above = 1), "on top of a residual of its own")
        assertFalse(kit(79).near(page, px = 1, topPx = 1, above = 1), "the residual stays two-sided, the allowance not")
    }
}
