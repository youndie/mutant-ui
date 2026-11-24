package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.qa.Still
import io.github.youndie.mutant.surface.HeatMachine
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-201, the designer's answers 28 and 39: near the window's foot the menu opens above its anchor; one taller than the
 * window keeps 16 dp from its top and 34 dp from its foot and scrolls; a press on the chosen row answers it.
 */
@OptIn(ExperimentalTestApi::class)
class MenuEdgesAndChosenRowTest {
    private fun ComposeUiTest.window(tag: String): Rect = onNode(hasTestTag(tag)).fetchSemanticsNode().boundsInWindow

    private fun ComposeUiTest.openAt(
        d: Float,
        x: Int,
        y: Int,
        items: List<String>,
    ) {
        mainClock.autoAdvance = false
        setContent {
            Still(Density(d)) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset(x.dp, y.dp)) {
                        Box(Modifier.size(120.dp, 48.dp).testTag("anchor"))
                        MutantMenu(true, {}, items, 0, {}, Modifier.testTag("menu"))
                    }
                }
            }
        }
        mainClock.advanceTimeBy(200)
    }

    @Test
    fun `an anchor near the window's foot opens the menu above it, its drips' 18 dp clear of the anchor`() {
        for (d in listOf(1f, 2f)) {
            runDesktopComposeUiTest((400 * d).toInt(), (400 * d).toInt()) {
                openAt(d, 40, 330, listOf("ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ", "СБРОС"))
                val anchor = window("anchor")
                val menu = window("menu")
                // 4 rows: 6 + 4 · 36 + 6 = 156 dp, from 330 − 18 − 156 = 156 dp to 312 dp.
                assertEquals(156f * d, menu.height, "the whole menu at density $d")
                assertEquals(anchor.top - 18f * d, menu.bottom, "the menu's foot 18 dp above the anchor at density $d")
                assertEquals(40f * d, menu.left, "across, at the anchor's start at density $d")
            }
        }
    }

    @Test
    fun `with room under its anchor the menu stands 50 dp under the anchor's top, as before`() =
        runDesktopComposeUiTest(400, 400) {
            openAt(1f, 40, 40, listOf("ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ", "СБРОС"))
            assertEquals(90f, window("menu").top)
        }

    /**
     * 30 rows at density [d]. Both densities ran as one test in 15–35 s on a Mac (B-210), each a body of 30 key presses
     * 250 ms apart; each density is a test of its own now.
     */
    private fun thirtyRows(d: Float) {
        runDesktopComposeUiTest((400 * d).toInt(), (400 * d).toInt()) {
            val items = List(30) { "ПУНКТ ${it + 1}" }
            openAt(d, 40, 20, items)
            val menu = window("menu")
            assertEquals(16f * d, menu.top, "16 dp from the window's top at density $d")
            assertEquals((400f - 34f) * d, menu.bottom, "34 dp from the window's foot at density $d")
            onNode(hasText("ПУНКТ 1")).assertIsFocused()
            // Down through every row, then once more round to the first: each focused row is brought into the view.
            for (i in 1..30) {
                val to = items[i % 30]
                onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
                mainClock.advanceTimeBy(250)
                onNode(hasText(to)).assertIsFocused()
                val row = onNode(hasText(to)).fetchSemanticsNode().boundsInWindow
                assertTrue(
                    row.top >= menu.top - .5f && row.bottom <= menu.bottom + .5f,
                    "$to in view at density $d: $row in $menu",
                )
            }
        }
    }

    @Test
    fun `30 rows in a 400 dp window stand 16 dp from its top and 34 dp from its foot and scroll to the last`() =
        thirtyRows(1f)

    @Test
    fun `at density 2, 30 rows in a 400 dp window stand 16 dp from its top and 34 dp from its foot and scroll`() =
        thirtyRows(2f)

    /** Lime pixels outside rows [from, to) of the menu drawn 40 dp into the stage. */
    private fun ComposeUiTest.limeOutside(
        from: Int,
        to: Int,
    ): Int {
        val m = onNode(hasTestTag("stage")).captureToImage().toPixelMap()
        val acid = MutantPalettes.Efir.primary
        var n = 0
        for (y in 0 until m.height) {
            // The rows' band, 40 + 6 + 36 · i dp, with 4 dp of the window's outline round it.
            if (y >= 46 + 36 * from - 4 && y < 46 + 36 * to + 4) continue
            for (x in 0 until m.width) {
                val c = m[x, y]
                if (abs(c.red - acid.red) < .03f && abs(c.green - acid.green) < .03f &&
                    abs(c.blue - acid.blue) < .03f
                ) {
                    n++
                }
            }
        }
        return n
    }

    /** The window's row as drawn: pixels that differ from [before] by more than 0.1 in any channel. */
    private fun ComposeUiTest.changedInRow(
        row: Int,
        before: androidx.compose.ui.graphics.PixelMap?,
    ): Pair<androidx.compose.ui.graphics.PixelMap, Int> {
        val m = onNode(hasTestTag("stage")).captureToImage().toPixelMap()
        var n = 0
        if (before != null) {
            for (y in 46 + 36 * row until 46 + 36 * (row + 1)) {
                for (x in 46 until 46 + 204) {
                    val a = m[x, y]
                    val b = before[x, y]
                    if (abs(a.red - b.red) > .1f || abs(a.green - b.green) > .1f || abs(a.blue - b.blue) > .1f) n++
                }
            }
        }
        return m to n
    }

    @Test
    fun `a press on the chosen row calls onSelect with it, its window's heat reads 0,8 and nothing pours`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val items = listOf("ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ", "СБРОС")
            val clock = MutantClock()
            val plate = HeatMachine()
            var selected by mutableIntStateOf(1)
            val picks = mutableListOf<Int>()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false) {
                        ProvideMutantClock(clock) {
                            Box(Modifier.testTag("stage").padding(40.dp)) {
                                MenuBody(
                                    items,
                                    selected,
                                    {
                                        picks += it
                                        selected = it
                                    },
                                    Modifier,
                                    "menu",
                                    "menu-pill",
                                    plate = plate,
                                )
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val (rest, _) = changedInRow(1, null)
            assertEquals(0, limeOutside(1, 2), "at rest, lime only in the chosen row")
            onNode(hasText("ЦВЕТНОСТЬ")).performClick()
            assertEquals(listOf(1), picks, "the chosen row handed to onSelect")
            assertEquals(.8, plate.at(clock.nowMs), 1e-9, "the window's heat in the press's frame")
            val outside = mutableListOf<Int>()
            var boiled = 0
            repeat(12) {
                mainClock.advanceTimeBy(32)
                outside += limeOutside(1, 2)
                if (it == 1) boiled = changedInRow(1, rest).second
            }
            assertTrue(outside.all { it == 0 }, "no pour: lime outside the chosen row $outside")
            // The boil drawn: heat 0.8 turns most of the window's lime (204 × 36 less the word) from acid.
            assertTrue(boiled > 2_000, "the window drawn hot: $boiled pixels changed in its row")
            mainClock.advanceTimeBy(1_000)
            assertEquals(0.0, plate.at(clock.nowMs), "cooled from 0.8 in 384 ms")
        }
}
