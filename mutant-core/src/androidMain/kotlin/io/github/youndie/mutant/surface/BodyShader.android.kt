package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Brush
import io.github.youndie.mutant.gen.DistanceField
import io.github.youndie.mutant.gen.Slot

/** B-110's spike draws by shader on desktop and in the browser only. */
internal actual val bodyShaderSupported: Boolean = false

internal actual fun bodyShaderBrush(
    field: DistanceField,
    slot: Slot,
    look: BodyLook,
    density: Float,
): Brush? = null
