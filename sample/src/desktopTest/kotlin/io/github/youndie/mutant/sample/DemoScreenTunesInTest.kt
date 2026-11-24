package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-180 in the demo: switching a destination on the navigation bar, a tab of the recording or a filter chip of the
 * library, the old screen's last frame is the one before the click; from the click's frame the new one tunes in
 * (B-179's steps, its words from 224 ms), as the pill or the tabs' bar starts its run in that same frame. Under reduce
 * motion the new screen shows its words in the click's frame.
 *
 * The phone is read at density 1 only: at 2 the test window squeezes it to half its height, and what a switch shows
 * stands under its navigation bar (the kit's own test, `ScreenTunesInTest`, runs at 2). The tabs and the chip are read
 * in both palettes. Words are counted as pixels in their ink inside their own bounds, against the tape's grain with a
 * tenfold margin.
 */
@OptIn(ExperimentalTestApi::class)
class DemoScreenTunesInTest {
    private val s = SampleStrings(SampleLanguage.English)

    private class Switch(
        val name: String,
        val phone: DemoScreen,
        val tag: String,
        /** A word of the screen left, and one of the screen taken. */
        val old: String,
        val new: String,
        val click: ComposeUiTest.() -> Unit,
        /** Where the pill or the tabs' bar runs, in the phone's pixels, or null where a chip pours instead. */
        val runs: (ComposeUiTest.(d: Float) -> Rect)? = null,
    )

    private val navigation =
        Switch("navigation", DemoScreen.Library, "demo-library", s.findRecording, s.rain, {
            onNodeWithText(s.queue).performClick()
        }) { d ->
            // The bar's icon row, left of the button that stands over the bar's right end.
            Rect(0f, 650 * d, 250 * d, 700 * d)
        }

    private val tabs =
        Switch("tabs", DemoScreen.Recording, "demo-recording", s.testCard, s.master, {
            onNodeWithText(s.copies).performClick()
        }) { _ ->
            // The tabs' row's foot, under the labels, whose accent is the bar's paint.
            val tab = onNode(hasText(s.tracks)).fetchSemanticsNode().boundsInRoot
            val phone = onNodeWithTag("demo-recording").fetchSemanticsNode().boundsInRoot
            Rect(0f, tab.bottom - phone.top - tab.height / 6, phone.width, tab.bottom - phone.top)
        }

    private val chip =
        Switch("chip", DemoScreen.Library, "demo-library", s.nightTuesday, s.staticBetween, {
            onNodeWithText(s.noise).performClick()
        })

    private class Shot(
        val ms: Int,
        val px: PixelMap,
        val oldShown: Int,
        val newGlyphs: Int,
    )

    private fun ComposeUiTest.strip(
        sw: Switch,
        colors: MutantColors,
        d: Float,
        reduce: Boolean = false,
        frames: Int = 18,
    ): List<Shot> {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors, reduceMotion = reduce) {
                    DemoScreenFrame(sw.phone, s, dialogOpen = false, keyframeLoss = false)
                }
            }
        }
        mainClock.advanceTimeBy(1_500)

        fun shot(ms: Int): Shot {
            val phone = onNodeWithTag(sw.tag).fetchSemanticsNode().boundsInRoot
            val px = onNodeWithTag(sw.tag).captureToImage().toPixelMap()
            val rects =
                onAllNodes(hasText(sw.new), useUnmergedTree = true).fetchSemanticsNodes().map {
                    it.boundsInRoot.translate(-phone.topLeft)
                }
            return Shot(
                ms,
                px,
                onAllNodes(hasText(sw.old), useUnmergedTree = true).fetchSemanticsNodes().size,
                glyphs(px, rects, colors.text),
            )
        }
        val before = shot(-16)
        sw.click(this)
        return listOf(before) + List(frames) { i -> mainClock.advanceTimeByFrame().let { shot(i * 16) } }
    }

    private fun near(
        a: Color,
        b: Color,
        within: Float,
    ) = abs(a.red - b.red) < within && abs(a.green - b.green) < within && abs(a.blue - b.blue) < within

    private fun glyphs(
        px: PixelMap,
        rects: List<Rect>,
        ink: Color,
    ): Int =
        rects.sumOf { r ->
            var n = 0
            for (y in max(0, r.top.toInt()) until minOf(px.height, r.bottom.toInt())) {
                for (x in max(0, r.left.toInt()) until minOf(px.width, r.right.toInt())) {
                    if (near(px[x, y], ink, .08f)) n++
                }
            }
            n
        }

    /** The middle of the primary paint inside [r]: where the pill or the bar stands. */
    private fun primaryX(
        px: PixelMap,
        r: Rect,
        colors: MutantColors,
    ): Double {
        var sum = 0.0
        var n = 0
        for (y in max(0, r.top.toInt()) until minOf(px.height, r.bottom.toInt())) {
            for (x in max(0, r.left.toInt()) until minOf(px.width, r.right.toInt())) {
                if (near(px[x, y], colors.primary, .04f)) {
                    sum += x
                    n++
                }
            }
        }
        assertTrue(n > 20, "the pill or the bar is drawn ($n px)")
        return sum / n
    }

    private fun check(
        sw: Switch,
        colors: MutantColors,
        d: Float,
    ) = runComposeUiTest {
        val strip = strip(sw, colors, d)
        val tag = "${sw.name} at density $d"
        assertTrue(strip[0].oldShown > 0, "$tag: the old screen before the click")
        val after = strip.drop(1)
        for (x in after) assertEquals(0, x.oldShown, "$tag: the old screen gone from the click's frame, ${x.ms} ms")
        val whole = after.filter { it.ms >= 224 }
        val words = whole.minOf { it.newGlyphs }
        assertTrue(words > 40 * d * d, "$tag: the new words from 224 ms ($words px)")
        for (x in after.filter { it.ms <= 208 }) {
            assertTrue(x.newGlyphs * 10 < words, "$tag: no new word before the third step, ${x.ms} ms: ${x.newGlyphs}")
        }
        val runs = sw.runs ?: return@runComposeUiTest
        val r = runs(d)
        val xs = strip.take(3).map { primaryX(it.px, r, colors) }
        assertTrue(abs(xs[1] - xs[0]) < 1.5 * d, "$tag: in the click's frame the run is at its start: $xs")
        assertTrue(abs(xs[2] - xs[1]) > 2 * d, "$tag: the frame after, it has moved: $xs")
    }

    @Test
    fun `a destination on the navigation bar tunes its screen in as the pill starts`() =
        check(navigation, MutantPalettes.Efir, 1f)

    @Test
    fun `a tab of the recording tunes its page in as the bar starts`() {
        check(tabs, MutantPalettes.Efir, 1f)
        check(tabs, MutantPalettes.Kopiya, 1f)
    }

    @Test
    fun `a filter chip tunes in what it files as it starts to pour`() {
        check(chip, MutantPalettes.Efir, 1f)
        check(chip, MutantPalettes.Kopiya, 1f)
    }

    @Test
    fun `under reduce motion each switch shows the new screen finished at once`() {
        for (sw in listOf(navigation, tabs, chip)) {
            runComposeUiTest {
                val strip = strip(sw, MutantPalettes.Efir, 1f, reduce = true, frames = 2)
                assertEquals(0, strip[1].oldShown, "${sw.name}: the old screen gone")
                assertTrue(
                    strip[1].newGlyphs > 40,
                    "${sw.name}: the new words in the click's frame: ${strip[1].newGlyphs}",
                )
            }
        }
    }
}
