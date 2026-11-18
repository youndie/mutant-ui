package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap

internal actual fun ImageBitmap.freeze() {
    asSkiaBitmap().setImmutable()
}
