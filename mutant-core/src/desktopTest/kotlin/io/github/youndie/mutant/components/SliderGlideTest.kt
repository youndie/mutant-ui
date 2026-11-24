package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.surface.HeatMachine
import io.github.youndie.mutant.surface.TRANSITION_STEP_MS
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-182, the designer's answer 4: what the slider's handle does by itself glides 330 ms on the slide curve and boils; a
 * key lands in one frame with a heat impulse of 0.8; a drag follows the pointer; reduce motion lands at once. Density 1:
 * a dp is a pixel. The handle's place is read from its value's text, which stands centred in it.
 */
@OptIn(ExperimentalTestApi::class)
class SliderGlideTest {
    private class Rig(
        val clock: MutantClock,
        val heat: HeatMachine,
        val focus: FocusRequester,
        val get: () -> Int,
        val set: (Int) -> Unit,
    )

    private fun ComposeUiTest.rig(
        start: Int,
        reduceMotion: Boolean = false,
        stepped: Boolean = false,
    ): Rig {
        var value by mutableIntStateOf(start)
        val clock = MutantClock().apply { paused = reduceMotion }
        val heat = HeatMachine()
        val focus = FocusRequester()
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion, steppedMotion = stepped) {
                    ProvideMutantClock(clock) {
                        Box {
                            // As MutantSlider builds it over −10…10, with the handle's heat in the test's hands.
                            SliderBody(
                                fraction = (value + 10) / 20f,
                                valueText = signed(value),
                                onFraction = { f -> value = -10 + (f * 20).roundToInt() },
                                step = { by -> value = (value + by).coerceIn(-10, 10) },
                                modifier = Modifier.width(300.dp).focusRequester(focus).testTag("s"),
                                label = "ТРЕКИНГ",
                                rangeText = "−10 … +10",
                                enabled = true,
                                key = "ТРЕКИНГ",
                                interactionSource = null,
                                widest = listOf("−10", "+10"),
                                heat = heat,
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_000)
        return Rig(clock, heat, focus, { value }, { value = it })
    }

    /** The row's top, under the header; the handle stands 10 dp into the row, 34 dp. */
    private val row = 25f

    /** The handle's middle for a value of −10…10 on a 300 dp slider: 266 dp of travel. */
    private fun middle(v: Int) = (v + 10) / 20f * TRAVEL + 17f

    /** The middle of the handle as drawn, from its value's text. */
    private fun ComposeUiTest.handle(): Float {
        val n =
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .single { n ->
                    val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                    val t =
                        results
                            .single()
                            .layoutInput.text.text
                    t != "ТРЕКИНГ" && '…' !in t
                }
        return n.positionInRoot.x + n.size.width / 2f
    }

    /** One frame: the clock's time and where the handle is drawn. */
    private class Shot(
        val ms: Double,
        val x: Float,
        val boiling: Boolean,
    )

    private fun ComposeUiTest.frames(
        rig: Rig,
        n: Int,
    ): List<Shot> =
        List(n) {
            mainClock.advanceTimeByFrame()
            Shot(rig.clock.nowMs, handle(), rig.heat.on)
        }

    /**
     * The handle's way to [end] over [shots], read against the slide curve: `way · (1 − p)²` left at each frame, p the
     * time since the glide began over 330 ms, in the hot tick's steps where [stepped]. The glide begins at the first
     * frame's time, which draws it where it was.
     */
    private fun assertOnTheCurve(
        what: String,
        shots: List<Shot>,
        way: Float,
        end: Float,
        stepped: Boolean,
    ) {
        val since = shots.first().ms
        for (s in shots) {
            val e = s.ms - since
            val p =
                if (stepped) {
                    minOf(1.0, kotlin.math.floor(e / TRANSITION_STEP_MS) * TRANSITION_STEP_MS / MutantMotion.SLIDE_MS)
                } else {
                    minOf(1.0, e / MutantMotion.SLIDE_MS)
                }
            val want = end + way * ((1 - p) * (1 - p)).toFloat()
            assertTrue(abs(s.x - want) <= 1.5f, "$what at ${e.toInt()} ms: the handle at ${s.x}, $want on the curve")
        }
    }

    @Test
    fun `a tap on the channel glides the handle there in 330 ms on the slide curve, boiling`() {
        for (mouse in listOf(false, true)) {
            runComposeUiTest {
                val rig = rig(start = -8)
                val from = handle()
                if (mouse) {
                    onNodeWithTag("s").performMouseInput {
                        moveTo(Offset(middle(8), row + 27))
                        press()
                        release()
                    }
                } else {
                    onNodeWithTag("s").performTouchInput {
                        down(Offset(middle(8), row + 27))
                        up()
                    }
                }
                val shots = frames(rig, 30)
                val end = shots.last().x
                val what = if (mouse) "a mouse" else "a finger"
                assertEquals(8, rig.get(), what)
                assertTrue(abs(end - middle(8)) <= 1f, "$what: landed at $end, ${middle(8)} wanted")
                // No frame moves it further than the curve's steepest stretch does: 2 · way / 330 ms a ms, at its start.
                val way = from - end
                for ((a, b) in shots.zipWithNext()) {
                    val bound = 2 * abs(way) * (b.ms - a.ms) / MutantMotion.SLIDE_MS + 1
                    assertTrue(abs(b.x - a.x) <= bound, "$what: ${abs(b.x - a.x)} in a frame against $bound")
                }
                assertTrue(abs(shots.first().x - from) <= 1, "$what: the glide's first frame draws it where it stood")
                assertOnTheCurve(what, shots, way, end, stepped = false)
                // It boils while it glides, and cools once landed.
                val gliding = shots.filter { it.ms - shots.first().ms < MutantMotion.SLIDE_MS - 16 }
                assertTrue(gliding.all { it.boiling }, "$what: boiling through the glide")
                assertTrue(!shots.last().boiling, "$what: let go of once landed")
            }
        }
    }

    @Test
    fun `under steppedMotion the glide takes the kit's four steps`() =
        runComposeUiTest {
            val rig = rig(start = -8, stepped = true)
            val from = handle()
            onNodeWithTag("s").performTouchInput {
                down(Offset(middle(8), row + 27))
                up()
            }
            val shots = frames(rig, 30)
            val end = shots.last().x
            val places = shots.map { it.x }.zipWithNext().count { (a, b) -> abs(a - b) > .5f }
            assertEquals(4, places, "moves: ${shots.map { it.x }}")
            assertOnTheCurve("stepped", shots, from - end, end, stepped = true)
        }

    @Test
    fun `the settle at release and a value the app sets glide as well`() =
        runComposeUiTest {
            val rig = rig(start = 0)
            val rest = handle()
            // Taken by its middle and let go 6 dp right, under half a step (13.3 dp): the value stays 0, and the handle
            // goes back.
            onNodeWithTag("s").performMouseInput {
                moveTo(Offset(middle(0), row + 27))
                press()
                moveBy(Offset(3f, 0f))
                moveBy(Offset(3f, 0f))
            }
            frames(rig, 2)
            assertTrue(abs(handle() - (rest + 6)) <= 1, "held 6 dp right: at ${handle()}, ${rest + 6} wanted")
            onNodeWithTag("s").performMouseInput { release() }
            val settle = frames(rig, 30)
            assertEquals(0, rig.get())
            assertTrue(abs(settle.last().x - rest) <= 1, "settled at ${settle.last().x}, $rest wanted")
            assertOnTheCurve("the settle", settle, 6f, settle.last().x, stepped = false)
            // The app sets −8: the handle glides there.
            rig.set(-8)
            val set = frames(rig, 30)
            val end = set.last().x
            assertTrue(abs(end - middle(-8)) <= 1, "set: landed at $end, ${middle(-8)} wanted")
            assertOnTheCurve("a value set", set, rest - end, end, stepped = false)
        }

    @Test
    fun `a key lands in one frame, and the handle's heat reads 0,8 in that frame`() =
        runComposeUiTest {
            val rig = rig(start = 0)
            runOnIdle { rig.focus.requestFocus() }
            mainClock.advanceTimeBy(600)
            assertEquals(0.0, rig.heat.at(rig.clock.nowMs), "cool before the key")
            // The heat read before the key goes up: the up comes frames later, and the heat cools by then.
            onNodeWithTag("s").performKeyInput { keyDown(Key.DirectionRight) }
            assertEquals(.8, rig.heat.at(rig.clock.nowMs), 1e-9, "the heat in the key's frame")
            onNodeWithTag("s").performKeyInput { keyUp(Key.DirectionRight) }
            mainClock.advanceTimeByFrame()
            assertTrue(abs(handle() - middle(1)) <= 1, "an arrow: at ${handle()}, ${middle(1)} wanted")
            mainClock.advanceTimeBy(1_000)
            onNodeWithTag("s").performKeyInput { keyDown(Key.MoveEnd) }
            assertEquals(.8, rig.heat.at(rig.clock.nowMs), 1e-9, "End's heat")
            onNodeWithTag("s").performKeyInput { keyUp(Key.MoveEnd) }
            mainClock.advanceTimeByFrame()
            assertTrue(abs(handle() - middle(10)) <= 1, "End: at ${handle()}, ${middle(10)} wanted")
            // A key in the middle of a glide lands it.
            onNodeWithTag("s").performTouchInput {
                down(Offset(middle(-8), row + 27))
                up()
            }
            frames(rig, 4)
            onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionLeft) }
            mainClock.advanceTimeByFrame()
            assertEquals(-9, rig.get())
            assertTrue(abs(handle() - middle(-9)) <= 1, "a key over a glide: at ${handle()}, ${middle(-9)} wanted")
        }

    @Test
    fun `a drag follows the pointer 1 to 1, also when it takes the handle out of a glide`() =
        runComposeUiTest {
            val rig = rig(start = 0)
            for (glide in listOf(false, true)) {
                if (glide) {
                    onNodeWithTag("s").performMouseInput {
                        moveTo(Offset(middle(-8), row + 27))
                        press()
                        release()
                    }
                    frames(rig, 5)
                }
                // Pressed on the handle where it is drawn; the input's own time moves the clock, so where it was taken is
                // read in the frame after the press, which already stands still under it.
                onNodeWithTag("s").performMouseInput {
                    moveTo(Offset(handle(), row + 27))
                    press()
                }
                mainClock.advanceTimeByFrame()
                val at = handle()
                mainClock.advanceTimeByFrame()
                assertTrue(abs(handle() - at) <= 1, "glide $glide: taken at $at, then at ${handle()} with no move")
                var moved = 0f
                repeat(8) {
                    onNodeWithTag("s").performMouseInput { moveBy(Offset(7f, 0f)) }
                    moved += 7f
                    mainClock.advanceTimeByFrame()
                    val x = handle()
                    assertTrue(abs(x - (at + moved)) <= 1, "glide $glide: at $x after $moved, ${at + moved} wanted")
                }
                onNodeWithTag("s").performMouseInput { release() }
                frames(rig, 30)
            }
        }

    @Test
    fun `under reduce motion every move lands at once`() =
        runComposeUiTest {
            val rig = rig(start = -8, reduceMotion = true)
            onNodeWithTag("s").performTouchInput {
                down(Offset(middle(8), row + 27))
                up()
            }
            mainClock.advanceTimeByFrame()
            assertTrue(abs(handle() - middle(8)) <= 1, "a tap: at ${handle()}, ${middle(8)} wanted")
            rig.set(-3)
            mainClock.advanceTimeByFrame()
            assertTrue(abs(handle() - middle(-3)) <= 1, "a value set: at ${handle()}, ${middle(-3)} wanted")
        }

    private companion object {
        const val TRAVEL = 266f
    }
}
