package io.github.youndie.mutant.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-31: what the segmented button does, and what a consumer does to it. Density 1: a dp is a pixel. */
@OptIn(ExperimentalTestApi::class)
class SegmentedBehaviourTest {
    private val options = listOf("SP · 2 ч", "LP · 4 ч", "EP · 6 ч")

    private fun ComposeUiTest.segmented(
        reduceMotion: Boolean = false,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
    ): () -> Int {
        var chosen by mutableIntStateOf(0)
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    Box {
                        MutantSegmentedButton(
                            options,
                            chosen,
                            { chosen = it },
                            modifier.testTag("s"),
                            enabled = enabled,
                        )
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        return { chosen }
    }

    private val lime = MutantPalettes.Efir.primary

    /** How much of a segment's window row is the window's lime: its middle band, the label's rows left out. */
    private fun ComposeUiTest.limeIn(segment: Int): Float =
        onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
            val x0 = 4 + segment * 104 + 8
            val xs = x0 until x0 + 84
            val ys = (8 until 14) + (36 until 42)
            val all = xs.flatMap { x -> ys.map { y -> px[x, y] } }
            all.count { it.near(lime) }.toFloat() / all.size
        }

    private fun Color.near(c: Color) =
        kotlin.math.abs(red - c.red) < .08f && kotlin.math.abs(green - c.green) < .08f &&
            kotlin.math.abs(blue - c.blue) < .08f

    @Test
    fun `a click chooses the segment, and a reader sees which is chosen`() =
        runComposeUiTest {
            val chosen = segmented()
            onNodeWithText("LP · 4 ч").performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(1, chosen())
            onNodeWithText("LP · 4 ч").assertIsSelected()
        }

    @Test
    fun `the window flows to the new segment by venom and stands there after 720 ms`() =
        runComposeUiTest {
            segmented()
            assertTrue(limeIn(0) > .8f && limeIn(2) < .05f, "at rest: ${limeIn(0)} / ${limeIn(2)}")
            onNodeWithText("EP · 6 ч").performClick()
            mainClock.advanceTimeBy(48)
            // The tentacles have shot, the mass is still where it was: the new window is not shown yet.
            val early = limeIn(0) to limeIn(2)
            mainClock.advanceTimeBy(800)
            val late = limeIn(0) to limeIn(2)
            assertTrue(early.first > .5f && early.second < .5f, "48 ms in: old ${early.first}, new ${early.second}")
            assertTrue(late.first < .05f && late.second > .8f, "800 ms in: old ${late.first}, new ${late.second}")
        }

    @Test
    fun `the labels change colour 340 ms in`() =
        runComposeUiTest {
            segmented()
            val text = MutantPalettes.Efir.text

            // The third label's glyphs in the text's own colour: lit before the switch, none after it.
            fun lit(): Int =
                onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
                    (232 until 296).sumOf { x -> (18 until 30).count { y -> px[x, y].near(text) } }
                }
            val rest = lit()
            onNodeWithText("EP · 6 ч").performClick()
            mainClock.advanceTimeBy(240)
            val before = lit()
            mainClock.advanceTimeBy(160)
            val after = lit()
            assertTrue(rest > 20 && before > 20 && after == 0, "lit at rest $rest, at 240 ms $before, at 400 ms $after")
        }

    @Test
    fun `the venom steps at 24 frames a second`() =
        runComposeUiTest {
            segmented()
            onNodeWithText("EP · 6 ч").performClick()
            // The picture's changes over the run's 720 ms in 16 ms frames: a step every 41.67 ms is 17 of them.
            var changes = 0
            var last =
                onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
                    List(px.width * px.height) {
                        px[
                            it %
                                px.width, it / px.width,
                        ]
                    }
                }
            repeat(45) {
                mainClock.advanceTimeByFrame()
                val now =
                    onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
                        List(px.width * px.height) {
                            px[
                                it %
                                    px.width, it / px.width,
                            ]
                        }
                    }
                if (now != last) changes++
                last = now
            }
            assertTrue(changes >= 15, "changes over the run: $changes")
        }

    @Test
    fun `under reduce motion the window moves at once`() =
        runComposeUiTest {
            segmented(reduceMotion = true)
            onNodeWithText("EP · 6 ч").performClick()
            mainClock.advanceTimeByFrame()
            assertTrue(limeIn(2) > .8f && limeIn(0) < .05f, "at once: old ${limeIn(0)}, new ${limeIn(2)}")
        }

    @Test
    fun `the arrows choose, and the button shows keyboard focus`() =
        runComposeUiTest {
            val focus = FocusRequester()
            val chosen = segmented(reduceMotion = true, modifier = Modifier.focusRequester(focus))
            val rest =
                onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
                    List(px.width * px.height) {
                        px[
                            it %
                                px.width, it / px.width,
                        ]
                    }
                }
            runOnIdle { focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            val ring =
                onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
                    List(px.width * px.height) {
                        px[
                            it %
                                px.width, it / px.width,
                        ]
                    }
                }
            assertNotEquals(rest, ring, "focused")
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionRight) }
            assertEquals(1, chosen())
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionRight) }
            assertEquals(2, chosen())
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionLeft) }
            assertEquals(1, chosen())
        }

    @Test
    fun `disabled, a click chooses nothing`() =
        runComposeUiTest {
            val chosen = segmented(enabled = false)
            onNodeWithText("EP · 6 ч").performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(0, chosen())
        }

    @Test
    fun `with no width a segment is 104 dp, and at font scale 2 no label is cut`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            MutantSegmentedButton(options, 1, {}, Modifier.testTag("s"))
                            MutantSegmentedButton(emptyList(), -1, {}, Modifier.testTag("none"))
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("s").assertWidthIsEqualTo(312.dp)
            val cut =
                onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .flatMap { n ->
                        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                        n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                        results.filter { it.didOverflowHeight }.map { it.layoutInput.text.text }
                    }
            assertEquals(emptyList(), cut)
            onNodeWithTag("none").captureToImage()
        }
}
