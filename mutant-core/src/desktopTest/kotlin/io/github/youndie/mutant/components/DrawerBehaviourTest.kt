package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.Still
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-34: what the drawer does — pick, dismiss, the keys — and the 42 dp rows of research D19. */
@OptIn(ExperimentalTestApi::class)
class DrawerBehaviourTest {
    private val items =
        listOf(
            MutantDrawerItem("CH 01", "Эфир"),
            MutantDrawerItem("CH 02", "Очередь"),
            MutantDrawerItem("CH 03", "Поиск"),
            MutantDrawerItem("CH 04", "Настройка"),
        )
    private val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test
    fun `a pick goes to onSelect, and the scrim and Escape dismiss`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableIntStateOf(0)
            var open by mutableStateOf(true)
            setContent {
                Still {
                    Box(Modifier.size(360.dp, 400.dp)) {
                        MutantNavigationDrawer(open, { open = false }, items, selected, { selected = it })
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Поиск")).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(2, selected)
            // The scrim, beyond the drawer.
            onRoot().performTouchInput { click(Offset(330f, 200f)) }
            mainClock.advanceTimeBy(300)
            assertEquals(false, open)
            open = true
            mainClock.advanceTimeBy(400)
            onNode(hasText("Поиск")).requestFocus()
            onNode(hasText("Поиск")).performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeBy(300)
            assertEquals(false, open)
        }

    @Test
    fun `Down moves the focus to the next row, wrapping`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still { Box(Modifier.size(360.dp, 400.dp)) { MutantNavigationDrawer(true, {}, items, 0, {}) } }
            }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Настройка")).requestFocus()
            mainClock.advanceTimeByFrame()
            onNode(hasText("Настройка")).performKeyInput { pressKey(Key.DirectionDown) }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Эфир")).assertIsFocused()
        }

    private fun ComposeUiTest.tab(shift: Boolean = false) {
        onRoot().performKeyInput { if (shift) withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } else pressKey(Key.Tab) }
        mainClock.advanceTimeByFrame()
    }

    private fun ComposeUiTest.open(onDismiss: () -> Unit = {}) {
        mainClock.autoAdvance = false
        setContent {
            Still { Box(Modifier.size(360.dp, 400.dp)) { MutantNavigationDrawer(true, onDismiss, items, 1, {}) } }
        }
        mainClock.advanceTimeByFrame()
    }

    @Test
    fun `B-207 Tab steps the rows and comes round from the last to the first`() =
        runComposeUiTest {
            open()
            val seen =
                List(6) {
                    tab()
                    items.indexOfFirst { item -> onAllNodes(hasText(item.label) and isFocused()).fetchSemanticsNodes().isNotEmpty() }
                }
            assertEquals(listOf(0, 1, 2, 3, 0, 1), seen, "the focused row after each Tab")
        }

    @Test
    fun `B-207 Shift+Tab from the first row reaches the last, and Escape closes the drawer`() =
        runComposeUiTest {
            var dismissed = 0
            open { dismissed++ }
            // From the drawer itself, as it opens, Shift+Tab goes to the last row.
            tab(shift = true)
            onNode(hasText("Настройка")).assertIsFocused()
            onNode(hasText("Эфир")).requestFocus()
            mainClock.advanceTimeByFrame()
            tab(shift = true)
            onNode(hasText("Настройка")).assertIsFocused()
            tab(shift = true)
            onNode(hasText("Поиск")).assertIsFocused()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            assertEquals(1, dismissed, "Escape calls the drawer's close callback")
        }

    @Test
    fun `D19 each row is its whole target, 42 dp tall and 180 wide, 4 dp apart`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still { Box(Modifier.size(360.dp, 400.dp)) { MutantNavigationDrawer(true, {}, items, 0, {}) } }
            }
            mainClock.advanceTimeByFrame()
            val rows = onAllNodes(tab).fetchSemanticsNodes().map { it.boundsInRoot }.sortedBy { it.top }
            assertEquals(4, rows.size)
            assertTrue(rows.all { it.height == 42f && it.width == 180f }, "rows $rows")
            assertEquals(listOf(46f, 46f, 46f), rows.zipWithNext { a, b -> b.top - a.top })
        }
}
