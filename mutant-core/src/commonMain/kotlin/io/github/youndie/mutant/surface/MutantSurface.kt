package io.github.youndie.mutant.surface

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.REST_BOIL
import io.github.youndie.mutant.gen.REST_ROUGH
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantFill
import io.github.youndie.mutant.tokens.MutantGrid
import io.github.youndie.mutant.tokens.MutantMotion

/** Collects the bounds of what a surface's content marks clean, relative to the surface. */
@Stable
internal class CleanZoneRegistry {
    var surface: LayoutCoordinates? = null
    val bounds = mutableStateMapOf<Any, Rect>()

    /** The union of the marked bounds, dp, or null when nothing is marked. */
    fun union(density: Float): Rect? =
        bounds.values
            .takeIf { it.isNotEmpty() }
            ?.let { rs ->
                Rect(rs.minOf { it.left }, rs.minOf { it.top }, rs.maxOf { it.right }, rs.maxOf { it.bottom })
            }?.let { Rect(it.left / density, it.top / density, it.right / density, it.bottom / density) }
}

internal val LocalCleanZone = staticCompositionLocalOf<CleanZoneRegistry?> { null }

/**
 * Marks this element's bounds as part of its surface's clean zone: the text and icons that flow lines,
 * mosaic and dead pixels never enter (research D5, the engine's `textZone`).
 */
internal fun Modifier.mutantCleanZone(): Modifier =
    composed {
        val registry = LocalCleanZone.current ?: return@composed this
        val id = remember { Any() }
        androidx.compose.runtime.DisposableEffect(registry) { onDispose { registry.bounds.remove(id) } }
        onGloballyPositioned { c ->
            val s = registry.surface ?: return@onGloballyPositioned
            if (!s.isAttached || !c.isAttached) return@onGloballyPositioned
            val r = s.localBoundingBoxOf(c, clipBounds = false)
            if (registry.bounds[id] != r) registry.bounds[id] = r
        }
    }

/** The fill a profile asks for, in a theme. */
internal fun MutantColors.fillOf(fill: MutantFill): Color =
    when (fill) {
        MutantFill.Primary -> primary
        MutantFill.Surface -> surface
        MutantFill.SurfaceVariant -> surfaceVariant
        MutantFill.Phosphor -> phosphor
        MutantFill.Magenta -> magenta
    }

/**
 * A generated slime body behind [content] (research D5): the shape of [body]'s profile for this [key] and
 * size, drawn once per tick of the screen's clock, behind the content and outside the bounds — drips hang up
 * to the profile's overhang below the slot and take no space. The content marks its text and icons with
 * [mutantCleanZone]; nothing decorative enters that zone. The body's origin is its own, so its shape travels
 * with it when it moves; a size change regenerates it.
 *
 * [calm] is the instance's wear (0.2…1.7); [slip] shifts the fill from the outline (default 1.5 dp in a
 * print theme for bodies that are not round); [shadow] draws the misregistered shadow.
 *
 * [state] is the handoff's: [BodyState.Loading] runs the seek bands, [BodyState.Success] is the clean frame,
 * [BodyState.Disabled] is «нет сигнала» with no slime, [BodyState.Error] adds the red offset outline and the
 * corruption blocks. [focused] calms the damage to 0.3 and draws the amber brackets. A press on
 * [interactionSource] boils the body: heat rises to 1 in 320 ms and cools in 480 ms on the screen's clock,
 * and while hot the body is redrawn on the hot tick; [heat] fixes it instead (a fixture, the engine's
 * `data-heat-k`). [probe] is told the instance each time a frame is built, for tests.
 */
@Composable
internal fun MutantSurface(
    body: MutantBody,
    key: String,
    modifier: Modifier = Modifier,
    fill: Color? = null,
    calm: Double = 1.0,
    slip: Double? = null,
    shadow: Color? = null,
    state: BodyState = BodyState.Rest,
    focused: Boolean = false,
    interactionSource: InteractionSource? = null,
    heat: Double? = null,
    probe: ((io.github.youndie.mutant.gen.Instance) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalMutantColors.current
    val cassette = LocalMutantCassette.current
    val stage = LocalMutantStage.current
    val clock = readMutantClock()
    val registry = remember { CleanZoneRegistry() }
    val held = remember { HeldInstance() }
    val boil = remember { HeatMachine() }
    if (interactionSource != null) {
        androidx.compose.runtime.LaunchedEffect(interactionSource, clock) {
            interactionSource.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> boil.press(clock.nowMs)
                    is PressInteraction.Release, is PressInteraction.Cancel -> boil.release(clock.nowMs)
                }
            }
        }
    }
    val wear = if (focused) FOCUS_CALM else calm
    val p = body.profile
    val bodyFill = fill ?: colors.fillOf(p.fill)
    val look =
        BodyLook(
            fill = bodyFill,
            highlight = highlightFor(bodyFill, colors),
            ink = colors.ink,
            outline = p.outline.toFloat(),
            slip = (slip ?: if (colors.print && !p.round) PRINT_SLIP else 0.0).toFloat(),
            shadow = shadow,
        )
    Box(
        modifier
            .onGloballyPositioned { registry.surface = it }
            .drawWithCache {
                val d = density
                val zone = registry.union(d)
                val w = size.width / d.toDouble()
                val h = size.height / d.toDouble()
                val gen = MutantGenerator(cassette)
                if (w < 1 || h < 1) return@drawWithCache onDrawBehind {}
                val tz =
                    zone?.let {
                        doubleArrayOf(
                            it.left - MutantGrid.CLEAN_ZONE_HORIZONTAL,
                            it.top - MutantGrid.CLEAN_ZONE_VERTICAL,
                            it.width + 2 * MutantGrid.CLEAN_ZONE_HORIZONTAL,
                            it.height + 2 * MutantGrid.CLEAN_ZONE_VERTICAL,
                        )
                    }
                val slot =
                    Slot(
                        ox = 0.0,
                        oy = 0.0,
                        w = w,
                        h = h,
                        r = minOf(p.radius, w / 2, h / 2),
                        over = p.overhang * (if (wear < 1) .4 + .6 * wear else 1.0),
                        dripShare = p.dripShare,
                        wobble = p.wobble,
                        round = p.round,
                        calm = wear,
                        dead = p.deadPixels,
                        marks = p.marks,
                        edge = p.mosaic,
                        textZone = tz ?: doubleArrayOf(-99.0, -99.0, 0.0, 0.0),
                        cleanZone =
                            tz?.let { doubleArrayOf(it[0] - 2, it[1] - 2, it[2] + 4, it[3] + 4) }
                                ?: doubleArrayOf(0.0, 0.0, 0.0, 0.0),
                    )
                val signature =
                    listOf(
                        cassette,
                        key,
                        slot.w,
                        slot.h,
                        slot.calm,
                        body,
                        tz?.toList(),
                        state == BodyState.Disabled || state == BodyState.Success,
                    )
                val (g, cache, dead) =
                    held.of(signature) {
                        gen
                            .instance(
                                slot,
                                key,
                            ).let { Triple(it, FieldCache(), if (slot.dead) gen.deadPixels(it) else null) }
                    }
                probe?.invoke(g)
                if (state == BodyState.Disabled || state == BodyState.Success) {
                    if (state == BodyState.Disabled && slot.edge && slot.w >= 40 &&
                        g.mosaicTiles.isEmpty()
                    ) {
                        gen.edgeMosaic(g)
                    }
                    val patterns = StagePatterns.of(cassette, stage)
                    return@drawWithCache onDrawBehind {
                        scale(d, Offset.Zero) {
                            if (state == BodyState.Disabled) {
                                drawDisabledBody(
                                    g,
                                    look.fill,
                                    look.highlight,
                                    colors,
                                    p.outline.toFloat(),
                                    patterns,
                                    Offset.Zero,
                                )
                            } else {
                                drawCleanBody(g, look.fill, look.highlight, look.ink, p.outline.toFloat())
                            }
                        }
                    }
                }
                val speckle = Speckle.of(cassette, stage, REST_ROUGH * wear)
                val hot = heat?.let { it > 0 } ?: (boil.on || boil.at(clock.nowMs) > 0)
                val t =
                    when {
                        clock.frozen -> clock.nowMs
                        hot -> clock.hotTimeMs()
                        else -> clock.restTimeMs()
                    }
                val k = heat ?: boil.at(clock.nowMs)
                val frame = gen.frame(g, cache, t, k, pressed = boil.on)
                val paths = BodyPaths.of(frame)
                val outline = outlineWidth(g, p.outline, frame.boilPhase, frame.boil)
                val frameLook =
                    BodyLook(
                        boilFill(look.fill, colors, k, t),
                        look.highlight,
                        look.ink,
                        outline,
                        look.slip,
                        look.shadow,
                        p.outline.toFloat(),
                    )
                val decor = gen.decorOf(g, state, focused, k, t, colors)
                onDrawBehind {
                    scale(d, Offset.Zero) { drawBody(g, paths, frameLook, colors, speckle, Offset.Zero, dead, decor) }
                }
            },
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalCleanZone provides registry) { content() }
    }
}

/** The print theme's default slip, dp (tokens `genome.slip`: «тема «Копия» 1.5»). */
private const val PRINT_SLIP: Double = 1.5

/**
 * One surface's instance and field cache, kept across ticks and rebuilt only when what shapes the instance
 * changes (cassette, key, size, wear, profile, clean zone): a fresh cache makes a cold frame, ten times a warm
 * one (B-06).
 */
internal class HeldInstance {
    private var signature: List<Any?>? = null
    private var held:
        Triple<io.github.youndie.mutant.gen.Instance, FieldCache, io.github.youndie.mutant.gen.DeadPixels?>? = null

    /** How many times the instance was built: a test reads it. */
    var builds: Int = 0
        private set

    fun of(
        signature: List<Any?>,
        build:
            () -> Triple<io.github.youndie.mutant.gen.Instance, FieldCache, io.github.youndie.mutant.gen.DeadPixels?>,
    ): Triple<io.github.youndie.mutant.gen.Instance, FieldCache, io.github.youndie.mutant.gen.DeadPixels?> {
        val h = held
        if (h != null && signature == this.signature) return h
        builds++
        this.signature = signature
        return build().also { held = it }
    }
}

/** A focused body's wear (`data-calm=".3"`: «повреждения 30 %»). */
private const val FOCUS_CALM: Double = .3

/**
 * The boil's heat (the engine's `frame`): rising to 1 in 320 ms while pressed, cooling to 0 in 480 ms after,
 * from wherever it was. A pure function of the clock's time and the last press or release; only [on] is state.
 */
@androidx.compose.runtime.Stable
internal class HeatMachine {
    var on: Boolean by mutableStateOf(false)
        private set
    private var from = 0.0
    private var since = 0.0

    fun at(now: Double): Double =
        if (on) {
            minOf(1.0, from + (now - since) / MutantMotion.BOIL_OVER_IN_MS)
        } else {
            maxOf(
                0.0,
                from - (now - since) / MutantMotion.BOIL_OVER_OUT_MS,
            )
        }

    fun press(now: Double) {
        if (on) return
        from = at(now)
        since = now
        on = true
    }

    fun release(now: Double) {
        if (!on) return
        from = at(now)
        since = now
        on = false
    }
}
