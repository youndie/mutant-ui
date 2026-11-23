package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.LocalMutantClock
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-186, the designer's answer 8: the caret's blink starts over lit on the frame after focus and after every keystroke,
 * then stays lit five scene ticks and dark three, counted from the tick of the restart. Read from pixels on the screen's
 * running clock, frame by frame.
 */
@OptIn(ExperimentalTestApi::class)
class TextFieldCaretBlinkTest {
    private val text = MutantPalettes.Efir.text

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) < .05f && abs(a.green - b.green) < .05f && abs(a.blue - b.blue) < .05f

    /** Whether the caret is drawn: a column holding a run of 18 text-coloured pixels (a glyph of Rubik 15 is shorter). */
    private fun ComposeUiTest.lit(): Boolean =
        onNodeWithTag("f").captureToImage().toPixelMap().let { px ->
            (0 until px.width).any { x ->
                var run = 0
                var best = 0
                for (y in 0 until px.height) {
                    run = if (near(px[x, y], text)) run + 1 else 0
                    best = maxOf(best, run)
                }
                best >= 18
            }
        }

    private class Field(
        val clock: () -> MutantClock,
        val focus: FocusRequester,
    )

    private fun ComposeUiTest.field(): Field {
        var clock: MutantClock? = null
        var value by mutableStateOf("Nig")
        val focus = FocusRequester()
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = false) {
                    clock = LocalMutantClock.current
                    Box {
                        MutantTextField(
                            value,
                            { value = it },
                            Modifier.width(240.dp).focusRequester(focus).testTag("f"),
                        )
                    }
                }
            }
        }
        return Field({ clock!! }, focus)
    }

    /** Frames until the scene tick stands [phase] of eight past [from]. */
    private fun ComposeUiTest.until(
        clock: MutantClock,
        phase: Long,
        from: Long = 0,
    ) {
        var guard = 0
        while ((clock.sceneTick - from).mod(8L) != phase) {
            mainClock.advanceTimeByFrame()
            check(++guard < 200) { "the clock does not run" }
        }
    }

    /** Two seconds of frames after a restart at [from]: lit exactly while (tick − from) mod 8 < 5. */
    private fun ComposeUiTest.cycle(
        clock: MutantClock,
        from: Long,
        what: String,
    ) {
        val wrong = ArrayList<String>()
        repeat(125) {
            mainClock.advanceTimeByFrame()
            val age = clock.sceneTick - from
            if (lit() != (age.mod(8L) < 5)) wrong += "tick +$age: ${if (lit()) "lit" else "dark"}"
        }
        assertTrue(wrong.isEmpty(), "$what, five lit and three dark from its tick: $wrong")
    }

    @Test
    fun `focus brings the caret lit on the next frame, then five ticks lit and three dark`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val f = field()
            mainClock.advanceTimeBy(500)
            // The screen's own blink would be dark at this tick.
            until(f.clock(), 5)
            runOnIdle { f.focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            assertTrue(lit(), "lit on the frame after focus, at the screen's tick ${f.clock().sceneTick}")
            cycle(f.clock(), f.clock().sceneTick, "after focus")
        }

    @Test
    fun `every keystroke starts the blink over lit`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val f = field()
            mainClock.advanceTimeBy(500)
            until(f.clock(), 0)
            runOnIdle { f.focus.requestFocus() }
            mainClock.advanceTimeByFrame()
            var from = f.clock().sceneTick
            val dark = ArrayList<String>()
            // Each key lands in a dark tick of the blink running since the last restart. Focus came on the screen's
            // tick 0 of eight, so the first key's tick is dark on the screen's own blink too.
            for ((i, phase) in listOf(5L, 6L, 7L, 5L).withIndex()) {
                until(f.clock(), phase, from)
                assertTrue(!lit(), "dark before key $i, $phase ticks past the last restart")
                if (i == 3) {
                    onNode(hasSetTextAction()).performKeyInput { pressKey(Key.DirectionLeft) }
                } else {
                    onNode(hasSetTextAction()).performTextInput("ht!"[i].toString())
                }
                mainClock.advanceTimeByFrame()
                if (!lit()) dark += "key $i, $phase ticks past the last restart"
                from = f.clock().sceneTick
            }
            assertEquals(listOf(), dark, "the caret lit on the frame after each key")
            cycle(f.clock(), f.clock().sceneTick, "after the last key")
        }
}
