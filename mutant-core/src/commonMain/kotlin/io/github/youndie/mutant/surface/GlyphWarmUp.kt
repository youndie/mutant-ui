package io.github.youndie.mutant.surface

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.theme.MutantTheme

/**
 * B-165: in the browser, a screen's first frames also draw every letter the kit's type sets, in each of the sizes the
 * design gives its roles, all but invisible (alpha 1/255), one size a frame: a tab shown for the first time then finds
 * its glyphs rasterised and its text's GPU work warm, where it rasterised every new letter and size in the frame that
 * composed it. Elsewhere, and once done, it draws nothing.
 */
internal fun Modifier.mutantGlyphWarmUp(): Modifier =
    composed {
        if (firstFrameWaits) return@composed this
        val styles = MutantTheme.textStyles
        val all = remember(styles) { warmStyles(styles) }
        val resolver = LocalFontFamilyResolver.current
        val density = LocalDensity.current
        val direction = LocalLayoutDirection.current
        val measurer = remember(resolver, density, direction) { TextMeasurer(resolver, density, direction, 1) }
        var next by remember(all) { mutableIntStateOf(0) }
        LaunchedEffect(all) {
            while (next < all.size) {
                withFrameNanos { }
                next++
            }
        }
        val i = next
        if (i >= all.size) return@composed this
        val style = all[i]
        drawWithContent {
            drawContent()
            // Laid out to the screen's width, so every letter lands inside it and is drawn, not culled.
            val layout =
                measurer.measure(
                    WARM_LETTERS,
                    style,
                    constraints =
                        androidx.compose.ui.unit
                            .Constraints(maxWidth = size.width.toInt()),
                )
            drawText(layout, topLeft = Offset.Zero, alpha = WARM_ALPHA)
        }
    }

/** Every role at every size the design gives it (MutantTextStyles' notes), Rubik in both weights. */
private fun warmStyles(s: io.github.youndie.mutant.type.MutantTextStyles): List<TextStyle> =
    listOf(22, 24, 26, 28).map { s.display(it.sp) } +
        listOf(14, 16, 18, 20).map { s.osd(it.sp) } +
        listOf(11, 12, 13, 14, 15, 16).flatMap { n ->
            listOf(FontWeight.Normal, FontWeight.Medium).map { s.text(n.sp, it) }
        }

/** The letters the kit's words are made of: Latin, Russian, digits and the punctuation the design sets. */
private const val WARM_LETTERS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789 .,:;!?'\"()[]-–—·•…«»№%+/×#&@ " +
        "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ абвгдеёжзийклмнопрстуфхцчшщъыьэюя"

private const val WARM_ALPHA = 1f / 255f
