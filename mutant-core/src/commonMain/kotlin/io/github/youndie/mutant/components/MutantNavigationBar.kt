package io.github.youndie.mutant.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.SlideRequest
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/** One destination of [MutantNavigationBar]: its label, always shown, its icon, and an optional [badge]. */
@Immutable
public class MutantNavigationItem(
    public val label: String,
    public val icon: ImageVector,
    public val badge: String? = null,
    public val enabled: Boolean = true,
)

/**
 * The navigation bar: the `bar` body, 68 dp tall, across what it is given, its items in equal cells, each the whole
 * cell's target. An item is a 22 dp icon in a 56 × 30 box over its label (Rubik 11/500, always shown); the selected
 * one stands on the `pill`, a lime body under its icon, and its label takes the text colour. A [MutantNavigationItem.badge]
 * is the page's amber count over the icon's corner. A new [selectedIndex] slides the pill over from the cell it left
 * with the body's stretch (330 ms), at once under reduce motion; -1 shows no pill. The bar stands above [windowInsets],
 * the navigation bar by default, and draws nothing under it.
 */
@Composable
public fun MutantNavigationBar(
    items: List<MutantNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    key: String = "navbar",
    pillKey: String = "navpill",
) {
    NavigationBarBody(items, selectedIndex, onSelect, modifier.windowInsetsPadding(windowInsets), key, pillKey)
}

/** [MutantNavigationBar]; [hidden] draws the icons and words transparent for a fixture's bare twin, the badge's box kept. */
@Composable
internal fun NavigationBarBody(
    items: List<MutantNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    key: String,
    pillKey: String,
    hidden: Boolean = false,
    probe: ((Instance) -> Unit)? = null,
    pillProbe: ((Instance) -> Unit)? = null,
) {
    val from = remember { PillFrom(selectedIndex) }
    from.follow(selectedIndex)
    MutantSurface(
        MutantBody.Bar,
        key,
        modifier.fillMaxWidth().heightIn(min = BAR),
        // The page's `data-marks="0"`: no dead pixels or marks on the bar.
        profile = MutantBody.Bar.profile.with(marks = false),
        probe = probe,
    ) {
        // As wide as the bar, as tall as its items: its height is a minimum now (B-61), and filling it took the window.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val n = items.size.coerceAtLeast(1)
            // Unbounded (a horizontally scrolling row): each destination takes the design's cell, never Infinity.
            val bounded = constraints.hasBoundedWidth
            val cell = if (bounded) maxWidth / n else DESIGN_CELL
            val chosen = selectedIndex in items.indices
            // Captured by the pill's content, so the lambda is not a singleton whose name moves with every edit (ABI).
            val box = DpSize(ICON_BOX_W, ICON_BOX_H)
            // Where an item's icon box stands in its cell, from the measure of the items (written as they measure).
            val tops = remember(n) { IntArray(n) }
            Layout(
                content = {
                    if (chosen) {
                        // The cell the pill came from, on the screen: to the other side in a right-to-left layout (B-65).
                        val side = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
                        val dx = if (from.at in items.indices) cell * (side * (from.at - selectedIndex)) else 0.dp
                        MutantSurface(
                            MutantBody.Pill,
                            pillKey,
                            // The pill is a body inside the bar: the engine keeps it in the bar's clean zone.
                            Modifier.size(box).mutantCleanZone(),
                            slide = SlideRequest(selectedIndex, dx.value.toDouble()),
                            probe = pillProbe,
                        ) {
                            Box(Modifier.size(box), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(ICON).mutantCleanZone())
                            }
                        }
                    }
                    items.forEachIndexed { i, item ->
                        NavigationItem(item, i == selectedIndex, { onSelect(i) }, hidden) { tops[i] = it }
                    }
                },
                modifier = Modifier.selectableGroup(),
            ) { measurables, constraints ->
                val w = if (bounded) constraints.maxWidth else DESIGN_CELL.roundToPx() * n
                val cells = IntArray(n + 1) { (w.toFloat() * it / n).roundToInt() }
                // The page's 68 dp, or what the tallest item needs: a label grown with the font grows the bar (B-61).
                val items = measurables.drop(if (chosen) 1 else 0)
                val tallest =
                    items.withIndex().maxOfOrNull { (i, m) ->
                        m.maxIntrinsicHeight((cells[i + 1] - cells[i]).coerceAtLeast(0))
                    }
                        ?: 0
                val h = maxOf(constraints.minHeight, BAR.roundToPx(), tallest)
                // The pill is never wider than its cell: twelve destinations in 360 dp leave 30 dp a cell (B-66).
                val pill =
                    if (chosen) {
                        measurables.first().measure(
                            Constraints(maxWidth = (cells[selectedIndex + 1] - cells[selectedIndex]).coerceAtLeast(0)),
                        )
                    } else {
                        null
                    }
                val placed =
                    items.mapIndexed { i, m ->
                        m.measure(Constraints.fixed((cells[i + 1] - cells[i]).coerceAtLeast(0), h))
                    }
                layout(w, h) {
                    if (pill != null) {
                        val x =
                            cells[selectedIndex] +
                                ((cells[selectedIndex + 1] - cells[selectedIndex] - pill.width) / 2f).roundToInt()
                        pill.placeRelative(x, tops[selectedIndex])
                    }
                    // Mirrored in a right-to-left layout, the first destination at the right (B-65).
                    placed.forEachIndexed { i, p -> p.placeRelative(cells[i], 0) }
                }
            }
        }
    }
}

/** One item: the whole cell its target, the icon box and the label centred in it, 3 dp apart. */
@Composable
private fun NavigationItem(
    item: MutantNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    hidden: Boolean,
    top: (Int) -> Unit,
) {
    val colors = MutantTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val word =
        when {
            hidden -> Color.Transparent
            !item.enabled -> colors.textDisabled
            selected -> colors.text
            else -> colors.textSecondary
        }
    // On the pill the icon is the page's literal black; elsewhere it is the label's colour.
    val glyph = if (selected && !hidden && item.enabled) INK else word
    Box(
        Modifier
            .mutantHand(item.enabled)
            .selectable(selected, source, indication = null, enabled = item.enabled, role = Role.Tab, onClick = onClick)
            .drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(1f, 1f, size.width / density - 1, size.height / density - 1, colors.focus)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .layout { measurable, c ->
                    val p = measurable.measure(c.copy(minWidth = 0, minHeight = 0))
                    // The icon box's top in the cell, as the Box centres the column: the pill stands there.
                    top(((c.maxHeight - p.height) / 2f).roundToInt())
                    layout(p.width, p.height) { p.place(0, 0) }
                }
                // The whole cell wide: the icon box and the label each centre on the cell, as the page's grid centres
                // them, and not on a column whose width the label sets.
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GAP),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(ICON_BOX_W, ICON_BOX_H), contentAlignment = Alignment.Center) {
                // The engine's clean zone is its text runs, its icons and its bodies, not the boxes around them.
                MutantIcon(item.icon, null, Modifier.mutantCleanZone(), tint = glyph, size = ICON)
                item.badge?.let {
                    BadgeBody(
                        it,
                        Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentSize(Alignment.TopEnd, unbounded = true)
                            .offset(-BADGE_RIGHT, -BADGE_UP),
                        compact = true,
                        hidden = hidden,
                    )
                }
            }
            BasicText(
                item.label,
                Modifier.padding(horizontal = LABEL_MARGIN).mutantCleanZone(),
                style =
                    TextStyle(
                        fontFamily = LocalMutantFonts.current?.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = LABEL_SIZE.sp,
                        color = word,
                        platformStyle = LocalMutantTextRaster.current,
                    ).cssLine(MutantFaceMetrics.Rubik),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Follows the selected index from composition without writing state: the pill slides from where it stood. */
internal class PillFrom(
    private var now: Int,
) {
    var at: Int = -1
        private set

    fun follow(to: Int) {
        if (to == now) return
        at = now
        now = to
    }
}

/** The bar's height (the page's `height: 68px`). */
private val BAR = 68.dp

/** The design's bar, 328 dp over four destinations: a cell's width where the bar is given no width of its own. */
private val DESIGN_CELL = 82.dp

/** The icon's box, the pill's size, and the icon in it (the page's 56 × 30 and 22). */
private val ICON_BOX_W = 56.dp
private val ICON_BOX_H = 30.dp
private val ICON = 22.dp

/** Between the icon box and the label (`gap: 3px`); the label's size, sp (`font-size: 11px; font-weight: 500`). */
private val GAP = 3.dp
private const val LABEL_SIZE = 11

/** A label too long for its cell keeps 4 dp from the cell's sides, then an ellipsis (designed here, D15). */
private val LABEL_MARGIN = 4.dp

/** The badge ([MutantBadge], compact) 6 dp in from the icon box's right and 2 dp above it. */
private val BADGE_RIGHT = 6.dp
private val BADGE_UP = 2.dp

/** The black on the pill: the page's literal `#05060c` in both themes, not «Копия»'s own ink. */
private val INK = Color(0xFF05060C)
