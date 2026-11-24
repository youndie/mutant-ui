package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-192, the designer's answer 15: pressed, the text button's label and the snackbar's action drop 1 dp and a solid 2 dp
 * line in the label's colour stands under it (the text button's in its tracking divider's place); released, the rest
 * picture is back on the next frame; under reduce motion the same, at once. In both themes, at density 1 and 2, with
 * the clock running and paused.
 *
 * Every reading is over an area: the ink's mean row over the label's box (an integer offset moves the raster whole, so
 * the shift is exact), the share of the line's band in the label's ink, and the count of pixels that differ from the
 * rest frame. None hangs on an edge pixel.
 */
@OptIn(ExperimentalTestApi::class)
class TextButtonsPressedTest {
    private val palettes = listOf("Эфир" to MutantPalettes.Efir, "Копия" to MutantPalettes.Kopiya)

    /** The frames of one press: at rest, the frame after the press, 300 ms into it, the frame after the release. */
    private class Film(
        val rest: PixelMap,
        val pressed: PixelMap,
        val held: PixelMap,
        val released: PixelMap,
        /** The label's box at rest, in the stage's pixels. */
        val label: Rect,
    )

    private fun ComposeUiTest.stage() = onNodeWithTag("stage").captureToImage().toPixelMap()

    private fun ComposeUiTest.box(
        text: String,
        d: Float,
    ): Rect {
        val stage = onNodeWithTag("stage").getBoundsInRoot()
        return onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot().let {
            Rect(
                (it.left - stage.left).value * d,
                (it.top - stage.top).value * d,
                (it.right - stage.left).value * d,
                (it.bottom - stage.top).value * d,
            )
        }
    }

    /** A text button pressed through its own source: a pointer would also focus it, and draw the brackets. */
    private fun textButton(
        colors: MutantColors,
        d: Float,
        reduceMotion: Boolean,
    ): Film {
        var film: Film? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            val source = MutableInteractionSource()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(d)) {
                    MutantTheme(colors = colors, reduceMotion = reduceMotion) {
                        Box(Modifier.size(240.dp, 96.dp).background(colors.background).testTag("stage")) {
                            Box(Modifier.padding(16.dp)) { MutantTextButton(LABEL, {}, interactionSource = source) }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(SETTLE_MS)
            val label = box(LABEL, d)
            val rest = stage()
            val press = PressInteraction.Press(Offset.Zero)
            runBlocking { source.emit(press) }
            mainClock.advanceTimeByFrame()
            val pressed = stage()
            mainClock.advanceTimeBy(HELD_MS)
            val held = stage()
            runBlocking { source.emit(PressInteraction.Release(press)) }
            mainClock.advanceTimeByFrame()
            film = Film(rest, pressed, held, stage(), label)
        }
        return film!!
    }

    /** A snackbar's action pressed by a finger, the real path: down on the word, held, lifted. */
    private fun snackbar(
        colors: MutantColors,
        d: Float,
        reduceMotion: Boolean,
    ): Film {
        var film: Film? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(d)) {
                    MutantTheme(colors = colors, reduceMotion = reduceMotion) {
                        Column(Modifier.size(360.dp, 96.dp).background(colors.background).testTag("stage")) {
                            MutantSnackbar(
                                "Добавлено в очередь",
                                Modifier.width(328.dp).padding(16.dp, 16.dp),
                                actionLabel = ACTION,
                            )
                        }
                    }
                }
            }
            // Past its ride in and its tune-in.
            mainClock.advanceTimeBy(SETTLE_MS)
            val label = box(ACTION, d)
            val rest = stage()
            val word = onNodeWithText(ACTION, useUnmergedTree = true)
            word.performTouchInput { down(center) }
            mainClock.advanceTimeByFrame()
            val pressed = stage()
            mainClock.advanceTimeBy(HELD_MS)
            val held = stage()
            word.performTouchInput { up() }
            mainClock.advanceTimeByFrame()
            film = Film(rest, pressed, held, stage(), label)
        }
        return film!!
    }

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < NEAR

    /** The mean row of the [ink]'s pixels over the label's box and the drop's room under it. */
    private fun inkRow(
        px: PixelMap,
        box: Rect,
        ink: Color,
        d: Float,
    ): Double {
        var sum = 0.0
        var n = 0
        for (y in box.top.roundToInt() until (box.bottom + 1.5f * d).roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                if (near(px[x, y], ink)) {
                    sum += y
                    n++
                }
            }
        }
        assertTrue(n > 50, "the label's ink is there to read: $n pixels")
        return sum / n
    }

    /** The share of the 2 dp band [gap] dp under the label's box that stands in the [ink], over the label's width. */
    private fun lineShare(
        px: PixelMap,
        box: Rect,
        ink: Color,
        d: Float,
        gap: Float,
    ): Double {
        val top = (box.bottom + gap * d).roundToInt()
        // A pixel in from each end, and the band's middle rows only, so no edge's anti-aliasing is read.
        val x0 = box.left.roundToInt() + 1
        val x1 = box.right.roundToInt() - 1
        var hit = 0
        var all = 0
        for (y in top until top + (2 * d).roundToInt()) {
            for (x in x0 until x1) {
                all++
                if (near(px[x, y], ink)) hit++
            }
        }
        return hit.toDouble() / all
    }

    /** Pixels of [a] differing from [b] over the label's box and the band under it. */
    private fun differ(
        a: PixelMap,
        b: PixelMap,
        box: Rect,
        d: Float,
    ): Int {
        var n = 0
        for (y in box.top.roundToInt() until (box.bottom + 8 * d).roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                val p = a[x, y]
                val q = b[x, y]
                if (abs(p.red - q.red) + abs(p.green - q.green) + abs(p.blue - q.blue) > .02f) n++
            }
        }
        return n
    }

    private fun check(
        what: String,
        film: Film,
        ink: Color,
        d: Float,
        gap: Float,
        dividerAtRest: Color?,
    ) {
        val rest = inkRow(film.rest, film.label, ink, d)
        for ((frame, px) in listOf("pressed" to film.pressed, "held" to film.held)) {
            val drop = inkRow(px, film.label, ink, d) - rest
            assertTrue(abs(drop - d) < .3, "$what, $frame: the label drops 1 dp, ${d}px; it dropped $drop")
            val share = lineShare(px, film.label, ink, d, gap)
            assertTrue(share > .9, "$what, $frame: a solid line in the label's ink under it; ${"%.2f".format(share)}")
            if (dividerAtRest != null && dividerAtRest != ink) {
                val blue = lineShare(px, film.label, dividerAtRest, d, gap)
                assertTrue(
                    blue < .02,
                    "$what, $frame: no tracking divider; ${"%.2f".format(blue)} of the band in its blue",
                )
            }
        }
        val atRest = lineShare(film.rest, film.label, ink, d, gap)
        assertTrue(atRest < .8, "$what: at rest there is no solid line; ${"%.2f".format(atRest)}")
        val back = inkRow(film.released, film.label, ink, d) - rest
        assertTrue(abs(back) < .1, "$what, released: the label back at rest on the next frame; it stands $back lower")
        val released = lineShare(film.released, film.label, ink, d, gap)
        assertTrue(abs(released - atRest) < .02, "$what, released: the band as at rest; $released against $atRest")
        val moved = differ(film.pressed, film.rest, film.label, d)
        val left = differ(film.released, film.rest, film.label, d)
        assertTrue(moved > 100 * d, "$what: the press changes the picture; $moved pixels")
        assertTrue(left * 20 < moved, "$what, released: the rest picture again; $left pixels differ, $moved pressed")
    }

    @Test
    fun `the text button pressed drops its label over a solid line, and is at rest the frame after`() {
        for ((name, colors) in palettes) {
            for (d in listOf(1f, 2f)) {
                val film = textButton(colors, d, reduceMotion = false)
                check("text button, $name, density $d", film, colors.textAccent, d, TEXT_GAP, colors.noSignal)
            }
        }
    }

    @Test
    fun `the text button under reduce motion shows the same at once`() {
        for ((name, colors) in palettes) {
            val film = textButton(colors, 2f, reduceMotion = true)
            check("text button paused, $name", film, colors.textAccent, 2f, TEXT_GAP, colors.noSignal)
        }
    }

    @Test
    fun `the snackbar's action pressed drops its word over a solid line, and is at rest the frame after`() {
        for ((name, colors) in palettes) {
            for (d in listOf(1f, 2f)) {
                val film = snackbar(colors, d, reduceMotion = false)
                check("snackbar, $name, density $d", film, colors.onNoSignalAccent, d, TEXT_GAP, null)
            }
        }
    }

    @Test
    fun `the snackbar's action under reduce motion shows the same at once`() {
        for ((name, colors) in palettes) {
            val film = snackbar(colors, 2f, reduceMotion = true)
            check("snackbar paused, $name", film, colors.onNoSignalAccent, 2f, TEXT_GAP, null)
        }
    }

    private companion object {
        const val LABEL = "Подробнее"
        const val ACTION = "ОТМЕНИТЬ"
        const val SETTLE_MS = 800L
        const val HELD_MS = 300L

        /** The label's box to the line: the text button's divider gap, which the snackbar's line takes too (dp). */
        const val TEXT_GAP = 5f

        /** Colour distance, summed over the channels, under which a pixel reads as the ink. */
        const val NEAR = .25f
    }
}
