package io.github.youndie.mutant.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

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
 * runs on one line and ends in an ellipsis; a larger font scale grows the row.
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
) {
    DataTableBody(rows, columns, modifier, selected, onSelectedChange, enabled, key)
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

    fun styleOf(s: MutantTableCellStyle) =
        when (s) {
            MutantTableCellStyle.Text -> text
            MutantTableCellStyle.Osd -> osd
            MutantTableCellStyle.OsdSecondary -> osdSecondary
        }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().heightIn(min = HEADER_ROW), verticalAlignment = Alignment.CenterVertically) {
            if (onSelectedChange != null) Box(Modifier.width(CHECK_COLUMN))
            for (c in columns) Cell(c.width, c.header, header)
        }
        MutantDivider(key = "0")
        rows.forEachIndexed { i, row ->
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
                        CheckBoxControl(on, enabled, "$key$i", source, hidden, blocks = false)
                    }
                }
                for (c in columns) Cell(c.width, c.cell(row), styleOf(c.style))
            }
            MutantDivider(key = "${i + 1}")
        }
    }
}

@Composable
private fun RowScope.Cell(
    width: Dp?,
    text: String,
    style: TextStyle,
) {
    BasicText(
        text,
        if (width == null) Modifier.weight(1f) else Modifier.width(width),
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The header row (`height: 36px`) and a body row (`height: 48px`), the touch target. */
private val HEADER_ROW = 36.dp
private val ROW = 48.dp

/** The checkbox column (`grid-template-columns: 40px …`). */
private val CHECK_COLUMN = 40.dp

/** The header (Handjet `18px`), a text cell (`14px`, 500) and an OSD cell (Handjet `20px`), sp. */
private const val HEADER_SIZE = 18
private const val TEXT_SIZE = 14
private const val OSD_SIZE = 20
