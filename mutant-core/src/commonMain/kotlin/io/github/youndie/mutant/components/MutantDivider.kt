package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.drawTrackingDivider
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme

/**
 * A divider: a 2 dp line of tracking strokes in the no-signal blue, some ending in a comet, across what it is given.
 * Its strokes are seeded by the screen and [key]: dividers on one screen each take their own, or they fall alike.
 */
@Composable
public fun MutantDivider(
    modifier: Modifier = Modifier,
    key: String = "0",
) {
    val colors = MutantTheme.colors
    val seed = "${LocalMutantCassette.current}/${LocalMutantStage.current}/div$key"
    Canvas(modifier.fillMaxWidth().height(HEIGHT)) {
        scale(density, Offset.Zero) { drawTrackingDivider(seed, size.width / density, HEIGHT.value, colors) }
    }
}

/** The page's `data-mu-divider` row, `height: 2px`. */
private val HEIGHT = 2.dp
