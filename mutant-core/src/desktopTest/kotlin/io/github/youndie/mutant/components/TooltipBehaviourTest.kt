package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-32: when the tooltip shows, where, and that it takes no press. Density 1: a dp is a pixel. */
@OptIn(ExperimentalTestApi::class)
class TooltipBehaviourTest {
    private fun ComposeUiTest.tooltip(
        supporting: String? = null,
        reduceMotion: Boolean = false,
        above: () -> Unit = {},
    ) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    Box(Modifier.size(400.dp, 300.dp)) {
                        // A button where the plate stands: a press through the plate must reach it.
                        MutantButton("Выше", above, Modifier.offset(40.dp, 80.dp).testTag("above"))
                        MutantTooltipBox(
                            "ПОДЕЛИТЬСЯ",
                            Modifier.offset(56.dp, 160.dp).testTag("anchor"),
                            supportingText = supporting,
                        ) {
                            MutantIconButton(MutantIcons.Share, "Поделиться", {})
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
    }

    private fun ComposeUiTest.shown(): Boolean =
        onAllNodes(hasTextExactly("ПОДЕЛИТЬСЯ")).fetchSemanticsNodes().isNotEmpty()

    private fun hasTextExactly(s: String) =
        androidx.compose.ui.test
            .hasText(s, substring = false)

    @Test
    fun `a mouse hovering the anchor shows the plate, and leaving hides it`() =
        runComposeUiTest {
            tooltip()
            assertTrue(!shown(), "before")
            onNodeWithTag("anchor").performMouseInput { moveTo(center) }
            mainClock.advanceTimeByFrame()
            assertTrue(shown(), "hovered")
            onNodeWithTag("anchor").performMouseInput { moveTo(Offset(-200f, -200f)) }
            mainClock.advanceTimeByFrame()
            assertTrue(!shown(), "left")
        }

    @Test
    fun `keyboard focus shows it, focus taken by a click does not`() =
        runComposeUiTest {
            tooltip()
            onNodeWithTag("anchor").performMouseInput { click(center) }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("anchor").performMouseInput { moveTo(Offset(-200f, -200f)) }
            mainClock.advanceTimeByFrame()
            assertTrue(!shown(), "after a click")
            // Away and back by the keyboard: from the anchor to the button above, then to the anchor again.
            onNodeWithTag("anchor").performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
            assertTrue(!shown(), "focus on the button above")
            onNodeWithTag("anchor").performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
            assertTrue(shown(), "focused by the keyboard")
        }

    @Test
    fun `a long press shows it, and it stays 1_5 s after the finger lifts`() =
        runComposeUiTest {
            tooltip()
            onNodeWithTag("anchor").performTouchInput { down(center) }
            mainClock.advanceTimeBy(200)
            assertTrue(!shown(), "a short press")
            mainClock.advanceTimeBy(600)
            assertTrue(shown(), "a long press")
            onNodeWithTag("anchor").performTouchInput { up() }
            mainClock.advanceTimeBy(1200)
            assertTrue(shown(), "1.2 s after")
            mainClock.advanceTimeBy(500)
            assertTrue(!shown(), "1.7 s after")
        }

    @Test
    fun `the plate stands centred over the anchor, its bottom 28 dp above it`() =
        runComposeUiTest {
            tooltip()
            onNodeWithTag("anchor").performMouseInput { moveTo(center) }
            mainClock.advanceTimeByFrame()
            val anchor = onNodeWithTag("anchor").fetchSemanticsNode().boundsInWindow
            val words = onNodeWithText("ПОДЕЛИТЬСЯ").fetchSemanticsNode().boundsInWindow
            // The words are centred in the plate, 12 dp in from each side, and 5 dp down its 30 dp.
            assertTrue(
                abs(words.center.x - anchor.center.x) <= 1f,
                "words ${words.center.x}, anchor ${anchor.center.x}",
            )
            assertEquals(anchor.top - 28 - 30 + 5, words.top, 1f)
        }

    @Test
    fun `the plate takes no press - a click through it reaches what lies under it`() {
        var clicks = 0
        runComposeUiTest {
            tooltip(above = { clicks++ })
            onNodeWithTag("anchor").performMouseInput { moveTo(center) }
            mainClock.advanceTimeByFrame()
            assertTrue(shown())
            val words = onNodeWithText("ПОДЕЛИТЬСЯ").fetchSemanticsNode().boundsInWindow
            val button = onNodeWithTag("above").fetchSemanticsNode().boundsInWindow
            assertTrue(words.overlaps(button), "the plate stands over the button: $words, $button")
            onNodeWithTag("above").performMouseInput {
                click(
                    Offset(
                        words.center.x - button.left,
                        words.center.y - button.top,
                    ),
                )
            }
            mainClock.advanceTimeByFrame()
        }
        assertEquals(1, clicks)
    }

    @Test
    fun `under reduce motion the plate stands`() =
        runComposeUiTest {
            tooltip(reduceMotion = true)
            onNodeWithTag("anchor").performMouseInput { moveTo(center) }
            mainClock.advanceTimeByFrame()
            val roots = onAllNodes(isRoot())

            fun shot() =
                roots.fetchSemanticsNodes().size.let { roots[it - 1].captureToImage().toPixelMap() }.let { px ->
                    List(px.width * px.height) { px[it % px.width, it / px.width] }
                }
            // The plate's body is built off the frame and draws from the next one (B-50).
            mainClock.advanceTimeBy(100)
            val a = shot()
            mainClock.advanceTimeBy(1000)
            val b = shot()
            assertEquals(0, a.indices.count { a[it] != b[it] }, "pixels that moved over a second")
        }
}
