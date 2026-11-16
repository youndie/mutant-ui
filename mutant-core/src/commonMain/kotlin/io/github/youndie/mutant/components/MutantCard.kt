package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.inkOnBody
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/**
 * A card (body `card`, at least 80 dp tall) with dead pixels in one corner: [content] stands 14 dp from the top and
 * 16 from the sides, kept clean — no flow line and no dead pixel enters it. [MutantCardTitle] and
 * [MutantCardSubtitle] are the design's two lines. Given [onClick], the whole card is the button; disabled, it loses
 * its signal and its texts grey.
 */
@Composable
public fun MutantCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    key: String = "card",
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val click =
        if (onClick == null) {
            Modifier
        } else {
            Modifier
                .mutantHand(
                    enabled,
                ).clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        }
    MutantSurface(
        MutantBody.Card,
        key,
        modifier.defaultMinSize(minWidth = MIN_WIDTH, minHeight = MIN_HEIGHT).then(click),
        state = if (enabled) BodyState.Rest else BodyState.Disabled,
        focused = focused && keyboard,
        interactionSource = if (onClick == null) null else source,
    ) {
        CompositionLocalProvider(LocalCardEnabled provides enabled) {
            Box(Modifier.padding(horizontal = PADDING_X, vertical = PADDING_Y), contentAlignment = Alignment.TopStart) {
                // The content's own box is the clean zone (the engine's `textZone` over the card's texts).
                Column(
                    Modifier.wrapContentSize(Alignment.TopStart).mutantCleanZone(),
                    verticalArrangement = Arrangement.spacedBy(GAP),
                    content = content,
                )
            }
        }
    }
}

/** The card's title: Rubik 500 at 16, the text's colour (grey on a disabled card). One line. */
@Composable
public fun MutantCardTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    CardLine(text, modifier, TITLE_SIZE, FontWeight.Medium, MutantTheme.colors.text)
}

/** The card's second line: Rubik at 13, the secondary text's colour (grey on a disabled card). One line. */
@Composable
public fun MutantCardSubtitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    CardLine(text, modifier, SUBTITLE_SIZE, FontWeight.Normal, MutantTheme.colors.textSecondary)
}

@Composable
private fun CardLine(
    text: String,
    modifier: Modifier,
    size: Int,
    weight: FontWeight,
    color: Color,
) {
    val colors = MutantTheme.colors
    val hidden = LocalCardHidden.current
    val tint =
        when {
            hidden -> Color.Transparent
            LocalCardEnabled.current -> inkOnBody(color)
            else -> colors.textDisabled
        }
    BasicText(
        text,
        modifier,
        style =
            TextStyle(
                fontFamily = LocalMutantFonts.current?.text,
                fontWeight = weight,
                fontSize = size.sp,
                color = tint,
                platformStyle = LocalMutantTextRaster.current,
            ).cssLine(MutantFaceMetrics.Rubik),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Whether the card around a line is enabled: its lines grey with it. */
internal val LocalCardEnabled = staticCompositionLocalOf { true }

/** A fixture's bare twin: the lines laid out, transparent. False in the kit. */
internal val LocalCardHidden = staticCompositionLocalOf { false }

/** The card's least size (the page's `height: 80px`; the width it takes given none, research D15). */
private val MIN_HEIGHT = 80.dp
private val MIN_WIDTH = 200.dp

/** The content from the edges and between its lines (the page's `padding: 14px 16px; gap: 4px`). */
private val PADDING_X = 16.dp
private val PADDING_Y = 14.dp
private val GAP = 4.dp

/** The title and subtitle, sp. */
private const val TITLE_SIZE = 16
private const val SUBTITLE_SIZE = 13
