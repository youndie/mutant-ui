package io.github.youndie.mutant.surface

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/** Collects the bounds of what a surface's content marks clean, relative to the surface. */
@Stable
internal class CleanZoneRegistry {
    var surface: LayoutCoordinates? = null
    val bounds = mutableStateMapOf<Any, Rect>()

    /** The union of the marked bounds, dp, or null when nothing is marked. */
    fun union(density: Float): Rect? =
        bounds.values
            .takeIf { it.isNotEmpty() }
            ?.let { rs ->
                Rect(rs.minOf { it.left }, rs.minOf { it.top }, rs.maxOf { it.right }, rs.maxOf { it.bottom })
            }?.let { Rect(it.left / density, it.top / density, it.right / density, it.bottom / density) }
}

internal val LocalCleanZone = staticCompositionLocalOf<CleanZoneRegistry?> { null }

/**
 * Marks this element's bounds as part of its surface's clean zone: the text and icons that flow lines,
 * mosaic and dead pixels never enter (research D5, the engine's `textZone`).
 */
internal fun Modifier.mutantCleanZone(): Modifier =
    composed {
        val registry = LocalCleanZone.current ?: return@composed this
        val id = remember { Any() }
        androidx.compose.runtime.DisposableEffect(registry) { onDispose { registry.bounds.remove(id) } }
        onGloballyPositioned { c ->
            val s = registry.surface ?: return@onGloballyPositioned
            if (!s.isAttached || !c.isAttached) return@onGloballyPositioned
            val r = s.localBoundingBoxOf(c, clipBounds = false)
            if (registry.bounds[id] != r) registry.bounds[id] = r
        }
    }
