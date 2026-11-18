package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.ImageBitmap

/** Android draws a bitmap without copying it; nothing to mark. */
internal actual fun ImageBitmap.freeze() {}
