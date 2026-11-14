package io.github.youndie.mutant.surface

import kotlin.test.Test
import kotlin.test.assertEquals

/** B-12: transitions step at 83.33 ms from the event (`meltStep`, `drawItem`), and a pour lasts `clamp(260 + 5h, 300, 620)`. */
class TransitionTimingTest {
    @Test
    fun `progress moves in whole steps of 83_33 ms and stops at 1`() {
        assertEquals(0.0, stepped(83.32, 330.0))
        assertEquals(83.33 / 330, stepped(83.33, 330.0), 1e-12)
        assertEquals(2 * 83.33 / 330, stepped(170.0, 330.0), 1e-12)
        assertEquals(1.0, stepped(400.0, 330.0))
        assertEquals(0.0, stepped(-5.0, 330.0))
    }

    @Test
    fun `a pour lasts 260 plus 5 per dp of height and stays between 300 and 620 ms`() {
        assertEquals(300.0, pourDuration(8.0))
        assertEquals(420.0, pourDuration(32.0))
        assertEquals(620.0, pourDuration(80.0))
    }
}
