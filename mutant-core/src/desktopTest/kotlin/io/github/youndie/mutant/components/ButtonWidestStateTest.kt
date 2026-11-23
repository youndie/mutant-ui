package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-203 (designer's answer 35): a button given no width is as wide as the widest of its states, in every state, so no
 * state change moves its width or regenerates its body; a new label is a new measurement, and a caller's width wins.
 */
@OptIn(ExperimentalTestApi::class)
class ButtonWidestStateTest {
    private class Stage {
        var state by mutableStateOf(MutantButtonState.Idle)
        var enabled by mutableStateOf(true)
        var text by mutableStateOf("Воспроизвести")
    }

    /** The button's width, px, in each of rest, pressed, Loading, Success, back to rest and disabled. */
    private fun ComposeUiTest.widthsThroughStates(s: Stage): Map<String, Int> {
        val out = LinkedHashMap<String, Int>()

        fun read(name: String) {
            mainClock.advanceTimeBy(48)
            onNodeWithTag("b").captureToImage()
            out[name] = onNodeWithTag("b").fetchSemanticsNode().size.width
        }
        read("rest")
        onNodeWithTag("b").performTouchInput { down(center) }
        read("pressed")
        onNodeWithTag("b").performTouchInput { up() }
        s.state = MutantButtonState.Loading
        read("loading")
        s.state = MutantButtonState.Success
        read("success")
        s.state = MutantButtonState.Idle
        read("rest again")
        s.enabled = false
        read("disabled")
        s.enabled = true
        return out
    }

    @Test
    fun `a button given no width keeps its width to the pixel through every state, at any density`() {
        for ((d, scale) in listOf(1f to 1f, 2f to 1f, 2f to 1.5f)) {
            for (icon in listOf<ImageVector?>(null, MutantIcons.Home)) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    val s = Stage()
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(d, scale)) {
                            MutantTheme {
                                Box {
                                    MutantButton(
                                        s.text,
                                        {},
                                        Modifier.testTag("b"),
                                        enabled = s.enabled,
                                        state = s.state,
                                        icon = icon,
                                    )
                                }
                            }
                        }
                    }
                    val at = "density $d, font scale $scale, icon ${icon?.name}"
                    val widths = widthsThroughStates(s)
                    assertEquals(1, widths.values.toSet().size, "one width through every state, $at: $widths")
                    // As wide as the widest state: the check (or the caller's icon), its gap and the label in the padding.
                    val label = onNodeWithText(s.text, useUnmergedTree = true).fetchSemanticsNode().size.width
                    val widest = label + ((24 + 8 + 2 * 24) * d).toInt()
                    assertTrue(
                        kotlin.math.abs(widths.getValue("rest") - widest) <= 1,
                        "as wide as Success, $at: ${widths.getValue("rest")} against $widest",
                    )

                    // A new label is a new measurement, kept through the states in its turn.
                    s.text = "Перемотка назад до начала"
                    val longer = widthsThroughStates(s)
                    assertEquals(
                        1,
                        longer.values.toSet().size,
                        "the new label's width through every state, $at: $longer",
                    )
                    assertTrue(longer.getValue("rest") > widths.getValue("rest"), "a longer label is wider, $at")
                    s.text = "Воспроизвести"
                    assertEquals(widths, widthsThroughStates(s), "the first label measures as before, $at")
                }
            }
        }
    }

    @Test
    fun `a button given no width keeps one body through every state`() {
        for (d in listOf(1f, 2f)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                val s = Stage()
                val built = mutableListOf<Pair<MutantButtonState, Instance>>()
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(d)) {
                        MutantTheme {
                            Box {
                                // MutantButton's own call, with the surface's probe: it adds the check on Success.
                                ButtonBody(
                                    text = s.text,
                                    onClick = {},
                                    modifier = Modifier.testTag("b"),
                                    enabled = s.enabled,
                                    state = s.state,
                                    icon = if (s.state == MutantButtonState.Success) MutantIcons.Done else null,
                                    key = s.text,
                                    interactionSource = null,
                                    label = MutantTheme.colors.onPrimary,
                                    outlined = false,
                                    checkRoom = true,
                                    probe = { built += s.state to it },
                                )
                            }
                        }
                    }
                }
                onNodeWithTag("b").assertIsDisplayed()
                widthsThroughStates(s)
                assertTrue(built.any { it.first == MutantButtonState.Success }, "Success drew its body at density $d")
                // The press focuses it, and focus calms the wear (0.3), which reshapes the body as at a fixed width: the
                // shape is compared between bodies of one wear, the size between all.
                for ((state, g) in built) {
                    val first = built.first { it.second.slot.calm == g.slot.calm }.second
                    assertEquals(first.slot.w, g.slot.w, "the body's width in $state at density $d")
                    assertEquals(first.slot.h, g.slot.h, "the body's height in $state at density $d")
                    assertTrue(first.shape.contentEquals(g.shape), "the body's shape in $state at density $d")
                    assertEquals(first.drips.size, g.drips.size, "the body's drips in $state at density $d")
                }
            }
        }
    }

    @Test
    fun `a width from the caller still wins, narrower or wider than the widest state`() {
        for (d in listOf(1f, 2f)) {
            for (given in listOf(120, 260)) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    val s = Stage()
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(d)) {
                            MutantTheme {
                                Box {
                                    MutantButton(
                                        s.text,
                                        {},
                                        Modifier.width(given.dp).testTag("b"),
                                        enabled = s.enabled,
                                        state = s.state,
                                    )
                                }
                            }
                        }
                    }
                    val widths = widthsThroughStates(s)
                    assertEquals(
                        setOf((given * d).toInt()),
                        widths.values.toSet(),
                        "the caller's $given dp at density $d: $widths",
                    )
                    // At rest the label stands in the middle of the caller's width, inside it.
                    val button = onNodeWithTag("b").fetchSemanticsNode().boundsInRoot
                    val label = onNodeWithText(s.text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                    assertTrue(label.left >= button.left && label.right <= button.right, "inside, $given dp: $label")
                    assertTrue(
                        kotlin.math.abs((label.left - button.left) - (button.right - label.right)) <= 1f,
                        "centred, $given dp at density $d: $label in $button",
                    )
                }
            }
        }
    }
}
