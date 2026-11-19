package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/*
 * B-145: a block of a picture moved as pixels, not drawn. Skia draws a bitmap whose pixels may be transparent through
 * its raster pipeline, and in a browser that pipeline runs a pixel at a time in floats (no SIMD): a keyframe loss's
 * blocks, read and written by `drawImageRect`, held a phone's frames at 110–220 ms (B-142, B-144). A copy of the
 * pixels is the same picture wherever nothing is blended: reading into an empty part, and writing a part with no
 * transparent pixel. Each answers false where the platform has no such copy, and the caller draws instead.
 */

/**
 * Copies the [size] rectangle at [from] of the bitmap this canvas draws into, into [part] at its row [row] (the
 * rectangle's width is [part]'s), pixels as they are.
 */
internal expect fun Canvas.readBlock(
    part: ImageBitmap,
    from: IntOffset,
    size: IntSize,
    row: Int,
): Boolean

/**
 * Copies [size] of [part] from its row [row] over the bitmap this canvas draws into, at [to], replacing what is there;
 * what falls outside the bitmap is left out.
 */
internal expect fun Canvas.writeBlock(
    part: ImageBitmap,
    row: Int,
    size: IntSize,
    to: IntOffset,
): Boolean

/** Whether no pixel of the image is transparent at all; false where the platform does not say. */
internal expect fun ImageBitmap.allOpaque(): Boolean
