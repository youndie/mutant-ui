package io.github.youndie.mutant.illustration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
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
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.pattern.MutantPattern
import io.github.youndie.mutant.pattern.mutantPattern
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * B-14: icons depend on neither the cassette nor the clock; the cassette's reels turn on the scene's tick and
 * stand under reduce motion; patterns draw in dp at any density. B-170: the reels and the set's rods draw in dp too, and
 * an illustration in a narrow slot keeps its size, centred.
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

    /** [kind] alone at [density] on the still clock, its own box captured. */
    private fun art(
        kind: MutantIllustrationKind,
        colors: MutantColors,
        density: Float,
    ): PixelMap {
        lateinit var px: PixelMap
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    MutantTheme(colors = colors, reduceMotion = true) {
                        ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                            Box(Modifier.background(colors.background).testTag("s")) { MutantIllustration(kind) }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            px = onNodeWithTag("s").captureToImage().toPixelMap()
        }
        return px
    }

    /** The box, in pixels, of the pixels within 48 a channel of [ink] inside [region] (dp) at [density]. */
    private fun PixelMap.inkBox(
        region: Rect,
        density: Float,
        ink: Color,
    ): Rect {
        val near = { c: Color ->
            abs(c.red - ink.red) < .19f && abs(c.green - ink.green) < .19f && abs(c.blue - ink.blue) < .19f
        }
        var l = Int.MAX_VALUE
        var t = Int.MAX_VALUE
        var r = Int.MIN_VALUE
        var b = Int.MIN_VALUE
        for (y in (region.top * density).toInt() until (region.bottom * density).toInt()) {
            for (x in (region.left * density).toInt() until (region.right * density).toInt()) {
                if (near(this[x, y])) {
                    l = minOf(l, x)
                    t = minOf(t, y)
                    r = maxOf(r, x + 1)
                    b = maxOf(b, y + 1)
                }
            }
        }
        assertTrue(l <= r, "nothing of $ink in $region at density $density")
        return Rect(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat())
    }

    /**
     * Each part's drawn box at [density] is density 1's times the density, every edge within 2 px: an edge's
     * anti-aliased pixel can fall either side of the 48 cut, at most a pixel at 1 and one at the density. Measured, the
     * reels stand within 0.9 px and the rods within 1.5; drawn in pixels, the left reel ended 26 px short at density 2.
     */
    private fun assertScales(
        kind: MutantIllustrationKind,
        colors: MutantColors,
        ink: Color,
        parts: Map<String, Rect>,
    ) {
        val one = art(kind, colors, 1f)
        for (density in listOf(2f, 2.625f)) {
            val px = art(kind, colors, density)
            for ((name, region) in parts) {
                val want =
                    one.inkBox(region, 1f, ink).let {
                        Rect(
                            it.left * density,
                            it.top * density,
                            it.right * density,
                            it.bottom * density,
                        )
                    }
                val got = px.inkBox(region, density, ink)
                val off =
                    maxOf(
                        abs(got.left - want.left),
                        abs(got.top - want.top),
                        abs(got.right - want.right),
                        abs(got.bottom - want.bottom),
                    )
                assertTrue(off <= 2f, "$kind's $name at density $density: drawn $got, density 1 puts it at $want")
            }
        }
    }

    @Test
    fun `the cassette's reels draw in dp at density 2 and at a phone's`() {
        // Each reel's 26 dp box (the window at 44, 66, the reels at 8, 7 and 94, 7 in it), 4 dp round it: the window's
        // ground and the edge of its outline, nothing else of the text's colour in «Эфир», where the text is light (in
        // «Копия» it is within 48 of the outline's ink).
        val reels = mapOf("left reel" to Rect(48f, 69f, 82f, 103f), "right reel" to Rect(134f, 69f, 168f, 103f))
        assertScales(MutantIllustrationKind.Cassette, MutantPalettes.Efir, MutantPalettes.Efir.text, reels)
    }

    @Test
    fun `the set's rods draw in dp at density 2 and at a phone's`() {
        // Above the set's body (from y 34, its outline from about 27): the rods alone, the ink on «Копия»'s light ground
        // (on «Эфир»'s navy the ink is within 48 of the ground). Split at the rods' foot, 108.5 between their pivots.
        val ink = Color(0xFF05060C)
        val rods = mapOf("left rod" to Rect(60f, 0f, 108f, 22f), "right rod" to Rect(109f, 0f, 160f, 22f))
        assertScales(MutantIllustrationKind.Television, MutantPalettes.Kopiya, ink, rods)
    }

    @Test
    fun `given less than its width, an illustration keeps its size, centred`() {
        // The same cassette in a 120 dp slot and in a 216 dp one, both centred in 360: one picture, to the pixel. Squeezed
        // to 120, the art stood from the slot's left, 48 dp right of where it stands centred.
        val shots =
            listOf(120, 216).map { slot ->
                lateinit var px: List<Color>
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            MutantTheme(reduceMotion = true) {
                                ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                                    Box(Modifier.size(360.dp, 180.dp).testTag("s")) {
                                        Box(Modifier.align(Alignment.TopCenter).width(slot.dp)) {
                                            MutantIllustration(MutantIllustrationKind.Cassette)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    mainClock.advanceTimeByFrame()
                    px = pixels()
                }
                px
            }
        val differ = shots[0].indices.count { shots[0][it] != shots[1][it] }
        assertEquals(0, differ, "pixels differing between the 120 dp slot and the 216 dp one")
    }
}
