package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** B-23: the tabs choose, their bar runs and lands, and the bar keeps its row; what a consumer does to them. */
@OptIn(ExperimentalTestApi::class)
class BarTabsBehaviourTest {
    private val three = listOf(MutantTab("Дорожки"), MutantTab("Заметки"), MutantTab("Копии"))

    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    /** The acid's first and last column in row [y] of the tabs, or null; [from] to read only from a column on. */
    private fun ComposeUiTest.acid(
        y: Int,
        from: Int = 0,
    ): IntRange? {
        val acid = MutantPalettes.Efir.primary
        val px = onNodeWithTag("t").captureToImage().toPixelMap()
        val xs =
            (from until px.width).filter { x ->
                val p = px[x, y]
                abs(p.red - acid.red) + abs(p.green - acid.green) + abs(p.blue - acid.blue) < .05f
            }
        return if (xs.isEmpty()) null else xs.first()..xs.last()
    }

    /** Whether any pixel of row [y] left of column [x] is lit by the acid's echo (the acid at any opacity). */
    private fun ComposeUiTest.echoLeftOf(
        x: Int,
        y: Int,
    ): Boolean {
        val px = onNodeWithTag("t").captureToImage().toPixelMap()
        val ground = px[0, 0]
        return (0 until x).any { px[it, y].green - ground.green > .05f }
    }

    private fun ComposeUiTest.tabs(
        selected: () -> Int,
        reduceMotion: Boolean = false,
        onSelect: (Int) -> Unit = {},
    ) = setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MutantTheme(reduceMotion = reduceMotion) {
                Box { MutantTabs(three, selected(), onSelect, Modifier.testTag("t")) }
            }
        }
    }

    @Test
    fun `a click selects a tab, the semantics say so, and a disabled tab takes none`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            setContent {
                MutantTheme {
                    MutantTabs(
                        listOf(MutantTab("Дорожки"), MutantTab("Заметки"), MutantTab("Копии", enabled = false)),
                        at,
                        { at = it },
                    )
                }
            }
            onNodeWithText(
                "Дорожки",
            ).assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            onNodeWithText("Заметки").assertIsNotSelected().performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(1, at)
            onNodeWithText("Заметки").assertIsSelected()
            onNodeWithText("Копии").assertIsNotEnabled().performClick()
            assertEquals(1, at)
            // A disabled label greys; a live one stands in the secondary colour, the selected in the accent, which it takes
            // 280 ms after the click (B-187).
            mainClock.advanceTimeBy(320)
            val c = MutantPalettes.Efir
            for ((label, colour) in listOf(
                "Копии" to c.textDisabled,
                "Дорожки" to c.textSecondary,
                "Заметки" to c.textAccent,
            )) {
                val px = onNodeWithText(label, useUnmergedTree = true).captureToImage().toPixelMap()
                val inked =
                    (0 until px.width * px.height).count {
                        val q = px[it % px.width, it / px.width]
                        abs(q.red - colour.red) + abs(q.green - colour.green) + abs(q.blue - colour.blue) < .04f
                    }
                assertTrue(inked > 10, "$label in its colour: $inked")
            }
        }

    @Test
    fun `each tab is a target 48 tall and at least 48 wide, reaching 12 dp past its label`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme { MutantTabs(listOf(MutantTab("Да"), MutantTab("Дорожки")), 0, {}) }
                }
            }
            for (label in listOf("Да", "Дорожки")) {
                onNodeWithText(label).assertHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
            }
            val tab = onNodeWithText("Дорожки").fetchSemanticsNode().boundsInRoot
            val text = onNodeWithText("Дорожки", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(12f, text.left - tab.left, "from the target's left")
            assertEquals(12f, tab.right - text.right, "to the target's right")
        }

    @Test
    fun `the bar stands 43 dp down under the selected label, as wide as it, and none for no selection`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(1)
            tabs({ at })
            mainClock.advanceTimeByFrame()
            val label = onNodeWithText("Заметки", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val bar = acid(43)
            assertEquals(label.left.toInt()..(label.right.toInt() - 1), bar, "under the label, as wide")
            assertEquals(bar, acid(46), "4 dp tall")
            assertNull(acid(42), "nothing above it")
            assertNull(acid(47), "nothing under it")
            at = -1
            mainClock.advanceTimeByFrame()
            assertNull(acid(43), "no selection, no bar")
        }

    @Test
    fun `a new selection runs the bar across in 330 ms with its echo trailing, and lands at once when paused`() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            tabs({ at })
            mainClock.advanceTimeByFrame()
            val start = acid(43)!!
            val end = onNodeWithText("Копии", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue(!echoLeftOf(start.first, 45), "at rest the echo is to the right")
            at = 2
            mainClock.advanceTimeBy(160)
            val mid = acid(43)!!
            // 160 ms on, the run is 144 ms of 330 in: it starts with the frame that composes the new selection, one frame
            // after it was made. On every frame (B-100), eased out by the cube, 1 - (1 - p)^3, from the first label to the third.
            val p =
                io.github.youndie.mutant.surface
                    .stepped(160.0 - 16, 330.0, steps = false)
            val e = 1 - Math.pow(1 - p, 3.0)
            val expected = start.first + (end.left - start.first) * e
            assertTrue(abs(mid.first - expected) <= 1.0, "at 160 ms the bar at ${mid.first}, eased to $expected")
            // Two columns clear of the bar's own edge, which stands at a fraction mid-run.
            assertTrue(echoLeftOf(mid.first - 2, 45), "running right, the echo trails on the left")
            mainClock.advanceTimeBy(200)
            assertEquals(end.left.toInt(), acid(43)!!.first, "landed")
            assertTrue(!echoLeftOf(end.left.toInt(), 45), "landed, the echo back to the right")
        }
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            tabs({ at }, reduceMotion = true)
            mainClock.advanceTimeByFrame()
            at = 2
            mainClock.advanceTimeByFrame()
            val end = onNodeWithText("Копии", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(end.left.toInt(), acid(43)!!.first, "paused: at once")
        }
    }

    @Test
    fun `the bar's ring is the page's black in both themes`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(
                        colors = MutantPalettes.Kopiya,
                    ) { Box { MutantTabs(three, 0, {}, Modifier.testTag("t")) } }
                }
            }
            mainClock.advanceTimeByFrame()
            val bar = acid(44)!!
            // The ring's whole row 1.5 dp above the bar: row 41 of 43 is half covered, row 42 whole.
            val ring = onNodeWithTag("t").captureToImage().toPixelMap()[bar.first + 10, 42]
            val black = Color(0xFF05060C)
            assertTrue(
                abs(ring.red - black.red) + abs(ring.green - black.green) + abs(ring.blue - black.blue) < .02f,
                "$ring",
            )
        }

    @Test
    fun `keyboard focus draws the brackets around a tab`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box {
                            MutantTabs(
                                listOf(MutantTab("Дорожки"), MutantTab("Заметки")),
                                -1,
                                {},
                                Modifier.testTag("t"),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val rest = pixels("t")
            onNodeWithText("Заметки").requestFocus()
            mainClock.advanceTimeByFrame()
            val focused = pixels("t")
            assertNotEquals(rest, focused, "focused under keyboard input")
            // The brackets are the focus colour, at the target's corner.
            val px = onNodeWithTag("t").captureToImage().toPixelMap()
            val tab = onNodeWithText("Заметки").fetchSemanticsNode().boundsInRoot
            val corner = px[tab.left.toInt() + 4, 1]
            val f = MutantPalettes.Efir.focus
            assertTrue(
                abs(corner.red - f.red) + abs(corner.green - f.green) + abs(corner.blue - f.blue) < .05f,
                "$corner",
            )
        }

    @Test
    fun `the bar stands under its insets, its row grown at font scale 2, the long title cut, the status kept`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Эфир с начала первой стороны без подписи и до конца второй"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column(Modifier.testTag("all")) {
                            MutantTopAppBar("Эфир", Modifier.testTag("bar"), windowInsets = WindowInsets(top = 24))
                            MutantTopAppBar(
                                long,
                                Modifier.width(240.dp).testTag("long"),
                                status = "CH 03",
                                windowInsets = WindowInsets(0),
                            )
                            MutantTopAppBar(
                                long,
                                Modifier.width(0.dp),
                                status = "CH 03",
                                windowInsets = WindowInsets(0),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            // Handjet 26 at font scale 2 lays a 58 dp line: the 56 dp row grows to it, and the divider's 2 under (B-61).
            onNodeWithTag("bar").assertHeightIsEqualTo(84.dp)
            onNodeWithTag("long").assertIsDisplayed().assertHeightIsEqualTo(60.dp)
            pixels("all")
            val bar = onNodeWithTag("long").fetchSemanticsNode().boundsInRoot
            val status = onAllNodesWithText("CH 03", useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
            assertTrue(status.right <= bar.right + .5f && status.width > 0, "the status kept whole: $status in $bar")
            val title = onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
            assertTrue(title.right <= status.left, "the title cut before the status: $title")
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            onAllNodesWithText(long, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .first()
                .config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult]
                .action
                ?.invoke(layouts)
            assertEquals(
                androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                layouts.single().layoutInput.overflow,
                "with an ellipsis",
            )
            // The divider: the two rows under the row, grown to 58 dp at font scale 2, carry the tracking strokes.
            val px = onNodeWithTag("bar").captureToImage().toPixelMap()
            val ground = px[px.width - 1, 0]
            assertTrue((0 until px.width).any { px[it, 82] != ground }, "the divider drawn under the row")
        }

    @Test
    fun `tabs given no width, long labels and a doubled font neither throw nor leave their targets`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Дорожки первой стороны без подписи"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column(Modifier.testTag("all")) {
                            MutantTabs(listOf(MutantTab(long), MutantTab(long)), 0, {}, Modifier.width(0.dp))
                            MutantTabs(
                                listOf(MutantTab(long), MutantTab(long)),
                                1,
                                {},
                                Modifier.width(200.dp).testTag("t"),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            pixels("all")
            onNodeWithTag("t").assertHeightIsEqualTo(48.dp)
            val row = onNodeWithTag("t").fetchSemanticsNode().boundsInRoot
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                if (b.top < row.top) continue
                assertTrue(b.right <= row.right + .5f && b.bottom <= row.bottom, "label $b inside $row")
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                assertEquals(
                    androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    layouts.single().layoutInput.overflow,
                )
            }
        }

    @Test
    fun `at font scale 3 the tabs grow with their labels and the bar stands under the selected one, not on it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 3f)) {
                    MutantTheme(reduceMotion = true) {
                        Box {
                            MutantTabs(
                                listOf(MutantTab("Дорожки"), MutantTab("Заметки")),
                                0,
                                {},
                                Modifier.testTag("tabs"),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val label =
                onAllNodesWithText("Дорожки", useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
            val px = onNodeWithTag("tabs").captureToImage().toPixelMap()
            val lime = io.github.youndie.mutant.tokens.MutantPalettes.Efir.primary

            // The bar's top row: the first with an unbroken run of the acid lime most of the label wide. The selected
            // label is lime too, but its glyphs break every row they cross.
            fun lime(
                x: Int,
                y: Int,
            ) = px[x, y].let {
                kotlin.math.abs(it.red - lime.red) < .05f &&
                    kotlin.math.abs(it.green - lime.green) < .05f
            }
            val barTop =
                (0 until px.height).first { y ->
                    var run = 0
                    var best = 0
                    for (x in 0 until px.width) {
                        run = if (lime(x, y)) run + 1 else 0
                        best = maxOf(best, run)
                    }
                    best > label.width * .8f
                }
            assertTrue(px.height > 48, "the row grew: ${px.height}")
            assertTrue(barTop >= label.bottom, "the bar at $barTop, the label down to ${label.bottom}")
        }

    @Test
    fun `a click during a run sends the bar on from where it stands, not from the tab it was running to`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            tabs({ at }, onSelect = { at = it })
            mainClock.advanceTimeByFrame()
            onNodeWithText("Заметки").performMouseInput { click(center) }
            mainClock.advanceTimeBy(48)
            // Mid-run between the first label and the second, the second clicked away to the third (B-168). The press
            // and the release each move the test's clock, so each edge is read with its time.
            val xs = mutableListOf(mainClock.currentTime to acid(43)!!.first)
            onNodeWithText("Копии").performMouseInput {
                moveTo(center)
                press()
            }
            xs += mainClock.currentTime to acid(43)!!.first
            onNodeWithText("Копии").performMouseInput { release() }
            xs += mainClock.currentTime to acid(43)!!.first
            repeat(30) {
                mainClock.advanceTimeByFrame()
                xs += mainClock.currentTime to acid(43)!!.first
            }
            assertEquals(2, at, "the third selected")
            // A run's fastest frame is its first, 1 - (1 - 16 / 330)^3 = 14 % of the way: under 20 px a frame here. The
            // bar leapt 65 px in one frame to the second label.
            val steps = xs.zipWithNext { (t0, a), (t1, b) -> (b - a) to ((t1 - t0) / 16).coerceAtLeast(1) }
            assertTrue(steps.all { (d, frames) -> d in 0..20 * frames }, "the bar's left edge, (ms, px): $xs")
            val end = onNodeWithText("Копии", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(end.left.toInt(), xs.last().second, "landed under the third label")
        }

    @Test
    fun `the last tab's echo is drawn whole past the row, and a row that scrolls still clips its bar`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var many by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        Box(Modifier.width(400.dp).testTag("all")) {
                            if (many) {
                                MutantTabs(List(12) { MutantTab("Вкладка $it") }, 2, {}, Modifier.width(200.dp))
                            } else {
                                MutantTabs(three, 2, {})
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val acid = MutantPalettes.Efir.primary
            val label = onNodeWithText("Копии", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val px = onNodeWithTag("all").captureToImage().toPixelMap()
            val ground = px[px.width - 1, 0]

            fun Color.fromAcid() = abs(red - acid.red) + abs(green - acid.green) + abs(blue - acid.blue)
            // Row 45: the echo's strokes, 10, 8 and 6 dp, start 4 dp past the bar, past its 1.5 dp ring; the row ends
            // 12 dp past the label.
            val lit =
                (label.right.toInt() + 2 until px.width).count { x ->
                    px[x, 45].fromAcid() < ground.fromAcid() - .05f
                }
            assertEquals(24, lit, "the echo's three strokes past the last label")
            many = true
            mainClock.advanceTimeByFrame()
            val px2 = onNodeWithTag("all").captureToImage().toPixelMap()
            val outside = (200 until px2.width).count { x -> px2[x, 44] != px2[px2.width - 1, 0] }
            assertEquals(0, outside, "a row that scrolls draws nothing of its bar past its width")
            // Scrolled to bring the third tab into view, the bar stands under its label where the scroll put it.
            val third = onNodeWithText("Вкладка 2", useUnmergedTree = true).fetchSemanticsNode()
            val bar = (0 until 200).first { x -> px2[x, 44].fromAcid() < .05f }
            assertEquals(third.positionInRoot.x.toInt(), bar, "the bar under the scrolled label")
        }
}
