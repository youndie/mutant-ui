package io.github.youndie.mutant.surface

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.tokens.MutantBody
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-87: a body off the screen builds no tick — a scrolled-away body, or one outside a box it is laid out in — and
 * ticks again once it shows, whether it was scrolled into view or a box grew to it.
 */
@OptIn(ExperimentalTestApi::class)
class OffScreenTicksTest {
    private class Count {
        var frames = 0
    }

    /** A 360 × [viewport] dp scrolled column with one card [gap] dp down it, and 3 000 dp under it to scroll by. */
    private fun ComposeUiTest.column(
        count: Count,
        gap: Int,
        scroll: ScrollState,
        viewport: () -> Int,
    ) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalMutantFrameProbe provides { _, _, _ -> count.frames++ },
            ) {
                ProvideMutantClock(remember { MutantClock() }) {
                    Box(Modifier.size(360.dp, viewport().dp)) {
                        Column(Modifier.verticalScroll(scroll)) {
                            Spacer(Modifier.height(gap.dp))
                            MutantSurface(MutantBody.Card, "card/b87", Modifier.size(328.dp, 80.dp)) {}
                            Spacer(Modifier.height(3_000.dp))
                        }
                    }
                }
            }
        }
    }

    /** The body's frames over [ms] of clock, in whole frames, real time between them for the builds ahead. */
    private fun ComposeUiTest.framesOver(
        count: Count,
        ms: Int,
    ): Int {
        val from = count.frames
        repeat(ms / 16) {
            mainClock.advanceTimeByFrame()
            Thread.sleep(4)
        }
        return count.frames - from
    }

    @Test
    fun `a body scrolled away builds no tick and ticks again once scrolled back`() =
        runComposeUiTest {
            val count = Count()
            val scroll = ScrollState(0)
            column(count, gap = 10, scroll) { 300 }
            framesOver(count, 1_000)
            val shown = framesOver(count, 2_000)
            assertTrue(shown >= 14, "on the screen, a tick's frame eight times a second: $shown in 2 s")
            runBlocking { scroll.scrollTo(2_000) }
            framesOver(count, 250)
            assertEquals(0, framesOver(count, 2_000), "frames of a body scrolled 2 000 dp away")
            runBlocking { scroll.scrollTo(0) }
            framesOver(count, 250)
            val back = framesOver(count, 2_000)
            assertTrue(back >= 14, "scrolled back, it ticks again: $back in 2 s")
        }

    @Test
    fun `a body a box grows to ticks again`() =
        runComposeUiTest {
            val count = Count()
            var viewport by mutableIntStateOf(300)
            // The test's window is 768 dp tall: the box grows inside it.
            column(count, gap = 500, ScrollState(0)) { viewport }
            framesOver(count, 1_000)
            assertEquals(0, framesOver(count, 2_000), "frames of a body 500 dp down a 300 dp box")
            // The box grows past the body. The body does not move; the box's layout places it again, and tells it.
            viewport = 700
            framesOver(count, 250)
            val grown = framesOver(count, 2_000)
            assertTrue(grown >= 14, "once the box reaches it, it ticks again: $grown in 2 s")
        }

    @Test
    fun `a body just past the edge, its drips in reach, still ticks`() =
        runComposeUiTest {
            val count = Count()
            // The card's top 40 dp under the box's bottom: inside its 64 dp reach.
            column(count, gap = 340, ScrollState(0)) { 300 }
            framesOver(count, 1_000)
            val near = framesOver(count, 2_000)
            assertTrue(near >= 14, "a body within its reach of the edge ticks: $near in 2 s")
        }
}
