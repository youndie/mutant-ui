package io.github.youndie.mutant.type

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.youndie.mutant.resources.Res
import io.github.youndie.mutant.resources.handjet_regular
import io.github.youndie.mutant.resources.handjet_semibold
import io.github.youndie.mutant.resources.rubik_medium
import io.github.youndie.mutant.resources.rubik_regular
import org.jetbrains.compose.resources.Font

/**
 * The kit's two families (research §1.6, D9), as `mutant-tokens.json` assigns them:
 *
 * | Role | Face | Weights |
 * |---|---|---|
 * | [display] — `display` and `osd` (titles, on-screen text, counters, badges) | Handjet | 400, 600 |
 * | [text] — body text and labels | Rubik | 400, 500 |
 *
 * Both are static instances cut by `scripts/fonts.py` from the variable files, every axis but `wght`
 * at its default (Handjet's `ELGR` 1 and `ELSH` 2, which is what the pages draw). Static, because a
 * `wght` setting needs Android API 26, and under it the variable Rubik would draw its default
 * instance, 300. Both carry the whole Russian alphabet, so no run of text needs a backing face.
 */
@Immutable
internal class MutantFonts(
    val display: FontFamily,
    val text: FontFamily,
)

/** The bundled families, loaded through compose resources on every target. */
@Composable
internal fun mutantFonts(): MutantFonts =
    MutantFonts(
        display =
            FontFamily(
                Font(Res.font.handjet_regular, FontWeight.Normal),
                Font(Res.font.handjet_semibold, FontWeight.SemiBold),
            ),
        text =
            FontFamily(
                Font(Res.font.rubik_regular, FontWeight.Normal),
                Font(Res.font.rubik_medium, FontWeight.Medium),
            ),
    )
