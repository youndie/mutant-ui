package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B-196 (B-168's answers 21, 32 and 40; F-pickers-06, -07, -08): today wears four corner brackets in the amber; a day the
 * caller refuses is grey, has no drop, takes no press and no key, and reads as disabled; the calendar lays out six weeks
 * whatever the month; and the time picker's acid follows the field the arrow keys set.
 *
 * Pixels are counted over areas, with margins far from the thresholds (a different rasteriser moves a few edge pixels):
 * a colour counts within .12 per channel of a token.
 */
@OptIn(ExperimentalTestApi::class)
class PickersTodayDisabledSixRowsTest {
    private val palettes = listOf("Efir" to MutantPalettes.Efir, "Kopiya" to MutantPalettes.Kopiya)

    @Test
    fun `today wears four brackets in the amber around its number, and no other day does`() {
        for (d in listOf(1f, 2f)) {
            for ((name, palette) in palettes) {
                for (today in listOf(MutantDate(2026, 11, 23), MutantDate(2026, 12, 23))) {
                    runComposeUiTest {
                        mainClock.autoAdvance = false
                        setContent {
                            At(d, palette) {
                                MutantDatePicker(2026, 11, 13, {}, { _, _ -> }, Modifier.testTag("cal"), today = today)
                            }
                        }
                        mainClock.advanceTimeBy(500)
                        val img = onNodeWithTag("stage").captureToImage().toPixelMap()
                        val case = "$name, density $d, today $today"
                        val corners = corners(img, cell(23), d, palette.textOsd)
                        if (today.month == 11) {
                            // A corner's two strokes in the 10 dp square at the cell's corner: 2 × 6 dp × 2 dp, ~24 dp².
                            assertTrue(
                                corners.all { it >= 12 * d * d },
                                "$case: the amber in the 23rd's corners $corners",
                            )
                        } else {
                            assertTrue(corners.all { it == 0 }, "$case: today is in another month, yet $corners")
                        }
                        // Not in the focus colour where the palette tells the two apart, and on no other day.
                        if (palette.focus != palette.textOsd) {
                            val focus = corners(img, cell(23), d, palette.focus)
                            assertTrue(focus.all { it == 0 }, "$case: the focus colour in the corners $focus")
                        }
                        val other = corners(img, cell(24), d, palette.textOsd)
                        assertTrue(other.all { it == 0 }, "$case: the 24th's corners $other")
                    }
                }
            }
        }
    }

    @Test
    fun `a day that cannot be picked is grey, has no drop, takes no press and no key, and reads disabled`() {
        for (d in listOf(1f, 2f)) {
            for ((name, palette) in palettes) {
                for (refused in listOf(true, false)) {
                    runComposeUiTest {
                        mainClock.autoAdvance = false
                        var picked = 0
                        setContent {
                            At(d, palette) {
                                MutantDatePicker(
                                    2026,
                                    11,
                                    13,
                                    { picked = it },
                                    { _, _ -> },
                                    Modifier.testTag("cal"),
                                    selectableDates = { _, _, day -> !refused || day != 13 },
                                )
                            }
                        }
                        mainClock.advanceTimeBy(500)
                        val case = "$name, density $d, the 13th ${if (refused) "refused" else "open"}"
                        val img = onNodeWithTag("stage").captureToImage().toPixelMap()
                        val acid = count(img, cell(13), palette.primary)
                        val cellArea = (36 * 36 * d * d).toInt()
                        val ink = count(img, cell(13), palette.text)
                        val grey = count(img, cell(13), palette.textDisabled)
                        // The 14th, open, keeps its number in the text colour either way: the control for the ink count
                        // (~40 px at density 1, ~200 at 2; a refused 13th shows none of it and ~45 px of grey).
                        val open = count(img, cell(14), palette.text)
                        assertTrue(open >= 6 * d * d, "$case: the 14th's number, $open px")
                        if (refused) {
                            assertTrue(acid < cellArea / 50, "$case: the drop's acid in the cell, $acid px")
                            assertTrue(grey >= 6 * d * d, "$case: the grey number, $grey px")
                            assertTrue(ink < open / 4, "$case: the number in the text colour, $ink px against $open")
                        } else {
                            assertTrue(acid > cellArea / 3, "$case: the drop's acid in the cell, $acid px")
                        }

                        val node = onNode(hasText("13").and(hasClickAction()))
                        if (refused) node.assertIsNotEnabled() else node.assertIsEnabled()
                        node.performClick()
                        mainClock.advanceTimeByFrame()
                        assertEquals(if (refused) 0 else 13, picked, "$case: a press on the 13th")
                        picked = 0
                        // It takes no focus, so no key reaches it: Right from the 12th steps over it to the 14th, and
                        // Enter there picks the 14th.
                        val focusable = node.fetchSemanticsNode().config.contains(SemanticsActions.RequestFocus)
                        assertEquals(!refused, focusable, "$case: the 13th can take focus")
                        onNode(hasText("12").and(hasClickAction())).requestFocus()
                        onNode(hasText("12").and(hasClickAction())).performKeyInput { pressKey(Key.DirectionRight) }
                        mainClock.advanceTimeByFrame()
                        val landed = if (refused) "14" else "13"
                        onNode(hasText(landed).and(hasClickAction())).assertIsFocused()
                        onNode(hasText(landed).and(hasClickAction())).performKeyInput { pressKey(Key.Enter) }
                        mainClock.advanceTimeByFrame()
                        assertEquals(landed.toInt(), picked, "$case: Enter after Right from the 12th")
                    }
                }
            }
        }
    }

    @Test
    fun `every month of 2026 lays the calendar out at the same height`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var month by mutableIntStateOf(1)
            setContent {
                At(1f, MutantPalettes.Efir) {
                    MutantDatePicker(2026, month, 13, {}, { _, _ -> }, Modifier.testTag("cal"))
                }
            }
            val heights =
                (1..12).associateWith { m ->
                    month = m
                    mainClock.advanceTimeBy(500)
                    onNodeWithTag("cal").fetchSemanticsNode().boundsInRoot.height
                }
            // Six weeks of 36 dp under the header: November 2026, which reaches six, is the design's 307 dp.
            assertTrue(heights.values.all { it == 307f }, "the calendar's height by month: $heights")
        }

    @Test
    fun `editing the minutes by Up and Down moves the acid to the minutes, and the hours take it back`() {
        for ((name, palette) in palettes) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                var hour by mutableIntStateOf(21)
                var minute by mutableIntStateOf(30)
                setContent {
                    At(1f, palette) {
                        MutantTimePicker(hour, minute, { h, m ->
                            hour = h
                            minute = m
                        })
                    }
                }
                mainClock.advanceTimeBy(500)
                val hours = onNode(hasContentDescription("Часы"))
                val minutes = onNode(hasContentDescription("Минуты"))
                assertEquals(true to false, selected(hours) to selected(minutes), "$name: at rest the hours are set")

                minutes.requestFocus()
                minutes.performKeyInput { pressKey(Key.DirectionUp) }
                mainClock.advanceTimeBy(1_000)
                assertEquals(21 to 31, hour to minute, "$name: Up on the minutes")
                assertEquals(false to true, selected(hours) to selected(minutes), "$name: after Up on the minutes")
                val img = onNodeWithTag("stage").captureToImage().toPixelMap()
                val area = { n: Rect -> (n.width * n.height).toInt() }
                val hBox = hours.fetchSemanticsNode().boundsInRoot
                val mBox = minutes.fetchSemanticsNode().boundsInRoot
                val onMinutes = count(img, mBox, palette.primary)
                val onHours = count(img, hBox, palette.primary)
                assertTrue(onMinutes > area(mBox) / 3, "$name: the minutes' acid, $onMinutes of ${area(mBox)} px")
                assertTrue(onHours < area(hBox) / 20, "$name: the hours' acid, $onHours of ${area(hBox)} px")

                hours.requestFocus()
                hours.performKeyInput { pressKey(Key.DirectionDown) }
                mainClock.advanceTimeBy(1_000)
                assertEquals(20 to 31, hour to minute, "$name: Down on the hours")
                assertEquals(true to false, selected(hours) to selected(minutes), "$name: after Down on the hours")
            }
        }
    }

    private fun selected(n: androidx.compose.ui.test.SemanticsNodeInteraction) =
        n.fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.Selected) { null }

    @Composable
    private fun At(
        d: Float,
        palette: MutantColors,
        content: @Composable () -> Unit,
    ) = CompositionLocalProvider(LocalDensity provides Density(d)) {
        MutantTheme(palette, reduceMotion = true) {
            Box(Modifier.testTag("stage").padding(24.dp)) { content() }
        }
    }

    /** A day's cell in the stage's pixels (the stage stands at the root's origin). */
    private fun ComposeUiTest.cell(day: Int): Rect =
        onNode(hasText("$day").and(hasClickAction())).fetchSemanticsNode().boundsInRoot

    /** Pixels of [c] in each of the four 10 dp squares at the cell's corners, from 1 dp inside it. */
    private fun corners(
        img: PixelMap,
        cell: Rect,
        d: Float,
        c: Color,
    ): List<Int> {
        val q = 10 * d
        val i = 1 * d
        return listOf(
            Rect(cell.left + i, cell.top + i, cell.left + i + q, cell.top + i + q),
            Rect(cell.right - i - q, cell.top + i, cell.right - i, cell.top + i + q),
            Rect(cell.left + i, cell.bottom - i - q, cell.left + i + q, cell.bottom - i),
            Rect(cell.right - i - q, cell.bottom - i - q, cell.right - i, cell.bottom - i),
        ).map { count(img, it, c) }
    }

    private fun count(
        img: PixelMap,
        r: Rect,
        c: Color,
    ): Int {
        var n = 0
        for (y in r.top.toInt() until r.bottom.toInt().coerceAtMost(img.height)) {
            for (x in r.left.toInt() until r.right.toInt().coerceAtMost(img.width)) {
                val p = img[x, y]
                if (abs(p.red - c.red) < TOL && abs(p.green - c.green) < TOL && abs(p.blue - c.blue) < TOL) n++
            }
        }
        return n
    }

    private companion object {
        const val TOL = .12f
    }
}
