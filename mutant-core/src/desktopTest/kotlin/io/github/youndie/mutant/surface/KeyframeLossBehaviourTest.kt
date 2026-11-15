package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.gen.moshPlan
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-48: when the keyframe loss runs, when it does not, and what it flashes. */
@OptIn(ExperimentalTestApi::class)
class KeyframeLossBehaviourTest {
    @Composable
    private fun Screen(
        frozenAt: Double? = null,
        probe: ((Boolean) -> Unit)? = null,
    ) {
        KeyframeLossBody(Modifier.size(250.dp, 212.dp).testTag("s"), MutantTheme.colors.page, frozenAt, {
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
        }, probe = probe)
    }

    /** Whether a loss runs at each of [times], on a running clock, [reduce] and [clean] as given. */
    private fun timeline(
        times: List<Long>,
        reduce: Boolean = false,
        clean: Boolean = false,
    ): List<Boolean> {
        val out = ArrayList<Boolean>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var running = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = reduce, cleanSignal = clean) {
                        ProvideMutantClock(MutantClock()) { Screen(probe = { running = it }) }
                    }
                }
            }
            var now = 0L
            for (t in times) {
                mainClock.advanceTimeBy(t - now)
                now = t
                out += running
            }
        }
        return out
    }

    @Test
    fun `a loss starts 3,5 s into each 9 s window and is over after its run and 375 ms`() {
        // The first loss of seed 5 on 250 × 212: its run, then the recovery.
        val dur = moshPlan("5", 1, 250, 212).durationMs
        val end = (3_500 + dur + 375 + 250).toLong()
        val seen = timeline(listOf(3_300, 3_700, 4_000, end, 9_000, 12_700))
        assertEquals(listOf(false, true, true, false, false, true), seen, "loss of ${dur.toLong()} ms")
    }

    @Test
    fun `no loss under reduce motion or a clean signal`() {
        assertEquals(listOf(false, false), timeline(listOf(3_700, 4_000), reduce = true))
        assertEquals(listOf(false, false), timeline(listOf(3_700, 4_000), clean = true))
    }

    /** The scene's pixels at step [k] of a loss frozen there: the first loss of seed 5. */
    private fun step(k: Int): IntArray {
        val dur = moshPlan("5", 1, 250, 212).durationMs
        var px = IntArray(0)
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        ProvideMutantClock(MutantClock(startMs = 5_000.0, frozen = true)) {
                            Box { Screen(frozenAt = (k * 125 + 1) / dur) }
                        }
                    }
                }
            }
            val m = onNodeWithTag("s").captureToImage().toPixelMap()
            px =
                IntArray(m.width * m.height) { i ->
                    val c = m[i % m.width, i / m.width]
                    ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
                }
        }
        return px
    }

    private fun luminance(c: Int): Double {
        fun ch(v: Int) = (v / 255.0).let { if (it <= .03928) it / 12.92 else ((it + .055) / 1.055).pow(2.4) }
        return .2126 * ch(c shr 16 and 255) + .7152 * ch(c shr 8 and 255) + .0722 * ch(c and 255)
    }

    /**
     * The tokens' flash rule (`flash.rule`, WCAG 2.3.1): no more than three flashes a second, a flash a pair of opposite
     * changes of relative luminance by 0.1 or more, over an area past `flash.keyframe-loss`'s threshold of 21 824 px².
     * The loss steps at 8 a second; a pixel counts when, in any 8 steps, its changes of 0.1 or more alternate in sign
     * more than six times. The share of the band changed a step is printed beside it (the design's own demo screens
     * measured at most 2.9 %; this stage is smaller and denser).
     */
    @Test
    fun `the loss flashes within the tokens' rule`() {
        val plan = moshPlan("5", 1, 250, 212)
        val band = 250 * plan.rows * 16
        val steps = (plan.durationMs / 125).toInt()
        val frames = (0 until steps).map { step(it) }
        val lum = frames.map { f -> DoubleArray(f.size) { luminance(f[it]) } }
        val shares =
            (1 until steps).map { k ->
                lum[k].indices.count { abs(lum[k][it] - lum[k - 1][it]) >= .1 }.toDouble() /
                    band
            }
        var flashing = 0
        for (i in lum[0].indices) {
            var worst = 0
            for (w0 in 0 until maxOf(1, steps - 8)) {
                var turns = 0
                var last = 0
                for (k in w0 + 1 until minOf(steps, w0 + 9)) {
                    val d = lum[k][i] - lum[k - 1][i]
                    if (abs(d) < .1) continue
                    val sign = if (d > 0) 1 else -1
                    if (sign != last) turns++
                    last = sign
                }
                worst = maxOf(worst, turns)
            }
            if (worst > 6) flashing++
        }
        println(
            "FLASH share a step: " + shares.joinToString { "%.4f".format(it) } +
                "; flashing past 3 a second: $flashing px²",
        )
        assertTrue(flashing <= FLASH_AREA, "flashing past 3 a second over $flashing px²")
    }

    /** The scene's pixels with one body under it, [shown] or not, frozen at [p] of loss [n] of seed 5 or with none. */
    private fun throughHole(
        n: Int,
        p: Double?,
        shown: Boolean,
    ): IntArray {
        var px = IntArray(0)
        runComposeUiTest {
            var show by mutableStateOf(true)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        ProvideMutantClock(MutantClock(startMs = 1_000.0, frozen = true)) {
                            KeyframeLossBody(
                                Modifier.size(250.dp, 212.dp).background(MutantTheme.colors.page).testTag("s"),
                                MutantTheme.colors.page,
                                p,
                                {
                                    if (show) {
                                        MutantSurface(
                                            MutantBody.Card,
                                            "spec-card",
                                            Modifier.fillMaxSize().padding(8.dp),
                                        ) {}
                                    }
                                },
                                first = n,
                            )
                        }
                    }
                }
            }
            waitForIdle()
            // The loss froze over the body; now the body goes, as a screen's content may, with the band standing.
            show = shown
            waitForIdle()
            val m = onNodeWithTag("s").captureToImage().toPixelMap()
            px =
                IntArray(m.width * m.height) { i ->
                    val c = m[i % m.width, i / m.width]
                    ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
                }
        }
        return px
    }

    private fun near(
        a: Int,
        b: Int,
    ) = (0..2).all { abs((a shr (8 * it) and 255) - (b shr (8 * it) and 255)) <= 2 }

    /**
     * The engine clears half the blocks of the band's first and last rows (`gone`) on its canvas, which lies over the
     * bodies: through such a block the screen shows as it is — the body while it stands, what is under it once it goes.
     * Read on a cleared block's first row, which no block's drag reaches (every pull is at least 1 dp down).
     */
    @Test
    fun `through a cleared block the screen shows as it is`() {
        val (n, hole) =
            (1..40)
                .asSequence()
                .map { it to moshPlan("5", it, 250, 212) }
                .mapNotNull { (n, plan) ->
                    plan.runs
                        .firstOrNull {
                            it.gone && it.y == plan.y0 && plan.y0 >= 24 && it.w >= 32 && it.x >= 24 &&
                                it.x + it.w <= 226
                        }?.let { n to it }
                }.first()
        val xs = hole.x + 4 until hole.x + hole.w - 4
        val at = { px: IntArray -> xs.map { px[hole.y * 250 + it] } }
        val body = at(throughHole(n, null, shown = true))
        val bare = at(throughHole(n, null, shown = false))
        val bodyShows = body.indices.count { !near(body[it], bare[it]) }
        assertTrue(bodyShows * 2 > body.size, "the body is under the block on $bodyShows of ${body.size} px")
        val p = 2 * 125.0 / moshPlan("5", n, 250, 212).durationMs
        val standing = at(throughHole(n, p, shown = true))
        val gone = at(throughHole(n, p, shown = false))
        assertEquals(
            0,
            body.indices.count { !near(standing[it], body[it]) },
            "px off the body through block $hole of loss $n",
        )
        assertEquals(
            0,
            body.indices.count {
                !near(gone[it], bare[it])
            },
            "px off the bare screen through block $hole of loss $n",
        )
    }

    private companion object {
        /** `flash.keyframe-loss`: the area threshold, px² (dp² at density 1). */
        const val FLASH_AREA = 21_824
    }
}
