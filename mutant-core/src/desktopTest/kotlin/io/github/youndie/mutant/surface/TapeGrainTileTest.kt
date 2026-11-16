package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * B-77: at a whole density the Tape's grain is one fill repeating its tile, rasterised once, and it draws the very
 * pixels its strokes drew, tile by tile; at a fractional density, where a tile is not whole pixels, the strokes are
 * drawn as before.
 */
class TapeGrainTileTest {
    private fun render(
        d: Float,
        fromTile: Boolean,
        tick: Long,
        print: Boolean,
    ): IntArray {
        val w = 640f
        val h = 400f
        val colors = if (print) MutantPalettes.Kopiya else MutantPalettes.Efir
        val img = ImageBitmap((w * d).toInt(), (h * d).toInt())
        val grain = TapeGrain.of("tape-tile")
        val tile = if (fromTile) grainTile(grain, colors, d) else null
        CanvasDrawScope().draw(Density(d), LayoutDirection.Ltr, Canvas(img), Size(w * d, h * d)) {
            drawRect(colors.page)
            scale(d, Offset.Zero) {
                clipRect(
                    0f,
                    0f,
                    w,
                    h,
                ) { drawTape("tape-tile", tick * 125.0 + 1, colors, w, h, grain, emptySet(), tile) }
            }
        }
        return IntArray(img.width * img.height).also { img.readPixels(it) }
    }

    @Test
    fun `the grain from its tile is the strokes' own, to the byte, at density 1, 2 and 3 in both themes`() {
        for (print in listOf(false, true)) {
            for (d in listOf(1f, 2f, 3f)) {
                // Ticks whose tile offsets differ: (37, 71) a tick, modulo the tile.
                for (tick in listOf(3L, 11L, 40L)) {
                    val strokes = render(d, fromTile = false, tick, print)
                    val tiled = render(d, fromTile = true, tick, print)
                    val off = strokes.indices.count { strokes[it] != tiled[it] }
                    assertEquals(0, off, "pixels off at density $d, tick $tick, ${if (print) "Kopiya" else "Efir"}")
                }
            }
        }
    }

    @Test
    fun `a fractional density keeps the strokes`() {
        assertNull(grainTile(TapeGrain.of("tape-tile"), MutantPalettes.Efir, 2.625f))
    }
}
