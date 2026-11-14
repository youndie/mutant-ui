package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-16: what the icon button and the FABs do, and what a consumer does to them. Clocks are driven by hand. */
@OptIn(ExperimentalTestApi::class)
class IconButtonBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) {
                px[
                    it % px.width, it /
                        px.width,
                ]
            }
        }

    @Test
    fun `each fires on release, the disabled icon button never`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks by mutableIntStateOf(0)
            setContent {
                MutantTheme {
                    Column {
                        MutantIconButton(MutantIcons.Share, "Поделиться", { clicks++ }, Modifier.testTag("i"))
                        MutantFab(MutantIcons.Add, "Добавить", { clicks++ }, Modifier.testTag("f"))
                        MutantExtendedFab("Запись", MutantIcons.Add, { clicks++ }, Modifier.testTag("x"))
                        MutantIconButton(
                            MutantIcons.Share,
                            "Нельзя",
                            { clicks++ },
                            Modifier.testTag("d"),
                            enabled = false,
                        )
                    }
                }
            }
            for (tag in listOf("i", "f", "x")) {
                val before = clicks
                onNodeWithTag(tag).performTouchInput { down(center) }
                mainClock.advanceTimeByFrame()
                assertEquals(before, clicks, "$tag pressed, not released")
                onNodeWithTag(tag).performTouchInput { up() }
                mainClock.advanceTimeByFrame()
                assertEquals(before + 1, clicks, "$tag released")
            }
            onNodeWithTag("d").assertIsNotEnabled().performClick()
            assertEquals(3, clicks)
            onNodeWithContentDescription("Поделиться").assertIsDisplayed()
            onNodeWithContentDescription("Добавить").assertIsDisplayed()
        }

    @Test
    fun `the icon darkens while held, as its body boils to amber`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme { Box { MutantIconButton(MutantIcons.Share, null, {}, Modifier.testTag("i")) } }
                }
            }
            mainClock.advanceTimeByFrame()

            // The icon's own pixels: the centre column of the 48 dp body, where the share arrow's shaft stands.
            fun shaft(): Color = pixels("i").let { px -> px[18 * 48 + 24] }
            val rest = shaft()
            onNodeWithTag("i").performTouchInput { down(center) }
            repeat(30) { mainClock.advanceTimeByFrame() }
            val held = shaft()
            val text = efir().text
            val ink = efir().onPrimary
            assertTrue(near(rest, text), "at rest the icon is the text's colour: $rest")
            assertTrue(near(held, ink), "held, the icon is the ink: $held")
        }

    private fun efir() = io.github.youndie.mutant.tokens.MutantPalettes.Efir

    private fun near(
        a: Color,
        b: Color,
    ): Boolean =
        maxOf(kotlin.math.abs(a.red - b.red), kotlin.math.abs(a.green - b.green), kotlin.math.abs(a.blue - b.blue)) <
            .1f

    @Test
    fun `the icon button is 48 dp square, the FAB 56, the extended FAB 56 tall`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme {
                    Column {
                        MutantIconButton(MutantIcons.Share, null, {}, Modifier.testTag("i"))
                        MutantFab(MutantIcons.Add, null, {}, Modifier.testTag("f"))
                        MutantExtendedFab("Да", MutantIcons.Add, {}, Modifier.testTag("x"))
                    }
                }
            }
            onNodeWithTag("i").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
            onNodeWithTag("f").assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp)
            onNodeWithTag("x").assertWidthIsAtLeast(56.dp).assertHeightIsEqualTo(56.dp)
        }

    private fun frames(reduceMotion: Boolean): Boolean {
        var changed = false
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(
                    reduceMotion = reduceMotion,
                ) { MutantFab(MutantIcons.Add, null, {}, Modifier.testTag("f")) }
            }
            mainClock.advanceTimeByFrame()
            val a = pixels("f")
            repeat(2000 / 16) { mainClock.advanceTimeByFrame() }
            changed = a != pixels("f")
        }
        return changed
    }

    @Test
    fun `reduce motion is the pause - the FAB stands, and drips on while the clock runs`() {
        assertTrue(frames(reduceMotion = false), "running")
        assertTrue(!frames(reduceMotion = true), "paused")
    }

    @Test
    fun `the extended FAB takes a long label, no width and a doubled font`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Запись с начала первой стороны без подписи"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme {
                        Column {
                            MutantExtendedFab(long, MutantIcons.Add, {}, Modifier.width(0.dp).testTag("zero"))
                            MutantExtendedFab(long, MutantIcons.Add, {}, Modifier.width(160.dp).testTag("long"))
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("long").assertIsDisplayed().assertHeightIsEqualTo(56.dp)
            pixels("long")
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                assertTrue(node.boundsInRoot.height <= 56f, "label ${node.boundsInRoot}")
            }
        }
}
