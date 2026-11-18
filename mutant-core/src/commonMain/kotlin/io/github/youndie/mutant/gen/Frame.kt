package io.github.youndie.mutant.gen

import kotlin.math.ceil
import kotlin.math.floor

/** One drawn frame of a body: the outline, the highlight line and the flow lines, with what made them. */
internal class BodyFrame(
    val contour: GenPath,
    val highlight: GenPath,
    val flowLines: GenPath,
    val primitives: List<Primitive>,
    val field: DistanceField,
    val boilPhase: Int,
    val boil: Double,
    /** Whether the boil's tremble was damped in the webs (B-84). */
    val steadyWebs: Boolean = false,
)

/** The kit's resting boil and edge roughness (`props.boil`, `props.rough`; tokens `genome.boil`, `genome.noise`). */
internal const val REST_BOIL: Double = .8
internal const val REST_ROUGH: Double = .35

/** The resting tick and the pressed one is 1.5 times faster (tokens `motion.tick`, `motion.tick-hot`). */
internal const val TICK_MS: Double = 125.0

/**
 * The frame the engine's `drawItem` draws at time [t] with heat [heat]: the boil phase from the tick,
 * boil' = boil · (1 + 2·heat) + 0.6·heat, drops held and bubbles added while hot, the field, then the
 * outline at 0, the highlight at −3.4 dp and the flow lines with boil'. [pressed] keeps the hot tick and
 * the held drops with no heat yet, as the engine does between press and the first heat. [steadyWebs] damps the
 * boil's tremble where the field is flat, so the webs between drips do not flip with the boil phase (B-84): a running
 * body's departure from the engine; a still keeps the engine's field.
 */
internal fun MutantGenerator.frame(
    g: Instance,
    cache: FieldCache,
    t: Double,
    heat: Double = 0.0,
    pressed: Boolean = false,
    steadyWebs: Boolean = false,
    grow: Boolean = true,
    /** B-110: false leaves the outline and highlight empty, for a body a shader draws from the field; not the flow lines (B-131). */
    paths: Boolean = true,
): BodyFrame {
    val el = g.slot
    val hot = heat > 0 || pressed
    val boilPhase = boilPhase(t, hot)
    val boil0 = REST_BOIL * el.calm
    val boil = boil0 * (1 + 2 * heat) + .6 * heat
    val prims = primitives(g, t, hot)
    if (heat > 0) prims += bubbles(g, t, heat)
    val fd = distanceField(g, cache, prims, t, REST_ROUGH * el.calm, boilPhase, boil, heat, boil0, steadyWebs, grow)
    val flow = if (g.flowLines.isEmpty()) GenPath.Empty else flowLinePath(g, t, boilPhase, boil)
    if (!paths) return BodyFrame(GenPath.Empty, GenPath.Empty, flow, prims, fd, boilPhase, boil, steadyWebs)
    val contour = smoothPath(march(fd, 0.0), .9, 5.0)
    val highlight =
        smoothPath(
            march(fd, -3.4, ceil((el.r + 19) / fd.step).toInt() + fd.pad, floor((el.h + 7) / fd.step).toInt() + fd.pad),
            1.1,
        )
    return BodyFrame(contour, highlight, flow, prims, fd, boilPhase, boil, steadyWebs)
}

/**
 * The boil phase 0…3 a body trembles in at [ms] (the engine's `drawItem`): `floor(t / 125 + 0.001) mod 4`
 * at rest, every 83⅓ ms when [hot], whatever the resting tick's rate. The 0.001 makes a time a hair short
 * of a boundary count as past it, as the tick does not: at 83.333333 ms the hot phase is already 1 while
 * the hot tick is still 0.
 */
internal fun boilPhase(
    ms: Double,
    hot: Boolean,
): Int = (floor(ms / (if (hot) TICK_MS / 1.5 else TICK_MS) + .001) % 4).toInt()
