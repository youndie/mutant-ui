package io.github.youndie.mutant.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.Still
import io.github.youndie.mutant.surface.LocalMutantBuildOffFrame
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
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

    @Test
    fun `given no width, the table is its content's width and every name shows whole`() {
        for (scroll in listOf(true, false)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    Still {
                        val table = @Composable { MutantDataTable(rows, columns, Modifier, setOf(1), { _, _ -> }) }
                        // A horizontal scroll, and a box letting its content be as wide as it likes.
                        if (scroll) {
                            Row(Modifier.horizontalScroll(rememberScrollState())) { table() }
                        } else {
                            Box(Modifier.wrapContentWidth(unbounded = true)) { table() }
                        }
                    }
                }
                mainClock.advanceTimeByFrame()
                for (name in listOf("ЗАПИСЬ") + rows.map { it.first }) {
                    val node = onNode(hasText(name), useUnmergedTree = true).fetchSemanticsNode()
                    val laid = mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(laid)
                    assertTrue(node.boundsInRoot.width > 0f, "$name is ${node.boundsInRoot.width} wide")
                    assertTrue(!laid.single().hasVisualOverflow, "$name is cut at ${node.boundsInRoot.width}")
                }
                // The box column, the widest name and the length column, no more.
                val names = rows.map { onNode(hasText(it.first), useUnmergedTree = true).fetchSemanticsNode() }
                val widest = names.maxOf { it.boundsInRoot.width }
                val row = onAllNodes(checkbox).fetchSemanticsNodes().first().boundsInRoot
                assertEquals(40f + widest + 90f, row.width)
            }
        }
    }

    @Test
    fun `a keyed row moved by the app takes its selection with it, the box whole in its first frame`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val three = rows + ("Настроечная таблица" to "02:10")
            var shown by mutableStateOf(three)
            var selected by mutableStateOf(setOf(1))
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalMutantBuildOffFrame provides false,
                ) {
                    MutantTheme(reduceMotion = false) {
                        Box(Modifier.testTag("t")) {
                            MutantDataTable(
                                shown,
                                columns,
                                Modifier.width(480.dp),
                                selected,
                                { _, _ -> },
                                rowKey = { it.first },
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val acid = MutantPalettes.Efir.primary.toArgb()

            // The acid pixels of row [r]'s box: the 40 dp column under the 36 dp header and its divider, 50 dp a row.
            fun acidIn(r: Int): Int {
                val shot = onNodeWithTag("t").captureToImage().toAwtImage()
                var n = 0
                for (y in 38 + 50 * r until 38 + 50 * r + 48) {
                    for (x in 0 until 40) {
                        val p = shot.getRGB(x, y)
                        val d =
                            abs((p shr 16 and 255) - (acid shr 16 and 255)) +
                                abs((p shr 8 and 255) - (acid shr 8 and 255)) +
                                abs((p and 255) - (acid and 255))
                        if (d < 60) n++
                    }
                }
                return n
            }
            val whole = acidIn(1)
            assertTrue(whole > 200, "a chosen box at rest shows its acid: $whole")
            assertEquals(0, acidIn(3), "an empty box at rest")
            // The app sorts: the chosen record moves from the second row to the last, its selection with it.
            shown = listOf(three[0], three[2], three[3], three[1])
            selected = setOf(3)
            mainClock.advanceTimeByFrame()
            val left = acidIn(1)
            val reached = acidIn(3)
            assertTrue(left < whole / 10, "the place it left shows no acid in the first frame: $left of $whole")
            assertTrue(
                reached > whole * 9 / 10,
                "the place it reached shows it whole in the first frame: $reached of $whole",
            )
        }

    @Test
    fun `a keyed row moved by the app keeps its box's slime`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val three = rows + ("Настроечная таблица" to "02:10")
            var shown by mutableStateOf(three)
            var selected by mutableStateOf(setOf(1))
            setContent {
                // The clock stands, so a body is the same picture wherever it is drawn.
                CompositionLocalProvider(LocalMutantBuildOffFrame provides false) {
                    Still {
                        Box(Modifier.testTag("t")) {
                            MutantDataTable(
                                shown,
                                columns,
                                Modifier.width(480.dp),
                                selected,
                                { _, _ -> },
                                rowKey = { it.first },
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            // Row [r]'s box column, 40 × 48 under the header and its divider, 50 dp a row.
            fun box(r: Int): IntArray {
                val shot = onNodeWithTag("t").captureToImage().toAwtImage()
                return shot.getRGB(0, 38 + 50 * r, 40, 48, null, 0, 40)
            }
            val before = box(1)
            shown = listOf(three[0], three[2], three[3], three[1])
            selected = setOf(3)
            mainClock.advanceTimeByFrame()
            val after = box(3)
            val unlike = before.indices.count { before[it] != after[it] }
            assertEquals(0, unlike, "the moved record's box differs on $unlike of ${before.size} pixels")
        }
}
