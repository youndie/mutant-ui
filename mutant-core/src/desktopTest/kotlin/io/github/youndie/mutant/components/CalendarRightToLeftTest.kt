package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-168 (F-pickers-03): right to left, the calendar's days stand under their own weekdays. The weekday row mirrored and
 * the day grid did not, so November 2026's 1st, a Sunday, stood under Monday; the drop's run and the arrow keys mirror
 * with the grid.
 */
@OptIn(ExperimentalTestApi::class)
class CalendarRightToLeftTest {
    private fun ComposeUiTest.middle(text: String): Offset {
        val n = onNodeWithText(text).fetchSemanticsNode()
        return n.positionInRoot + Offset(n.size.width / 2f, n.size.height / 2f)
    }

    private fun calendar(block: ComposeUiTest.(set: (Int) -> Unit) -> Unit) =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var day by mutableIntStateOf(5)
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    MutantTheme(MutantPalettes.Efir) {
                        Box(Modifier.size(330.dp, 380.dp).testTag("s")) {
                            MutantDatePicker(2026, 11, day, { day = it }, { _, _ -> })
                        }
                    }
                }
            }
            repeat(30) { mainClock.advanceTimeByFrame() }
            block { day = it }
        }

    @Test
    fun `right to left, every day stands under its own weekday`() =
        calendar {
            // November 2026 begins on a Sunday: the 1st is a Sunday, the 2nd to the 7th Monday to Saturday.
            val week = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
            for (d in 1..7) {
                val under = week[(d + 5) % 7]
                val dx = abs(middle("$d").x - middle(under).x)
                assertTrue(dx < 2f, "day $d stands ${"%.0f".format(dx)} px off $under")
            }
            assertTrue(middle("ВС").x < middle("ПН").x, "Sunday stands left of Monday")
        }

    @Test
    fun `right to left, the drop lands on the day picked and Right moves to the day on the right`() =
        calendar { set ->
            set(18)
            repeat(30) { mainClock.advanceTimeByFrame() }
            val px = onNodeWithTag("s").captureToImage().toPixelMap()
            var sx = 0.0
            var n = 0
            for (y in 0 until px.height) {
                for (x in 0 until px.width) {
                    val c = px[x, y]
                    if (c.green > .85f && c.red > .55f && c.blue < .3f) {
                        sx += x
                        n++
                    }
                }
            }
            val at = onNodeWithTag("s").fetchSemanticsNode().positionInRoot.x + sx / n
            assertTrue(abs(at - middle("18").x) < 4, "the lime stands at $at, the 18th at ${middle("18").x}")
            onNodeWithText("18").requestFocus()
            onNodeWithText("18").performKeyInput { pressKey(Key.DirectionRight) }
            mainClock.advanceTimeByFrame()
            onNodeWithText("17").assertIsFocused()
            assertTrue(middle("17").x > middle("18").x, "the 17th stands right of the 18th")
        }
}
