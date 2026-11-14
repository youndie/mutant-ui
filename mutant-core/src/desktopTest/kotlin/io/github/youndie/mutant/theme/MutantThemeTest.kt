package io.github.youndie.mutant.theme

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** B-09: the theme's switches reach the bodies — reduce motion as the pause, the two palettes, the platform default. */
@OptIn(ExperimentalTestApi::class)
class MutantThemeTest {
    @Test
    fun `reduce motion follows the platform when not forced, off on desktop`() =
        runComposeUiTest {
            var seen: Boolean? = null
            setContent { MutantTheme { seen = MutantTheme.reduceMotion } }
            assertEquals(false, seen)
        }

    private fun frames(reduceMotion: Boolean): Pair<Int, Boolean> {
        var built = 0
        var changed = false
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduceMotion) {
                        MutantSurface(
                            MutantBody.Btn,
                            "btn/a",
                            Modifier.size(200.dp, 48.dp).testTag("b"),
                            probe = { built++ },
                        ) {}
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val first =
                onNodeWithTag("b").captureToImage().toPixelMap().let { px ->
                    List(px.width * px.height) {
                        px[
                            it %
                                px.width, it / px.width,
                        ]
                    }
                }
            val builtAfterFirst = built
            repeat(3000 / 16) { mainClock.advanceTimeByFrame() }
            val later =
                onNodeWithTag("b").captureToImage().toPixelMap().let { px ->
                    List(px.width * px.height) {
                        px[
                            it %
                                px.width, it / px.width,
                        ]
                    }
                }
            changed = first != later
            built -= builtAfterFirst
        }
        return built to changed
    }

    @Test
    fun `under reduce motion the body stands still and builds no new frame`() {
        val (built, changed) = frames(reduceMotion = true)
        assertEquals(0, built, "frames built after the first")
        assertEquals(false, changed, "the body changed while paused")
    }

    @Test
    fun `without reduce motion the body moves on the clock`() {
        val (built, changed) = frames(reduceMotion = false)
        assertTrue(built >= 20, "frames built in 3 s: $built")
        assertTrue(changed, "the body never changed")
    }

    private fun dominant(colors: MutantColors): Color {
        var mode: Color = Color.Unspecified
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(colors = colors, reduceMotion = true) {
                        MutantSurface(MutantBody.Card, "card/a", Modifier.size(328.dp, 80.dp).testTag("c")) {}
                    }
                }
            }
            val px = onNodeWithTag("c").captureToImage().toPixelMap()
            val counts = HashMap<Color, Int>()
            for (y in 20 until 60) for (x in 20 until 300) counts.merge(px[x, y], 1, Int::plus)
            mode = counts.maxBy { it.value }.key
        }
        return mode
    }

    @Test
    fun `the palette paints the body`() {
        val efir = dominant(MutantPalettes.Efir)
        val kopiya = dominant(MutantPalettes.Kopiya)
        assertEquals(MutantPalettes.Efir.surface, efir, "a card in «Эфир» is filled with its surface")
        assertEquals(MutantPalettes.Kopiya.surface, kopiya, "a card in «Копия» is filled with its surface")
        assertNotEquals(efir, kopiya)
    }
}
