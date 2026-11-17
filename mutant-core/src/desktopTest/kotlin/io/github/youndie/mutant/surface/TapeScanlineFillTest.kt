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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * B-107: the Tape's scanlines are one fill repeating a block of rows that is whole pixels, and it draws the very pixels
 * the rects drew, at a whole density and at Android's 2.625, with the band passing over them and away.
 */
class TapeScanlineFillTest {
    private fun render(
        d: Float,
        fromFill: Boolean,
        t: Double,
    ): IntArray {
        val w = 411f
        val h = 400f
        val colors = MutantPalettes.Efir
        val img = ImageBitmap((w * d).toInt(), (h * d).toInt())
        val lines = if (fromFill) assertNotNull(scanlineFill(colors, d)) else null
        CanvasDrawScope().draw(Density(d), LayoutDirection.Ltr, Canvas(img), Size(w * d, h * d)) {
            drawRect(colors.page)
            scale(d, Offset.Zero) {
                clipRect(0f, 0f, w, h) {
                    drawTape("tape-lines", t, colors, w, h, TapeGrain.of("tape-lines"), emptySet(), null, lines)
                }
            }
        }
        return IntArray(img.width * img.height).also { img.readPixels(it) }
    }

    @Test
    fun `the scanlines from their fill are the rects' own, to the byte, at density 1, 2, 3 and 2_625`() {
        for (d in listOf(1f, 2f, 3f, 2.625f)) {
            // 3000 ms: the band away; 500 ms: the band passing over the scanlines.
            for (t in listOf(3000.0, 500.0)) {
                val rects = render(d, fromFill = false, t)
                val filled = render(d, fromFill = true, t)
                val off = rects.indices.count { rects[it] != filled[it] }
                assertEquals(0, off, "pixels off at density $d, $t ms")
            }
        }
    }

    @Test
    fun `the block is eight periods at 2_625, one at a whole density, and Kopiya has none`() {
        assertEquals(8, wholeSpan(3f, 2.625f))
        assertEquals(1, wholeSpan(3f, 2f))
        assertNull(scanlineFill(MutantPalettes.Kopiya, 2f))
    }
}
