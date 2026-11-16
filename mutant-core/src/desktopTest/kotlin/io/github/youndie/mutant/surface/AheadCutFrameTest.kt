package io.github.youndie.mutant.surface

import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-97: a frame built ahead, off the frame's thread, never grows the field's grid. One that came out cut at the grid's
 * edge is not taken, so the frame builds it again where the grid grows; a whole one is taken.
 */
class AheadCutFrameTest {
    @Test
    fun `a frame built ahead and cut at its grid's edge is not taken, a whole one is`() {
        val p = MutantBody.Blob.profile
        val slot =
            Slot(
                ox = 0.0,
                oy = 0.0,
                w = 376.0,
                h = 271.0,
                r = 22.0,
                over = 0.0,
                dripShare = p.dripShare,
                wobble = 1.6,
                round = p.round,
                calm = 1.0,
                dead = p.deadPixels,
                marks = false,
                edge = p.mosaic,
                textZone = doubleArrayOf(-99.0, -99.0, 0.0, 0.0),
                cleanZone = doubleArrayOf(0.0, 0.0, 0.0, 0.0),
            )
        val gen = MutantGenerator("5")
        val g = gen.instance(slot, "demo-share")
        val cut = gen.frame(g, FieldCache(), 0.0, grow = false)
        val whole = gen.frame(g, FieldCache(), 0.0)
        assertTrue(cut.field.cut, "on the engine's grid the sheet reaches the edge")
        assertFalse(whole.field.cut, "grown, it does not")
        val ahead = FrameAhead()
        ahead.preset(g, 0.0, Ahead(cut, null, null))
        assertNull(ahead.take(g, 0.0), "the cut frame is not taken")
        ahead.preset(g, 0.0, Ahead(whole, null, null))
        assertNotNull(ahead.take(g, 0.0), "the whole frame is")
    }
}
