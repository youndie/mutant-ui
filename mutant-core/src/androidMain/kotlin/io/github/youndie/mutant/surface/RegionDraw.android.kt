package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Android draws a bitmap without copying it, so the rectangle is drawn as it is. */
internal actual fun Canvas.drawRegion(
    src: ImageBitmap,
    from: IntOffset,
    size: IntSize,
    to: IntOffset,
    paint: Paint,
) {
    drawImageRect(src, from, size, to, size, paint)
}
