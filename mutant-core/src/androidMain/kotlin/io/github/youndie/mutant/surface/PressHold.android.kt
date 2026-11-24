package io.github.youndie.mutant.surface

/** Compose's `clickable` holds a press inside a scrolling container for `ViewConfiguration.getTapTimeout()` here (B-189). */
internal actual val composeHoldsPressInScroll: Boolean = true
