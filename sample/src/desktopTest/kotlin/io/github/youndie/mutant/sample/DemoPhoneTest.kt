package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-174: a phone on the Demo page is a screen — what is done in it stays in it. */
@OptIn(ExperimentalTestApi::class)
class DemoPhoneTest {
    private val s = SampleStrings(SampleLanguage.English)

    /** The Demo page on the Library phone, the page scrolled [down] px so the phone fills the window. */
    private fun demoPage(
        down: Float,
        block: ComposeUiTest.(ScrollState) -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val scroll = ScrollState(0)
        setContent { MutantUiSample(SampleSettings(page = SamplePage.Demo), scroll) }
        mainClock.advanceTimeBy(1_000)
        runOnUiThread { scroll.dispatchRawDelta(down) }
        mainClock.advanceTimeBy(100)
        block(scroll)
    }

    @Test
    fun `a fling that reaches a tab's top stops there, and the page stays where the finger left it`() =
        demoPage(289f) { scroll ->
            assertEquals(289, scroll.value, "the page scrolled to the phone")
            onNodeWithText(s.findRecording, useUnmergedTree = true).performTouchInput {
                swipeDown(startY = 4f, endY = 200f, durationMillis = 80)
            }
            // The drag itself goes on to the page, the phone's tab being at its top; its fling does not. The drag's last
            // move is applied in the frame after the lift.
            mainClock.advanceTimeByFrame()
            val lift = scroll.value
            mainClock.advanceTimeBy(1_500)
            // Before: 150 at the lift, 0 after it, the phone sliding out of the window.
            assertEquals(lift, scroll.value, "the page after the fling, against $lift at the lift")
        }

    @Test
    fun `a pull to refresh released moving leaves the page where it was`() =
        demoPage(289f) { scroll ->
            onNodeWithText(s.queue).performClick()
            mainClock.advanceTimeBy(800)
            onNodeWithText(s.rain, useUnmergedTree = true).performTouchInput {
                swipeDown(startY = 4f, endY = 260f, durationMillis = 200)
            }
            mainClock.advanceTimeBy(3_000)
            // Before: the pull consumed the drag and its release flung the page 286 px, 289 to 3.
            assertEquals(289, scroll.value, "the page after the pull's release")
            onNodeWithText(s.freshOffTheAir, useUnmergedTree = true).assertExists()
        }
}
