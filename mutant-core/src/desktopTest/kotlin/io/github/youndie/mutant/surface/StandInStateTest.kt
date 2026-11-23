package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-177: in the browser's way (the first frame does not wait for the build, bodies pooled, drawn by the shader), the
 * stand-in a body draws in its first frame shows the body's state, its heat and its stroke: a disabled body is grey, a
 * successful one its clean frame, a hot one boils, an outlined one has its stroke and its misregistered shadow, an error
 * its red marks, a loading one its bands, a focused one its brackets.
 *
 * The first frame is read from its own recording ([GraphicsLayer] of the stage, recorded in that frame's draw): a
 * capture renders a frame of its own, which takes the build if it is done by then (a still body's is quick) and shows
 * the body instead. Each case asserts that the frame read was the stand-in's ([LocalMutantStandInProbe]).
 */
@OptIn(ExperimentalTestApi::class)
class StandInStateTest {
    @BeforeTest
    fun empty() = BodyPool.clear()

    @AfterTest
    fun emptyAgain() = BodyPool.clear()

    /** Two pixels differ by more than 40 levels in the sum of their channels. */
    private fun differ(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > 40f / 255

    private class Frames(
        val first: List<Color>,
        val body: List<Color>,
        val colors: MutantColors,
    ) {
        fun firstCount(c: Color) =
            first.count {
                abs(it.red - c.red) + abs(it.green - c.green) + abs(it.blue - c.blue) <
                    .14f
            }
    }

    /** [surface] on a 260 × 100 dp stage at density 1: its first frame, and its body once the build has been taken. */
    private fun frames(
        print: Boolean = false,
        surface: @Composable () -> Unit,
    ): Frames {
        var frames: Frames? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            var standIns = 0
            var layer: GraphicsLayer? = null
            var colors: MutantColors? = null
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalMutantFirstFrameWaits provides false,
                    LocalMutantPoolBodies provides true,
                    LocalMutantBodyOnGpu provides true,
                    LocalMutantBodyAsImage provides false,
                    LocalMutantStandInProbe provides { standIns++ },
                ) {
                    MutantTheme(if (print) MutantPalettes.Kopiya else MutantPalettes.Efir, reduceMotion = false) {
                        colors = LocalMutantColors.current
                        val l = rememberGraphicsLayer()
                        layer = l
                        Box(
                            Modifier
                                .size(260.dp, 100.dp)
                                .drawWithContent {
                                    l.record { this@drawWithContent.drawContent() }
                                    drawLayer(l)
                                }.testTag("stage"),
                        ) {
                            Box(Modifier.padding(start = 20.dp, top = 20.dp)) { surface() }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            assertEquals(1, standIns, "the first frame is the stand-in")
            val first =
                runBlocking { layer!!.toImageBitmap() }.toPixelMap().let { px ->
                    List(px.width * px.height) {
                        px[
                            it %
                                px.width, it / px.width,
                        ]
                    }
                }
            repeat(40) {
                Thread.sleep(4)
                mainClock.advanceTimeByFrame()
            }
            frames = Frames(first, pixels(), colors!!)
        }
        return frames!!
    }

    private fun ComposeUiTest.pixels() =
        onNodeWithTag("stage").captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private fun Frames.differing() = first.indices.count { differ(first[it], body[it]) }

    @Test
    fun `a disabled body's first frame is the disabled body, but for its edge mosaic`() {
        val f =
            frames {
                val c = LocalMutantColors.current
                MutantSurface(
                    MutantBody.Btn,
                    "b177/off",
                    Modifier.size(200.dp, 48.dp),
                    fill = c.primary,
                    state = BodyState.Disabled,
                ) {}
            }
        // On main the first frame was the live acid plate: 8 041 px of it, 9 075 px differing from the body.
        assertEquals(0, f.firstCount(f.colors.primary), "the live fill in the disabled body's first frame")
        // What remains is the edge mosaic, generated with the instance: 952 px measured; held under 1 400.
        val differing = f.differing()
        assertTrue(differing < 1400, "first frame against the disabled body: $differing px differ")
    }

    @Test
    fun `a successful body's first frame is its clean frame`() {
        val f =
            frames {
                MutantSurface(
                    MutantBody.Btn,
                    "b177/ok",
                    Modifier.size(200.dp, 48.dp),
                    state = BodyState.Success,
                    focused = true,
                ) {}
            }
        // On main: 1 250 px differing (the live fill's plate, no highlight, an inset outline, no brackets).
        assertEquals(0, f.differing(), "first frame against the clean body")
    }

    @Test
    fun `an outlined body's first frame has its stroke and its misregistered shadow`() {
        val f =
            frames(print = true) {
                val c = LocalMutantColors.current
                MutantSurface(
                    MutantBody.Btn,
                    "b177/outlined",
                    Modifier.size(200.dp, 48.dp),
                    fill = c.background,
                    shadow = c.misregister,
                    stroke = c.textAccent,
                ) {}
            }
        val stroke = f.firstCount(f.colors.textAccent)
        val shadow = f.firstCount(f.colors.misregister)
        // The plate's stroke round 200 × 48: 1 387 px measured (1 358 on main). The shadow's 4 dp band below and right,
        // partly under the fill slipped 1.5 dp towards it: 495 px measured, 0 on main.
        assertTrue(stroke > 1000, "the stroke's ink: $stroke px")
        assertTrue(shadow > 300, "the shadow: $shadow px")
    }

    @Test
    fun `a hot body's first frame boils`() {
        val f =
            frames {
                val c = LocalMutantColors.current
                MutantSurface(MutantBody.Btn, "b177/hot", Modifier.size(200.dp, 48.dp), fill = c.primary, heat = .8) {}
            }
        val cold = f.firstCount(f.colors.primary)
        // The boil's flicker lifts the fill on every other 333 ms: either colour is the heat's.
        val hot =
            maxOf(
                f.firstCount(boilFill(f.colors.primary, f.colors, .8, 0.0)),
                f.firstCount(boilFill(f.colors.primary, f.colors, .8, 333.0)),
            )
        // Measured: 0 cold and 8 042 hot; on main 8 041 cold and 0 hot.
        assertEquals(0, cold, "the cold fill in a hot body's first frame")
        assertTrue(hot > 7000, "the hot fill: $hot px")
    }

    @Test
    fun `an error body's first frame has its red marks`() {
        val f =
            frames {
                MutantSurface(MutantBody.Btn, "b177/error", Modifier.size(200.dp, 48.dp), state = BodyState.Error) {}
            }
        val danger = f.firstCount(f.colors.danger)
        // The offset outline's 2.5 dp crescent on the right and the red blocks: 254 px measured, 0 on main.
        assertTrue(danger > 150, "the error's red: $danger px")
    }

    @Test
    fun `a loading body's first frame has its bands`() {
        val f =
            frames {
                MutantSurface(
                    MutantBody.Btn,
                    "b177/loading",
                    Modifier.size(200.dp, 48.dp),
                    state = BodyState.Loading,
                ) {}
            }
        // The bands are white at 0.5 and 0.35 over the fill.
        val bands =
            listOf(.5f, .35f).sumOf { a ->
                val p = f.colors.primary
                f.firstCount(Color(p.red + (1 - p.red) * a, p.green + (1 - p.green) * a, p.blue + (1 - p.blue) * a))
            }
        // Two bands across the plate, 6 and 3 dp, where they stand at the first frame's time: 582 px measured, 0 on main.
        assertTrue(bands > 300, "the loading bands: $bands px")
    }

    @Test
    fun `a focused body's first frame has its brackets`() {
        val f =
            frames {
                MutantSurface(MutantBody.Btn, "b177/focus", Modifier.size(200.dp, 48.dp), focused = true) {}
            }
        val brackets = f.firstCount(f.colors.focus)
        // Four corners, 2 × 10 dp each way: 160 px measured, 0 on main.
        assertTrue(brackets > 100, "the focus brackets: $brackets px")
    }
}
