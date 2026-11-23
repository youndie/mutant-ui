package io.github.youndie.mutant.surface

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.BodyFrame
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.REST_ROUGH
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.gen.frameSteps
import io.github.youndie.mutant.gen.pourFront
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantFill
import io.github.youndie.mutant.tokens.MutantGrid
import io.github.youndie.mutant.tokens.MutantProfile

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
    // B-156: kept across ticks, and given to the pool when the surface leaves, to be taken back by its next composition.
    val keep = remember { BodyKeep() }
    // B-160: a frame built in the frame, kept for the frames after it at the same tick (a slide, a pour, a boil).
    val inFrame = remember { FrameMemo() }
    keep.pooled = LocalMutantPoolBodies.current ?: !firstFrameWaits
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
    moves.follow(bodyFill, slide, clock.nowMs, drawnPlain[0]?.slot?.h, steps, instant)
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
                val from = moves.pourUnder
                val g = drawn.value
                val p = g?.let { stepped(now - moves.pourSince, pourDuration(it.slot.h), steps) } ?: 1.0
                // A pour asked for before the body first draws starts when it does: the old fill stands until then.
                val pouring = from != null && !instantNow && (g == null || p < 1)
                // A pour is drawn on every frame, and its front read on every frame with it.
                if (pouring) clock.frame
                val under =
                    if (pouring && g == null) {
                        from.base
                    } else if (pouring && g != null) {
                        val el = g.slot
                        val depth = MutantGenerator(cassetteNow).pourFront(g, p).depthUnderLabel(el.ox, el.oy, el.w)
                        // B-171: under the front, the picture the pour runs over, bands of an interrupted pour included.
                        if (depth < el.h / 2) from.underLabel(g) else fillNow
                    } else {
                        fillNow
                    }
                if (k <= 0 && !pouring) null else boilFill(under, colorsNow, k, clock.hotTimeMs())
            }
        }
    // B-149: whether this body's first frame waits for its build, and the slot and signature a size gives, at layout
    // or at draw.
    val waits = LocalMutantFirstFrameWaits.current ?: firstFrameWaits
    val tickWaits = waits && LocalMutantTickWaits.current
    val standInProbe = LocalMutantStandInProbe.current
    val densityNow = androidx.compose.ui.platform.LocalDensity.current.density

    fun slotOf(
        d: Float,
        widthPx: Float,
        heightPx: Float,
    ): Triple<Slot, List<Any?>, DoubleArray?>? {
        val zone = registry.union(d)
        val w = engineSize?.width?.toDouble() ?: (widthPx / d.toDouble())
        val h = engineSize?.height?.toDouble() ?: (heightPx / d.toDouble())
        if (w < 1 || h < 1) return null
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
        return Triple(slot, signature, tz)
    }
    Box(
        modifier
            .onGloballyPositioned {
                registry.surface = it
                moshBody.coords = it
                view.place(it, reach)
                // B-149: no instance yet, and a running clock: ask for the first build, started with the frame's others
                // by the first body drawn in it. A standing clock's first build also makes its image (B-83): in its draw.
                // B-156: a body the pool holds for this key is taken back in the draw instead, where its signature is
                // whole (the clean zones registered by then).
                val pooled = keep.pooled && BodyPool.holds(cassette, key)
                val standing = clock.paused || clock.holds > 0
                if (waits && offFrame && keep.held.isEmpty && !keep.held.starting && !standing && !pooled) {
                    val coords = it
                    FirstBuilds.ask {
                        if (!coords.isAttached || !keep.held.isEmpty) return@ask
                        val (slot, signature, _) =
                            slotOf(densityNow, coords.size.width.toFloat(), coords.size.height.toFloat()) ?: return@ask
                        val still = state == BodyState.Disabled || state == BodyState.Success
                        val warmAt = clock.nowMs
                        keep.held.startFirst(signature, scope) {
                            val gen = MutantGenerator(cassette)
                            val g = gen.instance(slot, key)
                            val built = Triple(g, FieldCache(), if (slot.dead) gen.deadPixels(g) else null)
                            if (!still) {
                                gen.frame(
                                    g,
                                    built.second,
                                    warmAt,
                                    0.0,
                                    pressed = false,
                                    steadyWebs = steadyWebs,
                                )
                            }
                            built
                        }
                    }
                }
            }
            // B-135: a layer of its own, so a tick records the body and its content and nothing beside it.
            .graphicsLayer()
            .drawWithCache {
                // An instance built off the frame arriving invalidates this cache.
                keep.held.arrivals
                // B-149: the frame's first builds start together, every clean zone of the frame registered by now.
                FirstBuilds.start()
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
                val gen = MutantGenerator(cassette)
                val (slot, signature, tz) = slotOf(d, size.width, size.height) ?: return@drawWithCache onDrawBehind {}
                moshBody.zone = tz
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

                // B-163: whether this body's decorations need its paths (B-131's list): its frames are then built with them,
                // ahead as well, so the frame never builds them itself.
                fun decorNeedsPaths(
                    g: Instance,
                    tt: Double,
                ): Boolean {
                    val decor = gen.decorOf(g, state, focused, 0.0, tt, colors)
                    return decor.runningMosaic != null || decor.errorBlocks.isNotEmpty() || decor.bands.isNotEmpty() ||
                        decor.errorOutline != null || decor.brackets != null
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
                            keep.stillLayers.of(
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
                    return keep.stillLayers.of(
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

                // B-166: what a body's picture is drawn with besides its frame — a brush built ahead bakes them in, and is
                // used only while they are still what it was built with: a chip chosen pours its new fill in, and drew
                // its old one again from a brush built ahead before it was chosen once the pour was over.
                fun lookOf(dead: io.github.youndie.mutant.gen.DeadPixels?): List<Any?> =
                    listOf(
                        outlineOverride?.invoke(key),
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

                // B-82: everything the image of a resting body depends on besides its frame. An image built ahead is
                // used only if these are still what they were when it was prepared.
                fun imageInputsOf(dead: io.github.youndie.mutant.gen.DeadPixels?): List<Any?>? =
                    if (bodyAsImage) lookOf(dead) else null

                // A resting frame at [next], built off the frame, with its image when the body is drawn as one (B-75, B-82).
                fun aheadOf(
                    g: Instance,
                    dead: io.github.youndie.mutant.gen.DeadPixels?,
                    next: Double,
                    band: Rect?,
                    f: io.github.youndie.mutant.gen.BodyFrame,
                ): Ahead {
                    // B-110: a body the shader draws gets its brush ahead, with its frame, and no image.
                    if (onGpu) {
                        return Ahead(
                            f,
                            null,
                            lookOf(dead),
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

                fun buildAhead(
                    g: Instance,
                    cache: FieldCache,
                    dead: io.github.youndie.mutant.gen.DeadPixels?,
                    next: Double,
                    band: Rect?,
                    grow: Boolean = false,
                ): Ahead {
                    // Built off the frame's thread: the field's grid is not grown here (B-97), but for a body's first
                    // frame in the browser, whose one thread is the frame's too (B-153).
                    val f =
                        gen.frame(
                            g,
                            cache,
                            next,
                            0.0,
                            pressed = false,
                            steadyWebs = steadyWebs,
                            grow = grow,
                            paths = !onGpu || decorNeedsPaths(g, next),
                        )
                    return aheadOf(g, dead, next, band, f)
                }

                // B-161: in the browser the frame's field is built a few columns at a time, the thread let go to the
                // page's frames every few milliseconds, so a build between two frames does not hold the next one up.
                suspend fun buildAheadInSteps(
                    g: Instance,
                    cache: FieldCache,
                    dead: io.github.youndie.mutant.gen.DeadPixels?,
                    next: Double,
                    band: Rect?,
                    grow: Boolean,
                ): Ahead {
                    val steps =
                        gen.frameSteps(
                            g,
                            cache,
                            next,
                            0.0,
                            false,
                            steadyWebs,
                            grow,
                            !onGpu || decorNeedsPaths(g, next),
                        )
                    inSteps { steps.columns(it) }
                    return aheadOf(g, dead, next, band, steps.frame())
                }
                // B-156: a body this signature left in the pool is taken back, with its last frame: nothing to build.
                if (keep.held.isEmpty) keep.restore(signature)
                // B-149: a first build started for this frame is taken here, waited for.
                if (offFrame && keep.held.isEmpty && waits) keep.held.takeFirst(signature)
                if (offFrame && keep.held.isEmpty) {
                    // The static layers are what makes a first frame ten times a warm one: where the frame cannot wait
                    // for another thread (the browser), they are built there with the instance and the body is drawn
                    // from the frame that takes them, a stand-in until then (B-50, B-149).
                    val warmAt = clock.nowMs
                    // B-83: a clock that stands (held by a dialog, paused) has no next tick, so a body under it would
                    // never be given an image and would draw its vectors on every frame something else asks for. Its
                    // first build also rasterises it for the time it stands at, and the body draws that image from its
                    // first frame: nothing switches under the stopped clock.
                    val firstAt = clock.restTimeMs()
                    val firstImage =
                        bodyAsImage && !still && heat == null && !boil.on && (clock.paused || clock.holds > 0)
                    val firstOf = {
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
                    if (waits) {
                        // Nothing was started for this frame (a standing clock, a slot changed since layout): built here.
                        val (built, first) = firstOf()
                        keep.held.of(signature) { built }
                        first?.let { keep.ahead.preset(built.first, firstAt, it) }
                    } else {
                        // B-153: in the browser the first build is a task of its own between frames, never the frame's,
                        // and it is the whole first picture: the frame of the tick it is drawn at, with its brush or its
                        // image, so the frame that takes it builds nothing.
                        keep.held.buildOffFrame(
                            signature,
                            scope,
                            taken = { built, first: Ahead? ->
                                first?.let { keep.ahead.preset(built.first, firstAt, it) }
                            },
                        ) {
                            val built = build()
                            val (g, cache, dead) = built
                            built to if (still) null else buildAheadInSteps(g, cache, dead, firstAt, null, grow = true)
                        }
                        standInProbe?.invoke()
                        return@drawWithCache onDrawBehind { drawStandIn(slot, look, d) }
                    }
                }
                // B-162: in the browser a body whose slot changed (a text zone, a size settled once its font came) goes
                // on drawing the instance it has while the new one is built between frames, never in the frame.
                val held = keep.held.current
                val later =
                    !waits && offFrame && held != null && keep.held.signatureNow != signature &&
                        !keep.hasRecent(signature)
                if (later) {
                    val at = clock.restTimeMs()
                    keep.held.buildOffFrame(
                        signature,
                        scope,
                        taken = { built, first: Ahead? -> first?.let { keep.ahead.preset(built.first, at, it) } },
                    ) {
                        val built = build()
                        val (bg, bcache, bdead) = built
                        built to if (still) null else buildAheadInSteps(bg, bcache, bdead, at, null, grow = true)
                    }
                }
                val (g, cache, dead) = if (later) held else keep.of(signature, build)
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
                // B-153, B-155: in the browser the latest frame done is drawn, a tick or more behind, rather than one built here.
                val early = if (resting) keep.ahead.take(g, t, orEarlier = !waits, wait = tickWaits) else null

                var frame =
                    early?.frame
                        ?: inFrame.of(g, t, k, boil.on, !onGpu) {
                            gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs, paths = !onGpu)
                        }
                // B-86: what the window shows of the body now, in the surface's pixels; an image covering less is not drawn.
                val shown = registry.surface?.takeIf { it.isAttached }?.let { windowIn(it) }
                // B-155: in the browser the next tick is built ahead from the first, its cache filled there: the one thread
                // is the frame's, so nothing reads the cache while the build writes it.
                if (resting && (cache.settledFor(g) || !waits)) {
                    // B-89: the next tick's time as the clock will read it, so the frame built for it is found.
                    val next = clock.nextRestTimeMs()
                    keep.ahead.prepare(g, next, shown, scope) { band ->
                        if (waits) {
                            buildAhead(
                                g,
                                cache,
                                dead,
                                next,
                                band,
                            )
                        } else {
                            buildAheadInSteps(g, cache, dead, next, band, true)
                        }
                    }
                }
                val now = clock.nowMs
                val pourP =
                    pourAt?.first ?: if (instant) 1.0 else stepped(now - moves.pourSince, pourDuration(slot.h), steps)
                val pourUnder = pourAt?.second?.let { MutantPourUnder(it) } ?: moves.pourUnder
                val run = moves.slideRun
                val slideP = slideAt?.first ?: run.progress(now, instant, steps)
                val slideDx = slideAt?.second ?: run.dx
                val slideDy = if (slideAt != null) 0.0 else run.dy
                // B-171: a re-aimed run's stretch, eased from what it showed; the engine's own where nothing was carried.
                val slideSx = if (slideAt != null || slideP >= 1) null else run.stretchX(slideP)
                val slideSy = if (slideAt != null || slideP >= 1) null else run.stretchY(slideP)
                // A running transition is evaluated on every frame, as the engine does, so its steps keep to the event.
                if (pourP < 1 || slideP < 1) clock.frame
                val pour =
                    if (pourP < 1 && pourUnder != null) {
                        BodyPour(
                            gen.pourFront(g, pourP).toPath(),
                            pourUnder.base,
                            // A band the front has passed is covered; drawn, its edge would leave a hairline seam.
                            pourUnder.bands.filter { it.p > pourP }.map { it.path(g) to it.color },
                        )
                    } else {
                        null
                    }
                // B-110: a pouring or sliding body is drawn by paths, as before.
                if (onGpu && (pour != null || slideP < 1)) {
                    frame =
                        inFrame.of(g, t, k, boil.on, true) {
                            gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs)
                        }
                } else if (onGpu) {
                    val shaded =
                        early?.shaded.takeIf { early?.frame === frame && early.inputs == lookOf(dead) }
                            ?: shaderOf(g, frame, dead, t, k)
                    if (shaded != null) {
                        view.drawn = null
                        frameProbe?.invoke(early != null, false, frame.steadyWebs)
                        val ox = slot.ox.toFloat()
                        val oy = slot.oy.toFloat()
                        // B-133: the keyframe loss's snapshot is a CPU bitmap, where the shader would run pixel by pixel;
                        // there the body draws by paths, built only when a loss takes its snapshot.
                        // B-154: the snapshot draws each of its strips with this, so the frame is built once for all of them.
                        val byPathsPicture by lazy {
                            pictureOf(g, gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs), t, k)
                        }
                        val byPaths: DrawScope.() -> Unit = {
                            val (pp, pl, pd) = byPathsPicture
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
                    // B-163: a frame built ahead with its paths is drawn as it is.
                    frame =
                        early?.frame?.takeIf { early.frame === frame && !it.contour.isEmpty }
                            ?: inFrame.of(g, t, k, boil.on, true) {
                                gen.frame(g, cache, t, k, pressed = boil.on, steadyWebs = steadyWebs)
                            }
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
                                slideSx,
                                slideSy,
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

/** The print theme's default slip, dp (tokens `genome.slip`: «тема «Копия» 1.5»). */
private const val PRINT_SLIP: Double = 1.5

/** What one frame of a body is drawn with. */
internal data class BodyPicture(
    val paths: BodyPaths,
    val look: BodyLook,
    val decor: BodyDecor,
)

/** A focused body's wear (`data-calm=".3"`: «повреждения 30 %»). */
private const val FOCUS_CALM: Double = .3
