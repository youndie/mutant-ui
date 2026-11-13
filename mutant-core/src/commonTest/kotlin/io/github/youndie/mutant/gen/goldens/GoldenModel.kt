package io.github.youndie.mutant.gen.goldens

/**
 * One instance as `mutant-generator.js` generated it (B-03), written by `scripts/gen-goldens.mjs`.
 *
 * [slot] is `ox, oy, w, h, r, over, cf, wob, round, calm, dead, marks, edge`, then `tz` and `cz`.
 * [drips] are eight numbers each: `x, b, Lmax, L0, P, off, u, drip`. [flowLines] are fifteen: kind
 * (0 hook, 1 arc), `x, y, L, dx, a, hk, dir, fl, sl, wd, dp, P, off, sd`. [mosaic] is `x, y, size` per
 * block; [mosaicTiles] `x, y, size, black, opacity` per drawn tile; [mosaicSide] 0 top, 1 left,
 * 2 right, −1 none. [deadPixels] are `x, y, size, colour role` per pixel, [deadLine] `x1, x2, y`, the
 * x's as the JS prints them, to one decimal.
 */
internal class GoldenInstance(
    val name: String,
    val key: String,
    val still: Boolean,
    val slot: DoubleArray,
    val k: Double,
    val bottomRadius: Double,
    val wave: DoubleArray,
    val shape: DoubleArray,
    val drips: DoubleArray,
    val flowLines: DoubleArray,
    val mosaic: DoubleArray,
    val mosaicTiles: DoubleArray,
    val mosaicSide: Int,
    val deadZone: DoubleArray,
    val deadPixels: DoubleArray,
    val deadLine: DoubleArray,
    val frames: List<GoldenFrame>,
)

/** One frame as the engine's `drawItem` draws it: [primitives] seven numbers each, [grid] `nx, ny, x0, y0, s`. */
internal class GoldenFrame(
    val t: Double,
    val heat: Double,
    val boilPhase: Int,
    val boil: Double,
    val primitives: DoubleArray,
    val grid: DoubleArray,
    val contour: String,
    val highlight: String,
    val flowLines: String,
)
