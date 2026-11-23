package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `the demo's sort menu stands inside the phone, its end at its button's end`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme { DemoScreenFrame(DemoScreen.Library, s, keyframeLoss = false) }
                }
            }
            mainClock.advanceTimeBy(600)
            onNodeWithText(s.find).performClick()
            mainClock.advanceTimeBy(800)
            val button = onNode(hasText(s.sortBy(0)) and hasClickAction()).fetchSemanticsNode().boundsInWindow
            onNodeWithText(s.sortBy(0), useUnmergedTree = true).performClick()
            mainClock.advanceTimeBy(400)
            val phone = onNodeWithTag("demo-library", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
            // Each row is the menu's 216 dp less its 6 dp padding either side, so the menu ends 6 dp past its rows.
            val rows =
                s.sortOrders.map {
                    onNode(hasText(it) and hasClickAction()).fetchSemanticsNode().boundsInWindow.right + 6
                }
            val end = rows.max()
            // From the button's start the menu ran to 483 dp in the 360 dp phone (B-168's F-menu-09).
            assertTrue(end <= phone.right, "the menu ends at $end, the phone at ${phone.right}")
            assertEquals(button.right, end, 1f, "the menu's end at the button's end")
        }

    /** The laid-out width of the one text that reads [text], in px (1 dp here): what a plate is measured by. */
    private fun ComposeUiTest.lineWidth(text: String): Int {
        val layouts = ArrayList<TextLayoutResult>()
        onNode(hasText(text), useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        return layouts.single().size.width
    }

    @Test
    fun `the header's hints are lines no wider than their titles, in both languages`() {
        val failures = ArrayList<String>()
        for (language in SampleLanguage.entries) {
            val t = SampleStrings(language)
            page(SampleSettings(language = language)) {
                for ((label, title, hint) in listOf(
                    Triple(t.steppedMotion, t.steppedMotionTitle, t.steppedMotionHint),
                    Triple(t.mosaicPastContour, t.mosaicPastContourTitle, t.mosaicPastContourHint),
                )) {
                    onNodeWithText(label, useUnmergedTree = true).performMouseInput { moveTo(center) }
                    mainClock.advanceTimeBy(1_500)
                    val titleWidth = lineWidth(title)
                    val hintWidth = lineWidth(hint)
                    // The plate is one line per text, as wide as its widest (B-32): the stepped-motion hint was 1 381 dp,
                    // its plate 1 405 in the 1 024 dp window. No wider than its title, a hint no longer sets the plate.
                    if (hintWidth >
                        titleWidth
                    ) {
                        failures += "$language, $label: hint $hintWidth dp, title $titleWidth dp"
                    }
                    onNodeWithText(label, useUnmergedTree = true).performMouseInput { moveTo(Offset(-400f, -400f)) }
                    mainClock.advanceTimeBy(1_500)
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
