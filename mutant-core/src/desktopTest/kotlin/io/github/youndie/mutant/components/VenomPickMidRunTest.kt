package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-172: a pick during a venom run goes on from where the slime stands. The segmented button, the menu and the drawer,
 * picked 0 → 2 and then again at moments across the run, never change more pixels in one frame than a run from rest
 * does. Before, the window stood whole on the slot picked first in the pick's frame: 8 000–10 000 pixels on the
 * segmented button, 15 000–20 000 on the menu and the drawer, against a run's 3 000–6 000.
 *
 * The clock is held and moved a 16 ms frame at a time. A pixel counts as changed when a channel moves by more than 16.
 * The bound is a run from rest's largest frame (0 → 2 and back, measured in the same test) plus the body's largest tick
 * at rest: a frame of a run and a tick of the body boiling under it can fall together, and they do (the drawer's
 * 3 000-pixel venom frame and its 1 070-pixel tick, 4 673 together).
 */
@OptIn(ExperimentalTestApi::class)
class VenomPickMidRunTest {
    private val names = listOf("On air", "Queue", "Search", "Settings")

    /** Where the second pick lands, ms after the first, across a run of 720 ms. */
    private val moments = listOf(48L, 160L, 304L, 400L, 496L, 608L)

    private fun changed(
        a: BufferedImage,
        b: BufferedImage,
    ): Int {
        var n = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                val p = a.getRGB(x, y)
                val q = b.getRGB(x, y)
                val d =
                    maxOf(
                        abs((p shr 16 and 255) - (q shr 16 and 255)),
                        abs((p shr 8 and 255) - (q shr 8 and 255)),
                        abs((p and 255) - (q and 255)),
                    )
                if (d > 16) n++
            }
        }
        return n
    }

    /**
     * The pixels changed in each frame from [from] to [until] ms (the first pick's frame is 0), the [picks] made at
     * their ms, keyed by each frame's end.
     */
    private fun frames(
        face: String,
        stepped: Boolean,
        picks: List<Pair<Long, Int>>,
        from: Long,
        until: Long,
    ): Map<Long, Int> {
        val out = LinkedHashMap<Long, Int>()
        runComposeUiTest {
            var chosen by mutableIntStateOf(0)
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(MutantPalettes.Efir, steppedMotion = stepped) {
                        Box(Modifier.background(MutantTheme.colors.page).padding(16.dp).testTag("face")) {
                            when (face) {
                                "segmented" -> {
                                    Box(Modifier.width(280.dp)) {
                                        SegmentedBody(
                                            listOf("SP · 2 h", "LP · 4 h", "EP · 6 h"),
                                            chosen,
                                            {},
                                            Modifier,
                                            true,
                                            "seg",
                                            "seg/pill",
                                        )
                                    }
                                }

                                "menu" -> {
                                    Box(Modifier.padding(bottom = 30.dp)) {
                                        MenuBody(names, chosen, {}, Modifier, "menu", "menu-pill")
                                    }
                                }

                                else -> {
                                    MutantScreenClock {
                                        Box(Modifier.size(320.dp, 300.dp)) {
                                            MutantNavigationDrawer(
                                                true,
                                                {},
                                                names.mapIndexed { i, n -> MutantDrawerItem("CH 0${i + 1}", n) },
                                                chosen,
                                                {},
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // The drawer slides in first.
            mainClock.advanceTimeBy(if (face == "drawer") 1_000 else 200)
            var prev: BufferedImage? = null
            var t = 0L
            while (t < until) {
                // The frame before the pick's, captured before the pick: a capture composes what it is given.
                if (t >= from - 16 && prev == null) prev = onNodeWithTag("face").captureToImage().toAwtImage()
                for ((at, to) in picks) if (at == t) chosen = to
                mainClock.advanceTimeByFrame()
                t += 16
                if (prev != null) {
                    val img = onNodeWithTag("face").captureToImage().toAwtImage()
                    out[t] = changed(prev, img)
                    prev = img
                }
            }
        }
        return out
    }

    /** A run from rest's largest frame plus the body's largest tick at rest, as the bound of a frame. */
    private fun bound(
        face: String,
        stepped: Boolean,
    ): Int {
        val f = frames(face, stepped, listOf(0L to 2, 1_200L to 0), 0, 1_984)
        val run = f.filterKeys { it <= 800 || it > 1_200 }.values.max()
        val tick = f.filterKeys { it in 816..1_200 }.values.max()
        return run + tick
    }

    private fun sweep(
        face: String,
        stepped: Boolean,
    ) {
        val bound = bound(face, stepped)
        val over = mutableListOf<String>()
        val cases =
            moments.flatMapIndexed { i, m ->
                // Back to where the run came from, and on to a third slot; under steppedMotion one of the two in turn.
                val targets = if (stepped) listOf(i % 2) else listOf(0, 1)
                targets.map { listOf(0L to 2, m to it) }
            } +
                if (stepped) emptyList() else listOf(listOf(0L to 1, 64L to 2, 128L to 1, 192L to 0))
        for (picks in cases) {
            val second = picks[1].first
            // From the frame of the second pick until the last run has landed, at most 1.5 runs after it.
            val f = frames(face, stepped, picks, second + 16, picks.last().first + 1_200)
            val worst = f.maxBy { it.value }
            if (worst.value > bound) {
                over +=
                    "picks ${picks.joinToString { "${it.second} at ${it.first}" }}: ${worst.value} at ${worst.key} ms"
            }
        }
        assertEquals(emptyList(), over, "frames changing more pixels than a run's largest plus a tick ($bound)")
    }

    @Test
    fun `a pick during the segmented button's run changes no frame more than a run does`() = sweep("segmented", false)

    @Test
    fun `a pick during the segmented button's stepped run changes no frame more than a run does`() =
        sweep("segmented", true)

    @Test
    fun `a pick during the menu's run changes no frame more than a run does`() = sweep("menu", false)

    @Test
    fun `a pick during the menu's stepped run changes no frame more than a run does`() = sweep("menu", true)

    @Test
    fun `a pick during the drawer's run changes no frame more than a run does`() = sweep("drawer", false)

    @Test
    fun `a pick during the drawer's stepped run changes no frame more than a run does`() = sweep("drawer", true)
}
