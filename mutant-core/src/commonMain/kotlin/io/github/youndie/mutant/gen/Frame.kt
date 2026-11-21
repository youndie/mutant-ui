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
    val steps = frameSteps(g, cache, t, heat, pressed, steadyWebs, grow, paths)
    var done = false
    while (!done) done = steps.columns(Int.MAX_VALUE)
    return steps.frame()
}

/** [frame] a few of its field's columns at a time (B-161): [FrameSteps.columns] until true, then [FrameSteps.frame]. */
internal fun MutantGenerator.frameSteps(
    g: Instance,
    cache: FieldCache,
    t: Double,
    heat: Double = 0.0,
    pressed: Boolean = false,
    steadyWebs: Boolean = false,
    grow: Boolean = true,
    paths: Boolean = true,
): FrameSteps {
    val el = g.slot
    val hot = heat > 0 || pressed
    val boilPhase = boilPhase(t, hot)
    val boil0 = REST_BOIL * el.calm
    val boil = boil0 * (1 + 2 * heat) + .6 * heat
    val prims = primitives(g, t, hot)
    if (heat > 0) prims += bubbles(g, t, heat)
    val field = FieldSteps(g, cache, prims, t, REST_ROUGH * el.calm, boilPhase, boil, heat, boil0, steadyWebs, grow)
    return FrameSteps(this, g, t, prims, field, boilPhase, boil, steadyWebs, paths)
}

/** A frame being built ([frameSteps]): its field a few columns at a time, then its lines. */
internal class FrameSteps(
    private val gen: MutantGenerator,
    private val g: Instance,
    private val t: Double,
    private val prims: List<Primitive>,
    private val field: FieldSteps,
    private val boilPhase: Int,
    private val boil: Double,
    private val steadyWebs: Boolean,
    private val paths: Boolean,
) {
    /** Computes up to [count] more of the field's columns; true once it is whole. */
    fun columns(count: Int): Boolean = field.columns(count)

    fun frame(): BodyFrame {
        val el = g.slot
        val fd = field.field()
        val flow = if (g.flowLines.isEmpty()) GenPath.Empty else gen.flowLinePath(g, t, boilPhase, boil)
        if (!paths) return BodyFrame(GenPath.Empty, GenPath.Empty, flow, prims, fd, boilPhase, boil, steadyWebs)
        val contour = smoothPath(march(fd, 0.0), .9, 5.0)
        val highlight =
            smoothPath(
                march(
                    fd,
                    -3.4,
                    ceil((el.r + 19) / fd.step).toInt() + fd.pad,
                    floor((el.h + 7) / fd.step).toInt() + fd.pad,
                ),
                1.1,
            )
        return BodyFrame(contour, highlight, flow, prims, fd, boilPhase, boil, steadyWebs)
    }
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
