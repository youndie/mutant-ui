package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-82: under a running clock a resting body is rasterised once per tick, ahead with its frame, and drawn as that
 * image until the next tick; what it draws is what the vector body draws, tick for tick, but for the rounding of
 * anti-aliased edges.
 */
@OptIn(ExperimentalTestApi::class)
class BodyAsImageTest {
    private class Count {
        var asImage = 0
        var asVectors = 0
        var rasters = 0
    }

    private fun ComposeUiTest.card(
        asImage: Boolean,
        count: Count,
        density: Float = 1f,
        print: Boolean = false,
    ) {
        // Before the content: the kit's clock asks for frames for ever, so the content never goes idle.
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density),
                LocalMutantBodyAsImage provides asImage,
                LocalMutantFrameProbe provides { _, image, _ -> if (image) count.asImage++ else count.asVectors++ },
                LocalMutantRasterProbe provides { _, _ -> count.rasters++ },
            ) {
                ProvideMutantClock(androidx.compose.runtime.remember { MutantClock() }) {
                    // «Копия»'s paper shows the speckle's ink far over the edges' rounding; «Эфир»'s dark fill does not.
                    CompositionLocalProvider(
                        LocalMutantColors provides if (print) MutantPalettes.Kopiya else MutantPalettes.Efir,
                    ) {
                        Box(Modifier.size(360.dp, 120.dp).testTag("stage")) {
                            MutantSurface(MutantBody.Card, "card/b82", Modifier.size(328.dp, 80.dp)) {
                                BasicText("Кассета 07", Modifier.padding(16.dp).mutantCleanZone())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.pixels(): List<Color> =
        onNodeWithTag("stage").captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    /** The largest difference of two pixels' premultiplied channels, in levels of 255. */
    private fun premultipliedGap(
        a: Color,
        b: Color,
    ): Int {
        fun level(
            c: Float,
            alpha: Float,
        ) = kotlin.math.round(c * alpha * 255).toInt()
        return maxOf(
            kotlin.math.abs(level(a.red, a.alpha) - level(b.red, b.alpha)),
            kotlin.math.abs(level(a.green, a.alpha) - level(b.green, b.alpha)),
            kotlin.math.abs(level(a.blue, a.alpha) - level(b.blue, b.alpha)),
            kotlin.math.abs(kotlin.math.round(a.alpha * 255).toInt() - kotlin.math.round(b.alpha * 255).toInt()),
        )
    }

    /** Two seconds of clock in whole frames, real time between them for the background, a picture at each tick. */
    private fun ComposeUiTest.ticks(): List<List<Color>> {
        val shots = ArrayList<List<Color>>()
        repeat(120) { i ->
            mainClock.advanceTimeByFrame()
            Thread.sleep(6)
            if (i % 8 == 7) shots += pixels()
        }
        return shots
    }

    @Test
    fun `once the cache settles, resting ticks are drawn from an image rasterised once per tick`() =
        runComposeUiTest {
            val count = Count()
            card(asImage = true, count)
            ticks()
            assertTrue(count.asImage >= 8, "ticks drawn from an image: ${count.asImage}, as vectors ${count.asVectors}")
            // Sixteen ticks in two seconds: an image per tick, not one per frame (120).
            assertTrue(count.rasters in count.asImage..20, "images rasterised: ${count.rasters}")
        }

    @Test
    fun `a body drawn from its image draws exactly what the vector body draws`() {
        fun run(
            asImage: Boolean,
            density: Float,
            print: Boolean,
        ): Pair<List<List<Color>>, Count> {
            val count = Count()
            var shots = emptyList<List<Color>>()
            runComposeUiTest {
                card(asImage, count, density, print)
                shots = ticks()
            }
            return shots to count
        }
        // B-115: and the Pixel 6a's 2.625, where an image cut on whole scene units stood off the vectors by up to half a pixel.
        for ((density, print) in listOf(1f to false, 2f to false, 1f to true, 2.625f to true)) {
            val (image, used) = run(asImage = true, density, print)
            val (vectors, none) = run(asImage = false, density, print)
            assertEquals(0, none.asImage, "no image with the switch off")
            assertTrue(used.asImage >= 8, "the ticks compared were drawn from images: ${used.asImage}")
            // The first tick of an off-frame body draws no body yet (B-50); every tick after it is compared.
            // An image is rasterised apart and then drawn, a vector body is recorded and replayed: their anti-aliased
            // edges round apart, by a level on about 1 % of pixels and by more than 16 levels on 0.013–0.03 %
            // (measured 2026-11-15, the ticks before the cache settles 0). Held as goldens are: at most 0.05 % of
            // pixels more than 16 levels apart. A body one pixel off, or a layer missing, is thousands.
            for (i in 1 until image.size) {
                val off = image[i].indices.count { premultipliedGap(image[i][it], vectors[i][it]) > EDGE_LEVELS }
                assertTrue(
                    off <= image[i].size * EDGE_SHARE,
                    "density $density, print $print, tick ${i + 1}: $off of ${image[i].size} pixels more than $EDGE_LEVELS levels apart",
                )
            }
        }
    }

    private companion object {
        const val EDGE_LEVELS = 16
        const val EDGE_SHARE = .0005
    }
}
