package io.github.youndie.mutant.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import io.github.youndie.mutant.qa.Still
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.sqrt
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

    /** The next card's left, per frame after Tab from the current card: (ms since the key, px). */
    private fun tabGlide(
        reduce: Boolean,
        steps: Boolean,
    ): List<Pair<Long, Float>> {
        val shots = ArrayList<Pair<Long, Float>>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = reduce, steppedMotion = steps) {
                    MutantCarousel(items, Modifier.width(400.dp), rememberPagerState(initialPage = 1) { items.size })
                }
            }
            repeat(5) { mainClock.advanceTimeByFrame() }
            val current = onNode(hasText("Эфир, ночь на вторник"))
            current.performSemanticsAction(SemanticsActions.RequestFocus)
            repeat(5) { mainClock.advanceTimeByFrame() }

            fun next() = onNode(hasText("Настроечная таблица")).fetchSemanticsNode().positionInRoot.x
            assertEquals(318f, next(), "the next card before the key")
            val t0 = mainClock.currentTime
            current.performKeyInput { pressKey(Key.Tab) }
            repeat(40) {
                mainClock.advanceTimeByFrame()
                shots += (mainClock.currentTime - t0) to next()
            }
        }
        return shots
    }

    @Test
    fun `B-188 Tab to the next card glides the shelf over 330 ms on the slide curve`() {
        // The slide: the card stands 234 dp short of 84 by (1 − p)², p = elapsed / 330 (ease-out²). Each frame on the
        // way names when the glide began; they agree within a few ms on the curve, and the pager's own bring-into-view
        // (234 dp in four or five frames, a spring) spread them over 200 ms.
        val shots = tabGlide(reduce = false, steps = false)
        val way = shots.filter { (_, x) -> x - 84f in 20f..214f }
        assertTrue(way.size >= 10, "frames on the way: $shots")
        val begun = way.map { (t, x) -> t - (1 - sqrt((x - 84.0) / 234)) * SLIDE }
        val spread = begun.max() - begun.min()
        assertTrue(spread <= 10, "the glide's start by frame ${begun.map { "%.0f".format(it) }}: spread $spread ms")
        val start = begun.average()
        for ((t, x) in shots) {
            val p = ((t - start) / SLIDE).coerceIn(0.0, 1.0)
            val want = 84 + 234 * (1 - p) * (1 - p)
            assertTrue(
                abs(x - want) <= 3,
                "at ${t - start} ms of the glide the next card stands at $x, the slide $want",
            )
        }
        // On every frame (D22): no two frames on the way stand alike.
        assertEquals(way.size, way.map { it.second }.toSet().size, "a frame on the way stood still: $way")
        // It lands, and stays, 84 dp in; not before 300 ms.
        val landed = shots.first { it.second == 84f }.first - start
        assertTrue(landed in 300.0..350.0, "landed $landed ms into the glide")
        assertTrue(shots.dropWhile { it.second != 84f }.all { it.second == 84f }, "it stays: $shots")
        println("B-188 Tab glide: start spread ${"%.1f".format(spread)} ms, landed at ${"%.0f".format(landed)} ms")
    }

    @Test
    fun `B-188 Tab glides in the hot tick's steps under steppedMotion and lands at once under reduce motion`() {
        // steppedMotion (D22): 330 ms in 83 ms steps stands at no more than five places on the way.
        val stepped = tabGlide(reduce = false, steps = true)
        val places = stepped.map { it.second }.distinct()
        assertTrue(places.size in 3..5, "under the steps the next card stood at $places")
        assertEquals(84f, stepped.last().second)
        // Reduce motion (D24): the finished frame at once, nothing between.
        val still = tabGlide(reduce = true, steps = false)
        assertEquals(listOf(84f), still.map { it.second }.distinct().filter { it != 318f }, "under the pause: $still")
        assertTrue(still.take(2).any { it.second == 84f }, "under the pause it lands in the key's frame: $still")
    }

    @Test
    fun `B-188 at widths from 400 down to 200 dp the current card is whole and min(220, w - 84) wide`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var width by mutableStateOf(400.dp)
            setContent {
                Still {
                    MutantCarousel(items, Modifier.width(width), rememberPagerState(initialPage = 1) { items.size })
                }
            }
            for (w in (400 downTo 200 step 8) + listOf(304, 303, 260, 201)) {
                width = w.dp
                mainClock.advanceTimeByFrame()
                // Its own size and place, not its bounds in the root: those are cut by the view, so a card running out of
                // it read as whole.
                val node = onNode(hasText("Эфир, ночь на вторник")).fetchSemanticsNode()
                val card = Rect(node.positionInRoot, node.size.toSize())
                assertEquals(84f, card.left, "$w dp: the current card's left")
                assertEquals(minOf(220f, w - 84f), card.width, "$w dp: the current card's width")
                assertTrue(card.right <= w, "$w dp: the current card ends at ${card.right}")
                // The next card keeps its 14 dp gap after it, where it is in view (from 318 dp; the pager places no card
                // out of view).
                if (card.right + 14f < w) {
                    val next = onNode(hasText("Настроечная таблица")).fetchSemanticsNode().positionInRoot.x
                    assertEquals(card.right + 14f, next, "$w dp: the next card")
                }
            }
        }

    @Test
    fun `B-188 eight cards show a counter and no dots, seven show seven dots`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var count by mutableStateOf(8)
            lateinit var state: PagerState
            setContent {
                Still {
                    state = rememberPagerState { count }
                    MutantCarousel(
                        List(count) { MutantCarouselItem("Кассета ${it + 1}") },
                        Modifier.width(400.dp).testTag("car"),
                        state,
                    )
                }
            }
            mainClock.advanceTimeByFrame()

            // Under the 172 dp view and its 16 dp gap: the dots' row, 10 dp tall, or the counter's line.
            fun under(): List<androidx.compose.ui.graphics.Color> {
                val map = onNodeWithTag("car").captureToImage().toPixelMap()
                return (188 until map.height).flatMap { y -> (0 until map.width).map { x -> map[x, y] } }
            }

            fun lime(c: androidx.compose.ui.graphics.Color) = c.green > .8f && c.red > .5f && c.blue < .3f

            fun osd(c: androidx.compose.ui.graphics.Color) =
                abs(c.red - MutantPalettes.Efir.textOsd.red) < .05f &&
                    abs(c.green - MutantPalettes.Efir.textOsd.green) < .05f &&
                    abs(c.blue - MutantPalettes.Efir.textOsd.blue) < .05f && c.alpha > .95f

            // The pointer is a lime capsule 26 dp wide: its rows hold runs of lime a dozen pixels long. A lone lime pixel is
            // not a pointer: on the CI runner's macOS two pixels of the counter's anti-aliased edge read as lime (B-188),
            // where this Mac's read none, so the longest run is what tells a pointer from an edge.
            fun limeRun(): Int {
                val map = onNodeWithTag("car").captureToImage().toPixelMap()
                var longest = 0
                for (y in 188 until map.height) {
                    var run = 0
                    for (x in 0 until map.width) {
                        run = if (lime(map[x, y])) run + 1 else 0
                        longest = maxOf(longest, run)
                    }
                }
                return longest
            }

            // The dots' runs on their middle line, 5 dp into the row.
            fun runs(): Int {
                val map = onNodeWithTag("car").captureToImage().toPixelMap()
                var n = 0
                var ink = false
                for (x in 0 until map.width) {
                    val now = map[x, 193].alpha > .5f
                    if (now && !ink) n++
                    ink = now
                }
                return n
            }
            onNode(hasText("01/08"), useUnmergedTree = true).assertExists()
            val lone = under().count(::lime)
            assertTrue(limeRun() < POINTER_RUN, "eight cards: no pointer under the view, a lime run of ${limeRun()} px")
            assertTrue(under().count(::osd) > 20, "eight cards: the counter in the OSD colour")
            state.requestScrollToPage(2)
            mainClock.advanceTimeByFrame()
            onNode(hasText("03/08"), useUnmergedTree = true).assertExists()

            count = 7
            state.requestScrollToPage(0)
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertEquals(
                0,
                onAllNodes(hasText("/07", substring = true), useUnmergedTree = true).fetchSemanticsNodes().size,
            )
            assertEquals(0, under().count(::osd), "seven cards: no counter")
            assertTrue(limeRun() >= POINTER_RUN, "seven cards: the pointer, a lime run of ${limeRun()} px")
            println("B-188 counter: $lone lime px under eight cards; seven cards' longest lime run ${limeRun()} px")
            assertEquals(7, runs(), "seven cards: six dots and the pointer")
        }

    @Test
    fun `B-188 a card focused below the fold of a scrolling page is still brought into view`() =
        runComposeUiTest {
            // The node that takes the card's request from the pager passes its place on to the page round the carousel.
            mainClock.autoAdvance = false
            val scroll = ScrollState(0)
            lateinit var state: PagerState
            setContent {
                Still {
                    Column(Modifier.height(300.dp).verticalScroll(scroll)) {
                        Spacer(Modifier.height(600.dp))
                        state = rememberPagerState(initialPage = 1) { items.size }
                        MutantCarousel(items, Modifier.width(400.dp), state)
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            assertEquals(0, scroll.value)
            onNode(hasText("Настроечная таблица")).performSemanticsAction(SemanticsActions.RequestFocus)
            repeat(30) { mainClock.advanceTimeByFrame() }
            assertEquals(2, state.currentPage, "the shelf went to the focused card")
            // The card's foot, 600 + 8 + 120 dp down, inside the 300 dp page.
            assertTrue(scroll.value >= 428, "the page scrolled ${scroll.value} dp, the card's foot needs 428")
        }

    private companion object {
        /** The slide's duration (tokens `motion.slide`). */
        const val SLIDE = 330.0

        /** A lime run this long is the pointer's body (26 dp wide), not an edge's pixel or two. */
        const val POINTER_RUN = 8
    }
}
