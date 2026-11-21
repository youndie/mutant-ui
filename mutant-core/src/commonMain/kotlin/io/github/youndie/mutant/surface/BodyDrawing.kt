package io.github.youndie.mutant.surface

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import io.github.youndie.mutant.gen.BodyFrame
import io.github.youndie.mutant.gen.DeadPixelColor
import io.github.youndie.mutant.gen.DeadPixels
import io.github.youndie.mutant.gen.GenPath
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.Mulberry32
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.gen.valueNoise
import io.github.youndie.mutant.tokens.MutantColors
import kotlin.math.floor
import kotlin.math.min

/**
 * How one body is painted, besides its shape: what the engine's `makeItem` puts on the slot (`fill`, `hl`,
 * `stroke`, `slip`, `shadow`) and the outline's width on this frame.
 */
internal class BodyLook(
    val fill: Color,
    val highlight: Color,
    val ink: Color,
    val outline: Float,
    val slip: Float = 0f,
    val shadow: Color? = null,
    /** The profile's outline before the tremble: the error's offset outline is drawn with it. */
    val baseOutline: Float = outline,
)

/** The engine's `hl`: white over a light fill, the theme's highlight over a dark one (`lum(fill) > .45`). */
internal fun highlightFor(
    fill: Color,
    colors: MutantColors,
): Color {
    val lum = (.3 * channel(fill.red) + .59 * channel(fill.green) + .11 * channel(fill.blue)) / 255
    return if (lum > .45) Color.White else colors.highlightOnDark
}

private fun channel(v: Float): Int = (v * 255 + .5f).toInt()

/**
 * The outline's width on a frame: `ow · (1 + 0.12 · boil · vn(phase · 3.3 + s · 7.1, 2.5))` to two decimals,
 * as `drawItem` sets it. The engine's s is its running count of slots on the page, a host artefact; the
 * kit's is the instance's own, from its seed, so a body trembles the same wherever it stands (research §1.8).
 */
internal fun outlineWidth(
    g: Instance,
    outline: Double,
    boilPhase: Int,
    boil: Double,
): Float =
    jsToFixed(outline * (1 + .12 * boil * valueNoise(boilPhase * 3.3 + trembleSeedOf(g) * 7.1, 2.5)), 2).toFloat()

/** The instance's tremble seed: its seed's hash, folded to 0…1023. */
internal fun trembleSeedOf(g: Instance): Double =
    (
        io.github.youndie.mutant.gen
            .fnv1a(g.seed) % 1024
    ).toDouble()

/** A [GenPath] as a Compose path, even-odd as the engine's `fill-rule`/`clip-rule`. */
internal fun GenPath.toPath(evenOdd: Boolean = true): Path =
    Path().apply {
        fillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
        for (loop in loops) {
            moveTo(loop[0] / 10f, loop[1] / 10f)
            var i = 2
            while (i < loop.size) {
                quadraticTo(loop[i] / 10f, loop[i + 1] / 10f, loop[i + 2] / 10f, loop[i + 3] / 10f)
                i += 4
            }
            close()
        }
    }

/**
 * The stage's speckle (`buildStage`, pattern `sp`): eight ink squares in a 40 dp tile, seeded by the
 * cassette and the stage, at the group opacity `0.35 · min(1, 2 · rough)`. Positions and sizes are rounded as
 * the JS writes them.
 */
internal class Speckle(
    val rects: List<Rect>,
    val alpha: Float,
) {
    companion object {
        const val TILE: Float = 40f

        fun of(
            cassette: String,
            stageKey: String,
            rough: Double,
        ): Speckle {
            val r = Mulberry32("$cassette/$stageKey", "speck")
            val rects =
                List(8) {
                    val x = jsToFixed(r.next() * 40, 1).toFloat()
                    val y = jsToFixed(r.next() * 40, 1).toFloat()
                    val w = jsToFixed(.6 + r.next() * .8, 2).toFloat()
                    val h = jsToFixed(.6 + r.next() * .8, 2).toFloat()
                    Rect(x, y, x + w, y + h)
                }
            return Speckle(rects, jsToFixed(.35 * min(1.0, rough * 2), 2).toFloat())
        }
    }
}

/** The paths of one frame, built once per tick. */
internal class BodyPaths(
    val contour: Path,
    val highlight: Path,
    val flowLines: Path,
) {
    companion object {
        fun of(frame: BodyFrame): BodyPaths =
            BodyPaths(frame.contour.toPath(), frame.highlight.toPath(), frame.flowLines.toPath(evenOdd = false))
    }
}

/**
 * Draws one body as the engine's `itemHtml` layers it, bottom to top: the misregistered shadow, the group
 * clipped by the outline and shifted by `slip` (fill, the stage's speckle, the edge mosaic, dead pixels, the
 * flow lines), the highlight inside its two clip rectangles (under the bottom edge, in the top-left corner),
 * the outline. Coordinates are the generator's scene coordinates; [speckleOrigin] is where the speckle's tile
 * grid starts, in the same coordinates.
 */
internal fun DrawScope.drawBody(
    g: Instance,
    paths: BodyPaths,
    look: BodyLook,
    colors: MutantColors,
    speckle: Speckle,
    speckleOrigin: Offset,
    deadPixels: DeadPixels? = null,
    decor: BodyDecor = BodyDecor(),
    pour: Pair<Path, Color>? = null,
    skip: Set<BodyPart> = emptySet(),
    still: StillImage? = null,
    pastContour: Boolean = false,
) {
    val el = g.slot
    val ox = el.ox.toFloat()
    val oy = el.oy.toFloat()
    val w = el.w.toFloat()
    val h = el.h.toFloat()
    val over = el.over.toFloat()
    val bb = Rect(ox - 12, oy - 15, ox + w + 12, oy + h + over + 30)
    if (BodyPart.Shadow !in skip) look.shadow?.let { translate(4f, 4f) { drawPath(paths.contour, it) } }
    decor.errorOutline?.let {
        translate(2.5f, 0f) { drawPath(paths.contour, it, style = Stroke(width = look.baseOutline)) }
    }
    translate(look.slip, look.slip) {
        clipPathUnless(BodyPart.ContourClip in skip, paths.contour) {
            if (BodyPart.Fill !in skip) drawRect(look.fill, bb.topLeft, bb.size)
            pour?.let { (path, from) -> drawPath(path, from) }
            if (still != null && decor.runningMosaic == null) {
                // B-117: the still layers, drawn once into an image on this canvas's own pixel grid, are one draw under
                // the clip instead of hundreds. Undo the slip and the scale, so the image's pixels land on the canvas's.
                translate(still.gridX - look.slip, still.gridY - look.slip) {
                    scale(
                        1 / density,
                        Offset.Zero,
                    ) { drawImage(still.image, Offset(still.at.x.toFloat(), still.at.y.toFloat())) }
                }
            } else {
                drawStillLayers(g, colors, speckle, speckleOrigin, deadPixels, decor, skip)
            }
            for ((r, c) in decor.errorBlocks) drawRect(c, r.topLeft, r.size)
            if (BodyPart.FlowLines !in skip) drawPath(paths.flowLines, colors.ink)
            for ((i, b) in decor.bands.withIndex()) {
                drawRect(
                    Color.White,
                    b.topLeft,
                    b.size,
                    alpha =
                        if (i ==
                            0
                        ) {
                            .5f
                        } else {
                            .35f
                        },
                )
            }
        }
    }
    val r = el.r.toFloat()
    val highlightClip =
        Path().apply {
            addRect(Rect(ox - 6, oy + h + .5f, ox + w + 6, oy + h + .5f + over + 30))
            addRect(Rect(ox - 4, oy - 6, ox - 4 + r + 13, oy - 6 + r + 10))
        }
    if (pour == null && BodyPart.Highlight !in skip) {
        clipPath(highlightClip) {
            drawPath(paths.highlight, look.highlight)
            translate(1.5f, 1.5f) { drawPath(paths.highlight, look.fill) }
        }
    }
    if (BodyPart.Outline !in skip) {
        drawPath(paths.contour, look.ink, style = Stroke(width = look.outline, join = StrokeJoin.Round))
    }
    if (pastContour &&
        BodyPart.Mosaic !in skip
    ) {
        drawMosaicPastContour(g, paths.contour, look, colors.page, decor)
    }
    decor.brackets?.let { drawBrackets(g, it) }
}

/**
 * The layers under a body's clip that do not move with the tick, in the order [drawBody] draws them: the stage's speckle,
 * the edge mosaic, the dead pixels. Coordinates are the scene's, inside the slip.
 */
internal fun DrawScope.drawStillLayers(
    g: Instance,
    colors: MutantColors,
    speckle: Speckle,
    speckleOrigin: Offset,
    deadPixels: DeadPixels?,
    decor: BodyDecor,
    skip: Set<BodyPart>,
) {
    val el = g.slot
    val ox = el.ox.toFloat()
    val oy = el.oy.toFloat()
    val bb = Rect(ox - 12, oy - 15, ox + el.w.toFloat() + 12, oy + el.h.toFloat() + el.over.toFloat() + 30)
    // Speckle and flow lines are the theme's ink whatever the outline's colour (the engine's `C.ink`).
    if (BodyPart.Speckle !in skip) drawSpeckle(speckle, speckleOrigin, bb, colors.ink)
    val tiles =
        if (BodyPart.Mosaic in skip) {
            emptyList()
        } else {
            decor.runningMosaic ?: g.mosaicTiles.map { it to (if (it.black) Color.Black else Color.White) }
        }
    for ((t, c) in tiles) {
        drawRect(
            c,
            Offset(t.x.toFloat(), t.y.toFloat()),
            Size(t.size.toFloat(), t.size.toFloat()),
            alpha = t.opacity.toFloat(),
        )
    }
    if (deadPixels != null && BodyPart.DeadPixels !in skip) drawDeadPixels(deadPixels, colors)
}

/**
 * B-117: a body's still layers drawn once, on the pixel grid of a canvas that draws the scene at [density] with the
 * scene's point ([gridX], [gridY]) at its pixel (0, 0); the image stands at [at] on that grid.
 */
internal class StillImage(
    val image: ImageBitmap,
    val gridX: Float,
    val gridY: Float,
    val at: androidx.compose.ui.unit.IntOffset,
)

/**
 * B-117: a body's still layers ([drawStillLayers]) drawn once, into an image covering what [rasterisedBody]'s image
 * covers, on the grid of a canvas whose pixel (0, 0) is the scene's point ([gridX], [gridY]): the body's tick image
 * (its raster origin) or the surface a frame draws on (the slot's origin). The mosaic of a hot body moves and is not in it.
 */
internal fun stillLayersImage(
    g: Instance,
    slip: Float,
    colors: MutantColors,
    speckle: Speckle,
    speckleOrigin: Offset,
    deadPixels: DeadPixels?,
    skip: Set<BodyPart>,
    density: Float,
    direction: androidx.compose.ui.unit.LayoutDirection,
    gridX: Float,
    gridY: Float,
): StillImage {
    val box = rasterBox(g)
    val x0 = floor((box.left - gridX) * density).toInt()
    val y0 = floor((box.top - gridY) * density).toInt()
    val x1 = kotlin.math.ceil((box.right - gridX) * density).toInt()
    val y1 = kotlin.math.ceil((box.bottom - gridY) * density).toInt()
    val image = ImageBitmap(maxOf(1, x1 - x0), maxOf(1, y1 - y0))
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit
            .Density(density),
        direction,
        androidx.compose.ui.graphics
            .Canvas(image),
        Size(image.width.toFloat(), image.height.toFloat()),
    ) {
        translate(-x0.toFloat(), -y0.toFloat()) {
            scale(density, Offset.Zero) {
                translate(slip - gridX, slip - gridY) {
                    drawStillLayers(g, colors, speckle, speckleOrigin, deadPixels, BodyDecor(), skip)
                }
            }
        }
    }
    // B-119: finished, so a frame drawing it shares its pixels instead of copying them (B-113).
    image.freeze()
    return StillImage(
        image,
        gridX,
        gridY,
        androidx.compose.ui.unit
            .IntOffset(x0, y0),
    )
}

/**
 * B-137, a departure from the design (D20): the edge mosaic past the contour. The edge row (row 0) and the blocks
 * outside the side ([Instance.spillTiles]) are drawn over the outline, unclipped, so the compression's blocks cross the
 * ink line and stand outside the body. Each block takes the colour of what it stands on, under its tile's tint: the
 * body's fill inside the contour, the [ground] outside it, so where a block crosses the outline the ink is gone. The
 * ground is the theme's page: a body does not know what lies behind it. The rows further in stay inside the clip, as
 * the design draws them. A pressed body's edge row runs as [BodyDecor.runningMosaic] does; the blocks outside keep
 * their rest.
 */
private fun DrawScope.drawMosaicPastContour(
    g: Instance,
    contour: Path,
    look: BodyLook,
    ground: Color,
    decor: BodyDecor,
) {
    val running = decor.runningMosaic
    val edge =
        running?.let { g.mosaicTiles.zip(it) { d, r -> if (d.row == 0) r else null } }
            ?: g.mosaicTiles.map { if (it.row == 0) it to tint(it) else null }
    val tiles = edge.filterNotNull() + g.spillTiles.map { it to tint(it) }

    fun squares(color: Color?) =
        translate(look.slip, look.slip) {
            for ((t, c) in tiles) {
                val at = Offset(t.x.toFloat(), t.y.toFloat())
                val size = Size(t.size.toFloat(), t.size.toFloat())
                if (color == null) drawRect(c, at, size, alpha = t.opacity.toFloat()) else drawRect(color, at, size)
            }
        }
    clipPath(contour) { squares(look.fill) }
    clipPath(contour, ClipOp.Difference) { squares(ground) }
    squares(null)
}

private fun tint(t: io.github.youndie.mutant.gen.MosaicTile): Color = if (t.black) Color.Black else Color.White

/** [block] clipped by [path], or unclipped when [unclipped] (B-78's ablation of the clip alone). */
private inline fun DrawScope.clipPathUnless(
    unclipped: Boolean,
    path: Path,
    block: DrawScope.() -> Unit,
) {
    if (unclipped) block() else clipPath(path) { block() }
}

private fun DrawScope.drawSpeckle(
    speckle: Speckle,
    origin: Offset,
    area: Rect,
    ink: Color,
) {
    val t = Speckle.TILE
    val i0 = floor((area.left - origin.x) / t).toInt()
    val i1 = floor((area.right - origin.x) / t).toInt()
    val j0 = floor((area.top - origin.y) / t).toInt()
    val j1 = floor((area.bottom - origin.y) / t).toInt()
    for (j in j0..j1) {
        for (i in i0..i1) {
            val dx = origin.x + i * t
            val dy = origin.y + j * t
            // An SVG pattern clips its content to the tile: a square that starts near the tile's edge is cut there,
            // not carried into the next tile.
            for (r in speckle.rects) {
                val c = r.intersect(Rect(0f, 0f, t, t))
                if (c.width > 0 &&
                    c.height > 0
                ) {
                    drawRect(ink, Offset(dx + c.left, dy + c.top), c.size, alpha = speckle.alpha)
                }
            }
        }
    }
}

private fun DrawScope.drawDeadPixels(
    d: DeadPixels,
    colors: MutantColors,
) {
    for (p in d.pixels) {
        val c =
            when (p.color) {
                DeadPixelColor.Green -> colors.primary
                DeadPixelColor.Magenta -> colors.magenta
                DeadPixelColor.Cyan -> colors.cyan
                DeadPixelColor.White -> Color.White
                DeadPixelColor.Cocoon1 -> colors.cocoon1
                DeadPixelColor.Surface2 -> colors.surfaceVariant
                DeadPixelColor.Cocoon2 -> colors.cocoon2
                DeadPixelColor.Ink -> colors.ink
            }
        drawRect(c, Offset(p.x.toFloat(), p.y.toFloat()), Size(p.size.toFloat(), p.size.toFloat()))
    }
    // The JS prints the line's x's with toFixed(1).
    val x1 = jsToFixed(d.lineX1, 1).toFloat()
    val x2 = jsToFixed(d.lineX2, 1).toFloat()
    drawLine(Color.White, Offset(x1, d.lineY.toFloat()), Offset(x2, d.lineY.toFloat()), strokeWidth = 1f, alpha = .85f)
}
