package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.surface.mutantPlaceKey
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine

/**
 * The top app bar: no body, a 56 dp row — the [navigationIcon], the [title] in Handjet at 26 taking what is left,
 * an on-screen-display [status] in amber (`CH 03 · SP`), then the [actions] — 12 dp apart, over a 2 dp tracking
 * divider. A long title ends in an ellipsis before the status; in a bar too narrow for both, the status goes first,
 * once the title would keep less than the status's own width (or less than its whole, when shorter), and then the
 * title is cut; the navigation icon and the actions keep their widths. It stands below [windowInsets], the status bar by default, and draws nothing under it; the screen's
 * ground shows there. The row has no side margins: the screen gives them, as the page's column does.
 *
 * [key] seeds its body: by default its place in the composition and its body type (B-206), so two unkeyed ones are
 * two bodies and one keeps its body whatever its words or state; in a list, give it from the item's id.
 */
@Composable
public fun MutantTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    status: String? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    key: String = mutantPlaceKey("topbar"),
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    Column(modifier.ownWidthWhenUnbounded().windowInsetsPadding(windowInsets)) {
        Layout(
            content = {
                if (navigationIcon != null) Box(Modifier.layoutId(TopAppBarPart.Navigation)) { navigationIcon() }
                MutantText(
                    title,
                    Modifier.layoutId(TopAppBarPart.Title),
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = TITLE_SIZE.sp,
                            color = colors.text,
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Handjet),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                if (status != null) {
                    MutantText(
                        status,
                        Modifier.layoutId(TopAppBarPart.Status),
                        style =
                            TextStyle(
                                fontFamily = fonts?.display,
                                fontSize = STATUS_SIZE.sp,
                                letterSpacing = STATUS_TRACKING.sp,
                                color = colors.textOsd,
                                platformStyle = raster,
                            ).cssLine(MutantFaceMetrics.Handjet),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Row(
                    Modifier.layoutId(TopAppBarPart.Actions),
                    horizontalArrangement = Arrangement.spacedBy(GAP),
                    verticalAlignment = Alignment.CenterVertically,
                ) { actions() }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = BAR),
            measurePolicy = TopAppBarRow,
        )
        MutantDivider(key = key)
    }
}

private enum class TopAppBarPart { Navigation, Title, Status, Actions }

private val IntrinsicMeasurable.topAppBarPart: Any? get() = (parentData as? LayoutIdParentData)?.layoutId

/**
 * The bar's row: the navigation icon and the actions keep their own widths, the title takes what is left, 12 dp apart,
 * each centred on the row's height. The status stands while the title keeps at least as much room as the status
 * itself takes, or its whole width when that is less; past that the status goes first, and the title then ends in an
 * ellipsis (B-187, the designer's answer 31 to B-168). Gone, it is not placed, so neither the eye nor a reader finds it.
 */
private object TopAppBarRow : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val gap = GAP.roundToPx()
        val part = { p: TopAppBarPart -> measurables.firstOrNull { it.topAppBarPart == p } }
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val nav = part(TopAppBarPart.Navigation)?.measure(loose)
        val navW = nav?.width?.takeIf { it > 0 }?.plus(gap) ?: 0
        val actions =
            part(TopAppBarPart.Actions)!!.measure(
                if (loose.hasBoundedWidth) loose.copy(maxWidth = (loose.maxWidth - navW).coerceAtLeast(0)) else loose,
            )
        val actionsW = if (actions.width > 0) actions.width + gap else 0
        val titleM = part(TopAppBarPart.Title)!!
        val whole = titleM.maxIntrinsicWidth(constraints.maxHeight)
        val statusM = part(TopAppBarPart.Status)
        val statusP = statusM?.measure(Constraints(maxHeight = constraints.maxHeight))
        val statusW = statusP?.let { it.width + gap } ?: 0
        val room =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth - navW - actionsW
            } else {
                whole + statusW
            }
        val status = statusP?.takeIf { room - statusW >= minOf(whole, it.width) }
        val titleW = (room - if (status != null) statusW else 0).coerceAtLeast(0)
        val title = titleM.measure(Constraints(titleW, titleW, 0, constraints.maxHeight))
        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                navW + titleW + (if (status != null) statusW else 0) + actionsW
            }
        val rowH =
            maxOf(nav?.height ?: 0, title.height, status?.height ?: 0, actions.height)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), rowH) {
            var x = 0
            // Centred as the row's `CenterVertically` centres, rounding the half pixel the same way.
            val place = { p: Placeable ->
                p.placeRelative(x, Alignment.CenterVertically.align(p.height, rowH))
                x += p.width + gap
            }
            nav?.takeIf { it.width > 0 }?.let(place)
            place(title)
            status?.let(place)
            if (actions.width > 0) place(actions)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = row(measurables) { it.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int =
        row(measurables) {
            if (it.topAppBarPart == TopAppBarPart.Title || it.topAppBarPart == TopAppBarPart.Status) {
                0
            } else {
                it.minIntrinsicWidth(height)
            }
        }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.maxOfOrNull { it.maxIntrinsicHeight(Constraints.Infinity) } ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.maxOfOrNull { it.minIntrinsicHeight(Constraints.Infinity) } ?: 0

    /** The parts' widths in a row, 12 dp between those that take any. */
    private fun IntrinsicMeasureScope.row(
        measurables: List<IntrinsicMeasurable>,
        width: (IntrinsicMeasurable) -> Int,
    ): Int {
        val ws = measurables.map(width).filter { it > 0 }
        return ws.sum() + GAP.roundToPx() * (ws.size - 1).coerceAtLeast(0)
    }
}

/** The bar's row (the page's `height: 56px`); the divider under it is [MutantDivider]'s 2 dp. */
private val BAR = 56.dp

/** Between the icon, the title, the status and the actions (the page's `gap: 12px`). */
private val GAP = 12.dp

/** The title's and the status's sizes, sp, and the status's tracking (`letter-spacing: 1px`). */
private const val TITLE_SIZE = 26
private const val STATUS_SIZE = 20
private const val STATUS_TRACKING = 1
