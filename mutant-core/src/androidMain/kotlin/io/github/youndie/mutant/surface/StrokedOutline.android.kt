package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Path

// Android's canvas strokes on its own pipeline, which B-79 did not measure: the body strokes as before there.
internal actual fun Path.strokedOutline(
    width: Float,
    resScale: Float,
): Path? = null
