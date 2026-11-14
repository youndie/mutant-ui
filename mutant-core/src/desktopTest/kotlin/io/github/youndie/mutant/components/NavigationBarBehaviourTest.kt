package io.github.youndie.mutant.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** B-24: the navigation bar chooses, its pill stands and slides, and what a consumer does to it. */
@OptIn(ExperimentalTestApi::class)
class NavigationBarBehaviourTest {
    private val four =
        listOf(
            MutantNavigationItem("Эфир", MutantIcons.Home),
            MutantNavigationItem("Очередь", MutantIcons.Schedule, badge = "3"),
            MutantNavigationItem("Поиск", MutantIcons.Search),
            MutantNavigationItem("Настройка", MutantIcons.Settings),
        )

    /** Labels of many widths, odd and even. */
    private val widths =
        listOf("Ж", "ЖЖ", "Поиск", "Поиск.", "Настройка", "Настройки", "Настройкам", "Очередь", "Очереди")

    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private fun near(
        a: Color,
        b: Color,
        within: Float = .06f,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < within

    /** The pill's lime: its first and last column in row [y] of the bar, or null. */
    private fun ComposeUiTest.pill(y: Int = 26): IntRange? {
        val lime = MutantPalettes.Efir.primary
        val px = onNodeWithTag("n").captureToImage().toPixelMap()
        val xs = (0 until px.width).filter { near(px[it, y], lime) }
        return if (xs.isEmpty()) null else xs.first()..xs.last()
    }

    private fun ComposeUiTest.bar(
        selected: () -> Int,
        reduceMotion: Boolean = false,
    ) = setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MutantTheme(reduceMotion = reduceMotion) {
                Box {
                    MutantNavigationBar(
                        four,
                        selected(),
                        {},
                        Modifier.width(328.dp).testTag("n"),
                        windowInsets = WindowInsets(0),
                    )
                }
            }
        }
    }

    @Test
    fun `a click selects an item, the semantics say so, and a disabled one takes none`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            setContent {
                MutantTheme {
                    MutantNavigationBar(
                        listOf(
                            MutantNavigationItem("Эфир", MutantIcons.Home),
                            MutantNavigationItem("Поиск", MutantIcons.Search),
                            MutantNavigationItem("Настройка", MutantIcons.Settings, enabled = false),
                        ),
                        at,
                        { at = it },
                        windowInsets = WindowInsets(0),
                    )
                }
            }
            onNodeWithText(
                "Эфир",
            ).assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            onNodeWithText("Поиск").assertIsNotSelected().performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(1, at)
            onNodeWithText("Поиск").assertIsSelected()
            onNodeWithText("Настройка").assertIsNotEnabled().performClick()
            assertEquals(1, at)
            // A disabled label greys.
            val grey = MutantPalettes.Efir.textDisabled
            val px = onNodeWithText("Настройка", useUnmergedTree = true).captureToImage().toPixelMap()
            val inked = (0 until px.width * px.height).count { near(px[it % px.width, it / px.width], grey, .04f) }
            assertTrue(inked > 10, "the disabled label in its grey: $inked")
        }

    @Test
    fun `in a horizontally scrolling row each item takes the design's 82 dp cell, and the pill slides one cell`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            MutantNavigationBar(
                                listOf(
                                    MutantNavigationItem("Эфир", MutantIcons.Home),
                                    MutantNavigationItem("Поиск", MutantIcons.Search),
                                ),
                                at,
                                {},
                                Modifier.testTag("n"),
                                windowInsets = WindowInsets(0),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            onNodeWithTag("n").assertWidthIsEqualTo(164.dp)
            val second = onNodeWithText("Поиск").getBoundsInRoot()
            assertEquals(82f, second.left.value, "the second cell starts one design cell in")
            assertEquals(164f, second.right.value, "and ends at the bar's end")
            at = 1
            mainClock.advanceTimeBy(160)
            val mid = pill()!!
            assertTrue((mid.first + mid.last) / 2 in 61..103, "under way from 41 to 123: $mid")
            mainClock.advanceTimeBy(400)
            val end = pill()!!
            assertTrue(abs((end.first + end.last) / 2 - 123) <= 2, "landed in the second cell: $end")
        }

    @Test
    fun `each item is its whole cell, the bar 68 tall over its insets`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Column {
                            MutantNavigationBar(four, 0, {}, Modifier.width(328.dp), windowInsets = WindowInsets(0))
                            MutantNavigationBar(
                                four,
                                0,
                                {},
                                Modifier.width(328.dp).testTag("inset"),
                                windowInsets = WindowInsets(bottom = 24),
                            )
                        }
                    }
                }
            }
            onAllNodesWithText("Поиск").fetchSemanticsNodes().first().let {
                assertEquals(82f, it.boundsInRoot.width, "a quarter of 328")
                assertEquals(68f, it.boundsInRoot.height, "the bar's height")
            }
            onNodeWithTag("inset").assertHeightIsEqualTo(92.dp).assertWidthIsEqualTo(328.dp)
        }

    @Test
    fun `the pill stands under the selected icon, 56 wide in its cell, and none for no selection`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(2)
            bar({ at })
            mainClock.advanceTimeByFrame()
            val p = pill()!!
            // The third cell, 164 to 246: the pill 56 wide centred, give or take its wobbling outline.
            assertTrue(abs((p.first + p.last) / 2 - 205) <= 2 && p.last - p.first in 46..58, "pill $p")
            assertNull(pill(8), "nothing above it")
            at = -1
            mainClock.advanceTimeByFrame()
            assertNull(pill(), "no selection, no pill")
        }

    @Test
    fun `a new selection slides the pill over from the cell it left, at once when paused`() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            bar({ at })
            mainClock.advanceTimeByFrame()
            val start = pill()!!
            at = 3
            mainClock.advanceTimeBy(160)
            val mid = pill()!!
            val endCentre = 287
            val midCentre = (mid.first + mid.last) / 2
            assertTrue(midCentre > (start.first + start.last) / 2 + 20 && midCentre < endCentre - 20, "under way: $mid")
            mainClock.advanceTimeBy(400)
            val end = pill()!!
            assertTrue(abs((end.first + end.last) / 2 - endCentre) <= 2, "landed: $end")
        }
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            bar({ at }, reduceMotion = true)
            mainClock.advanceTimeByFrame()
            at = 3
            mainClock.advanceTimeByFrame()
            val end = pill()!!
            assertTrue(abs((end.first + end.last) / 2 - 287) <= 2, "paused, at once: $end")
        }
    }

    @Test
    fun `an icon centres on its cell whatever its label's width`() {
        // Labels of many widths: an odd one centres a column on a half pixel, which an icon centred on its column inherits.
        val columns =
            widths.map { label ->
                var ink = IntRange.EMPTY
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            MutantTheme {
                                Box {
                                    MutantNavigationBar(
                                        listOf(MutantNavigationItem(label, MutantIcons.Search)),
                                        -1,
                                        {},
                                        Modifier.width(82.dp).testTag("n"),
                                        windowInsets = WindowInsets(0),
                                    )
                                }
                            }
                        }
                    }
                    mainClock.advanceTimeByFrame()
                    val px = onNodeWithTag("n").captureToImage().toPixelMap()
                    val icon = MutantPalettes.Efir.textSecondary
                    val xs = (0 until px.width).filter { x -> (15 until 37).any { y -> near(px[x, y], icon, .1f) } }
                    ink = xs.first()..xs.last()
                }
                ink
            }
        assertEquals(1, columns.toSet().size, "the icon's columns under every label: $columns")
    }

    @Test
    fun `the badge is the amber count over the icon box's corner`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            bar({ 0 })
            mainClock.advanceTimeByFrame()
            val px = onNodeWithTag("n").captureToImage().toPixelMap()
            val amber = MutantPalettes.Efir.amber
            // The second cell, 82 to 164: its icon box 95 to 151, the badge's right 6 dp in, its top 2 dp above.
            val xs = (82 until 164).filter { x -> (0 until 68).any { y -> near(px[x, y], amber, .03f) } }
            val ys = (0 until 68).filter { y -> (82 until 164).any { x -> near(px[x, y], amber, .03f) } }
            assertTrue(xs.isNotEmpty(), "an amber plate")
            assertEquals(144, xs.last(), "its right edge 6 dp in from the icon box's, at 145")
            assertEquals(9, ys.first(), "2 dp above the icon box")
            assertEquals(16, ys.size, "16 dp tall")
            val elsewhere = (0 until 82).any { x -> (0 until 68).any { y -> near(px[x, y], amber, .03f) } }
            assertTrue(!elsewhere, "no badge on the first item")
        }

    @Test
    fun `the badge's ring is the page's black in both themes`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(colors = MutantPalettes.Kopiya) {
                        Box {
                            MutantNavigationBar(
                                four,
                                0,
                                {},
                                Modifier.width(328.dp).testTag("n"),
                                windowInsets = WindowInsets(0),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val px = onNodeWithTag("n").captureToImage().toPixelMap()
            // The plate's top is row 9; its ring's whole row is 8, 1.5 dp out. Read it a few columns in from its right.
            assertTrue(near(px[140, 8], Color(0xFF05060C), .02f), "ring ${px[140, 8]}")
        }

    @Test
    fun `keyboard focus draws the brackets around an item`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            bar({ 0 })
            mainClock.advanceTimeByFrame()
            val rest = pixels("n")
            onNodeWithText("Поиск").requestFocus()
            mainClock.advanceTimeByFrame()
            assertNotEquals(rest, pixels("n"))
            val px = onNodeWithTag("n").captureToImage().toPixelMap()
            assertTrue(
                near(px[164 + 4, 1], MutantPalettes.Efir.focus, .05f),
                "the bracket at the cell's corner: ${px[168, 1]}",
            )
        }

    @Test
    fun `no width, long labels and a doubled font neither throw nor leave the cells`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Настройка звука и изображения"
            val items =
                listOf(MutantNavigationItem(long, MutantIcons.Home), MutantNavigationItem(long, MutantIcons.Search))
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column(Modifier.testTag("all")) {
                            MutantNavigationBar(items, 0, {}, Modifier.width(0.dp), windowInsets = WindowInsets(0))
                            MutantNavigationBar(
                                items,
                                1,
                                {},
                                Modifier.width(200.dp).testTag("n"),
                                windowInsets = WindowInsets(0),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            pixels("all")
            onNodeWithTag("n").assertHeightIsEqualTo(68.dp)
            val bar = onNodeWithTag("n").fetchSemanticsNode().boundsInRoot
            val labels =
                onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes().filter {
                    it.boundsInRoot.top >=
                        bar.top
                }
            assertEquals(2, labels.size)
            for ((i, node) in labels.withIndex()) {
                val b = node.boundsInRoot
                val cell = bar.left + i * 100f
                assertTrue(
                    // 4 dp from the cell's sides, then an ellipsis.
                    b.left >= cell + 4 - .5f && b.right <= cell + 96f + .5f && b.bottom <= bar.bottom,
                    "label $b in its cell",
                )
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                assertEquals(TextOverflow.Ellipsis, layouts.single().layoutInput.overflow)
            }
        }
}
