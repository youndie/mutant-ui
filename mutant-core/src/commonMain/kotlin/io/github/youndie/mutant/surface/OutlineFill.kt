package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Path

/**
 * B-121: the area a stroke of [width] with round joins covers along [this] path, as a path to fill, at [resScale] device
 * pixels a unit (the precision Skia strokes with when it draws), or null where the platform keeps drawing the stroke.
 * Built once a tick, so a body drawn in the frame fills it each frame instead of stroking its outline again.
 */
internal expect fun Path.outlineFill(
    width: Float,
    resScale: Float,
): Path?
