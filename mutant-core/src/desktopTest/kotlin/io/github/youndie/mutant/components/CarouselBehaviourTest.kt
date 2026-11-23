package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.Still
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-47: what the carousel does — a press on a card beside brings it in, a press on the current one is the click. */
@OptIn(ExperimentalTestApi::class)
class CarouselBehaviourTest {
    private val items =
        listOf(
            MutantCarouselItem("Без подписи №3", "Сторона A · 46 мин"),
            MutantCarouselItem("Эфир, ночь на вторник", "Записано поверх · 12 мин", "CH 03 · SP"),
            MutantCarouselItem("Настроечная таблица", "Копия 4-го поколения"),
        )

    @Test
    fun `a press on the next card brings it to the middle, a press on the current one is the click`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var state: PagerState
            val clicks = ArrayList<Int>()
            setContent {
                Still {
                    state = rememberPagerState(initialPage = 1) { items.size }
                    MutantCarousel(items, Modifier.width(400.dp), state, onClick = { clicks += it })
                }
            }
            mainClock.advanceTimeByFrame()
            // A finger: a mouse press in the test window would focus the card, and the pager brings a focused card
            // into view by itself.
            onNode(hasText("Настроечная таблица")).performTouchInput { click() }
            // Under reduce motion (Still) the shelf moves at once: one frame.
            mainClock.advanceTimeByFrame()
            assertEquals(2, state.currentPage)
            assertEquals(emptyList(), clicks)
            onNode(hasText("Настроечная таблица")).performTouchInput { click() }
            mainClock.advanceTimeByFrame()
            assertEquals(listOf(2), clicks)
        }

    @Test
    fun `the cards are wide targets and the carousel says where it stands`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    MutantCarousel(
                        items,
                        Modifier.width(400.dp),
                        rememberPagerState(initialPage = 1) { items.size },
                    )
                }
            }
            mainClock.advanceTimeByFrame()
            val card = onNode(hasText("Эфир, ночь на вторник")).fetchSemanticsNode().boundsInRoot
            assertEquals(220f, card.width)
            assertEquals(120f, card.height)
            assertEquals(84f, card.left)
            val said =
                onAllNodes(
                    androidx.compose.ui.test.SemanticsMatcher
                        .keyIsDefined(SemanticsProperties.StateDescription),
                ).fetchSemanticsNodes()
                    .map { it.config[SemanticsProperties.StateDescription] }
            assertTrue("2 / 3" in said, "state descriptions $said")
        }

    @Test
    fun `B-168 the last card is reached and stands where the current one stands, at any width`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var width by mutableStateOf(400.dp)
            lateinit var state: PagerState
            val clicks = ArrayList<Int>()
            setContent {
                Still {
                    state = rememberPagerState(initialPage = 1) { items.size }
                    MutantCarousel(items, Modifier.width(width), state, onClick = { clicks += it })
                }
            }
            // 400 dp, the page's shelf; 328, the demo's column; 520, the sample's cell, where the shelf's travel was
            // shorter than a card past the middle one.
            for (w in listOf(400, 328, 520)) {
                width = w.dp
                mainClock.advanceTimeByFrame()
                state.requestScrollToPage(1)
                mainClock.advanceTimeByFrame()
                onNode(hasText("Настроечная таблица")).performTouchInput { click() }
                mainClock.advanceTimeByFrame()
                assertEquals(2, state.currentPage, "$w dp: the last card is the current one")
                val left = onNode(hasText("Настроечная таблица")).fetchSemanticsNode().positionInRoot.x
                assertEquals(84f, left, "$w dp: the last card's left")
                onNode(hasText("Настроечная таблица")).performTouchInput { click() }
                mainClock.advanceTimeByFrame()
                assertEquals(2, clicks.lastOrNull(), "$w dp: a press on it is the click")
                clicks.clear()
            }
        }

    @Test
    fun `B-168 the pointer slides to the next dot and stays lime on every frame`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = false) {
                    MutantCarousel(
                        items,
                        Modifier.width(400.dp).testTag("car"),
                        rememberPagerState(initialPage = 1) { items.size },
                    )
                }
            }
            mainClock.advanceTimeByFrame()

            // No frame moves the pointer more than half a dot's step (18 dp), nor leaves it under half its lime.
            // The dots' row: 16 dp under the 172 dp view, 10 tall; the lime is the pointer's fill (Эфир `primary`).
            fun lime(): Pair<Int, Double> {
                val map = onNodeWithTag("car").captureToImage().toPixelMap()
                var n = 0
                var sum = 0.0
                for (y in 184 until map.height) {
                    for (x in 0 until map.width) {
                        val c = map[x, y]
                        if (c.green > .8f && c.red > .5f && c.blue < .3f) {
                            n++
                            sum += x
                        }
                    }
                }
                return n to (if (n == 0) Double.NaN else sum / n)
            }
            val (rest, from) = lime()
            onNode(hasText("Настроечная таблица")).performTouchInput { click() }
            var last = from
            var least = rest
            var widest = 0.0
            repeat(50) { frame ->
                mainClock.advanceTimeByFrame()
                val (n, at) = lime()
                least = minOf(least, n)
                widest = maxOf(widest, kotlin.math.abs(at - last))
                assertTrue(n >= rest / 2, "frame $frame: $n lime pixels on the dots' row, $rest at rest")
                assertTrue(kotlin.math.abs(at - last) <= 9, "frame $frame: the pointer leapt from $last to $at")
                last = at
            }
            // It ends a dot's step (18 dp) on.
            assertTrue(kotlin.math.abs(last - from - 18) <= 1.5, "the pointer from $from ends at $last")
            println("B-168 pointer: rest $rest lime px, least $least; widest step ${"%.2f".format(widest)} px")
        }

    @Test
    fun `B-168 one notch of the wheel steps one card, a vertical one leaves the shelf`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var state: PagerState
            setContent {
                MutantTheme(reduceMotion = false) {
                    state = rememberPagerState(initialPage = 1) { items.size }
                    MutantCarousel(items, Modifier.width(400.dp).testTag("car"), state)
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithTag("car").performMouseInput { moveTo(Offset(200f, 60f)) }

            fun turn(
                delta: Float,
                wheel: ScrollWheel,
            ) {
                onNodeWithTag("car").performMouseInput { scroll(delta, wheel) }
                repeat(40) { mainClock.advanceTimeByFrame() }
            }
            turn(1f, ScrollWheel.Horizontal)
            assertEquals(2, state.currentPage, "a notch on")
            assertEquals(0f, state.currentPageOffsetFraction, .001f, "a notch on stops on the card")
            turn(-1f, ScrollWheel.Horizontal)
            assertEquals(1, state.currentPage, "a notch back")
            // A trackpad: three small deltas are not a notch, a fourth makes one.
            repeat(3) { turn(.3f, ScrollWheel.Horizontal) }
            assertEquals(1, state.currentPage, "0.9 of a notch")
            turn(.3f, ScrollWheel.Horizontal)
            assertEquals(2, state.currentPage, "1.2 notches")
            turn(-1f, ScrollWheel.Horizontal)
            turn(3f, ScrollWheel.Vertical)
            assertEquals(1, state.currentPage, "a vertical turn is the page's")
            // The pager's own wheel moved 0.047 of a card a notch.
            assertEquals(0f, state.currentPageOffsetFraction, .001f, "a vertical turn moves nothing")
        }

    @Test
    fun `B-168 a mouse drags the shelf with the pointer, with no jump by the slop, and clicks nothing`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var state: PagerState
            val clicks = ArrayList<Int>()
            setContent {
                MutantTheme(reduceMotion = false) {
                    state = rememberPagerState(initialPage = 1) { items.size }
                    MutantCarousel(items, Modifier.width(400.dp).testTag("car"), state, onClick = { clicks += it })
                }
            }
            mainClock.advanceTimeByFrame()

            fun next() = onNode(hasText("Настроечная таблица")).fetchSemanticsNode().positionInRoot.x
            val start = next()
            // On the current card, then 12 px left a frame for 15 frames.
            onNodeWithTag("car").performMouseInput {
                moveTo(Offset(200f, 60f))
                press()
            }
            mainClock.advanceTimeByFrame()
            var last = start
            var widest = 0f
            repeat(15) {
                onNodeWithTag("car").performMouseInput { moveBy(Offset(-12f, 0f)) }
                mainClock.advanceTimeByFrame()
                val at = next()
                widest = maxOf(widest, last - at)
                // Following the pointer is 12 px a frame; the slop's whole 18 px added to one frame made it 24 or more.
                assertTrue(last - at in 0f..18f, "frame $it: the next card moved from $last to $at")
                last = at
            }
            // The pointer went 180 px; the shelf follows all but the slop.
            assertTrue(start - last > 140f, "the shelf moved ${start - last} px under a 180 px drag")
            onNodeWithTag("car").performMouseInput { release() }
            repeat(40) { mainClock.advanceTimeByFrame() }
            assertEquals(2, state.currentPage, "the drag settles on the next card")
            assertEquals(0f, state.currentPageOffsetFraction, .001f, "and stops on it")
            assertEquals(emptyList(), clicks, "a drag is no click")
            println("B-168 mouse drag: moved ${start - last} px, widest step $widest px")
        }
}
