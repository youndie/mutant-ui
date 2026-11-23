package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-174: where the sample stands what the kit draws — clipped where a screen would clip it, inside what holds it. */
@OptIn(ExperimentalTestApi::class)
class SampleLayoutTest {
    private val s = SampleStrings(SampleLanguage.English)

    /** The Components page on [settings]' group, the clock driven by hand, the page scrolled to put [tag] at its top. */
    private fun page(
        settings: SampleSettings,
        tag: String? = null,
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val scroll = ScrollState(0)
        setContent { MutantUiSample(settings, scroll) }
        mainClock.advanceTimeBy(1_000)
        if (tag != null) {
            val top = onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y
            runOnUiThread { scroll.dispatchRawDelta(top - 40f) }
            mainClock.advanceTimeBy(100)
        }
        block()
    }

    private fun ComposeUiTest.window(): BufferedImage =
        onNodeWithTag("sample-root", useUnmergedTree = true).captureToImage().toAwtImage()

    private fun BufferedImage.cut(r: Rect): BufferedImage =
        getSubimage(r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())

    private fun unlike(
        a: BufferedImage,
        b: BufferedImage,
    ): Int {
        var n = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                if (a.getRGB(x, y) !=
                    b.getRGB(x, y)
                ) {
                    n++
                }
            }
        }
        return n
    }

    @Test
    fun `the snackbar of «Try it» rides off under its holder's foot, not over the section below`() =
        page(SampleSettings(group = SpecGroup.Communication, cleanSignal = true), "section-Snackbar") {
            onNodeWithText(s.showSnackbar, useUnmergedTree = true).performClick()
            mainClock.advanceTimeBy(600)
            val holder = onNodeWithTag("snack-holder", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            // The section's own padding under the holder, as wide as the bar and its shadow.
            val under = Rect(holder.left - 8, holder.bottom, holder.right + 8, holder.bottom + 24)
            val rest = window().cut(under)
            onAllNodes(hasText(s.undo), useUnmergedTree = true)[1].performClick()
            val failures = ArrayList<String>()
            for (t in 16..320 step 16) {
                mainClock.advanceTimeByFrame()
                val n = unlike(rest, window().cut(under))
                // Unclipped, the bar rode over this strip from about 100 ms and left it whole at 224 (20 034 pixels in
                // one frame); clipped, nothing of it reaches here. The Tape is off and nothing else is drawn in the strip.
                if (n > 0) failures += "at $t ms: $n px of the strip under the holder changed"
            }
            assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        }
}
