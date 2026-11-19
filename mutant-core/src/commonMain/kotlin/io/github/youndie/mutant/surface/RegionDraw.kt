package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * B-143: draws the [size] rectangle of [src] at [from] onto this canvas at [to], copying that rectangle and no more.
 * `drawImageRect` of a still mutable bitmap on Skiko makes an image of the whole bitmap first (B-113), so a keyframe
 * loss reading a 16 dp block out of a strip as wide as the screen copied the strip, for every block of every step.
 */
internal expect fun Canvas.drawRegion(
    src: ImageBitmap,
    from: IntOffset,
    size: IntSize,
    to: IntOffset,
    paint: Paint,
)
