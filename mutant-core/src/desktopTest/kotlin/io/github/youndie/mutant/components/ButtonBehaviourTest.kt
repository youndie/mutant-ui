package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-15: what the buttons do — on release, not while working or disabled — and what a consumer does to them. */
@OptIn(ExperimentalTestApi::class)
class ButtonBehaviourTest {
    @Test
    fun `the text button's divider spans its label and stands 2 dp tall at any density`() {
        for (d in listOf(1f, 2f, 3f)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(d)) {
                        io.github.youndie.mutant.theme.MutantTheme {
                            Box(Modifier.testTag("t")) { MutantTextButton("Сбросить настройки", {}) }
                        }
                    }
                }
                mainClock.advanceTimeByFrame()
                val px = onNodeWithTag("t").captureToImage().toPixelMap()
                val blue = io.github.youndie.mutant.tokens.MutantPalettes.Efir.noSignal

                fun isBlue(c: Color) =
                    kotlin.math.abs(c.red - blue.red) + kotlin.math.abs(c.green - blue.green) +
                        kotlin.math.abs(c.blue - blue.blue) < .06f && c.alpha > .9f
                val xs = (0 until px.width).filter { x -> (0 until px.height).any { y -> isBlue(px[x, y]) } }
                val ys = (0 until px.height).filter { y -> (0 until px.width).count { x -> isBlue(px[x, y]) } > 4 }
                assertTrue(xs.isNotEmpty(), "strokes drawn at density $d")
                assertTrue(
                    xs.last() > px.width * .75f,
                    "strokes reach the far end at density $d: ${xs.last()} of ${px.width}",
                )
                assertEquals((2 * d).toInt(), ys.size, "2 dp of strokes at density $d: rows $ys")
            }
        }
    }

    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) {
                px[
                    it % px.width, it /
                        px.width,
                ]
            }
        }

    @Test
    fun `the action fires on release, not on press`() =
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            var clicks by mutableIntStateOf(0)
            setContent { MutantTheme { MutantButton("Воспроизвести", { clicks++ }, Modifier.testTag("b")) } }
            onNodeWithTag("b").performTouchInput { down(center) }
            waitForIdle()
            assertEquals(0, clicks, "pressed, not released")
            onNodeWithTag("b").performTouchInput { up() }
            waitForIdle()
            assertEquals(1, clicks, "released")
        }

    @Test
    fun `a loading button takes no click and reads as indeterminate progress`() =
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            var clicks by mutableIntStateOf(0)
            setContent {
                MutantTheme {
                    MutantButton(
                        "Перемотка…",
                        { clicks++ },
                        Modifier.testTag("b"),
                        state = MutantButtonState.Loading,
                    )
                }
            }
            onNodeWithTag("b").assertIsNotEnabled()
            onNodeWithTag(
                "b",
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
            onNodeWithTag("b").performClick()
            assertEquals(0, clicks)
        }

    @Test
    fun `a disabled button of every kind takes no click`() =
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            var clicks by mutableIntStateOf(0)
            setContent {
                MutantTheme {
                    androidx.compose.foundation.layout.Column {
                        MutantButton("a", { clicks++ }, Modifier.testTag("f"), enabled = false)
                        MutantOutlinedButton("b", { clicks++ }, Modifier.testTag("o"), enabled = false)
                        MutantTextButton("c", { clicks++ }, Modifier.testTag("t"), enabled = false)
                    }
                }
            }
            for (t in listOf("f", "o", "t")) {
                onNodeWithTag(t).assertIsNotEnabled().performClick()
            }
            assertEquals(0, clicks)
            setContent { MutantTheme { MutantTextButton("c", { clicks++ }, Modifier.testTag("t")) } }
            onNodeWithTag("t").assertIsEnabled().performClick()
            assertEquals(1, clicks)
        }

    @Test
    fun `success shows the check before the label without being asked`() =
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box {
                            MutantButton(
                                "Готово",
                                {},
                                Modifier.width(200.dp).testTag("s"),
                                state = MutantButtonState.Success,
                            )
                        }
                    }
                }
            }
            val withCheck = pixels("s")
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box {
                            MutantButton(
                                "Готово",
                                {},
                                Modifier.width(200.dp).testTag("s"),
                                state = MutantButtonState.Success,
                                icon = io.github.youndie.mutant.icons.MutantIcons.Done,
                            )
                        }
                    }
                }
            }
            assertEquals(withCheck, pixels("s"), "the default icon of success is Done")
        }

    private fun frames(reduceMotion: Boolean): Boolean {
        var changed = false
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(
                    reduceMotion = reduceMotion,
                ) { MutantButton("Воспроизвести", {}, Modifier.testTag("b")) }
            }
            mainClock.advanceTimeByFrame()
            val a = pixels("b")
            repeat(2000 / 16) { mainClock.advanceTimeByFrame() }
            changed = a != pixels("b")
        }
        return changed
    }

    @Test
    fun `reduce motion is the pause - the body stands, and boils on while the clock runs`() {
        assertTrue(frames(reduceMotion = false), "running")
        assertTrue(!frames(reduceMotion = true), "paused")
    }

    @Test
    fun `the touch target is 48 dp tall and at least 48 wide whatever the label`() =
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            setContent {
                MutantTheme {
                    androidx.compose.foundation.layout.Column {
                        MutantButton("OK", {}, Modifier.testTag("f"))
                        MutantOutlinedButton("·", {}, Modifier.testTag("o"))
                        MutantTextButton("Да", {}, Modifier.testTag("t"))
                    }
                }
            }
            for (t in listOf("f", "o", "t")) {
                onNodeWithTag(t).assertHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
            }
        }

    @Test
    fun `keyboard focus shows on the body`() =
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            val focus = FocusRequester()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box {
                            MutantButton(
                                "Воспроизвести",
                                {},
                                Modifier.width(200.dp).focusRequester(focus).testTag("b"),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val rest = pixels("b")
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            val focused = pixels("b")
            assertTrue(rest.any { it.alpha > 0f }, "the button drew")
            assertNotEquals(rest, focused, "focused under keyboard input")
        }

    @Test
    fun `no width, a long label and a doubled font neither throw nor push the label out`() =
        runComposeUiTest {
            // The kit's clock asks for frames for ever: a test drives it by hand.
            mainClock.autoAdvance = false
            val long = "Воспроизвести с начала первой стороны без подписи"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        androidx.compose.foundation.layout.Column {
                            MutantButton(long, {}, Modifier.width(0.dp).testTag("zero"))
                            MutantButton(long, {}, Modifier.width(200.dp).testTag("long"))
                            MutantOutlinedButton(long, {}, Modifier.width(160.dp).testTag("outlined"))
                            MutantTextButton(long, {}, Modifier.width(120.dp).testTag("text"))
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            // Every label stays one line inside the 48 dp body, a doubled 15 sp line box included. A width of none is
            // the caller's to give: the button takes it and draws nothing, without throwing.
            for (tag in listOf(
                "long",
                "outlined",
                "text",
            )) {
                onNodeWithTag(tag).assertIsDisplayed().assertHeightIsEqualTo(48.dp)
            }
            val labels = onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()
            assertEquals(4, labels.size)
            for (node in labels) assertTrue(node.boundsInRoot.height <= 48f, "label ${node.boundsInRoot}")
            // Drawing at the cut must not throw, nor laying out at none.
            pixels("long")
            // The label is cut with an ellipsis where the width runs out.
            assertTrue(labels.all { overflowOf(it) == TextOverflow.Ellipsis })
        }

    private fun overflowOf(node: androidx.compose.ui.semantics.SemanticsNode): TextOverflow? {
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.config
            .getOrNull(
                androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult,
            )?.action
            ?.invoke(results)
        return results.firstOrNull()?.layoutInput?.overflow
    }

    /** How many of the button's pixels show its resting acid fill: a boil or a simmer mixes it towards amber. */
    private fun ComposeUiTest.acid(tag: String): Int {
        val acid = io.github.youndie.mutant.tokens.MutantPalettes.Efir.primary
        return pixels(tag).count {
            kotlin.math.abs(it.red - acid.red) + kotlin.math.abs(it.green - acid.green) +
                kotlin.math.abs(it.blue - acid.blue) < .06f
        }
    }

    @Test
    fun `a loading button shows no press, no simmer and no hand, and simmers again under the mouse when done`() =
        runComposeUiTest {
            // B-168 (F-buttons-01): busy, the button takes no click (B-62), so its body answers neither a press nor the
            // mouse; a mouse already over it when the work starts lets go, and one still over it when the work ends
            // simmers again (B-63).
            mainClock.autoAdvance = false
            var state by mutableStateOf(MutantButtonState.Idle)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box(Modifier.testTag("stage")) {
                            MutantButton(
                                "Воспроизвести",
                                {},
                                Modifier.width(200.dp).testTag("b"),
                                state = state,
                            )
                        }
                    }
                }
            }

            fun frames(n: Int): List<Int> =
                List(n) {
                    Thread.sleep(3)
                    mainClock.advanceTimeByFrame()
                    acid("b")
                }
            frames(30)
            state = MutantButtonState.Loading
            val busy = frames(30)
            val rest = busy.min()
            assertTrue(rest > 2_000, "the loading body drew its acid fill: $busy")
            // The mouse over it, then pressed and held: the fill stays acid on every frame.
            onNodeWithTag("b").performMouseInput { moveTo(center) }
            val hovered = frames(30)
            onNodeWithTag("b").performMouseInput { press() }
            val pressed = frames(30)
            onNodeWithTag("b").performMouseInput { release() }
            val released = frames(30)
            for ((phase, counts) in listOf("hovered" to hovered, "pressed" to pressed, "released" to released)) {
                assertTrue(counts.min() > rest * .8, "$phase while loading, acid pixels: $counts against $rest")
            }
            onNodeWithTag("b").assert(SemanticsMatcher.keyNotDefined(MutantHandCursor))
            // Done with the mouse still over it: the live button simmers, and shows the hand again.
            state = MutantButtonState.Idle
            val done = frames(30)
            assertTrue(done.last() < rest * .2, "idle under the mouse simmers: $done")
            onNodeWithTag("b").assert(SemanticsMatcher.expectValue(MutantHandCursor, true))
            // Working again with the mouse over it: the simmer lets go.
            state = MutantButtonState.Loading
            val again = frames(40)
            assertTrue(again.last() > rest * .8, "the simmer lets go when the work starts: $again")
        }
}
