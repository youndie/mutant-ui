package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * B-102: a pick that closes the drawer holds it on screen until the venom run has landed on the new row and lingered,
 * then it goes; a dismissal with no pick goes at once; under reduce motion everything is at once; and a selection
 * changed while the drawer is gone does not bring it back.
 */
@OptIn(ExperimentalTestApi::class)
class DrawerPickHoldTest {
    private class Stage {
        var open by mutableStateOf(true)
        var at by mutableIntStateOf(0)
    }

    private fun drawer(
        reduce: Boolean = false,
        block: ComposeUiTest.(Stage) -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val stage = Stage()
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduce) {
                    MutantScreenClock {
                        Box(Modifier.size(360.dp, 320.dp).background(MutantTheme.colors.page).testTag("s")) {
                            MutantNavigationDrawer(
                                stage.open,
                                { stage.open = false },
                                listOf("Эфир", "Очередь", "Поиск", "Настройка").mapIndexed { i, n ->
                                    MutantDrawerItem("CH 0${i + 1}", n)
                                },
                                stage.at,
                                {
                                    stage.at = it
                                    stage.open = false
                                },
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_000)
        block(stage)
    }

    private fun ComposeUiTest.showing() =
        onAllNodes(
            androidx.compose.ui.test
                .hasText("КАНАЛЫ"),
        ).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `a pick that closes the drawer holds it until the venom run lands, then it goes`() =
        drawer {
            onNodeWithText("Настройка", useUnmergedTree = true).performClick()
            mainClock.advanceTimeBy((MutantMotion.VENOM_MS - 40).toLong())
            assertTrue(showing(), "the drawer stands while the run is under way")
            mainClock.advanceTimeBy(1_000)
            assertFalse(showing(), "after the run, its linger and the exit, the drawer is gone")
        }

    @Test
    fun `a dismissal with no pick starts the exit at once`() =
        drawer {
            onNodeWithTag("s").performTouchInput { click(Offset(340f, 160f)) }
            mainClock.advanceTimeBy(300)
            assertFalse(showing(), "dismissed by the scrim, the drawer is gone after its 200 ms exit")
        }

    @Test
    fun `under reduce motion a pick closes the drawer at once`() =
        drawer(reduce = true) {
            onNodeWithText("Настройка", useUnmergedTree = true).performClick()
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertFalse(showing(), "paused, the drawer goes at once")
        }

    @Test
    fun `a selection changed while the drawer is gone does not bring it back`() =
        drawer { stage ->
            stage.open = false
            mainClock.advanceTimeBy(400)
            assertFalse(showing(), "closed")
            stage.at = 3
            mainClock.advanceTimeBy(100)
            assertFalse(showing(), "a selection made elsewhere leaves it closed")
        }
}
