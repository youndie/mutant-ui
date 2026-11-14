package io.github.youndie.mutant.surface

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-11: the body's states as a consumer drives them — a press boils it, focus calms it, disabled holds still. */
@OptIn(ExperimentalTestApi::class)
class SurfaceStatesTest {
    private val colors = MutantPalettes.Efir

    @Test
    fun `heat rises in 320 ms and cools in 480 from where it was`() {
        val h = HeatMachine()
        assertEquals(0.0, h.at(0.0))
        h.press(1000.0)
        assertEquals(.5, h.at(1160.0), 1e-9)
        assertEquals(1.0, h.at(1400.0), 1e-9)
        h.release(1400.0)
        assertEquals(.5, h.at(1640.0), 1e-9)
        assertEquals(0.0, h.at(1900.0), 1e-9)
        h.press(1640.0) // pressed again before it cooled: from 0.5
        assertEquals(.75, h.at(1720.0), 1e-9)
    }

    private fun amberShare(px: androidx.compose.ui.graphics.PixelMap): Double {
        var amber = 0
        var all = 0
        for (y in 20 until 60) {
            for (x in 20 until 220) {
                val c = px[x, y]
                all++
                if (c.red > .9f && c.green in .55f..0.85f && c.blue < .35f) amber++
            }
        }
        return amber.toDouble() / all
    }

    private fun pressRun(reduceMotion: Boolean): List<Double> {
        val shares = ArrayList<Double>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            val source = MutableInteractionSource()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) {
                        Box(Modifier.size(240.dp, 100.dp).testTag("f")) {
                            MutantSurface(
                                MutantBody.Btn,
                                "btn/a",
                                Modifier.padding(20.dp).size(200.dp, 48.dp),
                                interactionSource = source,
                            ) {}
                        }
                    }
                }
            }
            repeat(10) { mainClock.advanceTimeByFrame() }
            shares += amberShare(onNodeWithTag("f").captureToImage().toPixelMap())
            val press = PressInteraction.Press(Offset(100f, 24f))
            runBlocking { source.emit(press) }
            repeat(30) { mainClock.advanceTimeByFrame() }
            shares += amberShare(onNodeWithTag("f").captureToImage().toPixelMap())
            runBlocking { source.emit(PressInteraction.Release(press)) }
            repeat(60) { mainClock.advanceTimeByFrame() }
            shares += amberShare(onNodeWithTag("f").captureToImage().toPixelMap())
        }
        return shares
    }

    @Test
    fun `a press boils the body amber and a release cools it back`() {
        val (rest, pressed, released) = pressRun(reduceMotion = false)
        assertTrue(rest < .01, "amber at rest: $rest")
        assertTrue(pressed > .5, "amber while pressed for 480 ms: $pressed")
        assertTrue(released < .01, "amber 960 ms after the release: $released")
    }

    @Test
    fun `under reduce motion a press does not boil`() {
        val shares = pressRun(reduceMotion = true)
        assertTrue(shares.all { it < .01 }, "amber under reduce motion: $shares")
    }

    @Test
    fun `focus calms the body to 0_3 and draws the brackets`() =
        runComposeUiTest {
            var seen: Instance? = null
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        Box(Modifier.size(300.dp, 100.dp).testTag("f")) {
                            MutantSurface(
                                MutantBody.Field,
                                "field",
                                Modifier
                                    .padding(
                                        20.dp,
                                    ).size(260.dp, 52.dp),
                                focused = true,
                                probe = {
                                    seen =
                                        it
                                },
                            ) {}
                        }
                    }
                }
            }
            waitForIdle()
            assertEquals(.3, seen!!.slot.calm)
            val px = onNodeWithTag("f").captureToImage().toPixelMap()
            // The top-left bracket: x 13..23 along y 13, 7 dp outside the slot at (20, 20).
            val focus = (14 until 22).count { px[it, 13] == colors.focus || px[it, 14] == colors.focus }
            assertTrue(focus >= 6, "focus pixels on the top-left bracket: $focus")
        }

    @Test
    fun `a disabled body builds one frame and holds still on a running clock`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var built = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false) {
                        MutantSurface(
                            MutantBody.Btn,
                            "button",
                            Modifier.size(
                                200.dp,
                                48.dp,
                            ),
                            state = BodyState.Disabled,
                            probe = {
                                built++
                            },
                        ) {}
                    }
                }
            }
            repeat(200) { mainClock.advanceTimeByFrame() }
            assertEquals(1, built, "frames built for a disabled body")
            assertTrue(colors.ink != Color.Unspecified)
        }
}
