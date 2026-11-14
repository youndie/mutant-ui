package io.github.youndie.mutant.surface

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asSkiaPath
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathUtils

internal actual fun Path.strokedOutline(
    width: Float,
    resScale: Float,
): Path? {
    val paint =
        Paint().apply {
            mode = PaintMode.STROKE
            strokeWidth = width
            strokeJoin = PaintStrokeJoin.ROUND
        }
    val out = PathBuilder()
    if (!PathUtils.fillPathWithPaint(asSkiaPath(), paint, out, null, resScale)) return null
    return out.detach().asComposePath()
}
