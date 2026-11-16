package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-36: the pull, by a finger and by a mouse, and what the refresh shows. Density 1: a dp is a pixel. */
@OptIn(ExperimentalTestApi::class)
class PullToRefreshBehaviourTest {
    private class Screen(
        val refreshes: () -> Int,
        val clicks: () -> Int,
        val set: (Boolean) -> Unit,
    )

    private fun ComposeUiTest.screen(reduceMotion: Boolean = false): Screen {
        var refreshing by mutableStateOf(false)
        var refreshes = 0
        var clicks = 0
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    Box(Modifier.size(320.dp, 400.dp)) {
                        MutantPullToRefresh(refreshing, { refreshes++ }, Modifier.testTag("ptr")) {
                            Column(Modifier.verticalScroll(rememberScrollState()).testTag("list")) {
                                // Tall enough that the pull never leaves it: only the pull's own consuming can cancel its click.
                                Box(Modifier.size(300.dp, 300.dp).clickable { clicks++ }.testTag("first"))
                                repeat(20) { BasicText("Строка $it", Modifier.size(300.dp, 40.dp)) }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        return Screen({ refreshes }, { clicks }, { refreshing = it })
    }

    /** Where the list's top stands: the gap the pull opened, plus the 2 dp tear line. */
    private fun ComposeUiTest.listTop(): Float = onNodeWithTag("list").fetchSemanticsNode().positionInRoot.y

    private fun ComposeUiTest.shows(word: String): Boolean =
        onAllNodes(
            androidx.compose.ui.test
                .hasText(word),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `a finger pulled past 74 dp refreshes, and the list moved down with it`() =
        runComposeUiTest {
            val s = screen()
            onNodeWithTag("list").performTouchInput {
                down(Offset(150f, 60f))
                moveBy(Offset(0f, 100f))
                moveBy(Offset(0f, 100f))
            }
            mainClock.advanceTimeByFrame()
            val pulled = listTop()
            onNodeWithTag("list").performTouchInput { up() }
            mainClock.advanceTimeByFrame()
            assertTrue(pulled > 2f + 60f, "pulled: the list's top at $pulled")
            assertEquals(1, s.refreshes())
        }

    @Test
    fun `a mouse pulls too, and the press it began with clicks nothing`() =
        runComposeUiTest {
            val s = screen()
            onNodeWithTag("first").performMouseInput {
                // Near its top: the 200 px of the pull stay inside it.
                moveTo(Offset(centerX, 20f))
                press()
                moveBy(Offset(0f, 100f))
                moveBy(Offset(0f, 100f))
                release()
            }
            mainClock.advanceTimeByFrame()
            assertEquals(1, s.refreshes(), "refreshed by a mouse")
            assertEquals(0, s.clicks(), "the button under the press did not fire")
        }

    @Test
    fun `let go short, it does not refresh and the list goes back up in 300 ms`() =
        runComposeUiTest {
            val s = screen()
            onNodeWithTag("list").performTouchInput {
                down(Offset(150f, 60f))
                moveBy(Offset(0f, 80f))
                up()
            }
            mainClock.advanceTimeByFrame()
            val after = listTop()
            mainClock.advanceTimeBy(400)
            assertEquals(0, s.refreshes())
            assertTrue(after > 2f + 10f, "still part way down just after: $after")
            assertEquals(2f, listTop(), "back up")
        }

    @Test
    fun `refreshing shows the long tongue and its word, done the pill for a second, then the list back up`() =
        runComposeUiTest {
            val s = screen()
            s.set(true)
            mainClock.advanceTimeBy(400)
            assertTrue(shows("ОБНОВЛЯЕМ"), "refreshing")
            assertEquals(2f + 74f, listTop())
            s.set(false)
            mainClock.advanceTimeBy(400)
            assertTrue(shows("ОБНОВЛЕНО") && !shows("ОБНОВЛЯЕМ"), "done")
            assertEquals(2f + 52f, listTop())
            mainClock.advanceTimeBy(1500)
            assertTrue(!shows("ОБНОВЛЕНО"), "gone")
            assertEquals(2f, listTop())
        }

    @Test
    fun `under reduce motion the pill still stands its second, and the list goes back at once`() =
        runComposeUiTest {
            val s = screen(reduceMotion = true)
            s.set(true)
            mainClock.advanceTimeBy(100)
            s.set(false)
            mainClock.advanceTimeBy(500)
            assertTrue(shows("ОБНОВЛЕНО"), "the pill")
            mainClock.advanceTimeBy(600)
            assertTrue(!shows("ОБНОВЛЕНО"), "gone")
            assertEquals(2f, listTop(), "back at once")
        }

    @Test
    fun `a reader refreshes by the custom action`() =
        runComposeUiTest {
            val s = screen()
            val node = onNodeWithTag("ptr").fetchSemanticsNode()
            node.config[SemanticsActions.CustomActions].single { it.label == "ОБНОВЛЯЕМ" }.action()
            assertEquals(1, s.refreshes())
        }
}
