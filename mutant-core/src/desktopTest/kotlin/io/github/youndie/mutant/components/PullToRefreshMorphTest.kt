package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-105: when refreshing ends the long tongue becomes the pill frame by frame, with no jump in the hanging body's size,
 * and after its second the pill is drawn back into the line, narrowing to nothing, instead of standing whole until the
 * line cuts it. The body is read from the pixels: its slime, lime or boiled amber, above the list's first row, and the
 * rows wide enough to be the body proper rather than its drips.
 */
@OptIn(ExperimentalTestApi::class)
class PullToRefreshMorphTest {
    /** The hanging body's box, dp at density 1, or null when none of it shows. */
    private class Box2(
        val w: Int,
        val h: Int,
        val top: Int,
    )

    private fun ComposeUiTest.body(): Box2? {
        val img = onNodeWithTag("s").captureToImage().toAwtImage()
        // Per row, the slime's span: green high and blue low, lime or amber; the page, the line and the words are not.
        // A row of the body proper has at least [SOLID] of it: the drips under it, a few dp wide each, step on the hot
        // tick as every drip of the kit does, and are not the body's size.
        var top = -1
        var bottom = -1
        var w = 0
        for (y in 0 until TOP_ROWS) {
            var x0 = Int.MAX_VALUE
            var x1 = -1
            var n = 0
            for (x in 0 until img.width) {
                val p = img.getRGB(x, y)
                if ((p shr 8 and 255) > 140 && (p and 255) < 110 && (p shr 16 and 255) > 120) {
                    n++
                    if (x < x0) x0 = x
                    if (x > x1) x1 = x
                }
            }
            if (n >= SOLID) {
                if (top < 0) top = y
                bottom = y
                w = maxOf(w, x1 - x0 + 1)
            }
        }
        return if (top < 0) null else Box2(w, bottom - top + 1, top)
    }

    private fun run(block: ComposeUiTest.(set: (Boolean) -> Unit) -> Unit) =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var refreshing by mutableStateOf(true)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        MutantScreenClock {
                            Box(Modifier.size(300.dp, 200.dp).background(MutantTheme.colors.background).testTag("s")) {
                                MutantPullToRefresh(
                                    refreshing,
                                    {},
                                    Modifier.fillMaxSize(),
                                    refreshingLabel = "REFRESHING",
                                    doneLabel = "REFRESHED",
                                ) {
                                    Column { repeat(3) { MutantListItem("Untitled ${it + 1}", key = "r$it") } }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1_500)
            block { refreshing = it }
        }

    @Test
    fun `the long tongue becomes the pill with no jump in its size`() =
        run { set ->
            val before = body() ?: error("no tongue while refreshing")
            set(false)
            var last = before
            val steps = ArrayList<String>()
            repeat(30) {
                mainClock.advanceTimeByFrame()
                val now = body() ?: error("the body is gone at ${mainClock.currentTime} ms")
                val dw = abs(now.w - last.w)
                val dh = abs(now.h - last.h)
                if (dw > MAX_STEP ||
                    dh > MAX_STEP
                ) {
                    steps += "${mainClock.currentTime} ms: ${last.w}×${last.h} → ${now.w}×${now.h}"
                }
                last = now
            }
            assertTrue(before.w > last.w + 10, "it narrowed from the tongue's ${before.w} to the pill's ${last.w}")
            assertTrue(
                steps.isEmpty(),
                "frames where the body jumped more than $MAX_STEP dp:\n" + steps.joinToString("\n"),
            )
        }

    @Test
    fun `after its second the pill narrows into the line and is gone`() =
        run { set ->
            set(false)
            mainClock.advanceTimeBy(DONE_MS.toLong() - FRAME_MS.toLong())
            val standing = body() ?: error("no pill standing")
            val widths = ArrayList<Int>()
            repeat(30) {
                mainClock.advanceTimeByFrame()
                val b = body()
                widths += b?.w ?: 0
                if (b != null) assertTrue(b.top >= 0, "it stays below the component's top: ${b.top}")
            }
            val going = widths.takeWhile { it > 0 }
            assertTrue(widths.last() == 0, "gone once back: $widths")
            assertTrue(going.zipWithNext().all { (a, b) -> b <= a }, "it only narrows: $widths")
            assertTrue(
                going.count { it in 1 until standing.w * 3 / 4 } >= 3,
                "it narrows towards nothing over several frames, from ${standing.w}: $widths",
            )
        }

    private companion object {
        /** The rows the body hangs in: above the list once the gap closes to the pill's. */
        const val TOP_ROWS = 72

        /** The least slime in a row of the body proper, dp. */
        const val SOLID = 24

        /** The most the body's width or height may change from one frame to the next, dp. */
        const val MAX_STEP = 8
        const val FRAME_MS = 16.0
    }
}
