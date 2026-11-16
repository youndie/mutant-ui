package io.github.youndie.mutant.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.youndie.mutant.theme.MutantTheme

/** The fill the nearest body shows while it boils, or null at rest (B-91). [MutantSurface] provides it to its content. */
internal val LocalMutantBodyGround = staticCompositionLocalOf<State<Color?>?> { null }

/**
 * The colour for words or an icon on the nearest body: [preferred] at rest, and while the body boils towards amber
 * whichever of [preferred] and the theme's `onPrimary` reads better against the fill it shows on that frame (B-91). The
 * design pairs `onPrimary` with amber; a light label stood on it at 1.4:1. The pick follows the fill, not the press, so
 * the release keeps the dark word until the fill has cooled past the point where the light one reads better.
 */
@Composable
internal fun inkOnBody(preferred: Color): Color {
    val ground = LocalMutantBodyGround.current ?: return preferred
    val on = MutantTheme.colors.onPrimary
    val pick by remember(ground, preferred, on) {
        derivedStateOf {
            val g = ground.value
            if (g != null && contrast(on, g) > contrast(preferred, g)) on else preferred
        }
    }
    return pick
}

/** WCAG's contrast ratio of two opaque colours. */
internal fun contrast(
    a: Color,
    b: Color,
): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + .05f) / (minOf(la, lb) + .05f)
}
