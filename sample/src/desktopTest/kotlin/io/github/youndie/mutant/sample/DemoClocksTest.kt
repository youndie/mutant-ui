package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
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
 * B-90: on the demo page the Settings screen opens with its dialog, and the dialog stands that screen alone; the Library
 * screen beside it keeps ticking.
 */
@OptIn(ExperimentalTestApi::class)
class DemoClocksTest {
    private fun BufferedImage.differsFrom(other: BufferedImage): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getRGB(x, y) != other.getRGB(x, y)) n++
        return n
    }

    @Test
    fun `the library screen ticks while the settings screen's dialog is open`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false) { DemoPage(SampleStrings(SampleLanguage.English)) }
                }
            }

            fun shot() = onNodeWithTag("demo-library", useUnmergedTree = true).captureToImage().toAwtImage()
            repeat(90) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(3)
            }
            val before = shot()
            repeat(60) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(3)
            }
            val changed = shot().differsFrom(before)
            assertTrue(changed > 2_000, "the library screen's pixels changed over a second: $changed")
        }
}
