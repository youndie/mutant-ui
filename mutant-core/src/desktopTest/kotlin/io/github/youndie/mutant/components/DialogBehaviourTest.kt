package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-28: the dialog opens and closes, holds the screen still, keeps focus and gives it back, and what a consumer does. */
@OptIn(ExperimentalTestApi::class)
class DialogBehaviourTest {
    private var open by mutableStateOf(true)
    private var dismissals by mutableIntStateOf(0)
    private var confirms by mutableIntStateOf(0)
    private val opener = FocusRequester()

    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private fun ComposeUiTest.screen(reduceMotion: Boolean = false) =
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    Box(Modifier.size(360.dp, 600.dp).testTag("screen")) {
                        Column {
                            MutantButton("Стереть", {}, Modifier.focusRequester(opener).testTag("opener"))
                            MutantFab(MutantIcons.Add, null, {}, Modifier.testTag("behind"))
                        }
                        MutantDialog(
                            open,
                            "Стереть запись?",
                            { dismissals++ },
                            Modifier.testTag("dialog"),
                            text = "Плёнку можно будет записать заново.",
                            returnFocusTo = opener,
                        ) {
                            MutantDialogAction("Отмена", { open = false })
                            MutantDialogAction("Стереть", { confirms++ }, primary = true)
                        }
                    }
                }
            }
        }

    /** The scrim's darkness at a corner of the screen, where only it and the ground lie: 0 none, larger darker. */
    private fun ComposeUiTest.corner(): Float = onNodeWithTag("screen").captureToImage().toPixelMap()[350, 590].alpha

    @Test
    fun `it opens over 320 ms and closes over 200, then is gone and gives focus back to its opener`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            screen()
            mainClock.advanceTimeByFrame()
            val first = corner()
            mainClock.advanceTimeBy(100)
            val mid = corner()
            mainClock.advanceTimeBy(300)
            val full = corner()
            assertTrue(first < mid && mid < full, "darkening: $first, $mid, $full")
            assertEquals(.62f, full, .01f, "the scrim's 62 %")
            onNodeWithTag("dialog").assertIsFocused()
            open = false
            mainClock.advanceTimeByFrame()
            onAllNodesWithText("Отмена").fetchSemanticsNodes().single().let { node ->
                assertTrue(SemanticsProperties.Disabled in node.config, "closing, the actions no longer answer")
            }
            mainClock.advanceTimeBy(300)
            assertEquals(0, onAllNodesWithText("Отмена").fetchSemanticsNodes().size, "gone, nothing composed")
            mainClock.advanceTimeByFrame()
            onNodeWithTag("opener").assertIsFocused()
        }

    @Test
    fun `reversed mid-way it goes on from where it stands - no frame jumps`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            screen()
            var last = 0f
            var lastAt = mainClock.currentTime
            val jumps = mutableListOf<String>()

            // The scrim's darkness may move by the faster of the two rates (the close's 200 ms) over the time that passed.
            fun frame() {
                mainClock.advanceTimeByFrame()
                val now = corner()
                val at = mainClock.currentTime
                val allowed = .62f * (at - lastAt) / 200f + .02f
                if (kotlin.math.abs(now - last) > allowed) jumps += "$lastAt..$at: $last -> $now"
                last = now
                lastAt = at
            }
            repeat(7) { frame() }
            val opening = last
            assertTrue(opening in .1f..0.4f, "about a third into the open: $opening")
            // Cancel, about 110 ms into the open, and the dialog reopened about 100 ms into the close.
            onNodeWithText("Отмена").performMouseInput { click(center) }
            repeat(6) { frame() }
            assertTrue(last < opening, "closing from where it stood: $opening, then $last")
            open = true
            repeat(25) { frame() }
            assertTrue(jumps.isEmpty(), "jumps: $jumps")
            assertEquals(.62f, last, .01f, "open again")
        }

    @Test
    fun `opened while the screen's keyframe loss runs, its window is drawn whole`() {
        var running = false
        open = false
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        io.github.youndie.mutant.theme.MutantScreenClock {
                            Box(Modifier.size(360.dp, 300.dp).testTag("screen")) {
                                io.github.youndie.mutant.surface.KeyframeLossBody(
                                    Modifier.fillMaxSize(),
                                    MutantTheme.colors.background,
                                    null,
                                    {
                                        Column {
                                            MutantCard(Modifier.size(320.dp, 120.dp), key = "loss-card") {}
                                            MutantButton("Стереть", {}, key = "loss-button")
                                        }
                                        // A text long enough that the window stands across the whole screen, the band too.
                                        MutantDialog(
                                            open,
                                            "Стереть запись?",
                                            {},
                                            text = "Плёнку можно стереть. ".repeat(40),
                                        ) {
                                            MutantDialogAction("Отмена", {})
                                        }
                                    },
                                    probe = { running = it },
                                )
                            }
                        }
                    }
                }
            }
            while (!running && mainClock.currentTime < 12_000) mainClock.advanceTimeByFrame()
            assertTrue(running, "a loss runs by 12 s")
            open = true
            repeat(40) { mainClock.advanceTimeByFrame() }
            val window =
                onAllNodesWithText("Стереть запись?")
                    .fetchSemanticsNodes()
                    .first()
                    .let { title ->
                        generateSequence(title) { it.parent }.first { SemanticsProperties.PaneTitle in it.config }
                    }.boundsInRoot
            // A column inside the window's right padding, clear of its words and its button: the window's fill alone.
            val px = onNodeWithTag("screen").captureToImage().toPixelMap()
            val x = (window.right - 8).toInt()
            val top = maxOf(window.top.toInt() + 16, 0)
            val bottom = minOf(window.bottom.toInt() - 16, px.height)
            val column = (top until bottom).map { px[x, it] }
            val fill = column.sortedBy { it.red + it.green + it.blue }[column.size / 2]

            fun far(c: Color) = abs(c.red - fill.red) + abs(c.green - fill.green) + abs(c.blue - fill.blue) > .12f
            val off = column.count(::far)
            assertTrue(column.size > 200, "the window stands across the screen: $window")
            assertEquals(0, off, "rows of the window's column that are not its fill $fill")
        }
    }

    @Test
    fun `a click on the window's words focuses no action, and Enter after it fires none`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            screen(reduceMotion = true)
            mainClock.advanceTimeByFrame()
            onNodeWithText("Плёнку можно будет записать заново.").performMouseInput { click(center) }
            mainClock.advanceTimeByFrame()
            val focused =
                listOf("Отмена", "Стереть").flatMap { label ->
                    onAllNodesWithText(label).fetchSemanticsNodes().filter {
                        it.config.getOrElse(SemanticsProperties.Focused) { false }
                    }
                }
            assertTrue(focused.isEmpty(), "no action takes focus from a click on the text")
            onNodeWithTag("dialog").performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            assertTrue(open, "Enter cancelled nothing")
            assertEquals(0, confirms, "Enter erased nothing")
            assertEquals(0, dismissals, "and the click dismissed nothing")
            // And Tab from there still goes into the window, not to the screen behind.
            onNodeWithTag("dialog").performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
            onNodeWithText("Отмена").assertIsFocused()
        }

    @Test
    fun `under reduce motion it opens and closes at once`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            screen(reduceMotion = true)
            mainClock.advanceTimeByFrame()
            assertEquals(.62f, corner(), .01f)
            open = false
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertEquals(0, onAllNodesWithText("Отмена").fetchSemanticsNodes().size)
        }

    @Test
    fun `while it shows the screen stands still, and lives again once it is gone`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            open = false
            screen()
            mainClock.advanceTimeByFrame()
            val a = pixels("behind")
            mainClock.advanceTimeBy(1000)
            assertNotEquals(a, pixels("behind"), "the FAB drips on with no dialog")
            open = true
            mainClock.advanceTimeBy(500)
            // The FAB behind the scrim: the dialog's own window runs on its own clock, the screen's is held.
            val held = pixels("behind")
            mainClock.advanceTimeBy(1500)
            assertEquals(held, pixels("behind"), "held: nothing behind the dialog moves")
            open = false
            mainClock.advanceTimeBy(400)
            val after = pixels("behind")
            mainClock.advanceTimeBy(1000)
            assertNotEquals(after, pixels("behind"), "running again")
        }

    @Test
    fun `the scrim, Escape and the dismiss action dismiss it, the window does not`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            screen(reduceMotion = true)
            mainClock.advanceTimeByFrame()
            onNodeWithTag("screen").performTouchInput {
                click(
                    androidx.compose.ui.geometry
                        .Offset(10f, 590f),
                )
            }
            mainClock.advanceTimeByFrame()
            assertEquals(1, dismissals, "a click on the scrim")
            onNodeWithText("Плёнку можно будет записать заново.").performTouchInput { click(center) }
            mainClock.advanceTimeByFrame()
            assertEquals(1, dismissals, "the window takes its own clicks")
            onNodeWithTag("dialog").performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            assertEquals(2, dismissals, "Escape")
            val window =
                onAllNodesWithText("Стереть запись?").fetchSemanticsNodes().first().let { title ->
                    generateSequence(title) { it.parent }.first { SemanticsProperties.PaneTitle in it.config }
                }
            assertEquals("Стереть запись?", window.config[SemanticsProperties.PaneTitle])
            val title = onAllNodesWithText("Стереть запись?", useUnmergedTree = true).fetchSemanticsNodes().first()
            assertTrue(SemanticsProperties.Heading in title.config, "the title is a heading")
            assertTrue(SemanticsProperties.IsDialog in window.config, "a dialog")
            window.config[SemanticsActions.Dismiss].action?.invoke()
            mainClock.advanceTimeByFrame()
            assertEquals(3, dismissals, "the reader's dismiss")
        }

    @Test
    fun `its actions fire on release, the primary a 104 by 40 body in a 48 target, the plain one at least 48`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            screen(reduceMotion = true)
            mainClock.advanceTimeByFrame()
            val confirm = onAllNodesWithText("Стереть").fetchSemanticsNodes().last()
            assertEquals(48f, confirm.boundsInRoot.height)
            assertTrue(confirm.boundsInRoot.width >= 104f, "${confirm.boundsInRoot}")
            onNodeWithText("Отмена").assertHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
            onAllNodesWithText("Стереть")[1].performTouchInput { down(center) }
            mainClock.advanceTimeByFrame()
            assertEquals(0, confirms, "pressed, not released")
            onAllNodesWithText("Стереть")[1].performTouchInput { up() }
            mainClock.advanceTimeByFrame()
            assertEquals(1, confirms, "released")
        }

    @Test
    fun `focus stays inside while it is open`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            screen(reduceMotion = true)
            mainClock.advanceTimeByFrame()
            repeat(6) {
                onRoot().performKeyInput { pressKey(Key.Tab) }
                mainClock.advanceTimeByFrame()
            }
            val focused =
                onAllNodesWithText(
                    "Отмена",
                ).fetchSemanticsNodes().any { it.config.getOrElse(SemanticsProperties.Focused) { false } } ||
                    onAllNodesWithText("Стереть").fetchSemanticsNodes().any {
                        it.config.getOrElse(SemanticsProperties.Focused) { false }
                    } ||
                    onNodeWithTag("dialog").fetchSemanticsNode().config.getOrElse(SemanticsProperties.Focused) { false }
            assertTrue(focused, "focus on the dialog or its actions after six Tabs")
            assertTrue(
                !onNodeWithTag("opener").fetchSemanticsNode().config.getOrElse(SemanticsProperties.Focused) { false },
                "not on the screen behind",
            )
        }

    @Test
    fun `its window is the engine's - 280 by 171, its clean zone the engine's - and 280 at most on a wide screen`() {
        val ref =
            io.github.youndie.mutant.DesignParity
                .reference("MutantDialog_Window_Efir")["slot"]!!
                .jsonObject
        val zone = (ref["tz"] as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.double }
        var window: io.github.youndie.mutant.gen.Instance? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        CompositionLocalProvider(
                            io.github.youndie.mutant.type.LocalMutantTextRaster provides
                                io.github.youndie.mutant.ChromeText,
                        ) {
                            Box(Modifier.size(600.dp, 400.dp)) {
                                val keep = { it: io.github.youndie.mutant.gen.Instance -> window = it }
                                DialogFrame(
                                    true,
                                    "Стереть запись?",
                                    {},
                                    Modifier,
                                    "Плёнку можно будет записать заново.",
                                    null,
                                    {
                                        MutantDialogAction("Отмена", {})
                                        MutantDialogAction("Стереть", {}, primary = true)
                                    },
                                    probe = keep,
                                )
                            }
                        }
                    }
                }
            }
            // A running clock builds a body's first instance off the frame and draws it from the next (B-50).
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
        }
        val slot = window!!.slot
        assertEquals(280.0, slot.w, "280 wide on a 600 dp screen")
        assertEquals(ref["h"]!!.jsonPrimitive.double, slot.h, "as tall as the engine's")
        assertTrue(
            slot.textZone.toList().zip(zone).all { (k, e) ->
                kotlin.math.abs(k - e) <= 1.0
            },
            "zone ${slot.textZone.toList()}, engine $zone",
        )
    }

    @Test
    fun `a narrow screen, a long title and a doubled font neither throw nor leave the window`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Стереть запись с начала первой стороны и до конца второй без возврата"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme(reduceMotion = true) {
                        Column {
                            Box(
                                Modifier.size(0.dp),
                            ) { MutantDialog(true, long, {}, text = long) { MutantDialogAction("Да", {}) } }
                            Box(Modifier.size(260.dp, 700.dp).testTag("narrow")) {
                                MutantDialog(true, long, {}, Modifier.fillMaxSize(), text = long) {
                                    MutantDialogAction("Отмена", {})
                                    MutantDialogAction("Стереть", {}, primary = true)
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            pixels("narrow")
            val title = onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes().last().boundsInRoot
            assertTrue(
                title.left >= 24f + 20f - .5f && title.right <= 260f - 24f - 20f + .5f,
                "inside the window: $title",
            )
        }
}
