package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-97: a bottom sheet 271 dp tall (the demo's share sheet) showed a wedge-shaped hole through its body, the screen
 * under it showing through. Shown from the first frame or risen later, its body is whole on every frame once up: no
 * pixel of the body's right half, where no word stands, is darker than its fill.
 */
@OptIn(ExperimentalTestApi::class)
class SheetWholeBodyTest {
    private fun holes(risen: Boolean): List<String> {
        val holes = ArrayList<String>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var shown by mutableStateOf(!risen)
            var fill = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false) {
                        val c = MutantTheme.colors.surface
                        fill = ((c.red + c.green + c.blue) * 255).toInt()
                        Box(Modifier.size(360.dp, 720.dp).background(MutantTheme.colors.background).testTag("p")) {
                            MutantScreenClock {
                                Column { repeat(10) { MutantListItem("Row $it", supporting = "under", key = "u$it") } }
                                MutantBottomSheet(shown, {}, windowInsets = WindowInsets(0), key = "demo-share") {
                                    for (i in 0 until 3) MutantListItem("Item $i", onClick = {}, key = "share$i")
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(800)
            shown = true
            // The rise takes 330 ms; then a second and a half of ticks.
            mainClock.advanceTimeBy(400)
            repeat(12) { tick ->
                mainClock.advanceTimeBy(125)
                val img = onNodeWithTag("p").captureToImage().toAwtImage()
                var dark = 0
                for (y in 560 until 700) {
                    for (x in 300 until 345) {
                        val p = img.getRGB(x, y)
                        if ((p shr 16 and 255) + (p shr 8 and 255) + (p and 255) < fill - 40) dark++
                    }
                }
                if (dark > 0) holes += "tick ${tick + 1}: $dark px"
            }
        }
        return holes
    }

    @Test
    fun `a sheet shown at once has a whole body`() = assertEquals(emptyList(), holes(risen = false))

    @Test
    fun `a sheet risen later has a whole body`() = assertEquals(emptyList(), holes(risen = true))
}
