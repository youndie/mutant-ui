package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-174: the keyboard's focus in the sample is where a keyboard user expects it — back on what opened an overlay once
 * it has gone, never held by a picture of an open overlay, on the chosen navigation item after Enter.
 */
@OptIn(ExperimentalTestApi::class)
class SampleFocusTest {
    private val s = SampleStrings(SampleLanguage.English)

    /** What the focused nodes read: their words and descriptions, «none» when nothing is focused. */
    private fun ComposeUiTest.focused(): String =
        onAllNodes(isFocused())
            .fetchSemanticsNodes()
            .joinToString(" | ") { n ->
                (
                    n.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } +
                        n.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
                ).joinToString(", ").ifEmpty { "a node with no words" }
            }.ifEmpty { "none" }

    /** Focus [opener] as the keyboard would reach it, press Enter on it, wait for the overlay, press Escape. */
    private fun ComposeUiTest.openAndEscape(opener: SemanticsMatcher) {
        onNode(opener).performSemanticsAction(SemanticsActions.RequestFocus)
        mainClock.advanceTimeBy(100)
        onNode(opener).performKeyInput { pressKey(Key.Enter) }
        mainClock.advanceTimeBy(600)
        onNode(isFocused()).performKeyInput { pressKey(Key.Escape) }
        // The dialog goes in 200 ms; the drawer in 200 ms; focus goes back once it has gone.
        mainClock.advanceTimeBy(800)
    }

    private fun page(
        group: SpecGroup,
        section: String,
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val scroll = ScrollState(0)
        setContent { MutantUiSample(SampleSettings(group = group), scroll) }
        mainClock.advanceTimeBy(1_000)
        val top = onNodeWithTag(section, useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y
        runOnUiThread { scroll.dispatchRawDelta(top - 40f) }
        mainClock.advanceTimeBy(100)
        block()
    }

    private fun phone(
        screen: DemoScreen,
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme { DemoScreenFrame(screen, s, keyframeLoss = false) }
            }
        }
        mainClock.advanceTimeBy(600)
        block()
    }

    @Test
    fun `the page's pictures of an open dialog, drawer and sheet hold no focus`() {
        for ((group, section) in listOf(
            SpecGroup.Overlays to "section-Dialog",
            SpecGroup.Overlays to "section-Sheet",
            SpecGroup.Navigation to "section-Drawer",
        )) {
            page(group, section) {
                // Before: the «open» specimen took the focus as it appeared and kept it from the rest of the page.
                assertEquals("none", focused(), "$section as the page opens")
            }
        }
    }

    @Test
    fun `«Try it»'s dialog and drawer give the focus back to their buttons after Escape`() {
        page(SpecGroup.Overlays, "section-Dialog") {
            openAndEscape(hasText(s.openDialog) and hasClickAction())
            assertEquals(s.openDialog, focused(), "after the dialog")
        }
        page(SpecGroup.Navigation, "section-Drawer") {
            openAndEscape(hasText(s.openDrawer) and hasClickAction())
            assertEquals(s.openDrawer, focused(), "after the drawer")
        }
    }

    @Test
    fun `the demo's drawer and dialogs give the focus back after Escape`() {
        phone(DemoScreen.Library) {
            openAndEscape(hasContentDescription(s.menu) and hasClickAction())
            assertEquals(s.menu, focused(), "after the drawer")
        }
        phone(DemoScreen.Library) {
            onNode(hasText(s.find) and hasClickAction()).performClick()
            mainClock.advanceTimeBy(800)
            onNode(hasText(s.testCard) and isToggleable()).performClick()
            mainClock.advanceTimeBy(600)
            openAndEscape(hasText(s.eraseSelected(1)) and hasClickAction())
            assertEquals(s.eraseSelected(1), focused(), "after the archive's erase dialog")
        }
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme { DemoScreenFrame(DemoScreen.Settings, s, keyframeLoss = false) }
                }
            }
            // The Settings screen opens under its dialog, which takes the focus as it opens.
            mainClock.advanceTimeBy(600)
            onNode(isFocused()).performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeBy(800)
            assertEquals(s.menu, focused(), "after the Settings screen's dialog, the screen's first control")
        }
    }
}
