package io.github.youndie.mutant.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/**
 * Under an unbounded width (a horizontally scrolling row), measures what follows at its own widest — its maximum
 * intrinsic width — so the parts laid out by `weight` take their own widths instead of nothing (B-64). Under a bounded
 * width it changes nothing.
 */
internal fun Modifier.ownWidthWhenUnbounded(): Modifier =
    layout { m, c ->
        val p =
            if (c.hasBoundedWidth) {
                m.measure(c)
            } else {
                val own = m.maxIntrinsicWidth(if (c.hasBoundedHeight) c.maxHeight else Constraints.Infinity)
                m.measure(c.copy(maxWidth = maxOf(own, c.minWidth)))
            }
        layout(p.width, p.height) { p.place(0, 0) }
    }
