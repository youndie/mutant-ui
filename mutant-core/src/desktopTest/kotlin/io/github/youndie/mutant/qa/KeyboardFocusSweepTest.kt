package io.github.youndie.mutant.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantButtonState
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantExtendedFab
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
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
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-40, the keyboard axis: Tab from the window reaches every interactive component, its focus shows (the theme's
 * focus colour appears), and Enter and Space activate it.
 */
@OptIn(ExperimentalTestApi::class)
class KeyboardFocusSweepTest {
    private class Control(
        val name: String,
        val activates: Boolean = true,
        val content: @Composable (() -> Unit) -> Unit,
    )

    private val controls =
        listOf(
            Control("Button") { f -> MutantButton("Пуск", f) },
            Control("Button.Success") { f -> MutantButton("Пуск", f, state = MutantButtonState.Success) },
            Control("OutlinedButton") { f -> MutantOutlinedButton("Пуск", f) },
            Control("TextButton") { f -> MutantTextButton("Пуск", f) },
            Control("IconButton") { f -> MutantIconButton(MutantIcons.Search, "Поиск", f) },
            Control("Fab") { f -> MutantFab(MutantIcons.Add, "Добавить", f) },
            Control("ExtendedFab") { f -> MutantExtendedFab("Запись", MutantIcons.Add, f) },
            Control("Chip") { f -> MutantChip("Чип", false, f) },
            Control("Chip.Selected") { f -> MutantChip("Чип", true, f) },
            Control("Checkbox") { f -> MutantCheckbox(false, { f() }, label = "Галка") },
            Control("RadioButton") { f -> MutantRadioButton(false, f, label = "Радио") },
            Control("Switch") { f -> MutantSwitch(false, { f() }, label = "Тумблер") },
            Control("TextField", activates = false) { _ -> MutantTextField("abc", {}, Modifier.width(240.dp)) },
            Control(
                "Card.Clickable",
            ) { f -> MutantCard(Modifier.width(240.dp), onClick = f) { MutantCardTitle("Карта") } },
            Control("ListItem") { f -> MutantListItem("Строка", Modifier.width(300.dp), onClick = f) },
            Control("Tabs") { f -> MutantTabs(listOf(MutantTab("Один"), MutantTab("Два")), 1, { f() }) },
            Control("NavigationBar") { f ->
                MutantNavigationBar(
                    listOf(
                        MutantNavigationItem("Дом", MutantIcons.Home),
                        MutantNavigationItem("Поиск", MutantIcons.Search),
                    ),
                    1,
                    { f() },
                    Modifier.width(300.dp),
                )
            },
            Control(
                "Snackbar",
            ) { f -> MutantSnackbar("Сохранено", Modifier.width(300.dp), actionLabel = "ОТМЕНА", onAction = f) },
        )

    private val focusColour = MutantPalettes.Efir.focus

    /** Tabs once from the window; how many focus-coloured pixels appear, and how often [key] then fired. */
    private fun visit(
        c: Control,
        key: Key,
    ): Triple<Boolean, Int, Int> {
        var result = Triple(false, 0, 0)
        runComposeUiTest {
            mainClock.autoAdvance = false
            var clicks by mutableIntStateOf(0)
            setContent {
                Still {
                    Box(Modifier.size(400.dp, 200.dp)) {
                        Column {
                            Box(Modifier.size(20.dp))
                            c.content { clicks++ }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(200)
            val before = onRoot().captureToImage().pixels().count { near(it, focusColour) }
            press(Key.Tab)
            val reached = focusedBounds().isNotEmpty()
            val after = onRoot().captureToImage().pixels().count { near(it, focusColour) }
            press(key)
            result = Triple(reached, after - before, clicks)
        }
        return result
    }

    @Test
    fun `Tab reaches every control and Enter and Space fire it`() {
        val failures = mutableListOf<String>()
        for (c in controls) {
            for (key in listOf(Key.Enter, Key.Spacebar)) {
                val (reached, _, clicks) = visit(c, key)
                if (!reached) failures += "${c.name}: Tab does not reach it"
                if (c.activates && clicks != 1) failures += "${c.name}: $key fired $clicks times"
            }
        }
        assertEquals(emptyList(), failures)
    }

    /** Controls whose focus is invisible today, each held by an ignored test below. */
    private val invisible = setOf("TextButton", "Button.Success")

    @Test
    fun `keyboard focus shows on every control`() {
        val failures =
            controls.filter { it.name !in invisible }.mapNotNull { c ->
                visit(c, Key.Unknown)
                    .second
                    .takeIf {
                        it <=
                            0
                    }?.let { c.name }
            }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun `F02 the text button shows keyboard focus`() =
        assertTrue(visit(controls.first { it.name == "TextButton" }, Key.Unknown).second > 0)

    @Test
    fun `F03 a button in Success shows keyboard focus`() =
        assertTrue(visit(controls.first { it.name == "Button.Success" }, Key.Unknown).second > 0)

    @Test
    fun `Tab leaves the text field for the next control`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Column(Modifier.size(400.dp, 300.dp)) {
                        MutantTextField("abc", {}, Modifier.width(240.dp).testTag("f"))
                        MutantButton("Дальше", {}, Modifier.testTag("next"))
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            press(Key.Tab)
            press(Key.Tab)
            assertEquals(listOf(onNodeWithTag("next").fetchSemanticsNode().boundsInRoot), focusedBounds())
        }

    @Ignore("F11: a focused button that turns to Loading drops focus; the next Tab starts from the window's top")
    @Test
    fun `F11 a button keeps keyboard focus through Loading and Success`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var state by mutableStateOf(MutantButtonState.Idle)
            setContent {
                Still {
                    Column(Modifier.size(400.dp, 300.dp)) {
                        MutantButton(
                            "Сохранить",
                            { state = MutantButtonState.Loading },
                            Modifier.testTag("b"),
                            state = state,
                        )
                        MutantButton("Дальше", {}, Modifier.testTag("next"))
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            press(Key.Tab)
            press(Key.Enter)
            state = MutantButtonState.Success
            mainClock.advanceTimeBy(100)
            assertEquals(
                listOf(onNodeWithTag("b").fetchSemanticsNode().boundsInRoot),
                focusedBounds(),
                "still on the button",
            )
        }
}
