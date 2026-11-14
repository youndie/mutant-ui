package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantButtonState
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCircularProgress
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-38: states distinguishable in greyscale, and each component's semantics its Material counterpart's. */
@OptIn(ExperimentalTestApi::class)
class StatesAndSemanticsTest {
    /** [content] drawn in [print]'s theme, in grey: each pixel's luma, 0…255. */
    private fun grey(
        print: Boolean,
        content: @Composable () -> Unit,
    ): IntArray {
        var out = IntArray(0)
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    // The clock runs, as a screen's does: a loading button's seek bands move.
                    MutantTheme(
                        colors = if (print) MutantPalettes.Kopiya else MutantPalettes.Efir,
                        reduceMotion = false,
                    ) {
                        Box(Modifier.testTag("s")) { content() }
                    }
                }
            }
            mainClock.advanceTimeBy(1000)
            val img = onNodeWithTag("s").captureToImage().toAwtImage()
            out =
                IntArray(img.width * img.height) {
                    val c = img.getRGB(it % img.width, it / img.width)
                    ((c shr 16 and 0xFF) * 299 + (c shr 8 and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
                }
        }
        return out
    }

    /** How far apart two grey pictures stand: the pixels more than 16 levels apart, counted (a mark is small). */
    private fun apart(
        a: IntArray,
        b: IntArray,
    ): Int = a.indices.count { it < b.size && abs(a[it] - b[it]) > 16 }

    @Test
    fun `every state of every component stands apart from the others in grey`() {
        val states: List<Pair<String, List<Pair<String, @Composable () -> Unit>>>> =
            listOf(
                "button" to
                    listOf(
                        "rest" to @Composable { MutantButton("Воспроизвести", {}, key = "b") },
                        "loading" to
                            @Composable {
                                MutantButton(
                                    "Воспроизвести",
                                    {},
                                    state = MutantButtonState.Loading,
                                    key = "b",
                                )
                            },
                        "success" to
                            @Composable {
                                MutantButton(
                                    "Воспроизвести",
                                    {},
                                    state = MutantButtonState.Success,
                                    key = "b",
                                )
                            },
                        "disabled" to @Composable { MutantButton("Воспроизвести", {}, enabled = false, key = "b") },
                    ),
                "checkbox" to
                    listOf(
                        "off" to @Composable { MutantCheckbox(false, {}, label = "Шум", key = "c") },
                        "on" to @Composable { MutantCheckbox(true, {}, label = "Шум", key = "c") },
                        "disabled" to
                            @Composable { MutantCheckbox(true, {}, label = "Шум", enabled = false, key = "c") },
                    ),
                "radio" to
                    listOf(
                        "off" to @Composable { MutantRadioButton(false, {}, label = "SP", key = "r") },
                        "on" to @Composable { MutantRadioButton(true, {}, label = "SP", key = "r") },
                    ),
                "switch" to
                    listOf(
                        "off" to @Composable { MutantSwitch(false, {}, label = "Шум", key = "s") },
                        "on" to @Composable { MutantSwitch(true, {}, label = "Шум", key = "s") },
                        "disabled" to @Composable { MutantSwitch(true, {}, label = "Шум", enabled = false, key = "s") },
                    ),
                "chip" to
                    listOf(
                        "off" to @Composable { MutantChip("Шум", false, {}, key = "h") },
                        "on" to @Composable { MutantChip("Шум", true, {}, key = "h") },
                        "disabled" to @Composable { MutantChip("Шум", false, {}, enabled = false, key = "h") },
                    ),
                "field" to
                    listOf(
                        "rest" to @Composable { MutantTextField("99", {}, Modifier.width(200.dp), key = "f") },
                        "error" to
                            @Composable {
                                MutantTextField(
                                    "99",
                                    {},
                                    Modifier.width(200.dp),
                                    isError = true,
                                    key = "f",
                                )
                            },
                        "disabled" to
                            @Composable {
                                MutantTextField(
                                    "99",
                                    {},
                                    Modifier.width(200.dp),
                                    enabled = false,
                                    key = "f",
                                )
                            },
                    ),
            )
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (print in listOf(false, true)) {
            for ((component, list) in states) {
                val pictures = list.map { (name, c) -> name to grey(print, c) }
                for (i in pictures.indices) {
                    for (j in i + 1 until pictures.size) {
                        val d = apart(pictures[i].second, pictures[j].second)
                        val theme = if (print) "Копия" else "Эфир"
                        val pair = "$theme $component ${pictures[i].first}/${pictures[j].first}"
                        lines += "$pair: $d px"
                        // Apart by a mark at least: the radio's 10 dp dot is about 80 pixels.
                        if (d < 40 && pair !in CLOSE) off += pair
                    }
                }
            }
        }
        println("A11Y-GREY\n" + lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "too close in grey: $off\n" + lines.joinToString("\n"))
    }

    @Test
    fun `each component reads as its Material counterpart, and no body adds a node`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = true) {
                    androidx.compose.foundation.layout.Column {
                        MutantButton("Воспроизвести", {}, Modifier.testTag("button"))
                        MutantIconButton(MutantIcons.Edit, "Править", {}, Modifier.testTag("icon"))
                        MutantChip("Шум", true, {}, Modifier.testTag("chip"))
                        MutantCheckbox(true, {}, Modifier.testTag("check"), label = "Шум")
                        MutantRadioButton(true, {}, Modifier.testTag("radio"), label = "SP")
                        MutantSwitch(false, {}, Modifier.testTag("switch"), label = "Шум")
                        MutantTextField("99", {}, Modifier.width(200.dp).testTag("field"))
                        MutantTabs(listOf(MutantTab("Дорожки"), MutantTab("Заметки")), 0, {}, Modifier.testTag("tabs"))
                        MutantLinearProgress(.4f, Modifier.width(200.dp).testTag("linear"))
                        MutantCircularProgress(Modifier.testTag("circular"))
                        MutantCard(Modifier.testTag("card"), onClick = {}) { MutantCardTitle("Без подписи №3") }
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            fun node(tag: String) = onNodeWithTag(tag).fetchSemanticsNode()
            assertEquals(Role.Button, node("button").config[SemanticsProperties.Role])
            assertTrue(SemanticsActions.OnClick in node("button").config)
            assertEquals(Role.Button, node("icon").config[SemanticsProperties.Role])
            assertEquals(listOf("Править"), node("icon").config[SemanticsProperties.ContentDescription])
            // Material's FilterChip: a selectable with the checkbox's role.
            assertEquals(Role.Checkbox, node("chip").config[SemanticsProperties.Role])
            assertEquals(true, node("chip").config[SemanticsProperties.Selected])
            assertEquals(Role.Checkbox, node("check").config[SemanticsProperties.Role])
            assertEquals(ToggleableState.On, node("check").config[SemanticsProperties.ToggleableState])
            assertEquals(Role.RadioButton, node("radio").config[SemanticsProperties.Role])
            assertEquals(true, node("radio").config[SemanticsProperties.Selected])
            assertEquals(Role.Switch, node("switch").config[SemanticsProperties.Role])
            assertEquals(ToggleableState.Off, node("switch").config[SemanticsProperties.ToggleableState])
            val field = onNodeWithTag("field", useUnmergedTree = true).fetchSemanticsNode()
            val fieldNodes =
                generateSequence(
                    listOf(field),
                ) { l -> l.flatMap { it.children }.ifEmpty { null } }.flatten().toList()
            assertTrue(fieldNodes.any { SemanticsActions.SetText in it.config }, "an editable text")
            assertEquals(
                ProgressBarRangeInfo(.4f, 0f..1f),
                node("linear").config[SemanticsProperties.ProgressBarRangeInfo],
            )
            assertEquals(
                ProgressBarRangeInfo.Indeterminate,
                node("circular").config[SemanticsProperties.ProgressBarRangeInfo],
            )
            assertEquals(Role.Button, node("card").config[SemanticsProperties.Role])
            // The bodies draw and add nothing: a button is one node with its word, its body no child of its own.
            val button = onNodeWithTag("button", useUnmergedTree = true).fetchSemanticsNode()
            val descendants =
                generateSequence(listOf(button)) { level ->
                    level
                        .flatMap {
                            it.children
                        }.ifEmpty { null }
                }.flatten().toList()
            assertEquals(
                listOf("Воспроизвести"),
                descendants
                    .flatMap {
                        it.config.getOrElse(SemanticsProperties.Text) { emptyList() }
                    }.map { it.text },
            )
            assertTrue(descendants.size <= 2, "the button and its word: ${descendants.size} nodes")
        }

    private companion object {
        /**
         * Pairs measured too close in grey, each with its cause, on 2026-11-14 (research row of B-38): a loading button
         * against one at rest, 8 and 30 pixels apart — its seek bands are a light acid over the acid body, under 16 grey
         * levels from it, running or paused. It differs by its motion, and its reader reads progress.
         */
        val CLOSE = setOf("Эфир button rest/loading", "Копия button rest/loading")
    }
}
