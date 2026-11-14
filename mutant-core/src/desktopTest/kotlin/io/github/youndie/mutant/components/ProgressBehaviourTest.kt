package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

/** B-27: the progress's value and segments, the band's and the reel's steps, the pause, and what a consumer gives. */
@OptIn(ExperimentalTestApi::class)
class ProgressBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private val acid = MutantPalettes.Efir.primary

    private fun near(
        a: Color,
        b: Color,
        within: Float = .05f,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < within

    /** The columns of row 6 of [tag] in the acid, solid. */
    private fun ComposeUiTest.acidColumns(tag: String): List<Int> {
        val px = onNodeWithTag(tag).captureToImage().toPixelMap()
        return (0 until px.width).filter { near(px[it, px.height / 2], acid) }
    }

    private fun ComposeUiTest.show(
        reduceMotion: Boolean = false,
        content: @Composable () -> Unit,
    ) = setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MutantTheme(reduceMotion = reduceMotion) { Box { content() } }
        }
    }

    @Test
    fun `a determinate one reads as its value, fills its share in 6 dp segments 2 apart, and prints it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            show {
                Column {
                    MutantLinearProgress(.5f, Modifier.width(200.dp).testTag("half"), showValue = false)
                    MutantLinearProgress(1.5f, Modifier.width(200.dp).testTag("over"), showValue = false)
                    MutantLinearProgress(Float.NaN, Modifier.width(200.dp).testTag("nan"), showValue = false)
                    MutantLinearProgress(.07f, Modifier.width(328.dp).testTag("value"))
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("half").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        .5f,
                        0f..1f,
                    ),
                ),
            )
            val half = acidColumns("half")
            assertEquals(0, half.first())
            assertTrue(half.last() in 96..99, "half of 200: ${half.last()}")
            // 6 on, 2 off: columns 0–5 acid, 6–7 black, 8–13 acid.
            assertEquals((0..5).toList() + (8..13).toList(), half.filter { it < 14 })
            assertTrue(acidColumns("over").last() >= 197, "clamped to the whole")
            assertTrue(acidColumns("nan").isEmpty(), "NaN reads as none")
            onNodeWithText("007%").assertExists()
        }

    @Test
    fun `the track is ringed 2 dp in the page's black, outside it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            show {
                Box(
                    Modifier.testTag("room").padding(4.dp),
                ) { MutantLinearProgress(0f, Modifier.width(100.dp), showValue = false) }
            }
            mainClock.advanceTimeByFrame()
            val px = onNodeWithTag("room").captureToImage().toPixelMap()
            val black = Color(0xFF05060C)
            assertTrue(near(px[50, 2], black, .02f) && near(px[50, 3], black, .02f), "two rows above the track")
            assertTrue(!near(px[50, 1], black, .02f), "and not a third")
        }

    @Test
    fun `the band runs 12 dp a scene tick, round and round, and stands when paused`() {
        val starts = ArrayList<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            show { MutantLinearProgress(Modifier.width(200.dp).testTag("b")) }
            mainClock.advanceTimeByFrame()
            repeat(30) {
                // The band's end: its solid half's last column, while it stands inside the track.
                starts += acidColumns("b").lastOrNull()?.takeIf { it < 199 } ?: -1
                mainClock.advanceTimeBy(125)
            }
        }
        // Between ticks where the end is inside the track, it has moved 12 dp; past the end it comes round.
        val steps = starts.zipWithNext { a, b -> if (a >= 0 && b >= 0 && b > a) b - a else null }.filterNotNull()
        assertTrue(steps.size >= 10 && steps.all { it == 12 }, "12 dp a tick: $starts")
        val seen = starts.filter { it >= 0 }
        assertTrue(seen.zipWithNext().any { (a, b) -> b < a }, "round again: $starts")
        var paused = emptyList<Color>()
        var later = emptyList<Color>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            show(reduceMotion = true) { MutantLinearProgress(Modifier.width(200.dp).testTag("b")) }
            mainClock.advanceTimeByFrame()
            paused = pixels("b")
            mainClock.advanceTimeBy(1000)
            later = pixels("b")
        }
        assertEquals(paused, later, "paused")
    }

    @Test
    fun `the reel turns 30 degrees a tick, whole again after twelve, and stands when paused`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            show { MutantCircularProgress(Modifier.testTag("r")) }
            onNodeWithTag(
                "r",
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
            mainClock.advanceTimeByFrame()
            val first = pixels("r")
            mainClock.advanceTimeBy(125)
            assertNotEquals(first, pixels("r"), "turned a step")
            // Its holes are not quite 120° apart: the picture comes back after twelve ticks, a whole turn.
            mainClock.advanceTimeBy(125 * 11)
            assertEquals(first, pixels("r"), "a whole turn on, the same picture")
        }

    @Test
    fun `no width and a doubled font neither throw nor push the value out`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column(Modifier.testTag("all")) {
                            MutantLinearProgress(.4f, Modifier.width(0.dp))
                            MutantLinearProgress(Modifier.width(0.dp))
                            MutantLinearProgress(.4f, Modifier.width(120.dp).testTag("p"))
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            pixels("all")
            val row = onNodeWithTag("p").fetchSemanticsNode().boundsInRoot
            val value = onAllNodesWithText("040%", useUnmergedTree = true).fetchSemanticsNodes().last().boundsInRoot
            assertTrue(value.right <= row.right + .5f && value.width > 0, "the value whole: $value in $row")
        }
}
