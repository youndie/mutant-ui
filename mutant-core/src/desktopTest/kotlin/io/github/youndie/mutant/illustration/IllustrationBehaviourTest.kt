package io.github.youndie.mutant.illustration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.pattern.MutantPattern
import io.github.youndie.mutant.pattern.mutantPattern
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * B-14: icons depend on neither the cassette nor the clock; the cassette's reels turn on the scene's tick and
 * stand under reduce motion; patterns draw in dp at any density.
 */
@OptIn(ExperimentalTestApi::class)
class IllustrationBehaviourTest {
    private fun ComposeUiTest.pixels(): List<Color> =
        onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) {
                px[
                    it % px.width, it /
                        px.width,
                ]
            }
        }

    @Test
    fun `an icon is the same on every cassette and at every tick`() {
        val shots = ArrayList<List<Color>>()
        for (cassette in listOf("5", "77")) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(cassette = cassette) {
                            Box(Modifier.testTag("s")) { MutantIcon(MutantIcons.Refresh, null, size = 48.dp) }
                        }
                    }
                }
                mainClock.advanceTimeByFrame()
                shots += pixels()
                repeat(60) { mainClock.advanceTimeByFrame() }
                shots += pixels()
            }
        }
        assertTrue(shots.all { it == shots[0] }, "an icon changed with the cassette or the clock")
    }

    /** Inside the left reel's disc (the window at 44, 66, the reel 8, 7 in it, 26 dp): only the reel draws there. */
    private fun ComposeUiTest.reelPixels(): List<Color> =
        onNodeWithTag("s").captureToImage().toPixelMap().let { px ->
            (0 until 14 * 14).map { px[52 + 6 + it % 14, 73 + 6 + it / 14] }
        }

    private fun reel(reduceMotion: Boolean): Pair<List<Color>, List<Color>> {
        lateinit var first: List<Color>
        lateinit var later: List<Color>
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) {
                        // The cassette's window: the reels, kept clear of the body's damage.
                        Box(
                            Modifier.size(216.dp, 132.dp).testTag("s"),
                        ) { MutantIllustration(MutantIllustrationKind.Cassette) }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            first = reelPixels()
            repeat(8) { mainClock.advanceTimeByFrame() } // past a scene tick of 125 ms
            later = reelPixels()
        }
        return first to later
    }

    @Test
    fun `the cassette's reels turn while the clock runs and stand under reduce motion`() {
        val (a, b) = reel(reduceMotion = false)
        assertNotEquals(a, b, "running")
        val (c, d) = reel(reduceMotion = true)
        assertEquals(c, d, "paused")
    }

    @Test
    fun `a pattern draws in dp at density 2`() {
        lateinit var px: List<Color>
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    MutantTheme { Box(Modifier.size(30.dp).testTag("s").mutantPattern(MutantPattern.Lines)) }
                }
            }
            px = pixels()
        }
        // One dark dp in three: rows 0–1 dark at density 2, rows 2–5 the ground, then dark again at 6.
        val row = { y: Int -> px[y * 60 + 30].red }
        assertTrue(row(0) < row(2) && row(1) < row(2) && row(6) < row(5), (0..7).map(row).toString())
        assertEquals(row(2), row(5))
    }
}
