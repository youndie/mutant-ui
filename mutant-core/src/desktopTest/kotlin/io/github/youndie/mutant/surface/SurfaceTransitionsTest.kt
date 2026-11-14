package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
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
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-12: transitions as a consumer drives them — a new fill pours, a slide request slides, a melt melts; all land at once paused. */
@OptIn(ExperimentalTestApi::class)
class SurfaceTransitionsTest {
    private val c = MutantPalettes.Efir

    private fun PixelMap.share(
        color: Color,
        ys: IntRange,
    ): Double {
        var n = 0
        var all = 0
        for (y in ys) {
            for (x in 30 until 130) {
                all++
                if (this[x, y] == color) n++
            }
        }
        return n.toDouble() / all
    }

    private fun ComposeUiTest.frames(n: Int) = repeat(n) { mainClock.advanceTimeByFrame() }

    private fun pour(reduceMotion: Boolean): Triple<Double, Double, Double> {
        var shares = Triple(0.0, 0.0, 0.0)
        runComposeUiTest {
            mainClock.autoAdvance = false
            var fill by mutableStateOf(c.surfaceVariant)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) {
                        Box(Modifier.size(160.dp, 80.dp).testTag("f")) {
                            MutantSurface(
                                MutantBody.Chip,
                                "chip",
                                Modifier.padding(20.dp).size(112.dp, 32.dp),
                                fill = fill,
                            ) {}
                        }
                    }
                }
            }
            frames(10)
            fill = c.primary
            frames(9) // about 150 ms: past the first steps of a ~420 ms pour
            val mid = onNodeWithTag("f").captureToImage().toPixelMap()
            frames(40)
            val end = onNodeWithTag("f").captureToImage().toPixelMap()
            shares =
                Triple(mid.share(c.primary, 24..30), mid.share(c.surfaceVariant, 40..48), end.share(c.primary, 24..48))
        }
        return shares
    }

    @Test
    fun `a new fill pours down from the top and fills the body when done`() {
        val (topNew, bottomOld, endNew) = pour(reduceMotion = false)
        assertTrue(topNew > .3, "new colour at the top mid-pour: $topNew")
        assertTrue(bottomOld > .3, "old colour at the bottom mid-pour: $bottomOld")
        assertTrue(endNew > .5, "new colour once poured: $endNew")
    }

    @Test
    fun `under reduce motion a new fill lands at once`() {
        val (topNew, bottomOld, _) = pour(reduceMotion = true)
        assertTrue(topNew > .3, "new colour at the top: $topNew")
        assertTrue(bottomOld < .05, "old colour left at the bottom: $bottomOld")
    }

    @Test
    fun `a slide request moves the body from dx and home again`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var request by mutableStateOf(SlideRequest(0, -24.0))
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false) {
                        Box(Modifier.size(120.dp, 60.dp).background(c.background).testTag("f")) {
                            MutantSurface(
                                MutantBody.Thumb,
                                "thumb",
                                Modifier.padding(start = 60.dp, top = 20.dp).size(22.dp, 22.dp),
                                slide = request,
                            ) {
                            }
                        }
                    }
                }
            }
            frames(10)

            fun leftmost(px: PixelMap) =
                (0 until px.width).first { x ->
                    (0 until px.height).any { y ->
                        px[x, y] ==
                            c.phosphor
                    }
                }
            val home = leftmost(onNodeWithTag("f").captureToImage().toPixelMap())
            request = SlideRequest(1, -24.0)
            frames(2)
            val away = leftmost(onNodeWithTag("f").captureToImage().toPixelMap())
            frames(30)
            val back = leftmost(onNodeWithTag("f").captureToImage().toPixelMap())
            assertTrue(away < home - 10, "slid from the left: home $home, away $away")
            assertEquals(home, back, "home again after 330 ms")
        }

    @Test
    fun `a melt whose frame lands in its last 3 ms still lands`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val clock =
                io.github.youndie.mutant.clock
                    .MutantClock()
            var on by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        io.github.youndie.mutant.clock.ProvideMutantClock(clock) {
                            Box(Modifier.size(40.dp).background(Color.Black).testTag("f")) {
                                Box(
                                    Modifier
                                        .padding(10.dp)
                                        .size(20.dp)
                                        .mutantMelt(on)
                                        .background(Color.White),
                                )
                            }
                        }
                    }
                }
            }
            frames(5)
            on = true
            frames(1)
            val since = clock.nowMs
            // The next frame steps 16 ms: it lands 331 ms into the 330 ms melt, before its last step at 333.3.
            clock.setTime(since + 315)
            frames(1)
            assertTrue(clock.nowMs - since in 330.0..333.3, "a frame in the last 3 ms: ${clock.nowMs - since}")
            frames(30)
            val white =
                onNodeWithTag("f").captureToImage().toPixelMap().let { px ->
                    (0 until 40).sumOf { y -> (0 until 40).count { x -> px[x, y] == Color.White } }
                }
            assertEquals(400, white, "melted in")
        }

    @Test
    fun `a melt reveals its indicator step by step and at once when paused`() {
        for (paused in listOf(false, true)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                var on by mutableStateOf(false)
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(reduceMotion = paused) {
                            Box(Modifier.size(40.dp).background(Color.Black).testTag("f")) {
                                Box(
                                    Modifier
                                        .padding(10.dp)
                                        .size(20.dp)
                                        .mutantMelt(on)
                                        .background(Color.White),
                                )
                            }
                        }
                    }
                }
                frames(5)
                on = true
                frames(8) // ~130 ms: one step of 330, 83 ms after the event

                fun white() =
                    onNodeWithTag("f").captureToImage().toPixelMap().let { px ->
                        (0 until 40).sumOf { y ->
                            (
                                0 until
                                    40
                            ).count { x -> px[x, y] == Color.White }
                        }
                    }
                val early = white()
                frames(30)
                val done = white()
                if (paused) {
                    assertEquals(400, early, "paused: the indicator at once")
                } else {
                    assertTrue(early in 1 until 300, "melting in: $early white pixels")
                    assertEquals(400, done, "melted in")
                }
            }
        }
    }
}
