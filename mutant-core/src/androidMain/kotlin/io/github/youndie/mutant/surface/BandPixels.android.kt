package io.github.youndie.mutant.surface

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal actual val lossBandInMemory: Boolean = false

// An ARGB_8888 bitmap's memory holds premultiplied R, G, B, A bytes: read as little-endian ints, red is the low byte.
internal actual fun readImagePixels(
    image: ImageBitmap,
    into: IntArray,
) {
    val b = image.asAndroidBitmap()
    val buffer = ByteBuffer.allocate(b.byteCount).order(ByteOrder.LITTLE_ENDIAN)
    b.copyPixelsToBuffer(buffer)
    buffer.rewind()
    buffer.asIntBuffer().get(into, 0, minOf(into.size, b.width * b.height))
}

internal actual fun imageOfPixels(
    px: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    val buffer = ByteBuffer.allocate(width * height * 4).order(ByteOrder.LITTLE_ENDIAN)
    buffer.asIntBuffer().put(px)
    val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    b.copyPixelsFromBuffer(buffer)
    return b.asImageBitmap()
}
