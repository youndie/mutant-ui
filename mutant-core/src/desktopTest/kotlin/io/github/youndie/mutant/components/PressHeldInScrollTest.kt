package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.surface.fillOf
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-189 (the designer's answer 11): a finger's press on a body inside a scrolling container heats it only from 100 ms
 * after the down, and a gesture that becomes a scroll before then never heats it; outside a scroll, and for a mouse,
 * the heat starts on the down as before. The heat is read over the stage as the number of pixels lying on the way from
 * the card's fill to the amber (a heated fill), away from the fill itself: thousands when the card boils, at most the
 * focus brackets' few hundred when it does not. No single edge pixel decides.
 */
@OptIn(ExperimentalTestApi::class)
class PressHeldInScrollTest {
    private enum class Host { Scroll, Lazy, None }

    private fun ComposeUiTest.stage(
        host: Host,
        colors: MutantColors = MutantPalettes.Efir,
        d: Float = 1f,
    ) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors = colors, reduceMotion = false) {
                    Box(Modifier.size(300.dp, 240.dp).background(colors.background).testTag("stage")) {
                        val card = @Composable { i: Int ->
                            MutantCard(Modifier.width(260.dp).testTag("c$i"), onClick = {}) {
                                MutantCardTitle("Кассета $i")
                                MutantCardSubtitle("Сторона A")
                            }
                        }
                        val gap = Arrangement.spacedBy(16.dp)
                        when (host) {
                            Host.Scroll -> {
                                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), gap) {
                                    repeat(CARDS) { card(it) }
                                }
                            }

                            Host.Lazy -> {
                                LazyColumn(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = gap,
                                ) { items(CARDS) { card(it) } }
                            }

                            Host.None -> {
                                Column(Modifier.padding(16.dp)) { card(0) }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_000)
    }

    /**
     * Pixels on the stage warmed from the card's fill towards the amber: red over blue by [WARMER] more than the fill
     * has it, and no greener than the amber's mix allows. A boiling fill counts in thousands; the focus brackets and
     * the card's warm tiles stay in hundreds.
     */
    private fun ComposeUiTest.heated(colors: MutantColors = MutantPalettes.Efir): Int {
        val fill = colors.fillOf(MutantBody.Card.profile.fill)
        val amber = colors.amber
        val px = onNodeWithTag("stage").captureToImage().toPixelMap()
        var n = 0
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val p: Color = px[x, y]
                // How far towards the amber the red has come, and the green the amber's mix would have there.
                val t = (p.red - fill.red) / (amber.red - fill.red)
                if (t < 0f || t > 1.05f) continue
                val g = fill.green + t * (amber.green - fill.green)
                val warmer = (p.red - p.blue) - (fill.red - fill.blue)
                if (warmer > WARMER && kotlin.math.abs(p.green - g) < GREEN_OFF) n++
            }
        }
        return n
    }

    private fun ComposeUiTest.frames(n: Int) = repeat(n) { mainClock.advanceTimeByFrame() }

    /** Down on the first card and held still: cold through 64 ms, boiling by 100 ms and a few frames of heat. */
    private fun ComposeUiTest.heldStill(d: Float) {
        val rest = heated()
        onNodeWithTag("c0").performTouchInput { down(center) }
        val cold = mutableListOf<Int>()
        repeat(4) {
            mainClock.advanceTimeByFrame()
            cold += heated()
        }
        // 100 ms after the down, then two hot ticks of heat (0.5 of the way up).
        mainClock.advanceTimeBy(36 + 160)
        val hot = heated()
        onNodeWithTag("c0").performTouchInput { up() }
        assertTrue(rest < COLD, "at rest the card is cold: $rest")
        assertTrue(cold.all { it < COLD }, "held still, no heat through the first 64 ms (d $d): $cold")
        assertTrue(hot > HOT, "held still, heated from 100 ms (d $d): $hot")
    }

    @Test
    fun `a finger held still on a card in a scrolling column heats it from 100 ms`() {
        for (d in listOf(1f, 2f)) {
            runComposeUiTest {
                stage(Host.Scroll, d = d)
                heldStill(d)
            }
        }
    }

    @Test
    fun `a finger held still on a card in a lazy list heats it from 100 ms`() {
        for (d in listOf(1f, 2f)) {
            runComposeUiTest {
                stage(Host.Lazy, d = d)
                heldStill(d)
            }
        }
    }

    /** Down, then 60 px up over five frames (80 ms), past the slop: the list scrolls, and no frame for 600 ms shows a boil. */
    private fun ComposeUiTest.scrolled() {
        val bottom = onNodeWithTag("c0").fetchSemanticsNode().boundsInRoot.bottom
        onNodeWithTag("c0").performTouchInput { down(center) }
        val seen = mutableListOf<Int>()
        repeat(MOVES) {
            onNodeWithTag("stage").performTouchInput { moveBy(Offset(0f, -12f)) }
            mainClock.advanceTimeByFrame()
            seen += heated()
        }
        repeat(600 / 16) {
            mainClock.advanceTimeByFrame()
            seen += heated()
        }
        val moved = onNodeWithTag("c0").fetchSemanticsNode().boundsInRoot.bottom - bottom
        onNodeWithTag("stage").performTouchInput { up() }
        frames(4)
        assertTrue(moved < -10f, "the list scrolled: $moved")
        assertTrue(seen.all { it < COLD }, "a scroll never heats the card it started on: max ${seen.max()}")
    }

    @Test
    fun `a finger that scrolls a column within 100 ms never heats the card`() =
        runComposeUiTest {
            stage(Host.Scroll)
            scrolled()
        }

    @Test
    fun `a finger that scrolls a lazy list within 100 ms never heats the card`() =
        runComposeUiTest {
            stage(Host.Lazy)
            scrolled()
        }

    @Test
    fun `a tap lifted before 100 ms inside a scroll heats nothing, as Compose shows it on Android`() =
        runComposeUiTest {
            stage(Host.Scroll)
            onNodeWithTag("c0").performTouchInput { down(center) }
            frames(4)
            onNodeWithTag("c0").performTouchInput { up() }
            val seen = mutableListOf<Int>()
            repeat(400 / 16) {
                mainClock.advanceTimeByFrame()
                seen += heated()
            }
            assertTrue(seen.all { it < COLD }, "lifted at 64 ms, no heat after: max ${seen.max()}")
        }

    @Test
    fun `a body outside any scroll heats on the down, as before`() =
        runComposeUiTest {
            stage(Host.None)
            onNodeWithTag("c0").performTouchInput { down(center) }
            // Two hot ticks from the down: heat 0.25 or so.
            frames(5)
            val hot = heated()
            onNodeWithTag("c0").performTouchInput { up() }
            assertTrue(hot > HOT, "outside a scroll, heated within 80 ms of the down: $hot")
        }

    @Test
    fun `a mouse pressed on a card in a scroll heats it on the down, as a mouse never scrolls`() =
        runComposeUiTest {
            stage(Host.Scroll)
            onNodeWithTag("c0").performMouseInput {
                moveTo(center)
                press()
            }
            frames(5)
            val hot = heated()
            onNodeWithTag("c0").performMouseInput { release() }
            assertTrue(hot > HOT, "a mouse in a scroll, heated within 80 ms of the press: $hot")
        }

    private companion object {
        const val CARDS = 8
        const val MOVES = 5

        /** How much more red over blue than the fill a warmed pixel has (the first hot tick's mix has .066). */
        const val WARMER = .04f

        /** How far a warmed pixel's green may stand from the amber's mix at its red. */
        const val GREEN_OFF = .08f

        /** Below this many pixels the card is cold (the focus brackets and a few tiles stay under it). */
        const val COLD = 1_500

        /** Above this many the card's fill boils (a 260 × 80 dp card is some 20 000 pixels at density 1). */
        const val HOT = 6_000
    }
}
