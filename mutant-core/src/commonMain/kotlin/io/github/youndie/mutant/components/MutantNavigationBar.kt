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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.surface.MutantSlideRun
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.SlideRequest
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/**
 * One destination of [MutantNavigationBar]: its label, always shown, its icon, and an optional [badge]. The badge stands
 * at the icon's top-right corner and grows right with its text, past the cell where the cell is narrow; a whole number
 * above 99 reads «99+», any other text as it is given.
 */
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
 * is the page's amber count at the icon's top-right corner, growing right (B-204). A new [selectedIndex] slides the pill over from the cell it left
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

/**
 * [MutantNavigationBar]; [hidden] draws the icons and words transparent for a fixture's bare twin, the badge's box kept.
 * [probe] and [pillProbe] are told the bar's and the pill's body instances ([MutantSurface]'s): for tests only, never
 * for a consumer.
 */
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
    val clock = readMutantClock()
    val from = remember { PillFrom(selectedIndex) }
    from.follow(selectedIndex, items.size)
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
            // The screen's direction of reading: the pill's run goes the other way in a right-to-left layout (B-65).
            val side = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
            // Where the pill is drawn, for the icons it stands over (B-168).
            val place =
                if (chosen) {
                    PillPlace(
                        from,
                        clock,
                        selectedIndex,
                        cell.value.toDouble(),
                        minOf(ICON_BOX_W, cell).value.toDouble(),
                        side,
                        LocalMutantReduceMotion.current,
                        LocalMutantSteppedMotion.current,
                    )
                } else {
                    null
                }
            Layout(
                content = {
                    if (chosen) {
                        // Where the pill's run starts, from the cell it left; a pick under way starts from where it stands (B-171).
                        val dx = cell * (side * from.cells).toFloat()
                        MutantSurface(
                            MutantBody.Pill,
                            pillKey,
                            // The pill is a body inside the bar: the engine keeps it in the bar's clean zone.
                            Modifier.size(box).mutantCleanZone(),
                            slide = SlideRequest(selectedIndex, dx.value.toDouble(), run = from.run),
                            probe = pillProbe,
                        ) {
                            Box(Modifier.size(box), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(ICON).mutantCleanZone())
                            }
                        }
                    }
                    items.forEachIndexed { i, item ->
                        NavigationItem(item, i == selectedIndex, { onSelect(i) }, hidden, { place?.over(i) }) {
                            tops[i] = it
                        }
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
    pill: () -> Rect?,
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
    // On the pill the icon is the page's literal black, elsewhere the label's colour; read where the pill is drawn, not
    // from the pick: the pill reaches the chosen icon 330 ms after it, and passes over others on its way (B-168).
    val inks = !hidden && item.enabled
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
                Box(Modifier.size(ICON).mutantCleanZone()) {
                    MutantIcon(
                        item.icon,
                        null,
                        Modifier.underPill(pill, inks, onPill = false),
                        tint = word,
                        size = ICON,
                    )
                    if (inks) {
                        MutantIcon(
                            item.icon,
                            null,
                            Modifier.underPill(pill, true, onPill = true),
                            tint = INK,
                            size = ICON,
                        )
                    }
                }
                item.badge?.let {
                    BadgeBody(
                        navigationBadgeText(it),
                        Modifier.navigationBadgePlace(LocalMutantNavigationBarAsDesigned.current),
                        compact = true,
                        hidden = hidden,
                    )
                }
            }
            MutantText(
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

/**
 * The part of an icon on the pill ([onPill]) or off it, as the pill stands now: [pill] is its box from the icon's centre,
 * dp. An icon wholly on one side is drawn as it is, unclipped; one the pill's edge crosses is cut along a capsule inside
 * the pill's outline. With [inks] false the icon never turns, and is drawn whole.
 */
private fun Modifier.underPill(
    pill: () -> Rect?,
    inks: Boolean,
    onPill: Boolean,
): Modifier =
    drawWithContent {
        val box = if (inks) pill() else null
        // In the icon's pixels, half the outline's width inside the pill's box: the lime the black reads on.
        val inset = (MutantBody.Pill.profile.outline / 2).toFloat()
        val r =
            box?.let {
                Rect(
                    size.width / 2 + (it.left + inset) * density,
                    size.height / 2 + (it.top + inset) * density,
                    size.width / 2 + (it.right - inset) * density,
                    size.height / 2 + (it.bottom - inset) * density,
                )
            }
        val corner = r?.let { minOf(it.width, it.height) / 2 } ?: 0f
        val whole =
            r != null && r.left + corner <= 0 && r.right - corner >= size.width && r.top <= 0 && r.bottom >= size.height
        val none = r == null || r.right <= 0 || r.left >= size.width || r.bottom <= 0 || r.top >= size.height
        when {
            whole -> {
                if (onPill) drawContent()
            }

            none -> {
                if (!onPill) drawContent()
            }

            else -> {
                val capsule = Path().apply { addRoundRect(RoundRect(r, CornerRadius(corner))) }
                clipPath(capsule, if (onPill) ClipOp.Intersect else ClipOp.Difference) {
                    this@drawWithContent.drawContent()
                }
            }
        }
    }

/**
 * Where the pill is drawn now, from item [over]'s icon centre, dp: its box as the body slides it (`slid`, read from the
 * pill's own run: run home by `1 − (1 − p)²`, stretched `1 + .4 sin πp` along and squeezed `1 − .18 sin πp` across, a
 * re-aimed run's stretch eased from what it showed). While it runs, the reader reads every frame of the [clock].
 */
internal class PillPlace(
    private val from: PillFrom,
    private val clock: MutantClock,
    private val chosen: Int,
    private val cell: Double,
    private val width: Double,
    private val side: Int,
    private val instant: Boolean,
    private val steps: Boolean,
) {
    fun over(i: Int): Rect {
        val run = from.run
        val p = run.progress(clock.nowMs, instant, steps)
        if (p < 1) clock.frame
        val cx = side * cell * (chosen - i) + run.offsetX(p)
        val hw = width / 2 * jsToFixed(1 + run.stretchX(p), 2)
        val hh = ICON_BOX_H.value / 2 * jsToFixed(1 + run.stretchY(p), 2)
        return Rect((cx - hw).toFloat(), (-hh).toFloat(), (cx + hw).toFloat(), hh.toFloat())
    }
}

/**
 * Follows the selected index from composition without writing state: the pill slides over from the cell it left. A pick
 * while it is under way starts from where it stands: the pill's [run] carries that (B-171; B-168 did it here).
 */
internal class PillFrom(
    private var now: Int,
) {
    /** Where the pill's slide starts, in cells from the chosen one in reading order, from the cell it left; 0 when it does not slide. */
    var cells: Double = 0.0
        private set

    /** The pill's slide, shared with its body: the body re-aims it, this reads it. */
    val run: MutantSlideRun = MutantSlideRun()

    fun follow(
        to: Int,
        count: Int,
    ) {
        if (to == now) return
        cells = if (now in 0 until count && to in 0 until count) (now - to).toDouble() else 0.0
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

/**
 * The badge ([MutantBadge], compact) 2 dp above the icon box, its plate's left edge on the 22 dp icon's right edge, so a
 * longer count grows right, away from the icon (B-204). The page's `right: 6px` stands it 6 dp in from the icon box's
 * right instead, where a two-digit count covers half the icon; [LocalMutantNavigationBarAsDesigned] keeps that for the
 * parity cells.
 */
private val NAVIGATION_BADGE_UP = 2.dp
private val NAVIGATION_BADGE_RIGHT_AS_DESIGNED = 6.dp

/** The most a navigation badge counts: a whole number above it reads «99+» (B-204, the designer's answer 36). */
private const val NAVIGATION_BADGE_MAX = 99

/**
 * B-204's departure from the page, for the parity cells (research D20's registry): true stands the badge's right edge
 * 6 dp in from the icon box's right, as the page's `right: 6px` does; the kit's grows right from the icon's top-right
 * corner (the designer's answer 36).
 */
internal val LocalMutantNavigationBarAsDesigned = staticCompositionLocalOf { false }

/**
 * A badge's text as the bar shows it: a whole number above [NAVIGATION_BADGE_MAX], of any length, reads «99+»; any other
 * text as it is. Read by its digits, never parsed, so a count past `Long.MAX_VALUE` caps as well.
 */
internal fun navigationBadgeText(text: String): String {
    val digits = text.trimStart('0')
    val count = text.isNotEmpty() && text.all { it in '0'..'9' }
    val over = digits.length > NAVIGATION_BADGE_MAX.toString().length || (digits.isNotEmpty() && digits.toInt() > NAVIGATION_BADGE_MAX)
    return if (count && over) "$NAVIGATION_BADGE_MAX+" else text
}

/**
 * Stands the badge in the icon box: its plate's left edge on the icon's right edge, 2 dp above the box, so it grows right
 * whatever its count; mirrored in a right-to-left layout, its right edge on the icon's left. [asDesigned]: the page's
 * place, the right edge 6 dp in from the box's. The badge never takes the box's room.
 */
private fun Modifier.navigationBadgePlace(asDesigned: Boolean): Modifier =
    layout { measurable, constraints ->
        val p = measurable.measure(Constraints())
        val w = constraints.maxWidth
        // Where the box's centring puts the icon (BiasAlignment's rounding), so the corner is the icon's to the pixel.
        val icon = ICON.roundToPx()
        val iconRight = ((w - icon) / 2f).roundToInt() + icon
        val x = if (asDesigned) w - NAVIGATION_BADGE_RIGHT_AS_DESIGNED.roundToPx() - p.width else iconRight
        layout(w, constraints.maxHeight) { p.placeRelative(x, -NAVIGATION_BADGE_UP.roundToPx()) }
    }

/** The black on the pill: the page's literal `#05060c` in both themes, not «Копия»'s own ink. */
private val INK = Color(0xFF05060C)
