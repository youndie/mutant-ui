package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.allNodes
import io.github.youndie.mutant.qa.near
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-178 (B-168's F-bottom-sheet-09): given less height or width than it wants, the calendar keeps its size and overflows
 * its slot, centred across and from its top down; no row of it rises into another and every day stays under its own
 * weekday. Before, a shorter slot centred the day grid in what was left, its first week rising into the weekday names
 * (33 dp in a 300 dp screen's sheet), and a narrower one squeezed the weekday names to nothing and moved the days off
 * their columns.
 *
 * November 2026 (six weeks, the 1st a Sunday), «Эфир», reduce motion, at densities 1 and 2. Positions are read with
 * `positionInRoot` in dp, to within half a dp (a pixel at density 2); the drawn check counts the days' light numbers.
 */
@OptIn(ExperimentalTestApi::class)
class CalendarShortOfRoomTest {
    @Test
    fun `given less height or width than it wants, no row of the calendar overlaps another`() {
        // The calendar wants 284 × 307 dp. Heights from more than that down to half of it, then widths down to half.
        val slots =
            listOf(300 to 340, 300 to 300, 300 to 280, 300 to 240, 300 to 200, 300 to 154) +
                listOf(260 to 340, 220 to 340, 180 to 340, 142 to 340, 180 to 154)
        for (d in listOf(1f, 2f)) {
            // Where the month stands in the first slot, which gives the calendar all it wants.
            var monthTop = Float.NaN
            for ((w, h) in slots) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    setContent {
                        At(d) {
                            Box(Modifier.size(STAGE_W.dp, STAGE_H.dp).testTag("stage")) {
                                Box(Modifier.padding(start = SLOT_X.dp).size(w.dp, h.dp)) {
                                    MutantDatePicker(2026, 11, 13, {}, { _, _ -> }, Modifier.testTag("calendar"))
                                }
                            }
                        }
                    }
                    mainClock.advanceTimeBy(500)
                    val case = "density $d, slot $w × $h"
                    val rows = rows(d, case)
                    // Kept at its own width, centred on a narrower slot; from the slot's top, whatever its height.
                    val left = if (w < WANTS_W) (w - WANTS_W) / 2f else 0f
                    within(SLOT_X + left + SIDE, rows.monday.x, "$case: Monday's name from the slot's left")
                    if (monthTop.isNaN()) monthTop = rows.month.top
                    within(monthTop, rows.month.top, "$case: the month from the slot's top, as given all it wants")
                    drawn(rows, d, case)
                }
            }
        }
    }

    @Test
    fun `in a sheet on a short screen, the calendar keeps its size under the words above it`() {
        for (d in listOf(1f, 2f)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    At(d) {
                        Box(Modifier.size(360.dp, 300.dp).testTag("stage")) {
                            MutantBottomSheet(true, {}, windowInsets = WindowInsets(0)) {
                                BasicText("Таймер записи", Modifier.testTag("title"))
                                MutantDatePicker(2026, 11, 13, {}, { _, _ -> }, Modifier.testTag("calendar"))
                            }
                        }
                    }
                }
                mainClock.advanceTimeBy(800)
                val case = "density $d, a 300 dp screen's sheet"
                val title = onNodeWithTag("title", useUnmergedTree = true).fetchSemanticsNode()
                val rows = rows(d, case)
                val titleFoot = (title.positionInRoot.y + title.size.height) / d
                assertTrue(
                    rows.month.top >= titleFoot,
                    "$case: the month at ${rows.month.top}, the title's foot at $titleFoot",
                )
                drawn(rows, d, case)
            }
        }
    }

    @Composable
    private fun At(
        d: Float,
        content: @Composable () -> Unit,
    ) = CompositionLocalProvider(LocalDensity provides Density(d)) {
        MutantTheme(MutantPalettes.Efir, reduceMotion = true, content = content)
    }

    /** A node's box in the root, dp. */
    private class Span(
        val x: Float,
        val top: Float,
        val width: Float,
        val height: Float,
    ) {
        val right get() = x + width
        val foot get() = top + height
    }

    private class Rows(
        val month: Span,
        val names: List<Span>,
        /** The first day of each week: the 1st (a Sunday), the 2nd, 9th, 16th, 23rd and 30th. */
        val weekStarts: List<Span>,
        /** The 2nd to the 8th, Monday to Sunday. */
        val secondWeek: List<Span>,
    ) {
        val monday get() = names.first()
    }

    private fun ComposeUiTest.box(
        n: SemanticsNode,
        d: Float,
    ) = Span(n.positionInRoot.x / d, n.positionInRoot.y / d, n.size.width / d, n.size.height / d)

    private fun ComposeUiTest.word(
        text: String,
        d: Float,
    ): Span =
        box(
            allNodes().first { n ->
                n.children.isEmpty() &&
                    n.config.getOrElse(SemanticsProperties.Text) { emptyList() }.any { it.text == text }
            },
            d,
        )

    private fun ComposeUiTest.day(
        n: Int,
        d: Float,
    ): Span = box(onNode(hasText("$n").and(hasClickAction())).fetchSemanticsNode(), d)

    /** The rows read by their bounds: the month over the weekday names, the names over the days, the weeks apart. */
    private fun ComposeUiTest.rows(
        d: Float,
        case: String,
    ): Rows {
        val rows =
            Rows(
                word("НОЯБРЬ 2026", d),
                WEEKDAYS.map { word(it, d) },
                listOf(1, 2, 9, 16, 23, 30).map { day(it, d) },
                (2..8).map { day(it, d) },
            )
        val names = rows.names
        assertTrue(
            rows.month.foot <= names.first().top,
            "$case: the month's foot ${rows.month.foot}, the names' top ${names.first().top}",
        )
        within(names.first().foot + GAP, rows.weekStarts.first().top, "$case: the first week under the weekday names")
        rows.weekStarts.zipWithNext().forEach { (a, b) ->
            within(a.top + CELL, b.top, "$case: a week under the one before")
        }
        for ((i, name) in names.withIndex()) {
            val day = rows.secondWeek[i]
            within(CELL, name.width, "$case: ${WEEKDAYS[i]}'s width")
            within(name.x, day.x, "$case: day ${i + 2} under ${WEEKDAYS[i]}")
        }
        within(names.last().x, rows.weekStarts.first().x, "$case: the 1st under ВС")
        // Research D19: every day's target is its whole 36 dp cell.
        for (b in rows.weekStarts + rows.secondWeek) {
            within(CELL, b.width, "$case: a day's target width")
            within(CELL, b.height, "$case: a day's target height")
        }
        return rows
    }

    /**
     * What is drawn: no light day number in the weekday names' band, and the second week's numbers only under the names'
     * span. The stage is captured from the root's origin, so a dp position times the density is its pixel.
     */
    private fun ComposeUiTest.drawn(
        rows: Rows,
        d: Float,
        case: String,
    ) {
        val px = onNodeWithTag("stage").captureToImage().toPixelMap()
        val light = MutantPalettes.Efir.text

        fun lit(
            x0: Float,
            x1: Float,
            y0: Float,
            y1: Float,
        ) = count(px, light, (x0 * d).toInt(), (x1 * d).toInt(), (y0 * d).toInt(), (y1 * d).toInt())
        val names = rows.names
        val band = lit(0f, px.width / d, names.first().top, names.first().foot)
        assertEquals(0, band, "$case: light day numbers in the weekday names' row")
        val week = rows.secondWeek.first()
        val inside = lit(names.first().x, names.last().right, week.top, week.foot)
        val all = lit(0f, px.width / d, week.top, week.foot)
        assertTrue(inside > 50 * d * d, "$case: the second week's numbers drawn under the names: $inside px")
        assertEquals(inside, all, "$case: the second week's light pixels outside the names' span")
    }

    private fun count(
        px: PixelMap,
        c: androidx.compose.ui.graphics.Color,
        x0: Int,
        x1: Int,
        y0: Int,
        y1: Int,
    ): Int {
        var n = 0
        for (y in y0.coerceAtLeast(0) until y1.coerceAtMost(px.height)) {
            for (x in x0.coerceAtLeast(0) until x1.coerceAtMost(px.width)) if (near(px[x, y], c)) n++
        }
        return n
    }

    private fun within(
        expected: Float,
        actual: Float,
        what: String,
    ) = assertTrue(abs(expected - actual) <= HALF_DP, "$what: expected $expected, was $actual")

    private companion object {
        val WEEKDAYS = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
        const val STAGE_W = 400
        const val SLOT_X = 80
        const val STAGE_H = 360
        const val WANTS_W = 284
        const val SIDE = 16f
        const val GAP = 8f
        const val CELL = 36f
        const val HALF_DP = .5f
    }
}
