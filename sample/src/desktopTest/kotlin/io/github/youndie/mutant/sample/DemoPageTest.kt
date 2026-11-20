package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-148: the Demo page shows one screen at a time, Library first, and a row of chips chooses it. */
@OptIn(ExperimentalTestApi::class)
class DemoPageTest {
    private val tags =
        mapOf(
            DemoScreen.Library to "demo-library",
            DemoScreen.Recording to "demo-recording",
            DemoScreen.Settings to "demo-settings",
        )

    @Test
    fun `the demo page composes one screen, the one its chip chose`() {
        val s = SampleStrings(SampleLanguage.English)
        val labels =
            mapOf(
                DemoScreen.Library to s.demoLibrary,
                DemoScreen.Recording to s.demoRecording,
                DemoScreen.Settings to s.settings,
            )
        val settings = SampleSettings(page = SamplePage.Demo)
        val scroll = ScrollState(0)
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { MutantUiSample(settings, scroll) }
            mainClock.advanceTimeBy(1_000)

            fun composed() =
                tags
                    .filterValues {
                        onAllNodes(hasTestTag(it), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
                    }.keys

            assertEquals(DemoScreen.Library, settings.demoScreen, "the default screen")
            assertEquals(setOf(DemoScreen.Library), composed(), "the page as it opens")
            for (screen in DemoScreen.entries.reversed()) {
                onNode(hasText(labels.getValue(screen)) and hasAnyAncestorTagged("demo-index")).performClick()
                mainClock.advanceTimeBy(1_000)
                assertEquals(screen, settings.demoScreen, "the chip of ${screen.name} chose it")
                // The Settings screen's dialog takes focus as it opens; the page stays put, its chips in view.
                assertEquals(0, scroll.value, "the page's scroll under ${screen.name}")
                assertEquals(setOf(screen), composed(), "the screens composed under ${screen.name}")
                for (other in DemoScreen.entries) {
                    val chip = onNode(hasText(labels.getValue(other)) and hasAnyAncestorTagged("demo-index"))
                    if (other == screen) chip.assertIsSelected() else chip.assertIsNotSelected()
                }
            }
        }
    }

    private fun hasAnyAncestorTagged(tag: String) =
        androidx.compose.ui.test
            .hasAnyAncestor(hasTestTag(tag))
}
