package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.contrast
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-94: a chip's label turns as its fill's pour passes under its middle, on the screen clock as the pour runs. Under a
 * clock a dialog holds, the fill stands, and the label stands with it: no frame shows a light word on a lime fill.
 */
@OptIn(ExperimentalTestApi::class)
class ChipLabelFollowsFillTest {
    private fun worst(held: Boolean): Pair<Float, String> {
        var worst = Float.MAX_VALUE
        var at = ""
        runComposeUiTest {
            mainClock.autoAdvance = false
            var selected by mutableStateOf(true)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    MutantTheme(reduceMotion = false) {
                        MutantScreenClock {
                            Column(Modifier.size(400.dp, 300.dp).testTag("stage")) {
                                Box(Modifier.padding(24.dp)) { MutantChip("Компоненты", selected, {}) }
                                if (held) MutantDialog(true, "Стереть?", {}) { MutantDialogAction("Да", {}) }
                            }
                        }
                    }
                }
            }
            repeat(40) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(3)
            }
            val b = onNodeWithText("Компоненты", useUnmergedTree = true).getBoundsInRoot()
            val box = Rect(b.left.value * D, b.top.value * D, b.right.value * D, b.bottom.value * D)
            for (turn in 0 until 2) {
                selected = !selected
                repeat(60) {
                    mainClock.advanceTimeByFrame()
                    val c = readContrast(onNodeWithTag("stage").captureToImage().toPixelMap(), box)
                    if (c < worst) {
                        worst = c
                        at = "turn ${turn + 1}, frame ${it + 1}"
                    }
                }
            }
        }
        return worst to at
    }

    /** The label's ink against the fill 6 dp left of the word, at its middle. */
    private fun readContrast(
        px: PixelMap,
        box: Rect,
    ): Float {
        val ground = px[(box.left - 6 * D).roundToInt(), box.center.y.roundToInt()].copy(alpha = 1f)
        var best = 1f
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                best = maxOf(best, contrast(px[x, y].copy(alpha = 1f), ground))
            }
        }
        return best
    }

    @Test
    fun `a chip's label stands with its fill under a held clock`() {
        val (c, at) = worst(held = true)
        assertTrue(c >= 3f, "${"%.2f".format(c)}:1 at $at")
    }

    @Test
    fun `a chip's label follows its fill under a running clock`() {
        val (c, at) = worst(held = false)
        assertTrue(c >= 3f, "${"%.2f".format(c)}:1 at $at")
    }

    private companion object {
        const val D = 2f
    }
}
