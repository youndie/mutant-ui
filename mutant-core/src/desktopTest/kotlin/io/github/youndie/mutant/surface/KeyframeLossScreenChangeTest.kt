package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.gen.moshPlan
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-198, the designer's answer 24: a screen change ends the keyframe loss at once, no loss starts for 1 s after it, and
 * the dividers are in the loss's picture.
 */
@OptIn(ExperimentalTestApi::class)
class KeyframeLossScreenChangeTest {
    /** Screen 0: a card and a button, as the other loss tests stand them; screen 1: one wide chip over their band. */
    @Composable
    private fun Shown(tab: Int) {
        if (tab == 0) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MutantSurface(MutantBody.Card, "spec-card", Modifier.size(214.dp, 64.dp)) {
                    BasicText("Без подписи №3", Modifier.mutantCleanZone())
                }
                MutantSurface(
                    MutantBody.Btn,
                    "spec-btn",
                    Modifier.size(168.dp, 48.dp),
                    fill = MutantTheme.colors.primary,
                ) {}
            }
        } else {
            Box(Modifier.fillMaxSize().padding(8.dp)) {
                MutantSurface(
                    MutantBody.Chip,
                    "chip-b",
                    Modifier.padding(top = 60.dp).size(220.dp, 90.dp),
                    fill = MutantTheme.colors.magenta,
                ) {
                    BasicText("Записи", Modifier.mutantCleanZone())
                }
            }
        }
    }

    /**
     * Screen 0 on a running clock, switched to screen 1 at [switchAt] ms: whether a loss runs at each of [times] (ms, in
     * order, past [switchAt]), and the scene's pixels one frame after the switch. [host] false draws the same screens
     * with no loss over them, for the reference.
     */
    private fun run(
        switchAt: Long,
        times: List<Long>,
        host: Boolean = true,
    ): Pair<List<Boolean>, IntArray> {
        val seen = ArrayList<Boolean>()
        var px = IntArray(0)
        runComposeUiTest {
            mainClock.autoAdvance = false
            var tab by mutableIntStateOf(0)
            var running = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        ProvideMutantClock(MutantClock()) {
                            val m = Modifier.size(250.dp, 212.dp).testTag("s")
                            if (host) {
                                KeyframeLossBody(
                                    m,
                                    MutantTheme.colors.page,
                                    null,
                                    { Shown(tab) },
                                    probe = { running = it },
                                    screen = tab,
                                )
                            } else {
                                Box(m) { Shown(tab) }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(switchAt)
            seen += running
            tab = 1
            mainClock.advanceTimeBy(16)
            px = pixels()
            var now = switchAt + 16
            for (t in times) {
                mainClock.advanceTimeBy(t - now)
                now = t
                seen += running
            }
        }
        return seen to px
    }

    private fun ComposeUiTest.pixels(): IntArray {
        val m = onNodeWithTag("s").captureToImage().toPixelMap()
        return IntArray(m.width * m.height) { i ->
            val c = m[i % m.width, i / m.width]
            ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
        }
    }

    private fun near(
        a: Int,
        b: Int,
    ) = (0..2).all { abs((a shr (8 * it) and 255) - (b shr (8 * it) and 255)) <= 8 }

    @Test
    fun `a tab switch during a loss shows the new screen whole on the next frame`() {
        // The first loss starts 3.5 s in and runs past 4.4 s; the switch comes 400 ms into it.
        val (seen, after) = run(3_904, listOf(3_936, 4_400))
        val (_, bare) = run(3_904, emptyList(), host = false)
        assertEquals(listOf(true, false, false), seen, "a loss running before, after one frame and at 4.4 s")
        // The band covered most of the chip's rows: every pixel off the bare screen counts, edge pixels with them.
        val off = after.indices.count { !near(after[it], bare[it]) }
        println("SWITCH off the bare screen one frame after: $off of ${after.size} px")
        assertTrue(off < after.size / 200, "$off px off the new screen drawn with no loss, one frame after the switch")
    }

    @Test
    fun `no loss begins within 1 s of a screen change`() {
        // Switched 300 ms before the window's loss would start: it waits until 1 s past the switch, then starts.
        val switchAt = 3_200L
        val times = (switchAt + 32..switchAt + 1_400 step 16).toList()
        val (seen, _) = run(switchAt, times)
        val first = times.indices.firstOrNull { seen[it + 1] }?.let { times[it] }
        assertTrue(first != null, "no loss in the 1.4 s after the switch")
        assertTrue(
            first >= switchAt + KEYFRAME_LOSS_QUIET_MS,
            "a loss at $first ms, ${first - switchAt} ms after the switch",
        )
    }

    /** A list of four tiles with a divider under each, or a 2 dp gap in its place, frozen at [p] of its first loss. */
    private fun list(
        d: Float,
        dividers: Boolean,
        p: Double?,
    ): IntArray {
        var px = IntArray(0)
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(d)) {
                    MutantTheme(MutantPalettes.Efir) {
                        ProvideMutantClock(MutantClock(startMs = 5_000.0, frozen = true)) {
                            KeyframeLossBody(
                                Modifier
                                    .size(
                                        250.dp,
                                        212.dp,
                                    ).testTag("s"),
                                MutantTheme.colors.background,
                                p,
                                {
                                    Column {
                                        for (i in 0 until 4) {
                                            MutantListItem(
                                                "Row $i",
                                                supporting = "side A",
                                                leadingIcon = MutantIcons.Home,
                                                key = "t$i",
                                            )
                                            if (dividers) {
                                                MutantDivider(key = "$i")
                                            } else {
                                                Spacer(Modifier.fillMaxWidth().height(2.dp))
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
            waitForIdle()
            px = pixels()
        }
        return px
    }

    /**
     * The engine's loss takes the whole stage, dividers included, so its blocks drag their strokes (at least 1 dp down
     * a step). What the dividers add to a frozen loss's frame, past the pixels they hold at rest, is their strokes
     * dragged; standing whole over the band, as they did, they add nothing there.
     */
    @Test
    fun `a loss over a list drags its dividers`() {
        for (d in listOf(1f, 2f)) {
            val rest =
                list(d, true, null).let { on ->
                    list(d, false, null).let { off -> on.indices.map { !near(on[it], off[it]) } }
                }
            val on = list(d, true, .6)
            val off = list(d, false, .6)
            val dragged = on.indices.count { !rest[it] && !near(on[it], off[it]) }
            // Where the band lies, the dividers give way to it as a body does: their strokes at rest stand there only
            // where the band's picture happens to hold a stroke, not whole across it.
            val plan = moshPlan("5", 1, 250, 212)
            val w = (250 * d).toInt()
            val band = (plan.y0 * d).toInt() until ((plan.y0 + plan.rows * 16) * d).toInt()
            val inBand = on.indices.filter { rest[it] && it / w in band }
            val standing = inBand.count { !near(on[it], off[it]) }
            println(
                "DIVIDERS density $d: $dragged px of dragged strokes; $standing of ${inBand.size} px standing in the band",
            )
            assertTrue(dragged > 150 * d * d, "$dragged px of the dividers' strokes off their rows at density $d")
            assertTrue(inBand.size > 100 * d, "the band holds ${inBand.size} px of the dividers at rest")
            assertTrue(
                standing * 2 < inBand.size,
                "$standing of ${inBand.size} px of the dividers stand whole over the band",
            )
        }
    }
}
