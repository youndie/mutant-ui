package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.contrast
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-168 (F-pickers-01): a day's number is black where the drop is drawn under it and light where the bar is, frame by
 * frame through the drop's run. It turned by a half-cell rule instead: the day the drop reached stood white on the lime
 * (1.06:1), a day it crossed white on lime (1.46:1), the day it left black on the dark bar.
 */
@OptIn(ExperimentalTestApi::class)
class CalendarNumbersOnDropTest {
    private fun apart(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > .35f

    /**
     * Per day, the share of its ink that reads under 1.5:1 against what lies under it, the ground read from a bare twin
     * (the same bodies, the words transparent).
     */
    private fun ComposeUiTest.unreadable(days: Int): Map<Int, Double> {
        val whole = onNodeWithTag("whole").captureToImage().toPixelMap()
        val bare = onNodeWithTag("bare").captureToImage().toPixelMap()
        val origin = onNodeWithTag("whole").fetchSemanticsNode().positionInRoot
        return (1..days).associateWith { k ->
            val n = onNodeWithText("$k").fetchSemanticsNode()
            val p = n.positionInRoot - origin
            share(whole, bare, (p.x + n.size.width / 2).toInt(), (p.y + n.size.height / 2).toInt())
        }
    }

    private fun share(
        whole: PixelMap,
        bare: PixelMap,
        cx: Int,
        cy: Int,
    ): Double {
        var ink = 0
        var bad = 0
        for (y in cy - 8..cy + 8) {
            for (x in cx - 9..cx + 9) {
                if (apart(whole[x, y], bare[x, y])) {
                    ink++
                    if (contrast(whole[x, y].copy(alpha = 1f), bare[x, y].copy(alpha = 1f)) < 1.5f) bad++
                }
            }
        }
        return if (ink == 0) 0.0 else bad.toDouble() / ink
    }

    @Test
    fun `every day's number reads on what is drawn under it through the drop's runs`() =
        listOf(false, true).forEach { steps ->
            runComposeUiTest {
                mainClock.autoAdvance = false
                var day by mutableIntStateOf(5)
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(MutantPalettes.Efir, steppedMotion = steps) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                MutantDatePicker(
                                    2026,
                                    11,
                                    day,
                                    { day = it },
                                    { _, _ -> },
                                    Modifier.testTag("whole"),
                                    key = "cal",
                                )
                                DatePickerBody(
                                    2026,
                                    11,
                                    day,
                                    {},
                                    { _, _ -> },
                                    Modifier.testTag("bare").clearAndSetSemantics {},
                                    List(12) { "НОЯБРЬ" },
                                    listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"),
                                    "",
                                    "",
                                    "cal",
                                    hidden = true,
                                )
                            }
                        }
                    }
                }
                repeat(30) { mainClock.advanceTimeByFrame() }
                // The black copy is hidden from a reader: the chosen day reads once.
                val read = onNodeWithText("5").fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
                assertEquals(listOf("5"), read, "the chosen day's cell reads")
                val rest = unreadable(30)
                assertTrue(rest.values.all { it == 0.0 }, "at rest every number reads: $rest")
                // Down two rows and across one; then a pick 128 ms into a run, and another 96 ms into that one.
                val picks = mapOf(0 to 18, 30 to 25, 38 to 3, 44 to 19)
                repeat(70) { f ->
                    picks[f]?.let { day = it }
                    mainClock.advanceTimeByFrame()
                    unreadable(30).forEach { (k, s) ->
                        assertTrue(
                            s <= .25,
                            "stepped $steps, frame $f: ${"%.0f".format(s * 100)} % of the ${k}th unreadable",
                        )
                    }
                }
            }
        }
}
