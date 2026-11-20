package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import io.github.youndie.mutant.gen.Slot

/**
 * B-149: what a body draws in the frame its first build is not ready in, where the frame cannot wait for it (the
 * browser): the slot's rounded shape in the body's fill, its outline inside it, so a body is never bare. The body
 * replaces it from the frame its build is taken in.
 */
internal fun DrawScope.drawStandIn(
    slot: Slot,
    look: BodyLook,
    density: Float,
) {
    val w = slot.w.toFloat()
    val h = slot.h.toFloat()
    val r = slot.r.toFloat()
    val o = look.outline
    scale(density, Offset.Zero) {
        drawRoundRect(look.fill, Offset.Zero, Size(w, h), CornerRadius(r))
        drawRoundRect(
            look.ink,
            Offset(o / 2, o / 2),
            Size(w - o, h - o),
            CornerRadius((r - o / 2).coerceAtLeast(0f)),
            style = Stroke(o),
        )
    }
}
