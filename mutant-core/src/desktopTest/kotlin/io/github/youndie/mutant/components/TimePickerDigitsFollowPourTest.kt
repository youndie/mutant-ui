package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
 * B-168 (F-pickers-04): a time field's digits turn when the pour's front passes the body's middle, not at the press.
 * They turned at the press while the acid took the pour's time to come or go: for about 20 frames the field being set
 * showed black digits on its dark fill and the one left light digits on acid, 1.06:1.
 *
 * What the fix guarantees, frame by frame: the digits are coloured for the fill under the body's middle, so every part
 * of them over that fill reads. The part over the other fill, while the front crosses the 52 sp digits, does not
 * (F-pickers-11: splitting the digits along the front needs the surface to give its content the front's path).
 */
@OptIn(ExperimentalTestApi::class)
class TimePickerDigitsFollowPourTest {
    private fun apart(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > .35f

    private fun lime(c: Color) = abs(c.red - ACID.red) + abs(c.green - ACID.green) + abs(c.blue - ACID.blue) < .35f

    /** What a field shows on one frame, read against its bare twin. */
    private class Read(
        /** The fill across the body's middle row where the digits stand: true acid, false not, null both. */
        val middle: Boolean?,
        /** The share of the digits' ink over that fill that reads under 1.5:1 on it. */
        val unreadableOverMiddle: Double,
        /** The share of all the digits' ink that reads under 1.5:1 on what lies under it. */
        val unreadable: Double,
    )

    private fun ComposeUiTest.read(): List<Read> {
        val whole = onNodeWithTag("W").captureToImage().toPixelMap()
        val bare = onNodeWithTag("B").captureToImage().toPixelMap()
        val origin = onNodeWithTag("W").fetchSemanticsNode().positionInRoot
        return listOf("h", "m").map { label ->
            val n = onNodeWithContentDescription(label).fetchSemanticsNode()
            val p = n.positionInRoot - origin
            read(whole, bare, p.x.toInt(), p.y.toInt(), n.size.width, n.size.height)
        }
    }

    private fun read(
        whole: PixelMap,
        bare: PixelMap,
        x0: Int,
        y0: Int,
        w: Int,
        h: Int,
    ): Read {
        // The surface reads the front between 16 dp from each side, against half the body's height (B-94).
        val row = (x0 + INSET until x0 + w - INSET).map { lime(bare[it, y0 + h / 2]) }.toSet()
        val middle = row.singleOrNull()
        var ink = 0
        var bad = 0
        var over = 0
        var badOver = 0
        for (y in y0 until y0 + h) {
            for (x in x0 until x0 + w) {
                if (!apart(whole[x, y], bare[x, y])) continue
                val unreadable = contrast(whole[x, y].copy(alpha = 1f), bare[x, y].copy(alpha = 1f)) < 1.5f
                ink++
                if (unreadable) bad++
                if (middle != null && lime(bare[x, y]) == middle) {
                    over++
                    if (unreadable) badOver++
                }
            }
        }
        return Read(
            middle,
            if (over == 0) 0.0 else badOver.toDouble() / over,
            if (ink == 0) 0.0 else bad.toDouble() / ink,
        )
    }

    @Test
    fun `a press on a time field pours its acid in`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(MutantPalettes.Efir) {
                        Box(Modifier.padding(40.dp).testTag("W")) {
                            TimePickerBody(21, 30, { _, _ -> }, Modifier, "h", "m", "tp")
                        }
                    }
                }
            }
            repeat(30) { mainClock.advanceTimeByFrame() }
            val minutes = onNodeWithContentDescription("m").fetchSemanticsNode()
            val at = minutes.positionInRoot - onNodeWithTag("W").fetchSemanticsNode().positionInRoot
            onNodeWithTag("W").performMouseInput {
                click(at + Offset(minutes.size.width / 2f, minutes.size.height / 2f))
                moveTo(Offset(-20f, -20f))
            }
            repeat(60) { mainClock.advanceTimeByFrame() }
            val px = onNodeWithTag("W").captureToImage().toPixelMap()
            val acid =
                listOf("h", "m").map { label ->
                    val n = onNodeWithContentDescription(label).fetchSemanticsNode()
                    val p = n.positionInRoot - onNodeWithTag("W").fetchSemanticsNode().positionInRoot
                    (0 until n.size.width).count { lime(px[p.x.toInt() + it, p.y.toInt() + 4]) }.toDouble() /
                        n.size.width
                }
            assertTrue(acid[0] < .2 && acid[1] > .6, "the acid across each field's top after the press: $acid")
        }

    @Test
    fun `a time field's digits read on the fill under the body's middle through each pour`() =
        listOf(false, true).forEach { steps ->
            runComposeUiTest {
                mainClock.autoAdvance = false
                var field by mutableIntStateOf(0)
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(MutantPalettes.Efir, steppedMotion = steps) {
                            Row(Modifier.padding(40.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Box(Modifier.testTag("W").padding(4.dp)) {
                                    TimePickerBody(21, 30, { _, _ -> }, Modifier, "h", "m", "tp", field = field)
                                }
                                Box(Modifier.testTag("B").padding(4.dp).clearAndSetSemantics {}) {
                                    TimePickerBody(21, 30, { _, _ -> }, Modifier, "", "", "tp", true, field)
                                }
                            }
                        }
                    }
                }
                repeat(30) { mainClock.advanceTimeByFrame() }
                for ((to, name) in listOf(1 to "the minutes", 0 to "the hours")) {
                    // The picker and its twin turn on the same frame, unpressed: their bodies pour alike.
                    field = to
                    val crossing = IntArray(2)
                    repeat(48) { f ->
                        mainClock.advanceTimeByFrame()
                        read().forEachIndexed { k, r ->
                            if (r.middle == null) {
                                crossing[k]++
                                return@forEachIndexed
                            }
                            assertTrue(
                                r.unreadableOverMiddle <= .03,
                                "stepped $steps, $name set, frame $f, field $k: " +
                                    "${"%.0f".format(
                                        r.unreadableOverMiddle * 100,
                                    )} % of the digits over the middle's " +
                                    "fill unreadable (${"%.0f".format(r.unreadable * 100)} % of all)",
                            )
                        }
                    }
                    // The front stands across the middle row for 3 to 6 frames of a pour (about 100 ms at most).
                    assertTrue(
                        crossing.all {
                            it <= 8
                        },
                        "stepped $steps, $name set: frames with the front across the middle ${crossing.toList()}",
                    )
                }
            }
        }

    private companion object {
        /** The theme's acid, `primary` in «Эфир». */
        val ACID = MutantPalettes.Efir.primary

        /** Where the surface reads the pour's front from each side, dp (`depthUnderLabel`). */
        const val INSET = 16
    }
}
