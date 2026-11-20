package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.inkOnBody
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/**
 * A list row, 64 dp tall (taller only when its text needs it), its parts 12 dp apart: the [leadingIcon] on a 40 dp
 * `icon` tile whose drips hang 6 dp at most, the [headline] (Rubik 15/500) over the [supporting] line (Rubik 13,
 * secondary), and at the end either [trailing] (a [MutantBadge], say) or [trailingText] in the on-screen-display amber
 * (Handjet 20, the page's `12:04`). Given [onClick] the whole row is the button, fired on release, its press boiling
 * the tile; disabled, the tile loses its signal and the words grey. Put a [MutantDivider] between rows.
 */
@Composable
public fun MutantListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    key: String = "tile",
    interactionSource: MutableInteractionSource? = null,
) {
    ListItemBody(
        AnnotatedString(headline),
        modifier,
        supporting,
        leadingIcon,
        trailingText,
        trailing,
        onClick,
        enabled,
        key,
        interactionSource,
    )
}

/**
 * [MutantListItem]; [hidden] draws its words and icon transparent for a fixture's bare twin. [probe] is told the leading
 * icon tile's body instance ([MutantSurface]'s): for tests only, never for a consumer.
 */
@Composable
internal fun ListItemBody(
    headline: AnnotatedString,
    modifier: Modifier,
    supporting: String?,
    leadingIcon: ImageVector?,
    trailingText: String?,
    trailing: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    hidden: Boolean = false,
    probe: ((Instance) -> Unit)? = null,
    rowHeight: Dp = ROW,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard

    fun ink(c: Color) =
        when {
            hidden -> Color.Transparent
            !enabled -> colors.textDisabled
            else -> c
        }
    val click =
        if (onClick == null) {
            Modifier
        } else {
            Modifier
                .mutantHand(
                    enabled,
                ).clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        }
    Row(
        modifier
            .then(click)
            .ownWidthWhenUnbounded()
            .fillMaxWidth()
            .heightIn(min = rowHeight)
            .drawWithContent {
                drawContent()
                if (onClick != null && focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(1f, 1f, size.width / density - 1, size.height / density - 1, colors.focus)
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            MutantSurface(
                MutantBody.Icon,
                key,
                Modifier.size(TILE),
                state = if (enabled) BodyState.Rest else BodyState.Disabled,
                interactionSource = if (onClick == null) null else source,
                // The page's `data-over="6"`: a list keeps its drips short.
                profile = MutantBody.Icon.profile.with(overhang = OVERHANG),
                probe = probe,
            ) {
                Box(Modifier.size(TILE), contentAlignment = Alignment.Center) {
                    MutantIcon(
                        leadingIcon,
                        null,
                        Modifier.mutantCleanZone(),
                        tint = inkOnBody(ink(colors.text)),
                        size = ICON,
                    )
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LINE_GAP)) {
            BasicText(
                // A headline's own colours (a search result's match, B-44) give way to the row's grey or a bare twin's
                // transparency.
                if (hidden || !enabled) AnnotatedString(headline.text) else headline,
                style =
                    TextStyle(
                        fontFamily = fonts?.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = HEADLINE_SIZE.sp,
                        color = ink(colors.text),
                        platformStyle = raster,
                    ).cssLine(MutantFaceMetrics.Rubik),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                BasicText(
                    supporting,
                    style =
                        TextStyle(
                            fontFamily = fonts?.text,
                            fontSize = SUPPORTING_SIZE.sp,
                            color = ink(colors.textSecondary),
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Rubik),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            trailing != null -> {
                trailing()
            }

            trailingText != null -> {
                BasicText(
                    trailingText,
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = OSD_SIZE.sp,
                            color = ink(colors.textOsd),
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Handjet),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** The row (`height: 64px`), its gaps (`gap: 12px`), and the lines' (`gap: 2px`). */
private val ROW = 64.dp
private val GAP = 12.dp
private val LINE_GAP = 2.dp

/** The tile, its icon, and its drips' limit (`data-over="6"`). */
private val TILE = 40.dp
private val ICON = 22.dp
private const val OVERHANG = 6.0

/** The headline (`15px`, 500), the supporting line (`13px`) and the trailing OSD text (Handjet `20px`), sp. */
private const val HEADLINE_SIZE = 15
private const val SUPPORTING_SIZE = 13
private const val OSD_SIZE = 20
