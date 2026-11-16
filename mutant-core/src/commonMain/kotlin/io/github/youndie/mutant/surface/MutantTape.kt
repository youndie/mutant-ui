package io.github.youndie.mutant.surface

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.Mulberry32
import io.github.youndie.mutant.gen.jsRound
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.theme.LocalMutantCleanSignal
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.floor

/*
 * The Tape («Лента», research D10): one layer over the screen, as the engine's `tapeHtml` and `stageStep` draw
 * it — scanlines, the tracking band passing for 2100 ms every 7000 ms, the grain stepping per tick, the head's
 * noise at the bottom edge. It steps on the resting tick, stands still under the pause, and is off under clean
 * signal.
 */

/** The grain's tile: 44 strokes in 90 × 180 dp, seeded by the stage (`rng('tape', stage)`). */
internal class TapeGrain(
    /** `x, y, width, opacity` per stroke. */
    val strokes: List<DoubleArray>,
) {
    companion object {
        const val W: Float = 90f
        const val H: Float = 180f

        fun of(stage: String): TapeGrain {
            val r = Mulberry32("tape", stage)
            return TapeGrain(
                List(44) {
                    val x = jsRound(r.next() * 90)
                    val y = jsRound(r.next() * 180)
                    val v = r.next()
                    doubleArrayOf(x, y, if (v > .8) 3.0 else 1.0, jsToFixed(.1 + .06 * v, 3))
                },
            )
        }
    }
}

/** The Tape's colours per theme: scanline and band in «Эфир» are light on dark, in «Копия» ink on paper with no scanlines. */
private class TapeInk(
    val scanline: Float,
    val band: Color,
    val bandAlpha: Float,
    val bandTop: Color,
    val bandTopAlpha: Float,
    val bandBottom: Color,
    val bandBottomAlpha: Float,
    val grain: Color,
    val noiseBase: Double,
    val noiseRange: Double,
)

private fun inkOf(c: MutantColors): TapeInk =
    if (c.print) {
        val ink = Color(20, 17, 15)
        TapeInk(0f, ink, .05f, ink, .28f, ink, .1f, Color(0xFF6B5F48), .1, .1)
    } else {
        val phosphor = Color(233, 242, 228)
        TapeInk(.075f, phosphor, .07f, phosphor, .3f, Color(5, 6, 12), .5f, Color.White, .3, .4)
    }

/**
 * The tracking band's top at [t] over a screen [h] tall, or null while it is not passing: it shows for the
 * first 30 % of each [MutantMotion.TRACKING_EVERY_MS] cycle (2100 ms), entering at the bottom edge and leaving
 * 30 dp above the top, rounded to whole dp (`stageStep`).
 */
internal fun tapeBandTop(
    t: Double,
    h: Float,
): Float? {
    val phase = (t % MutantMotion.TRACKING_EVERY_MS) / MutantMotion.TRACKING_EVERY_MS
    return if (phase < TRACKING_SHARE) jsRound(h - (phase / TRACKING_SHARE) * (h + 30)).toFloat() else null
}

/** A part of the Tape, as B-77's bench leaves one out to read what it costs. */
internal enum class TapePart { Lines, Band, Grain, Noise }

/** The parts of the Tape left out: for B-77's bench only, never for a consumer. */
internal val LocalMutantTapeSkip = androidx.compose.runtime.staticCompositionLocalOf<Set<TapePart>> { emptySet() }

/** The share of the cycle the band passes in. */
private const val TRACKING_SHARE = .3

/**
 * Draws the Tape over [size] at time [t] for [stage]. The band's top is `round(H − (phase / 0.3) · (H + 30))`
 * during the first 30 % of each 7 s cycle; the grain's tile moves by (37, 71) per tick; the noise comes from the
 * stream `hd` of the stage and the tick, as `stageStep` draws it.
 */
internal fun DrawScope.drawTape(
    stage: String,
    t: Double,
    colors: MutantColors,
    w: Float = size.width,
    h: Float = size.height,
    grain: TapeGrain = TapeGrain.of(stage),
    skip: Set<TapePart> = emptySet(),
) {
    val ink = inkOf(colors)
    if (ink.scanline > 0 && TapePart.Lines !in skip) {
        var y = 0f
        while (y < h) {
            drawRect(Color.Black, Offset(0f, y), Size(w, 1f), alpha = ink.scanline)
            y += 3f
        }
    }
    val fi = floor(t / MutantClock.SCENE_TICK_MS).toLong()
    val by = tapeBandTop(t, h)
    if (by != null && TapePart.Band !in skip) {
        drawRect(ink.bandTop, Offset(0f, by), Size(w, 1f), alpha = ink.bandTopAlpha)
        drawRect(ink.band, Offset(0f, by + 1), Size(w, 22f), alpha = ink.bandAlpha)
        drawRect(ink.bandBottom, Offset(0f, by + 23), Size(w, 1f), alpha = ink.bandBottomAlpha)
    }
    val gx = ((fi * 37) % 90).toFloat()
    val gy = ((fi * 71) % 180).toFloat()
    var j = floor(-gy / TapeGrain.H).toInt()
    while (TapePart.Grain !in skip && gy + j * TapeGrain.H < h) {
        var i = floor(-gx / TapeGrain.W).toInt()
        while (gx + i * TapeGrain.W < w) {
            val ox = gx + i * TapeGrain.W
            val oy = gy + j * TapeGrain.H
            clipRect(ox, oy, ox + TapeGrain.W, oy + TapeGrain.H) {
                for (s in grain.strokes) {
                    drawRect(
                        ink.grain,
                        Offset(ox + s[0].toFloat(), oy + s[1].toFloat()),
                        Size(s[2].toFloat(), 1f),
                        alpha = s[3].toFloat(),
                    )
                }
            }
            i++
        }
        j++
    }
    val r = Mulberry32("hd", "$stage$fi")

    fun noise(
        y0: Float,
        hh: Double,
    ) {
        val x = jsToFixed(r.next() * w, 1).toFloat()
        val y = y0 + jsRound(r.next() * hh).toFloat()
        val nw = jsToFixed(6 + r.next() * 40, 1).toFloat()
        val a = jsToFixed(ink.noiseBase + ink.noiseRange * r.next(), 2).toFloat()
        drawRect(ink.grain, Offset(x, y), Size(nw, 1f), alpha = a)
    }
    if (TapePart.Noise in skip) return
    if (by != null) repeat(14) { noise(by, 22.0) }
    repeat(jsRound(w / 10.0).toInt()) { noise(h - 12, 11.0) }
}

/**
 * The Tape («Лента») over this element: scanlines, a grain stepping eight times a second, a tracking band
 * passing for 2.1 s every 7 s and the head's noise at the bottom edge, as the design draws them over a screen
 * (research D10). Put it once on the screen's root, not on each component; it is drawn over the content and
 * takes no input.
 *
 * It steps on the theme's clock, so it stands still under reduce motion, and it is not drawn under
 * «Чистый сигнал» ([MutantTheme]'s `cleanSignal`). Its grain and noise are seeded by the screen.
 */
public fun Modifier.mutantTape(): Modifier =
    composed {
        if (LocalMutantCleanSignal.current) return@composed this
        val clock = readMutantClock()
        val colors = LocalMutantColors.current
        val stage = LocalMutantStage.current
        val grain = remember(stage) { TapeGrain.of(stage) }
        val skip = LocalMutantTapeSkip.current
        drawWithContent {
            drawContent()
            // A frozen clock is the engine's `data-mu-t`, drawn at that very time; a running one steps the Tape on
            // the scene's tick, which the engine draws at the first frame past it, within a frame of this.
            val t = if (clock.frozen) clock.nowMs else clock.sceneTick * MutantClock.SCENE_TICK_MS
            val d = density
            val w = size.width / d
            val h = size.height / d
            scale(d, Offset.Zero) {
                clipRect(0f, 0f, w, h) { drawTape(stage, t, colors, w, h, grain, skip) }
            }
        }
    }
