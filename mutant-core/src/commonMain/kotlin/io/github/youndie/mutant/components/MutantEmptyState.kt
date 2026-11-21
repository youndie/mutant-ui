package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.illustration.IllustrationArt
import io.github.youndie.mutant.illustration.IllustrationParts
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/**
 * An empty state: one of the six illustrations ([kind]), 44 dp under it the [title] (Handjet 24, a heading) over the
 * [text] (Rubik 14, secondary), 6 dp apart, all centred in what it is given; and, given [action], that under the text
 * 20 dp apart (a [MutantButton], say — the page draws none). The words wrap and stay centred.
 */
@Composable
public fun MutantEmptyState(
    kind: MutantIllustrationKind,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    EmptyStateBody(kind, title, modifier, text, action)
}

/** [MutantEmptyState]; a fixture draws the art on another page's [stage] and leaves out its [parts] and words. */
@Composable
internal fun EmptyStateBody(
    kind: MutantIllustrationKind,
    title: String,
    modifier: Modifier,
    text: String?,
    action: (@Composable () -> Unit)?,
    stage: String = kind.stage,
    parts: IllustrationParts = IllustrationParts.All,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val hidden = !parts.text
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IllustrationArt(kind, Modifier, parts, stage)
        Spacer(Modifier.height(ART_GAP))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LINE_GAP),
        ) {
            MutantText(
                title,
                Modifier.semantics { heading() },
                style =
                    TextStyle(
                        fontFamily = fonts?.display,
                        fontSize = TITLE_SIZE.sp,
                        color = if (hidden) Color.Transparent else colors.text,
                        textAlign = TextAlign.Center,
                        platformStyle = raster,
                    ).cssLine(MutantFaceMetrics.Handjet),
            )
            if (text != null) {
                MutantText(
                    text,
                    style =
                        TextStyle(
                            fontFamily = fonts?.text,
                            fontSize = TEXT_SIZE.sp,
                            color = if (hidden) Color.Transparent else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Rubik),
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.height(ACTION_GAP))
            action()
        }
    }
}

/** Under the art (`gap: 44px`), between the title and the text (`gap: 6px`), and before an action (D15). */
private val ART_GAP = 44.dp
private val LINE_GAP = 6.dp
private val ACTION_GAP = 20.dp

/** The title (Handjet `24px`) and the text (`14px`), sp. */
private const val TITLE_SIZE = 24
private const val TEXT_SIZE = 14
