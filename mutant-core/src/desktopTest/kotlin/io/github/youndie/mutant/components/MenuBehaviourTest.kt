package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
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
    fun `the anchor opens the menu, a pick goes to onSelect, the menu stays open and the anchor closes it`() =
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
            // B-92: a pick leaves the menu open, the venom's run and the rest of the choice in view, for as long as it
            // takes: two seconds on, it still stands.
            mainClock.advanceTimeBy(2_000)
            assertEquals(2, selected)
            assertEquals(1, onAllNodes(hasText("СБРОС")).fetchSemanticsNodes().size, "still open after a pick")
            onNode(hasText("ЦВЕТНОСТЬ")).performClick()
            mainClock.advanceTimeBy(100)
            assertEquals(1, selected, "a second pick in the same opening")
            onAllNodes(hasText("Настройка"))[0].performClick()
            mainClock.advanceTimeBy(100)
            assertEquals(0, onAllNodes(hasText("СБРОС")).fetchSemanticsNodes().size, "the anchor closes it")
        }

    @Test
    fun `a press outside closes the menu`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Box(Modifier.size(400.dp, 400.dp)) {
                        MutantDropdown("Настройка", items, 0, {})
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Настройка")).performClick()
            mainClock.advanceTimeByFrame()
            onNode(hasText("ЧЁТКОСТЬ")).performClick()
            mainClock.advanceTimeBy(100)
            onAllNodes(
                androidx.compose.ui.test
                    .isRoot(),
            )[0].performTouchInput {
                click(
                    androidx.compose.ui.geometry
                        .Offset(390f, 390f),
                )
            }
            mainClock.advanceTimeBy(100)
            assertEquals(0, onAllNodes(hasText("СБРОС")).fetchSemanticsNodes().size, "closed by a press outside")
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

    /** Pixels of the focus brackets' colour in the window's own root, round the anchor (the menu is another root). */
    private fun androidx.compose.ui.test.ComposeUiTest.bracketsRoundAnchor(): Int {
        val m = onAllNodes(isRoot())[0].captureToImage().toPixelMap()
        val focus = io.github.youndie.mutant.tokens.MutantPalettes.Efir.focus
        var n = 0
        for (y in 0 until 60) {
            for (x in 0 until 140) {
                val c = m[x, y]
                if (kotlin.math.abs(c.red - focus.red) < .05f && kotlin.math.abs(c.green - focus.green) < .05f &&
                    kotlin.math.abs(c.blue - focus.blue) < .05f
                ) {
                    n++
                }
            }
        }
        return n
    }

    @Test
    fun `while its menu is open the anchor draws no focus brackets`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still { Box(Modifier.size(400.dp, 400.dp)) { MutantDropdown("Настройка", items, 1, {}) } }
            }
            mainClock.advanceTimeByFrame()
            val none = bracketsRoundAnchor()
            onAllNodes(isRoot())[0].performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeBy(48)
            val focused = bracketsRoundAnchor()
            onAllNodes(isRoot())[0].performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeBy(48)
            val open = bracketsRoundAnchor()
            assertTrue(focused > none + 20, "the anchor's brackets when focused: $focused against $none")
            assertTrue(open <= none + 2, "the anchor's brackets with its menu open: $open against $none")
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

    /** Lime pixels anywhere in [tag]'s capture, the menu and the 40 dp round it. */
    private fun androidx.compose.ui.test.ComposeUiTest.limeIn(tag: String): Int {
        val m = onNode(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, tag)).captureToImage().toPixelMap()
        val acid = io.github.youndie.mutant.tokens.MutantPalettes.Efir.primary
        var n = 0
        for (y in 0 until m.height) {
            for (x in 0 until m.width) {
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
    fun `no venom runs where no window shows, to an index outside the rows or from one`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableIntStateOf(2)
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit
                            .Density(1f),
                ) {
                    io.github.youndie.mutant.theme.MutantTheme {
                        Box(Modifier.testTag("stage").padding(40.dp)) {
                            MenuBody(items, selected, { selected = it }, Modifier, "menu", "menu-pill")
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val settled = limeIn("stage")
            val seen = mutableListOf<Int>()
            // Past the end, then back to a row from there, then to none: no lime while no window shows, and the window
            // whole in the first frame it shows again.
            for (to in listOf(7, 1, -1)) {
                selected = to
                repeat(15) {
                    mainClock.advanceTimeBy(48)
                    seen += limeIn("stage")
                }
            }
            val pastEnd = seen.subList(0, 15)
            val back = seen.subList(15, 30)
            val none = seen.subList(30, 45)
            assertTrue(settled > 2_000, "a settled window: $settled")
            assertTrue(pastEnd.all { it == 0 } && none.all { it == 0 }, "lime with no window shown: $pastEnd $none")
            assertTrue(
                back.all { kotlin.math.abs(it - settled) * 10 < settled },
                "the window back at once: $back of $settled",
            )
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
