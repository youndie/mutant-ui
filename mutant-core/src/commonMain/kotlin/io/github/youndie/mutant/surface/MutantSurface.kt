package io.github.youndie.mutant.surface

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.BodyFrame
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.Instance
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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
 * `data-slide`); no fixture passes either today, and they stay because dropping them renames the content lambdas of
 * four components in the ABI dump (B-147). [probe] is told the instance each time the body's drawing is built: for
 * tests only, never for a consumer.
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
    val ahead = remember { FrameAhead() }
    val stillLayers = remember { StillLayers() }
    val stillOnce = LocalMutantStillLayersOnce.current
    val frameProbe = LocalMutantFrameProbe.current
    val skipParts = LocalMutantSkipBodyParts.current
    // B-110, a spike: the body's silhouette drawn by a shader from its field, where the platform has one.
    // B-137: the mosaic past the contour is drawn by paths over the outline; such a body keeps its paths.
    val onGpu =
        LocalMutantBodyOnGpu.current && bodyShaderSupported &&
            !io.github.youndie.mutant.theme.LocalMutantMosaicPastContour.current
    val bodyAsImage = LocalMutantBodyAsImage.current
    val rasterProbe = LocalMutantRasterProbe.current
    val scope = rememberCoroutineScope()
    // A body's first instance is built off the frame (B-50), unless the clock is frozen (fixtures, references) or an
    // IDE preview draws it: those draw in their first frame, as before.
    val offFrame = LocalMutantBuildOffFrame.current && !clock.frozen && !LocalInspectionMode.current
    // B-84: a running body's webs follow its drips, not the boil; a still keeps the engine's field.
    val steadyWebs = !clock.frozen
    val boil = remember { HeatMachine() }
    if (interactionSource != null) {
        androidx.compose.runtime.LaunchedEffect(interactionSource, clock) {
            interactionSource.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> boil.press(clock.nowMs)

                    is PressInteraction.Release, is PressInteraction.Cancel -> boil.release(clock.nowMs)

                    // A mouse only: Compose's clickable reports no hover for a finger (B-63).
                    is androidx.compose.foundation.interaction.HoverInteraction.Enter -> boil.hover(clock.nowMs)

                    is androidx.compose.foundation.interaction.HoverInteraction.Exit -> boil.leave(clock.nowMs)
                }
            }
        }
    }
    val wear = if (focused) FOCUS_CALM else calm
    val instant = io.github.youndie.mutant.theme.LocalMutantReduceMotion.current
    val moves = remember { BodyMoves() }
    // The instance of the body's last draw: the pour's front is read from it (B-94).
    val drawn = remember { mutableStateOf<Instance?>(null) }
    val drawnPlain = remember { arrayOfNulls<Instance>(1) }
    val p = profile
    val outlineOverride = LocalMutantOutlineOverride.current
    val origin = LocalMutantSlotOrigin.current?.invoke(key)
    val engineSize = LocalMutantSlotSize.current?.invoke(key)
    val engineZone = LocalMutantTextZone.current?.invoke(key)
    // B-48: the screen's keyframe loss takes this body's picture and cuts its band out of it.
    val moshScene = LocalMutantMoshScene.current
    val moshBody = rememberMoshBody(moshScene)
    val view = remember { BodyView() }
    val reach = with(androidx.compose.ui.platform.LocalDensity.current) { OFF_SCREEN_REACH.dp.toPx() }
    val bodyFill = fill ?: colors.fillOf(p.fill)
    val steps = io.github.youndie.mutant.theme.LocalMutantSteppedMotion.current
    val pastContour = io.github.youndie.mutant.theme.LocalMutantMosaicPastContour.current
    moves.follow(bodyFill, slide, clock.nowMs, drawnPlain[0]?.slot?.h, steps)
    val look =
        BodyLook(
            fill = bodyFill,
            highlight = highlightFor(bodyFill, colors),
            ink = stroke ?: colors.ink,
            outline = p.outline.toFloat(),
            slip = (slip ?: if (colors.print && !p.round) PRINT_SLIP else 0.0).toFloat(),
            shadow = shadow,
        )
    // B-91: the fill a boiling body shows now, for its words to read against; null at rest. Derived per frame, its
    // readers recompose only when their pick of colour changes.
    val fillNow by androidx.compose.runtime.rememberUpdatedState(bodyFill)
    val colorsNow by androidx.compose.runtime.rememberUpdatedState(colors)
    val heatNow by androidx.compose.runtime.rememberUpdatedState(heat)
    // B-94: while a new fill pours down from the top, what stands under the label is the old fill until the front, its
    // drips included, has passed the label's middle. The instance comes from the body's last draw ([drawn]).
    val instantNow by androidx.compose.runtime.rememberUpdatedState(instant)
    val cassetteNow by androidx.compose.runtime.rememberUpdatedState(cassette)
    val ground =
        remember(boil, clock) {
            androidx.compose.runtime.derivedStateOf {
                // The clock's time is not state; its ticks are. A boiling body is redrawn on the hot tick, with the heat
                // of that frame, so the ground follows the hot tick too: followed on every frame, it ran ahead of the
                // fill on screen between two ticks, and a dim word turned black on a ground still dark (B-104).
                clock.hotTick
                val now = clock.nowMs
                val k = heatNow ?: boil.at(now)
                val from = moves.pourFrom
                val g = drawn.value
                val p = g?.let { stepped(now - moves.pourSince, pourDuration(it.slot.h), steps) } ?: 1.0
                // A pour asked for before the body first draws starts when it does: the old fill stands until then.
                val pouring = from != null && !instantNow && (g == null || p < 1)
                // A pour is drawn on every frame, and its front read on every frame with it.
                if (pouring) clock.frame
                val under =
                    if (pouring && g == null) {
                        from
                    } else if (pouring && g != null) {
                        val el = g.slot
                        val depth = MutantGenerator(cassetteNow).pourFront(g, p).depthUnderLabel(el.ox, el.oy, el.w)
                        if (depth < el.h / 2) from else fillNow
                    } else {
                        fillNow
                    }
                if (k <= 0 && !pouring) null else boilFill(under, colorsNow, k, clock.hotTimeMs())
            }
        }
    Box(
        modifier
            .onGloballyPositioned {
                registry.surface = it
                moshBody.coords = it
                view.place(it, reach)
            }
            // B-135: a layer of its own, so a tick records the body and its content and nothing beside it.
            .graphicsLayer()
            .drawWithCache {
                // An instance built off the frame arriving invalidates this cache.
                held.arrivals
                // B-87: a body off the screen builds no tick and reads no clock; its layout tells it when it shows
                // again ([BodyView.place]). Off the window it draws nothing; outside a box it is laid out in but on the
                // window, which that box may not clip, it draws its last picture.
                view.flips
                val last = moshBody.draw
                val where = view.whereNow(registry.surface, reach)
                if (last != null && where != SHOWN) {
                    if (where == OFF_WINDOW) return@drawWithCache onDrawBehind {}
                    return@drawWithCache moshBehind(moshScene, moshBody, snapshot = moshBody.snapshot, block = last)
                }
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
                moshBody.zone = tz
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
                val still = state == BodyState.Disabled || state == BodyState.Success
                val build = {
                    gen
                        .instance(
                            slot,
                            key,
                        ).let { Triple(it, FieldCache(), if (slot.dead) gen.deadPixels(it) else null) }
                }
                val speckle = Speckle.of(cassette, stage, REST_ROUGH * wear)

                // What a body's picture is drawn with at time [tt] and heat [kk], from its frame.
                fun pictureOf(
                    g: Instance,
                    f: BodyFrame,
                    tt: Double,
                    kk: Double,
                ): BodyPicture {
                    val outline = outlineOverride?.invoke(key) ?: outlineWidth(g, p.outline, f.boilPhase, f.boil)
                    val frameLook =
                        BodyLook(
                            boilFill(look.fill, colors, kk, tt),
                            look.highlight,
                            look.ink,
                            outline,
                            look.slip,
                            look.shadow,
                            p.outline.toFloat(),
                        )
                    return BodyPicture(BodyPaths.of(f), frameLook, gen.decorOf(g, state, focused, kk, tt, colors))
                }

                // B-131: the shader's brush with every decoration, or null where the body keeps its paths: anything else
                // drawn under the clip, or a surface off whole pixels, whose grid the still layers' image would miss.
                fun shaderOf(
                    g: Instance,
                    f: io.github.youndie.mutant.gen.BodyFrame,
                    dead: io.github.youndie.mutant.gen.DeadPixels?,
                    tt: Double,
                    kk: Double,
                ): androidx.compose.ui.graphics.Brush? {
                    val pic = pictureOf(g, f, tt, kk)
                    val decor = pic.decor
                    if (decor.runningMosaic != null || decor.errorBlocks.isNotEmpty() || decor.bands.isNotEmpty() ||
                        decor.errorOutline != null || decor.brackets != null
                    ) {
                        return null
                    }
                    val layers = listOf(BodyPart.Speckle, BodyPart.Mosaic, BodyPart.DeadPixels).any { it !in skipParts }
                    val still =
                        if (!layers) {
                            null
                        } else {
                            if (!onWholePixels(registry.surface)) return null
                            val grid = Offset(slot.ox.toFloat(), slot.oy.toFloat())
                            stillLayers.of(
                                g,
                                listOf(
                                    look.slip,
                                    colors,
                                    d,
                                    layoutDirection,
                                    skipParts,
                                    cassette,
                                    stage,
                                    wear,
                                    dead,
                                    grid,
                                ),
                            ) {
                                stillLayersImage(
                                    g,
                                    look.slip,
                                    colors,
                                    speckle,
                                    Offset.Zero,
                                    dead,
                                    skipParts,
                                    d,
                                    layoutDirection,
                                    grid.x,
                                    grid.y,
                                )
                            }
                        }
                    val flow =
                        if (BodyPart.FlowLines in
                            skipParts
                        ) {
                            io.github.youndie.mutant.gen.GenPath.Empty
                        } else {
                            f.flowLines
                        }
                    return bodyShaderBrush(f.field, slot, pic.look, d, still, flow)
                }

                // B-117: the body's still layers, drawn once for its instance and what they are drawn with.
                fun stillOf(
                    g: Instance,
                    dead: io.github.youndie.mutant.gen.DeadPixels?,
                    inFrame: Boolean,
                ): StillImage? {
                    if (!stillOnce) return null
                    // The surface's grid, the slot's origin at its pixel (0, 0): the tick's image is drawn on it (B-115), and
                    // so is the frame, where the surface stands on whole pixels; elsewhere the layers are drawn as before.
                    // A frozen clock (fixtures, references) draws them as before, as it draws vectors and no tick image (B-82).
                    if (inFrame && (clock.frozen || moshScene != null || !onWholePixels(registry.surface))) return null
                    val grid = Offset(slot.ox.toFloat(), slot.oy.toFloat())
                    return stillLayers.of(
                        g,
                        listOf(look.slip, colors, d, layoutDirection, skipParts, cassette, stage, wear, dead, grid),
                    ) {
                        stillLayersImage(
                            g,
                            look.slip,
                            colors,
                            speckle,
                            Offset.Zero,
                            dead,
                            skipParts,
                            d,
                            layoutDirection,
                            grid.x,
                            grid.y,
                        )
                    }
                }

                // B-82: everything the image of a resting body depends on besides its frame. An image built ahead is
                // used only if these are still what they were when it was prepared.
                fun imageInputsOf(dead: io.github.youndie.mutant.gen.DeadPixels?): List<Any?>? =
                    if (bodyAsImage) {
                        listOf(
                            look.fill,
                            look.highlight,
                            look.ink,
                            look.outline,
                            look.slip,
                            look.shadow,
                            colors,
                            state,
                            focused,
                            d,
                            layoutDirection,
                            skipParts,
                            cassette,
                            stage,
                            wear,
                            dead,
                        )
                    } else {
                        null
                    }

                // A resting frame at [next], built off the frame, with its image when the body is drawn as one (B-75, B-82).
                fun buildAhead(
                    g: Instance,
                    cache: FieldCache,
                    dead: io.github.youndie.mutant.gen.DeadPixels?,
                    next: Double,
                    band: Rect?,
                ): Ahead {
                    // Built off the frame's thread: the field's grid is not grown here (B-97).
                    val f =
                        gen.frame(
                            g,
                            cache,
                            next,
                            0.0,
                            pressed = false,
                            steadyWebs = steadyWebs,
                            grow = false,
                            paths = !onGpu,
                        )
                    // B-110: a body the shader draws gets its brush ahead, with its frame, and no image.
                    if (onGpu) {
                        return Ahead(
                            f,
                            null,
                            null,
                            shaderOf(g, f, dead, next, 0.0),
                        )
                    }
                    val inputs = imageInputsOf(dead)
                    val image =
                        inputs?.let {
                            val t0 =
                                kotlin.time.TimeSource.Monotonic
                                    .markNow()
                            val pic = pictureOf(g, f, next, 0.0)
                            val still = stillOf(g, dead, inFrame = false)
                            rasterisedBody(g, slot.ox, slot.oy, d, layoutDirection, band) {
                                drawBody(
                                    g,
                                    pic.paths,
                                    pic.look,
                                    colors,
                                    speckle,
                                    Offset.Zero,
                                    dead,
                                    pic.decor,
                                    null,
                                    skipParts,
                                    still,
                                    pastContour,
                                )
                            }.also {
                                rasterProbe?.invoke(
                                    t0.elapsedNow().inWholeNanoseconds,
                                    it.image.width * it.image.height * 4L,
                                )
                            }
                        }
                    return Ahead(f, image, inputs)
                }
                if (offFrame && held.isEmpty) {
                    // The static layers are what makes a first frame ten times a warm one: build them with the
                    // instance on a background thread, and draw the body from the next frame.
                    val warmAt = clock.nowMs
                    // B-83: a clock that stands (held by a dialog, paused) has no next tick, so a body under it would
                    // never be given an image and would draw its vectors on every frame something else asks for. Its
                    // first build also rasterises it for the time it stands at, and the body draws that image from its
                    // first frame: nothing switches under the stopped clock.
                    val firstAt = clock.restTimeMs()
                    val firstImage =
                        bodyAsImage && !still && heat == null && !boil.on && (clock.paused || clock.holds > 0)
                    held.buildOffFrame(
                        signature,
                        scope,
                        taken = { built, first: Ahead? -> first?.let { ahead.preset(built.first, firstAt, it) } },
                    ) {
                        val built = build()
                        val (g, cache, dead) = built
                        val first =
                            when {
                                // A standing clock has no next tick to follow a scroll: its one image is the whole body.
                                firstImage -> {
                                    buildAhead(g, cache, dead, firstAt, null)
                                }

                                !still -> {
                                    null.also {
                                        gen.frame(
                                            g,
                                            cache,
                                            warmAt,
                                            0.0,
                                            pressed = false,
                                            steadyWebs = steadyWebs,
                                        )
                                    }
                                }

                                else -> {
                                    null
                                }
                            }
                        built to first
                    }
                    return@drawWithCache onDrawBehind {}
                }
                val (g, cache, dead) = held.of(signature, build)
                // Compared through a plain reference: reading the state here would make this cache depend on its own write.
                if (drawnPlain[0] !== g) {
                    drawnPlain[0] = g
                    drawn.value = g
                }
                probe?.invoke(g)
                if (state == BodyState.Disabled || state == BodyState.Success) {
                    if (state == BodyState.Disabled && slot.edge && slot.w >= 40 &&
                        g.mosaicTiles.isEmpty()
                    ) {
                        gen.edgeMosaic(g)
                    }
                    val patterns = StagePatterns.of(cassette, stage)
                    return@drawWithCache moshBehind(moshScene, moshBody) {
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
                                    // A Success body is still a control: its keyboard focus shows (B-57).
                                    if (focused) drawBrackets(g, colors.focus)
                                }
                            }
                        }
                    }
                }
                val hot = heat?.let { it > 0 } ?: (boil.on || boil.hovered || boil.at(clock.nowMs) > 0)
                val t =
                    when {
                        clock.frozen -> clock.nowMs
                        hot -> clock.hotTimeMs()
                        else -> clock.restTimeMs()
                    }
                val k = heat ?: boil.at(clock.nowMs)
                // A resting tick's frame depends on its time alone, and the next tick's time is known: it is built
                // ahead on a background thread and taken here (B-75). A pressed or heated body, a frozen clock and a
                // frame not ready in time build in the frame, as before.
                val resting = offFrame && !hot && !boil.on && k == 0.0
                val early = if (resting) ahead.take(g, t) else null

                var frame =
                    early?.frame
                        ?: gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs, paths = !onGpu)
                // B-86: what the window shows of the body now, in the surface's pixels; an image covering less is not drawn.
                val shown = registry.surface?.takeIf { it.isAttached }?.let { windowIn(it) }
                if (resting && cache.settledFor(g)) {
                    // B-89: the next tick's time as the clock will read it, so the frame built for it is found.
                    val next = clock.nextRestTimeMs()
                    ahead.prepare(g, next, shown, scope) { band -> buildAhead(g, cache, dead, next, band) }
                }
                val now = clock.nowMs
                val pourP =
                    pourAt?.first ?: if (instant) 1.0 else stepped(now - moves.pourSince, pourDuration(slot.h), steps)
                val pourFrom = pourAt?.second ?: moves.pourFrom
                val slideP =
                    slideAt?.first
                        ?: if (instant) 1.0 else stepped(now - moves.slideSince, MutantMotion.SLIDE_MS, steps)
                val slideDx = slideAt?.second ?: moves.slideDx
                val slideDy = if (slideAt != null) 0.0 else moves.slideDy
                // A running transition is evaluated on every frame, as the engine does, so its steps keep to the event.
                if (pourP < 1 || slideP < 1) clock.frame
                val pour = if (pourP < 1 && pourFrom != null) gen.pourFront(g, pourP).toPath() to pourFrom else null
                // B-110: a pouring or sliding body is drawn by paths, as before.
                if (onGpu && (pour != null || slideP < 1)) {
                    frame = gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs)
                } else if (onGpu) {
                    val shaded =
                        early?.shaded.takeIf { early?.frame === frame }
                            ?: shaderOf(g, frame, dead, t, k)
                    if (shaded != null) {
                        view.drawn = null
                        frameProbe?.invoke(early != null, false, frame.steadyWebs)
                        val ox = slot.ox.toFloat()
                        val oy = slot.oy.toFloat()
                        // B-133: the keyframe loss's snapshot is a CPU bitmap, where the shader would run pixel by pixel;
                        // there the body draws by paths, built only when a loss takes its snapshot.
                        val byPaths: DrawScope.() -> Unit = {
                            val pf = gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs)
                            val (pp, pl, pd) = pictureOf(g, pf, t, k)
                            scale(d, Offset.Zero) {
                                translate(-ox, -oy) {
                                    drawBody(g, pp, pl, colors, speckle, Offset.Zero, dead, pd, null, skipParts, null)
                                }
                            }
                        }
                        return@drawWithCache moshBehind(moshScene, moshBody, snapshot = byPaths) {
                            scale(d, Offset.Zero) {
                                translate(-ox, -oy) {
                                    drawRect(
                                        shaded,
                                        Offset(ox - 16, oy - 19),
                                        androidx.compose.ui.geometry.Size(
                                            slot.w.toFloat() + 34,
                                            (slot.h + slot.over).toFloat() + 55,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    frame = gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs)
                }
                val imageInputs = imageInputsOf(dead)
                val image =
                    early?.image?.takeIf {
                        imageInputs != null && early.inputs == imageInputs && pour == null &&
                            slideP >= 1 && it.covers(shown)
                    }
                view.drawn = image
                frameProbe?.invoke(early != null, image != null, frame.steadyWebs)
                if (image != null) {
                    return@drawWithCache moshBehind(moshScene, moshBody) {
                        drawImage(
                            image.image,
                            dstOffset = image.offset,
                            dstSize =
                                androidx.compose.ui.unit
                                    .IntSize(image.image.width, image.image.height),
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                        )
                    }
                }
                val (paths, frameLook, decor) = pictureOf(g, frame, t, k)
                // B-117: drawn in the frame too, where a body is drawn when its tick's image is not ready (a thread too
                // busy to build ahead), on the surface's own grid. Not mid-slide.
                val stillLayer = if (slideP >= 1) stillOf(g, dead, inFrame = true) else null
                moshBehind(moshScene, moshBody) {
                    scale(d, Offset.Zero) {
                        translate(-slot.ox.toFloat(), -slot.oy.toFloat()) {
                            slid(
                                g,
                                slideP,
                                slideDx,
                                slideDy,
                            ) {
                                drawBody(
                                    g,
                                    paths,
                                    frameLook,
                                    colors,
                                    speckle,
                                    Offset.Zero,
                                    dead,
                                    decor,
                                    pour,
                                    skipParts,
                                    stillLayer,
                                    pastContour,
                                )
                            }
                        }
                    }
                }
            },
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalCleanZone provides registry, LocalMutantBodyGround provides ground) { content() }
    }
}

/**
 * How far past its box a body can ink, dp — its drips, shadow, slip and outline (B-82's raster margin and more) — so
 * a body counts as on the screen while any of that may show (B-87).
 */
private const val OFF_SCREEN_REACH = 64

/**
 * Where a body stands against the screen (B-87): [SHOWN] when its box, [OFF_SCREEN_REACH] wider all round, meets every
 * box it is laid out in — each layout and layout modifier up to the window; [OFF_WINDOW] when it misses the window
 * itself; [OFF_BOX] when it misses only a box inside it, which may or may not clip it. [flips] counts the changes
 * [place] saw, and invalidates the body's drawing.
 */
@androidx.compose.runtime.Stable
internal class BodyView {
    var flips by mutableIntStateOf(0)
        private set
    private var where = SHOWN

    /** After a layout: a change of where the body stands asks for its drawing again. */
    fun place(
        at: androidx.compose.ui.layout.LayoutCoordinates,
        reach: Float,
    ) {
        val now = at.standing(reach)
        // B-86: scrolled past the part of a tall body its image covers, the body draws again, as vectors until the next
        // tick's image covers where it now stands.
        val uncovered = drawn?.let { !it.covers(windowIn(at)) } ?: false
        if (now != where || uncovered) {
            where = now
            drawn = null
            flips++
        }
    }

    /** The image the body last drew, or null when it drew vectors (B-86). */
    var drawn: RasterisedBody? = null

    /** At a draw: where the body stands now, read from where it was last laid out. */
    fun whereNow(
        at: androidx.compose.ui.layout.LayoutCoordinates?,
        reach: Float,
    ): Int = (if (at == null || !at.isAttached) SHOWN else at.standing(reach)).also { where = it }
}

/**
 * The window's bounds in [at]'s own pixels (B-86): what of a box can show, whatever the boxes between clip. Null when
 * the box is no longer attached.
 */
internal fun windowIn(at: LayoutCoordinates): Rect? {
    if (!at.isAttached) return null
    var root = at
    while (true) root = root.parentCoordinates ?: break
    val w = root.size.width.toFloat()
    val h = root.size.height.toFloat()
    val corners = listOf(Offset.Zero, Offset(w, 0f), Offset(0f, h), Offset(w, h)).map { at.localPositionOf(root, it) }
    return Rect(corners.minOf { it.x }, corners.minOf { it.y }, corners.maxOf { it.x }, corners.maxOf { it.y })
}

/**
 * What a tall body rasterises ahead (B-86): the window, half its height and half its width more on each side, in the
 * surface's pixels, so a tick's image still covers the screen after a scroll of half a window.
 */
internal fun bandAround(window: Rect): Rect =
    Rect(
        window.left - window.width * BAND_SPARE,
        window.top - window.height * BAND_SPARE,
        window.right + window.width * BAND_SPARE,
        window.bottom + window.height * BAND_SPARE,
    )

/** How much of the window, on each side, a tall body's image covers past it (B-86). */
private const val BAND_SPARE = .5f

internal const val SHOWN = 0
internal const val OFF_BOX = 1
internal const val OFF_WINDOW = 2

/** Where this box, [reach] wider all round, stands against each box it is laid out in, up to the window. */
internal fun androidx.compose.ui.layout.LayoutCoordinates.standing(reach: Float): Int {
    var c = this
    var a = Offset(-reach, -reach)
    var b = Offset(size.width + reach, size.height + reach)
    var missed = false
    while (true) {
        val p = c.parentCoordinates ?: return if (missed) OFF_BOX else SHOWN
        a = p.localPositionOf(c, a)
        b = p.localPositionOf(c, b)
        val w = p.size.width + reach
        val h = p.size.height + reach
        val outside =
            maxOf(a.x, b.x) < -reach || maxOf(a.y, b.y) < -reach || minOf(a.x, b.x) > w || minOf(a.y, b.y) > h
        if (outside && p.parentCoordinates == null) return OFF_WINDOW
        missed = missed || outside
        c = p
    }
}

/**
 * Where a body's builds off the frame run (B-50, B-75, B-82): `Dispatchers.Default`, but in the browser, where it is the
 * one thread, each build is a task of its own, so a tick's builds do not all land between the same two frames (B-87).
 */
internal expect val aheadDispatcher: kotlinx.coroutines.CoroutineDispatcher

/** The print theme's default slip, dp (tokens `genome.slip`: «тема «Копия» 1.5»). */
private const val PRINT_SLIP: Double = 1.5

private typealias Built =
    Triple<io.github.youndie.mutant.gen.Instance, FieldCache, io.github.youndie.mutant.gen.DeadPixels?>

/**
 * One surface's instance and field cache, kept across ticks and rebuilt only when what shapes the instance
 * changes (cassette, key, size, wear, profile, clean zone): a fresh cache makes a cold frame, ten times a warm
 * one (B-06).
 *
 * The first instance can be built off the frame ([buildOffFrame], B-50): on a background thread from the frame
 * that first knows the slot, and taken in the next frame — finished there, or built then if the thread is late,
 * so the body always draws from that next frame and the same picture either way. Later rebuilds (a new size, a
 * focus's wear) stay in their frame, so a resizing window never shows a body missing.
 */
internal class HeldInstance {
    private var signature: List<Any?>? = null
    private var held: Built? = null
    private var pending: List<Any?>? = null

    /** Bumped when an instance built off the frame is taken; a draw cache reads it to be invalidated. */
    var arrivals: Int by mutableIntStateOf(0)
        private set

    /** No instance held yet: the body has not drawn. */
    val isEmpty: Boolean get() = held == null

    fun of(
        signature: List<Any?>,
        build: () -> Built,
    ): Built {
        val h = held
        if (h != null && signature == this.signature) return h
        this.signature = signature
        pending = null
        return build().also { held = it }
    }

    /**
     * Starts [build] on a background thread for [signature], once, and takes its result in the next frame, built there
     * if the thread is late. What the build made beside the instance goes to [taken] with it, whichever built it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <X> buildOffFrame(
        signature: List<Any?>,
        scope: CoroutineScope,
        taken: (Built, X) -> Unit,
        build: () -> Pair<Built, X>,
    ) {
        if (signature == pending) return
        pending = signature
        val off: Deferred<Pair<Built, X>> = scope.async(aheadDispatcher) { build() }
        // Undispatched: the wait for the next frame is registered in this one, so the result is taken in the next.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withFrameNanos { }
            if (pending != signature) return@launch off.cancel()
            val done = if (off.isCompleted && off.getCompletionExceptionOrNull() == null) off.getCompleted() else null
            if (done == null) off.cancel()
            val made = done ?: build()
            if (of(signature) { made.first } === made.first) taken(made.first, made.second)
            arrivals++
        }
    }
}

/** A tick's frame built ahead, with its image when one was asked for and the inputs that image was drawn with. */
internal class Ahead(
    val frame: BodyFrame,
    val image: RasterisedBody?,
    val inputs: List<Any?>?,
    /** B-110: the shader's brush, built ahead with the frame, for a body the shader draws. */
    val shaded: androidx.compose.ui.graphics.Brush? = null,
)

/** What one frame of a body is drawn with. */
internal data class BodyPicture(
    val paths: BodyPaths,
    val look: BodyLook,
    val decor: BodyDecor,
)

/**
 * A body rasterised at the device's pixels, and where its top-left pixel goes in the surface's own pixels. [whole] is
 * everything the body can ink, in the same pixels; the image covers all of it, or the part of it a band asked for (B-86).
 */
internal class RasterisedBody(
    val image: androidx.compose.ui.graphics.ImageBitmap,
    val offset: androidx.compose.ui.unit.IntOffset,
    val whole: Rect,
) {
    private val drawn =
        Rect(
            offset.x.toFloat(),
            offset.y.toFloat(),
            (offset.x + image.width).toFloat(),
            (offset.y + image.height).toFloat(),
        )

    /** Whether the image holds every pixel of the body inside [shown]; a null [shown] asks for the whole body. */
    fun covers(shown: Rect?): Boolean {
        val need = if (shown == null) whole else whole.intersect(shown)
        if (need.isEmpty) return true
        return need.left >= drawn.left && need.top >= drawn.top && need.right <= drawn.right &&
            need.bottom <= drawn.bottom
    }
}

/**
 * Draws [draw] (in the generator's scene coordinates) into an image covering everything a body can ink: its box,
 * the drips under it, the shadow, the slip, the outline's half width and the focus brackets (B-82). With a [band], in
 * the surface's pixels, only the rows and columns of that image inside it (B-86): the same pixels, cut on its grid.
 */
internal fun rasterisedBody(
    g: Instance,
    ox: Double,
    oy: Double,
    density: Float,
    direction: androidx.compose.ui.unit.LayoutDirection,
    band: Rect? = null,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
): RasterisedBody {
    val (left, top, right, bottom) = rasterBox(g)
    // On the surface's own pixel grid (B-115): the scene's point (ox, oy) is the surface's pixel (0, 0), and the image's
    // pixels are the surface's, so drawn at [at] nothing is resampled. Cut on whole scene units, a fractional density put
    // the image up to half a pixel off the body's vectors (2.9 % of pixels at 2.625).
    val ax = kotlin.math.floor((left - ox) * density).toInt()
    val ay = kotlin.math.floor((top - oy) * density).toInt()
    val w = kotlin.math.ceil((right - ox) * density).toInt() - ax
    val h = kotlin.math.ceil((bottom - oy) * density).toInt() - ay
    val at =
        androidx.compose.ui.unit
            .IntOffset(ax, ay)
    val whole = Rect(at.x.toFloat(), at.y.toFloat(), (at.x + w).toFloat(), (at.y + h).toFloat())
    // The band, on the whole image's pixel grid: whole pixels of it, so a cut image is the whole one's pixels.
    val x0 = band?.let { (kotlin.math.floor(it.left) - at.x).toInt().coerceIn(0, w) } ?: 0
    val y0 = band?.let { (kotlin.math.floor(it.top) - at.y).toInt().coerceIn(0, h) } ?: 0
    val x1 = band?.let { (kotlin.math.ceil(it.right) - at.x).toInt().coerceIn(x0, w) } ?: w
    val y1 = band?.let { (kotlin.math.ceil(it.bottom) - at.y).toInt().coerceIn(y0, h) } ?: h
    val image =
        androidx.compose.ui.graphics
            .ImageBitmap(maxOf(1, x1 - x0), maxOf(1, y1 - y0))
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit
            .Density(density),
        direction,
        androidx.compose.ui.graphics
            .Canvas(image),
        androidx.compose.ui.geometry
            .Size(image.width.toFloat(), image.height.toFloat()),
    ) {
        translate(-(x0 + ax).toFloat(), -(y0 + ay).toFloat()) {
            scale(density, Offset.Zero) { translate(-ox.toFloat(), -oy.toFloat()) { draw() } }
        }
    }
    return RasterisedBody(
        image,
        androidx.compose.ui.unit
            .IntOffset(at.x + x0, at.y + y0),
        whole,
    )
}

private const val RASTER_MARGIN = 20.0

/**
 * B-117: a body's still layers drawn once ([stillLayersImage]), kept for as long as its instance and the inputs of its
 * image are what they were. Built ahead off the frame's thread, so the three are held as one reference.
 */
internal class StillLayers {
    private var held: Triple<Instance, List<Any?>, StillImage>? = null

    fun of(
        g: Instance,
        inputs: List<Any?>,
        make: () -> StillImage,
    ): StillImage {
        val h = held
        if (h != null && h.first === g && h.second == inputs) return h.third
        return make().also { held = Triple(g, inputs, it) }
    }
}

/** Whether [surface] stands on whole pixels of the window, so an image on its grid is drawn without resampling (B-117). */
internal fun onWholePixels(surface: androidx.compose.ui.layout.LayoutCoordinates?): Boolean {
    if (surface == null || !surface.isAttached) return false
    val p = surface.localToWindow(Offset.Zero)
    return kotlin.math.abs(p.x - kotlin.math.round(p.x)) < 0.01f &&
        kotlin.math.abs(p.y - kotlin.math.round(p.y)) < 0.01f
}

/** What [rasterisedBody]'s image of [g] covers, in whole scene units. */
internal fun rasterBox(g: Instance): Rect {
    val el = g.slot
    return Rect(
        kotlin.math.floor(el.ox - RASTER_MARGIN).toFloat(),
        kotlin.math.floor(el.oy - RASTER_MARGIN - 4).toFloat(),
        kotlin.math.ceil(el.ox + el.w + RASTER_MARGIN).toFloat(),
        kotlin.math.ceil(el.oy + el.h + el.over + RASTER_MARGIN + 16).toFloat(),
    )
}

/**
 * The next resting tick's frame of one body, built on a background thread (B-75): [prepare] starts it for an
 * instance and a time, once; [take] hands it over only for that very instance and time, and only once it is done.
 */
internal class FrameAhead {
    private var instance: Instance? = null
    private var at = Double.NaN
    private var job: Deferred<Ahead>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun take(
        g: Instance,
        t: Double,
    ): Ahead? {
        val j = job ?: return null
        if (instance !== g || at != t || !j.isCompleted || j.getCompletionExceptionOrNull() != null) return null
        // A frame cut at its grid's edge is built again in the frame, where the grid grows (B-97).
        return j.getCompleted().takeIf { !it.frame.field.cut }
    }

    private var band: Rect? = null

    /**
     * Builds the frame for [t] once, with an image of the body around [shown] (B-86); again for the same [t] only when
     * the window has moved out of the band the first build was given.
     */
    fun prepare(
        g: Instance,
        t: Double,
        shown: Rect?,
        scope: CoroutineScope,
        build: (band: Rect?) -> Ahead,
    ) {
        val b = band
        val inside =
            (shown == null && b == null) ||
                (
                    shown != null && b != null && shown.left >= b.left && shown.top >= b.top &&
                        shown.right <= b.right && shown.bottom <= b.bottom
                )
        if (instance === g && at == t && inside) return
        job?.cancel()
        instance = g
        at = t
        val next = shown?.let { bandAround(it) }
        band = next
        job = scope.async(aheadDispatcher) { build(next) }
    }

    /** Hands over a frame built with the body's first instance, for [t] (B-83). */
    fun preset(
        g: Instance,
        t: Double,
        built: Ahead,
    ) {
        job?.cancel()
        instance = g
        at = t
        band = null
        job = CompletableDeferred(built)
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

    /** A mouse over the body (B-63): it simmers at [HOVER_HEAT], under a press's full boil. */
    var hovered: Boolean by mutableStateOf(false)
        private set
    private var from = 0.0
    private var since = 0.0

    private val target: Double get() =
        if (on) {
            1.0
        } else if (hovered) {
            HOVER_HEAT
        } else {
            0.0
        }

    /** The heat at [now]: rising towards the target at the press's rate, cooling towards it at the release's. */
    fun at(now: Double): Double {
        val t = target
        return if (from <= t) {
            minOf(t, from + (now - since) / MutantMotion.BOIL_OVER_IN_MS)
        } else {
            maxOf(t, from - (now - since) / MutantMotion.BOIL_OVER_OUT_MS)
        }
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

    fun hover(now: Double) {
        if (hovered) return
        from = at(now)
        since = now
        hovered = true
    }

    fun leave(now: Double) {
        if (!hovered) return
        from = at(now)
        since = now
        hovered = false
    }
}

/** A hovered body's heat (designed here, B-63): a simmer the eye catches, a third of a press's boil. */
internal const val HOVER_HEAT: Double = .3

/** A request to slide a body home from [dx] dp; a new [id] starts a new slide. */
internal class SlideRequest(
    val id: Any,
    val dx: Double,
    /** Down from the old place to the new one, dp: a calendar's day in another row (B-93). */
    val dy: Double = 0.0,
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
    var slideDy: Double = 0.0
        private set
    var slideSince: Double = Double.NEGATIVE_INFINITY
        private set

    fun follow(
        newFill: Color,
        slide: SlideRequest?,
        now: Double,
        h: Double? = null,
        steps: Boolean = false,
    ) {
        val old = fill
        if (old != null && old != newFill) {
            // A change back within a pour's first half pours from what still covers most of the body, the pour's own
            // old fill, not from the fill it was pouring in: a body never jumps to a colour it was not showing (B-94).
            val from = pourFrom
            val early = from != null && h != null && stepped(now - pourSince, pourDuration(h), steps) < .5
            pourFrom = if (early) from else old
            pourSince = now
        }
        fill = newFill
        if (slide != null && slide.id != slideId) {
            if (slideId != null || slideSince == Double.NEGATIVE_INFINITY) {
                slideDx = slide.dx
                slideDy = slide.dy
                slideSince = if (slideId == null) Double.NEGATIVE_INFINITY else now
            }
            slideId = slide.id
        }
    }
}
