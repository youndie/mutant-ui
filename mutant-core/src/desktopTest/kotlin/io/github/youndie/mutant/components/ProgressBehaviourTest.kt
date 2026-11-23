package io.github.youndie.mutant.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.unit.LayoutDirection
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
        colors: io.github.youndie.mutant.tokens.MutantColors = MutantPalettes.Efir,
        steppedMotion: Boolean = false,
        content: @Composable () -> Unit,
    ) = setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MutantTheme(
                colors = colors,
                reduceMotion = reduceMotion,
                steppedMotion = steppedMotion,
            ) { Box { content() } }
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
            onNodeWithTag("over").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        1f,
                        0f..1f,
                    ),
                ),
            )
            assertTrue(acidColumns("nan").isEmpty(), "NaN reads as none")
            onNodeWithText("007%").assertExists()
        }

    @Test
    fun `the track is ringed 2 dp in the page's black, outside it, in both themes`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            show(colors = MutantPalettes.Kopiya) {
                Box(
                    Modifier.testTag("room").padding(4.dp),
                ) { MutantLinearProgress(0f, Modifier.width(100.dp), showValue = false) }
            }
            mainClock.advanceTimeByFrame()
            val px = onNodeWithTag("room").captureToImage().toPixelMap()
            val black = Color(0xFF05060C)
            // Whole rows: opaque, not a half-covered row over the transparent ground.
            val solid = { c: Color -> near(c, black, .02f) && c.alpha > .99f }
            assertTrue(solid(px[50, 2]) && solid(px[50, 3]), "two rows above the track: ${px[50, 2]}, ${px[50, 3]}")
            assertTrue(!near(px[50, 1], black, .02f), "and not a third")
        }

    @Test
    fun `the band runs 12 dp a scene tick, round and round, and stands when paused`() {
        val starts = ArrayList<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            // In the design's steps (`steppedMotion`), read a tick apart; by default it moves on every frame (B-191).
            show(steppedMotion = true) { MutantLinearProgress(Modifier.width(200.dp).testTag("b")) }
            onNodeWithTag(
                "b",
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            )
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
        // The band's quarters over the black: 15 %, 40 % and solid acid, read where each stands whole.
        runComposeUiTest {
            mainClock.autoAdvance = false
            // Paused, it stands centred on the 200 dp track (B-191): from 52 dp to 148, its solid half from 100.
            show(reduceMotion = true) { MutantLinearProgress(Modifier.width(200.dp).testTag("q")) }
            mainClock.advanceTimeByFrame()
            val px = onNodeWithTag("q").captureToImage().toPixelMap()
            val end = acidColumns("q").last()
            assertEquals(147, end, "the band's end, paused centred")
            val black = Color(0xFF05060C)

            fun over(a: Float) =
                Color(
                    acid.red * a + black.red * (1 - a),
                    acid.green * a + black.green * (1 - a),
                    acid.blue * a + black.blue * (1 - a),
                )
            assertTrue(near(px[end - 60, 6], over(.4f), .02f), "the 40 % quarter: ${px[end - 60, 6]}")
            assertTrue(near(px[end - 84, 6], over(.15f), .02f), "the 15 % quarter: ${px[end - 84, 6]}")
        }
    }

    @Test
    fun `the reel turns 30 degrees a tick, whole again after twelve, and stands when paused`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            // In the design's steps (`steppedMotion`); by default it turns on every frame (B-191).
            show(steppedMotion = true) { MutantCircularProgress(Modifier.testTag("r")) }
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
            // The spool's paper at its centre, which no hole reaches: the page's literal, in both themes.
            val centre = onNodeWithTag("r").captureToImage().toPixelMap()[18, 18]
            assertTrue(near(centre, Color(0xFFE9F2E4), .01f), "the spool: $centre")
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

    @Test
    fun `right to left both bars draw their left-to-right picture mirrored`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            show {
                // Apart, so no ring (drawn 2 dp outside its track) reaches into the next bar.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (dir in LayoutDirection.entries) {
                        CompositionLocalProvider(LocalLayoutDirection provides dir) {
                            MutantLinearProgress(.3f, Modifier.width(200.dp).testTag("det$dir"), showValue = false)
                            MutantLinearProgress(Modifier.width(200.dp).testTag("ind$dir"))
                        }
                    }
                }
            }
            // A few ticks in, the band stands wholly inside the track.
            mainClock.advanceTimeBy(1000)
            for (bar in listOf("det", "ind")) {
                val ltr = onNodeWithTag("${bar}Ltr").captureToImage().toPixelMap()
                val rtl = onNodeWithTag("${bar}Rtl").captureToImage().toPixelMap()
                assertTrue((0 until ltr.width).any { near(ltr[it, 6], acid) }, "$bar: some acid to compare")
                for (y in 0 until ltr.height) {
                    for (x in 0 until ltr.width) {
                        assertEquals(ltr[x, y], rtl[ltr.width - 1 - x, y], "$bar at $x, $y")
                    }
                }
            }
        }

    @Test
    fun `with no width to fill, the running band keeps the determinate track's 200 dp`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            // In the design's steps, so the band's edges stand on whole pixels.
            show(steppedMotion = true) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    MutantLinearProgress(Modifier.testTag("b"))
                    MutantCircularProgress()
                }
            }
            mainClock.advanceTimeBy(1000)
            val size = onNodeWithTag("b").fetchSemanticsNode().size
            assertEquals(200, size.width, "the track's width: $size")
            // Drawn across it, not only measured: the band's solid half lies whole inside the track, and its black.
            assertEquals(48, acidColumns("b").size, "the band's solid half")
            val black = Color(0xFF05060C)
            assertTrue(near(onNodeWithTag("b").captureToImage().toPixelMap()[199, 6], black), "the track to its end")
        }

    @Test
    fun `given a box of another size, the reel keeps its 36 dp in the box's middle`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            show {
                Row {
                    MutantCircularProgress(Modifier.size(60.dp).testTag("big"))
                    MutantCircularProgress(Modifier.size(120.dp, 36.dp).testTag("wide"))
                }
            }
            mainClock.advanceTimeByFrame()
            val black = Color(0xFF05060C)
            for ((tag, w, h) in listOf(Triple("big", 60, 60), Triple("wide", 120, 36))) {
                val px = onNodeWithTag(tag).captureToImage().toPixelMap()
                // The ring's outermost black along the middle row and column: 36 + 2 · 2.5 dp across, centred.
                val row = (0 until px.width).filter { near(px[it, h / 2], black) }
                val col = (0 until px.height).filter { near(px[w / 2, it], black) }
                assertTrue(
                    abs(row.first() + row.last() - (w - 1)) <= 1,
                    "$tag across: ${row.first()}..${row.last()} in $w",
                )
                // In a box of its own height the ring stands past the capture: read down only in the taller box.
                if (h > 36) {
                    assertTrue(
                        abs(col.first() + col.last() - (h - 1)) <= 1,
                        "$tag down: ${col.first()}..${col.last()} in $h",
                    )
                }
                assertTrue(row.last() - row.first() in 38..41, "$tag: its own size, ${row.last() - row.first()}")
            }
        }

    @Test
    fun `in a row too narrow for it, the value stays whole past the row's end`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            show {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (w in listOf(120, 20)) {
                        Box(
                            Modifier.width(120.dp).testTag("room$w"),
                        ) { MutantLinearProgress(.42f, Modifier.width(w.dp)) }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val amber = MutantPalettes.Efir.textOsd

            fun ink(tag: String): IntRange {
                val px = onNodeWithTag(tag).captureToImage().toPixelMap()
                val cols = (0 until px.width).filter { x -> (0 until px.height).any { near(px[x, it], amber, .2f) } }
                return cols.first()..cols.last()
            }
            // The value's width, read where its row has room for it.
            val whole =
                onAllNodesWithText("042%", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .first()
                    .size.width
            val narrow = ink("room20")
            assertTrue(narrow.last - narrow.first + 1 >= whole - 4, "the value's ink $narrow, whole $whole px wide")
        }
}
