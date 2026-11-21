package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual val lossBandInMemory: Boolean = false

internal actual fun readImagePixels(
    image: ImageBitmap,
    into: IntArray,
) {
    val b = image.asSkiaBitmap()
    val info = ImageInfo(b.width, b.height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    val bytes = b.readPixels(info, b.width * 4, 0, 0) ?: return
    for (i in into.indices) {
        into[i] = (bytes[4 * i].toInt() and 255) or ((bytes[4 * i + 1].toInt() and 255) shl 8) or
            ((bytes[4 * i + 2].toInt() and 255) shl 16) or ((bytes[4 * i + 3].toInt() and 255) shl 24)
    }
}

internal actual fun imageOfPixels(
    px: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    val bytes = ByteArray(width * height * 4)
    for (i in px.indices) {
        val p = px[i]
        bytes[4 * i] = p.toByte()
        bytes[4 * i + 1] = (p ushr 8).toByte()
        bytes[4 * i + 2] = (p ushr 16).toByte()
        bytes[4 * i + 3] = (p ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    val b = Bitmap()
    b.allocPixels(info)
    b.installPixels(info, bytes, width * 4)
    b.setImmutable()
    return b.asComposeImageBitmap()
}
