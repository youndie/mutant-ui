package io.github.youndie.mutant.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
 * what it draws; keyboard focus draws the corner brackets around a tab.
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
    val marks = remember { TabMarks() }
    val slide = remember { TabSlide(selectedIndex) }
    val barWidth = LocalTabBarWidth.current
    slide.follow(selectedIndex, clock.nowMs)
    Layout(
        content = {
            tabs.forEachIndexed { i, tab -> TabItem(tab, i == selectedIndex, { onSelect(i) }, marks, i) }
        },
        modifier =
            modifier.selectableGroup().drawWithContent {
                drawContent()
                val p = if (reduce) 1.0 else stepped(clock.nowMs - slide.since, MutantMotion.SLIDE_MS)
                // While the bar runs it reads every frame; at rest it reads nothing.
                if (p < 1) clock.frame
                scale(
                    density,
                    Offset.Zero,
                ) { drawIndicator(marks, slide, selectedIndex, tabs.size, p, colors, barWidth) }
            },
    ) { measurables, constraints ->
        val h = TAB_HEIGHT.roundToPx()
        var x = 0
        val placeables =
            measurables.map { m ->
                val room =
                    if (constraints.hasBoundedWidth) {
                        (constraints.maxWidth - x).coerceAtLeast(
                            0,
                        )
                    } else {
                        Constraints.Infinity
                    }
                m.measure(Constraints(maxWidth = room, minHeight = h, maxHeight = h)).also { x += it.width }
            }
        val width =
            if (constraints.hasBoundedWidth) {
                x.coerceIn(
                    constraints.minWidth,
                    constraints.maxWidth,
                )
            } else {
                max(x, constraints.minWidth)
            }
        layout(width, h) {
            var at = 0
            placeables.forEachIndexed { i, p ->
                p.place(at, 0)
                marks.x[i] = at / density
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
            .selectable(selected, source, indication = null, enabled = tab.enabled, role = Role.Tab, onClick = onClick)
            .widthIn(min = TOUCH)
            .height(TAB_HEIGHT)
            .drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    val w = size.width / density
                    val h = size.height / density
                    scale(density, Offset.Zero) { drawCornerBrackets(1f, 1f, w - 1, h - 1, colors.focus) }
                }
            }.padding(start = SIDE, end = SIDE, top = TOP),
    ) {
        BasicText(
            tab.label,
            Modifier
                .height(LABEL_ROW)
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

/** Follows the selected index from composition without writing state: a change starts the bar's run then. */
internal class TabSlide(
    private var at: Int,
) {
    var from: Int = -1
        private set
    var since: Double = Double.NEGATIVE_INFINITY
        private set

    fun follow(
        to: Int,
        now: Double,
    ) {
        if (to == at) return
        from = at
        at = to
        since = now
    }
}

/** The bar under [selected], run from [TabSlide.from] by [p]; with its ring and its echo. */
private fun DrawScope.drawIndicator(
    marks: TabMarks,
    slide: TabSlide,
    selected: Int,
    count: Int,
    p: Double,
    colors: MutantColors,
    width: Float?,
) {
    if (selected !in 0 until count) return
    val toX = (marks.x[selected] ?: return) + SIDE.value
    val toW = width ?: marks.width[selected] ?: return
    val from = slide.from
    val fromX = marks.x[from]?.plus(SIDE.value)
    val fromW = marks.width[from]
    val run = p < 1 && from in 0 until count && fromX != null && fromW != null
    val e = if (run) (1 - (1 - p).pow(3)).toFloat() else 1f
    val x = if (run) fromX + (toX - fromX) * e else toX
    val w = if (run) fromW + (toW - fromW) * e else toW
    val y = BAR_Y
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
