package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/**
 * A badge: a clean on-screen-display plate, no body — [text] in Handjet in black on the amber, ringed 1.5 dp in black.
 * Full size (a list row's `НОВОЕ`) it is Handjet 16 on a 20 dp line, 6 dp in; [compact] (a navigation item's count)
 * Handjet 14 on 16, 5 dp in. Both blacks and the amber are the page's in both themes.
 */
@Composable
public fun MutantBadge(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    BadgeBody(text, modifier, compact)
}

/** [MutantBadge]; [hidden] draws the text transparent for a fixture's bare twin, the plate and ring kept. */
@Composable
internal fun BadgeBody(
    text: String,
    modifier: Modifier,
    compact: Boolean,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val size = if (compact) COMPACT_SIZE else SIZE
    val line = (if (compact) COMPACT_LINE else LINE) / size
    BasicText(
        text,
        modifier
            .drawBehind {
                val r = RING.toPx()
                drawRect(BLACK, Offset(-r, -r), Size(this.size.width + 2 * r, this.size.height + 2 * r))
                drawRect(colors.amber)
            }.padding(horizontal = if (compact) COMPACT_PAD else PAD)
            // Inside a body (a navigation bar) the engine keeps the badge's text clean.
            .mutantCleanZone(),
        style =
            TextStyle(
                fontFamily = LocalMutantFonts.current?.display,
                fontSize = size.sp,
                color = if (hidden) Color.Transparent else BLACK,
                platformStyle = LocalMutantTextRaster.current,
            ).cssLine(MutantFaceMetrics.Handjet, line),
        maxLines = 1,
        softWrap = false,
    )
}

/** Full size: Handjet 16 on a 20 dp line, 6 dp in (the page's list badge). */
private const val SIZE = 16f
private const val LINE = 20f
private val PAD = 6.dp

/** Compact: Handjet 14 on 16, 5 dp in (the page's navigation count). */
private const val COMPACT_SIZE = 14f
private const val COMPACT_LINE = 16f
private val COMPACT_PAD = 5.dp

/** The ring, the page's `box-shadow: 0 0 0 1.5px`. */
private val RING = 1.5.dp

/** The page's literal `#05060c`, in both themes. */
private val BLACK = Color(0xFF05060C)
