package io.github.youndie.mutant.surface

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** B-12: the venom pill's spring, at the moments the engine's formula is read. */
class VenomSpringTest {
    @Test
    fun `no spring before the pill is full or once it has died down`() {
        assertNull(venomSpring(.5, vertical = false))
        assertNull(venomSpring(1.0, vertical = false))
    }

    @Test
    fun `the spring sinks along the move and swells across it`() {
        // p = 0.84: s = 0.2, amp = 0.09 · sin(π / 2) · 0.8 = 0.072.
        assertEquals(.928f to 1.036f, venomSpring(.84, vertical = false))
        assertEquals(1.036f to .928f, venomSpring(.84, vertical = true))
    }
}
