package io.github.youndie.mutant.gen

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * One block of a keyframe loss (the engine's `moshStart` run): its place and size in dp on the scene, how far it pulls
 * the picture each step ([vx], [vy]), the share of the loss before it starts ([start]), whether it is cut out of the
 * snapshot ([gone], half the blocks of the band's first and last rows) and its tint (0 magenta, 1 acid, 2 cyan, -1
 * none), laid once over it in `screen` at 0.3.
 */
internal class MoshRun(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val vx: Int,
    val vy: Int,
    val start: Double,
    val gone: Boolean,
    val tint: Int,
)

/**
 * A keyframe loss's plan for a scene [width] × [height] dp: a band of [rows] rows of 16 dp from [y0], its blocks, and
 * how long the blocks pull before the band recovers ([durationMs], 900–1400 ms).
 */
internal class MoshPlan(
    val y0: Int,
    val rows: Int,
    val runs: List<MoshRun>,
    val durationMs: Double,
)

/**
 * The engine's `moshStart`, its numbers only: the [n]th loss of a page seeded [seed] draws its band and blocks from the
 * stream `rng(seed + '/' + n, 'mosh')`, in the engine's order of calls. A block 16, 32, 48 or 64 dp wide, three in ten
 * split into two halves 8 dp tall; every block's pull is the band's (`gvx` ±4, `gvy` 3–6) give or take ±3 and ±2.5.
 */
internal fun moshPlan(
    seed: String,
    n: Int,
    width: Int,
    height: Int,
): MoshPlan {
    val r = Mulberry32("$seed/$n", "mosh")
    val nr = height / ROW
    val rows = min(nr, 9 + floor(r.next() * 10).toInt())
    val y0 = floor(r.next() * (nr - rows + 1)).toInt() * ROW
    val gvx = jsRound((r.next() - .5) * 8).toInt()
    val gvy = 3 + floor(r.next() * 4).toInt()
    val runs = ArrayList<MoshRun>()
    for (row in 0 until rows) {
        var x = 0
        while (x < width) {
            val bw = min(width - x, ROW * (1 + floor(r.next() * 4).toInt()))
            val edge = row == 0 || row == rows - 1
            val halves = if (r.next() < .3) intArrayOf(0, 8) else intArrayOf(0)
            for (o in halves) {
                val vx = gvx + jsRound((r.next() - .5) * 6).toInt()
                val vy = max(1, gvy + jsRound((r.next() - .5) * 5).toInt())
                val start = r.next() * .6
                val gone = edge && r.next() < .5
                val tint = if (r.next() < .1) floor(r.next() * 3).toInt() else -1
                runs += MoshRun(x, y0 + row * ROW + o, bw, if (halves.size > 1) 8 else ROW, vx, vy, start, gone, tint)
            }
            x += bw
        }
    }
    return MoshPlan(y0, rows, runs, 900 + r.next() * 500)
}

/** A band's row and a block's height unit (16 dp). */
private const val ROW = 16
