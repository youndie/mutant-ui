package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-174: the Foundations group's palette specimens, each under a theme of its own, keep the header's switches. */
@OptIn(ExperimentalTestApi::class)
class ThemeSpecimensTest {
    private fun foundations(
        settings: SampleSettings,
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { MutantUiSample(settings, ScrollState(0)) }
        mainClock.advanceTimeBy(1_000)
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
                if (a.getRGB(x, y) != b.getRGB(x, y)) n++
            }
        }
        return n
    }

    @Test
    fun `the palette specimens stand still under the header's reduce motion`() =
        foundations(SampleSettings(reduceMotion = true)) {
            val section = onNodeWithTag("section-Theme", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val a = window().cut(section)
            mainClock.advanceTimeBy(500)
            val b = window().cut(section)
            // Before: their own theme took reduce motion from the platform, and 10 455 pixels changed in the 500 ms.
            assertEquals(0, unlike(a, b), "pixels of the Theme section changed under reduce motion")
        }
}
