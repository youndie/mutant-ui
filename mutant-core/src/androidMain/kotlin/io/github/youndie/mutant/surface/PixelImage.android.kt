package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Android's: `Bitmap.createBitmap` takes the colors as `readPixels` gives them, ARGB, not premultiplied. */
internal actual fun imageBitmapOfPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap =
    android.graphics.Bitmap
        .createBitmap(
            pixels,
            width,
            height,
            android.graphics.Bitmap.Config.ARGB_8888,
        ).asImageBitmap()
