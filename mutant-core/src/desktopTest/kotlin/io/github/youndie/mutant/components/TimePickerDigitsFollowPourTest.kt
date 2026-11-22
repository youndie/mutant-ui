package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.contrast
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-168 (F-pickers-04): a time field's digits turn when the pour's front passes under them, not at the press. They
 * turned at the press while the acid took the pour's time to come or go: for about 20 frames the field being set
 * showed black digits on its dark fill and the one left light digits on acid, 1.06:1.
 */
@OptIn(ExperimentalTestApi::class)
class TimePickerDigitsFollowPourTest {
    private fun apart(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > .35f

    /** Per field, the share of its digits' ink that reads under 1.5:1 on what lies under it, read from a bare twin. */
    private fun ComposeUiTest.unreadable(): List<Double> {
        val whole = onNodeWithTag("W").captureToImage().toPixelMap()
        val bare = onNodeWithTag("B").captureToImage().toPixelMap()
        val origin = onNodeWithTag("W").fetchSemanticsNode().positionInRoot
        return listOf("h", "m").map { label ->
            val n = onNodeWithContentDescription(label).fetchSemanticsNode()
            val p = n.positionInRoot - origin
            share(whole, bare, p.x.toInt(), p.y.toInt(), p.x.toInt() + n.size.width, p.y.toInt() + n.size.height)
        }
    }

    private fun share(
        whole: PixelMap,
        bare: PixelMap,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
    ): Double {
        var ink = 0
        var bad = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                if (apart(whole[x, y], bare[x, y])) {
                    ink++
                    if (contrast(whole[x, y].copy(alpha = 1f), bare[x, y].copy(alpha = 1f)) < 1.5f) bad++
                }
            }
        }
        return if (ink == 0) 0.0 else bad.toDouble() / ink
    }

    /** Clicks a field ([i]: 0 the hours, 1 the minutes) of the picker and of its twin, then takes the mouse away. */
    private fun ComposeUiTest.pick(i: Int) {
        val n = onNodeWithContentDescription(if (i == 0) "h" else "m").fetchSemanticsNode()
        val w = onNodeWithTag("W").fetchSemanticsNode().positionInRoot
        val at = n.positionInRoot - w + Offset(n.size.width / 2f, n.size.height / 2f)
        onNodeWithTag("B").performMouseInput { click(at) }
        onNodeWithTag("W").performMouseInput {
            click(at)
            moveTo(Offset(-40f, -40f))
        }
    }

    @Test
    fun `a time field's digits read on what is drawn under them through each pour`() =
        listOf(false, true).forEach { steps ->
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(MutantPalettes.Efir, steppedMotion = steps) {
                            Row(Modifier.padding(40.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Box(Modifier.testTag("W").padding(4.dp)) {
                                    TimePickerBody(21, 30, { _, _ -> }, Modifier, "h", "m", "tp")
                                }
                                Box(Modifier.testTag("B").padding(4.dp).clearAndSetSemantics {}) {
                                    TimePickerBody(21, 30, { _, _ -> }, Modifier, "", "", "tp", hidden = true)
                                }
                            }
                        }
                    }
                }
                repeat(30) { mainClock.advanceTimeByFrame() }
                for ((field, name) in listOf(1 to "the minutes", 0 to "the hours")) {
                    pick(field)
                    repeat(48) { f ->
                        mainClock.advanceTimeByFrame()
                        unreadable().forEachIndexed { k, s ->
                            assertTrue(
                                s <= .4,
                                "stepped $steps, $name picked, frame $f: ${"%.0f".format(
                                    s * 100,
                                )} % of field $k unreadable",
                            )
                        }
                    }
                }
            }
        }
}
