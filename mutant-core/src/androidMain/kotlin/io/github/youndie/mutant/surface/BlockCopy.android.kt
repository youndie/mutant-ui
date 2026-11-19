package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// Android's pipeline is vectorised and draws a bitmap without copying it: the blocks are drawn as they were.

internal actual fun Canvas.readBlock(
    part: ImageBitmap,
    from: IntOffset,
    size: IntSize,
    row: Int,
): Boolean = false

internal actual fun Canvas.writeBlock(
    part: ImageBitmap,
    row: Int,
    size: IntSize,
    to: IntOffset,
): Boolean = false

internal actual fun ImageBitmap.allOpaque(): Boolean = false
