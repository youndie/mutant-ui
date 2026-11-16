package io.github.youndie.mutant.gen

import io.github.youndie.mutant.tokens.MutantBody
import kotlin.math.hypot
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-97: the bottom sheet's body (a blob 376 dp wide, r 22, wobble 1.6, no overhang) bulged past the field's grid, and
 * its contour broke into open chains whose joins crossed the body: under the even-odd fill, a wedge-shaped hole. At
 * every height a sheet takes and for several seeds, the contour is now stitched only between neighbouring points.
 */
class TallBodyContourTest {
    /** The longest step between two consecutive end points of the contour's curves, dp. */
    private fun longestStitch(path: GenPath): Double {
        var longest = 0.0
        for (l in path.loops) {
            var px = l[0] / 10.0
            var py = l[1] / 10.0
            var i = 2
            while (i < l.size) {
                val x = l[i + 2] / 10.0
                val y = l[i + 3] / 10.0
                longest = maxOf(longest, hypot(x - px, y - py))
                px = x
                py = y
                i += 4
            }
            longest = maxOf(longest, hypot(l[0] / 10.0 - px, l[1] / 10.0 - py))
        }
        return longest
    }

    @Test
    fun `a sheet's body is stitched between neighbours at every height`() {
        val p = MutantBody.Blob.profile
        val gen = MutantGenerator("5")
        val bad = ArrayList<String>()
        var worst = 0.0
        for (key in listOf("demo-share", "sheet", "sample-sheet", "demo-timer-sheet")) {
            for (h in 120..440 step 10) {
                val slot =
                    Slot(
                        ox = 0.0,
                        oy = 0.0,
                        w = 376.0,
                        h = h.toDouble(),
                        r = min(22.0, h / 2.0),
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
                val g = gen.instance(slot, key)
                for (t in listOf(0.0, 125.0, 250.0, 375.0)) {
                    val s = longestStitch(gen.frame(g, FieldCache(), t).contour)
                    worst = maxOf(worst, s)
                    if (s > MAX_STITCH) bad += "$key, h $h, t $t: a stitch of ${"%.1f".format(s)} dp"
                }
            }
        }
        println("TALL worst stitch ${"%.1f".format(worst)} dp")
        assertTrue(bad.isEmpty(), bad.take(10).joinToString("\n") + "\n(${bad.size} in all)")
    }

    private companion object {
        /** A smoothed contour keeps a point every few dp; a join across the body is tens to hundreds. */
        const val MAX_STITCH = 20.0
    }
}
