package io.github.youndie.mutant.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantMotion
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.max
import kotlin.math.pow

/** One tab of [MutantTabs]: its label; disabled, it greys and takes no click. */
@Immutable
public class MutantTab(
    public val label: String,
    public val enabled: Boolean = true,
)

/**
 * Tabs: no body, the labels in a row 24 dp apart, the selected one in the accent. Under it stands a 4 dp acid bar
 * as wide as its label, ringed in black, with an echo of three fading strokes to its right. A new [selectedIndex]
 * sends the bar across in 330 ms, stepped twelve times a second, its echo trailing behind it; under reduce motion it
 * lands at once. An index outside the tabs, -1 for none, shows no bar. Each tab is a target 48 dp tall reaching half
 * the gap to each side (at least 48 wide), so the row stands 12 dp wider each side and 1 dp taller each way than
 * what it draws; keyboard focus draws the corner brackets around a tab. Tabs that need more than the row's width keep
 * their own: the row scrolls, and brings the focused tab and a newly selected one into view (B-60).
 */
@Composable
public fun MutantTabs(
    tabs: List<MutantTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MutantTheme.colors
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val steps = LocalMutantSteppedMotion.current
    val marks = remember { TabMarks() }
    val slide = remember { TabSlide(selectedIndex) }
    val barWidth = LocalTabBarWidth.current
    slide.follow(selectedIndex, clock.nowMs, marks, tabs.size, reduce, steps, barWidth)
    val scroll = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    // A newly selected tab out of view comes into it at once; a focused one is brought in by the scroll itself.
    LaunchedEffect(selectedIndex, marks.x[selectedIndex], marks.width[selectedIndex]) {
        val x = marks.x[selectedIndex] ?: return@LaunchedEffect
        val w = (marks.width[selectedIndex] ?: return@LaunchedEffect) + 2 * SIDE.value
        val left = (x * density).toInt()
        val right = ((x + w) * density).toInt()
        val view = scroll.viewportSize
        when {
            left < scroll.value -> scroll.scrollTo(left)
            right > scroll.value + view && view > 0 -> scroll.scrollTo(right - view)
        }
    }
    Layout(
        content = {
            tabs.forEachIndexed { i, tab -> TabItem(tab, i == selectedIndex, { onSelect(i) }, marks, i) }
        },
        modifier =
            modifier
                .layout { m, c ->
                    // No width at all (a scrolling row): the tabs' own, which a scroll cannot take as its limit.
                    val bounded =
                        if (c.hasBoundedWidth) {
                            c
                        } else {
                            c.copy(
                                maxWidth = m.maxIntrinsicWidth(c.maxHeight).coerceAtLeast(c.minWidth),
                            )
                        }
                    val p = m.measure(bounded)
                    layout(p.width, p.height) { p.place(0, 0) }
                }.horizontalScroll(scroll)
                .selectableGroup()
                .drawWithContent {
                    drawContent()
                    val p = slide.progress(clock.nowMs, reduce, steps)
                    // While the bar runs it reads every frame; at rest it reads nothing.
                    if (p < 1) clock.frame
                    scale(
                        density,
                        Offset.Zero,
                    ) {
                        drawIndicator(
                            marks,
                            slide,
                            selectedIndex,
                            tabs.size,
                            p,
                            colors,
                            barWidth,
                            size.height / density,
                        )
                    }
                },
    ) { measurables, constraints ->
        // Each tab its own width: inside the scroll the row has no limit, and one too wide for the window scrolls.
        val h = TAB_HEIGHT.roundToPx()
        var x = 0
        val placeables =
            measurables.map { m ->
                m.measure(Constraints(minHeight = h)).also { x += it.width }
            }
        val width = max(x, constraints.minWidth)
        val rowH = placeables.maxOfOrNull { it.height } ?: h
        layout(width, rowH) {
            var at = 0
            // Mirrored in a right-to-left layout, the first tab at the right; the indicator reads where each stands (B-65).
            val rtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
            placeables.forEachIndexed { i, p ->
                p.placeRelative(at, 0)
                marks.x[i] = (if (rtl) width - at - p.width else at) / density
                at += p.width
            }
        }
    }
}

/** One tab: its 48 dp target, the label 1 dp down in a 36 dp row, 12 dp in from each side. */
@Composable
private fun TabItem(
    tab: MutantTab,
    selected: Boolean,
    onClick: () -> Unit,
    marks: TabMarks,
    index: Int,
) {
    val colors = MutantTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val color =
        when {
            !tab.enabled -> colors.textDisabled
            selected -> colors.textAccent
            else -> colors.textSecondary
        }
    Box(
        Modifier
            .mutantHand(tab.enabled)
            .selectable(selected, source, indication = null, enabled = tab.enabled, role = Role.Tab, onClick = onClick)
            .widthIn(min = TOUCH)
            .heightIn(min = TAB_HEIGHT)
            .drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    val w = size.width / density
                    val h = size.height / density
                    scale(density, Offset.Zero) { drawCornerBrackets(1f, 1f, w - 1, h - 1, colors.focus) }
                }
            }.padding(start = SIDE, end = SIDE, top = TOP, bottom = FOOT),
    ) {
        MutantText(
            tab.label,
            Modifier
                .heightIn(min = LABEL_ROW)
                .wrapContentHeight(Alignment.CenterVertically)
                .layout { measurable, c ->
                    val p = measurable.measure(c)
                    // The bar is as wide as the label it stands under.
                    marks.width[index] = p.width / density
                    layout(p.width, p.height) { p.place(0, 0) }
                },
            style =
                TextStyle(
                    fontFamily = LocalMutantFonts.current?.text,
                    fontWeight = FontWeight.Medium,
                    fontSize = LABEL_SIZE.sp,
                    color = color,
                    platformStyle = LocalMutantTextRaster.current,
                ).cssLine(MutantFaceMetrics.Rubik),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A parity fixture's bar width at rest, dp: the page draws a literal 62 under a label the kit lays out 65 wide, where
 * the kit follows its label. Null in the kit.
 */
internal val LocalTabBarWidth = staticCompositionLocalOf<Float?> { null }

/** Where each tab stands and how wide its label is, dp, written as they are laid out and read by the bar's drawing. */
internal class TabMarks {
    val x = mutableStateMapOf<Int, Float>()
    val width = mutableStateMapOf<Int, Float>()
}

/** Where the bar stands and how wide it is, dp, in the row's own coordinates. */
private class BarPose(
    val x: Float,
    val width: Float,
)

/**
 * Follows the selected index from composition without writing state: a change starts the bar's run then, from where the
 * bar stands at that moment — under the tab it left, or partway along a run still going (B-168).
 */
internal class TabSlide(
    private var at: Int,
) {
    /** Where the run starts; null when no bar stood (nothing was selected), so the bar arrives with no run. */
    var fromX: Float? = null
        private set
    var fromWidth: Float? = null
        private set
    var since: Double = Double.NEGATIVE_INFINITY
        private set

    /** The run's progress at [now], 0 to 1: at once under reduce motion. */
    fun progress(
        now: Double,
        reduce: Boolean,
        steps: Boolean,
    ): Double = if (reduce) 1.0 else stepped(now - since, MutantMotion.SLIDE_MS, steps)

    fun follow(
        to: Int,
        now: Double,
        marks: TabMarks,
        count: Int,
        reduce: Boolean,
        steps: Boolean,
        width: Float?,
    ) {
        if (to == at) return
        // The layout's marks are read without subscribing the composition to them.
        val pose =
            Snapshot.withoutReadObservation { barPose(marks, this, at, count, progress(now, reduce, steps), width) }
        fromX = pose?.x
        fromWidth = pose?.width
        at = to
        since = now
    }
}

/** The bar under [selected], [p] of the way along its run from where it set off; null when it shows none. */
private fun barPose(
    marks: TabMarks,
    slide: TabSlide,
    selected: Int,
    count: Int,
    p: Double,
    width: Float?,
): BarPose? {
    if (selected !in 0 until count) return null
    val toX = (marks.x[selected] ?: return null) + SIDE.value
    val toW = width ?: marks.width[selected] ?: return null
    val fromX = slide.fromX
    val fromW = slide.fromWidth
    if (p >= 1 || fromX == null || fromW == null) return BarPose(toX, toW)
    val e = (1 - (1 - p).pow(3)).toFloat()
    return BarPose(fromX + (toX - fromX) * e, fromW + (toW - fromW) * e)
}

/** The bar under [selected], run from where it set off by [p]; with its ring and its echo. */
private fun DrawScope.drawIndicator(
    marks: TabMarks,
    slide: TabSlide,
    selected: Int,
    count: Int,
    p: Double,
    colors: MutantColors,
    width: Float?,
    rowHeight: Float,
) {
    val pose = barPose(marks, slide, selected, count, p, width) ?: return
    val toX = (marks.x[selected] ?: return) + SIDE.value
    val fromX = slide.fromX
    val run = p < 1 && fromX != null && slide.fromWidth != null
    val e = if (run) (1 - (1 - p).pow(3)).toFloat() else 1f
    val x = pose.x
    val w = pose.width
    // 5 dp over the row's foot: at 43 in the page's 48, and under a label grown with the font.
    val y = rowHeight - (TAB_HEIGHT.value - BAR_Y)
    // The ring, the page's `box-shadow: 0 0 0 1.5px`, then the bar.
    drawRect(RING, Offset(x - RING_WIDTH, y - RING_WIDTH), Size(w + 2 * RING_WIDTH, BAR_HEIGHT + 2 * RING_WIDTH))
    drawRect(colors.primary, Offset(x, y), Size(w, BAR_HEIGHT))
    // The echo trails behind the run: to the left on a run to the right, crossing back to its place at rest as the
    // bar lands; to the right otherwise.
    val rightward = run && toX > fromX
    val left = if (rightward) (1 - e) else 0f
    val right = if (rightward) e else 1f
    for (i in ECHO_GAP.indices) {
        val ew = ECHO_WIDTH[i]
        val ey = y + 1
        if (right >
            0
        ) {
            drawRect(colors.primary, Offset(x + w + ECHO_GAP[i], ey), Size(ew, 2f), alpha = ECHO_ALPHA[i] * right)
        }
        if (left >
            0
        ) {
            drawRect(colors.primary, Offset(x - ECHO_GAP[i] - ew, ey), Size(ew, 2f), alpha = ECHO_ALPHA[i] * left)
        }
    }
}

/** A tab's target and the row it stands in, the label's row inside it, and the label's 1 dp from the top. */
private val TAB_HEIGHT = 48.dp
private val TOUCH = 48.dp
private val LABEL_ROW = 36.dp
private val TOP = 1.dp

/** Under the label's row: the bar's 6 dp gap, its 4 dp and 1 below, so the bar keeps under a label grown with the font. */
private val FOOT = TAB_HEIGHT - TOP - LABEL_ROW

/** Half the page's `gap: 24px` on each side of a label: the target reaches it. */
private val SIDE = 12.dp

/** The label's size, sp (the page's `font-size: 14px; font-weight: 500`). */
private const val LABEL_SIZE = 14

/** The bar: 4 dp tall, 6 dp under the 36 dp row (the page's `gap: 6px`), ringed 1.5 dp. */
private const val BAR_HEIGHT = 4f
private const val BAR_Y = 1f + 36f + 6f
private const val RING_WIDTH = 1.5f

/**
 * The ring's black: the page's literal `#05060c` in both themes (the «Эфир» ink), not «Копия»'s own; the bar's acid
 * is both themes' primary.
 */
private val RING = Color(0xFF05060C)

/** The echo: three strokes 2 dp tall, 1 dp down, starting 4, 18 and 30 dp past the bar, 10, 8 and 6 wide. */
private val ECHO_GAP = floatArrayOf(4f, 18f, 30f)
private val ECHO_WIDTH = floatArrayOf(10f, 8f, 6f)
private val ECHO_ALPHA = floatArrayOf(.6f, .3f, .15f)
