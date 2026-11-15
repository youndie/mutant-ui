package io.github.youndie.mutant.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics

/**
 * Whether a control shows the hand cursor (B-63). Compose gives a test no way to read a node's hover icon, so the one
 * modifier that sets the icon marks the node with this as well, and a test reads the mark.
 */
internal val MutantHandCursor = SemanticsPropertyKey<Boolean>("MutantHandCursor")

/** The hand cursor over a live control (B-63, designed here); a disabled one keeps the arrow. */
internal fun Modifier.mutantHand(enabled: Boolean): Modifier =
    if (enabled) pointerHoverIcon(PointerIcon.Hand).semantics { this[MutantHandCursor] = true } else this
