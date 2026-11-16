package io.github.youndie.mutant.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.surface.mutantTape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-90: a screen given its own clock is held alone by a dialog opened on it; the screen beside it and the theme's clock
 * keep moving. Without it, the dialog stops every screen.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenClockTest {
    private class Clocks {
        lateinit var theme: MutantClock
        lateinit var under: MutantClock
        lateinit var beside: MutantClock
    }

    private fun run(ownClocks: Boolean): List<Double> {
        val c = Clocks()
        val moved = ArrayList<Double>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = false) {
                    c.theme = readMutantClock()
                    Box(Modifier.size(10.dp).mutantTape())
                    Row {
                        Screen(ownClocks) {
                            c.under = readMutantClock()
                            Box(Modifier.size(360.dp, 300.dp).mutantTape()) {
                                MutantDialog(true, "Стереть?", {}) { MutantDialogAction("Да", {}) }
                            }
                        }
                        Screen(ownClocks) {
                            c.beside = readMutantClock()
                            Box(Modifier.size(360.dp, 300.dp).mutantTape())
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            val start = listOf(c.theme.nowMs, c.under.nowMs, c.beside.nowMs)
            mainClock.advanceTimeBy(2_000)
            listOf(c.theme.nowMs, c.under.nowMs, c.beside.nowMs).zip(start) { a, b -> moved += a - b }
        }
        return moved
    }

    @androidx.compose.runtime.Composable
    private fun Screen(
        own: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) = if (own) MutantScreenClock(content) else content()

    @Test
    fun `a dialog holds its own screen's clock alone`() {
        val (theme, under, beside) = run(ownClocks = true)
        assertEquals(0.0, under, "the screen under the dialog stands")
        assertTrue(beside > 1_900, "the screen beside it moves: $beside ms in 2 s")
        assertTrue(theme > 1_900, "the theme's clock, the page's Tape, moves: $theme ms in 2 s")
    }

    @Test
    fun `a keyframe loss beside an open dialog runs`() {
        var ran = false
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = false) {
                    Row {
                        MutantScreenClock {
                            io.github.youndie.mutant.surface.KeyframeLossBody(
                                Modifier.size(360.dp, 300.dp),
                                MutantTheme.colors.background,
                                null,
                                {
                                    io.github.youndie.mutant.components.MutantCard(Modifier.size(328.dp, 80.dp)) {
                                        io.github.youndie.mutant.components
                                            .MutantCardTitle("Кассета 07")
                                    }
                                },
                                probe = { if (it) ran = true },
                            )
                        }
                        MutantScreenClock {
                            Box(Modifier.size(360.dp, 300.dp)) {
                                MutantDialog(true, "Стереть?", {}) { MutantDialogAction("Да", {}) }
                            }
                        }
                    }
                }
            }
            // A loss comes once in every 9 s window, 3.5 s into it; the bodies build off the frame in real time.
            repeat(12 * 60) {
                mainClock.advanceTimeByFrame()
                if (it % 4 == 0) Thread.sleep(2)
            }
        }
        assertTrue(ran, "the loss ran within 12 s beside a screen held by its dialog")
    }

    @Test
    fun `without screen clocks a dialog stops every screen`() {
        val (theme, under, beside) = run(ownClocks = false)
        assertEquals(listOf(0.0, 0.0, 0.0), listOf(theme, under, beside))
    }
}
