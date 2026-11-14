package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** B-10: the surface lays out as its content, draws its drips outside, measures its clean zone, keeps its instance. */
@OptIn(ExperimentalTestApi::class)
class MutantSurfaceTest {
    private val bg = MutantPalettes.Efir.background

    @Test
    fun `drips hang below the slot and the next element stands right under it`() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Box(Modifier.size(240.dp, 140.dp).background(bg).testTag("frame")) {
                        Column(Modifier.padding(20.dp)) {
                            MutantSurface(MutantBody.Btn, "btn/a", Modifier.size(200.dp, 48.dp).testTag("button")) {}
                            Box(Modifier.size(10.dp).testTag("next"))
                        }
                    }
                }
            }
            val button = onNodeWithTag("button").getBoundsInRoot()
            val next = onNodeWithTag("next").getBoundsInRoot()
            assertEquals(button.bottom, next.top, "the next element starts where the slot ends")
            assertEquals(48f, (button.bottom - button.top).value, "the slot keeps its size")
            val px = onNodeWithTag("frame").captureToImage().toPixelMap()
            // Below the slot (y 68..92 in the frame) the button's fill and ink are drawn: drips.
            var painted = 0
            for (y in 70 until 92) for (x in 30 until 210) if (px[x, y] != bg) painted++
            assertTrue(painted > 200, "drip pixels under the slot: $painted")
        }

    @Test
    fun `the content's marked bounds become the clean zone with the engine's padding`() =
        runComposeUiTest {
            var seen: Instance? = null
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantSurface(MutantBody.Btn, "btn/a", Modifier.size(200.dp, 48.dp), probe = { seen = it }) {
                        Box(Modifier.size(200.dp, 48.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(96.dp, 18.dp).mutantCleanZone())
                        }
                    }
                }
            }
            waitForIdle()
            val instance = assertNotNull(seen)
            val tz = instance.slot.textZone
            assertEquals(listOf(52.0 - 6, 15.0 - 2, 96.0 + 12, 18.0 + 4), tz.toList(), "tz")
            val cz = instance.slot.cleanZone
            assertEquals(listOf(52.0 - 8, 15.0 - 4, 96.0 + 16, 18.0 + 8), cz.toList(), "cz")
        }

    @Test
    fun `the instance is built once and its frames follow the clock's ticks`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val clock = MutantClock()
            val instances = HashSet<Instance>()
            var frames = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    ProvideMutantClock(clock) {
                        MutantSurface(MutantBody.Card, "card/a", Modifier.size(328.dp, 80.dp), probe = {
                            instances += it
                            frames++
                        }) {
                            BasicText("Кассета 07", Modifier.padding(16.dp).mutantCleanZone())
                        }
                    }
                }
            }
            repeat(40 * 8) { mainClock.advanceTimeByFrame() }
            assertTrue(clock.restTick >= 36, "the clock ran: tick ${clock.restTick}")
            assertTrue(
                frames in 30..clock.restTick.toInt() + 3,
                "a frame per tick: $frames frames over ${clock.restTick} ticks",
            )
            assertEquals(1, instances.size, "the instance was built once")
        }

    @Test
    fun `mosaic, flow lines and dead pixels stay out of the clean zone over 1000 keys`() {
        val gen =
            io.github.youndie.mutant.gen
                .MutantGenerator("5")
        val tz = doubleArrayOf(16.0 - 6, 14.0 - 2, 180.0 + 12, 36.0 + 4)
        val cz = doubleArrayOf(tz[0] - 2, tz[1] - 2, tz[2] + 4, tz[3] + 4)
        var lines = 0
        var blocks = 0
        for (i in 0 until 1000) {
            val slot =
                io.github.youndie.mutant.gen.Slot(
                    ox = 0.0,
                    oy = 0.0,
                    w = 328.0,
                    h = 80.0,
                    r = 10.0,
                    over = 24.0,
                    dripShare = .4,
                    wobble = 1.0,
                    round = false,
                    calm = 1.0,
                    dead = true,
                    marks = true,
                    edge = true,
                    textZone = tz,
                    cleanZone = cz,
                )
            val g = gen.instance(slot, "card/$i")
            for (b in g.mosaic.filter { it.y >= 0 && it.x >= 0 && it.x < slot.w }) {
                blocks++
                val overlaps =
                    b.x < cz[0] + cz[2] && b.x + b.size > cz[0] && b.y < cz[1] + cz[3] && b.y + b.size > cz[1]
                assertTrue(!overlaps, "card/$i: a mosaic block at ${b.x}, ${b.y} in the clean zone")
            }
            for (m in g.flowLines) {
                lines++
                for (p in gen.flowLineAt(m, 1.0).points) {
                    val inside = p[0] > tz[0] && p[0] < tz[0] + tz[2] && p[1] > tz[1] && p[1] < tz[1] + tz[3]
                    assertTrue(!inside, "card/$i: a flow line at ${p[0]}, ${p[1]} in the clean zone")
                }
            }
            val dead = gen.deadPixels(g)
            assertTrue(dead.pixels.all { it.x >= cz[0] + cz[2] }, "card/$i: a dead pixel left of the clean zone's edge")
        }
        assertTrue(lines > 2000 && blocks > 10000, "lines $lines, blocks $blocks")
    }
}
