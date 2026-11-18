package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Brush
import io.github.youndie.mutant.gen.Slot

/** B-110's spike draws by shader on desktop and in the browser only. */
internal actual val bodyShaderSupported: Boolean = false

internal actual fun bodyShaderBrush(
    contour: io.github.youndie.mutant.gen.GenPath,
    highlight: io.github.youndie.mutant.gen.GenPath,
    box: androidx.compose.ui.geometry.Rect,
    slot: Slot,
    look: BodyLook,
    density: Float,
): Brush? = null
