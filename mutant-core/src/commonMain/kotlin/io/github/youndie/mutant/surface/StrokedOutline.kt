package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Path

/**
 * The area a round-joined stroke of [width] along this path covers, as a path to fill: what Skia would build on the
 * render thread for the stroke every frame, built once per tick instead (B-79). [resScale] is the device pixels per
 * unit the path is drawn at, so curves are flattened finely enough there. Null where the platform cannot build it;
 * the body then strokes as before.
 */
internal expect fun Path.strokedOutline(
    width: Float,
    resScale: Float,
): Path?
