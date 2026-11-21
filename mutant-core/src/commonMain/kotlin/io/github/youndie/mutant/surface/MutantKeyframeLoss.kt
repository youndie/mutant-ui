package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.MoshPlan
import io.github.youndie.mutant.gen.MoshRun
import io.github.youndie.mutant.gen.moshPlan
import io.github.youndie.mutant.theme.LocalMutantCassette
import io.github.youndie.mutant.theme.LocalMutantCleanSignal
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The keyframe loss over a screen (the engine's `data-mu-mosh`, B-48): every 9 s, 3.5 s into the window, a band of 9–18
 * rows of 16 dp breaks into blocks of 16 and 8 dp that drag the picture of the screen's bodies for 900–1400 ms, then
 * recovers from the top in 375 ms. The words stay as they are: the band lies under the content, as the engine's canvas
 * lies under the page's text, the bodies inside it giving way to it, and their clean zones are kept clean.
 *
 * The loss is made of the bodies of this screen's [MutantSurface]s, drawn over [ground] (the screen's colour, which this
 * paints in the band). It stops with the clock (reduce motion, a held or frozen screen) and under «Чистый сигнал». One
 * per screen: the engine runs one loss at a time.
 */
@Composable
public fun MutantKeyframeLoss(
    modifier: Modifier = Modifier,
    ground: Color = MutantTheme.colors.background,
    content: @Composable BoxScope.() -> Unit,
) {
    KeyframeLossBody(modifier, ground, null, content)
}

/**
 * [MutantKeyframeLoss]; [frozenAt] (a fixture's `data-mu-mosh-p`) starts the [first] loss at once and stands it at that
 * share of its run: 0.45 at 45 %, 1.2 into its recovery. [probe] becomes the scene's ([MoshScene.probe]).
 */
@Composable
internal fun KeyframeLossBody(
    modifier: Modifier,
    ground: Color,
    frozenAt: Double?,
    content: @Composable BoxScope.() -> Unit,
    first: Int = 1,
    probe: ((running: Boolean) -> Unit)? = null,
) {
    val scene = remember { MoshScene(first) }
    scene.probe = probe
    val clock = readMutantClock()
    val cassette = LocalMutantCassette.current
    val off = LocalMutantCleanSignal.current
    val reduce = LocalMutantReduceMotion.current
    scene.cassette = cassette
    scene.ground = ground
    scene.frozenAt = frozenAt
    scene.live = !off && frozenAt == null && !reduce && !clock.frozen && !clock.paused && clock.holds == 0
    scene.scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(
        modifier
            .onGloballyPositioned { scene.coords = it }
            .drawBehind { scene.drawUnder(this, clock) },
    ) {
        // The content in a layer of its own: a step of the loss redraws the band, not every body.
        Box(Modifier.graphicsLayer()) {
            CompositionLocalProvider(LocalMutantMoshScene provides if (off) null else scene) { content() }
        }
    }
}

/** The scene a [MutantSurface] gives its body to, and takes the band from: the nearest [MutantKeyframeLoss]. */
internal val LocalMutantMoshScene = staticCompositionLocalOf<MoshScene?> { null }

/** One body of the scene: where it stands, how it draws, and its clean zone (dp, its own: x, y, w, h). */
internal class MoshBody {
    var coords: LayoutCoordinates? = null
    var draw: (DrawScope.() -> Unit)? = null

    /**
     * How it draws into the scene's snapshot, where that differs from [draw]: the snapshot is a bitmap on the CPU, and a
     * body the shader draws would run it there pixel by pixel (seconds a body in the browser); it draws by paths instead.
     */
    var snapshot: (DrawScope.() -> Unit)? = null
    var zone: DoubleArray? = null

    /** What the scene's snapshot draws of it. */
    val inSnapshot: (DrawScope.() -> Unit)? get() = snapshot ?: draw
}

/** Registers a surface's body with the scene for as long as it is composed. */
@Composable
internal fun rememberMoshBody(scene: MoshScene?): MoshBody {
    val body = remember { MoshBody() }
    DisposableEffect(scene) {
        scene?.bodies?.add(body)
        onDispose { scene?.bodies?.remove(body) }
    }
    return body
}

/**
 * A body's drawing: given to the scene for its snapshot, and drawn with the band cut out while a loss runs — the
 * engine's canvas covers the bodies there, but for its cleared blocks: through them the body shows, under whatever a
 * block's drag has drawn into them since.
 */
internal fun CacheDrawScope.moshBehind(
    scene: MoshScene?,
    body: MoshBody,
    snapshot: (DrawScope.() -> Unit)? = null,
    block: DrawScope.() -> Unit,
): DrawResult =
    onDrawBehind {
        // Kept with or without a scene: a body off the screen draws it again rather than build a frame (B-87).
        body.draw = block
        body.snapshot = snapshot
        if (scene == null) return@onDrawBehind block()
        val cut = scene.cutIn(body.coords)
        if (cut == null) {
            block()
        } else {
            clipPath(cut, ClipOp.Difference) { block() }
            scene.drawHolesOver(this, body.coords)
        }
    }

/** The scene's loss: when it runs, its plan, its picture and where its band stands. */
@Stable
internal class MoshScene(
    private var count: Int,
) {
    var coords: LayoutCoordinates? = null
    val bodies = LinkedHashSet<MoshBody>()
    var cassette = ""
    var ground = Color.Transparent
    var frozenAt: Double? = null
    var live = false

    /** Where the bodies' pieces are painted ahead of a loss (B-159). */
    var scope: kotlinx.coroutines.CoroutineScope? = null

    // B-159: each body's picture for the next loss's snapshot, painted between frames ahead of it, and the window it is for.
    private val pieces = HashMap<MoshBody, BodyPiece>()
    private var piecesFor = -1L

    /** Told, at each draw under the content, whether a loss runs: for tests only, never for a consumer. */
    var probe: ((Boolean) -> Unit)? = null

    // The band in scene pixels, top and bottom; NaN while no loss runs. Bodies read it as they draw.
    private var bandTop by mutableFloatStateOf(Float.NaN)
    private var bandBottom by mutableFloatStateOf(Float.NaN)

    // Bumped to ask for another draw, while a frozen loss waits for its bodies to have drawn once.
    private var retry by mutableIntStateOf(0)
    private var window = -1L
    private var plan: MoshPlan? = null
    private var picture: LossPicture? = null
    private var clean: LossPicture? = null

    // The blocks cut out of the snapshot (`gone`), in scene pixels.
    private var holes: List<Rect> = emptyList()

    // Bumped at each step of the blocks, for the bodies that draw the band over themselves.
    private var drawn by mutableIntStateOf(0)
    private var t0 = 0.0
    private var steps = -1
    private var frozen = false
    private var tries = 0
    private val tinted = HashSet<MoshRun>()

    /**
     * What the band covers in [of]'s own pixels — the band less its cleared blocks, which the engine's canvas leaves
     * transparent over the bodies — or null while no loss runs.
     */
    fun cutIn(of: LayoutCoordinates?): Path? {
        val top = bandTop
        val bottom = bandBottom
        if (top.isNaN() || of == null) return null
        val at = coords?.offsetOf(of) ?: return null
        val band = Path().apply { addRect(Rect(-HUGE, top - at.y, HUGE, bottom - at.y)) }
        if (holes.isEmpty()) return band
        val cut = Path().apply { for (h in holes) addRect(h.translate(-at.x, -at.y)) }
        return Path.combine(PathOperation.Difference, band, cut)
    }

    /**
     * Over a body drawn through the cleared blocks, what the band holds there: nothing, where the body shows as it is, or
     * what a block's drag has drawn into them, over the body as on the engine's canvas.
     */
    fun drawHolesOver(
        scope: DrawScope,
        of: LayoutCoordinates?,
    ) {
        val pic = picture ?: return
        val top = bandTop
        if (holes.isEmpty() || top.isNaN() || of == null) return
        val at = coords?.offsetOf(of) ?: return
        // Only the blocks this body may ink, its drips' reach around it (B-88): a body under none of them is not drawn
        // again at every step.
        val reach = HOLE_REACH * scope.density
        val mine = Rect(at.x - reach, at.y - reach, at.x + of.size.width + reach, at.y + of.size.height + reach)
        val open = holes.filter { it.top >= top && it.overlaps(mine) }
        if (open.isEmpty()) return
        // Read so a step's drag into a block shows over the bodies too, not only under them.
        drawn
        val path = Path().apply { for (h in open) addRect(h.translate(-at.x, -at.y)) }
        scope.clipPath(path) { translate(-at.x, -at.y) { pic.draw(this) } }
    }

    /** Under the content: the band's picture while a loss runs; starts, steps and ends the loss by the clock. */
    fun drawUnder(
        scope: DrawScope,
        clock: MutantClock,
    ) {
        retry
        val p = frozenAt
        if (p != null) {
            if (plan == null && !frozen) startFrozen(scope, p)
        } else {
            val t = clock.restTimeMs()
            if (live) {
                if (lossBandInMemory && plan == null) prepareAhead(scope, t)
                val win = floor(t / EVERY_MS).toLong()
                if (plan == null && win != window && t - win * EVERY_MS > OFFSET_MS) {
                    window = win
                    start(scope, t)
                }
            } else if (plan != null) {
                end()
            }
            if (plan != null) advance(scope, ((t - t0) / STEP_MS).toInt())
        }
        probe?.invoke(plan != null)
        val pic = picture ?: return
        val top = bandTop
        if (top.isNaN()) return
        scope.clipRect(0f, top, scope.size.width, scope.size.height) { pic.draw(this) }
    }

    private fun start(
        scope: DrawScope,
        now: Double,
    ): Boolean {
        val me = coords ?: return false
        if (bodies.isEmpty() || bodies.any { it.draw == null }) return false
        val d = scope.density
        val w = (scope.size.width / d).toInt()
        val h = (scope.size.height / d).toInt()
        if (w < 16 || h < 16) return false
        val plan = moshPlan(cassette, count++, w, h)
        val pw = scope.size.width.roundToInt()
        val ph = scope.size.height.roundToInt()
        // B-88: kept as strips over the band and a row under it, where the blocks pull (at most 8 dp down); the bodies
        // draw straight into them.
        val bandFrom = floor(plan.y0 * d).toInt().coerceIn(0, ph)
        val bandTo = ceil((plan.y0 + (plan.rows + 1) * ROW) * d).toInt().coerceIn(0, ph)
        // B-158: in the browser the band's pixels are kept in this module's memory and handed to Skia once a step.
        val strips: LossPicture =
            if (lossBandInMemory) {
                BandPixels(pw, bandFrom, bandTo)
            } else {
                StripPicture(pw, bandFrom, bandTo, (ROW * d).roundToInt().coerceAtLeast(1))
            }
        if (strips is BandPixels) {
            paintBand(strips, scope, plan, me)
        } else {
            paintStrips(strips, scope, plan, me, ph)
        }
        this.plan = plan
        picture = strips
        clean = strips.copy()
        holes = plan.runs.filter { it.gone }.map { Rect(it.x * d, it.y * d, (it.x + it.w) * d, (it.y + it.h) * d) }
        t0 = now
        steps = -1
        tinted.clear()
        bandTop = plan.y0 * d
        bandBottom = (plan.y0 + plan.rows * ROW) * d
        return true
    }

    /** The snapshot as the strips take it: every body that meets the band drawn into them (B-88, B-154). */
    private fun paintStrips(
        strips: LossPicture,
        scope: DrawScope,
        plan: MoshPlan,
        me: LayoutCoordinates,
        ph: Int,
    ) {
        val d = scope.density
        strips.paint(Density(d, scope.fontScale), scope.layoutDirection, ph) {
            val y0 = plan.y0 * d
            val y1 = (plan.y0 + plan.rows * ROW) * d
            drawRect(ground, Offset(0f, y0), Size(size.width, y1 - y0))
            // B-154: a body whose reach misses the band draws nothing the band keeps: it is not drawn into the snapshot,
            // where drawing it built its frame by paths for nothing.
            val reach = HOLE_REACH * d
            for (b in bodies) {
                val c = b.coords ?: continue
                val at = me.offsetOf(c) ?: continue
                if (at.y - reach >= y1 || at.y + c.size.height + reach <= y0) continue
                val saved = drawContext.size
                drawContext.size = c.size.toSize()
                translate(at.x, at.y) { b.inSnapshot?.invoke(this) }
                drawContext.size = saved
            }
            // Only the band is the loss's: what the bodies drew above and below it is not.
            drawRect(Color.Black, Offset.Zero, Size(size.width, y0), blendMode = BlendMode.Clear)
            drawRect(Color.Black, Offset(0f, y1), Size(size.width, size.height - y1), blendMode = BlendMode.Clear)
            for (r in plan.runs) {
                if (r.gone) {
                    drawRect(
                        Color.Black,
                        Offset(r.x * d, r.y * d),
                        Size(r.w * d, r.h * d),
                        blendMode = BlendMode.Clear,
                    )
                }
            }
        }
    }

    /**
     * The snapshot in the band's memory (B-159): the ground, then each body that meets the band from the piece painted
     * for it ahead of the loss, or drawn now where none was; then the band alone kept, its cleared blocks cleared.
     */
    private fun paintBand(
        band: BandPixels,
        scope: DrawScope,
        plan: MoshPlan,
        me: LayoutCoordinates,
    ) {
        val d = scope.density
        val ph = scope.size.height.roundToInt()
        val y0 = plan.y0 * d
        val y1 = (plan.y0 + plan.rows * ROW) * d
        val reach = HOLE_REACH * d
        val meets =
            bodies.mapNotNull { b ->
                val c = b.coords ?: return@mapNotNull null
                val at = me.offsetOf(c) ?: return@mapNotNull null
                if (at.y - reach >= y1 || at.y + c.size.height + reach <= y0) null else Triple(b, c, at)
            }
        val missing = meets.filter { (b, _, _) -> pieces[b] == null }
        band.paint(Density(d, scope.fontScale), scope.layoutDirection, ph) {
            drawRect(ground, Offset(0f, y0), Size(size.width, y1 - y0))
            for ((b, c, at) in missing) {
                val saved = drawContext.size
                drawContext.size = c.size.toSize()
                translate(at.x, at.y) { b.inSnapshot?.invoke(this) }
                drawContext.size = saved
            }
        }
        for ((b, _, at) in meets) {
            val piece = pieces[b] ?: continue
            band.overlay(
                piece.px,
                piece.width,
                piece.height,
                at.x.roundToInt() - piece.reach,
                at.y.roundToInt() - piece.reach,
            )
        }
        pieces.clear()
        val clear =
            androidx.compose.ui.graphics
                .Paint()
                .apply { blendMode = BlendMode.Clear }
        band.drawRect(0f, 0f, scope.size.width, y0, clear)
        band.drawRect(0f, y1, scope.size.width, scope.size.height, clear)
        for (r in plan.runs) if (r.gone) band.drawRect(r.x * d, r.y * d, (r.x + r.w) * d, (r.y + r.h) * d, clear)
    }

    /**
     * B-159: from [LEAD_MS] before the next loss, the bodies its band may meet (with room for a scroll meanwhile) paint
     * their pictures for its snapshot, each a task of its own between frames, so the loss's first frame only lays them in.
     */
    private fun prepareAhead(
        ds: DrawScope,
        t: Double,
    ) {
        val win = floor(t / EVERY_MS).toLong()
        val next = if (win != window && t - win * EVERY_MS <= OFFSET_MS) win else win + 1
        if (next * EVERY_MS + OFFSET_MS - t > LEAD_MS || piecesFor == next) return
        val sc = scope ?: return
        val me = coords ?: return
        val d = ds.density
        val w = (ds.size.width / d).toInt()
        val h = (ds.size.height / d).toInt()
        if (w < 16 || h < 16) return
        piecesFor = next
        pieces.clear()
        val plan = moshPlan(cassette, count, w, h)
        val y0 = plan.y0 * d
        val y1 = (plan.y0 + plan.rows * ROW) * d
        val margin = (HOLE_REACH + SCROLL_ROOM) * d
        val density = Density(d, ds.fontScale)
        val direction = ds.layoutDirection
        for (b in bodies.toList()) {
            sc.launch(aheadDispatcher) {
                if (piecesFor != next || b !in bodies) return@launch
                val c = b.coords ?: return@launch
                val at = me.offsetOf(c) ?: return@launch
                if (at.y - margin >= y1 || at.y + c.size.height + margin <= y0) return@launch
                paintPiece(b, c, density, direction)?.let { pieces[b] = it }
            }
        }
    }

    private fun startFrozen(
        scope: DrawScope,
        p: Double,
    ) {
        if (!start(scope, 0.0)) {
            // Its bodies have not drawn yet: ask for another frame, a few times at most (a scene with none waits on).
            if (tries++ < MAX_TRIES) retry++
            return
        }
        val plan = plan ?: return
        val last = floor(p * plan.durationMs / STEP_MS).toInt()
        for (k in 0..last) {
            if (this.plan == null) break
            advance(scope, k)
        }
        frozen = true
    }

    /** Steps the loss to step [n] (125 ms each): blocks pull while it runs, then the band recovers from the top. */
    private fun advance(
        scope: DrawScope,
        n: Int,
    ) {
        val plan = plan ?: return
        val pic = picture ?: return
        val ref = clean ?: return
        if (n == steps) return
        steps = n
        val d = scope.density
        val el = n * STEP_MS
        if (el < plan.durationMs) {
            val u = el / plan.durationMs
            val paint =
                androidx.compose.ui.graphics
                    .Paint()
            for (r in plan.runs) {
                if (r.gone || u < r.start) continue
                pic.drawPart(
                    pic,
                    IntOffset((r.x * d).roundToInt(), (r.y * d).roundToInt()),
                    IntSize((r.w * d).roundToInt(), (r.h * d).roundToInt()),
                    IntOffset(((r.x + r.vx) * d).roundToInt(), ((r.y + r.vy) * d).roundToInt()),
                    paint,
                )
                if (r.tint >= 0 && tinted.add(r)) {
                    val tp =
                        androidx.compose.ui.graphics.Paint().apply {
                            color = TINTS[r.tint].copy(alpha = TINT_ALPHA)
                            blendMode = BlendMode.Screen
                        }
                    pic.drawRect(r.x * d, r.y * d, (r.x + r.w) * d, (r.y + r.h) * d, tp)
                }
            }
            restoreCleanZones(pic, ref, d, plan)
            drawn++
        } else {
            val k = ceil(plan.rows * min(1.0, (el - plan.durationMs + 1) / RECOVER_MS)).toInt()
            if (k >= plan.rows) {
                end()
                return
            }
            bandTop = (plan.y0 + k * ROW) * d
        }
    }

    /** The bodies' clean zones (and 3 dp around them) inside the band are the clean snapshot's, as the engine's are. */
    private fun restoreCleanZones(
        pic: LossPicture,
        ref: LossPicture,
        d: Float,
        plan: MoshPlan,
    ) {
        val me = coords ?: return
        val y0 = plan.y0 * d
        val y1 = (plan.y0 + plan.rows * ROW) * d
        val clear =
            androidx.compose.ui.graphics
                .Paint()
                .apply { blendMode = BlendMode.Clear }
        val copy =
            androidx.compose.ui.graphics
                .Paint()
        for (b in bodies) {
            val z = b.zone ?: continue
            if (z[2] <= 0) continue
            val c = b.coords ?: continue
            val at = me.offsetOf(c) ?: continue
            val x = at.x + ((z[0] - ZONE_MARGIN) * d).toFloat()
            val y = at.y + ((z[1] - ZONE_MARGIN) * d).toFloat()
            val w = ((z[2] + 2 * ZONE_MARGIN) * d).toFloat()
            val h = ((z[3] + 2 * ZONE_MARGIN) * d).toFloat()
            if (y + h <= y0 || y >= y1) continue
            pic.drawRect(x, y, x + w, y + h, clear)
            val off = IntOffset(x.roundToInt().coerceAtLeast(0), y.roundToInt().coerceAtLeast(0))
            val sz =
                IntSize(
                    w.roundToInt().coerceAtMost(ref.width - off.x),
                    h.roundToInt(),
                )
            if (sz.width > 0 && sz.height > 0) pic.drawPart(ref, off, sz, off, copy)
        }
    }

    private fun end() {
        plan = null
        picture = null
        clean = null
        holes = emptyList()
        bandTop = Float.NaN
        bandBottom = Float.NaN
    }
}

/** Every 9 s, 3.5 s into the window (`t / 9000`, `> 3500`); a step each 125 ms; the recovery 375 ms. */
private const val EVERY_MS = 9000.0
private const val OFFSET_MS = 3500.0
private const val STEP_MS = 125.0
private const val RECOVER_MS = 375.0

/** B-159: how long before a loss its bodies' pieces are painted, and how far a scroll may move them meanwhile, dp. */
private const val LEAD_MS = 1500.0
private const val SCROLL_ROOM = 400f

/** A body's picture for a loss's snapshot, painted ahead (B-159): its box and [reach] px around it. */
private class BodyPiece(
    val px: IntArray,
    val width: Int,
    val height: Int,
    val reach: Int,
)

private fun paintPiece(
    b: MoshBody,
    c: LayoutCoordinates,
    density: Density,
    direction: androidx.compose.ui.unit.LayoutDirection,
): BodyPiece? {
    val draw = b.inSnapshot ?: return null
    val reach = (HOLE_REACH * density.density).roundToInt()
    val w = c.size.width + 2 * reach
    val h = c.size.height + 2 * reach
    val image = ImageBitmap(w, h)
    CanvasDrawScope().draw(
        density,
        direction,
        androidx.compose.ui.graphics
            .Canvas(image),
        Size(w.toFloat(), h.toFloat()),
    ) {
        translate(reach.toFloat(), reach.toFloat()) {
            val saved = drawContext.size
            drawContext.size = c.size.toSize()
            draw()
            drawContext.size = saved
        }
    }
    return BodyPiece(IntArray(w * h).also { readImagePixels(image, it) }, w, h, reach)
}

/** How far past its box a body may ink, dp, for the cleared blocks drawn over it (as B-87's reach off the screen). */
private const val HOLE_REACH = 64f

/** A band's row (16 dp) and the margin kept around a clean zone (3 dp). */
private const val ROW = 16
private const val ZONE_MARGIN = 3.0

/** The tints a block may take once (`#ff2e93`, `#b6ff1a`, `#19e3ff`), screened at 0.3. */
private val TINTS = listOf(Color(0xFFFF2E93), Color(0xFFB6FF1A), Color(0xFF19E3FF))
private const val TINT_ALPHA = .3f

/** How many frames a frozen loss waits for its bodies to draw. */
private const val MAX_TRIES = 8

/** Far enough to cut the band across any body. */
private const val HUGE = 1e5f

/**
 * Where [of] stands in this scene's pixels, or null when the scene does not draw it: detached, or in another layout
 * hierarchy — a popup's (a menu, a tooltip), which reads the scene through the composition but floats over the screen
 * the loss covers (B-98).
 */
private fun LayoutCoordinates.offsetOf(of: LayoutCoordinates?): Offset? {
    if (of == null || !isAttached || !of.isAttached || rootOf(this) !== rootOf(of)) return null
    return localPositionOf(of, Offset.Zero)
}

private fun rootOf(c: LayoutCoordinates): LayoutCoordinates {
    var root = c
    while (true) root = root.parentCoordinates ?: return root
}
