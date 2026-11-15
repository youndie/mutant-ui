package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.Still
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-46: what the data table does — the row is the toggle, a 48 dp target, from the mouse and the keyboard. */
@OptIn(ExperimentalTestApi::class)
class DataTableBehaviourTest {
    private val rows = listOf("Без подписи №3" to "46:00", "Эфир, ночь на вторник" to "12:04", "Нет сигнала" to "00:42")
    private val columns =
        listOf(
            MutantTableColumn<Pair<String, String>>("ЗАПИСЬ") { it.first },
            MutantTableColumn("ДЛИНА", 90.dp, MutantTableCellStyle.Osd) { it.second },
        )
    private val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    @Test
    fun `a press anywhere on a row toggles it, and each row is a 48 dp target`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableStateOf(setOf(1))
            setContent {
                Still {
                    MutantDataTable(rows, columns, Modifier.width(480.dp), selected, { i, on ->
                        selected = if (on) selected + i else selected - i
                    })
                }
            }
            mainClock.advanceTimeByFrame()
            val targets = onAllNodes(checkbox).fetchSemanticsNodes()
            assertEquals(3, targets.size)
            assertTrue(
                targets.all {
                    it.boundsInRoot.height >= 48f && it.boundsInRoot.width >= 48f
                },
                "targets ${targets.map { it.boundsInRoot }}",
            )
            assertEquals(
                listOf(ToggleableState.Off, ToggleableState.On, ToggleableState.Off),
                targets.map { it.config[SemanticsProperties.ToggleableState] },
            )
            // On the words, far from the box.
            onAllNodes(checkbox)[2].performClick()
            mainClock.advanceTimeByFrame()
            onAllNodes(checkbox)[1].performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(setOf(2), selected)
        }

    @Test
    fun `Tab reaches each row and Space toggles it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableStateOf(emptySet<Int>())
            setContent {
                Still {
                    MutantDataTable(rows, columns, Modifier.width(480.dp), selected, { i, on ->
                        selected = if (on) selected + i else selected - i
                    })
                }
            }
            mainClock.advanceTimeByFrame()
            onRoot().performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
            onAllNodes(checkbox)[0].assertIsFocused()
            onAllNodes(checkbox)[0].performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
            onAllNodes(checkbox)[1].assertIsFocused()
            onAllNodes(checkbox)[1].performKeyInput { pressKey(Key.Spacebar) }
            mainClock.advanceTimeByFrame()
            assertEquals(setOf(1), selected)
        }

    @Test
    fun `without a selection handler there is no checkbox column and nothing to toggle`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { Still { MutantDataTable(rows, columns, Modifier.width(480.dp)) } }
            mainClock.advanceTimeByFrame()
            assertEquals(0, onAllNodes(checkbox).fetchSemanticsNodes().size)
            val names =
                onNode(
                    androidx.compose.ui.test
                        .hasText("ЗАПИСЬ"),
                ).fetchSemanticsNode().boundsInRoot
            assertEquals(0f, names.left)
        }

    @Test
    fun `a row and the header stand at the page's heights`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { Still { MutantDataTable(rows, columns, Modifier.width(480.dp), emptySet(), { _, _ -> }) } }
            mainClock.advanceTimeByFrame()
            val rowsAt = onAllNodes(checkbox).fetchSemanticsNodes().map { it.boundsInRoot }
            // Header 36, divider 2, then rows of 48 each with its divider.
            assertEquals(listOf(38f, 88f, 138f), rowsAt.map { it.top })
            assertTrue(rowsAt.all { it.height == 48f })
        }
}
