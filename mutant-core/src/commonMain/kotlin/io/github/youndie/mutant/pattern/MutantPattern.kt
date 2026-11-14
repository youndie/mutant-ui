package io.github.youndie.mutant.pattern

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import io.github.youndie.mutant.gen.Mulberry32
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.tokens.MutantColors
import kotlin.math.floor

/**
 * The four seamless patterns for a screen's ground (the Icons page, «Узоры для Экрана»), each with a quiet
 * variant to put text on. They are drawn, not bundled, from the page's own `patterns()`, and never move.
 */
public enum class MutantPattern {
    /** Scanlines: one dark dp in three. */
    Lines,

    /** Snow: white strokes on a 64 dp tile. */
    Snow,

    /** A dot raster: a dot every 5 dp, quiet every 6. */
    Raster,

    /** Mosaic: 8 dp blocks of the cocoon's and the surfaces' paints on a 64 dp tile, quiet 16 dp at 55 %. */
    Mosaic,
}

/** One stroke or block of a tile: `x, y, w, h, alpha, colour index`. */
internal class PatternTile(
    val rects: List<FloatArray>,
    val paints: List<Color>,
)

/**
 * The tile of [pattern] for [colors], as the page builds it: snow from `rng('pattern', 'snow' or 'snowq')`,
 * 120 strokes (30 quiet) of 1 or 2 dp; mosaic from `rng('pattern', 'mosaic' or 'mosaicq')`, one paint per
 * block. Null for the patterns that are gradients, not tiles.
 */
internal fun patternTile(
    pattern: MutantPattern,
    quiet: Boolean,
    colors: MutantColors,
): PatternTile? {
    val r = Mulberry32("pattern", pattern.name.lowercase() + if (quiet) "q" else "")
    return when (pattern) {
        MutantPattern.Snow -> {
            PatternTile(
                List(if (quiet) 30 else 120) {
                    val w = if (r.next() < .2) 2 else 1
                    val x = floor(r.next() * (TILE - w))
                    val y = floor(r.next() * TILE)
                    val o = if (quiet) .08 + .1 * r.next() else .25 + .5 * r.next()
                    floatArrayOf(x.toFloat(), y.toFloat(), w.toFloat(), 1f, jsToFixed(o, 2).toFloat(), 0f)
                },
                listOf(if (colors.print) Color(0xFF14110F) else Color.White),
            )
        }

        MutantPattern.Mosaic -> {
            val b = if (quiet) 16 else 8
            val paints =
                if (quiet) {
                    listOf(colors.background, colors.surface, colors.background, colors.cocoon1)
                } else {
                    listOf(colors.cocoon1, colors.surface, colors.surfaceVariant, colors.cocoon2, colors.surface)
                }
            val rects = ArrayList<FloatArray>()
            for (y in 0 until TILE.toInt() step b) {
                for (x in 0 until TILE.toInt() step b) {
                    val i = floor(r.next() * paints.size).toInt()
                    rects +=
                        floatArrayOf(
                            x.toFloat(),
                            y.toFloat(),
                            b.toFloat(),
                            b.toFloat(),
                            if (quiet) PatternAlpha.MOSAIC_QUIET else 1f,
                            i.toFloat(),
                        )
                }
            }
            PatternTile(rects, paints)
        }

        else -> {
            null
        }
    }
}

/** Draws [pattern] over [size] (in dp, the scope already scaled), its ground the theme's background. */
internal fun DrawScope.drawPattern(
    pattern: MutantPattern,
    quiet: Boolean,
    colors: MutantColors,
    w: Float,
    h: Float,
    tile: PatternTile? = patternTile(pattern, quiet, colors),
) {
    drawRect(colors.background, Offset.Zero, Size(w, h))
    val print = colors.print
    when (pattern) {
        MutantPattern.Lines -> {
            val (ink, a) =
                if (print) {
                    Color(20, 17, 15) to (if (quiet) PatternAlpha.LINES_PRINT_QUIET else PatternAlpha.LINES_PRINT)
                } else {
                    Color.Black to (if (quiet) PatternAlpha.LINES_QUIET else PatternAlpha.LINES)
                }
            var y = 0f
            while (y < h) {
                drawRect(ink, Offset(0f, y), Size(w, 1f), alpha = a)
                y += 3f
            }
        }

        MutantPattern.Raster -> {
            val step = if (quiet) 6f else 5f
            val radius = if (quiet) .8f else 1.4f
            val a = if (quiet) (if (print) .16f else .1f) else (if (print) .5f else .3f)
            val ink = (if (print) Color(20, 17, 15) else Color(233, 242, 228)).copy(alpha = a)
            // The page's `radial-gradient(circle, ink 0 r, transparent r + .35)`: solid to r, fading over 0.35 dp.
            val outer = radius + RASTER_FADE
            var y = 0f
            while (y < h) {
                var x = 0f
                while (x < w) {
                    val c = Offset(x + step / 2, y + step / 2)
                    drawCircle(
                        Brush.radialGradient(
                            0f to ink,
                            radius / outer to ink,
                            1f to Color.Transparent,
                            center = c,
                            radius = outer,
                        ),
                        outer,
                        c,
                    )
                    x += step
                }
                y += step
            }
        }

        MutantPattern.Snow, MutantPattern.Mosaic -> {
            val t = tile ?: return
            var ty = 0f
            while (ty < h) {
                var tx = 0f
                while (tx < w) {
                    for (q in t.rects) {
                        drawRect(t.paints[q[5].toInt()], Offset(tx + q[0], ty + q[1]), Size(q[2], q[3]), alpha = q[4])
                    }
                    tx += TILE
                }
                ty += TILE
            }
        }
    }
}

/**
 * [pattern] as this element's ground, in the theme's paints; [quiet] is the variant to put text on. Drawn in dp
 * at any density, behind the content, clipped to the element.
 */
public fun Modifier.mutantPattern(
    pattern: MutantPattern,
    quiet: Boolean = false,
): Modifier =
    composed {
        val colors = LocalMutantColors.current
        val tile = androidx.compose.runtime.remember(pattern, quiet, colors) { patternTile(pattern, quiet, colors) }
        drawBehind {
            val d = density
            val w = size.width / d
            val h = size.height / d
            scale(d, Offset.Zero) {
                clipRect(0f, 0f, w, h) { drawPattern(pattern, quiet, colors, w, h, tile) }
            }
        }
    }

/**
 * The page's alphas that its swatches cannot show: on «Эфир»'s dark ground 0.38 and 0.30 are 3 levels apart, under
 * what the swatch's frame already moves. `PatternSourceTest` holds them to the page's `patterns()` instead.
 */
internal object PatternAlpha {
    const val LINES: Float = .38f
    const val LINES_QUIET: Float = .1f
    const val LINES_PRINT: Float = .2f
    const val LINES_PRINT_QUIET: Float = .06f
    const val MOSAIC_QUIET: Float = .55f
}

private const val TILE = 64f
private const val RASTER_FADE = .35f
