package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-21: where the card puts its lines, what it does when clicked or disabled, and what a consumer does to it. */
@OptIn(ExperimentalTestApi::class)
class CardBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private fun ComposeUiTest.card(
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
        title: String = "Без подписи №3",
    ) = setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MutantTheme {
                Box {
                    MutantCard(modifier.testTag("c"), enabled = enabled, onClick = onClick) {
                        MutantCardTitle(title, Modifier.testTag("t"))
                        MutantCardSubtitle("Сторона A · 46 мин", Modifier.testTag("s"))
                    }
                }
            }
        }
    }

    @Test
    fun `given no size it is 200 by 80, the title 16 in and 14 down, the subtitle 4 under it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            card(title = "Да")
            val c = onNodeWithTag("c").fetchSemanticsNode().boundsInRoot
            val t = onNodeWithTag("t", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val s = onNodeWithTag("s", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(200f, c.width, "width")
            assertEquals(80f, c.height, "height")
            assertEquals(16f, t.left - c.left, "title from the side")
            assertEquals(14f, t.top - c.top, "title from the top")
            assertEquals(16f, s.left - c.left, "subtitle from the side")
            assertEquals(4f, s.top - t.bottom, "between the lines")
        }

    @Test
    fun `a card without onClick is no button, with it fires on release`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks by mutableIntStateOf(0)
            setContent {
                MutantTheme {
                    Column {
                        MutantCard(Modifier.testTag("still")) { MutantCardTitle("Без подписи №3") }
                        MutantCard(Modifier.testTag("c"), onClick = { clicks++ }) { MutantCardTitle("Сторона A") }
                    }
                }
            }
            onNodeWithTag("still").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            onNodeWithTag("c").performTouchInput { down(center) }
            waitForIdle()
            assertEquals(0, clicks, "pressed, not released")
            onNodeWithTag("c").performTouchInput { up() }
            waitForIdle()
            assertEquals(1, clicks, "released")
        }

    @Test
    fun `a disabled card takes no click`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks = 0
            card(enabled = false, onClick = { clicks++ })
            onNodeWithTag("c").assertIsNotEnabled().performClick()
            assertEquals(0, clicks)
        }

    /** How many of a line's pixels are, within a few levels, [color]. */
    private fun ComposeUiTest.inked(
        tag: String,
        color: Color,
    ): Int =
        onNodeWithTag(tag, useUnmergedTree = true).captureToImage().toPixelMap().let { px ->
            var n = 0
            for (y in 0 until px.height) {
                for (x in 0 until px.width) {
                    val p = px[x, y]
                    if (abs(p.red - color.red) + abs(p.green - color.green) + abs(p.blue - color.blue) < .04f) n++
                }
            }
            n
        }

    @Test
    fun `the title reads in the text colour, the subtitle in the secondary, both grey when disabled`() {
        val c = MutantPalettes.Efir
        runComposeUiTest {
            mainClock.autoAdvance = false
            card()
            mainClock.advanceTimeByFrame()
            assertTrue(inked("t", c.text) > 20, "title in the text colour")
            assertTrue(inked("s", c.textSecondary) > 20, "subtitle in the secondary colour")
            assertEquals(0, inked("t", c.textDisabled), "no grey in the live title")
        }
        runComposeUiTest {
            mainClock.autoAdvance = false
            card(enabled = false)
            mainClock.advanceTimeByFrame()
            assertTrue(inked("t", c.textDisabled) > 20, "disabled title grey")
            assertTrue(inked("s", c.textDisabled) > 20, "disabled subtitle grey")
            assertEquals(0, inked("t", c.text), "no live text in a disabled title")
        }
    }

    private fun frames(reduceMotion: Boolean): Boolean {
        var changed = false
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = reduceMotion) {
                    MutantCard(Modifier.testTag("c")) { MutantCardTitle("Без подписи №3") }
                }
            }
            mainClock.advanceTimeByFrame()
            val a = pixels("c")
            repeat(2000 / 16) { mainClock.advanceTimeByFrame() }
            changed = a != pixels("c")
        }
        return changed
    }

    @Test
    fun `reduce motion is the pause - the card stands, and boils on while the clock runs`() {
        assertTrue(frames(reduceMotion = false), "running")
        assertTrue(!frames(reduceMotion = true), "paused")
    }

    @Test
    fun `keyboard focus shows on a clickable card`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val focus = FocusRequester()
            card(onClick = {}, modifier = Modifier.focusRequester(focus))
            mainClock.advanceTimeByFrame()
            val rest = pixels("c")
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            assertTrue(rest.any { it.alpha > 0f }, "the card drew")
            assertNotEquals(rest, pixels("c"), "focused under keyboard input")
        }

    @Test
    fun `no width, a long title and a doubled font neither throw nor leave the card`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Без подписи №3, сторона A, с начала первой стороны и до конца второй"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column(Modifier.testTag("all")) {
                            MutantCard(Modifier.width(0.dp).testTag("zero")) { MutantCardTitle(long) }
                            MutantCard(Modifier.width(240.dp).testTag("long")) {
                                MutantCardTitle(long)
                                MutantCardSubtitle(long)
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            // A width of none is the caller's: the card takes it and draws nothing, without throwing.
            pixels("all")
            onNodeWithTag("long").assertIsDisplayed()
            pixels("long")
            val card = onNodeWithTag("long").fetchSemanticsNode().boundsInRoot
            assertTrue(card.height >= 80f, "at least the least height: $card")
            // Each line stays one line inside the card, cut with an ellipsis at its right; the card grows to hold them.
            val lines = onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(3, lines.size)
            for (b in lines.filter { it.top >= card.top }) {
                assertTrue(
                    b.left >= card.left + 16f && b.right <= card.right - 16f + .5f,
                    "inside the sides: $b in $card",
                )
                assertTrue(b.bottom <= card.bottom, "inside the bottom: $b in $card")
            }
            // Cut with an ellipsis, not drawn on past the card (a node's bounds cannot show that).
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                node.config
                    .getOrNull(SemanticsActions.GetTextLayoutResult)
                    ?.action
                    ?.invoke(results)
                assertEquals(TextOverflow.Ellipsis, results.single().layoutInput.overflow)
            }
        }

    /** The pixels under a bare card's lines, drawn with [key]'s seed. */
    private fun textArea(key: String): List<Color> {
        var area = emptyList<Color>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f), LocalCardHidden provides true) {
                    MutantTheme {
                        Box {
                            MutantCard(Modifier.width(328.dp).testTag("c"), key = key) {
                                MutantCardTitle("Без подписи №3", Modifier.testTag("t"))
                                MutantCardSubtitle("Сторона A · 46 мин", Modifier.testTag("s"))
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val c = onNodeWithTag("c").fetchSemanticsNode().boundsInRoot
            val t = onNodeWithTag("t", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val s = onNodeWithTag("s", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val px = onNodeWithTag("c").captureToImage().toPixelMap()
            area =
                ((t.top - c.top).toInt() until (s.bottom - c.top).toInt()).flatMap { y ->
                    ((t.left - c.left).toInt() until (maxOf(t.right, s.right) - c.left).toInt()).map { x -> px[x, y] }
                }
        }
        return area
    }

    @Test
    fun `no flow line, mosaic or dead pixel enters the lines - the seed does not reach them`() {
        // Without the clean zone, 124 to 545 of these 5054 pixels change with the seed (measured 2026-11-14).
        val first = textArea("card/0")
        for (k in 1 until 8) assertEquals(first, textArea("card/$k"), "card/$k under its lines")
    }
}
