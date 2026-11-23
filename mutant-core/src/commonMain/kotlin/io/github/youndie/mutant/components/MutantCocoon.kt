package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.Mulberry32
import io.github.youndie.mutant.gen.jsRound
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantCleanSignal
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The skeleton, «Кокон»: no body, a mosaic of 16 dp blocks edged by 8 dp ones in the shape it is given — the
 * component that is coming — with its corners rounded to 10 dp (or half its height). About 6 % of its blocks
 * change paint on each tick of the scene, eight times a second; under reduce motion and under «Чистый сигнал» it
 * stands. It takes the size its [modifier] gives and none of its own. Several on one screen each take their own
 * [key], or they flicker alike. Read as indeterminate progress.
 *
 * It hands over to what it stood for through [io.github.youndie.mutant.surface.MutantAppear]: `if (loading)
 * MutantCocoon(…) else MutantAppear { MutantCard(…) }` — the cocoon goes in one frame and the card tunes in (B-179).
 */
@Composable
public fun MutantCocoon(
    modifier: Modifier = Modifier,
    key: String = "0",
) {
    val clock = readMutantClock()
    val colors = MutantTheme.colors
    val clean = LocalMutantCleanSignal.current
    val stage = LocalMutantStage.current
    val seed = "${LocalMutantCassette.current}/$stage/coc$key"
    val mosaic = remember(seed, stage, key) { CocoonMosaic(seed, "$stage$key") }
    Spacer(
        modifier
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .drawBehind {
                val d = density
                val w = jsRound(size.width / d.toDouble()).toInt()
                val h = jsRound(size.height / d.toDouble()).toInt()
                // The tick is read here, in the draw, so a new tick redraws without recomposing.
                val tick = clock.sceneTick
                mosaic.at(w, h, if (clean) null else tick)
                scale(d, Offset.Zero) { drawCocoon(mosaic, w, h, colors) }
            },
    )
}

/**
 * One cocoon's blocks (the engine's `buildStage` and `stageStep`): laid out once per size from `seed`, then on each
 * new tick a few repainted from the stream `rng('coc', <stage><index>/<tick>)`. The paints build up tick on tick,
 * as the page's do; a tick is applied once, and a new size lays the blocks out again without re-applying the tick
 * it stands at (the engine's `st.fi` outlives a rebuild).
 */
internal class CocoonMosaic(
    private val seed: String,
    private val name: String,
) {
    /** x, y and side of each block, dp, in the order drawn. */
    var blocks: IntArray = IntArray(0)
        private set

    /** Each block's paint, an index into [TICK_PAINTS]. */
    var paints: IntArray = IntArray(0)
        private set

    private var w = -1
    private var h = -1
    private var tick: Long? = null

    fun at(
        w: Int,
        h: Int,
        tick: Long?,
    ) {
        if (w != this.w || h != this.h) layOut(w, h)
        if (tick != null && tick != this.tick) {
            this.tick = tick
            step(tick)
        }
    }

    private fun layOut(
        w: Int,
        h: Int,
    ) {
        this.w = w
        this.h = h
        val q = Mulberry32(seed, "c")
        val b = ArrayList<Int>()
        val p = ArrayList<Int>()
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val edge = x < BLOCK || y < BLOCK || x + 2 * BLOCK > w || y + 2 * BLOCK > h
                if (edge) {
                    for (i in 0 until 4) {
                        if (q.next() < EDGE_SHARE) {
                            b += x + (i and 1) * HALF
                            b += y + (i shr 1) * HALF
                            b += HALF
                            p += BUILD_PAINTS[floor(q.next() * BUILD_PAINTS.size).toInt()]
                        }
                    }
                } else if (q.next() < INNER_SHARE) {
                    b += x
                    b += y
                    b += BLOCK
                    p += BUILD_PAINTS[floor(q.next() * BUILD_PAINTS.size).toInt()]
                }
                x += BLOCK
            }
            y += BLOCK
        }
        blocks = b.toIntArray()
        paints = p.toIntArray()
    }

    private fun step(tick: Long) {
        val n = paints.size
        if (n == 0) return
        val r = Mulberry32("coc", "$name/$tick")
        repeat(ceil(max(MIN_CHANGES, n * CHANGE_SHARE)).toInt()) {
            val i = floor(r.next() * n).toInt()
            paints[i] = floor(r.next() * TICK_PAINTS).toInt()
        }
    }
}

internal fun DrawScope.drawCocoon(
    mosaic: CocoonMosaic,
    w: Int,
    h: Int,
    colors: MutantColors,
) {
    if (w <= 0 || h <= 0) return
    val r = min(CORNER, h / 2f)
    val clip = Path().apply { addRoundRect(roundRect(w.toFloat(), h.toFloat(), r)) }
    val paint = arrayOf(colors.cocoon1, colors.surface, colors.surfaceVariant, colors.cocoon2)
    clipPath(clip) {
        val b = mosaic.blocks
        for (i in mosaic.paints.indices) {
            val s = b[i * 3 + 2].toFloat()
            drawRect(paint[mosaic.paints[i]], Offset(b[i * 3].toFloat(), b[i * 3 + 1].toFloat()), Size(s, s))
        }
    }
}

private fun roundRect(
    w: Float,
    h: Float,
    r: Float,
) = androidx.compose.ui.geometry
    .RoundRect(0f, 0f, w, h, CornerRadius(r, r))

/** The 16 dp block and the 8 dp one at the edge: a block within 16 dp of the left or top, or 32 of the right or bottom. */
private const val BLOCK = 16
private const val HALF = 8

/** An edge quarter is drawn four times in five; an inner block 94 times in a hundred. */
private const val EDGE_SHARE = .8
private const val INNER_SHARE = .94

/**
 * The paints a block is laid out in — `coc1, surf, surf2, coc2, surf, coc1`, as indices into the four the ticks
 * repaint with (`coc1, surf, surf2, coc2`).
 */
private val BUILD_PAINTS = intArrayOf(0, 1, 2, 3, 1, 0)
private const val TICK_PAINTS = 4

/** A tick repaints 6 % of the blocks, two at least (the tokens' «около 6 % блоков Кокона за тик»). */
private const val CHANGE_SHARE = .06
private const val MIN_CHANGES = 2.0

/** The corner: 10 dp, or half the height of a thinner cocoon. */
private const val CORNER = 10f
