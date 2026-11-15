package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.Still
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-33: what the menu does — open, pick, close — and the 36 dp rows of research D19. */
@OptIn(ExperimentalTestApi::class)
class MenuBehaviourTest {
    private val items = listOf("ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ", "СБРОС")
    private val row =
        SemanticsMatcher
            .expectValue(
                SemanticsProperties.Role,
                Role.DropdownList,
            ).and(hasText("СБРОС").or(hasText("ТРЕКИНГ")).or(hasText("ЦВЕТНОСТЬ")).or(hasText("ЧЁТКОСТЬ")))

    @Test
    fun `the anchor opens the menu, a pick goes to onSelect and the menu closes`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableIntStateOf(0)
            setContent {
                Still {
                    Box(Modifier.size(400.dp, 400.dp)) {
                        MutantDropdown("Настройка", items, selected, { selected = it })
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            assertEquals(0, onAllNodes(hasText("СБРОС")).fetchSemanticsNodes().size)
            onNode(hasText("Настройка")).performClick()
            mainClock.advanceTimeByFrame()
            onNode(hasText("ЧЁТКОСТЬ")).performClick()
            // Under reduce motion (Still) the window is there at once and the menu closes at once.
            mainClock.advanceTimeBy(100)
            assertEquals(2, selected)
            assertEquals(0, onAllNodes(hasText("СБРОС")).fetchSemanticsNodes().size)
        }

    @Test
    fun `Escape closes the menu, Down moves the focus to the next row`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still { Box(Modifier.size(400.dp, 400.dp)) { MutantDropdown("Настройка", items, 0, {}) } }
            }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Настройка")).performClick()
            mainClock.advanceTimeByFrame()
            onNode(hasText("ТРЕКИНГ")).requestFocus()
            mainClock.advanceTimeByFrame()
            onNode(hasText("ТРЕКИНГ")).assertIsFocused()
            onNode(hasText("ТРЕКИНГ")).performKeyInput { pressKey(Key.DirectionDown) }
            mainClock.advanceTimeByFrame()
            onNode(hasText("ЦВЕТНОСТЬ")).assertIsFocused()
            onNode(hasText("ЦВЕТНОСТЬ")).performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            assertEquals(0, onAllNodes(hasText("СБРОС")).fetchSemanticsNodes().size)
        }

    /** Acid pixels in row [row]'s band of the menu body. */
    private fun androidx.compose.ui.test.ComposeUiTest.acidInRow(row: Int): Int {
        val m = onNode(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "menu")).captureToImage().toPixelMap()
        val acid = io.github.youndie.mutant.tokens.MutantPalettes.Efir.primary
        var n = 0
        for (y in 6 + row * 36 until 6 + (row + 1) * 36) {
            for (x in 6 until 210) {
                val c = m[x, y]
                if (kotlin.math.abs(c.red - acid.red) < .03f && kotlin.math.abs(c.green - acid.green) < .03f &&
                    kotlin.math.abs(c.blue - acid.blue) < .03f
                ) {
                    n++
                }
            }
        }
        return n
    }

    @Test
    fun `on a running clock the window reaches the new row by venom, not at once`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableIntStateOf(0)
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit
                            .Density(1f),
                ) {
                    io.github.youndie.mutant.theme.MutantTheme {
                        Box(Modifier.size(400.dp, 400.dp)) {
                            MenuBody(items, selected, { selected = it }, Modifier.testTag("menu"), "menu", "menu-pill")
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            selected = 2
            mainClock.advanceTimeBy(160)
            val early = acidInRow(2)
            mainClock.advanceTimeBy(1_000)
            val settled = acidInRow(2)
            assertTrue(settled > 2_000 && early * 2 < settled, "acid in the third row: 160 ms $early, settled $settled")
        }

    @Test
    fun `D19 each row is its whole target, 36 dp tall and 204 wide, and the rows tile the column`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still { Box(Modifier.size(400.dp, 400.dp)) { MenuBody(items, 0, {}, Modifier, "menu", "menu-pill") } }
            }
            mainClock.advanceTimeByFrame()
            val rows = onAllNodes(row).fetchSemanticsNodes().map { it.boundsInRoot }.sortedBy { it.top }
            assertEquals(4, rows.size)
            assertTrue(rows.all { it.height == 36f && it.width == 204f }, "rows $rows")
            assertEquals(listOf(36f, 36f, 36f), rows.zipWithNext { a, b -> b.top - a.top })
        }
}
