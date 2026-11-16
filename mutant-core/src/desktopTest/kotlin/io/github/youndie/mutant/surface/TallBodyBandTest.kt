package io.github.youndie.mutant.surface

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.tokens.MutantBody
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-86: a body taller than the window rasterises ahead only the band around what the window shows, cut on the whole
 * image's pixel grid; scrolled past that band, it draws its vectors until a tick's image covers it again.
 */
@OptIn(ExperimentalTestApi::class)
class TallBodyBandTest {
    @Test
    fun `an image cut to a band holds the whole image's pixels inside it`() {
        val gen = MutantGenerator("5")
        val g = gen.instance(slot(360.0, 3_000.0), "card/b86")
        for (density in listOf(1f, 2f, 2.625f)) {
            val whole = rasterisedBody(g, 0.0, 0.0, density, LayoutDirection.Ltr) { drawSomething() }
            val band = Rect(37.4f, 811.3f * density, 290.6f * density, 1_904.8f * density)
            val cut = rasterisedBody(g, 0.0, 0.0, density, LayoutDirection.Ltr, band) { drawSomething() }
            assertEquals(whole.whole, cut.whole, "density $density: the same body")
            assertTrue(cut.image.height < whole.image.height / 2, "density $density: ${cut.image.height} rows")
            val w = whole.image.toPixelMap()
            val c = cut.image.toPixelMap()
            val dx = cut.offset.x - whole.offset.x
            val dy = cut.offset.y - whole.offset.y
            assertTrue(cut.offset.y <= kotlin.math.floor(band.top), "density $density: the cut starts by the band")
            // Skia rasterises an edge a little apart on a smaller canvas: measured 2026-11-16, 0.002–0.019 % of pixels
            // more than 16 levels apart (worst 41–56), on edges inside the band, not at its cut. Held as B-82 holds an
            // image against the vectors; a cut a pixel off the whole image's grid is a whole edge of every shape.
            var off = 0
            for (y in 0 until c.height) {
                for (x in 0 until c.width) if (premultipliedGap(c[x, y], w[x + dx, y + dy]) > EDGE_LEVELS) off++
            }
            assertTrue(
                off <= c.width * c.height * EDGE_SHARE,
                "density $density: $off of ${c.width * c.height} pixels more than $EDGE_LEVELS levels from the whole image",
            )
            assertTrue(cut.covers(band), "density $density: the cut covers its band")
            assertTrue(
                !cut.covers(band.translate(0f, band.height)),
                "density $density: a band further down is not covered",
            )
        }
    }

    @Test
    fun `a tall body rasterises the band around the window, not its whole height`() =
        runComposeUiTest {
            val bytes = ArrayList<Long>()
            val scroll = tallCard(asImage = true, bytes = bytes)
            repeat(40) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(6)
            }
            assertTrue(bytes.size >= 2, "images rasterised: ${bytes.size}")
            // The whole card is about 366 × 3 060 px at density 1; the window is 768 tall, the band twice that.
            val whole = 366L * 3_060 * 4
            assertTrue(bytes.last() < whole * 2 / 3, "the last image's bytes ${bytes.last()} against the whole $whole")
            assertEquals(0, scroll.value)
        }

    @Test
    fun `scrolled past its band, a tall body draws what its vectors draw, in the next frame and after`() {
        fun run(asImage: Boolean): Pair<List<List<Color>>, Pair<Int, Int>> {
            var shots = emptyList<List<Color>>()
            var counts = 0 to 0
            runComposeUiTest {
                var images = 0
                var vectors = 0
                var drawnAt = -1L
                val scroll =
                    tallCard(asImage, frames = { image ->
                        drawnAt = mainClock.currentTime
                        if (image) images++ else vectors++
                    })
                repeat(40) {
                    mainClock.advanceTimeByFrame()
                    Thread.sleep(6)
                }
                // To the frame that drew a tick: the next one draws no tick of its own.
                while (drawnAt != mainClock.currentTime) {
                    mainClock.advanceTimeByFrame()
                    Thread.sleep(6)
                }
                val before = pixels()
                // One and a half windows down, between two ticks: past the band the image was cut to.
                runBlocking { scroll.scrollTo(1_150) }
                mainClock.advanceTimeByFrame()
                val next = pixels()
                repeat(24) {
                    mainClock.advanceTimeByFrame()
                    Thread.sleep(6)
                }
                shots = listOf(before, next, pixels())
                counts = images to vectors
            }
            return shots to counts
        }
        val (image, used) = run(asImage = true)
        val (vectors, _) = run(asImage = false)
        assertTrue(used.first >= 3, "ticks drawn from images: ${used.first}")
        for ((i, name) in listOf("before the scroll", "the frame after it", "three ticks after").withIndex()) {
            val off = image[i].indices.count { premultipliedGap(image[i][it], vectors[i][it]) > EDGE_LEVELS }
            assertTrue(
                off <= image[i].size * EDGE_SHARE,
                "$name: $off of ${image[i].size} pixels more than $EDGE_LEVELS levels from the vector body",
            )
        }
    }

    private fun ComposeUiTest.tallCard(
        asImage: Boolean,
        bytes: MutableList<Long> = ArrayList(),
        frames: (image: Boolean) -> Unit = {},
    ): ScrollState {
        mainClock.autoAdvance = false
        lateinit var scroll: ScrollState
        setContent {
            scroll = rememberScrollState()
            CompositionLocalProvider(
                LocalMutantBodyAsImage provides asImage,
                LocalMutantFrameProbe provides { _, image, _ -> frames(image) },
                LocalMutantRasterProbe provides { _, b -> bytes += b },
            ) {
                ProvideMutantClock(remember { MutantClock() }) {
                    Box(Modifier.size(420.dp, 700.dp).testTag("stage")) {
                        Column(Modifier.verticalScroll(scroll)) {
                            Box(Modifier.padding(30.dp)) {
                                MutantSurface(MutantBody.Card, "card/b86", Modifier.size(360.dp, 3_000.dp)) {
                                    BasicText("Кассета 07", Modifier.padding(16.dp).mutantCleanZone())
                                }
                            }
                        }
                    }
                }
            }
        }
        return scroll
    }

    private fun ComposeUiTest.pixels(): List<Color> =
        onNodeWithTag("stage").captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

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

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSomething() {
        val wave =
            Path().apply {
                moveTo(3.3f, 0f)
                for (i in 1..60) lineTo(3.3f + 170f + 160f * kotlin.math.sin(i * .7f), i * 50.17f)
                close()
            }
        drawPath(wave, Color(0xFF2E8B57))
        drawPath(wave, Color.Black, style = Stroke(width = 1.7f))
        for (i in 0 until 400) drawRect(Color.Magenta, Offset(i * .91f % 350, i * 7.53f), Size(2.3f, 1.4f), alpha = .6f)
    }

    private fun slot(
        w: Double,
        h: Double,
    ) = Slot(
        ox = 0.0,
        oy = 0.0,
        w = w,
        h = h,
        r = 10.0,
        over = 24.0,
        dripShare = .4,
        wobble = 1.0,
        round = false,
        calm = 1.0,
        dead = true,
        marks = true,
        edge = true,
        textZone = doubleArrayOf(10.0, 12.0, 192.0, 40.0),
        cleanZone = doubleArrayOf(8.0, 10.0, 196.0, 44.0),
    )

    private companion object {
        const val EDGE_LEVELS = 16
        const val EDGE_SHARE = .0005
    }
}
