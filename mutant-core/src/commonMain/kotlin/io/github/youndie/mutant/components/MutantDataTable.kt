package io.github.youndie.mutant.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantGrid
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/** How a [MutantTableColumn]'s cells are set: the page's name, length and channel columns. */
public enum class MutantTableCellStyle {
    /** Rubik 14/500 in the text colour (the page's record name). */
    Text,

    /** Handjet 20 in the text colour (the page's length, `46:00`). */
    Osd,

    /** Handjet 20 in the secondary colour (the page's channel, `CH 03`). */
    OsdSecondary,
}

/**
 * One column of a [MutantDataTable]: its [header] (set in the on-screen-display amber), its [width] — null takes what
 * the fixed columns leave, as the page's `minmax(0, 1fr)` — the [style] of its cells, and the [cell] text of each row.
 * A fixed [width] is the least the column takes: where its widest word (header or cell) and the 8 dp gap at its end need
 * more, at a larger font scale or with longer words, it is as wide as that (B-202), so a number is never cut.
 */
public class MutantTableColumn<T>(
    public val header: String,
    public val width: Dp? = null,
    public val style: MutantTableCellStyle = MutantTableCellStyle.Text,
    public val cell: (T) -> String,
)

/**
 * A data table: a 36 dp header in Handjet 18 (tracked 1, the on-screen-display amber) over 48 dp rows, a tracking
 * divider under the header and under every row. Given [onSelectedChange], a 40 dp column of checkboxes leads each row
 * (body `box`, 22 dp, the acid fill and the tick when selected) and the whole row is the toggle, so its target is the
 * row (designed here, D15: the page presses only the box). The rows named in [selected] stand selected. A cell's text
 * runs on one line and ends in an ellipsis, with 8 dp at each cell's end so it never meets the next column's words;
 * a larger font scale grows the row, and the fixed columns as far as their words need (B-202). The free columns take the
 * rest, their names ellipsised, down to each one's header word; narrower than that, the table keeps that width and
 * scrolls sideways inside its slot, the header with the rows, cut at the slot's sides: by a finger, a sideways wheel
 * (shift and the wheel) or a reader's scroll action — a mouse drag scrolls nothing, as in every Compose scroll. Given
 * no width at all (a horizontal scroll), each free column takes its widest word, as the content's own width (B-168).
 *
 * A row is matched by its place unless [rowKey] names what it shows. Given a key, a row the app moves (a sort, a row
 * inserted above) takes its box, its pour and its press with it, and the box keeps its slime; without one, a moved
 * selection pours out of the place it left and into the place it reached, the wrong record standing chosen meanwhile.
 * Keys must be unique among the rows, as a lazy list's are.
 */
@Composable
public fun <T> MutantDataTable(
    rows: List<T>,
    columns: List<MutantTableColumn<T>>,
    modifier: Modifier = Modifier,
    selected: Set<Int> = emptySet(),
    onSelectedChange: ((index: Int, selected: Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    key: String = "trow",
    rowKey: ((row: T) -> Any)? = null,
) {
    DataTableBody(rows, columns, modifier, selected, onSelectedChange, enabled, key, rowKey = rowKey)
}

/** [MutantDataTable]; [hidden] draws its words and ticks transparent for a fixture's bare twin. */
@Composable
internal fun <T> DataTableBody(
    rows: List<T>,
    columns: List<MutantTableColumn<T>>,
    modifier: Modifier,
    selected: Set<Int>,
    onSelectedChange: ((Int, Boolean) -> Unit)?,
    enabled: Boolean,
    key: String,
    hidden: Boolean = false,
    rowKey: ((T) -> Any)? = null,
) {
    val colors = MutantTheme.colors
    val styles = MutantTheme.textStyles
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current

    fun ink(c: Color) =
        when {
            hidden -> Color.Transparent
            !enabled -> colors.textDisabled
            else -> c
        }
    val header = styles.osd(HEADER_SIZE.sp).copy(color = ink(colors.textOsd), platformStyle = raster)
    val text =
        TextStyle(
            fontFamily = fonts?.text,
            fontWeight = FontWeight.Medium,
            fontSize = TEXT_SIZE.sp,
            color = ink(colors.text),
            platformStyle = raster,
        ).cssLine(MutantFaceMetrics.Rubik)
    val osd = styles.display(OSD_SIZE.sp).copy(color = ink(colors.text), platformStyle = raster)
    val osdSecondary = osd.copy(color = ink(colors.textSecondary))
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    fun styleOf(s: MutantTableCellStyle) =
        when (s) {
            MutantTableCellStyle.Text -> text
            MutantTableCellStyle.Osd -> osd
            MutantTableCellStyle.OsdSecondary -> osdSecondary
        }
    // The columns' widths in pixels, from the words at this font scale (B-202): a fixed column is its own width or, where
    // its widest word and the gap need more, that; a free column takes the rest, down to its header and the gap.
    val widths =
        remember(rows, columns, density, header, text, osd, measurer, onSelectedChange != null) {
            DataTableWidths.of(rows, columns, density, measurer, header, ::styleOf, onSelectedChange != null)
        }
    val scroll = remember { DataTableScroll() }
    val reverse = ScrollableDefaults.reverseDirection(LocalLayoutDirection.current, Orientation.Horizontal, false)
    val scrolls = scroll.max > 0
    Column(
        modifier
            .then(
                if (scrolls) {
                    Modifier.semantics {
                        horizontalScrollAxisRange = ScrollAxisRange({ scroll.offset }, { scroll.max.toFloat() })
                        scrollBy { x, _ -> scroll.state.dispatchRawDelta(x) != 0f }
                    }
                } else {
                    Modifier
                },
            ).scrollable(scroll.state, Orientation.Horizontal, enabled = scrolls, reverseDirection = reverse)
            .drawWithContent {
                if (scroll.max > 0) {
                    // Scrolling, what runs past the sides is cut there; above and below, the drips and the dividers run
                    // free, as at rest.
                    clipRect(0f, -size.height, size.width, 2 * size.height) { this@drawWithContent.drawContent() }
                } else {
                    drawContent()
                }
            }.layout { m, c ->
                if (!c.hasBoundedWidth) {
                    // No width at all: the free columns would get nothing and every name vanish (B-168). The table is
                    // its content's width instead: the fixed columns and each free one as wide as its widest word.
                    scroll.max = 0
                    val p = m.measure(c.copy(maxWidth = maxOf(c.minWidth, widths.whole)))
                    return@layout layout(p.width, p.height) { p.place(0, 0) }
                }
                if (c.maxWidth >= widths.least) {
                    scroll.max = 0
                    val p = m.measure(c)
                    return@layout layout(p.width, p.height) { p.place(0, 0) }
                }
                // Narrower than its columns can go (B-202): the table is laid out at its least width and scrolls sideways
                // inside the slot, its header with its rows.
                val p = m.measure(c.copy(minWidth = widths.least, maxWidth = widths.least))
                scroll.max = p.width - c.maxWidth
                layout(c.maxWidth, p.height) {
                    val at = scroll.offset.roundToInt().coerceIn(0, scroll.max)
                    p.placeRelative(-at, 0)
                }
            },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = HEADER_ROW), verticalAlignment = Alignment.CenterVertically) {
            if (onSelectedChange != null) Box(Modifier.width(CHECK_COLUMN))
            columns.forEachIndexed { j, c -> Cell(widths.fixed[j], c.header, header) }
        }
        MutantDivider(key = "0")
        rows.forEachIndexed { i, row ->
            // A keyed row's composition, its press and its box's pour follow what it shows; its slime follows too.
            val id = rowKey?.invoke(row)
            val slime = if (id == null) "$key$i" else "$key:$id"
            androidx.compose.runtime.key(id ?: i) {
                val on = i in selected
                val source = remember { MutableInteractionSource() }
                val toggle =
                    if (onSelectedChange == null) {
                        Modifier
                    } else {
                        Modifier.mutantHand(enabled).toggleable(
                            on,
                            source,
                            indication = null,
                            enabled = enabled,
                            role = Role.Checkbox,
                        ) { onSelectedChange(i, it) }
                    }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = ROW).then(toggle),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onSelectedChange != null) {
                        Box(Modifier.width(CHECK_COLUMN)) {
                            CheckBoxControl(on, enabled, slime, source, hidden, blocks = false)
                        }
                    }
                    columns.forEachIndexed { j, c -> Cell(widths.fixed[j], c.cell(row), styleOf(c.style)) }
                }
            }
            MutantDivider(key = "${i + 1}")
        }
    }
}

/**
 * The columns' widths at one font scale (B-202), in pixels. [fixed] holds each column's width as a [Dp], null for a free
 * one; [least] is the narrowest the table goes before it scrolls, each free column at its header and the gap; [whole]
 * is its width given no limit, each free column at its widest word and the gap.
 */
private class DataTableWidths(
    val fixed: List<Dp?>,
    val least: Int,
    val whole: Int,
) {
    companion object {
        fun <T> of(
            rows: List<T>,
            columns: List<MutantTableColumn<T>>,
            density: Density,
            measurer: TextMeasurer,
            header: TextStyle,
            styleOf: (MutantTableCellStyle) -> TextStyle,
            boxes: Boolean,
        ): DataTableWidths =
            with(density) {
                val gap = DATA_TABLE_GAP.roundToPx()

                fun widest(col: MutantTableColumn<T>) =
                    maxOf(
                        tableWordWidth(measurer, col.header, header),
                        rows.maxOfOrNull { tableWordWidth(measurer, col.cell(it), styleOf(col.style)) } ?: 0,
                    )
                // A numeric column is as wide as its words and the gap, never under the width it was given.
                val fixedPx = columns.map { col -> col.width?.let { maxOf(it.roundToPx(), widest(col) + gap) } }
                val free = columns.filter { it.width == null }
                val box = if (boxes) CHECK_COLUMN.roundToPx() else 0
                val set = box + fixedPx.sumOf { it ?: 0 }
                val headerMin = free.maxOfOrNull { tableWordWidth(measurer, it.header, header) + gap } ?: 0
                val wordMax = free.maxOfOrNull { widest(it) + gap } ?: 0
                DataTableWidths(
                    fixed = fixedPx.map { it?.toDp() },
                    least = set + headerMin * free.size,
                    whole = set + wordMax * free.size,
                )
            }
    }
}

/** The table's sideways scroll, given only when it is narrower than its [DataTableWidths.least] (B-202). */
@Stable
private class DataTableScroll {
    var offset by mutableFloatStateOf(0f)
    var max by mutableIntStateOf(0)

    val state =
        ScrollableState { delta ->
            val whole = offset + delta
            val to = whole.coerceIn(0f, max.toFloat())
            // Unclamped, all of it, exactly, as `ScrollState` answers: Compose's bring-into-view stops at a consumption a
            // float's rounding short of what it asked (B-184).
            val used = if (to == whole) delta else to - offset
            offset = to
            used
        }
}

@Composable
private fun RowScope.Cell(
    width: Dp?,
    text: String,
    style: TextStyle,
) {
    // The gap stands at each cell's end (B-202), so an ellipsis or a wide word never meets the next column's ink.
    MutantText(
        text,
        (if (width == null) Modifier.weight(1f) else Modifier.width(width)).padding(end = DATA_TABLE_GAP),
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** How wide [text] is set on one line in [style], in pixels, as the cell's text measures itself under no width. */
private fun tableWordWidth(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
): Int = measurer.measure(text, style, softWrap = false, maxLines = 1).size.width

/** The header row (`height: 36px`) and a body row (`height: 48px`), the touch target. */
private val HEADER_ROW = 36.dp
private val ROW = 48.dp

/** The checkbox column (`grid-template-columns: 40px …`). */
private val CHECK_COLUMN = 40.dp

/**
 * The room at a cell's end, two grid units (designed here, D15; B-202): the page's grid has no gap, as its words are
 * short; at a larger font scale a column's words would meet the next one's.
 */
private val DATA_TABLE_GAP = (MutantGrid.UNIT * 2).dp

/** The header (Handjet `18px`), a text cell (`14px`, 500) and an OSD cell (Handjet `20px`), sp. */
private const val HEADER_SIZE = 18
private const val TEXT_SIZE = 14
private const val OSD_SIZE = 20
