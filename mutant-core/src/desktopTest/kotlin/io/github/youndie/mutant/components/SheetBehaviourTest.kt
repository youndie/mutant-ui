package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-35: how the sheet opens and closes, and what it holds. Density 1: a dp is a pixel. */
@OptIn(ExperimentalTestApi::class)
class SheetBehaviourTest {
    private class Screen(
        val clock: MutantClock,
        val dismissed: () -> Int,
        val show: (Boolean) -> Unit,
    )

    private fun ComposeUiTest.sheet(
        start: Boolean = true,
        reduceMotion: Boolean = false,
        insets: WindowInsets = WindowInsets(0),
        opener: FocusRequester? = null,
    ): Screen {
        var visible by mutableStateOf(start)
        var dismissed = 0
        val clock = MutantClock()
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    ProvideMutantClock(clock) {
                        Box(Modifier.size(360.dp, 600.dp).testTag("screen")) {
                            MutantButton(
                                "Открыть",
                                { visible = true },
                                if (opener !=
                                    null
                                ) {
                                    Modifier.focusRequester(opener)
                                } else {
                                    Modifier
                                },
                            )
                            MutantBottomSheet(
                                visible,
                                {
                                    dismissed++
                                    visible = false
                                },
                                windowInsets = insets,
                                returnFocusTo = opener,
                            ) {
                                BasicText("Скорость записи", Modifier.testTag("title"))
                                BasicText("SP", Modifier.testTag("last"))
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        return Screen(clock, { dismissed }, { visible = it })
    }

    private fun ComposeUiTest.titleTop(): Float =
        // Its position, not its bounds: bounds are clipped where the sheet stands below the screen.
        onNodeWithTag("title", useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y

    @Test
    fun `a click on the scrim dismisses, a click on the sheet does not, and so does Escape`() =
        runComposeUiTest {
            val s = sheet()
            mainClock.advanceTimeBy(500)
            onNodeWithText("Скорость записи").performMouseInput { click(center) }
            mainClock.advanceTimeByFrame()
            assertEquals(0, s.dismissed(), "a click on the sheet")
            onNodeWithTag("screen").performMouseInput { click(Offset(180f, 40f)) }
            mainClock.advanceTimeByFrame()
            assertEquals(1, s.dismissed(), "a click on the scrim")
            s.show(true)
            mainClock.advanceTimeBy(500)
            onNodeWithTag("screen").performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            assertEquals(2, s.dismissed(), "Escape")
        }

    @Test
    fun `it rises from below in 330 ms, and under reduce motion is there at once`() {
        var rising = 0f to 0f
        var mid = 0f
        var at = 0f
        runComposeUiTest {
            sheet(start = false).show(true)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val early = titleTop()
            // Two of the kit's 83 ms steps in: risen part of the way, not yet there.
            mainClock.advanceTimeBy(140)
            mid = titleTop()
            mainClock.advanceTimeBy(500)
            rising = early to titleTop()
        }
        runComposeUiTest {
            sheet(start = false, reduceMotion = true).show(true)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            at = titleTop()
        }
        val (early, settled) = rising
        assertTrue(early > settled + 40, "rising: ${early - settled} dp below where it settles")
        assertTrue(
            mid < early - 20 && mid > settled + 20,
            "halfway: ${mid - settled} dp below, from ${early - settled}",
        )
        assertEquals(settled, at, 1f, "reduce motion: at once")
    }

    @Test
    fun `while it shows the screen's clock stands, and runs again once it has sunk`() =
        runComposeUiTest {
            val s = sheet()
            mainClock.advanceTimeBy(500)
            assertTrue(s.clock.holds > 0, "held while open")
            s.show(false)
            mainClock.advanceTimeBy(100)
            assertTrue(s.clock.holds > 0, "held while it sinks")
            mainClock.advanceTimeBy(300)
            assertEquals(0, s.clock.holds, "released once gone")
            assertTrue(
                onNodeWithTag("title", useUnmergedTree = true).let {
                    runCatching { it.fetchSemanticsNode() }.isFailure
                },
                "gone",
            )
        }

    @Test
    fun `closed, focus goes back to what opened it`() =
        runComposeUiTest {
            val opener = FocusRequester()
            val s = sheet(start = false, opener = opener)
            runOnIdle { opener.requestFocus() }
            s.show(true)
            mainClock.advanceTimeBy(500)
            onNodeWithTag("screen").performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeBy(500)
            onNodeWithText("Открыть").assertIsFocused()
        }

    @Test
    fun `its content keeps above the window's bottom inset`() =
        runComposeUiTest {
            sheet(insets = WindowInsets(bottom = 40))
            mainClock.advanceTimeBy(500)
            val last = onNodeWithTag("last", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            // The content ends 12 dp above the inset.
            assertEquals(600f - 40f - 12f, last.bottom, 1f)
        }

    @Test
    fun `reversed mid-way it goes on from where it stands - no frame jumps`() =
        runComposeUiTest {
            val s = sheet(start = false)
            // Where it settles, and how far below that its first frame stands: the travel of the rise.
            s.show(true)
            mainClock.advanceTimeByFrame()
            val first = titleTop()
            mainClock.advanceTimeBy(500)
            val settled = titleTop()
            val travel = first - settled
            s.show(false)
            mainClock.advanceTimeBy(400)
            var last = Float.NaN
            val jumps = mutableListOf<String>()

            // The fastest the sheet moves on its own: the sink's 200 ms through ease-out² is under a fifth of the travel
            // a frame; a restart from either end moves it by half the travel or more.
            fun frame() {
                mainClock.advanceTimeByFrame()
                val now = runCatching { titleTop() }.getOrDefault(settled + travel)
                if (!last.isNaN() && kotlin.math.abs(now - last) > .2f * travel) {
                    jumps += "${mainClock.currentTime}: $last -> $now"
                }
                last = now
            }
            s.show(true)
            repeat(7) { frame() }
            val rising = last
            assertTrue(rising > settled + .2f * travel, "a third into the rise: ${rising - settled} of $travel")
            s.show(false)
            repeat(6) { frame() }
            assertTrue(last > rising, "sinking from where it stood: ${rising - settled}, then ${last - settled}")
            s.show(true)
            repeat(30) { frame() }
            assertEquals(settled, last, 1f, "risen again")
            s.show(false)
            repeat(5) { frame() }
            val sinking = last
            assertTrue(sinking - settled in .1f * travel..0.8f * travel, "half into the sink: ${sinking - settled}")
            s.show(true)
            repeat(30) { frame() }
            assertTrue(jumps.isEmpty(), "jumps over ${.2f * travel} dp: $jumps")
            assertEquals(settled, last, 1f, "risen again")
        }

    @Test
    fun `through its sink the keys reach nothing behind it, and focus is back with the opener after`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var visible by mutableStateOf(false)
            var opens = 0
            val opener = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box(Modifier.size(360.dp, 600.dp)) {
                            MutantButton("Открыть", {
                                opens++
                                visible = true
                            }, Modifier.focusRequester(opener).testTag("opener"))
                            MutantBottomSheet(visible, { visible = false }, returnFocusTo = opener) {
                                MutantButton("Готово", { visible = false }, Modifier.testTag("done"))
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            runOnIdle { opener.requestFocus() }
            visible = true
            mainClock.advanceTimeBy(500)
            focusOn("done")
            // Enter on Done, then Enter again as the sheet sinks.
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            assertEquals(0, opens, "the second Enter reached the opener under the scrim")
            assertTrue(!visible, "sinking")
            mainClock.advanceTimeBy(300)
            onNodeWithTag("opener").assertIsFocused()
        }

    /** Tab until [tag] holds focus. */
    private fun ComposeUiTest.focusOn(tag: String) {
        repeat(6) {
            if (onNodeWithTag(tag).fetchSemanticsNode().config.getOrElse(SemanticsProperties.Focused) { false }) return
            onRoot().performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
        }
        onNodeWithTag(tag).assertIsFocused()
    }

    @Test
    fun `as it sinks, what is on it answers neither a click nor a key`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var visible by mutableStateOf(true)
            var done = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box(Modifier.size(360.dp, 600.dp)) {
                            MutantBottomSheet(visible, { visible = false }) {
                                MutantButton("Готово", {
                                    done++
                                    visible = false
                                }, Modifier.testTag("done"))
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            focusOn("done")
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            assertEquals(1, done, "Enter on Done closes")
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            assertEquals(1, done, "a second Enter as it sinks")
            visible = true
            mainClock.advanceTimeBy(500)
            onNodeWithTag("done").performMouseInput { click(center) }
            mainClock.advanceTimeByFrame()
            assertEquals(2, done, "a click on Done closes")
            // Done where it stands now, a frame into the sink.
            onNodeWithTag("done").performMouseInput { click(center) }
            mainClock.advanceTimeByFrame()
            assertEquals(2, done, "a second click as it sinks")
        }
}
