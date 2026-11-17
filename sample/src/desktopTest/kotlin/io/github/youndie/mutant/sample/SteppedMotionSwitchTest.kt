package io.github.youndie.mutant.sample

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-106: the header's stepped-motion switch is named plainly, and says what it does when the pointer rests on it. */
@OptIn(ExperimentalTestApi::class)
class SteppedMotionSwitchTest {
    @Test
    fun `the stepped-motion switch explains itself on hover, in both languages`() {
        for (language in SampleLanguage.entries) {
            val s = SampleStrings(language)
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent { MutantUiSample(SampleSettings(language = language)) }
                mainClock.advanceTimeBy(1_000)
                assertTrue(
                    onAllNodes(hasText(s.steppedMotionHint), useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
                    "$language: no hint before the pointer comes",
                )
                onNodeWithText(s.steppedMotion, useUnmergedTree = true).performMouseInput { moveTo(center) }
                mainClock.advanceTimeBy(2_000)
                assertTrue(
                    onAllNodes(hasText(s.steppedMotionHint), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
                    "$language: the hint shows on hover",
                )
            }
        }
    }
}
