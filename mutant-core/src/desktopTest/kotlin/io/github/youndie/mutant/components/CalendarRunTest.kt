package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.contrast
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-93: picking a day runs the drop from the old day to the new one through both axes, as the navigation bar's pill
 * slides; the numbers it leaves, crosses and reaches read on every frame; under reduce motion it lands at once.
 */
@OptIn(ExperimentalTestApi::class)
class CalendarRunTest {
    private class Run(
        val centres: List<Offset>,
        val worst: Float,
        val old: Offset,
        val new: Offset,
    )

    private fun ComposeUiTest.centreOf(day: String): Offset =
        onNodeWithText(day, useUnmergedTree = true).getBoundsInRoot().let {
            Offset(((it.left + it.right) / 2).value * D, ((it.top + it.bottom) / 2).value * D)
        }

    private fun run(reduce: Boolean): Run {
        val centres = ArrayList<Offset>()
        var worst = Float.MAX_VALUE
        var old = Offset.Zero
        var new = Offset.Zero
        runComposeUiTest {
            mainClock.autoAdvance = false
            var day by mutableStateOf(5)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    MutantTheme(reduceMotion = reduce) {
                        Box(Modifier.size(330.dp, 380.dp).testTag("s")) {
                            MutantDatePicker(2026, 11, day, { day = it }, { _, _ -> })
                        }
                    }
                }
            }
            repeat(40) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(3)
            }
            old = centreOf("5")
            new = centreOf("18")
            val crossed = centreOf("12")
            day = 18
            repeat(30) {
                mainClock.advanceTimeByFrame()
                val px = onNodeWithTag("s").captureToImage().toPixelMap()
                centres += limeCentre(px)
                for (c in listOf(old, new, crossed)) worst = minOf(worst, numberContrast(px, c))
            }
        }
        return Run(centres, worst, old, new)
    }

    /** The middle of the drop's lime, px. */
    private fun limeCentre(px: PixelMap): Offset {
        var sx = 0.0
        var sy = 0.0
        var n = 0
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val c = px[x, y]
                if (c.green > .85f && c.red > .55f && c.blue < .3f) {
                    sx += x
                    sy += y
                    n++
                }
            }
        }
        return if (n == 0) Offset.Unspecified else Offset((sx / n).toFloat(), (sy / n).toFloat())
    }

    /** A number's ink against the ground 13 dp left of its middle, inside its 36 dp cell. */
    private fun numberContrast(
        px: PixelMap,
        c: Offset,
    ): Float {
        val ground = px[(c.x - 13 * D).roundToInt(), c.y.roundToInt()].copy(alpha = 1f)
        var best = 1f
        for (y in (c.y - 6 * D).roundToInt()..(c.y + 6 * D).roundToInt()) {
            for (x in (c.x - 8 * D).roundToInt()..(c.x + 8 * D).roundToInt()) {
                best = maxOf(best, contrast(px[x, y].copy(alpha = 1f), ground))
            }
        }
        return best
    }

    @Test
    fun `the drop runs to the new day through both axes and lands on it`() {
        val r = run(reduce = false)
        val between =
            r.centres.filter { it != Offset.Unspecified }.filter {
                it.x < r.old.x - 4 && it.x > r.new.x + 4 && it.y > r.old.y + 4 && it.y < r.new.y - 4
            }
        assertTrue(between.size >= 2, "frames with the drop between the two days on both axes: ${between.size}")
        val last = r.centres.last()
        assertTrue((last - r.new).getDistance() < 6 * D, "it lands on the new day: $last against ${r.new}")
    }

    @Test
    fun `the days it leaves, crosses and reaches read on every frame`() {
        val r = run(reduce = false)
        assertTrue(r.worst >= 3f, "the worst number on a frame: ${"%.2f".format(r.worst)}:1")
    }

    @Test
    fun `under reduce motion the drop is on the new day at once`() {
        val r = run(reduce = true)
        val first = r.centres.first()
        assertTrue((first - r.new).getDistance() < 6 * D, "the first frame: $first against ${r.new}")
        assertEquals(0, r.centres.count { (it - r.new).getDistance() >= 6 * D }, "never anywhere else")
    }

    @Test
    fun `B-168 a pick while the drop runs starts the new run from where the drop stands`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var day by mutableStateOf(5)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    MutantTheme {
                        Box(Modifier.size(330.dp, 380.dp).testTag("s")) {
                            MutantDatePicker(2026, 11, day, { day = it }, { _, _ -> })
                        }
                    }
                }
            }
            repeat(40) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(3)
            }
            val three = centreOf("3")
            day = 25
            var last = limeCentre(onNodeWithTag("s").captureToImage().toPixelMap())
            repeat(36) { f ->
                // The second pick lands 128 ms into the first run, a little over half the way there.
                if (f == 8) day = 3
                mainClock.advanceTimeByFrame()
                val at = limeCentre(onNodeWithTag("s").captureToImage().toPixelMap())
                val step = (at - last).getDistance()
                // An eased run moves the drop at most ~23 px a frame here; the jump back to the 25th's cell was ~85 px.
                assertTrue(step < 30f, "frame $f: the drop leapt ${"%.0f".format(step)} px")
                last = at
            }
            assertTrue((last - three).getDistance() < 6 * D, "it lands on the 3rd: $last against $three")
        }

    private companion object {
        const val D = 2f
    }
}
