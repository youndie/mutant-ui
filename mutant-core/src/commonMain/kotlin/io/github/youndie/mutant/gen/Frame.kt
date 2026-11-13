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
 * the held drops with no heat yet, as the engine does between press and the first heat.
 */
internal fun MutantGenerator.frame(
    g: Instance,
    cache: FieldCache,
    t: Double,
    heat: Double = 0.0,
    pressed: Boolean = false,
): BodyFrame {
    val el = g.slot
    val hot = heat > 0 || pressed
    val boilPhase = (floor(t / (if (hot) TICK_MS / 1.5 else TICK_MS) + .001) % 4).toInt()
    val boil0 = REST_BOIL * el.calm
    val boil = boil0 * (1 + 2 * heat) + .6 * heat
    val prims = primitives(g, t, hot)
    if (heat > 0) prims += bubbles(g, t, heat)
    val fd = distanceField(g, cache, prims, t, REST_ROUGH * el.calm, boilPhase, boil, heat, boil0)
    val contour = smoothPath(march(fd, 0.0), .9, 5.0)
    val highlight =
        smoothPath(
            march(fd, -3.4, ceil((el.r + 19) / fd.step).toInt(), floor((el.h + 7) / fd.step).toInt()),
            1.1,
        )
    val flow = if (g.flowLines.isEmpty()) GenPath.Empty else flowLinePath(g, t, boilPhase, boil)
    return BodyFrame(contour, highlight, flow, prims, fd, boilPhase, boil)
}
