package io.github.youndie.mutant.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-30: what the slider does, and what a consumer does to it. Density 1: a dp is a pixel. */
@OptIn(ExperimentalTestApi::class)
class SliderBehaviourTest {
    private fun ComposeUiTest.slider(
        start: Int = 0,
        enabled: Boolean = true,
        reduceMotion: Boolean = false,
        modifier: Modifier = Modifier.width(300.dp),
    ): () -> Int {
        var value by mutableIntStateOf(start)
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    Box {
                        MutantSlider(value, { value = it }, modifier.testTag("s"), label = "ТРЕКИНГ", enabled = enabled)
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        return { value }
    }

    /** The row's top: under the 23 dp header and its 2 dp gap. The handle stands 10 dp into the row, 34 dp. */
    private val row = 25f

    /** The handle's middle for a value of -10…10 on a 300 dp slider. */
    private fun middle(v: Int) = (v + 10) / 20f * 266f + 17f

    private fun ComposeUiTest.pixels(): List<Color> =
        onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) {
                px[
                    it % px.width, it /
                        px.width,
                ]
            }
        }

    @Test
    fun `a mouse drags the handle by where it took it, and the value follows`() =
        runComposeUiTest {
            val value = slider()
            onNodeWithTag("s").performMouseInput {
                // Taken 5 dp right of its middle, moved 66.5 dp (a quarter of the travel): the middle goes to +5.
                moveTo(Offset(middle(0) + 5, row + 27))
                press()
                moveBy(Offset(33.25f, 0f))
                moveBy(Offset(33.25f, 0f))
                release()
            }
            mainClock.advanceTimeByFrame()
            assertEquals(5, value())
        }

    @Test
    fun `a finger drags it too, and past the end it stops at the end`() =
        runComposeUiTest {
            val value = slider()
            onNodeWithTag("s").performTouchInput {
                down(Offset(middle(0), row + 27))
                moveBy(Offset(100f, 0f))
                moveBy(Offset(400f, 0f))
                up()
            }
            mainClock.advanceTimeByFrame()
            assertEquals(10, value())
        }

    @Test
    fun `a press on the channel away from the handle takes it there by its middle`() =
        runComposeUiTest {
            val value = slider()
            onNodeWithTag("s").performMouseInput {
                moveTo(Offset(middle(-8), row + 27))
                press()
                release()
            }
            mainClock.advanceTimeByFrame()
            assertEquals(-8, value())
        }

    @Test
    fun `the arrows step it one at a time, Home and End go to the ends`() =
        runComposeUiTest {
            val focus = FocusRequester()
            val value = slider(modifier = Modifier.width(300.dp).focusRequester(focus))
            runOnIdle { focus.requestFocus() }
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionRight) }
            assertEquals(1, value())
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionLeft) }
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionLeft) }
            assertEquals(-1, value())
            onNodeWithTag("s").performKeyInput { pressKey(Key.MoveEnd) }
            assertEquals(10, value())
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionRight) }
            assertEquals(10, value())
            onNodeWithTag("s").performKeyInput { pressKey(Key.MoveHome) }
            assertEquals(-10, value())
        }

    @Test
    fun `a reader sees the range and the value, and sets it`() =
        runComposeUiTest {
            val value = slider(start = 3)
            val node = onNodeWithTag("s").fetchSemanticsNode()
            val range = node.config[SemanticsProperties.ProgressBarRangeInfo]
            assertEquals(3f, range.current)
            assertEquals(-10f..10f, range.range)
            assertEquals("+3", node.config[SemanticsProperties.StateDescription])
            onNodeWithTag("s").performSemanticsAction(SemanticsActions.SetProgress) { it(-4f) }
            assertEquals(-4, value())
        }

    @Test
    fun `disabled, it takes neither a drag nor a key`() =
        runComposeUiTest {
            val value = slider(start = -4, enabled = false)
            onNodeWithTag("s").assertIsNotEnabled()
            onNodeWithTag("s").performMouseInput {
                moveTo(Offset(middle(-4), row + 27))
                press()
                moveBy(Offset(100f, 0f))
                release()
            }
            mainClock.advanceTimeByFrame()
            assertEquals(-4, value())
        }

    /** The slider's pixels a second of clock apart. */
    private fun ComposeUiTest.secondApart(hold: Boolean): Pair<List<Color>, List<Color>> {
        if (hold) onNodeWithTag("s").performMouseInput { moveTo(Offset(middle(0), row + 27)).also { press() } }
        mainClock.advanceTimeBy(1_000)
        val a = pixels()
        mainClock.advanceTimeBy(1_000)
        return a to pixels()
    }

    @Test
    fun `reduce motion is the pause - the slider stands, and boils on while the clock runs`() {
        var running: Pair<List<Color>, List<Color>>? = null
        var paused: Pair<List<Color>, List<Color>>? = null
        runComposeUiTest {
            slider()
            running = secondApart(hold = false)
        }
        runComposeUiTest {
            slider(reduceMotion = true)
            paused = secondApart(hold = false)
        }
        val (r0, r1) = running!!
        val (p0, p1) = paused!!
        assertNotEquals(r0, r1, "running")
        assertEquals(p0, p1, "paused")
    }

    @Test
    fun `held, the handle boils - it steps at the hot tick, half again as often as at rest`() {
        // The picture's changes over a second of 16 ms frames: a resting body steps at 8 ticks a second, a held one
        // at the hot tick, 12 a second.
        fun changes(hold: Boolean): Int {
            var n = 0
            runComposeUiTest {
                slider()
                mainClock.advanceTimeBy(2_000)
                if (hold) onNodeWithTag("s").performMouseInput { moveTo(Offset(middle(0), row + 27)).also { press() } }
                mainClock.advanceTimeBy(400)
                var last = pixels()
                repeat(63) {
                    mainClock.advanceTimeByFrame()
                    val now = pixels()
                    if (now != last) n++
                    last = now
                }
            }
            return n
        }
        val held = changes(hold = true)
        val rest = changes(hold = false)
        assertTrue(held >= 11 && rest <= 9, "changes a second: held $held, at rest $rest")
    }

    @Test
    fun `keyboard focus shows on the handle`() =
        runComposeUiTest {
            val focus = FocusRequester()
            slider(reduceMotion = true, modifier = Modifier.width(300.dp).focusRequester(focus))
            val rest = pixels()
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            assertNotEquals(rest, pixels())
        }

    @Test
    fun `with no width it takes the page's 300 dp, and a range of one value takes no input`() =
        runComposeUiTest {
            var value by mutableIntStateOf(5)
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            MutantSlider(3, {}, Modifier.testTag("s"), label = "ТРЕКИНГ")
                            MutantSlider(value, { value = it }, Modifier.testTag("one"), valueRange = 5..5)
                            MutantSlider(0, {}, Modifier.testTag("reversed"), valueRange = 10..-10)
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("s").assertWidthIsEqualTo(300.dp)
            onNodeWithTag("one").assertIsNotEnabled()
            onNodeWithTag("one").performMouseInput {
                moveTo(Offset(150f, row + 27))
                press()
                moveBy(Offset(100f, 0f))
                release()
            }
            mainClock.advanceTimeByFrame()
            assertEquals(5, value)
            onNodeWithTag("reversed").captureToImage()
        }

    @Test
    fun `at font scale 2 the handle widens for its value, and no word of it is cut`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Box {
                            MutantSlider(
                                -10,
                                {},
                                Modifier.width(300.dp).testTag("s"),
                                label = "ТРЕКИНГ",
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val cut =
                onAllNodes(hasGetTextLayoutResult(), useUnmergedTree = true).fetchSemanticsNodes().flatMap { n ->
                    val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                    results
                        // Compose's own width flag compares the box with the paragraph, which is as wide as the
                        // constraint: the text's own width against its box is what says it is cut.
                        .filter {
                            it.didOverflowHeight ||
                                it.multiParagraph.intrinsics.maxIntrinsicWidth > it.size.width + .5f
                        }.map { it.layoutInput.text.text }
                }
            assertEquals(emptyList(), cut)
        }

    /** The first and last column of the handle's amber on the channel's middle row. */
    private fun ComposeUiTest.amberSpan(tag: String): IntRange {
        val px = onNodeWithTag(tag).captureToImage().toPixelMap()
        val amber = MutantPalettes.Efir.amber
        val y = px.height - 33
        val cols =
            (0 until px.width).filter { x ->
                val c = px[x, y]
                abs(c.red - amber.red) < .1f && abs(c.green - amber.green) < .1f && abs(c.blue - amber.blue) < .1f
            }
        return cols.first()..cols.last()
    }

    @Test
    fun `at font scale 2 the handle keeps one width whatever value it shows`() =
        runComposeUiTest {
            // B-168: the handle fitted the value shown, 34 dp at 0, 38 at +9 and 53 at +10, so a finger dragging across
            // them widened the knob under it and regenerated its body. It fits the range's widest value instead.
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme(reduceMotion = true) {
                        Column {
                            for (v in listOf(0, 9, 10)) {
                                MutantSlider(v, {}, Modifier.width(300.dp).testTag("v$v"), label = "T", key = "same")
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val widths = listOf(0, 9, 10).map { amberSpan("v$it").let { r -> r.last - r.first } }
            assertTrue(widths.max() - widths.min() <= 1, "the handle's amber at 0, +9, +10: $widths")
            assertTrue(widths.min() >= 45, "wide enough for +10 with its room: $widths")
        }

    private fun hasGetTextLayoutResult() =
        androidx.compose.ui.test.SemanticsMatcher
            .keyIsDefined(SemanticsActions.GetTextLayoutResult)
}
