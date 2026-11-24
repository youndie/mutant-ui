package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B-183, the designer's answer 5: the drawer and the bottom sheet follow a finger and a mouse 1:1, close when let go past
 * 40 % of their size or faster than 800 dp/s, else settle back in 330 ms on the slide curve; the scrim is as dark as the
 * panel is open; past open they move 0.3 of the pointer, at most 24 dp; the body boils while dragged. Density 1: a dp is a
 * pixel. Where the panel stands is read from its words: the drawer's title, the sheet's own word.
 */
@OptIn(ExperimentalTestApi::class)
class PanelDragTest {
    private enum class Panel { Drawer, Sheet }

    private class Rig(
        val panel: Panel,
        val dismissed: () -> Int,
        /** The window's place at rest: the word's x (the drawer) or y (the sheet). */
        val rest: Float,
        /** The window's extent along its way, dp. */
        val size: Float,
    )

    private fun ComposeUiTest.rig(
        panel: Panel,
        reduceMotion: Boolean = false,
        white: Boolean = false,
        playIn: Boolean = false,
    ): Rig {
        var visible by mutableStateOf(!playIn)
        var dismissed = 0
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    MutantScreenClock {
                        Box(
                            Modifier
                                .size(360.dp, 600.dp)
                                .background(if (white) Color.White else MutantTheme.colors.page)
                                .testTag("screen"),
                        ) {
                            val dismiss = {
                                dismissed++
                                visible = false
                            }
                            when (panel) {
                                Panel.Drawer -> {
                                    MutantNavigationDrawer(
                                        visible,
                                        dismiss,
                                        NAMES.mapIndexed { i, n -> MutantDrawerItem("CH 0${i + 1}", n) },
                                        0,
                                        {},
                                    )
                                }

                                Panel.Sheet -> {
                                    MutantBottomSheet(visible, dismiss, windowInsets = WindowInsets(0)) {
                                        // First on the sheet, so its top stands a fixed 39 dp under the window's.
                                        Box(Modifier.size(300.dp, CONTENT_H.dp).testTag("content"))
                                        MutantTextButton(WORD, {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (playIn) {
            mainClock.advanceTimeBy(500)
            visible = true
            return Rig(panel, { dismissed }, Float.NaN, Float.NaN)
        }
        mainClock.advanceTimeBy(1_000)
        return measured(panel) { dismissed }
    }

    /** The rig over what is composed and at rest: the window's place and size read now. */
    private fun ComposeUiTest.measured(
        panel: Panel,
        dismissed: () -> Int,
    ): Rig {
        val size =
            when (panel) {
                Panel.Drawer -> DRAWER_W

                // The window's top stands above the content by the body's 14, its 12 of padding, the grabber's 5 and
                // the gap's 8; its foot is the screen's.
                Panel.Sheet -> 600f - (contentTop() - 14f - 12f - 5f - 8f)
            }
        return Rig(panel, dismissed, word(panel)!!, size)
    }

    private fun ComposeUiTest.word(panel: Panel): Float? =
        onAllNodes(hasText(if (panel == Panel.Drawer) "КАНАЛЫ" else WORD), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.positionInRoot
            ?.let { if (panel == Panel.Drawer) it.x else it.y }

    private fun ComposeUiTest.contentTop(): Float =
        onAllNodes(
            androidx.compose.ui.test
                .hasTestTag("content"),
        ).fetchSemanticsNodes()
            .single()
            .positionInRoot.y

    /** How far towards closed the window stands, dp; null once it is gone. */
    private fun ComposeUiTest.at(rig: Rig): Float? =
        word(rig.panel)?.let { if (rig.panel == Panel.Drawer) rig.rest - it else it - rig.rest }

    /** A pointer move of [d] dp towards closed. */
    private fun towards(
        panel: Panel,
        d: Float,
    ) = if (panel == Panel.Drawer) Offset(-d, 0f) else Offset(0f, d)

    /** Where a press takes the window: the drawer's empty body under its rows, the sheet's content. */
    private fun start(panel: Panel) = if (panel == Panel.Drawer) Offset(150f, 400f) else Offset(180f, 420f)

    private fun ComposeUiTest.press(
        panel: Panel,
        mouse: Boolean,
    ) {
        if (mouse) {
            onNodeWithTag("screen").performMouseInput {
                moveTo(start(panel))
                press()
            }
        } else {
            onNodeWithTag("screen").performTouchInput { down(start(panel)) }
        }
    }

    private fun ComposeUiTest.move(
        mouse: Boolean,
        by: Offset,
        delay: Long = 16,
    ) {
        if (mouse) {
            onNodeWithTag("screen").performMouseInput { moveBy(by, delay) }
        } else {
            onNodeWithTag("screen").performTouchInput { moveBy(by, delay) }
        }
    }

    private fun ComposeUiTest.lift(mouse: Boolean) {
        if (mouse) {
            onNodeWithTag("screen").performMouseInput {
                release()
            }
        } else {
            onNodeWithTag("screen").performTouchInput { up() }
        }
    }

    /** Frames until the window is gone or [n] have passed: the clock's time and where it stood. */
    private fun ComposeUiTest.frames(
        rig: Rig,
        n: Int,
    ): List<Pair<Long, Float?>> =
        List(n) {
            mainClock.advanceTimeByFrame()
            mainClock.currentTime to at(rig)
        }

    /**
     * [shots] against the slide curve from [from] to [to], `(1 − p)²` of the way left, p the time since the first shot
     * (which draws it where it was) over 330 ms; past the glide the window is gone or at [to].
     */
    private fun assertOnTheCurve(
        what: String,
        shots: List<Pair<Long, Float?>>,
        from: Float,
        to: Float,
    ) {
        val since = shots.first().first
        for ((t, x) in shots) {
            val p = minOf(1.0, (t - since) / MutantMotion.SLIDE_MS)
            val want = to + (from - to) * ((1 - p) * (1 - p)).toFloat()
            if (x == null) {
                assertTrue(p >= 1 - 1e-9 || abs(want - to) < 2f, "$what: gone at ${t - since} ms, $want on the curve")
            } else {
                assertTrue(abs(x - want) <= 1.5f, "$what at ${t - since} ms: the window at $x, $want on the curve")
            }
        }
    }

    @Test
    fun `a drag moves the panel by the pointer's distance on every frame by a finger and a mouse`() {
        for (panel in Panel.entries) {
            for (mouse in listOf(false, true)) {
                runComposeUiTest {
                    val rig = rig(panel)
                    val what = "$panel, ${if (mouse) "a mouse" else "a finger"}"
                    press(panel, mouse)
                    var moved = 0f
                    // From the first move, inside the slop as well as past it.
                    repeat(10) {
                        move(mouse, towards(panel, 6f))
                        moved += 6f
                        mainClock.advanceTimeByFrame()
                        val x = at(rig)!!
                        assertTrue(abs(x - moved) <= 1f, "$what: at $x after $moved")
                    }
                    lift(mouse)
                    assertEquals(0, rig.dismissed(), "$what: 60 dp, slowly, does not close")
                }
            }
        }
    }

    @Test
    fun `let go short of two fifths and slowly it settles back in 330 ms on the slide curve`() {
        for (panel in Panel.entries) {
            for (mouse in listOf(false, true)) {
                runComposeUiTest {
                    val rig = rig(panel)
                    val what = "$panel, ${if (mouse) "a mouse" else "a finger"}"
                    press(panel, mouse)
                    val travel = rig.size * .3f
                    repeat(10) { move(mouse, towards(panel, travel / 10)) }
                    // Still for a moment: the release carries no speed.
                    move(mouse, Offset.Zero, 120)
                    mainClock.advanceTimeByFrame()
                    val from = at(rig)!!
                    assertTrue(abs(from - travel) <= 1f, "$what: held at $from, $travel wanted")
                    lift(mouse)
                    val shots = frames(rig, 30)
                    assertEquals(0, rig.dismissed(), "$what: settled back, not dismissed")
                    assertOnTheCurve("$what settles back", shots, from, 0f)
                    assertTrue(abs(shots.last().second!!) <= .5f, "$what: back at ${shots.last().second}")
                }
            }
        }
    }

    @Test
    fun `let go past two fifths or faster than 800 dp a second it closes going on from where it stands`() {
        for (panel in Panel.entries) {
            for (mouse in listOf(false, true)) {
                for (fling in listOf(false, true)) {
                    runComposeUiTest {
                        val rig = rig(panel)
                        val how = if (fling) "flung" else "past 40 %"
                        val what = "$panel, ${if (mouse) "a mouse" else "a finger"}, $how"
                        press(panel, mouse)
                        if (fling) {
                            // 48 dp, a quarter of the way at most, at 1 500 dp a second.
                            repeat(4) { move(mouse, towards(panel, 12f), 8) }
                        } else {
                            // Past half the way, at under 400 dp a second, then still.
                            val travel = rig.size * .5f
                            repeat(10) { move(mouse, towards(panel, travel / 10), 40) }
                            move(mouse, Offset.Zero, 120)
                        }
                        mainClock.advanceTimeByFrame()
                        val from = at(rig)!!
                        assertTrue(from < rig.size * .4f == fling, "$what: let go at $from of ${rig.size}")
                        lift(mouse)
                        val shots = frames(rig, 30)
                        assertEquals(1, rig.dismissed(), "$what: dismissed")
                        assertOnTheCurve("$what closes", shots, from, rig.size)
                        assertNull(shots.last().second, "$what: gone once closed")
                    }
                }
                // Short of both, it settles: a slow move of the same 48 dp.
                runComposeUiTest {
                    val rig = rig(panel)
                    press(panel, mouse)
                    repeat(8) { move(mouse, towards(panel, 6f), 16) }
                    move(mouse, Offset.Zero, 120)
                    lift(mouse)
                    frames(rig, 30)
                    assertEquals(0, rig.dismissed(), "$panel: 48 dp slowly is no fling")
                }
            }
        }
    }

    @Test
    fun `dragged past open they move three tenths of the pointer and never past 24 dp`() {
        for (panel in Panel.entries) {
            for (mouse in listOf(false, true)) {
                runComposeUiTest {
                    val rig = rig(panel)
                    val what = "$panel, ${if (mouse) "a mouse" else "a finger"}"
                    press(panel, mouse)
                    var moved = 0f
                    repeat(12) {
                        move(mouse, towards(panel, -10f))
                        moved += 10f
                        mainClock.advanceTimeByFrame()
                        val x = at(rig)!!
                        val want = -minOf(moved * .3f, 24f)
                        assertTrue(abs(x - want) <= 1f, "$what: at $x after $moved towards open, $want wanted")
                    }
                    // Back towards closed, it comes back through open with the pointer.
                    repeat(12) {
                        move(mouse, towards(panel, 10f))
                        moved -= 10f
                        mainClock.advanceTimeByFrame()
                    }
                    assertTrue(abs(at(rig)!!) <= 1f, "$what: back at open, at ${at(rig)}")
                    lift(mouse)
                }
            }
        }
    }

    @Test
    fun `the scrim's alpha is proportional to where the panel stands on every frame`() {
        for (panel in Panel.entries) {
            runComposeUiTest {
                // The slide in from closed, read now and judged once the window's rest is known.
                val first = rig(panel, white = true, playIn = true)
                val sliding = mutableListOf<Pair<Float, Float>>()
                repeat(24) {
                    mainClock.advanceTimeByFrame()
                    val x = word(panel) ?: return@repeat
                    sliding += x to scrimAlpha(onNodeWithTag("screen").captureToImage().toPixelMap(), panel)
                }
                mainClock.advanceTimeBy(500)
                val rig = measured(panel, first.dismissed)
                val seen = mutableListOf<Float>()

                fun judge(
                    at: Float,
                    a: Float,
                    what: String,
                ) {
                    val f = at / rig.size
                    seen += f
                    val want = SCRIM_ALPHA * (1 - f).coerceIn(0f, 1f)
                    assertTrue(abs(a - want) <= .02f, "$panel, $what: the scrim at $a, $want for $f closed")
                }
                for ((x, a) in sliding) {
                    judge(if (panel == Panel.Drawer) rig.rest - x else x - rig.rest, a, "sliding in")
                }

                fun sample(what: String) {
                    val at = at(rig) ?: return
                    judge(at, scrimAlpha(onNodeWithTag("screen").captureToImage().toPixelMap(), panel), what)
                }
                press(panel, false)
                repeat(12) {
                    move(false, towards(panel, rig.size * .05f))
                    mainClock.advanceTimeByFrame()
                    sample("dragged")
                }
                move(false, Offset.Zero, 120)
                lift(false)
                repeat(24) {
                    mainClock.advanceTimeByFrame()
                    sample("closing")
                }
                assertTrue(seen.any { it > .5f }, "$panel: the drag took it past half way: $seen")
                assertTrue(sliding.size > 10, "$panel: the slide in was read: ${sliding.size} frames")
            }
        }
    }

    /** The scrim's alpha over the white screen, from a patch where nothing else stands. */
    private fun scrimAlpha(
        pixels: PixelMap,
        panel: Panel,
    ): Float {
        val xs = if (panel == Panel.Drawer) 290 until 340 else 20 until 340
        val ys = 20 until 60
        var sum = 0f
        for (x in xs) for (y in ys) sum += pixels[x, y].red
        val red = sum / (xs.count() * ys.count())
        return (1f - red) / (1f - SCRIM_RED)
    }

    @Test
    fun `the body is hot while dragged and cools once let go`() {
        for (panel in Panel.entries) {
            for (mouse in listOf(false, true)) {
                runComposeUiTest {
                    val rig = rig(panel)
                    val what = "$panel, ${if (mouse) "a mouse" else "a finger"}"
                    val cool = amber(panel, 0f)
                    press(panel, mouse)
                    repeat(5) { move(mouse, towards(panel, 8f)) }
                    // Held still, 40 dp along, for half a second.
                    repeat(30) {
                        move(mouse, Offset.Zero, 16)
                        mainClock.advanceTimeByFrame()
                    }
                    val held = at(rig)!!
                    val hot = amber(panel, held)
                    assertTrue(hot > cool + .3f, "$what: the body's amber at $hot dragged, $cool at rest")
                    lift(mouse)
                    mainClock.advanceTimeBy(1_500)
                    val after = amber(panel, 0f)
                    assertTrue(after < cool + .05f, "$what: cooled to $after once let go, $cool at rest")
                }
            }
        }
    }

    /** How amber the body's empty middle reads: its red over its blue, averaged, with the window [at] dp along. */
    private fun ComposeUiTest.amber(
        panel: Panel,
        at: Float,
    ): Float {
        val pixels = onNodeWithTag("screen").captureToImage().toPixelMap()
        val xs = if (panel == Panel.Drawer) (40 - at.toInt()) until (160 - at.toInt()) else 40 until 320
        val ys =
            if (panel ==
                Panel.Drawer
            ) {
                300 until 420
            } else {
                (contentTop().toInt() + 20) until (contentTop().toInt() + 100)
            }
        var sum = 0f
        for (x in xs) for (y in ys) sum += pixels[x, y].let { it.red - it.blue }
        return sum / (xs.count() * ys.count())
    }

    @Test
    fun `under reduce motion a let go lands at once`() {
        for (panel in Panel.entries) {
            runComposeUiTest {
                val rig = rig(panel, reduceMotion = true)
                press(panel, false)
                repeat(10) { move(false, towards(panel, rig.size * .03f)) }
                move(false, Offset.Zero, 120)
                mainClock.advanceTimeByFrame()
                assertTrue(at(rig)!! > rig.size * .25f, "$panel: it follows the finger under reduce motion")
                lift(false)
                mainClock.advanceTimeByFrame()
                assertTrue(abs(at(rig)!!) <= .5f, "$panel: back at once, at ${at(rig)}")
                press(panel, false)
                repeat(10) { move(false, towards(panel, rig.size * .06f)) }
                move(false, Offset.Zero, 120)
                lift(false)
                mainClock.advanceTimeByFrame()
                mainClock.advanceTimeByFrame()
                assertEquals(1, rig.dismissed(), "$panel: dismissed")
                assertNull(at(rig), "$panel: gone at once")
            }
        }
    }

    @Test
    fun `right to left the drawer drags towards the right`() =
        runComposeUiTest {
            var visible by mutableStateOf(true)
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Rtl,
                ) {
                    MutantTheme {
                        MutantScreenClock {
                            Box(Modifier.size(360.dp, 600.dp).testTag("screen")) {
                                MutantNavigationDrawer(
                                    visible,
                                    { visible = false },
                                    NAMES.mapIndexed { i, n -> MutantDrawerItem("CH 0${i + 1}", n) },
                                    0,
                                    {},
                                )
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val rest = word(Panel.Drawer)!!
            // The window stands at the screen's right; a press on its body, then a drag to the right.
            onNodeWithTag("screen").performTouchInput { down(Offset(210f, 400f)) }
            var moved = 0f
            repeat(10) {
                move(false, Offset(6f, 0f))
                moved += 6f
                mainClock.advanceTimeByFrame()
                val x = word(Panel.Drawer)!! - rest
                assertTrue(abs(x - moved) <= 1f, "right to left: at $x after $moved")
            }
            lift(false)
        }

    private companion object {
        val NAMES = listOf("Эфир", "Очередь", "Поиск")
        const val WORD = "Слово"
        const val CONTENT_H = 160

        /** The drawer's window: its 212 dp body less the 10 dp it runs off the screen's start. */
        const val DRAWER_W = 202f

        /** The scrim, `rgba(5, 6, 12, .5)` as a byte: 128 / 255. */
        const val SCRIM_ALPHA = 128f / 255f
        const val SCRIM_RED = 5f / 255f
    }
}
