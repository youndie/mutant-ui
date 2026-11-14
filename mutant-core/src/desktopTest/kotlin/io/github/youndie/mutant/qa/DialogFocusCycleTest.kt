package io.github.youndie.mutant.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-40, overlays and the keyboard: inside an open dialog every Tab stop is one the user can see, and Tab cycles
 * through the actions. Found in the real window first (two Tabs with nothing on screen, then Cancel), then here.
 */
@OptIn(ExperimentalTestApi::class)
class DialogFocusCycleTest {
    private fun ComposeUiTest.dialog() {
        setContent {
            MutantTheme(reduceMotion = true) {
                Box(Modifier.size(360.dp, 600.dp)) {
                    MutantDialog(true, "Erase the recording?", {}, Modifier.testTag("dialog"), text = "Body") {
                        MutantDialogAction("Cancel", {}, Modifier.testTag("cancel"))
                        MutantDialogAction("Erase", {}, Modifier.testTag("erase"), primary = true)
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
    }

    private fun ComposeUiTest.focused(): String =
        onAllNodes(isFocused(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .joinToString {
                it.config.getOrElse(
                    SemanticsProperties.TestTag,
                ) { "untagged #${it.id} ${it.boundsInRoot}" }
            }

    private fun ComposeUiTest.tab(shift: Boolean = false) {
        onRoot().performKeyInput { if (shift) withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } else pressKey(Key.Tab) }
        mainClock.advanceTimeBy(100)
    }

    private fun ComposeUiTest.pixels(tag: String) =
        onNodeWithTag(tag, useUnmergedTree = true).captureToImage().toPixelMap().let { m ->
            List(m.width * m.height) { m[it % m.width, it / m.width] }
        }

    @Test
    fun `each action shows keyboard focus`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            dialog()
            val cancel = pixels("cancel")
            val erase = pixels("erase")
            repeat(6) {
                tab()
                when (focused()) {
                    "cancel" -> {
                        assertTrue(
                            pixels("cancel").zip(cancel).count { (a, b) ->
                                a != b
                            } > 40,
                            "Cancel's ring",
                        )
                    }

                    "erase" -> {
                        assertTrue(pixels("erase").zip(erase).count { (a, b) -> a != b } > 40, "Erase's ring")
                    }
                }
            }
        }

    @Ignore("F22: Tab inside a dialog stops on the scrim and the sheet, both invisible, and stays on the last action")
    @Test
    fun `F22 Tab inside a dialog visits only its actions and wraps`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            dialog()
            val seen = List(5) { tab().let { focused() } }
            assertEquals(listOf("cancel", "erase", "cancel", "erase", "cancel"), seen)
            tab(shift = true)
            assertEquals("erase", focused(), "Shift+Tab from the first action wraps to the last")
        }
}
