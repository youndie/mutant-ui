package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.qa.Still
import io.github.youndie.mutant.qa.near
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** B-44: what the search bar does — typing, clearing, searching, suggestions, the match and the rows' geometry. */
@OptIn(ExperimentalTestApi::class)
class SearchBarBehaviourTest {
    private val field = hasSetTextAction()

    @Test
    fun `the clear icon shows with a query, empties it and keeps the field's focus`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var query by mutableStateOf("")
            setContent { Still { MutantSearchBar(query, { query = it }, Modifier.width(320.dp)) } }
            mainClock.advanceTimeByFrame()
            onNode(field).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(
                0,
                onAllNodes(hasContentDescription("Очистить"), useUnmergedTree = true).fetchSemanticsNodes().size,
            )
            onNode(field).performTextInput("ночь")
            mainClock.advanceTimeByFrame()
            assertEquals("ночь", query)
            onNodeWithContentDescription("Очистить", useUnmergedTree = true).assertExists()
            onNode(
                hasClickAction().and(
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button),
                ),
            ).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals("", query)
            onNode(field).assertIsFocused()
            assertEquals(
                0,
                onAllNodes(hasContentDescription("Очистить"), useUnmergedTree = true).fetchSemanticsNodes().size,
            )
        }

    @Test
    fun `the keyboard's action is search, heard with the query`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val heard = ArrayList<String>()
            setContent { Still { MutantSearchBar("ночь", {}, Modifier.width(320.dp), onSearch = { heard += it }) } }
            mainClock.advanceTimeByFrame()
            onNode(field).performClick()
            onNode(field).performImeAction()
            mainClock.advanceTimeByFrame()
            assertEquals(listOf("ночь"), heard)
        }

    @Test
    fun `Tab reaches the clear icon after the field, its brackets show and Enter clears`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var query by mutableStateOf("ночь")
            setContent {
                Still {
                    Box(
                        Modifier.size(400.dp, 120.dp),
                    ) { MutantSearchBar(query, { query = it }, Modifier.width(320.dp)) }
                }
            }
            mainClock.advanceTimeByFrame()
            val clear =
                onNode(
                    hasClickAction().and(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.Role,
                            androidx.compose.ui.semantics.Role.Button,
                        ),
                    ),
                )
            val rest = clear.captureFocusColour(onRoot())
            onNode(field).performClick()
            mainClock.advanceTimeByFrame()
            onNode(field).performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
            clear.assertIsFocused()
            val focused = clear.captureFocusColour(onRoot())
            assertTrue(
                rest == 0 && focused > 0,
                "focus-coloured pixels around the clear icon: rest $rest, focused $focused",
            )
            clear.performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            assertEquals("", query)
        }

    @Test
    fun `a suggestion hands its words over, and a disabled bar clears nothing`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val heard = ArrayList<String>()
            var query by mutableStateOf("ночь")
            setContent {
                Still {
                    androidx.compose.foundation.layout.Column {
                        MutantSearchBar(query, { query = it }, Modifier.width(320.dp), enabled = false)
                        MutantSearchSuggestions(listOf("ночь на вторник", "ночной эфир"), { heard += it })
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNode(hasText("ночной эфир")).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(listOf("ночной эфир"), heard)
            val clear = onNodeWithContentDescription("Очистить", useUnmergedTree = true).fetchSemanticsNode()
            assertTrue(
                clear.config.getOrNull(SemanticsActions.OnClick) == null ||
                    clear.config.contains(SemanticsProperties.Disabled),
            )
            assertEquals("ночь", query)
        }

    /**
     * B-168: a chip takes focus when pressed by a mouse or the keyboard, and the field lost it to every suggestion taken;
     * where the suggestions leave as the query fills, as the demo's do, nothing was focused at all.
     */
    @Test
    fun `a suggestion taken by the mouse or the keyboard hands focus back to the field`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var query by mutableStateOf("")
            var keep by mutableStateOf(false)
            setContent {
                Still {
                    MutantSearchBar(query, { query = it }, Modifier.width(320.dp)) {
                        if (keep || query.isEmpty()) {
                            MutantSearchSuggestions(listOf("ночь на вторник", "ночной эфир"), { query = it })
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            onNode(field).performMouseInput { click(center) }
            mainClock.advanceTimeByFrame()
            onNode(field).assertIsFocused()
            // The suggestions leave as the query fills.
            onNode(hasText("ночной эфир") and hasClickAction()).performMouseInput { click(center) }
            mainClock.advanceTimeByFrame()
            assertEquals("ночной эфир", query)
            assertEquals(0, onAllNodes(hasText("ночь на вторник")).fetchSemanticsNodes().size)
            onNode(field).assertIsFocused()
            // The suggestions stay, taken from the keyboard.
            keep = true
            mainClock.advanceTimeByFrame()
            onNode(hasText("ночь на вторник") and hasClickAction())
                .performSemanticsAction(SemanticsActions.RequestFocus)
            mainClock.advanceTimeByFrame()
            onNode(hasText("ночь на вторник") and hasClickAction()).assertIsFocused()
            onNode(hasText("ночь на вторник") and hasClickAction()).performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            assertEquals("ночь на вторник", query)
            onNode(field).assertIsFocused()
        }

    @Test
    fun `the match is drawn in the accent, the first one, case aside`() {
        val accent = Color.Red
        val m = searchMatch("Эфир, Ночь на вторник, ночь", "ночь", accent)
        assertEquals("Эфир, Ночь на вторник, ночь", m.text)
        assertEquals(1, m.spanStyles.size)
        val span = m.spanStyles.single()
        assertEquals(6 until 10, span.start until span.end)
        assertEquals(SpanStyle(color = accent), span.item)
        assertTrue(searchMatch("Эфир", "ночь", accent).spanStyles.isEmpty())
        assertTrue(searchMatch("Эфир", "  ", accent).spanStyles.isEmpty())
    }

    @Test
    fun `rows as the page's - results and loading 60 dp, suggestion rows 40 dp apart, chip targets 48 dp`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    androidx.compose.foundation.layout.Column(Modifier.width(200.dp)) {
                        MutantSearchResult(
                            "Эфир, ночь",
                            "ночь",
                            Modifier.testTagged("r"),
                            leadingIcon = MutantIcons.Search,
                        )
                        MutantSearchResultLoading(Modifier.testTagged("l"))
                        MutantSearchSuggestions(listOf("ночь на вторник", "ночной эфир"), {}, Modifier.testTagged("s"))
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            val r = onNode(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "r")).fetchSemanticsNode().size
            val l = onNode(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "l")).fetchSemanticsNode().size
            val s = onNode(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "s")).fetchSemanticsNode().size
            val chips = onAllNodes(hasClickAction()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(60, r.height)
            assertEquals(60, l.height)
            // Two rows at 200 dp: 32 + 8 + 32 drawn.
            assertEquals(72, s.height)
            assertEquals(2, chips.size)
            assertEquals(40f, chips[1].top - chips[0].top)
            assertTrue(chips.all { it.height >= 48f }, "chip targets $chips")
        }

    @Test
    fun `nothing else is announced as a clear button`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { Still { MutantSearchBar("", {}, Modifier.width(320.dp)) } }
            mainClock.advanceTimeByFrame()
            assertNull(
                onAllNodes(
                    hasContentDescription("Очистить"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().firstOrNull(),
            )
        }
}

private fun Modifier.testTagged(tag: String) = this.then(Modifier.testTag(tag))

/** Pixels in the theme's focus colour within 6 dp around the node, in the whole window's capture. */
@OptIn(ExperimentalTestApi::class)
private fun androidx.compose.ui.test.SemanticsNodeInteraction.captureFocusColour(
    root: androidx.compose.ui.test.SemanticsNodeInteraction,
): Int {
    val b = fetchSemanticsNode().boundsInRoot
    val m = root.captureToImage().toPixelMap()
    val focus = MutantPalettes.Efir.focus
    var n = 0
    for (y in (b.top.toInt() - 6).coerceAtLeast(0) until (b.bottom.toInt() + 6).coerceAtMost(m.height)) {
        for (x in (b.left.toInt() - 6).coerceAtLeast(0) until (b.right.toInt() + 6).coerceAtMost(m.width)) {
            if (near(m[x, y], focus)) n++
        }
    }
    return n
}
