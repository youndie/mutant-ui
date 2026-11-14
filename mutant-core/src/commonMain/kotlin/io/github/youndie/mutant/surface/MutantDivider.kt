package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.youndie.mutant.gen.Mulberry32
import io.github.youndie.mutant.gen.jsRound
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.tokens.MutantColors

/**
 * A tracking divider (`data-mu-divider`, the engine's `buildStage`): 2 dp runs of the no-signal blue, 6–52 dp
 * each with gaps of 2–20, three in ten of the longer ones ending in a comet of four fading strokes. Seeded by
 * [seed] — the engine's `seed/stage/div<index>`, here the kit's own key in its place (D16).
 */
internal fun DrawScope.drawTrackingDivider(
    seed: String,
    w: Float,
    h: Float,
    colors: MutantColors,
) {
    val q = Mulberry32(seed, "d")
    val y = jsRound(h / 2.0).toFloat() - 1
    var x = 0.0
    while (x < w) {
        val len = minOf(w - x, 6 + q.next() * 46)
        val comet = q.next() < .3 && len > 20
        drawRect(colors.noSignal, Offset(jsToFixed(x, 1).toFloat(), y), Size(jsToFixed(len, 1).toFloat(), 2f))
        if (comet) {
            for (i in 0 until 4) {
                val cx = x + len - 4 - i * 6
                drawRect(
                    colors.comet,
                    Offset(jsToFixed(cx, 1).toFloat(), y),
                    Size(
                        if (i ==
                            0
                        ) {
                            4f
                        } else {
                            6f
                        },
                        2f,
                    ),
                    alpha = COMET[i],
                )
            }
        }
        x += len + 2 + q.next() * q.next() * 18
    }
}

/** The comet's four strokes, head first. */
private val COMET = floatArrayOf(1f, .55f, .3f, .14f)
