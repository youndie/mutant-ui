package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-184, the designer's answer 30: the bottom sheet scrolls a content taller than itself, by a finger, the wheel and the
 * keys; with the content at its top a pull down is the sheet's drag (B-183), elsewhere it scrolls the content, and a
 * finger's scroll that brings the content to its top goes on as the sheet's drag; a lazy list in it scrolls itself and
 * does not throw. A 360 × 360 dp screen, so a density of 2 still fits the test window. Positions are read in dp: the
 * window's top from its node (the one with the dismiss action), the content's scroll from its title, which stands 39 dp
 * under the window's top at rest.
 */
@OptIn(ExperimentalTestApi::class)
class SheetScrollTest {
    private class Rig(
        val d: Float,
        val dismissed: () -> Int,
        /** The window's top at rest, dp. */
        val rest: Float,
    )

    private fun ComposeUiTest.rig(
        d: Float = 1f,
        list: LazyListState? = null,
    ): Rig {
        var visible by mutableStateOf(true)
        var dismissed = 0
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(reduceMotion = false) {
                    MutantScreenClock {
                        Box(Modifier.size(SCREEN.dp).background(MutantTheme.colors.page).testTag("screen")) {
                            MutantBottomSheet(visible, {
                                dismissed++
                                visible = false
                            }, windowInsets = WindowInsets(0)) {
                                BasicText("Поделиться", Modifier.testTag("title"))
                                if (list == null) {
                                    repeat(ROWS) { BasicText("Строка $it", Modifier.size(200.dp, 40.dp)) }
                                } else {
                                    LazyColumn(state = list) {
                                        items(LIST) { BasicText("Строка $it", Modifier.size(200.dp, 40.dp)) }
                                    }
                                }
                                MutantButton("Готово", {}, Modifier.testTag("done"), key = "done")
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_000)
        return Rig(d, { dismissed }, top(d)!!)
    }

    /** The window's top, dp; null once it is gone. */
    private fun ComposeUiTest.top(d: Float): Float? =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.positionInRoot
            ?.y
            ?.div(d)

    private fun ComposeUiTest.top(rig: Rig): Float = top(rig.d)!!

    /** How far the content is scrolled, dp: the title's distance above its place under the window's top. */
    private fun ComposeUiTest.scrolled(rig: Rig): Float =
        top(rig) + TITLE_DOWN - onNodeWithTag("title", useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y / rig.d

    private fun ComposeUiTest.slop(rig: Rig): Float = onRoot().fetchSemanticsNode().layoutInfo.viewConfiguration.touchSlop / rig.d

    private fun ComposeUiTest.finger(
        rig: Rig,
        at: Offset,
    ) = onNodeWithTag("screen").performTouchInput { down(at * rig.d) }

    private fun ComposeUiTest.fingerBy(
        rig: Rig,
        by: Offset,
    ) = onNodeWithTag("screen").performTouchInput { moveBy(by * rig.d, 16) }

    private fun ComposeUiTest.lift() = onNodeWithTag("screen").performTouchInput { up() }

    private fun ComposeUiTest.wheel(
        rig: Rig,
        notches: Float,
    ) {
        onNodeWithTag("screen").performMouseInput {
            moveTo(Offset(180f, 250f) * rig.d)
            scroll(notches)
        }
        mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun `a content taller than the sheet scrolls inside it by a finger, the wheel and the keys`() {
        for (d in listOf(1f, 2f)) {
            // A finger: up 120 dp, the window standing still on every frame.
            runComposeUiTest {
                val rig = rig(d)
                finger(rig, Offset(180f, 250f))
                repeat(10) {
                    fingerBy(rig, Offset(0f, -12f))
                    mainClock.advanceTimeByFrame()
                    assertEquals(rig.rest, top(rig), .5f, "density $d, a finger: the window stays")
                }
                val s = scrolled(rig)
                // Compose's scroll starts past its slop and passes on what lies beyond, within a step.
                assertTrue(s >= 120f - slop(rig) - 12f, "density $d, a finger: scrolled $s after 120 dp, the slop ${slop(rig)}")
                lift()
                mainClock.advanceTimeBy(1_000)
            }
            // The wheel: down, then back past the top, the window at rest on every frame and nothing dismissed.
            runComposeUiTest {
                val rig = rig(d)
                wheel(rig, 8f)
                val s = scrolled(rig)
                assertTrue(s > 40f, "density $d, the wheel: scrolled $s after eight notches")
                onNodeWithTag("screen").performMouseInput { scroll(-20f) }
                repeat(60) {
                    mainClock.advanceTimeByFrame()
                    assertEquals(rig.rest, top(rig), .5f, "density $d, the wheel past the top: the window stays")
                }
                assertEquals(0f, scrolled(rig), .5f, "density $d, the wheel: back at the top")
                assertEquals(0, rig.dismissed(), "density $d, the wheel: not dismissed")
            }
            // The keys: Tab to the button under the screen's foot brings it in; the page keys scroll.
            runComposeUiTest {
                val rig = rig(d)
                onNodeWithTag("screen").performKeyInput { pressKey(Key.Tab) }
                mainClock.advanceTimeBy(1_000)
                onNodeWithTag("done").assertIsFocused()
                val done = onNodeWithTag("done").fetchSemanticsNode()
                val foot = (done.positionInRoot.y + done.size.height) / d
                assertTrue(foot <= SCREEN, "density $d, Tab: the button's foot at $foot, the screen's at $SCREEN")
                assertTrue(scrolled(rig) > 0f, "density $d, Tab: scrolled ${scrolled(rig)}")
                // A page key moves the content by about the room it has (309 dp here).
                val end = scrolled(rig)
                onNodeWithTag("screen").performKeyInput { pressKey(Key.PageUp) }
                mainClock.advanceTimeBy(1_000)
                val up = scrolled(rig)
                assertTrue(end - up >= 280f, "density $d, PageUp: from $end to $up")
                onNodeWithTag("screen").performKeyInput { pressKey(Key.PageDown) }
                mainClock.advanceTimeBy(1_000)
                assertTrue(scrolled(rig) - up >= 280f, "density $d, PageDown: from $up to ${scrolled(rig)}")
                assertEquals(rig.rest, top(rig), .5f, "density $d, the keys: the window stays")
            }
        }
    }

    @Test
    fun `at its top a pull down moves the sheet, elsewhere it scrolls the content - by a finger and a mouse`() {
        for (mouse in listOf(false, true)) {
            val what = if (mouse) "a mouse" else "a finger"

            fun ComposeUiTest.press(
                rig: Rig,
                at: Offset,
            ) = if (mouse) {
                onNodeWithTag("screen").performMouseInput {
                    moveTo(at * rig.d)
                    press()
                }
            } else {
                finger(rig, at)
            }

            fun ComposeUiTest.by(
                rig: Rig,
                by: Offset,
            ) = if (mouse) onNodeWithTag("screen").performMouseInput { moveBy(by * rig.d, 16) } else fingerBy(rig, by)

            fun ComposeUiTest.let() = if (mouse) onNodeWithTag("screen").performMouseInput { release() } else lift()
            runComposeUiTest {
                val rig = rig()
                // At the top: the window follows the pointer 1:1 from its first move, and the content stays (B-183).
                press(rig, Offset(180f, 250f))
                repeat(10) {
                    by(rig, Offset(0f, 6f))
                    mainClock.advanceTimeByFrame()
                    val moved = top(rig) - rig.rest
                    assertEquals(6f * (it + 1), moved, 1f, "$what at the top: the window after ${6 * (it + 1)} dp")
                    assertEquals(0f, scrolled(rig), .5f, "$what at the top: the content stays")
                }
                let()
                mainClock.advanceTimeBy(1_000)
                assertEquals(rig.rest, top(rig), .5f, "$what at the top: back at rest")
                // Scrolled away from the top, by the wheel: a pull down is the content's, the window stays.
                wheel(rig, 8f)
                val s = scrolled(rig)
                assertTrue(s > 40f, "$what: scrolled $s after eight notches")
                press(rig, Offset(180f, 200f))
                repeat(5) {
                    by(rig, Offset(0f, 6f))
                    mainClock.advanceTimeByFrame()
                    assertEquals(rig.rest, top(rig), .5f, "$what, scrolled: the window stays")
                }
                if (!mouse) {
                    val back = s - scrolled(rig)
                    assertTrue(back >= 30f - slop(rig) - 6f, "a finger, scrolled: the content back by $back of 30 dp")
                }
                let()
                mainClock.advanceTimeBy(1_000)
                assertEquals(rig.rest, top(rig), .5f, "$what, scrolled: at rest")
                assertEquals(0, rig.dismissed(), "$what: not dismissed")
            }
        }
    }

    @Test
    fun `a finger's scroll that brings the content to its top goes on as the sheet's drag, and lets go as a drag`() {
        for (far in listOf(false, true)) {
            runComposeUiTest {
                val rig = rig()
                wheel(rig, 8f)
                val s = scrolled(rig)
                assertTrue(s > 40f, "scrolled $s after eight notches")
                val slop = slop(rig)
                finger(rig, Offset(180f, 150f))
                val step = 6f
                var travel = 0f
                var was = top(rig)
                // On to 60 dp past the content's top, or 160 for the long pull.
                val past = if (far) 160f else 60f
                while (travel < s + slop + past) {
                    fingerBy(rig, Offset(0f, step))
                    travel += step
                    mainClock.advanceTimeByFrame()
                    val now = top(rig)
                    // Once handed over, the window follows the finger 1:1: no frame jumps, none stands back.
                    assertTrue(now - was in -.5f..step + .5f, "at $travel dp the window moved ${now - was}")
                    was = now
                }
                assertEquals(0f, scrolled(rig), .5f, "the content at its top")
                val pulled = top(rig) - rig.rest
                assertEquals(travel - s - slop - step / 2, pulled, step / 2 + 1f, "the window after $travel dp, $s of it scrolled back")
                // Still for a moment, then let go: short of two fifths it settles back, past them it is dismissed.
                fingerBy(rig, Offset.Zero)
                mainClock.advanceTimeBy(120)
                lift()
                mainClock.advanceTimeBy(1_000)
                if (far) {
                    assertEquals(1, rig.dismissed(), "let go at $pulled dp: dismissed once")
                } else {
                    assertEquals(0, rig.dismissed(), "let go at $pulled dp: not dismissed")
                    assertEquals(rig.rest, top(rig), .5f, "let go at $pulled dp: back at rest")
                }
            }
        }
    }

    @Test
    fun `a lazy list in the sheet scrolls itself, does not throw, and hands a pull at its top to the sheet`() {
        for (d in listOf(1f, 2f)) {
            runComposeUiTest {
                val list = LazyListState()
                val rig = rig(d, list)
                // A finger on the list scrolls it, not the sheet nor the window.
                finger(rig, Offset(180f, 250f))
                repeat(15) {
                    fingerBy(rig, Offset(0f, -12f))
                    mainClock.advanceTimeByFrame()
                }
                lift()
                mainClock.advanceTimeBy(1_000)
                assertTrue(list.firstVisibleItemIndex > 0, "density $d: the list at ${list.firstVisibleItemIndex}")
                assertEquals(0f, scrolled(rig), .5f, "density $d: the sheet stays at its top")
                assertEquals(rig.rest, top(rig), .5f, "density $d: the window stays")
                // The wheel to the list's end and on: the sheet scrolls on to its button, whole on the screen.
                wheel(rig, 200f)
                val done = onNodeWithTag("done").fetchSemanticsNode()
                val foot = (done.positionInRoot.y + done.size.height) / d
                assertTrue(done.size.height / d >= 40f, "density $d: the button ${done.size.height / d} dp tall")
                assertTrue(foot <= SCREEN, "density $d: the button's foot at $foot")
                // Back to the tops by the wheel, then a pull down: the window follows it 1:1 from its first move (B-183).
                wheel(rig, -400f)
                assertEquals(0, list.firstVisibleItemIndex, "density $d: the wheel back to the list's top")
                finger(rig, Offset(180f, 100f))
                repeat(10) {
                    fingerBy(rig, Offset(0f, 12f))
                    mainClock.advanceTimeByFrame()
                }
                val pulled = top(rig) - rig.rest
                assertEquals(120f, pulled, 1f, "density $d: the window after 120 dp at the list's top")
                lift()
                mainClock.advanceTimeBy(1_000)
            }
        }
    }

    private companion object {
        const val SCREEN = 360f
        const val ROWS = 12
        const val LIST = 40

        /** The title under the window's top: the body's 14, its 12 of padding, the grabber's 5 and the gap's 8. */
        const val TITLE_DOWN = 14f + 12f + 5f + 8f
    }
}
