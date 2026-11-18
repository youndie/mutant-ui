package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Path

/** Android keeps drawing the stroke. */
internal actual fun Path.outlineFill(
    width: Float,
    resScale: Float,
): Path? = null
