package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import io.github.youndie.mutant.gen.ErrorBlockColor
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.MosaicSide
import io.github.youndie.mutant.gen.MosaicTile
import io.github.youndie.mutant.gen.Mulberry32
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.blockMeetsCleanZone
import io.github.youndie.mutant.gen.errorBlocks
import io.github.youndie.mutant.gen.jsRound
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.tokens.MutantColors
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** What a body shows besides rest (handoff README, «Время, состояния, взаимодействие»). */
internal enum class BodyState { Rest, Loading, Success, Disabled, Error }

private fun ch(v: Float): Int = (v * 255 + .5f).toInt()

/**
 * `mix`: the colour a → b by [k], lifted towards white by [lift], per channel `round(x + (255 − x) · lift)` —
 * the boil's fill (`mix(fill, amber, min(1, 1.15 · heat), lift)`).
 */
internal fun mixColor(
    a: Color,
    b: Color,
    k: Double,
    lift: Double,
): Color {
    fun c(
        x0: Float,
        x1: Float,
    ): Int {
        val x = ch(x0) + (ch(x1) - ch(x0)) * k
        return jsRound(x + (255 - x) * lift).toInt()
    }
    return Color(c(a.red, b.red), c(a.green, b.green), c(a.blue, b.blue))
}

/** The fill of a pressed body at frame time [t] and [heat]: towards amber, flickering lighter every 333 ms past half heat. */
internal fun boilFill(
    fill: Color,
    colors: MutantColors,
    heat: Double,
    t: Double,
): Color =
    if (heat <= 0) {
        fill
    } else {
        mixColor(
            fill,
            colors.amber,
            min(1.0, heat * 1.15),
            if (heat > .5 &&
                floor(t / 333).toLong() % 2 == 1L
            ) {
                .2
            } else {
                0.0
            },
        )
    }

/**
 * The edge mosaic of a pressed body at [t]: every tile recoloured, faded or shifted along its side per hot tick
 * (`drawItem`, the `data-px` tiles), from the stream `px<round(t / 83.33)>`. Shifted tiles keep their order and their
 * row.
 *
 * B-199, a departure from the engine (D20, the designer's answer 25): a tile whose shift makes it meet the clean zone is
 * not drawn, as a resting block that would meet it is never placed (`edgeMos`). The engine shifts it into the zone,
 * over the label; [asDesigned] draws it so. Every tile still takes its four draws, so the others stand as the engine's.
 */
internal fun runningMosaic(
    g: Instance,
    t: Double,
    heat: Double,
    asDesigned: Boolean = false,
): List<Pair<MosaicTile, Color>> {
    val q = Mulberry32(g.seed, "px" + jsRound(t / 83.33).toLong())
    val top = g.mosaicSide == MosaicSide.Top
    val el = g.slot
    val out = ArrayList<Pair<MosaicTile, Color>>(g.mosaicTiles.size)
    for (p in g.mosaicTiles) {
        val c = q.next()
        val v = q.next()
        val d = q.next()
        val n = q.next()
        val bs = p.size
        val sh = if (d < .5 * heat) (if (n < .5) -1.0 else 1.0) * bs * (if (n < .25 || n > .75) 2.0 else 1.0) else 0.0
        val x = p.x + (if (top) sh else 0.0)
        val y = p.y + (if (top) 0.0 else sh)
        if (!asDesigned && el.blockMeetsCleanZone(x - el.ox, y - el.oy, bs)) continue
        val color =
            when {
                c < .38 -> Color.Black

                c < .78 -> Color.White

                c < .9 -> Color.Unspecified

                // magenta, resolved by the caller's palette
                else -> Color.Transparent // cyan, resolved by the caller's palette
            }
        val opacity = if (v < .22 * heat) 0.0 else jsToFixed(.3 + .5 * v, 2)
        out += MosaicTile(x, y, p.size, black = false, opacity = opacity, row = p.row) to color
    }
    return out
}

/** The two seek bands of a loading body at [t] (900 ms cycle): their top edges, 6 and 3 dp tall, white at 0.5 and 0.35. */
internal fun loadingBands(
    g: Instance,
    t: Double,
): List<Rect> = loadingBands(g.slot, t)

/** [loadingBands] from the slot alone, which is all they read (B-177's stand-in). */
internal fun loadingBands(
    el: Slot,
    t: Double,
): List<Rect> {
    val ph = (t % 900) / 900
    return List(2) { i ->
        val y = jsToFixed(el.oy + el.h + 4 - ((ph + i * .45) % 1) * (el.h + 12), 1).toFloat()
        Rect((el.ox - 10).toFloat(), y, (el.ox + el.w + 10).toFloat(), y + if (i == 0) 6f else 3f)
    }
}

/** What a body draws on top of rest in a state: the error's offset outline and blocks, the bands, the brackets. */
internal class BodyDecor(
    val errorOutline: Color? = null,
    val errorBlocks: List<Pair<Rect, Color>> = emptyList(),
    val bands: List<Rect> = emptyList(),
    val brackets: Color? = null,
    val runningMosaic: List<Pair<MosaicTile, Color>>? = null,
)

internal fun MutantGenerator.decorOf(
    g: Instance,
    state: BodyState,
    focused: Boolean,
    heat: Double,
    t: Double,
    colors: MutantColors,
): BodyDecor =
    BodyDecor(
        errorOutline = if (state == BodyState.Error) colors.danger else null,
        errorBlocks =
            if (state == BodyState.Error) {
                errorBlocks(g).map {
                    Rect(it.x.toFloat(), it.y.toFloat(), (it.x + it.size).toFloat(), (it.y + it.size).toFloat()) to
                        when (it.color) {
                            ErrorBlockColor.Danger -> colors.danger
                            ErrorBlockColor.Ink -> colors.ink
                            ErrorBlockColor.Background -> colors.background
                        }
                }
            } else {
                emptyList()
            },
        bands = if (state == BodyState.Loading) loadingBands(g, t) else emptyList(),
        brackets = if (focused) colors.focus else null,
        runningMosaic =
            if (heat > 0) {
                runningMosaic(g, t, heat).map { (tile, c) ->
                    tile to
                        when (c) {
                            Color.Unspecified -> colors.magenta
                            Color.Transparent -> colors.cyan
                            else -> c
                        }
                }
            } else {
                null
            },
    )

/** The focus brackets: four corner strokes 7 dp outside the slot, 10 dp long, 2 dp wide. */
internal fun DrawScope.drawBrackets(
    g: Instance,
    color: Color,
) = drawBrackets(g.slot, color)

/** [drawBrackets] around a slot (B-177's stand-in). */
internal fun DrawScope.drawBrackets(
    el: Slot,
    color: Color,
) {
    drawCornerBrackets(
        (el.ox - 7).toFloat(),
        (el.oy - 7).toFloat(),
        (el.ox + el.w + 7).toFloat(),
        (el.oy + el.h + 7).toFloat(),
        color,
    )
}

/** Four corner strokes on the box from ([x0], [y0]) to ([x1], [y1]), dp: 10 dp long, 2 dp wide. */
internal fun DrawScope.drawCornerBrackets(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    color: Color,
) {
    val q = 10f
    val p =
        Path().apply {
            moveTo(x0, y0 + q)
            lineTo(x0, y0)
            lineTo(x0 + q, y0)
            moveTo(x1 - q, y0)
            lineTo(x1, y0)
            lineTo(x1, y0 + q)
            moveTo(x1, y1 - q)
            lineTo(x1, y1)
            lineTo(x1 - q, y1)
            moveTo(x0 + q, y1)
            lineTo(x0, y1)
            lineTo(x0, y1 - q)
        }
    drawPath(p, color, style = Stroke(width = 2f))
}

/** The stage's patterns a still body draws with: the snow (32 dp) and the scanlines, per theme. */
internal class StagePatterns(
    /** `x, y, black, opacity` per snow point. */
    val snow: List<DoubleArray>,
) {
    companion object {
        fun of(
            cassette: String,
            stageKey: String,
        ): StagePatterns {
            val r = Mulberry32("$cassette/$stageKey", "speck")
            repeat(32) { r.next() } // the speckle's eight squares come first
            val snow =
                List(60) {
                    val c = r.next()
                    val x = floor(r.next() * 32)
                    val y = floor(r.next() * 32)
                    doubleArrayOf(x, y, if (c < .4) 1.0 else 0.0, jsToFixed(.2 + .4 * c, 2))
                }
            return StagePatterns(snow)
        }
    }
}

/** `desat`: a disabled body's fill — its luminance times [dark], 85 % of it and 15 % of the theme's tint. */
internal fun desaturate(
    fill: Color,
    dark: Double,
    tint: Color,
): Color {
    val l = (.3 * ch(fill.red) + .59 * ch(fill.green) + .11 * ch(fill.blue)) * dark

    fun m(t: Float) = jsRound(l * .85 + ch(t) * .15).toInt().coerceIn(0, 255)
    return Color(m(tint.red), m(tint.green), m(tint.blue))
}

private fun saturation(c: Color): Int =
    maxOf(ch(c.red), ch(c.green), ch(c.blue)) - minOf(ch(c.red), ch(c.green), ch(c.blue))

/**
 * A disabled body, «нет сигнала» (`staticHtml`): no slime — the slot's rounded rectangle in a desaturated fill,
 * scanlines, still snow, white dropout strokes outside the clean zone, and one band of 14 % of the height
 * shifted right by 4–7 dp. Patterns are anchored at [patternOrigin].
 */
internal fun DrawScope.drawDisabledBody(
    g: Instance,
    fill: Color,
    highlight: Color,
    colors: MutantColors,
    outline: Float,
    patterns: StagePatterns,
    patternOrigin: Offset,
    tearTop: Double? = null,
) = drawDisabledBody(g.slot, g.seed, g.mosaicTiles, fill, highlight, colors, outline, patterns, patternOrigin, tearTop)

/**
 * The fill a disabled body shows for its [fill] (`staticHtml`): [desaturate]d, darkened where the fill is a saturated one
 * (its [highlight] white on the screen, a saturation over 90 in print).
 */
internal fun disabledFill(
    fill: Color,
    highlight: Color,
    colors: MutantColors,
): Color {
    val dark =
        if (if (colors.print) {
                saturation(
                    fill,
                ) > 90
            } else {
                highlight == Color.White
            }
        ) {
            colors.disabledDarken.toDouble()
        } else {
            1.0
        }
    return desaturate(fill, dark, colors.disabledTint)
}

/** The height of a disabled body's shifted band on a slot [h] dp tall: 14 % of it, at least 4 dp. */
internal fun disabledBandHeight(h: Double): Double = max(4.0, jsRound(h * .14))

/** How far right a disabled body's band is shifted, 4–7 dp: the stream `vhs`'s second number, after the band's top. */
private fun disabledShift(rnd: Mulberry32): Float = jsToFixed(4 + 3 * rnd.next(), 1).toFloat()

/** [disabledShift] for the body of [seed] (`<cassette>/<key>`), as its «нет сигнала» draws it. */
internal fun disabledShiftOf(seed: String): Float =
    Mulberry32(seed, "vhs").let {
        it.next()
        disabledShift(it)
    }

/**
 * B-181: a tear band over a body losing its signal, [height] dp tall from [top], the slot's sides 20 dp past: in it,
 * [body] is drawn again shifted [shift] dp right under the dropout's veil, as the disabled look's own band is.
 */
internal inline fun DrawScope.drawTearBand(
    el: Slot,
    top: Double,
    height: Double,
    shift: Float,
    colors: MutantColors,
    crossinline body: DrawScope.() -> Unit,
) {
    clipRect((el.ox - 20).toFloat(), top.toFloat(), (el.ox + el.w + 20).toFloat(), (top + height).toFloat()) {
        translate(shift, 0f) {
            body()
            drawRect(
                colors.dropout,
                Offset(el.ox.toFloat(), top.toFloat()),
                Size(el.w.toFloat(), height.toFloat()),
                alpha = .22f,
            )
        }
    }
}

/**
 * [drawDisabledBody] from what it reads of the instance: its slot, its seed (the dropouts and the shifted band) and its
 * edge mosaic's [tiles]. B-177's stand-in draws it with no tiles, the one part that needs the instance generated.
 * [tearTop] draws a second band there, B-181's tear on the third step of losing the signal.
 */
internal fun DrawScope.drawDisabledBody(
    el: Slot,
    seed: String,
    tiles: List<MosaicTile>,
    fill: Color,
    highlight: Color,
    colors: MutantColors,
    outline: Float,
    patterns: StagePatterns,
    patternOrigin: Offset,
    tearTop: Double? = null,
) {
    val ox = el.ox
    val oy = el.oy
    val w = el.w
    val h = el.h
    val cz = el.cleanZone
    val r = el.r.toFloat()
    val rnd = Mulberry32(seed, "vhs")
    val body = disabledFill(fill, highlight, colors)
    val hh = disabledBandHeight(h)
    val by =
        oy +
            if (cz[2] > 0) {
                jsRound(min(h - hh, cz[1] + cz[3] + rnd.next() * max(0.0, h - cz[1] - cz[3] - hh)))
            } else {
                jsRound(rnd.next() * (h - hh))
            }
    val dx = disabledShift(rnd)
    val inCz = { x: Double, y: Double, l: Double ->
        cz[2] > 0 && x < cz[0] + cz[2] && x + l > cz[0] && y < cz[1] + cz[3] &&
            y + 1 > cz[1]
    }
    val lines = ArrayList<DoubleArray>()
    for (i in 0 until max(2.0, jsRound(w / 55)).toInt()) {
        val x = rnd.next() * w
        val y = jsRound(rnd.next() * h) + .5
        val l = min(w * .5, 8 + 32 * rnd.next())
        if (!inCz(x, y, l)) lines += doubleArrayOf(jsToFixed(ox + x, 1), jsToFixed(ox + x + l, 1), oy + y)
    }
    val grain = ArrayList<DoubleArray>()
    var y = h - min(5.0, h * .2)
    while (y < h) {
        for (i in 0 until max(1.0, jsRound(w / 65)).toInt()) {
            val gx = jsToFixed(ox + rnd.next() * w, 1)
            val gw = jsToFixed(3 + 18 * rnd.next(), 1)
            val ga = jsToFixed(.3 + .4 * rnd.next(), 2)
            grain += doubleArrayOf(gx, jsToFixed(oy + y, 1), gw, ga)
        }
        y += 1
    }
    val rect = RoundRect(ox.toFloat(), oy.toFloat(), (ox + w).toFloat(), (oy + h).toFloat(), CornerRadius(r))
    val bodyPath = Path().apply { addRoundRect(rect) }
    val box = Rect(ox.toFloat(), oy.toFloat(), (ox + w).toFloat(), (oy + h).toFloat())

    fun scanlines() {
        clipRect(box.left, box.top, box.right, box.bottom) {
            if (colors.print) {
                var yy = floor((box.top - patternOrigin.y) / 3) * 3 + patternOrigin.y
                while (yy < box.bottom) {
                    var xx = floor((box.left - patternOrigin.x) / 3) * 3 + patternOrigin.x
                    while (xx < box.right) {
                        drawCircle(colors.ink, .75f, Offset(xx + 1.5f, yy + 1.5f), alpha = .4f)
                        xx += 3
                    }
                    yy += 3
                }
            } else {
                var yy = floor((box.top - patternOrigin.y) / 2) * 2 + patternOrigin.y
                while (yy < box.bottom) {
                    drawRect(colors.ink, Offset(box.left, yy + 1), Size(box.width, 1f), alpha = .42f)
                    yy += 2
                }
            }
        }
    }
    drawPath(bodyPath, body)
    clipPath(bodyPath) {
        for (t in tiles) {
            drawRect(
                if (t.black) Color.Black else Color.White,
                Offset(t.x.toFloat(), t.y.toFloat()),
                Size(t.size.toFloat(), t.size.toFloat()),
                alpha = t.opacity.toFloat(),
            )
        }
        scanlines()
        clipRect(box.left, box.top, box.right, box.bottom) {
            val j0 = floor((box.top - patternOrigin.y) / 32).toInt()
            val j1 = floor((box.bottom - patternOrigin.y) / 32).toInt()
            val i0 = floor((box.left - patternOrigin.x) / 32).toInt()
            val i1 = floor((box.right - patternOrigin.x) / 32).toInt()
            for (j in j0..j1) {
                for (i in i0..i1) {
                    for (s in patterns.snow) {
                        drawRect(
                            if (s[2] == 1.0) Color.Black else colors.dropout,
                            Offset(
                                patternOrigin.x + i * 32 + s[0].toFloat(),
                                patternOrigin.y + j * 32 + s[1].toFloat(),
                            ),
                            Size(1f, 1f),
                            alpha = s[3].toFloat(),
                        )
                    }
                }
            }
        }
        for (l in lines) {
            drawLine(
                colors.dropout,
                Offset(l[0].toFloat(), l[2].toFloat()),
                Offset(l[1].toFloat(), l[2].toFloat()),
                strokeWidth = 1f,
                alpha = .8f,
            )
        }
        for (gr in grain) {
            drawRect(
                colors.dropout,
                Offset(gr[0].toFloat(), gr[1].toFloat()),
                Size(gr[2].toFloat(), 1f),
                alpha = gr[3].toFloat(),
            )
        }
    }
    drawPath(bodyPath, colors.ink, style = Stroke(width = outline))
    for (top in listOfNotNull(by, tearTop)) {
        clipRect((ox - 20).toFloat(), top.toFloat(), (ox + w + 20).toFloat(), (top + hh).toFloat()) {
            translate(dx, 0f) {
                drawPath(bodyPath, body)
                scanlines()
                drawRect(
                    colors.dropout,
                    Offset(ox.toFloat(), top.toFloat()),
                    Size(w.toFloat(), hh.toFloat()),
                    alpha = .22f,
                )
                drawPath(bodyPath, colors.ink, style = Stroke(width = outline))
            }
        }
    }
}

/** A successful body, «чистый кадр»: the slot's plain rounded rectangle and one highlight in its top-left corner. */
internal fun DrawScope.drawCleanBody(
    g: Instance,
    fill: Color,
    highlight: Color,
    ink: Color,
    outline: Float,
) = drawCleanBody(g.slot, fill, highlight, ink, outline)

/** [drawCleanBody] from the slot alone, which is all it reads (B-177's stand-in). */
internal fun DrawScope.drawCleanBody(
    el: Slot,
    fill: Color,
    highlight: Color,
    ink: Color,
    outline: Float,
) {
    val ox = el.ox.toFloat()
    val oy = el.oy.toFloat()
    val w = el.w.toFloat()
    val r = el.r.toFloat()
    val rect = RoundRect(ox, oy, ox + w, oy + el.h.toFloat(), CornerRadius(r))
    val body = Path().apply { addRoundRect(rect) }
    drawPath(body, fill)
    drawPath(body, ink, style = Stroke(width = outline))
    val rr = max(1f, r - 5)
    val hl =
        Path().apply {
            moveTo(ox + 5, oy + r + 4)
            lineTo(ox + 5, oy + r)
            arcTo(Rect(ox + 5, oy + r - rr, ox + 5 + 2 * rr, oy + r + rr), 180f, 90f, false)
            lineTo(ox + r + min(18f, w * .2f), oy + 5)
        }
    drawPath(hl, highlight, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
}
