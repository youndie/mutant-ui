package io.github.youndie.mutant.type

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.tokens.MutantColors

/**
 * The design's type roles (`mutant-tokens.json`, `mutant.type`) as text styles in the faces the kit bundles, each
 * laid out as Chrome lays the design's line, so a consumer's own words sit where the kit's do (B-55):
 *
 * | Role | Face | The design's sizes | Colour |
 * |---|---|---|---|
 * | [display] — screen and dialog titles | Handjet | 22, 24, 26, 28 | the theme's text |
 * | [osd] — on-screen lines, counters, badges; tracked 1 | Handjet | 14, 16, 18, 20 | the theme's OSD amber |
 * | [text] — body text and labels; regular or medium | Rubik | 11, 13, 14, 15, 16 | the theme's text |
 *
 * A size is given to the role, which lays the line for it: a style copied to another size keeps the line of the
 * first. A colour can be copied freely. Read it as `MutantTheme.textStyles` inside a `MutantTheme`; outside one the
 * faces fall back to the platform's.
 */
@Immutable
public class MutantTextStyles internal constructor(
    private val fonts: MutantFonts?,
    private val colors: MutantColors,
) {
    /** Handjet, for screen and dialog titles: the design sets them at 22, 24, 26 or 28. */
    public fun display(size: TextUnit = 26.sp): TextStyle =
        TextStyle(fontFamily = fonts?.display, fontSize = size, color = colors.text).cssLine(MutantFaceMetrics.Handjet)

    /** Handjet tracked 1, for on-screen lines, counters and badges: 14, 16, 18 or 20, in the theme's OSD amber. */
    public fun osd(size: TextUnit = 20.sp): TextStyle =
        TextStyle(
            fontFamily = fonts?.display,
            fontSize = size,
            letterSpacing = OSD_TRACKING.sp,
            color = colors.textOsd,
        ).cssLine(MutantFaceMetrics.Handjet)

    /** Rubik, for body text and labels: 11, 13, 14, 15 or 16, regular or [FontWeight.Medium]. */
    public fun text(
        size: TextUnit = 15.sp,
        weight: FontWeight = FontWeight.Normal,
    ): TextStyle =
        TextStyle(fontFamily = fonts?.text, fontSize = size, fontWeight = weight, color = colors.text)
            .cssLine(MutantFaceMetrics.Rubik)
}

/** The OSD role's tracking, CSS px (`mutant.type.osd.letter-spacing`). */
private const val OSD_TRACKING = 1
