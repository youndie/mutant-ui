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
import androidx.compose.ui.graphics.drawscope.translate
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
import io.github.youndie.mutant.gen.pourFront
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantFill
import io.github.youndie.mutant.tokens.MutantGrid
import io.github.youndie.mutant.tokens.MutantMotion
import io.github.youndie.mutant.tokens.MutantProfile

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

/**
 * A parity fixture's way to draw an outline at the width the engine drew it: the engine seeds the outline's
 * tremble with its running slot count, which a kit has no counterpart for (research §1.10, D16). Null in the kit.
 */
internal val LocalMutantOutlineOverride = staticCompositionLocalOf<((String) -> Float?)?> { null }

/**
 * A parity fixture's way to stand a body where the engine stood it: the engine generates a slot in its stage's
 * coordinates, so its noise depends on where the slot is, where the kit's is its own (D16). The body is generated
 * at that origin and drawn back at its place; the speckle is anchored there too. Null in the kit.
 */
internal val LocalMutantSlotOrigin = staticCompositionLocalOf<((String) -> Offset?)?> { null }

/**
 * A parity fixture's way to give a body the engine's text zone (`textZone`): Chrome measures a label's box at a
 * fraction (83.29 dp) where Compose rounds it up (84), and a flow line starting between the two is kept by one and
 * dropped by the other. Null in the kit, which measures its own content.
 */
internal val LocalMutantTextZone = staticCompositionLocalOf<((String) -> DoubleArray?)?> { null }

/**
 * A parity fixture's way to generate a body at the size the engine generated it: Chrome lays a label-sized slot out
 * at a fraction (103.4 dp) the engine records whole (103), where Compose rounds the label up (104). The body is
 * generated at that size and drawn in its own box. Null in the kit.
 */
internal val LocalMutantSlotSize = staticCompositionLocalOf<((String) -> androidx.compose.ui.geometry.Size?)?> { null }

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

/**
 * This profile with some numbers replaced, as the engine reads a slot's `data-r`, `data-over`, `data-cf`,
 * `data-ow`, `data-wob`, `data-marks` and `data-edge` over its type's (`makeItem`).
 */
internal fun MutantProfile.with(
    radius: Double = this.radius,
    overhang: Double = this.overhang,
    dripShare: Double = this.dripShare,
    outline: Double = this.outline,
    wobble: Double = this.wobble,
    marks: Boolean = this.marks,
    mosaic: Boolean = this.mosaic,
): MutantProfile = MutantProfile(radius, overhang, dripShare, fill, outline, marks, mosaic, wobble, deadPixels, round)

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
 * [stroke] is the outline's ink, the theme's unless given (the outlined button's accent, `data-stroke`).
 *
 * [profile] is [body]'s unless a composition overrides some of its numbers per slot, as the design's
 * illustrations do with `data-r`, `data-over` and the like ([with]).
 *
 * [calm] is the instance's wear (0.2…1.7); [slip] shifts the fill from the outline (default 1.5 dp in a
 * print theme for bodies that are not round); [shadow] draws the misregistered shadow.
 *
 * [state] is the handoff's: [BodyState.Loading] runs the seek bands, [BodyState.Success] is the clean frame,
 * [BodyState.Disabled] is «нет сигнала» with no slime, [BodyState.Error] adds the red offset outline and the
 * corruption blocks. [focused] calms the damage to 0.3 and draws the amber brackets. A press on
 * [interactionSource] boils the body: heat rises to 1 in 320 ms and cools in 480 ms on the screen's clock,
 * and while hot the body is redrawn on the hot tick; [heat] fixes it instead (a fixture, the engine's
 * `data-heat-k`).
 *
 * A change of [fill] pours the new colour down from the top (`clamp(260 + 5 · h, 300, 620)` ms); a new [slide]
 * request slides the body home from `dx` with its stretch (330 ms). Both step at 12 frames a second and land at
 * once under reduce motion. [pourAt] and [slideAt] fix a progress instead (fixtures, the engine's `data-pour`,
 * `data-slide`). [probe] is told the instance each time a frame is built, for tests.
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
    slide: SlideRequest? = null,
    pourAt: Pair<Double, Color>? = null,
    slideAt: Pair<Double, Double>? = null,
    profile: MutantProfile = body.profile,
    stroke: Color? = null,
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
    val instant = io.github.youndie.mutant.theme.LocalMutantReduceMotion.current
    val moves = remember { BodyMoves() }
    val p = profile
    val outlineOverride = LocalMutantOutlineOverride.current
    val origin = LocalMutantSlotOrigin.current?.invoke(key)
    val engineSize = LocalMutantSlotSize.current?.invoke(key)
    val engineZone = LocalMutantTextZone.current?.invoke(key)
    val bodyFill = fill ?: colors.fillOf(p.fill)
    moves.follow(bodyFill, slide, clock.nowMs)
    val look =
        BodyLook(
            fill = bodyFill,
            highlight = highlightFor(bodyFill, colors),
            ink = stroke ?: colors.ink,
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
                val w = engineSize?.width?.toDouble() ?: (size.width / d.toDouble())
                val h = engineSize?.height?.toDouble() ?: (size.height / d.toDouble())
                val gen = MutantGenerator(cassette)
                if (w < 1 || h < 1) return@drawWithCache onDrawBehind {}
                val tz =
                    engineZone ?: zone?.let {
                        doubleArrayOf(
                            it.left - MutantGrid.CLEAN_ZONE_HORIZONTAL,
                            it.top - MutantGrid.CLEAN_ZONE_VERTICAL,
                            it.width + 2 * MutantGrid.CLEAN_ZONE_HORIZONTAL,
                            it.height + 2 * MutantGrid.CLEAN_ZONE_VERTICAL,
                        )
                    }
                val slot =
                    Slot(
                        ox = origin?.x?.toDouble() ?: 0.0,
                        oy = origin?.y?.toDouble() ?: 0.0,
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
                        // A profile overridden per slot: its numbers, not its identity, shape the instance.
                        listOf(slot.r, slot.over, slot.dripShare, slot.wobble, slot.marks, slot.edge),
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
                            translate(-slot.ox.toFloat(), -slot.oy.toFloat()) {
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
                val outline = outlineOverride?.invoke(key) ?: outlineWidth(g, p.outline, frame.boilPhase, frame.boil)
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
                val now = clock.nowMs
                val pourP = pourAt?.first ?: if (instant) 1.0 else stepped(now - moves.pourSince, pourDuration(slot.h))
                val pourFrom = pourAt?.second ?: moves.pourFrom
                val slideP =
                    slideAt?.first ?: if (instant) 1.0 else stepped(now - moves.slideSince, MutantMotion.SLIDE_MS)
                val slideDx = slideAt?.second ?: moves.slideDx
                // A running transition is evaluated on every frame, as the engine does, so its steps keep to the event.
                if (pourP < 1 || slideP < 1) clock.frame
                val pour = if (pourP < 1 && pourFrom != null) gen.pourFront(g, pourP).toPath() to pourFrom else null
                onDrawBehind {
                    scale(d, Offset.Zero) {
                        translate(-slot.ox.toFloat(), -slot.oy.toFloat()) {
                            slid(
                                g,
                                slideP,
                                slideDx,
                            ) { drawBody(g, paths, frameLook, colors, speckle, Offset.Zero, dead, decor, pour) }
                        }
                    }
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

/** A request to slide a body home from [dx] dp; a new [id] starts a new slide. */
internal class SlideRequest(
    val id: Any,
    val dx: Double,
)

/** Follows a body's fill and slide requests from composition without writing state; a change starts a move then. */
internal class BodyMoves {
    private var fill: Color? = null
    private var slideId: Any? = null
    var pourFrom: Color? = null
        private set
    var pourSince: Double = Double.NEGATIVE_INFINITY
        private set
    var slideDx: Double = 0.0
        private set
    var slideSince: Double = Double.NEGATIVE_INFINITY
        private set

    fun follow(
        newFill: Color,
        slide: SlideRequest?,
        now: Double,
    ) {
        val old = fill
        if (old != null && old != newFill) {
            pourFrom = old
            pourSince = now
        }
        fill = newFill
        if (slide != null && slide.id != slideId) {
            if (slideId != null || slideSince == Double.NEGATIVE_INFINITY) {
                slideDx = slide.dx
                slideSince = if (slideId == null) Double.NEGATIVE_INFINITY else now
            }
            slideId = slide.id
        }
    }
}
