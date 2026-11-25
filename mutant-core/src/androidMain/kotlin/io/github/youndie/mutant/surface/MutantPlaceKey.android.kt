package io.github.youndie.mutant.surface

import androidx.compose.runtime.CompositeKeyHashCode

internal actual fun placeKeyText(hash: CompositeKeyHashCode): String = hash.toString(36)
