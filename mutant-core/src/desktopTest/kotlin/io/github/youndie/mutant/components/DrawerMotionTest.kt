package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.KeyframeLossBody
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-168: how the drawer moves as the sample's «Try it» stage composes it — a screen of its own, an opener, the drawer —
 * when it is turned round mid-way, opened over a running keyframe loss, closed, and laid out right to left.
 */
@OptIn(ExperimentalTestApi::class)
class DrawerMotionTest {
    private class Stage {
        var open by mutableStateOf(false)
        var at by mutableIntStateOf(2)
        var picks = 0
        var opens = 0
        val opener = FocusRequester()
    }

    private val names = listOf("Эфир", "Очередь", "Поиск", "Настройка")

    private fun drawer(
        rtl: Boolean = false,
        around: @Composable (@Composable () -> Unit) -> Unit = { it() },
        block: ComposeUiTest.(Stage) -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val stage = Stage()
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                MutantTheme {
                    MutantScreenClock {
                        Box(Modifier.size(360.dp, 320.dp).background(MutantTheme.colors.page).testTag("screen")) {
                            around {
                                Box(Modifier.padding(16.dp)) {
                                    MutantButton(
                                        "Каналы",
                                        {
                                            stage.opens++
                                            stage.open = true
                                        },
                                        Modifier.focusRequester(stage.opener).testTag("opener"),
                                    )
                                }
                                MutantNavigationDrawer(
                                    stage.open,
                                    { stage.open = false },
                                    names.mapIndexed { i, n -> MutantDrawerItem("CH 0${i + 1}", n) },
                                    stage.at,
                                    {
                                        stage.picks++
                                        stage.at = it
                                        stage.open = false
                                    },
                                    returnFocusTo = stage.opener,
                                )
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_000)
        block(stage)
    }

    /** Where the window's title stands from the screen's left edge, dp; null once the drawer is gone. */
    private fun ComposeUiTest.titleX(): Float? =
        onAllNodes(hasText("КАНАЛЫ"), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.positionInRoot
            ?.x

    @Test
    fun `turned round mid-way it goes on from where it stands - no frame jumps`() =
        drawer { stage ->
            stage.open = true
            val xs = mutableListOf<Float?>()
            // Closed a third into the slide in, then opened again a third into the slide out.
            repeat(7) {
                mainClock.advanceTimeByFrame()
                xs += titleX()
            }
            stage.open = false
            repeat(4) {
                mainClock.advanceTimeByFrame()
                xs += titleX()
            }
            stage.open = true
            repeat(30) {
                mainClock.advanceTimeByFrame()
                xs += titleX()
            }
            val steps = xs.filterNotNull().zipWithNext { a, b -> abs(b - a) }
            assertEquals(xs.size, xs.filterNotNull().size, "the drawer showed in every frame: $xs")
            // The slide's fastest frame moves the window 19 dp in, 30 dp out; a restart jumps 100 dp and more.
            assertTrue(steps.all { it < 40f }, "the window's steps frame by frame: $steps")
        }

    @Test
    fun `opened while the screen's keyframe loss runs, its window is drawn whole`() {
        var running = false
        drawer(
            around = { content ->
                KeyframeLossBody(
                    Modifier.fillMaxSize(),
                    MutantTheme.colors.background,
                    null,
                    { content() },
                    probe = { running = it },
                )
            },
        ) { stage ->
            while (!running && mainClock.currentTime < 20_000) mainClock.advanceTimeByFrame()
            assertTrue(running, "a loss runs by 20 s")
            stage.open = true
            repeat(40) { mainClock.advanceTimeByFrame() }
            // A column of the window left of its words, run off the screen's start: the window's fill alone, top to foot.
            val px = onNodeWithTag("screen").captureToImage().toPixelMap()
            val column = (0 until px.height).map { px[5, it] }
            val fill = column.sortedBy { it.red + it.green + it.blue }[column.size / 2]

            fun far(c: Color) = abs(c.red - fill.red) + abs(c.green - fill.green) + abs(c.blue - fill.blue) > .12f
            assertEquals(0, column.count(::far), "rows of the window's column that are not its fill $fill")
        }
    }

    @Test
    fun `through its exit the keys reach nothing behind it, and focus is back with the opener after`() =
        drawer { stage ->
            stage.opener.requestFocus()
            mainClock.advanceTimeByFrame()
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeBy(500)
            assertEquals(1, stage.opens, "Enter on the opener opens the drawer")
            // Escape, then Enter as the drawer slides out.
            onRoot().performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeByFrame()
            assertEquals(1, stage.opens, "the Enter during the exit reached the opener under the scrim")
            assertTrue(!stage.open, "closing")
            mainClock.advanceTimeBy(300)
            onNodeWithTag("opener").assertIsFocused()
        }

    /** A mouse click on the row reading [name]. */
    private fun ComposeUiTest.clickRow(name: String) {
        val row = onAllNodes(hasText(name), useUnmergedTree = true).fetchSemanticsNodes().first()
        onNodeWithTag("screen").performMouseInput { click(row.positionInRoot + Offset(8f, row.size.height / 2f)) }
    }

    @Test
    fun `a closing drawer answers no row - through the hold after a pick and through the exit`() =
        drawer { stage ->
            stage.open = true
            mainClock.advanceTimeBy(500)
            clickRow("Эфир")
            mainClock.advanceTimeBy(400)
            assertEquals(1, stage.picks, "the first pick")
            // B-102's hold: the drawer stands while the run lands, and takes no input.
            clickRow("Настройка")
            mainClock.advanceTimeBy(100)
            assertEquals(1 to 0, stage.picks to stage.at, "a row clicked during the hold")
            mainClock.advanceTimeBy(1_500)
            stage.open = true
            mainClock.advanceTimeBy(500)
            onRoot().performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeByFrame()
            clickRow("Очередь")
            mainClock.advanceTimeBy(100)
            assertEquals(1 to 0, stage.picks to stage.at, "a row clicked during the exit")
        }

    @Test
    fun `opened, Down takes the keys to the chosen row, and the window itself never holds them`() =
        drawer { stage ->
            stage.opener.requestFocus()
            mainClock.advanceTimeByFrame()
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeBy(500)
            onRoot().performKeyInput { pressKey(Key.DirectionDown) }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Поиск")).assertIsFocused()
            // A click on the window's foot, below the rows, leaves the keys where they were.
            onNodeWithTag("screen").performMouseInput { click(Offset(100f, 300f)) }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Поиск")).assertIsFocused()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            mainClock.advanceTimeBy(400)
            onRoot().performKeyInput { pressKey(Key.Enter) }
            mainClock.advanceTimeBy(500)
            // The first Tab goes to the first row, not to the window around the rows.
            onRoot().performKeyInput { pressKey(Key.Tab) }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Эфир")).assertIsFocused()
        }
}
