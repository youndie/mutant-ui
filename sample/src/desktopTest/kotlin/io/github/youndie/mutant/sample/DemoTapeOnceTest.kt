package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-101: on the Demo page a phone carries its own Tape, and only it. Over a whole Tape cycle the phone on the page draws
 * exactly what the same phone draws on its own; the page's Tape over it was a second noise.
 *
 * B-173: both compositions draw every resting tick from the image built ahead for it. A tick whose build was late drew
 * its vectors instead, about 7 000 pixels apart from its image, so the comparison turned on which background thread had
 * finished in wall time.
 */
@OptIn(ExperimentalTestApi::class)
class DemoTapeOnceTest {
    /** The phone's frames over a Tape cycle, 7 s at every other scene tick, as [content] composes it. */
    private fun frames(content: @Composable () -> Unit): List<BufferedImage> {
        val out = ArrayList<BufferedImage>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f), tickWaits provides true) { content() }
            }
            mainClock.advanceTimeBy(1_000)
            repeat(28) {
                mainClock.advanceTimeBy(250)
                out += onNodeWithTag("demo-recording", useUnmergedTree = true).captureToImage().toAwtImage()
            }
        }
        return out
    }

    @Test
    fun `a phone on the Demo page carries one Tape, its own`() {
        val page =
            frames {
                MutantUiSample(
                    SampleSettings(page = SamplePage.Demo, demoScreen = DemoScreen.Recording),
                    ScrollState(0),
                )
            }
        val alone =
            frames {
                MutantTheme {
                    // On the page's ground, as the phone's rounded corners show it on the page.
                    Box(Modifier.background(MutantTheme.colors.page)) {
                        DemoScreenFrame(DemoScreen.Recording, SampleStrings(SampleLanguage.English))
                    }
                }
            }
        val failures = ArrayList<String>()
        for (i in page.indices) {
            val a = page[i]
            val b = alone[i]
            var n = 0
            for (y in 0 until minOf(a.height, b.height)) {
                for (x in 0 until minOf(a.width, b.width)) if (a.getRGB(x, y) != b.getRGB(x, y)) n++
            }
            // A body's image is cut to the band of the window it stands in (B-86): its edges differ by a few pixels with
            // where the phone stands. The page's Tape over the phone differed on 28 % of it.
            if (n > a.width * a.height * MAX_SHARE) failures += "instant $i: $n px unlike the phone alone"
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        /**
         * B-173: the kit's `LocalMutantTickWaits`, internal to mutant-core, a test seam this module cannot name: its
         * getter is reached as the JVM sees it.
         */
        @Suppress("UNCHECKED_CAST")
        val tickWaits =
            Class
                .forName("io.github.youndie.mutant.surface.TestSeamsKt")
                .getMethod("getLocalMutantTickWaits")
                .invoke(null) as ProvidableCompositionLocal<Boolean>

        /** A body's edge pixels where its image is cut (B-86), against the 28 % the page's Tape added. */
        const val MAX_SHARE = .002
    }
}
