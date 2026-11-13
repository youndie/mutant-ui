package io.github.youndie.mutant.gen

/**
 * `el`: one slot as the host measured it, in dp, with its type profile already applied (the engine's
 * `makeItem`). [ox], [oy] place it in the scene; [textZone] (`tz`) is the clean zone relative to the
 * slot, `[x, y, w, h]`, and [cleanZone] (`cz`) the same with 2 dp to spare; `[-99, -99, 0, 0]` and
 * `[0, 0, 0, 0]` when the slot has no content.
 */
internal class Slot(
    val ox: Double,
    val oy: Double,
    val w: Double,
    val h: Double,
    /** `r`: the corner radius, already at most half of either side. */
    val r: Double,
    /** `over`: how far drips may hang below the slot. */
    val over: Double,
    /** `cf`: the share of drips that let a drop go. */
    val dripShare: Double,
    /** `wob`: the strength of the melt of the outline. */
    val wobble: Double,
    /** `round`: a full circle; its corner radii are not randomised. */
    val round: Boolean,
    /** `calm`: wear, 0.2…1.7; 1 is a resting copy. */
    val calm: Double,
    /** `dead`: dead pixels in a corner (the card). */
    val dead: Boolean,
    /** `marks`: flow lines inside the body. */
    val marks: Boolean,
    /** `edge`: the edge mosaic. */
    val edge: Boolean,
    val textZone: DoubleArray,
    val cleanZone: DoubleArray,
)

/** One drip (`D[]`): its centre, half-width, lengths, period, phase and whether it lets a drop go. */
internal class Drip(
    val x: Double,
    val halfWidth: Double,
    val maxLength: Double,
    val restLength: Double,
    val period: Double,
    val offset: Double,
    val u: Double,
    val drips: Boolean,
)

/** The two shapes a flow line takes: a hook (`'j'`) or an arc (`'a'`). */
internal enum class FlowLineKind { Hook, Arc }

/** One flow line (`ML[]`): its seed geometry and, once placed, its cycle. */
internal class FlowLine(
    val kind: FlowLineKind,
    val x: Double,
    val y: Double,
    val length: Double = 0.0,
    val dx: Double = 0.0,
    val a: Double = 0.0,
    val hook: Double = 0.0,
    val dir: Double = 0.0,
    val flick: Double = 0.0,
    val slide: Double,
    val width: Double = 0.0,
    val depth: Double = 0.0,
) {
    /** `P`: the cycle, ms. */
    var period: Double = 0.0

    /** `off`: the phase, 0…1. */
    var offset: Double = 0.0

    /** `sd`: the line's noise seed. */
    var noiseSeed: Double = 0.0
}

/** A square block `[x, y, size]` relative to the slot: under one, the outline is quantised (`mz`). */
internal class Block(
    val x: Double,
    val y: Double,
    val size: Double,
)

/** One drawn tile of the edge mosaic: black or white at an opacity. */
internal class MosaicTile(
    val x: Double,
    val y: Double,
    val size: Double,
    val black: Boolean,
    val opacity: Double,
)

/** The colours a dead pixel takes, by the theme's role (`deadPx`). */
internal enum class DeadPixelColor { Green, Magenta, Cyan, White, Cocoon1, Surface2, Cocoon2, Ink }

/** One dead pixel, in scene coordinates. */
internal class DeadPixel(
    val x: Double,
    val y: Double,
    val size: Double,
    val color: DeadPixelColor,
)

/** The dead-pixel corner: its blocks and the white scan line, in scene coordinates. */
internal class DeadPixels(
    val pixels: List<DeadPixel>,
    val lineX1: Double,
    val lineX2: Double,
    val lineY: Double,
)

/** Which side the edge mosaic runs along (`ms`). */
internal enum class MosaicSide { Top, Left, Right }

/**
 * `g`: one instance, generated once per slot and key, independent of time and state.
 * [shape] is `sh`: bulge left and right, top tilt, noise melt, top hump, hump count, hump phase, noise
 * shift, then the corner radii top-left, top-right, bottom-right, bottom-left.
 */
internal class Instance(
    val slot: Slot,
    /** `k`: the scale from size, 0.5…1. */
    val k: Double,
    /** `rb`: the bottom radius. */
    val bottomRadius: Double,
    /** `wave`: amplitude, wavelength, phase 1, phase 2, angular speed of the top edge. */
    val wave: DoubleArray,
    val shape: DoubleArray,
    val drips: List<Drip>,
    /** `sd`: the instance seed, `"$cassette/$key"`. */
    val seed: String,
) {
    var flowLines: List<FlowLine> = emptyList()
    var mosaic: List<Block> = emptyList()
    var mosaicTiles: List<MosaicTile> = emptyList()
    var mosaicSide: MosaicSide? = null

    /** `dz`: the dead-pixel zone `[cx, cy, r]`. */
    var deadZone: DoubleArray? = null
}

/** `[ax, ay, bx, by, ra, rb, k]`: an unequal capsule and its blend radius into the body. */
internal typealias Primitive = DoubleArray
