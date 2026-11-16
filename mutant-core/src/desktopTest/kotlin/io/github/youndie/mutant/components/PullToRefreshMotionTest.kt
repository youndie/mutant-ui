package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-95: the content's offset moves on eased curves of the screen clock, every frame, with no jump between the phases
 * (pull, let go short, let go past the line, refreshing, done, back). Density 1: a dp is a pixel.
 */
@OptIn(ExperimentalTestApi::class)
class PullToRefreshMotionTest {
    /** The steepest a glide of [distance] dp moves in [ms]: the eased-out cubic's slope at its start, 3 / duration. */
    private fun steepest(
        distance: Float,
        ms: Double,
    ): Double = 3.0 * abs(distance) / GLIDE_MS * ms

    private fun ComposeUiTest.screen(reduceMotion: Boolean = false): (Boolean) -> Unit {
        var refreshing by mutableStateOf(false)
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    Box(Modifier.size(320.dp, 400.dp)) {
                        // A caller refreshing as soon as it is asked, as a screen's state does.
                        MutantPullToRefresh(refreshing, { refreshing = true }) {
                            Column(Modifier.verticalScroll(rememberScrollState()).testTag("list")) {
                                repeat(20) { BasicText("Строка $it", Modifier.size(300.dp, 40.dp)) }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        return { refreshing = it }
    }

    /** The gap the content stands under, dp: the list's top less the 2 dp tear line. */
    private fun ComposeUiTest.gap(): Float = onNodeWithTag("list").fetchSemanticsNode().positionInRoot.y - 2f

    private fun ComposeUiTest.pull(by: Float) {
        onNodeWithTag("list").performTouchInput {
            down(Offset(150f, 60f))
            var left = by
            while (left > 0) {
                moveBy(Offset(0f, minOf(20f, left)))
                left -= 20f
            }
        }
        mainClock.advanceTimeByFrame()
    }

    /** The gap at each of [frames] frames from now. */
    private fun ComposeUiTest.frames(frames: Int): List<Float> =
        List(frames) {
            mainClock.advanceTimeByFrame()
            gap()
        }

    /**
     * Asserts that [gaps], read a frame apart, go from [from] to [to] without a step steeper than the curve allows
     * (plus a pixel of rounding), through at least one frame between, and land.
     */
    private fun assertGlides(
        phase: String,
        from: Float,
        to: Float,
        gaps: List<Float>,
    ) {
        val bound = steepest(to - from, FRAME_MS) + 1
        var last = from
        gaps.forEachIndexed { i, g ->
            assertTrue(
                abs(g - last) <= bound,
                "$phase: frame $i stepped ${g - last} dp (from $last to $g), over $bound",
            )
            last = g
        }
        assertTrue(gaps.any { abs(it - from) > 1 && abs(it - to) > 1 }, "$phase: no frame between $from and $to: $gaps")
        assertEquals(to, gaps.last(), "$phase: landed")
    }

    @Test
    fun `let go short, the list glides back up, every frame within the curve`() =
        runComposeUiTest {
            screen()
            pull(100f)
            val held = gap()
            assertTrue(held in 40f..60f, "held at $held")
            onNodeWithTag("list").performTouchInput { up() }
            assertGlides("back", held, 0f, frames(FRAMES))
        }

    @Test
    fun `let go past the line, it glides to 74, done to 52, then back to 0, every frame within the curve`() =
        runComposeUiTest {
            val set = screen()
            pull(MAX_PULL * 2 + 40f)
            val held = gap()
            assertEquals(MAX_PULL, held, "held at the most")
            onNodeWithTag("list").performTouchInput { up() }
            assertGlides("to the long tongue", held, REFRESH_AT, frames(FRAMES))
            set(false)
            assertGlides("to the pill", REFRESH_AT, DONE_GAP, frames(FRAMES))
            // The pill stands its second from the end of refreshing, then the list goes back up: no frame jumps.
            val rest = frames(((DONE_MS + GLIDE_MS) / FRAME_MS).toInt() + 4)
            val standing = rest.takeWhile { it == DONE_GAP }
            val stood = (FRAMES + standing.size) * FRAME_MS
            assertTrue(stood in DONE_MS - 2 * FRAME_MS..DONE_MS + 2 * FRAME_MS, "the pill stood $stood ms")
            assertGlides("back", DONE_GAP, 0f, rest.drop(standing.size))
        }

    @Test
    fun `after its second the pill rides back up with the list, and no tongue grows in the closing gap`() =
        runComposeUiTest {
            val set = screen()
            set(true)
            mainClock.advanceTimeBy(GLIDE_MS.toLong() * 2)
            set(false)
            mainClock.advanceTimeBy(DONE_MS.toLong() + 6 * FRAME_MS.toLong())
            val riding = gap()
            assertTrue(riding > 0f && riding < DONE_GAP, "on the way back: $riding")
            assertTrue(shows("ОБНОВЛЕНО"), "the pill rides up with the list")
            mainClock.advanceTimeBy(GLIDE_MS.toLong())
            assertEquals(0f, gap())
            assertTrue(!shows("ОБНОВЛЕНО"), "gone once back")
        }

    private fun ComposeUiTest.shows(word: String): Boolean =
        onAllNodes(
            androidx.compose.ui.test
                .hasText(word),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `under reduce motion every move is at once`() =
        runComposeUiTest {
            val set = screen(reduceMotion = true)
            pull(60f)
            onNodeWithTag("list").performTouchInput { up() }
            mainClock.advanceTimeByFrame()
            assertEquals(0f, gap(), "let go short: back at once")
            pull(MAX_PULL * 2 + 40f)
            onNodeWithTag("list").performTouchInput { up() }
            mainClock.advanceTimeByFrame()
            assertEquals(REFRESH_AT, gap(), "let go past the line: at 74 at once")
            set(false)
            mainClock.advanceTimeByFrame()
            assertEquals(DONE_GAP, gap(), "done: at 52 at once")
            mainClock.advanceTimeBy(DONE_MS.toLong() + 2 * FRAME_MS.toLong())
            assertEquals(0f, gap(), "after the pill: back at once")
        }

    @Test
    fun `the glide is continuous over every millisecond, across a change of phase half way`() {
        val glide = PullGlide(0f)
        glide.follow(false, 0.0)
        val gaps = mutableListOf<Float>()
        glide.hold(MAX_PULL)
        // Let go at 1000 toward the long tongue; refreshing ends half way, and the pill's gap is taken up from there.
        for (t in 1000..3000) {
            val now = t.toDouble()
            val target = if (t < 1150) REFRESH_AT else DONE_GAP
            glide.toward(target, now, still = false)
            gaps += glide.at(now, still = false)
        }
        val bound = steepest(MAX_PULL - DONE_GAP, 1.0) + 1e-3
        gaps.zipWithNext().forEachIndexed { i, (a, b) ->
            assertTrue(abs(b - a) <= bound, "ms ${1000 + i}: stepped ${b - a} dp, over $bound")
        }
        assertEquals(MAX_PULL, gaps.first())
        assertEquals(DONE_GAP, gaps.last())
    }

    @Test
    fun `the glide eases out, landing at rest`() {
        val glide = PullGlide(0f)
        glide.toward(REFRESH_AT, 0.0, still = false)
        // The eased-out cubic: seven eighths of the way at half time, and its last frame moves under a twentieth of a dp.
        assertEquals(REFRESH_AT * .875f, glide.at(GLIDE_MS / 2, still = false), 1e-3f)
        val lastFrame = glide.at(GLIDE_MS, still = false) - glide.at(GLIDE_MS - FRAME_MS, still = false)
        assertTrue(lastFrame < .05f, "the last frame moved $lastFrame dp")
    }

    @Test
    fun `the tongue grows out of nothing from 16 dp, with no step of its size`() {
        assertEquals(0f, tongueSize(SHOWN_FROM, 34f).width, "nothing at 16 dp")
        assertEquals(0f, tongueSize(SHOWN_FROM, 34f).height, "nothing at 16 dp")
        var last = tongueSize(0f, 34f)
        var gap = 0f
        while (gap <= MAX_PULL) {
            val s = tongueSize(gap, 34f)
            // The steepest the page's sizes allow over 0.01 dp: the short tongue's 64 dp grown over 24 dp.
            assertTrue(abs(s.width - last.width) <= .03f, "width at $gap: ${last.width} to ${s.width}")
            assertTrue(abs(s.height - last.height) <= .03f, "height at $gap: ${last.height} to ${s.height}")
            last = s
            gap += .01f
        }
    }

    private companion object {
        const val FRAME_MS = 16.0
        val FRAMES = (GLIDE_MS / FRAME_MS).toInt() + 3
    }
}
