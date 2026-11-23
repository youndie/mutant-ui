package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-174: what the demo shows follows the record it belongs to, not the place it stands in — the queue's rows, the
 * table's selection, the chosen day and the recording's own side, length and time.
 */
@OptIn(ExperimentalTestApi::class)
class DemoRecordsTest {
    private val s = SampleStrings(SampleLanguage.English)

    /** The Library phone; with [reduce] its clock stands (reduce motion) and its Tape is off (the clean signal). */
    private fun phone(
        reduce: Boolean = false,
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduce, cleanSignal = reduce) {
                    DemoScreenFrame(DemoScreen.Library, s, keyframeLoss = false)
                }
            }
        }
        mainClock.advanceTimeBy(600)
        block()
    }

    private fun ComposeUiTest.screen(): BufferedImage =
        onNodeWithTag("demo-library", useUnmergedTree = true).captureToImage().toAwtImage()

    /** A list row with its divider under it: the row's clickable bounds and 6 dp below. */
    private fun ComposeUiTest.row(title: String): Rect =
        onNode(hasText(title) and hasClickAction()).fetchSemanticsNode().boundsInRoot.let {
            Rect(it.left, it.top, it.right, it.bottom + 6)
        }

    private fun BufferedImage.cut(r: Rect): BufferedImage =
        getSubimage(r.left.toInt(), r.top.toInt() - PHONE_TOP, r.width.toInt(), r.height.toInt())

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
    fun `a tape put in at the top of the queue leaves every row below its own tile and divider`() =
        phone(reduce = true) {
            onNodeWithText(s.queue).performClick()
            mainClock.advanceTimeBy(800)
            val titles = listOf(s.rain, s.weather, s.lateRepeat)
            val before = titles.associateWith { screen().cut(row(it)) }
            onNodeWithText(s.rain, useUnmergedTree = true).performTouchInput {
                swipeDown(startY = 4f, endY = 260f, durationMillis = 400)
            }
            // The refresh brings «Fresh off the air» in after 2 s; then the rows have moved a row down.
            mainClock.advanceTimeBy(4_000)
            onNodeWithText(s.freshOffTheAir, useUnmergedTree = true).assertExists()
            val failures = ArrayList<String>()
            for (t in titles) {
                val was = before.getValue(t)
                val now = screen().cut(row(t))
                val n = unlike(was, now)
                // Under reduce motion and with the Tape off nothing moves but the rows, so a row keyed by its record draws
                // the same picture a row lower: 0 pixels unlike on 2026-11-23, the limit leaving B-101's 0.2 % for a body's
                // edges cut to the band it stands in (B-86). Keyed by its place, it takes the tile and divider above.
                if (n > was.width * was.height * MAX_SHARE) failures += "$t: $n of ${was.width * was.height} px unlike"
            }
            assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        }

    private companion object {
        /** Where the phone stands in the test's window: under its 28 dp title and the 16 dp gap. */
        const val PHONE_TOP = 48

        /** B-101's share for a body's edges where its image is cut to its band (B-86). */
        const val MAX_SHARE = .002
    }
}
