package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.IRect

/** A subset shares the bitmap's pixels, so the image made of it to draw holds the rectangle only. */
internal actual fun Canvas.drawRegion(
    src: ImageBitmap,
    from: IntOffset,
    size: IntSize,
    to: IntOffset,
    paint: Paint,
) {
    val part = Bitmap()
    if (!src.asSkiaBitmap().extractSubset(part, IRect.makeXYWH(from.x, from.y, size.width, size.height))) return
    drawImageRect(part.asComposeImageBitmap(), IntOffset.Zero, size, to, size, paint)
    part.close()
}
