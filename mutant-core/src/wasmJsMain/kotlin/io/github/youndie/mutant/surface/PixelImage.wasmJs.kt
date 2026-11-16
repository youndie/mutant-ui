package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

/** Skia's: the pixels as BGRA bytes, not premultiplied, the layout `readPixels` reads them in. */
internal actual fun imageBitmapOfPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val c = pixels[i]
        bytes[4 * i] = c.toByte()
        bytes[4 * i + 1] = (c shr 8).toByte()
        bytes[4 * i + 2] = (c shr 16).toByte()
        bytes[4 * i + 3] = (c ushr 24).toByte()
    }
    val info = ImageInfo(ColorInfo(ColorType.BGRA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB), width, height)
    val bitmap = Bitmap()
    bitmap.allocPixels(info)
    bitmap.installPixels(bytes)
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}
