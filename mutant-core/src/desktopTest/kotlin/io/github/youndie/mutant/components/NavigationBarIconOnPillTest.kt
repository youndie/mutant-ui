package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-168 (F-nav-bar-01): an icon is black where the pill is drawn under it and in the label's colour where the bar is,
 * frame by frame through a slide. It took its colour from the pick: in «Эфир» the chosen icon stood black on the dark
 * bar, 1.45:1, until the pill arrived 330 ms later, and the icons it left and passed stood pale on lime, 1.6:1.
 */
@OptIn(ExperimentalTestApi::class)
class NavigationBarIconOnPillTest {
    private val four =
        listOf(
            MutantNavigationItem("Эфир", MutantIcons.Home),
            MutantNavigationItem("Очередь", MutantIcons.Schedule),
            MutantNavigationItem("Поиск", MutantIcons.Search),
            MutantNavigationItem("Настройка", MutantIcons.Settings),
        )

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + .05) / (min(la, lb) + .05)
    }

    private fun apart(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > .35f

    /**
     * Per cell of the 328 dp bar, the median contrast of the icon's ink against what lies under it, read from a bare
     * twin (the same body, the icons and words transparent); null where a cell shows too little ink to read.
     */
    private fun ComposeUiTest.icons(): List<Double?> {
        val whole = onNodeWithTag("whole").captureToImage().toPixelMap()
        val bare = onNodeWithTag("bare").captureToImage().toPixelMap()
        return List(4) { c -> median(whole, bare, 41 + 82 * c - 11, 15) }
    }

    private fun median(
        whole: PixelMap,
        bare: PixelMap,
        x0: Int,
        y0: Int,
    ): Double? {
        val ink =
            (y0 until y0 + 22).flatMap { y ->
                (x0 until x0 + 22).mapNotNull { x ->
                    if (apart(whole[x, y], bare[x, y])) contrast(whole[x, y], bare[x, y]) else null
                }
            }
        return if (ink.size < 20) null else ink.sorted()[ink.size / 2]
    }

    @Test
    fun `every icon reads on what is drawn under it through a slide across the bar`() =
        listOf(false, true).forEach { steps ->
            runComposeUiTest {
                mainClock.autoAdvance = false
                var at by mutableIntStateOf(0)
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(MutantPalettes.Efir, steppedMotion = steps) {
                            Column {
                                NavigationBarBody(four, at, {}, Modifier.width(328.dp).testTag("whole"), "n", "p")
                                NavigationBarBody(
                                    four,
                                    at,
                                    {},
                                    Modifier.width(328.dp).testTag("bare").clearAndSetSemantics {},
                                    "n",
                                    "p",
                                    hidden = true,
                                )
                            }
                        }
                    }
                }
                mainClock.advanceTimeBy(500)
                val rest = icons()
                assertTrue(rest.all { it != null && it > 3 }, "at rest every icon reads: $rest")
                at = 3
                repeat(30) { f ->
                    mainClock.advanceTimeByFrame()
                    val read = icons()
                    read.forEachIndexed { c, k ->
                        assertTrue(
                            k == null || k >= 2.5,
                            "stepped $steps, frame $f, cell $c reads ${"%.2f".format(k)}:1",
                        )
                    }
                }
            }
        }
}
