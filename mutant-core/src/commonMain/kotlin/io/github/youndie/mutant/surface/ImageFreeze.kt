package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.ImageBitmap

/**
 * B-113, B-119: marks [this] image finished, so drawing it does not copy it. Compose on Skiko draws an `ImageBitmap` through
 * `Image.makeFromBitmap` on every call, which copies a bitmap that is still mutable, and a GPU canvas then uploads the
 * copy as a new texture. Called once the image is drawn into; nothing draws into it after.
 */
internal expect fun ImageBitmap.freeze()
