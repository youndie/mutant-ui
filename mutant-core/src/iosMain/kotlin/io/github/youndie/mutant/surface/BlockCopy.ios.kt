package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.IRect

/** [size] of [this] from its row [row], sharing its pixels, given to [use]; the bitmap itself when that is all of it. */
private inline fun Bitmap.rows(
    row: Int,
    size: IntSize,
    use: (Bitmap) -> Boolean,
): Boolean {
    if (row == 0 && size.width == width && size.height == height) return use(this)
    val part = Bitmap()
    try {
        return extractSubset(part, IRect.makeXYWH(0, row, size.width, size.height)) && use(part)
    } finally {
        part.close()
    }
}

internal actual fun Canvas.readBlock(
    part: ImageBitmap,
    from: IntOffset,
    size: IntSize,
    row: Int,
): Boolean = part.asSkiaBitmap().rows(row, size) { skiaCanvas.readPixels(it, from.x, from.y) }

internal actual fun Canvas.writeBlock(
    part: ImageBitmap,
    row: Int,
    size: IntSize,
    to: IntOffset,
): Boolean = part.asSkiaBitmap().rows(row, size) { skiaCanvas.writePixels(it, to.x, to.y) }

internal actual fun ImageBitmap.allOpaque(): Boolean = asSkiaBitmap().computeIsOpaque()
