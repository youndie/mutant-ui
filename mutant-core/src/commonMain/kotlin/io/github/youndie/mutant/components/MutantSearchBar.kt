package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.roundToInt

/**
 * A search bar: the on-screen-display line in focus (a [MutantTextField] with its [prefix], `ПОИСК` on the page, and
 * the block caret) with a clear icon at its end, and under it, 24 dp apart, whatever [content] shows — the page's
 * [MutantSearchSuggestions], [MutantSearchResult] rows with a [MutantDivider] between them, and a
 * [MutantSearchResultLoading] row while results come.
 *
 * The clear icon (the kit's Close, 24 dp, in the text colour) shows while [query] is not empty and empties it, keeping
 * the field's focus; [clearLabel] names it to a screen reader. The keyboard's action is search, and [onSearch] hears
 * it with the query. The bar is as wide as its [modifier] allows; the page's is 328 dp.
 */
@Composable
public fun MutantSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String = "ПОИСК",
    placeholder: String? = null,
    onSearch: ((String) -> Unit)? = null,
    clearLabel: String = "Очистить",
    enabled: Boolean = true,
    key: String = "search",
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    SearchBarBody(
        query,
        onQueryChange,
        modifier,
        prefix,
        placeholder,
        onSearch,
        clearLabel,
        enabled,
        key,
        interactionSource,
        content = content,
    )
}

/** [MutantSearchBar]; [focusedAs] holds a fixture's focus, [hidden] draws its words, caret and icon transparent. */
@Composable
internal fun SearchBarBody(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier,
    prefix: String,
    placeholder: String?,
    onSearch: ((String) -> Unit)?,
    clearLabel: String,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    focusedAs: Boolean? = null,
    hidden: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // The clear icon takes focus when pressed and leaves with the query: the field takes it back. So does a suggestion
    // under it (B-168), which takes focus when pressed by a mouse or the keyboard and may leave as the query fills.
    val requester = remember { FocusRequester() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PART_GAP)) {
        TextFieldBody(
            query,
            onQueryChange,
            Modifier.fillMaxWidth(),
            prefix,
            placeholder,
            enabled,
            false,
            KeyboardOptions(imeAction = ImeAction.Search),
            key,
            interactionSource,
            focusedAs = focusedAs,
            hidden = hidden,
            keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke(query) }),
            focusRequester = requester,
            trailing =
                if (query.isEmpty()) {
                    null
                } else {
                    {
                        ClearIcon(enabled, clearLabel, hidden) {
                            onQueryChange("")
                            requester.requestFocus()
                        }
                    }
                },
        )
        CompositionLocalProvider(LocalSearchField provides requester) { content() }
    }
}

/** The field of the [MutantSearchBar] its content stands in, for a suggestion taken to hand focus back to (B-168). */
private val LocalSearchField = staticCompositionLocalOf<FocusRequester?> { null }

/**
 * The clear icon: 24 dp in the row, a touch on it widened to 48 dp by Compose's own minimum touch target, the hand
 * over it and the focus brackets around it from the keyboard (designed here, D15: the page draws it at rest only). It is
 * part of the field's clean zone, as the engine counts every `svg` in a slot: no mosaic or flow line runs under it.
 */
@Composable
private fun ClearIcon(
    enabled: Boolean,
    label: String,
    hidden: Boolean,
    onClear: () -> Unit,
) {
    val colors = MutantTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        Modifier
            .size(ICON)
            .mutantCleanZone()
            .mutantHand(enabled)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClickLabel = label) {
                onClear()
            }.drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        val w = size.width / density
                        val h = size.height / density
                        drawCornerBrackets(-3f, -3f, w + 3, h + 3, colors.focus)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        MutantIcon(
            MutantIcons.Close,
            label,
            tint =
                when {
                    hidden -> Color.Transparent
                    enabled -> colors.text
                    else -> colors.textDisabled
                },
            size = ICON,
        )
    }
}

/**
 * The suggestions under a [MutantSearchBar]: a [MutantChip] for each of [suggestions], 6 dp apart in a row and 8 dp
 * between rows when they wrap (Rubik 13/500, its label 14 dp from its sides, the page's). A press hands the suggestion to [onSuggestion]; the chip
 * named by [selected], if any, stands selected. Each chip's body is keyed by its place, `sg1`, `sg2`…, so a new set of
 * suggestions keeps the bodies where they were. Under a [MutantSearchBar], a suggestion taken hands focus back to the
 * bar's field, as its clear icon does: a chip pressed by a mouse or the keyboard takes focus, and leaves with it when
 * the suggestions go as the query fills.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MutantSearchSuggestions(
    suggestions: List<String>,
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
    selected: String? = null,
    enabled: Boolean = true,
) {
    SuggestionsBody(suggestions, onSuggestion, modifier, selected, enabled)
}

/** [MutantSearchSuggestions]; [hidden] draws the labels transparent for a bare twin. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SuggestionsBody(
    suggestions: List<String>,
    onSuggestion: (String) -> Unit,
    modifier: Modifier,
    selected: String?,
    enabled: Boolean,
    hidden: Boolean = false,
) {
    // A chip is 32 dp drawn in a 48 dp touch box. The page's rows stand 32 + 8 apart, so the boxes overlap by 8 dp
    // between rows, and the bar lays out the drawn rows: the 8 dp each box reaches above the first row and below the
    // last stay out of its height.
    val reach = (CHIP_TOUCH - CHIP_HEIGHT) / 2
    // Chrome lays each chip at its label's fractional width and the engine stands its body on the nearest pixel;
    // Compose would round the label up, and every chip after the first would stand a pixel late (B-44). Each chip
    // advances the row by that rounded width, drawn at its own.
    val measurer = rememberTextMeasurer()
    val label = buttonLabelStyle(CHIP_LABEL_SIZE, Color.Unspecified)
    val density = LocalDensity.current
    val field = LocalSearchField.current
    FlowRow(
        modifier.layout { m, c ->
            val extra = (reach * 2).roundToPx()
            val p =
                m.measure(
                    c.copy(
                        minHeight = c.minHeight + extra,
                        maxHeight = if (c.hasBoundedHeight) c.maxHeight + extra else c.maxHeight,
                    ),
                )
            layout(p.width, (p.height - extra).coerceAtLeast(c.minHeight)) { p.place(0, -reach.roundToPx()) }
        },
        horizontalArrangement = Arrangement.spacedBy(CHIP_GAP),
        verticalArrangement = Arrangement.spacedBy(CHIP_ROW_GAP - reach * 2),
    ) {
        suggestions.forEachIndexed { i, s ->
            val advance =
                remember(s, label, density) {
                    val l = measurer.measure(s, label, maxLines = 1, softWrap = false)
                    with(density) {
                        maxOf(
                            (l.getLineRight(0) - l.getLineLeft(0) + 2 * CHIP_PADDING.toPx()).roundToInt(),
                            CHIP_TOUCH.roundToPx(),
                        )
                    }
                }
            ChipBody(
                s,
                s == selected,
                {
                    onSuggestion(s)
                    field?.requestFocus()
                },
                Modifier.layout { m, c ->
                    val p = m.measure(c)
                    layout(minOf(advance, p.width), p.height) { p.place(0, 0) }
                },
                enabled,
                "sg${i + 1}",
                null,
                hidden = hidden,
                padding = CHIP_PADDING,
            )
        }
    }
}

/**
 * One result under a [MutantSearchBar]: a list row 60 dp tall, its [leadingIcon] on a 40 dp `icon` tile whose drips
 * hang 6 dp at most, the [headline] (Rubik 15/500) with the first match of [query] in it, case aside, drawn in the
 * accent colour, over the [supporting] line (Rubik 13, secondary). Given [onClick] the whole row is the button. Put a
 * [MutantDivider] between rows.
 */
@Composable
public fun MutantSearchResult(
    headline: String,
    query: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    key: String = "sr1",
    interactionSource: MutableInteractionSource? = null,
) {
    ListItemBody(
        searchMatch(headline, query, MutantTheme.colors.textAccent),
        modifier,
        supporting,
        leadingIcon,
        null,
        null,
        onClick,
        enabled,
        key,
        interactionSource,
        rowHeight = RESULT_ROW,
    )
}

/** [headline] with the first match of [query] in it, case aside, in [accent]; the headline alone when none. */
internal fun searchMatch(
    headline: String,
    query: String,
    accent: Color,
): AnnotatedString {
    val q = query.trim()
    val at = if (q.isEmpty()) -1 else headline.indexOf(q, ignoreCase = true)
    if (at < 0) return AnnotatedString(headline)
    return buildAnnotatedString {
        append(headline, 0, at)
        withStyle(SpanStyle(color = accent)) { append(headline, at, at + q.length) }
        append(headline, at + q.length, headline.length)
    }
}

/**
 * The row a [MutantSearchBar] shows while results come, 60 dp tall: a 40 dp [MutantCocoon] where the tile stands and
 * two 14 dp bars, 8 dp apart, of 70 % and 45 % of the row's text width. Its cocoons are keyed `[key]0`…`[key]2`; the
 * page's stand first on their stage, keys `0`, `1`, `2`.
 */
@Composable
public fun MutantSearchResultLoading(
    modifier: Modifier = Modifier,
    key: String = "",
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = RESULT_ROW),
        horizontalArrangement = Arrangement.spacedBy(RESULT_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MutantCocoon(Modifier.size(TILE), key = "${key}0")
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BAR_GAP)) {
            MutantCocoon(Modifier.fillMaxWidth(.7f).heightIn(min = BAR, max = BAR), key = "${key}1")
            MutantCocoon(Modifier.fillMaxWidth(.45f).heightIn(min = BAR, max = BAR), key = "${key}2")
        }
    }
}

/** The bar's column (`gap: 24px`). */
private val PART_GAP = 24.dp

/** The clear icon (`width="24"`). */
private val ICON = 24.dp

/** The suggestions' gaps (`gap: 8px 6px`: between rows, then between chips). */
private val CHIP_ROW_GAP = 8.dp
private val CHIP_GAP = 6.dp

/** The suggestion's label from its sides (`padding: 0 14px`; a filter chip's is 16). */
private val CHIP_PADDING = 14.dp

/** The suggestion's label, sp (MutantChip's). */
private const val CHIP_LABEL_SIZE = 13

/** The chip's drawn height and its touch box (MutantChip's). */
private val CHIP_HEIGHT = 32.dp
private val CHIP_TOUCH = 48.dp

/** A result row (`height: 60px`), its gap (`gap: 12px`), the tile (40), and the loading row's bars (14, `gap: 8px`). */
private val RESULT_ROW = 60.dp
private val RESULT_GAP = 12.dp
private val TILE = 40.dp
private val BAR = 14.dp
private val BAR_GAP = 8.dp
