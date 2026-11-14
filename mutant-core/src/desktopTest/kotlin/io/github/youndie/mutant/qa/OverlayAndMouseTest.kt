package io.github.youndie.mutant.qa

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-40, the overlay, hidden-input and mouse axes: what a dialog lets through, what a mouse click does on every
 * control, where a click on the text field lands, where its caret stands, and what right-to-left does.
 */
@OptIn(ExperimentalTestApi::class)
class OverlayAndMouseTest {
    @Test
    fun `nothing under an open dialog takes the wheel or a key`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var open by mutableStateOf(true)
            var under by mutableIntStateOf(0)
            val scroll = ScrollState(0)
            setContent {
                Still {
                    Box(Modifier.size(360.dp, 600.dp)) {
                        Column(Modifier.verticalScroll(scroll)) {
                            MutantButton("Под", { under++ }, Modifier.size(360.dp, 48.dp))
                            repeat(30) { MutantListItem("Строка $it") }
                        }
                        MutantDialog(open, "Стереть?", { open = false }) { MutantDialogAction("Да", {}) }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            onRoot().performMouseInput {
                moveTo(Offset(10f, 300f))
                scroll(5f)
            }
            press(Key.PageDown)
            press(Key.DirectionDown)
            press(Key.Spacebar)
            assertEquals(0, scroll.value, "nothing scrolls under the dialog")
            assertEquals(0, under, "no key reaches what is under it")
        }

    @Test
    fun `a second click on the scrim while the dialog closes reaches nothing under it`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var open by mutableStateOf(true)
            var under by mutableIntStateOf(0)
            setContent {
                MutantTheme {
                    Box(Modifier.size(360.dp, 600.dp)) {
                        MutantButton("Под", { under++ }, Modifier.size(360.dp, 48.dp))
                        MutantDialog(open, "Стереть?", { open = false }) { MutantDialogAction("Да", {}) }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            // The close takes 200 ms: the second click lands inside it.
            onRoot().performMouseInput { click(Offset(10f, 20f)) }
            mainClock.advanceTimeByFrame()
            assertEquals(false, open, "the first click dismisses")
            onRoot().performMouseInput { click(Offset(10f, 20f)) }
            mainClock.advanceTimeByFrame()
            assertEquals(0, under, "the second reaches nothing under it")
        }

    @Test
    fun `a closing snackbar's action takes neither a click nor focus`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var visible by mutableStateOf(true)
            var actions by mutableIntStateOf(0)
            setContent {
                CompositionLocalProvider {
                    MutantTheme {
                        Box(Modifier.size(360.dp, 200.dp)) {
                            MutantSnackbar(
                                "Сохранено",
                                Modifier.width(360.dp),
                                visible = visible,
                                actionLabel = "ОТМЕНА",
                                onAction = { actions++ },
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(400)
            visible = false
            mainClock.advanceTimeByFrame()
            onRoot().performMouseInput { click(Offset(320f, 24f)) }
            press(Key.Tab)
            press(Key.Enter)
            assertEquals(0, actions)
        }

    private class Target(
        val name: String,
        val content: @Composable (() -> Unit) -> Unit,
    )

    private val targets =
        listOf(
            Target("Button") { f -> MutantButton("Пуск", f, Modifier.testTag("t")) },
            Target("OutlinedButton") { f -> MutantOutlinedButton("Пуск", f, Modifier.testTag("t")) },
            Target("TextButton") { f -> MutantTextButton("Пуск", f, Modifier.testTag("t")) },
            Target("IconButton") { f -> MutantIconButton(MutantIcons.Search, "Поиск", f, Modifier.testTag("t")) },
            Target("Fab") { f -> MutantFab(MutantIcons.Add, "Добавить", f, Modifier.testTag("t")) },
            Target("Chip") { f -> MutantChip("Чип", false, f, Modifier.testTag("t")) },
            Target("Checkbox") { f -> MutantCheckbox(false, { f() }, Modifier.testTag("t"), label = "Галка") },
            Target("RadioButton") { f -> MutantRadioButton(false, f, Modifier.testTag("t"), label = "Радио") },
            Target("Switch") { f -> MutantSwitch(false, { f() }, Modifier.testTag("t"), label = "Тумблер") },
            Target(
                "Card",
            ) { f -> MutantCard(Modifier.width(240.dp).testTag("t"), onClick = f) { MutantCardTitle("Карта") } },
            Target("ListItem") { f -> MutantListItem("Строка", Modifier.width(300.dp).testTag("t"), onClick = f) },
            Target("Tabs") { f -> MutantTabs(listOf(MutantTab("Один")), 0, { f() }, Modifier.testTag("t")) },
            Target("NavigationBar") { f ->
                MutantNavigationBar(listOf(MutantNavigationItem("Дом", MutantIcons.Home)), 0, {
                    f()
                }, Modifier.width(300.dp).testTag("t"))
            },
            Target(
                "Snackbar",
            ) { f ->
                MutantSnackbar(
                    "Сохранено",
                    Modifier.width(300.dp).testTag("t"),
                    actionLabel = "ОТМЕНА",
                    onAction = f,
                )
            },
        )

    @Test
    fun `a left click fires every control, a right click or a press dragged off does not`() {
        val failures = mutableListOf<String>()
        for (t in targets) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                var clicks by mutableIntStateOf(0)
                setContent { Still { Box(Modifier.size(400.dp, 300.dp)) { t.content { clicks++ } } } }
                mainClock.advanceTimeBy(100)
                val node = onNodeWithTag("t")
                // The snackbar's action is at its end; everything else answers at its centre.
                val at = if (t.name == "Snackbar") Offset(280f, 24f) else node.fetchSemanticsNode().boundsInRoot.center
                onRoot().performMouseInput {
                    moveTo(at)
                    press(MouseButton.Secondary)
                    release(MouseButton.Secondary)
                }
                mainClock.advanceTimeBy(100)
                if (clicks != 0) failures += "${t.name}: a right click fired"
                onRoot().performMouseInput {
                    moveTo(at)
                    press()
                    moveTo(Offset(at.x, 290f))
                    release()
                }
                mainClock.advanceTimeBy(100)
                if (clicks != 0) failures += "${t.name}: a press dragged off fired"
                onRoot().performMouseInput { click(at) }
                mainClock.advanceTimeBy(100)
                if (clicks != 1) failures += "${t.name}: a left click fired $clicks times"
            }
        }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun `F04 a click anywhere on the text field's body focuses it`() {
        val missed = mutableListOf<Offset>()
        for (y in listOf(4f, 12f, 26f, 40f, 48f)) {
            for (x in listOf(4f, 20f, 120f, 236f)) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    setContent {
                        Still {
                            Box(
                                Modifier.size(360.dp, 200.dp),
                            ) { MutantTextField("abc", {}, Modifier.width(240.dp), prefix = "CH") }
                        }
                    }
                    mainClock.advanceTimeBy(100)
                    onRoot().performMouseInput { click(Offset(x, y)) }
                    mainClock.advanceTimeBy(100)
                    if (focusedBounds().isEmpty()) missed += Offset(x, y)
                }
            }
        }
        assertEquals(emptyList(), missed)
    }

    private val text = MutantPalettes.Efir.text

    @Test
    fun `F05 the caret stands where the next character goes`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var value by mutableStateOf("abc")
            setContent {
                Still {
                    Box(
                        Modifier.size(360.dp, 100.dp),
                    ) { MutantTextField(value, { value = it }, Modifier.width(240.dp).testTag("f")) }
                }
            }
            mainClock.advanceTimeBy(100)
            val glyphs =
                onNodeWithTag("f").captureToImage().toPixelMap().let { px ->
                    (0 until px.width).filter { x -> (0 until px.height).any { y -> near(px[x, y], text) } }
                }
            press(Key.Tab)
            // Focused by Tab, the cursor is at the end: the block stands past the text, and a letter goes there.
            val atTab = onNodeWithTag("f").captureToImage().toPixelMap().columnsOf(text)
            assertTrue(atTab.isNotEmpty() && atTab.first() > 30, "the block past 'abc': $atTab")
            onNode(hasSetTextAction()).performTextInput("Z")
            mainClock.advanceTimeBy(100)
            assertEquals("abcZ", value)
            // After Home the caret stands before 'a', 14 dp in, and a letter goes there.
            press(Key.MoveHome)
            val atHome = onNodeWithTag("f").captureToImage().toPixelMap().columnsOf(text)
            assertTrue(atHome.isNotEmpty() && atHome.last() <= 16, "after Home the caret stands at $atHome")
            assertTrue(atHome.none { it in glyphs }, "and covers no letter: caret $atHome, glyphs $glyphs")
            onNode(hasSetTextAction()).performTextInput("Y")
            mainClock.advanceTimeBy(100)
            assertEquals("YabcZ", value)
        }

    /** The caret's columns and the glyphs' columns of [value] in a focused field, laid out in [direction]. */
    private fun caretAndGlyphs(
        value: String,
        direction: LayoutDirection,
    ): Pair<List<Int>, List<Int>> {
        var result = emptyList<Int>() to emptyList<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Still {
                        Box(
                            Modifier.size(360.dp, 100.dp),
                        ) { MutantTextField(value, {}, Modifier.width(240.dp).testTag("f")) }
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            val glyphs =
                onNodeWithTag("f").captureToImage().toPixelMap().let { px ->
                    (0 until px.width).filter { x -> (0 until px.height).any { y -> near(px[x, y], text) } }
                }
            press(Key.Tab)
            result = onNodeWithTag("f").captureToImage().toPixelMap().columnsOf(text) to glyphs
        }
        return result
    }

    @Test
    fun `the caret stands clear of left-to-right text`() {
        val (caret, glyphs) = caretAndGlyphs("abc", LayoutDirection.Ltr)
        assertTrue(caret.isNotEmpty() && caret.none { it in glyphs }, "caret $caret over glyphs $glyphs")
    }

    @Test
    fun `F12 the caret stands clear of right-to-left text and of text in a right-to-left layout`() {
        for ((v, d) in listOf(
            "كاسيت قديم" to LayoutDirection.Ltr,
            "كاسيت قديم" to LayoutDirection.Rtl,
            "abc" to LayoutDirection.Rtl,
        )) {
            val (caret, glyphs) = caretAndGlyphs(v, d)
            assertTrue(
                caret.isNotEmpty() && caret.none { it in glyphs },
                "'$v' in $d: caret $caret over glyphs $glyphs",
            )
        }
    }

    @Test
    fun `the list item, snackbar and top app bar mirror in a right-to-left layout`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Still {
                        Column(Modifier.size(360.dp, 400.dp)) {
                            MutantListItem("Кассета", Modifier.width(360.dp), trailingText = "12:04")
                            MutantSnackbar("Сохранено", Modifier.width(360.dp), actionLabel = "ОТМЕНА")
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            assertTrue(boundsOf("Кассета").left > boundsOf("12:04").left)
            assertTrue(boundsOf("Сохранено").left > boundsOf("ОТМЕНА").left)
        }

    @Ignore("F15: tabs and the navigation bar place their items left to right in a right-to-left layout")
    @Test
    fun `F15 tabs and the navigation bar mirror in a right-to-left layout`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Still {
                        Column(Modifier.size(360.dp, 400.dp)) {
                            MutantTabs(listOf(MutantTab("Один"), MutantTab("Два")), 0, {}, Modifier.width(360.dp))
                            MutantNavigationBar(
                                listOf(
                                    MutantNavigationItem("Дом", MutantIcons.Home),
                                    MutantNavigationItem("Поиск", MutantIcons.Search),
                                ),
                                0,
                                {},
                                Modifier.width(360.dp),
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            assertTrue(boundsOf("Один").left > boundsOf("Два").left, "tabs: ${boundsOf("Один")} ${boundsOf("Два")}")
            assertTrue(
                boundsOf("Дом").left > boundsOf("Поиск").left,
                "navigation: ${boundsOf("Дом")} ${boundsOf("Поиск")}",
            )
        }

    @Ignore(
        "F14: in unbounded width the weighted parts collapse to nothing: the list item's words, the bar's title, the progress track",
    )
    @Test
    fun `F14 in a scrolling row the list item, the bar and the progress keep their words and track`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Column(Modifier.size(400.dp, 400.dp)) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                        ) { MutantListItem("Кассета", supporting = "Сторона А") }
                        Row(Modifier.horizontalScroll(rememberScrollState())) { MutantTopAppBar("Кассеты") {} }
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                        ) { MutantLinearProgress(.5f, Modifier.testTag("p")) }
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            assertTrue(boundsOf("Кассета").width > 0f, "headline ${boundsOf("Кассета")}")
            assertTrue(boundsOf("Кассеты").width > 0f, "title ${boundsOf("Кассеты")}")
            assertTrue(
                onNodeWithTag("p").fetchSemanticsNode().boundsInRoot.width > 100f,
                "progress ${onNodeWithTag("p").fetchSemanticsNode().boundsInRoot}",
            )
        }
}
