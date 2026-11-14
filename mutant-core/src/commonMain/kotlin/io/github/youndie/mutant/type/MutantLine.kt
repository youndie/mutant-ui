package io.github.youndie.mutant.type

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/**
 * A face's vertical metrics per em (`hhea`, which the bundled faces repeat in `OS/2` typo with `USE_TYPO_METRICS`),
 * which Blink rounds and Compose does not. Held to the bundled files by `MutantFaceMetricsTest`.
 */
internal class FaceMetrics(
    val ascent: Float,
    val descent: Float,
)

internal object MutantFaceMetrics {
    /** Handjet, both cuts: ascender 7200, descender −1920 of 8160. */
    val Handjet = FaceMetrics(7200f / 8160f, 1920f / 8160f)

    /** Rubik, both cuts: ascender 935, descender −250 of 1000. */
    val Rubik = FaceMetrics(.935f, .25f)
}

/**
 * This style laid out as Blink lays a CSS line box, so its baseline lands on Chrome's pixel (B-52). [lineHeight]
 * is the CSS `line-height` as a multiple of the size, null for `normal`.
 *
 * Blink rounds the ascent and the descent to whole pixels each, takes `normal` as their sum, and floors the
 * half-leading — negative when the line is shorter than the face, as Handjet's 20 px in a 20 px line is (its own
 * line is 22.35 px). Compose centres the face's unrounded metrics instead. The share of the leading above the
 * text is chosen so the baseline falls where Blink's does. A line height equal to the size is ignored by Compose,
 * which then lays out the face's own taller line with the baseline at the ascent: it is nudged by 0.01 %.
 */
internal fun TextStyle.cssLine(
    face: FaceMetrics,
    lineHeight: Float? = null,
): TextStyle {
    val px = fontSize.value
    val ascent = face.ascent * px
    val content = round(ascent) + round(face.descent * px)
    val line = lineHeight?.let { it * px } ?: content
    val baseline = floor((line - content) / 2) + round(ascent)
    val leading = line - (face.ascent + face.descent) * px
    val ratio = if (abs(leading) <= .01f) 0f else ((baseline - ascent) / leading).coerceIn(0f, 1f)
    val honoured = if (abs(line - px) < .001f) line * HONOURED else line
    return copy(
        lineHeight = honoured.sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment(ratio), LineHeightStyle.Trim.None),
    )
}

/** The nudge that makes Compose keep a line height equal to the font size. */
private const val HONOURED = 1.0001f

/**
 * How a parity fixture rasterises the kit's text, to draw as Chrome does (glyphs at fractional x, no hinting);
 * null in the kit, which never overrides the platform's settings for its consumers (B-52).
 */
internal val LocalMutantTextRaster =
    androidx.compose.runtime
        .staticCompositionLocalOf<androidx.compose.ui.text.PlatformTextStyle?> {
            null
        }
