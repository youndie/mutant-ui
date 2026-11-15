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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntSize
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
import kotlinx.coroutines.Dispatchers
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
 * Whether a running clock builds a body's first instance off the frame (B-50). Off only for a test or a measurement
 * that compares with building it in the frame, as before.
 */
internal val LocalMutantBuildOffFrame = staticCompositionLocalOf { true }

/**
 * Told, for every body frame a surface draws, whether it was built ahead off the frame (B-75) and whether it is
 * drawn from an image rasterised ahead (B-82): a test reads it.
 */
internal val LocalMutantFrameProbe =
    staticCompositionLocalOf<
        (
            (
                fromAhead: Boolean,
                asImage: Boolean,
            ) -> Unit
        )?,
    > { null }

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
    val ahead = remember { FrameAhead() }
    val frameProbe = LocalMutantFrameProbe.current
    val skipParts = LocalMutantSkipBodyParts.current
    val bodyAsImage = LocalMutantBodyAsImage.current
    val rasterProbe = LocalMutantRasterProbe.current
    val scope = rememberCoroutineScope()
    // A body's first instance is built off the frame (B-50), unless the clock is frozen (fixtures, references) or an
    // IDE preview draws it: those draw in their first frame, as before.
    val offFrame = LocalMutantBuildOffFrame.current && !clock.frozen && !LocalInspectionMode.current
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
                // An instance built off the frame arriving invalidates this cache.
                held.arrivals
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
                ): Ahead {
                    val f = gen.frame(g, cache, next, 0.0, pressed = false)
                    val inputs = imageInputsOf(dead)
                    val image =
                        inputs?.let {
                            val t0 =
                                kotlin.time.TimeSource.Monotonic
                                    .markNow()
                            val pic = pictureOf(g, f, next, 0.0)
                            rasterisedBody(g, slot.ox, slot.oy, d, layoutDirection) {
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
                                firstImage -> buildAhead(g, cache, dead, firstAt)
                                !still -> null.also { gen.frame(g, cache, warmAt, 0.0, pressed = false) }
                                else -> null
                            }
                        built to first
                    }
                    return@drawWithCache onDrawBehind {}
                }
                val (g, cache, dead) = held.of(signature, build)
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
                                    // A Success body is still a control: its keyboard focus shows (B-57).
                                    if (focused) drawBrackets(g, colors.focus)
                                }
                            }
                        }
                    }
                }
                val hot = heat?.let { it > 0 } ?: (boil.on || boil.at(clock.nowMs) > 0)
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

                val frame = early?.frame ?: gen.frame(g, cache, t, k, pressed = boil.on)
                if (resting && cache.settledFor(g)) {
                    val next = t + clock.stepMs
                    ahead.prepare(g, next, scope) { buildAhead(g, cache, dead, next) }
                }
                val now = clock.nowMs
                val pourP = pourAt?.first ?: if (instant) 1.0 else stepped(now - moves.pourSince, pourDuration(slot.h))
                val pourFrom = pourAt?.second ?: moves.pourFrom
                val slideP =
                    slideAt?.first ?: if (instant) 1.0 else stepped(now - moves.slideSince, MutantMotion.SLIDE_MS)
                val slideDx = slideAt?.second ?: moves.slideDx
                // A running transition is evaluated on every frame, as the engine does, so its steps keep to the event.
                if (pourP < 1 || slideP < 1) clock.frame
                val pour = if (pourP < 1 && pourFrom != null) gen.pourFront(g, pourP).toPath() to pourFrom else null
                // B-82: a resting body's picture changes only at a tick. Rasterised ahead with its frame, the image is
                // drawn until the next tick; a pouring, sliding or heated body, or one whose inputs moved since, draws
                // as vectors.
                val imageInputs = imageInputsOf(dead)
                val image =
                    early?.image?.takeIf {
                        imageInputs != null && early.inputs == imageInputs && pour == null &&
                            slideP >= 1
                    }
                frameProbe?.invoke(early != null, image != null)
                if (image != null) {
                    return@drawWithCache onDrawBehind {
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
                onDrawBehind {
                    scale(d, Offset.Zero) {
                        translate(-slot.ox.toFloat(), -slot.oy.toFloat()) {
                            slid(
                                g,
                                slideP,
                                slideDx,
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
                                )
                            }
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

    /** How many times the instance was built: a test reads it. */
    var builds: Int = 0
        private set

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
        builds++
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
        val off: Deferred<Pair<Built, X>> = scope.async(Dispatchers.Default) { build() }
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
)

/** What one frame of a body is drawn with. */
internal data class BodyPicture(
    val paths: BodyPaths,
    val look: BodyLook,
    val decor: BodyDecor,
)

/**
 * Whether a resting body under a running clock is rasterised once per tick, ahead with its frame, and drawn as that
 * image until the next tick (B-82). Off only for a test or a measurement comparing it with drawing the vectors.
 */
internal val LocalMutantBodyAsImage = staticCompositionLocalOf { true }

/** Told the time and the bytes of every body rasterised (B-82): the bench reads it. */
internal val LocalMutantRasterProbe = staticCompositionLocalOf<((nanos: Long, bytes: Long) -> Unit)?> { null }

/** A body rasterised at the device's pixels, and where its top-left pixel goes in the surface's own pixels. */
internal class RasterisedBody(
    val image: androidx.compose.ui.graphics.ImageBitmap,
    val offset: androidx.compose.ui.unit.IntOffset,
)

/**
 * Draws [draw] (in the generator's scene coordinates) into an image covering everything a body can ink: its box,
 * the drips under it, the shadow, the slip, the outline's half width and the focus brackets (B-82).
 */
internal fun rasterisedBody(
    g: Instance,
    ox: Double,
    oy: Double,
    density: Float,
    direction: androidx.compose.ui.unit.LayoutDirection,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
): RasterisedBody {
    val el = g.slot
    val left = kotlin.math.floor(el.ox - RASTER_MARGIN).toFloat()
    val top = kotlin.math.floor(el.oy - RASTER_MARGIN - 4).toFloat()
    val right = kotlin.math.ceil(el.ox + el.w + RASTER_MARGIN).toFloat()
    val bottom = kotlin.math.ceil(el.oy + el.h + el.over + RASTER_MARGIN + 16).toFloat()
    val w = kotlin.math.ceil((right - left) * density).toInt()
    val h = kotlin.math.ceil((bottom - top) * density).toInt()
    val image =
        androidx.compose.ui.graphics
            .ImageBitmap(w, h)
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit
            .Density(density),
        direction,
        androidx.compose.ui.graphics
            .Canvas(image),
        androidx.compose.ui.geometry
            .Size(w.toFloat(), h.toFloat()),
    ) {
        scale(density, Offset.Zero) { translate(-left, -top) { draw() } }
    }
    val offset =
        androidx.compose.ui.unit.IntOffset(
            kotlin.math.round((left - ox) * density).toInt(),
            kotlin.math.round((top - oy) * density).toInt(),
        )
    return RasterisedBody(image, offset)
}

private const val RASTER_MARGIN = 20.0

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
        return j.getCompleted()
    }

    fun prepare(
        g: Instance,
        t: Double,
        scope: CoroutineScope,
        build: () -> Ahead,
    ) {
        if (instance === g && at == t) return
        job?.cancel()
        instance = g
        at = t
        job = scope.async(Dispatchers.Default) { build() }
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
