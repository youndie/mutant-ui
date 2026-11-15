package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.qa.Still
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
}
