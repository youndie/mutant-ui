package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.Still
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-45: what the calendar and the time picker do, and the 36 dp day cells of research D19. */
@OptIn(ExperimentalTestApi::class)
class DateTimePickerBehaviourTest {
    @Test
    fun `the calendar's days and weeks fall where November 2026 and February 2024 have them`() {
        assertEquals(6, firstWeekday(2026, 11))
        assertEquals(30, daysIn(2026, 11))
        assertEquals(29, daysIn(2024, 2))
        assertEquals(28, daysIn(2100, 2))
        assertEquals(29, daysIn(2000, 2))
        assertEquals(3, firstWeekday(2024, 2))
        assertEquals(0, firstWeekday(2024, 1))
    }

    @Test
    fun `a press on a day picks it, the arrows turn the month and the year`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var day by mutableIntStateOf(13)
            var month by mutableIntStateOf(12)
            var year by mutableIntStateOf(2026)
            setContent {
                Still {
                    Box(Modifier.size(400.dp, 400.dp)) {
                        MutantDatePicker(year, month, day, { day = it }, { y, m ->
                            year = y
                            month = m
                        })
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNode(hasText("21")).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(21, day)
            onNode(hasContentDescription("Следующий месяц")).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(2027 to 1, year to month)
            onNode(hasText("ЯНВАРЬ 2027")).assertExists()
            onNode(hasContentDescription("Предыдущий месяц")).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(2026 to 12, year to month)
        }

    @Test
    fun `D19 each day is its whole 36 dp cell, the cells tile the grid, the arrows are 48 dp targets`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Box(
                        Modifier.size(400.dp, 400.dp),
                    ) { MutantDatePicker(2026, 11, 13, {}, { _, _ -> }) }
                }
            }
            mainClock.advanceTimeByFrame()
            val days =
                (1..30).map { d -> onNode(hasText("$d").and(hasClickAction())).fetchSemanticsNode().boundsInRoot }
            assertTrue(days.all { it.width == 36f && it.height == 36f }, "cells ${days.distinct().take(3)}")
            // November 2026 starts on a Sunday: the 1st in the last column, the 2nd a row down at the first.
            assertEquals(days[0].left - 6 * 36f, days[1].left)
            assertEquals(days[0].top + 36f, days[1].top)
            assertEquals(days[1].right, days[2].left)
            val arrows =
                listOf("Предыдущий месяц", "Следующий месяц").map {
                    onNode(hasContentDescription(it)).fetchSemanticsNode().boundsInRoot
                }
            assertTrue(arrows.all { it.width >= 48f && it.height >= 48f }, "arrows $arrows")
        }

    @Test
    fun `given more room than it needs, the calendar keeps its width and its header over its days`() =
        // B-150: the header's row filled the width it was offered, so the calendar stretched over a wide column and its
        // month and arrows ran past the days (the Components page, 2026-11-20).
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Box(Modifier.width(600.dp)) {
                        MutantDatePicker(2026, 11, 13, {}, { _, _ -> }, Modifier.testTag("calendar"))
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val calendar = onNodeWithTag("calendar").fetchSemanticsNode().boundsInRoot
            assertEquals(284f, calendar.width, "the calendar's width")
            // November 2026's Sundays, the 29th among them, are its last column.
            val days = onNode(hasText("29").and(hasClickAction())).fetchSemanticsNode().boundsInRoot
            val next = onNode(hasContentDescription("Следующий месяц")).fetchSemanticsNode().boundsInRoot
            assertTrue(next.left < days.right, "the arrow from ${next.left}, past the days' end at ${days.right}")
        }

    @Test
    fun `the arrow keys move the focus a day and a week`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Box(
                        Modifier.size(400.dp, 400.dp),
                    ) { MutantDatePicker(2026, 11, 13, {}, { _, _ -> }) }
                }
            }
            mainClock.advanceTimeByFrame()
            onNode(hasText("10").and(hasClickAction())).requestFocus()
            onNode(hasText("10").and(hasClickAction())).performKeyInput { pressKey(Key.DirectionRight) }
            mainClock.advanceTimeByFrame()
            onNode(hasText("11").and(hasClickAction())).assertIsFocused()
            onNode(hasText("11").and(hasClickAction())).performKeyInput { pressKey(Key.DirectionDown) }
            mainClock.advanceTimeByFrame()
            onNode(hasText("18").and(hasClickAction())).assertIsFocused()
        }

    @Test
    fun `the time picker steps the focused field, wraps, and can be set by a reader`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var hour by mutableIntStateOf(23)
            var minute by mutableIntStateOf(30)
            setContent {
                Still {
                    Column {
                        MutantTimePicker(hour, minute, { h, m ->
                            hour = h
                            minute = m
                        })
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val hours = onNode(hasContentDescription("Часы"))
            hours.requestFocus()
            hours.performKeyInput { pressKey(Key.DirectionUp) }
            mainClock.advanceTimeByFrame()
            assertEquals(0 to 30, hour to minute)
            val minutes = onNode(hasContentDescription("Минуты"))
            minutes.performClick()
            minutes.performSemanticsAction(SemanticsActions.SetProgress) { it(45f) }
            mainClock.advanceTimeByFrame()
            assertEquals(0 to 45, hour to minute)
            assertEquals(
                true,
                minutes.fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.Selected) { null },
            )
        }
}
