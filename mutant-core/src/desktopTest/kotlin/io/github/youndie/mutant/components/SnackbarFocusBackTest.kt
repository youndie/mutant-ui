package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** B-207: after the snackbar's action focus goes back to where it stood before the snackbar took it. */
@OptIn(ExperimentalTestApi::class)
class SnackbarFocusBackTest {
    private var visible by mutableStateOf(false)
    private var withButton by mutableStateOf(true)
    private var actions = 0
    private val button = FocusRequester()
    private val other = FocusRequester()

    private fun ComposeUiTest.screen() {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = false) {
                    MutantScreenClock {
                        Column(Modifier.size(360.dp, 300.dp)) {
                            if (withButton) {
                                MutantButton(
                                    "Добавить",
                                    { visible = true },
                                    Modifier.focusRequester(button).testTag("button"),
                                )
                            }
                            MutantButton("Другое", {}, Modifier.focusRequester(other).testTag("other"))
                            MutantSnackbar(
                                "Добавлено в очередь",
                                Modifier.width(328.dp),
                                visible = visible,
                                actionLabel = "ОТМЕНИТЬ",
                                onAction = {
                                    actions++
                                    visible = false
                                },
                                returnFocusTo = button,
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
    }

    private fun ComposeUiTest.focusedCount() = onAllNodes(isFocused()).fetchSemanticsNodes().size

    @Test
    fun `focus a button, open the snackbar, take its action from the keyboard - the button is focused again`() =
        runComposeUiTest {
            screen()
            button.requestFocus()
            mainClock.advanceTimeByFrame()
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeBy(400)
            onNodeWithText("ОТМЕНИТЬ").requestFocus()
            mainClock.advanceTimeByFrame()
            onNodeWithText("ОТМЕНИТЬ").assertIsFocused()
            onRoot().performKeyInput { pressKey(Key.Enter) }
            assertEquals(1, actions, "the action fired")
            // Back at once, as the bar starts to leave, and still there once it has gone.
            mainClock.advanceTimeByFrame()
            onNodeWithTag("button").assertIsFocused()
            mainClock.advanceTimeBy(400)
            onNodeWithTag("button").assertIsFocused()
            assertEquals(1, focusedCount())
        }

    @Test
    fun `a snackbar whose action never held focus moves nothing as it leaves`() =
        runComposeUiTest {
            screen()
            visible = true
            mainClock.advanceTimeBy(400)
            other.requestFocus()
            mainClock.advanceTimeByFrame()
            visible = false
            mainClock.advanceTimeBy(400)
            onNodeWithTag("other").assertIsFocused()
            assertEquals(1, focusedCount())
        }

    @Test
    fun `a requester on nothing composed any more leaves no node focused, and nothing throws`() =
        runComposeUiTest {
            screen()
            visible = true
            mainClock.advanceTimeBy(400)
            withButton = false
            mainClock.advanceTimeByFrame()
            onNodeWithText("ОТМЕНИТЬ").requestFocus()
            mainClock.advanceTimeByFrame()
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeBy(400)
            assertEquals(1, actions)
            assertEquals(0, focusedCount(), "no node focused")
        }
}
