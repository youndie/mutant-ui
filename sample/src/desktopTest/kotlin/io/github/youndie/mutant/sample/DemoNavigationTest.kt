package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-96: each demo phone runs the app; its navigation bar and its drawer move between the four tabs, a card opens the
 * recording and its back arrow returns, and every public component of the kit is shown by the sample.
 */
@OptIn(ExperimentalTestApi::class)
class DemoNavigationTest {
    private val s = SampleStrings(SampleLanguage.English)

    private fun app(block: ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme { DemoScreenFrame(DemoScreen.Library, s, keyframeLoss = false) }
                }
            }
            mainClock.advanceTimeBy(600)
            block()
        }

    private fun ComposeUiTest.settle() = mainClock.advanceTimeBy(800)

    /** How many texts on screen read [text]. */
    private fun ComposeUiTest.shows(text: String) =
        onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `the navigation bar moves between the four tabs`() =
        app {
            onNodeWithText(s.findRecording, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText(s.queue).performClick()
            settle()
            onNodeWithText(s.copying, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText(s.find).performClick()
            settle()
            onNodeWithText(s.archiveWord, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText(s.settings).performClick()
            settle()
            onNodeWithText(s.autoTracking, useUnmergedTree = true).assertIsDisplayed()
            assertEquals(0, shows(s.copying), "the queue's words are gone once another tab is taken")
        }

    @Test
    fun `the drawer moves between the tabs and closes`() =
        app {
            onNodeWithContentDescription(s.menu).performClick()
            settle()
            onNode(hasText("CH 03") and hasText(s.channelNames[2])).assertIsDisplayed().performClick()
            settle()
            onNodeWithText(s.archiveWord, useUnmergedTree = true).assertIsDisplayed()
            assertEquals(0, shows(s.channels), "the drawer has closed")
        }

    @Test
    fun `a card opens its recording and the back arrow returns to the library`() =
        app {
            onNodeWithText(s.nightTuesday).performClick()
            settle()
            onNodeWithText(s.listen, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithText(s.volume, useUnmergedTree = true).assertExists()
            onNodeWithContentDescription(s.back).performClick()
            settle()
            onNodeWithText(s.findRecording, useUnmergedTree = true).assertIsDisplayed()
            assertEquals(0, shows(s.listen), "the recording is closed")
        }

    @Test
    fun `every public component of the kit is shown by the sample`() {
        val public =
            File("../mutant-core/src/commonMain/kotlin")
                .walk()
                .filter { it.extension == "kt" }
                .flatMap { f -> PUBLIC_FUN.findAll(f.readText()).map { it.groupValues[1] } }
                .toSortedSet()
        assertTrue(public.size > 40, "public functions found: ${public.size}")
        val sample =
            File("src/commonMain/kotlin")
                .walk()
                .filter { it.extension == "kt" }
                .joinToString("\n") { it.readText() }
        // A call, not a mention: an import alone does not show the component.
        val missing = public.filter { !Regex("\\b$it\\s*[({<]").containsMatchIn(sample) }
        assertTrue(missing.isEmpty(), "never shown: $missing")
    }

    private companion object {
        val PUBLIC_FUN = Regex("""(?m)^public fun (?:<[^>]+> )?(Mutant[A-Za-z]+)\(""")
    }
}
