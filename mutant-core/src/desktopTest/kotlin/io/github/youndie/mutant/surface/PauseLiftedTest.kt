package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-176: the pause (D7) lands a running pour, slide and melt and a released press at once, and lifting it replays none
 * of them. One scene at density 2 — a 300 × 88 card pouring lime, a 56 × 30 pill sliding 160 dp, a 24 dp mark melting in
 * and a 200 × 48 button pressed — driven by the test's clock, advanced by hand frame by frame; every reading is of drawn
 * pixels.
 */
@OptIn(ExperimentalTestApi::class)
class PauseLiftedTest {
    private val c = MutantPalettes.Efir

    /** What a frame shows: the card's lime pixels, the pill's centre, the mark's white pixels, the button's warmth. */
    private data class Look(
        val card: Int,
        val pill: Int,
        val mark: Int,
        val warmth: Int,
    )

    private sealed interface Event

    private class Phase(
        val n: Int,
    ) : Event

    private class Pause(
        val on: Boolean,
    ) : Event

    private data object Press : Event

    private data object Release : Event

    /** Each run's looks, one per frame from its rest (index 0) on; [Run.events] fire before the frame they are keyed by. */
    private class Run(
        val frames: Int,
        val events: Map<Int, List<Event>>,
    )

    private fun film(
        steps: Boolean,
        runs: List<Run>,
    ): List<List<Look>> {
        val out = ArrayList<List<Look>>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var run by mutableIntStateOf(0)
            var phase by mutableIntStateOf(0)
            var paused by mutableStateOf(false)
            var source = MutableInteractionSource()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    MutantTheme(reduceMotion = paused, steppedMotion = steps) {
                        key(run) {
                            Box(Modifier.size(400.dp, 370.dp).background(c.background).testTag("f")) {
                                MutantSurface(
                                    MutantBody.Card,
                                    "card",
                                    Modifier.padding(20.dp).size(300.dp, 88.dp),
                                    fill = if (phase == 1) c.primary else c.surfaceVariant,
                                ) {}
                                // The pill's request as a switch makes it: the id is the place, its offset the way there.
                                val home = if (phase == 1) 180 else 20
                                val req = remember { arrayOfNulls<SlideRequest>(1) }
                                if (req[0] == null || req[0]!!.id != home) {
                                    val from = req[0]?.id as Int? ?: home
                                    req[0] = SlideRequest(home, (from - home).toDouble())
                                }
                                // Placed by padding: a body moved by `offset` past its own box counts as off it (B-87).
                                Box(Modifier.padding(start = home.dp, top = 160.dp)) {
                                    MutantSurface(
                                        MutantBody.Pill,
                                        "pill",
                                        Modifier.size(56.dp, 30.dp),
                                        slide = req[0],
                                    ) {}
                                }
                                Box(Modifier.offset(300.dp, 150.dp).size(60.dp).background(Color.Black)) {
                                    Box(
                                        Modifier
                                            .padding(18.dp)
                                            .size(24.dp)
                                            .mutantMelt(phase == 1)
                                            .background(Color.White),
                                    )
                                }
                                Box(Modifier.padding(start = 20.dp, top = 250.dp)) {
                                    MutantSurface(
                                        MutantBody.Btn,
                                        "btn",
                                        Modifier.size(200.dp, 48.dp),
                                        interactionSource = source,
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
            for (r in runs) {
                paused = false
                phase = 0
                source = MutableInteractionSource()
                run++
                repeat(WARM_UP) { mainClock.advanceTimeByFrame() }
                val press = PressInteraction.Press(Offset(100f, 24f))
                val m = ArrayList<Look>()
                m += look(onNodeWithTag("f").captureToImage().toPixelMap())
                for (i in 1..r.frames) {
                    for (e in r.events[i].orEmpty()) {
                        when (e) {
                            is Phase -> phase = e.n
                            is Pause -> paused = e.on
                            Press -> runBlocking { source.emit(press) }
                            Release -> runBlocking { source.emit(PressInteraction.Release(press)) }
                        }
                    }
                    mainClock.advanceTimeByFrame()
                    m += look(onNodeWithTag("f").captureToImage().toPixelMap())
                }
                out += m
            }
        }
        return out
    }

    private fun look(px: PixelMap): Look {
        var card = 0
        var l = Int.MAX_VALUE
        var r = -1
        var mark = 0
        var warm = 0
        var green = 0.0
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val p = px[x, y]
                when {
                    // The card and its drips: x 40…640, y 40…216 and below.
                    y < 290 -> {
                        val d =
                            abs(p.red - c.primary.red) + abs(p.green - c.primary.green) + abs(p.blue - c.primary.blue)
                        if (d < .12f) card++
                    }

                    // The pill, home at x 40 or 360, y 320…380.
                    y in 300 until 400 && x < 580 -> {
                        if (p.green > .6f && p.blue < .4f) {
                            if (x < l) l = x
                            if (x > r) r = x
                        }
                    }

                    // The mark, 48 × 48 px at (636, 336) on black.
                    y in 300 until 420 && x >= 600 && x < 720 -> {
                        if (p.red > .5f && p.green > .5f && p.blue > .5f) mark++
                    }

                    // The button's slime, y 500…596 and its drips.
                    y >= 480 && x < 500 -> {
                        if (p.red > .6f && p.green > .6f && p.blue < .3f) {
                            warm++
                            green += p.green
                        }
                    }
                }
            }
        }
        // Lime's green is 1, amber's .69: 0 is lime, 1000 is amber.
        val warmth = if (warm == 0) -1 else ((1 - green / warm) / (1 - AMBER_GREEN) * 1000).toInt()
        return Look(card, if (r < 0) -1 else (l + r) / 2, mark, warmth)
    }

    @Test
    fun `a transition landed under the pause stays landed when the pause lifts`() {
        val report = StringBuilder()
        var bad = false
        for (steps in listOf(false, true)) {
            // Paused 16, 144 and 272 ms into the pour (620 ms), the slide and the melt (330 ms), for six frames.
            val at = listOf(1, 9, 17)
            val runs =
                at.map {
                    Run(
                        it + HOLD + AFTER,
                        mapOf(
                            1 to listOf(Phase(1)),
                            it + 1 to listOf(Pause(true)),
                            it + 1 + HOLD to listOf(Pause(false)),
                        ),
                    )
                }
            for ((k, m) in film(steps, runs).withIndex()) {
                val lift = at[k] + 1 + HOLD
                val landed = m[lift - 1]
                val after = m.drop(lift)
                val card = after.minOf { it.card }
                val pill = after.maxOf { abs(it.pill - landed.pill) }
                val mark = after.minOf { it.mark }
                report.append(
                    "stepped $steps, paused at ${at[k] * 16} ms: landed card ${landed.card} px, " +
                        "pill at ${landed.pill}, " +
                        "mark ${landed.mark}; after the lift card ≥ $card, pill off by ≤ $pill, mark ≥ $mark\n",
                )
                // The pour landed whole, the mark whole, the pill home (x 360…472).
                if (landed.card < FULL_CARD || landed.mark != WHOLE_MARK ||
                    abs(landed.pill - HOME_PILL) > PX
                ) {
                    bad = true
                }
                // From the lift on the boil goes on, nothing else: the card keeps all but 2 % of its lime (the boil's
                // wobble at rest moves 0.6 %), the pill stays within 2 px, the mark keeps every pixel.
                if (card < landed.card * (1 - CARD_MARGIN) || pill > PX || mark < landed.mark) bad = true
            }
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    @Test
    fun `a press released under the pause shows released at once`() {
        // Pressed for 320 ms, paused, released two frames later, lifted six frames after that.
        val run =
            Run(
                PRESS + 2 + HOLD + AFTER,
                mapOf(
                    1 to listOf(Press),
                    PRESS + 1 to listOf(Pause(true)),
                    PRESS + 3 to listOf(Release),
                    PRESS + 3 + HOLD to listOf(Pause(false)),
                ),
            )
        val m = film(false, listOf(run))[0]
        val rest = m[0].warmth
        val held = m.subList(PRESS + 1, PRESS + 3).minOf { it.warmth }
        val released = m.subList(PRESS + 3, PRESS + 3 + HOLD).maxOf { it.warmth }
        val lifted = m.drop(PRESS + 3 + HOLD).maxOf { it.warmth }
        val report =
            "warmth at rest $rest, held under the pause ≥ $held, released under it ≤ $released, after the lift ≤ $lifted"
        println(report)
        // Held across the pause, the press stays hot; released under it, the body is back at rest in the release's own
        // frame, to within 50 of 1 000 (the mosaic's few amber tiles), and stays there once the pause lifts.
        assertTrue(held > HOT, report)
        assertTrue(released <= rest + WARMTH_MARGIN && lifted <= rest + WARMTH_MARGIN, report)
    }

    @Test
    fun `a press still cooling when the pause starts lands at once`() {
        // Pressed for 320 ms and released; 80 ms into its 480 ms of cooling the pause starts, and lifts six frames later.
        val run =
            Run(
                PRESS + COOL + HOLD + AFTER,
                mapOf(
                    1 to listOf(Press),
                    PRESS + 1 to listOf(Release),
                    PRESS + 1 + COOL to listOf(Pause(true)),
                    PRESS + 1 + COOL + HOLD to listOf(Pause(false)),
                ),
            )
        val m = film(false, listOf(run))[0]
        val rest = m[0].warmth
        val cooling = m[PRESS + COOL].warmth
        val paused = m.subList(PRESS + 1 + COOL, PRESS + 1 + COOL + HOLD).maxOf { it.warmth }
        val lifted = m.drop(PRESS + 1 + COOL + HOLD).maxOf { it.warmth }
        val report =
            "warmth at rest $rest, cooling before the pause $cooling, under it ≤ $paused, after the lift ≤ $lifted"
        println(report)
        assertTrue(cooling > HOT, report)
        assertTrue(paused <= rest + WARMTH_MARGIN && lifted <= rest + WARMTH_MARGIN, report)
    }

    @Test
    fun `a transition started after the lift plays as before`() {
        val report = StringBuilder()
        var bad = false
        for (steps in listOf(false, true)) {
            // The change back right after a pause that landed the change, and the same change back from a rest that was
            // never paused.
            val paused =
                Run(
                    9 + HOLD + 1 + BACK,
                    mapOf(
                        1 to listOf(Phase(1)),
                        10 to listOf(Pause(true)),
                        10 + HOLD to listOf(Pause(false)),
                        11 + HOLD to listOf(Phase(0)),
                    ),
                )
            val never = Run(SETTLE + BACK, mapOf(1 to listOf(Phase(1)), SETTLE + 1 to listOf(Phase(0))))
            val (p, n) = film(steps, listOf(paused, never))
            val a = p.takeLast(BACK)
            val b = n.takeLast(BACK)
            val card = a.indices.maxOf { abs(a[it].card - b[it].card) }
            val pill = a.indices.maxOf { abs(a[it].pill - b[it].pill) }
            val mark = a.indices.maxOf { abs(a[it].mark - b[it].mark) }
            report.append(
                "stepped $steps: the change back after the lift against one never paused, frame by frame: " +
                    "card ≤ $card px, " +
                    "pill ≤ $pill, mark ≤ $mark; card ${a.map { it.card }}\n",
            )
            // The two pours run over bodies boiled at other times: 2 % of the full card for the boil, no more. The slide
            // within 2 px, the melt (no boil) to the pixel.
            if (card > FULL_CARD * CARD_MARGIN || pill > PX || mark > 0) bad = true
            // And it does run: the card loses its lime, the pill has left home 128 ms in (a step of 83 ms under the steps).
            if (a.first().card < FULL_CARD || a.last().card > FULL_CARD / 20 || a[8].pill > HOME_PILL - 40) bad = true
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    private companion object {
        const val D = 2f
        const val WARM_UP = 12

        /** Frames the pause holds, and frames read after the lift (480 ms: past every transition's end). */
        const val HOLD = 6
        const val AFTER = 30

        /** Frames a press is held before the pause (320 ms: the heat at 1). */
        const val PRESS = 20

        /** Frames a released press cools before the pause starts (80 ms of its 480). */
        const val COOL = 5

        /** Frames to read the change back over (the card's pour is 620 ms), and frames to let the first change settle. */
        const val BACK = 42
        const val SETTLE = 45

        /** The card poured whole: 96 494 lime px at density 2, less 3 % for the boil. */
        const val FULL_CARD = 93_000

        /** The pill home at 180 dp: its centre at 416 px. */
        const val HOME_PILL = 416
        const val WHOLE_MARK = 48 * 48
        const val CARD_MARGIN = .02
        const val PX = 2

        /** A pressed body's warmth over 600 of 1 000; at rest within 50 of where it started. */
        const val HOT = 600
        const val WARMTH_MARGIN = 50
        const val AMBER_GREEN = 0.69
    }
}
