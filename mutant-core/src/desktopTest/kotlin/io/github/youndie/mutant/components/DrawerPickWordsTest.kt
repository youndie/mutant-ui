package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-103: after a pick the words follow the pill. The new row's words turn black on the window, the old row's turn back
 * to the text's colour on the drawer's ground, read from the pixels a second after the pick.
 */
@OptIn(ExperimentalTestApi::class)
class DrawerPickWordsTest {
    private val c = MutantPalettes.Efir
    private val names = listOf("On air", "Queue", "Search", "Settings")

    private fun drawer(block: ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                var at by mutableIntStateOf(2)
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        MutantScreenClock {
                            Box(Modifier.size(360.dp, 320.dp).background(MutantTheme.colors.page)) {
                                MutantNavigationDrawer(
                                    true,
                                    {},
                                    names.mapIndexed { i, n -> MutantDrawerItem("CH 0${i + 1}", n) },
                                    at,
                                    { at = it },
                                )
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            block()
        }

    /** The share of a word's pixels within 40 of [ink] in every channel. */
    private fun ComposeUiTest.share(
        word: String,
        ink: Color,
    ): Double {
        val img = onNodeWithText(word, useUnmergedTree = true).captureToImage().toAwtImage()
        val r = (ink.red * 255).toInt()
        val g = (ink.green * 255).toInt()
        val b = (ink.blue * 255).toInt()
        var n = 0
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val p = img.getRGB(x, y)
                if (abs((p shr 16 and 255) - r) < 40 && abs((p shr 8 and 255) - g) < 40 &&
                    abs((p and 255) - b) < 40
                ) {
                    n++
                }
            }
        }
        return n.toDouble() / (img.width * img.height)
    }

    @Test
    fun `before a pick the chosen row's words are black and the others' light`() =
        drawer {
            val chosen = share("Search", c.onPrimary)
            val other = share("On air", c.text)
            assertTrue(chosen > .05, "the chosen row's words in black: $chosen")
            assertTrue(other > .05, "another row's words in the text's colour: $other")
        }

    @Test
    fun `after a pick the new row's words are black and the old row's light`() =
        drawer {
            onNodeWithText("On air", useUnmergedTree = true).performClick()
            mainClock.advanceTimeBy(1_000)
            val picked = share("On air", c.onPrimary)
            val left = share("Search", c.text)
            assertTrue(picked > .05, "the picked row's words in black on the window: $picked")
            assertTrue(left > .05, "the row left behind in the text's colour: $left")
        }
}
