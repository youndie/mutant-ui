package io.github.youndie.mutant.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCarousel
import io.github.youndie.mutant.components.MutantCarouselItem
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantDataTable
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantDrawerItem
import io.github.youndie.mutant.components.MutantDropdown
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantHandCursor
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationDrawer
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSearchBar
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchSuggestions
import io.github.youndie.mutant.components.MutantSegmentedButton
import io.github.youndie.mutant.components.MutantSlider
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTableColumn
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-63: the hand over every live control, none over a disabled one, and a body that simmers under the mouse. */
@OptIn(ExperimentalTestApi::class)
class HoverAndCursorTest {
    private val hand = SemanticsMatcher.expectValue(MutantHandCursor, true)

    private val controls: List<Pair<String, @Composable (Boolean) -> Unit>> =
        listOf(
            "Button" to { e -> MutantButton("Пуск", {}, enabled = e) },
            "OutlinedButton" to { e -> MutantOutlinedButton("Пуск", {}, enabled = e) },
            "TextButton" to { e -> MutantTextButton("Пуск", {}, enabled = e) },
            "IconButton" to { e -> MutantIconButton(MutantIcons.Search, "Поиск", {}, enabled = e) },
            "Fab" to { _ -> MutantFab(MutantIcons.Add, "Добавить", {}) },
            "Chip" to { e -> MutantChip("Чип", false, {}, enabled = e) },
            "Checkbox" to { e -> MutantCheckbox(false, {}, label = "Галка", enabled = e) },
            "RadioButton" to { e -> MutantRadioButton(false, {}, label = "Радио", enabled = e) },
            "Switch" to { e -> MutantSwitch(false, {}, label = "Тумблер", enabled = e) },
            "Card" to
                { e -> MutantCard(Modifier.width(240.dp), onClick = {}, enabled = e) { MutantCardTitle("Карта") } },
            "ListItem" to { e -> MutantListItem("Строка", Modifier.width(300.dp), onClick = {}, enabled = e) },
            "Tabs" to { e -> MutantTabs(listOf(MutantTab("Один", e)), 0, {}) },
            "NavigationBar" to { e ->
                MutantNavigationBar(
                    listOf(MutantNavigationItem("Дом", MutantIcons.Home, enabled = e)),
                    0,
                    {},
                    Modifier.width(300.dp),
                )
            },
            "Snackbar" to { e ->
                MutantSnackbar("Сохранено", Modifier.width(300.dp), visible = e, actionLabel = "ОТМЕНА", onAction = {})
            },
            "SegmentedButton" to { e -> MutantSegmentedButton(listOf("SP", "LP"), 0, {}, enabled = e) },
            "Slider" to { e -> MutantSlider(0, {}, Modifier.width(240.dp), enabled = e) },
            "SearchBar" to { e -> MutantSearchBar("ночь", {}, Modifier.width(300.dp), enabled = e) },
            "NavigationDrawer" to
                { _ -> MutantNavigationDrawer(true, {}, listOf(MutantDrawerItem("CH 01", "Эфир")), 0, {}) },
            "Dropdown" to { _ -> MutantDropdown("Настройка", listOf("ТРЕКИНГ"), 0, {}) },
            "Carousel" to { _ -> MutantCarousel(listOf(MutantCarouselItem("Эфир")), Modifier.width(300.dp)) },
            "DataTable" to { e ->
                MutantDataTable(
                    listOf("а"),
                    listOf(
                        MutantTableColumn<String>("ЗАПИСЬ") {
                            it
                        },
                    ),
                    Modifier.width(300.dp),
                    emptySet(),
                    { _, _ -> },
                    enabled = e,
                )
            },
            "SearchSuggestions" to { e -> MutantSearchSuggestions(listOf("ночь"), {}, enabled = e) },
            "SearchResult" to { e -> MutantSearchResult("Эфир, ночь", "ночь", onClick = {}, enabled = e) },
        )

    private fun hands(
        enabled: Boolean,
        content: @Composable (Boolean) -> Unit,
    ): Int {
        var n = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { Still { Box(Modifier.size(400.dp, 300.dp)) { content(enabled) } } }
            mainClock.advanceTimeByFrame()
            n = onAllNodes(hand, useUnmergedTree = true).fetchSemanticsNodes().size
        }
        return n
    }

    @Test
    fun `every live control shows the hand, and a disabled one the arrow`() {
        val missing = controls.filter { (_, c) -> hands(true, c) == 0 }.map { it.first }
        // The FAB and the carousel have no disabled state; the dialog's action is held with the dialog open.
        val disabledWithHand =
            controls
                .filter { (n, c) ->
                    n !in setOf("Fab", "Carousel", "Dropdown", "NavigationDrawer") && hands(false, c) > 0
                }.map { it.first }
        assertEquals(emptyList(), missing, "no hand")
        assertEquals(emptyList(), disabledWithHand, "a hand over a disabled control")
    }

    /** How often the button's picture changes over a second of 16 ms frames. */
    private fun ComposeUiTest.changesASecond(): Int {
        fun px(): List<Color> =
            onNodeWithTag("b").captureToImage().toPixelMap().let { m ->
                List(m.width * m.height) {
                    m[
                        it %
                            m.width, it / m.width,
                    ]
                }
            }
        var n = 0
        var last = px()
        repeat(63) {
            mainClock.advanceTimeByFrame()
            val now = px()
            if (now != last) n++
            last = now
        }
        return n
    }

    /** The button's pixels in its acid fill. */
    private fun ComposeUiTest.lime(): Int {
        val acid = io.github.youndie.mutant.tokens.MutantPalettes.Efir.primary
        val m = onNodeWithTag("b").captureToImage().toPixelMap()
        return (0 until m.width * m.height).count {
            val c = m[it % m.width, it / m.width]
            kotlin.math.abs(c.red - acid.red) < .03f && kotlin.math.abs(c.green - acid.green) < .03f &&
                kotlin.math.abs(c.blue - acid.blue) < .03f
        }
    }

    private fun ComposeUiTest.button() {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme { Box(Modifier.size(300.dp, 200.dp)) { MutantButton("Пуск", {}, Modifier.testTag("b")) } }
            }
        }
        mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun `under the mouse the body simmers at the hot tick, and settles once the mouse leaves`() =
        runComposeUiTest {
            button()
            val limeRest = lime()
            val rest = changesASecond()
            onNodeWithTag("b").performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(400)
            val hovered = changesASecond()
            // Its heat shows in the fill: the acid lime turns a third of the way to amber, so few pixels stay lime.
            val limeHovered = lime()
            onNodeWithTag("b").performMouseInput { moveTo(Offset(-100f, -100f)) }
            mainClock.advanceTimeBy(600)
            val after = changesASecond()
            assertTrue(
                rest <= 9 && hovered >= 11 && after <= 9,
                "changes a second: rest $rest, hovered $hovered, after $after",
            )
            assertTrue(limeHovered * 2 < limeRest, "lime pixels at rest $limeRest, under the mouse $limeHovered")
        }

    @Test
    fun `a finger's press leaves no simmer behind`() =
        runComposeUiTest {
            button()
            onNodeWithTag("b").performTouchInput {
                down(center)
                up()
            }
            mainClock.advanceTimeBy(1_000)
            val after = changesASecond()
            assertTrue(after <= 9, "changes a second after a tap: $after")
        }
}
