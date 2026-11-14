package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-25: the list row's layout, click, press and pause; the divider across any density; the badge's two sizes. */
@OptIn(ExperimentalTestApi::class)
class ListItemBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private fun near(
        a: Color,
        b: Color,
        within: Float = .04f,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < within

    private fun ComposeUiTest.row(
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        reduceMotion: Boolean = false,
    ) = setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MutantTheme(reduceMotion = reduceMotion) {
                Box {
                    MutantListItem(
                        "Настроечная таблица",
                        Modifier.width(328.dp).testTag("r"),
                        supporting = "Копия 4-го поколения",
                        leadingIcon = MutantIcons.Home,
                        trailingText = "12:04",
                        onClick = onClick,
                        enabled = enabled,
                    )
                }
            }
        }
    }

    @Test
    fun `the row is 64 tall, the tile at its start, the words 12 dp past it, the time at its end`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            row()
            onNodeWithTag("r").assertHeightIsEqualTo(64.dp)
            val r = onNodeWithTag("r").fetchSemanticsNode().boundsInRoot
            val headline =
                onNodeWithText(
                    "Настроечная таблица",
                    useUnmergedTree = true,
                ).fetchSemanticsNode().boundsInRoot
            val supporting =
                onNodeWithText(
                    "Копия 4-го поколения",
                    useUnmergedTree = true,
                ).fetchSemanticsNode().boundsInRoot
            val time = onNodeWithText("12:04", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(52f, headline.left - r.left, "past the 40 dp tile and the 12 dp gap")
            assertEquals(headline.left, supporting.left)
            assertEquals(2f, supporting.top - headline.bottom, "the lines 2 dp apart")
            assertEquals(r.right, time.right, "the time at the row's end")
            assertEquals(r.height / 2, (headline.top + supporting.bottom) / 2 - r.top, .5f, "the words centred")
        }

    @Test
    fun `a row with onClick fires on release, none without it, none disabled - and disabled words grey`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks by mutableIntStateOf(0)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Column {
                            MutantListItem("Без действия", Modifier.testTag("still"))
                            MutantListItem("Действие", Modifier.testTag("r"), onClick = { clicks++ })
                            MutantListItem("Выключено", Modifier.testTag("off"), supporting = "Нет сигнала", onClick = {
                                clicks++
                            }, enabled = false)
                        }
                    }
                }
            }
            onNodeWithTag("still").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            onNodeWithTag("r").performTouchInput { down(center) }
            waitForIdle()
            assertEquals(0, clicks, "pressed, not released")
            onNodeWithTag("r").performTouchInput { up() }
            waitForIdle()
            assertEquals(1, clicks, "released")
            onNodeWithTag("off").assertIsNotEnabled().performClick()
            assertEquals(1, clicks)
            val grey = MutantPalettes.Efir.textDisabled
            for (word in listOf("Выключено", "Нет сигнала")) {
                val px = onNodeWithText(word, useUnmergedTree = true).captureToImage().toPixelMap()
                assertTrue(
                    (0 until px.width * px.height).count { near(px[it % px.width, it / px.width], grey) } > 10,
                    "$word greys",
                )
            }
        }

    @Test
    fun `a press boils the tile, and keyboard focus shows on the row`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            row(onClick = {})
            mainClock.advanceTimeByFrame()
            val rest = pixels("r")
            onNodeWithTag("r").performTouchInput { down(center) }
            mainClock.advanceTimeBy(320)
            val pressed = pixels("r")
            // The words do not move; the tile, the first 40 dp, boils. (A press focuses the row, and the test window's
            // input is the keyboard's, so the brackets show in the corners: rows 12 to 52 lie between them.)
            val changed = rest.indices.filter { rest[it] != pressed[it] && it / 328 in 12 until 52 }
            assertTrue(
                changed.isNotEmpty() &&
                    changed.all {
                        it % 328 < 48
                    },
                "only the tile changes: ${changed.filter { it % 328 >= 48 }.size}",
            )
            onNodeWithTag("r").performTouchInput { up() }
            mainClock.advanceTimeBy(600)
            onNodeWithTag("r").requestFocus()
            mainClock.advanceTimeByFrame()
            // Before any press or focus, the corner was the ground; focused under keyboard input, it is the bracket.
            assertTrue(
                !near(rest[4 + 1 * 328], MutantPalettes.Efir.focus, .05f),
                "no bracket at rest: ${rest[4 + 328]}",
            )
            val px = onNodeWithTag("r").captureToImage().toPixelMap()
            assertTrue(near(px[4, 1], MutantPalettes.Efir.focus, .05f), "the bracket at the row's corner: ${px[4, 1]}")
        }

    @Test
    fun `a press boils the tile - two tiles of one key at one moment, one pressed, differ`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Column {
                            MutantListItem(
                                "А",
                                Modifier.width(328.dp).testTag("a"),
                                leadingIcon = MutantIcons.Home,
                                onClick = {},
                                key = "same",
                            )
                            MutantListItem(
                                "А",
                                Modifier.width(328.dp).testTag("b"),
                                leadingIcon = MutantIcons.Home,
                                onClick = {},
                                key = "same",
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            fun tile(tag: String) =
                onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
                    (12 until 52).flatMap { y -> (0 until 40).map { x -> px[x, y] } }
                }
            assertEquals(tile("a"), tile("b"), "one key, one moment: one tile")
            onNodeWithTag("a").performTouchInput { down(center) }
            mainClock.advanceTimeBy(320)
            assertNotEquals(tile("a"), tile("b"), "the pressed one boils")
        }

    @Test
    fun `the tile keeps clean what the engine keeps clean`() {
        // The page's tile: its 22 dp icon at 9, 9 of the 40 dp tile, with 6 dp across and 2 down (`tz` in the manifest).
        val zone =
            (
                io.github.youndie.mutant.DesignParity
                    .reference(
                        "MutantListItem_Time_Efir",
                    )["slot"]!!
                    .jsonObject["tz"] as kotlinx.serialization.json.JsonArray
            ).map { it.jsonPrimitive.double }
        var tile: io.github.youndie.mutant.gen.Instance? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        val icon = MutantIcons.Home
                        val keep = { it: io.github.youndie.mutant.gen.Instance -> tile = it }
                        ListItemBody("А", Modifier, null, icon, null, null, null, true, "tile1", null, probe = keep)
                    }
                }
            }
            mainClock.advanceTimeByFrame()
        }
        assertEquals(zone, tile!!.slot.textZone.toList(), "kit against the engine")
    }

    @Test
    fun `a disabled row's tile loses its signal`() {
        val tiles =
            listOf(true, false).map { enabled ->
                var tile = emptyList<Color>()
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    row(enabled = enabled)
                    mainClock.advanceTimeByFrame()
                    val px = onNodeWithTag("r").captureToImage().toPixelMap()
                    tile = (12 until 52).flatMap { y -> (0 until 40).map { x -> px[x, y] } }
                }
                tile
            }
        val fill = MutantPalettes.Efir.surfaceVariant
        assertTrue(tiles[0].count { near(it, fill, .03f) } > 200, "the live tile's fill")
        assertTrue(tiles[1].count { near(it, fill, .03f) } < 50, "the disabled tile is no longer that fill")
    }

    private fun frames(reduceMotion: Boolean): Boolean {
        var changed = false
        runComposeUiTest {
            mainClock.autoAdvance = false
            row(reduceMotion = reduceMotion)
            mainClock.advanceTimeByFrame()
            val a = pixels("r")
            repeat(2000 / 16) { mainClock.advanceTimeByFrame() }
            changed = a != pixels("r")
        }
        return changed
    }

    @Test
    fun `reduce motion is the pause - the tile stands, and boils on while the clock runs`() {
        assertTrue(frames(reduceMotion = false), "running")
        assertTrue(!frames(reduceMotion = true), "paused")
    }

    @Test
    fun `no width, a long headline and a doubled font neither throw nor leave the row`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Настроечная таблица с начала первой стороны и до конца второй"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column(Modifier.testTag("all")) {
                            MutantListItem(
                                long,
                                Modifier.width(0.dp),
                                supporting = long,
                                leadingIcon = MutantIcons.Home,
                                trailingText = "12:04",
                            )
                            MutantListItem(
                                long,
                                Modifier.width(240.dp).testTag("r"),
                                supporting = long,
                                leadingIcon = MutantIcons.Home,
                                trailing = { MutantBadge("НОВОЕ") },
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            pixels("all")
            val r = onNodeWithTag("r").fetchSemanticsNode().boundsInRoot
            assertTrue(r.height > 64f, "two doubled lines grow the row: ${r.height}")
            val badge = onAllNodesWithText("НОВОЕ", useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
            assertTrue(badge.right <= r.right + .5f && badge.width > 0, "the badge kept whole: $badge")
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                if (b.top < r.top) continue
                assertTrue(b.right <= badge.left && b.bottom <= r.bottom, "words $b before the badge, inside $r")
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                assertEquals(TextOverflow.Ellipsis, layouts.single().layoutInput.overflow)
            }
        }

    @Test
    fun `the divider spans its width at any density, its strokes seeded by its key`() {
        for (d in listOf(1f, 2f)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(d)) {
                        MutantTheme {
                            Column {
                                MutantDivider(Modifier.width(200.dp).testTag("a"), key = "0")
                                MutantDivider(Modifier.width(200.dp).testTag("b"), key = "1")
                            }
                        }
                    }
                }
                mainClock.advanceTimeByFrame()
                val px = onNodeWithTag("a").captureToImage().toPixelMap()
                assertEquals((2 * d).toInt(), px.height, "2 dp tall at $d")
                val blue = MutantPalettes.Efir.noSignal
                val xs = (0 until px.width).filter { x -> (0 until px.height).any { y -> near(px[x, y], blue, .06f) } }
                assertTrue(
                    xs.last() > px.width * .85f,
                    "strokes reach the far end at density $d: ${xs.last()} of ${px.width}",
                )
                assertNotEquals(pixels("a"), pixels("b"), "another key, other strokes")
            }
        }
    }

    @Test
    fun `the badge is 20 dp tall and 6 in, compact 16 and 5, ringed in the page's black in both themes`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(colors = MutantPalettes.Kopiya) {
                        Column {
                            Box(Modifier.testTag("full")) { MutantBadge("НОВОЕ") }
                            Box(Modifier.testTag("compact")) { MutantBadge("НОВОЕ", compact = true) }
                            Box(Modifier.testTag("room").padding(4.dp)) { MutantBadge("НОВОЕ") }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val full = onNodeWithTag("full").fetchSemanticsNode().boundsInRoot
            val compact = onNodeWithTag("compact").fetchSemanticsNode().boundsInRoot
            assertEquals(20f, full.height)
            assertEquals(16f, compact.height)
            // The plate is the text and its padding: 6 dp each side in full, 5 compact, over the wider Handjet 16.
            assertTrue(full.width - compact.width >= 2f, "full ${full.width}, compact ${compact.width}")
            val px = onNodeWithTag("full").captureToImage().toPixelMap()
            assertTrue(near(px[px.width / 2, 0], MutantPalettes.Kopiya.amber, .03f), "the plate's amber")
            assertTrue(near(px[2, px.height / 2], MutantPalettes.Kopiya.amber, .03f), "6 dp of amber before the word")
            // The ring's whole row, 1.5 dp out: drawn past the plate, read where the parent leaves room for it.
            val room = onNodeWithTag("room").captureToImage().toPixelMap()
            // The word's darkest pixel: Handjet 16's stems are covered whole, the page's black, not «Копия»'s ink.
            // Inside the plate only: the capture keeps the ring drawn past the node, which is that black anyway.
            val darkest =
                (6 until px.width - 6)
                    .flatMap { x ->
                        (3 until px.height - 3).map { y -> px[x, y] }
                    }.minBy { it.red }
            assertTrue(near(darkest, Color(0xFF05060C), .01f), "the word's black: $darkest")
            assertTrue(near(room[room.width / 2, 3], Color(0xFF05060C), .02f), "the ring ${room[room.width / 2, 3]}")
        }
}
