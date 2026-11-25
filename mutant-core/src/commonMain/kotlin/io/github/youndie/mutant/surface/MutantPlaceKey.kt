package io.github.youndie.mutant.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode

/**
 * A body's default key (B-206, the designer's answer 38): its [type] and its place in the composition, the runtime's
 * [currentCompositeKeyHashCode] where the component reads it. Read as a default argument, it is the place of the
 * component's own call. Two unkeyed components side by side, or in a loop, get two bodies; one component keeps its body
 * however its words, icon or state change, as its place does not move.
 *
 * What the place holds and what it does not (B-206, measured on desktop): it holds through recomposition, a branch
 * appearing before the call, a move by `movableContentOf` (the runtime hashes moved content by itself, not by where it
 * stands) and a [MutantScreen] shown again (its `key(screen)` hashes the screen: a string or a number, any value
 * whose `hashCode` is the same from run to run, gives the same bodies on every visit and every run). A row of a plain
 * loop has its index for a place, so a removal above hands its body to the row below: a list gives each item's key from
 * its id. The place counts every group above the call, the platform's own host among them: the runtime hashes one
 * tree alike on the JVM, Kotlin/Native and wasm, but two hosts on desktop already give one call two places, so an
 * unkeyed body is not expected to be the same in a desktop window, a browser and on iOS. A body that must be the same
 * there takes a key of its own.
 */
@Composable
internal fun mutantPlaceKey(type: String): String = type + "@" + currentCompositeKeyHashCode.toString(36)
