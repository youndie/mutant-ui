package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    /** The focus's amber pixels in the cell captioned [caption] of [section]: its brackets, and the prefix's words. */
    private fun ComposeUiTest.amberIn(
        section: String,
        caption: String,
    ): Int {
        val cell =
            onAllNodes(hasTestTag("cell") and hasAnyAncestor(hasTestTag(section)), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .single { n ->
                    n.children.any { c ->
                        c.config.getOrElse(SemanticsProperties.Text) { emptyList() }.any {
                            it.text ==
                                caption
                        }
                    }
                }.boundsInRoot
        val shot = onNodeWithTag("sample-root", useUnmergedTree = true).captureToImage().toAwtImage()
        val amber = MutantPalettes.Efir.textOsd.toArgb()
        var n = 0
        // The brackets stand up to 8 dp outside the field's corners.
        for (y in (cell.top.toInt() - 8).coerceAtLeast(0) until cell.bottom.toInt().coerceAtMost(shot.height)) {
            for (x in (cell.left.toInt() - 8).coerceAtLeast(
                0,
            ) until (cell.right.toInt() + 8).coerceAtMost(shot.width)) {
                val p = shot.getRGB(x, y)
                val d =
                    abs((p shr 16 and 255) - (amber shr 16 and 255)) +
                        abs((p shr 8 and 255) - (amber shr 8 and 255)) +
                        abs((p and 255) - (amber and 255))
                if (d < 60) n++
            }
        }
        return n
    }

    @Test
    fun `a focused field's specimen stands down while a field beside it has the real focus`() {
        for (section in listOf("section-TextField", "section-Search")) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                val scroll = ScrollState(0)
                var focus: FocusManager? = null
                setContent {
                    focus = LocalFocusManager.current
                    MutantUiSample(SampleSettings(group = SpecGroup.Selection, cleanSignal = true), scroll)
                }
                mainClock.advanceTimeBy(1_000)
                val top = onNodeWithTag(section, useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y
                runOnUiThread { scroll.dispatchRawDelta(top - 40f) }
                mainClock.advanceTimeBy(500)
                val held = amberIn(section, s.stFocused)
                // A click into the field beside it: it takes the real focus, and the specimen loses its brackets.
                onAllNodes(
                    hasSetTextAction() and hasAnyAncestor(hasTestTag(section)),
                )[0].performMouseInput { click(center) }
                mainClock.advanceTimeBy(500)
                val beside = amberIn(section, s.stFocused)
                // The focus cleared, as a click on the page's ground clears it: the specimen shows its state once more.
                runOnUiThread { focus!!.clearFocus() }
                mainClock.advanceTimeBy(500)
                val away = amberIn(section, s.stFocused)
                // The four brackets are 160 amber pixels over the prefix's words (278 against 118, 314 against 154 in the
                // search's section, on 2026-11-23); before, the specimen kept them beside the focused field.
                assertTrue(held - beside > 100, "$section: $held amber pixels held, $beside with the focus beside it")
                assertEquals(held, away, "$section: the specimen's brackets back once the focus has left the section")
            }
        }
    }

    @Test
    fun `Enter on a navigation item leaves the focus on it in the new tab`() =
        phone(DemoScreen.Library) {
            for (item in listOf(s.queue, s.settings, s.onAir)) {
                val target = hasText(item) and hasClickAction() and hasAnyAncestor(hasTestTag("demo-library"))
                onNode(target).performSemanticsAction(SemanticsActions.RequestFocus)
                mainClock.advanceTimeBy(100)
                onNode(target).performKeyInput { pressKey(Key.Enter) }
                mainClock.advanceTimeBy(800)
                // Before: nothing was focused once the bar had moved to the new tab, and Tab went nowhere.
                assertTrue(focused().split(", ").contains(item), "after Enter on $item: ${focused()}")
            }
        }
}
