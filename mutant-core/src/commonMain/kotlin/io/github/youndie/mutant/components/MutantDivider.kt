package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.TrackingDivider
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme

/**
 * A divider: a 2 dp line of tracking strokes in the no-signal blue, some ending in a comet, across what it is given.
 * Its strokes are seeded by the screen and [key]: dividers on one screen each take their own, or they fall alike. A
 * screen's keyframe loss takes them into its picture with the bodies.
 */
@Composable
public fun MutantDivider(
    modifier: Modifier = Modifier,
    key: String = "0",
) {
    val colors = MutantTheme.colors
    val seed = "${LocalMutantCassette.current}/${LocalMutantStage.current}/div$key"
    TrackingDivider(seed, HEIGHT.value, colors, modifier.fillMaxWidth().height(HEIGHT))
}

/** The page's `data-mu-divider` row, `height: 2px`. */
private val HEIGHT = 2.dp
