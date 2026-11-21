package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-158: the keyframe loss's band reads a bitmap's pixels and makes an image of them in one form on every target —
 * premultiplied RGBA, red in an `Int`'s low byte — whatever order the platform's bitmaps keep (the browser reads Skiko's
 * memory in one block).
 */
class ImagePixelsTest {
    private fun ints(image: ImageBitmap) = IntArray(image.width * image.height).also { readImagePixels(image, it) }

    @Test
    fun `a bitmap's pixels read as premultiplied RGBA and an image of them reads the same`() {
        val image = ImageBitmap(3, 2)
        val canvas = Canvas(image)
        val red = Paint().apply { color = Color(0xFFFF0000) }
        val blue = Paint().apply { color = Color(0xFF0000FF) }
        canvas.drawRect(0f, 0f, 1f, 2f, red)
        canvas.drawRect(1f, 0f, 2f, 2f, blue)
        val px = ints(image)
        assertEquals(0xFF0000FF.toInt(), px[0], "red: R in the low byte, A in the high")
        assertEquals(0xFFFF0000.toInt(), px[1], "blue: B in the third byte")
        assertEquals(0, px[2], "transparent")
        val again = ints(imageOfPixels(px, 3, 2))
        assertEquals(px.toList(), again.toList(), "an image of the pixels reads them back")
        // A drawn image: the band's way of handing its pixels to Skia.
        val copy = ImageBitmap(3, 2)
        Canvas(copy).drawImage(imageOfPixels(px, 3, 2), Offset.Zero, Paint())
        assertEquals(px.toList(), ints(copy).toList(), "drawn, the same pixels")
        assertEquals(Size(3f, 2f), Size(copy.width.toFloat(), copy.height.toFloat()))
    }
}
