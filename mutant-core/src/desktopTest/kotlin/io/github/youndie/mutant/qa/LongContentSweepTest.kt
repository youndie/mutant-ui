package io.github.youndie.mutant.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-40, the long-content axis: 300 characters with and without spaces, the empty string, emoji and right-to-left
 * text in every component, narrow, unbounded and at a phone's width; 0, 1 and 12 tabs and destinations; and what
 * a dialog does with a long text or a long action.
 */
@OptIn(ExperimentalTestApi::class)
class LongContentSweepTest {
    private val texts = listOf("no spaces" to NO_SPACES, "words" to WORDS, "empty" to "", "mixed" to MIXED)
    private val wraps = places.filter { it.name in setOf("width 120", "unbounded width", "unbounded height") }

    @Test
    fun `every component takes long, empty and mixed text without throwing`() {
        val failures = mutableListOf<String>()
        for (s in subjects) {
            for ((tn, t) in texts) {
                for (p in wraps) {
                    // F01, held by ConstraintsSweepTest.
                    if (s.name == "NavigationBar" && p.name == "unbounded width") continue
                    val e = attempt { MutantTheme { p.wrap { m -> s.content(m, t) } } }
                    if (e != null) failures += "${s.name} / $tn / ${p.name}: ${e.brief()}"
                }
            }
        }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun `tabs and the navigation bar take 0, 1 and 12 items and any index`() {
        val failures = mutableListOf<String>()
        for (n in listOf(0, 1, 12)) {
            for (i in listOf(-1, 0, n - 1, n, Int.MAX_VALUE, Int.MIN_VALUE)) {
                val tabs =
                    attempt {
                        MutantTheme {
                            MutantTabs(
                                List(n) { MutantTab("Вкладка $it") },
                                i,
                                {},
                                Modifier.width(360.dp),
                            )
                        }
                    }
                if (tabs != null) failures += "tabs $n at $i: ${tabs.brief()}"
                val nav =
                    attempt {
                        MutantTheme {
                            MutantNavigationBar(
                                List(n) {
                                    MutantNavigationItem("П$it", MutantIcons.Home, badge = "9")
                                },
                                i,
                                {},
                                Modifier.width(360.dp),
                            )
                        }
                    }
                if (nav != null) failures += "navigation $n at $i: ${nav.brief()}"
            }
        }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun `a snackbar keeps its message beside a long action`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Box(Modifier.size(400.dp, 200.dp)) {
                        MutantSnackbar("Сохранено", Modifier.width(360.dp), actionLabel = "ОТМЕНИТЬ ПОСЛЕДНЕЕ")
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            assertTrue(boundsOf("Сохранено").width > 40f, "the message: ${boundsOf("Сохранено")}")
        }

    @Ignore("F21: a snackbar's action is never cut, so a long one pushes the message out of the bar")
    @Test
    fun `F21 a snackbar keeps its message and cuts a 300-character action`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Box(
                        Modifier.size(400.dp, 200.dp),
                    ) { MutantSnackbar("Сохранено", Modifier.width(360.dp), actionLabel = NO_SPACES) }
                }
            }
            mainClock.advanceTimeBy(300)
            assertTrue(boundsOf("Сохранено").width > 40f, "the message: ${boundsOf("Сохранено")}")
            assertTrue(boundsOf(NO_SPACES.take(10)).right <= 360f, "the action: ${boundsOf(NO_SPACES.take(10))}")
        }

    private fun dialog(
        text: String,
        actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    ): @Composable () -> Unit =
        {
            Still {
                Box(
                    Modifier.size(360.dp, 600.dp),
                ) { MutantDialog(true, "Стереть?", {}, text = text, actions = actions) }
            }
        }

    @Ignore("F06: a dialog whose text is taller than the screen lays its actions out at zero height, out of reach")
    @Test
    fun `F06 a dialog with a long text keeps its actions inside the screen and at their size`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent(
                dialog(WORDS.repeat(4)) {
                    MutantDialogAction("Отмена", {})
                    MutantDialogAction("Стереть", {}, primary = true)
                },
            )
            mainClock.advanceTimeBy(500)
            for (label in listOf("Отмена", "Стереть")) {
                val b = boundsOf(label, exact = true)
                assertTrue(b.height > 0f && b.top >= 0f && b.bottom <= 600f, "$label at $b")
            }
        }

    @Ignore("F07: a long label on one dialog action squeezes the action after it to zero width")
    @Test
    fun `F07 a long first action leaves the primary action visible`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent(
                dialog("Коротко.") {
                    MutantDialogAction("Отменить и вернуться к списку записей", {})
                    MutantDialogAction("Стереть", {}, primary = true)
                },
            )
            mainClock.advanceTimeBy(500)
            assertTrue(
                boundsOf("Стереть", exact = true).width >= 48f,
                "the primary action: ${boundsOf("Стереть", exact = true)}",
            )
        }

    @Ignore("F08: tabs past the row's width get zero width, yet stay focusable and selectable; no scroll, no bar")
    @Test
    fun `F08 twelve tabs in 360 dp each stay visible or scroll into view, and none focusable is zero-sized`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Column(Modifier.size(360.dp, 200.dp)) {
                        MutantTabs(List(12) { MutantTab("Вкладка $it") }, 11, {}, Modifier.width(360.dp))
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            repeat(12) {
                press(androidx.compose.ui.input.key.Key.Tab)
                val f = focusedBounds().single()
                assertTrue(f.width > 0f && f.left >= 0f && f.right <= 360f, "focus on tab $it at $f")
            }
        }
}
