package io.github.youndie.mutant.surface

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.findRootCoordinates
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.PourFront
import io.github.youndie.mutant.gen.jsRound
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.tokens.MutantMotion
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * The transitions between a body's looks (B-12), as the engine runs them: they step at 12 frames a second
 * (the hot tick, 83.33 ms), from the moment the event happened on the screen's clock, and land at once
 * under reduce motion, where the clock stands (D7).
 */

/** The transitions' step, ms: the hot tick. */
internal const val TRANSITION_STEP_MS: Double = 83.33

/**
 * A transition's progress after [elapsed] ms of [duration], at most 1: in whole steps where the theme's [steps] are on
 * (`steppedMotion`, the design's), else on every frame (B-100).
 */
internal fun stepped(
    elapsed: Double,
    duration: Double,
    steps: Boolean,
): Double =
    if (steps) {
        min(1.0, floor(max(0.0, elapsed) / TRANSITION_STEP_MS) * TRANSITION_STEP_MS / duration)
    } else {
        min(1.0, max(0.0, elapsed) / duration)
    }

/** How long a pour takes on a body [h] dp tall: `clamp(260 + 5 · h, 300, 620)` (tokens `motion.pour`). */
internal fun pourDuration(h: Double): Double = max(MutantMotion.POUR_MIN_MS, min(MutantMotion.POUR_MAX_MS, 260 + 5 * h))

/**
 * How far down a pour's front stands under the middle of a body's width, dp below its top: the median of its points
 * between 16 dp from each side, where a label stands, its drips included (B-94). Above it the new fill shows.
 */
internal fun PourFront.depthUnderLabel(
    ox: Double,
    oy: Double,
    w: Double,
): Double {
    val ys = ArrayList<Double>()
    var i = 0
    while (i + 1 < points.size) {
        val x = points[i] / 10.0 - ox
        if (x >= LABEL_INSET && x <= w - LABEL_INSET) ys += points[i + 1] / 10.0 - oy
        i += 2
    }
    if (ys.isEmpty()) return Double.NEGATIVE_INFINITY
    ys.sort()
    return ys[ys.size / 2]
}

/** Where a body's label starts from its side, dp: the chip's and the button's padding. */
private const val LABEL_INSET = 16.0

/** A pour front as a closed path: the front left to right, then down to the bottom and back. */
internal fun PourFront.toPath(): Path =
    Path().apply {
        moveTo(points[0] / 10f, points[1] / 10f)
        var i = 2
        while (i < points.size) {
            lineTo(points[i] / 10f, points[i + 1] / 10f)
            i += 2
        }
        lineTo(rightTenths / 10f, bottom.toFloat())
        lineTo(left.toFloat(), bottom.toFloat())
        close()
    }

/**
 * Draws [block] as a sliding body at progress [p] from [dx] dp away (the switch's thumb): eased home by
 * `1 − (1 − p)²` and stretched by `sin(πp)` — 40 % wider, 18 % flatter at the middle — about its centre, the
 * numbers rounded as the engine writes its transform. [sx] and [sy] are the scales' changes from 1 where a re-aimed run
 * gives them ([MutantSlideRun.stretchX]); null, the run's own.
 */
internal inline fun DrawScope.slid(
    g: Instance,
    p: Double,
    dx: Double,
    dy: Double = 0.0,
    sx: Double? = null,
    sy: Double? = null,
    block: DrawScope.() -> Unit,
) {
    if (p >= 1) return block()
    val e = 1 - (1 - p) * (1 - p)
    val q = sin(PI * p)
    val el = g.slot
    val cx = (el.ox + el.w / 2).toFloat()
    val cy = (el.oy + el.h / 2).toFloat()
    // Stretched along the run and squeezed across it: a run more down than across stretches tall (B-93).
    val along = jsToFixed(1 + .4 * q, 2).toFloat()
    val across = jsToFixed(1 - .18 * q, 2).toFloat()
    val down = kotlin.math.abs(dy) > kotlin.math.abs(dx)
    val scaleX = sx?.let { jsToFixed(1 + it, 2).toFloat() } ?: if (down) across else along
    val scaleY = sy?.let { jsToFixed(1 + it, 2).toFloat() } ?: if (down) along else across
    translate(jsToFixed(dx * (1 - e), 1).toFloat(), jsToFixed(dy * (1 - e), 1).toFloat()) {
        scale(scaleX, scaleY, Offset(cx, cy)) { block() }
    }
}

/**
 * An indicator melting in ([on]) or out (`melt`, `meltStep`): in 330 ms, stepped, it drops 7 dp from above while
 * stretched up to 1.9 times and is revealed from the top; out, it sinks and is eaten from the top, and is gone at
 * the end. Read from the screen's clock; the first state shows at once. Reversed while it runs, it retraces the motion
 * it was in from where it stands (B-171).
 */
internal fun Modifier.mutantMelt(on: Boolean): Modifier =
    composed {
        val clock = readMutantClock()
        val melt = remember { MeltFollower(on) }
        val instant = io.github.youndie.mutant.theme.LocalMutantReduceMotion.current
        val steps = io.github.youndie.mutant.theme.LocalMutantSteppedMotion.current
        melt.follow(on, clock.nowMs, instant, steps)
        val m = melt.at(clock.nowMs, instant, steps)
        val running = melt.running(m)
        // While it runs, the melt reads every frame, as the engine steps it; done, it reads nothing. Done is the
        // stepped progress at its end, which comes at 333.3 ms, not at the 330 ms duration (B-54).
        if (running) clock.frame
        // Done, the indicator stands as its target's rest: whole, or gone (the melt-out's end).
        val inward = if (running) melt.inward else on
        val q =
            when {
                !running -> if (on) 0.0 else 1.0
                inward -> 1 - m
                else -> m
            }
        this
            .graphicsLayer {
                transformOrigin = TransformOrigin(.5f, 0f)
                translationY = (jsToFixed((if (inward) -q else q) * 7, 1) * density).toFloat()
                scaleY = jsToFixed(1 + q * .9, 2).toFloat()
                alpha = if (!running && !on) 0f else 1f
            }.drawWithContent {
                if (q <= 0.0) return@drawWithContent drawContent()
                val inset = if (inward) jsRound(q * 100) / 100 else jsRound(q * 85) / 100
                if (inward) {
                    clipRect(bottom = size.height * (1 - inset.toFloat())) { this@drawWithContent.drawContent() }
                } else {
                    clipRect(top = size.height * inset.toFloat()) { this@drawWithContent.drawContent() }
                }
            }
    }

/**
 * Follows a melt's target from composition without writing state: a change starts the melt at that moment. A change
 * while a melt runs reverses it: the same motion runs back from where it stands, at the same pace, so the indicator
 * never jumps to the other motion's start (B-171). Under the pause the motion stands at its end (B-176).
 */
internal class MeltFollower(
    private var shown: Boolean,
) {
    /** Which motion is drawn: the melt-in's (revealed from the top, dropping) or the melt-out's (sinking, eaten). */
    var inward: Boolean = shown
        private set

    /** Whether the motion runs toward its end, or back toward its start after a reversal. */
    private var forward = true

    /** The motion's progress at [since]. */
    private var start = 0.0

    var since: Double = Double.NEGATIVE_INFINITY
        private set

    /** The motion's progress at [now], 0 at its start and 1 at its end; at its end at once under reduce motion. */
    fun at(
        now: Double,
        instant: Boolean,
        steps: Boolean,
    ): Double {
        if (instant) return if (forward) 1.0 else 0.0
        val s = stepped(now - since, MutantMotion.MELT_IN_MS, steps)
        return if (forward) min(1.0, start + s) else max(0.0, start - s)
    }

    /** Whether the motion at [m] is still under way. */
    fun running(m: Double): Boolean = if (forward) m < 1 else m > 0

    fun follow(
        on: Boolean,
        now: Double,
        instant: Boolean = false,
        steps: Boolean = false,
    ) {
        if (on != shown) {
            shown = on
            val m = at(now, instant, steps)
            if (running(m)) {
                forward = !forward
                start = m
            } else {
                inward = on
                forward = true
                start = 0.0
            }
            since = now
        }
        // B-176: the pause ends the motion where it is going (its end, or back at its start), so it has nothing to replay
        // when it lifts, and a change after it starts from rest.
        if (instant) since = Double.NEGATIVE_INFINITY
    }
}

/** The venom's goo (`venom`'s filter): a blur of σ 3.2 dp, then alpha · 18 − 8.2, so the joins round off. */
internal fun gooLayer(
    layer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    density: Float,
) {
    layer.renderEffect =
        androidx.compose.ui.graphics.BlurEffect(
            3.2f * density,
            3.2f * density,
            androidx.compose.ui.graphics.TileMode.Decal,
        )
    layer.colorFilter =
        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 18f, GOO_OFFSET),
            ),
        )
}

/** The alpha offset of the goo's matrix, in the 0…255 units Compose's [androidx.compose.ui.graphics.ColorMatrix] takes. */
internal const val GOO_OFFSET: Float = -8.2f * 255f

private fun io.github.youndie.mutant.gen.VenomPart.toPath(): Path =
    when (this) {
        is io.github.youndie.mutant.gen.VenomPart.Stroke -> {
            path.toPath(evenOdd = false)
        }

        is io.github.youndie.mutant.gen.VenomPart.Circle -> {
            Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        androidx.compose.ui.geometry
                            .Offset(cx.toFloat(), cy.toFloat()),
                        r.toFloat(),
                    ),
                )
            }
        }

        is io.github.youndie.mutant.gen.VenomPart.Rect -> {
            Path().apply {
                val rr = minOf(radius, width / 2, height / 2).toFloat()
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + width).toFloat(),
                        (y + height).toFloat(),
                        androidx.compose.ui.geometry
                            .CornerRadius(rr),
                    ),
                )
            }
        }
    }

/**
 * The venom at one moment, as `venom` layers it: under the pill, ink filled and stroked twice the outline; over
 * it, the fill. In each, the fat shapes go through the goo ([inkGoo], [fillGoo], set up by [gooLayer]) and the thin
 * ones are drawn sharp. Coordinates are the generator's.
 */
internal fun DrawScope.drawVenom(
    v: io.github.youndie.mutant.gen.VenomShapes,
    ink: androidx.compose.ui.graphics.Color,
    fill: androidx.compose.ui.graphics.Color,
    outline: Float,
    inkGoo: androidx.compose.ui.graphics.layer.GraphicsLayer,
    fillGoo: androidx.compose.ui.graphics.layer.GraphicsLayer,
    pill: DrawScope.() -> Unit = {},
) {
    drawVenomInk(v, ink, outline, inkGoo)
    pill()
    drawVenomFill(v, fill, fillGoo)
}

/** [this] in the generator's dp, scaled to pixels by [scale] (the density). */
private fun Path.scaled(scale: Float): Path =
    if (scale == 1f) {
        this
    } else {
        apply {
            transform(
                androidx.compose.ui.graphics
                    .Matrix()
                    .apply { scale(scale, scale) },
            )
        }
    }

/** [drawVenom]'s layer under the pill: the shapes in ink, filled and stroked twice [outline]. */
internal fun DrawScope.drawVenomInk(
    v: io.github.youndie.mutant.gen.VenomShapes,
    ink: androidx.compose.ui.graphics.Color,
    outline: Float,
    inkGoo: androidx.compose.ui.graphics.layer.GraphicsLayer,
    scale: Float = 1f,
) {
    val fat = v.fat.map { it.toPath().scaled(scale) }
    val thin = v.thin.map { it.toPath(evenOdd = false).scaled(scale) }
    val stroke =
        androidx.compose.ui.graphics.drawscope.Stroke(
            width = outline * 2 * scale,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
    inkGoo.record(
        size =
            androidx.compose.ui.unit
                .IntSize(size.width.toInt(), size.height.toInt()),
    ) {
        for (p in fat) {
            drawPath(p, ink)
            drawPath(p, ink, style = stroke)
        }
    }
    drawLayer(inkGoo)
    for (p in thin) {
        drawPath(p, ink)
        drawPath(p, ink, style = stroke)
    }
}

/** [drawVenom]'s layer over the pill: the shapes in the pill's fill. */
internal fun DrawScope.drawVenomFill(
    v: io.github.youndie.mutant.gen.VenomShapes,
    fill: androidx.compose.ui.graphics.Color,
    fillGoo: androidx.compose.ui.graphics.layer.GraphicsLayer,
    scale: Float = 1f,
) {
    val fat = v.fat.map { it.toPath().scaled(scale) }
    val thin = v.thin.map { it.toPath(evenOdd = false).scaled(scale) }
    fillGoo.record(
        size =
            androidx.compose.ui.unit
                .IntSize(size.width.toInt(), size.height.toInt()),
    ) {
        for (p in fat) drawPath(p, fill)
    }
    drawLayer(fillGoo)
    for (p in thin) drawPath(p, fill)
}

/**
 * The new pill's spring once the venom has filled it (`venom`): along the move it sinks by up to 9 % and comes
 * back damped over the last fifth, `0.09 · sin(2.5π · s) · (1 − s)` with `s = (p − 0.8) / 0.2`; across it, half
 * that the other way. Null while the pill is not yet full or the spring is under 0.004; the scales are rounded as
 * the engine writes them.
 */
internal fun venomSpring(
    p: Double,
    vertical: Boolean,
): Pair<Float, Float>? {
    if (p < .8) return null
    val sv = (p - .8) / .2
    val amp = .09 * sin(sv * PI * 2.5) * (1 - sv)
    if (kotlin.math.abs(amp) < .004) return null
    val along = jsToFixed(1 - amp, 3).toFloat()
    val across = jsToFixed(1 + amp * .5, 3).toFloat()
    return if (vertical) across to along else along to across
}

/*
 * B-179, the designer's answer 1 (research D24): a body tunes in as it appears and steps out as it leaves. Appearing is
 * `tune-in`, 320 ms in three steps — a 16 dp mosaic over the slot, an 8 dp mosaic, the finished body — and its words and
 * icons show only on the third; leaving is two steps over 160 ms, the 8 dp mosaic, then nothing. The steps are a
 * picture sequence, not a movement: they step whatever `steppedMotion` says (D22 is about movement). There is no alpha
 * anywhere. Under reduce motion, and on a clock that stands, the finished frame shows at once and leaving removes at once.
 * Built once here, for every body that appears: B-180's screens, B-181's switching on and B-185's dialog take it too.
 */

/** `tune-in`: 320 ms (tokens `motion`, the designer's answer 1), in three steps. */
internal const val TUNE_IN_MS: Double = 320.0
internal const val TUNE_IN_STEPS: Int = 3

/** Leaving: 160 ms in two steps, the 8 dp mosaic, then nothing (the designer's answer 1). */
internal const val TUNE_OUT_MS: Double = 160.0
internal const val TUNE_OUT_STEPS: Int = 2

/** The mosaics' blocks, dp: the first step's, and the second's (leaving's first). */
internal const val TUNE_COARSE_DP: Int = 16
internal const val TUNE_FINE_DP: Int = 8

/** What a tuning body shows at a moment. */
internal enum class MutantTuneLook {
    /** The 16 dp mosaic over the slot: tune-in's first step. */
    Coarse,

    /** The 8 dp mosaic: tune-in's second step, and leaving's first. */
    Fine,

    /** The finished body, its words and icons with it. */
    Whole,

    /** Nothing: leaving's second step, and gone. */
    Nothing,
}

/**
 * Follows whether a body is shown, from composition, without writing state (as [MeltFollower]): a change starts its
 * tune-in or its leaving at that moment on the screen's clock. Composed shown with [play], it tunes in from its first
 * frame; without, it stands finished (a fixture's still). Turned back half-way, it goes on from the picture it shows: a
 * leaving body still at its 8 dp mosaic tunes in again from that mosaic, and an appearing one leaves from the 8 dp
 * mosaic whatever step it stood at. [still] (reduce motion, a frozen, paused or held clock) shows the end at once.
 */
internal class MutantTune(
    visible: Boolean,
    now: Double,
    play: Boolean,
) {
    /** Whether the body is shown, or leaving or gone. */
    var shown: Boolean = visible
        private set

    /** When the current run began, its steps counted from here. */
    private var since: Double = if (visible && play) now else Double.NEGATIVE_INFINITY

    /** The picture the body shows, for [restartFor]: a new one tunes in again. */
    private var picture: Any? = Unset

    fun follow(
        visible: Boolean,
        now: Double,
        still: Boolean,
    ) {
        if (visible != shown) {
            val was = look(now, still)
            shown = visible
            since =
                when {
                    // Back in from leaving's 8 dp mosaic: tune-in from its own 8 dp step.
                    visible && was == MutantTuneLook.Fine -> now - TUNE_IN_MS / TUNE_IN_STEPS

                    else -> now
                }
        }
        if (still) since = Double.NEGATIVE_INFINITY
    }

    /**
     * A body that stays shown but takes a new [picture] (a snackbar's new message) tunes in again, its new words on the
     * third step (the designer's answer 1, F-snackbar-06). The first picture seen starts nothing.
     */
    fun restartFor(
        picture: Any?,
        now: Double,
        still: Boolean,
    ) {
        val was = this.picture
        this.picture = picture
        if (was !== Unset && was != picture && shown && !still) since = now
    }

    fun look(
        now: Double,
        still: Boolean,
    ): MutantTuneLook {
        if (still) return if (shown) MutantTuneLook.Whole else MutantTuneLook.Nothing
        val e = now - since
        return if (shown) {
            when {
                e >= TUNE_IN_MS * 2 / TUNE_IN_STEPS -> MutantTuneLook.Whole
                e >= TUNE_IN_MS / TUNE_IN_STEPS -> MutantTuneLook.Fine
                else -> MutantTuneLook.Coarse
            }
        } else if (e < TUNE_OUT_MS / TUNE_OUT_STEPS) {
            MutantTuneLook.Fine
        } else {
            MutantTuneLook.Nothing
        }
    }

    /** Whether a run is under way: the tune-in until its 320 ms, the leaving until its 160. */
    fun running(
        now: Double,
        still: Boolean,
    ): Boolean = !still && now - since < if (shown) TUNE_IN_MS else TUNE_OUT_MS

    /** Whether the body has left: hidden, and its 160 ms over (at once when [still]). */
    fun gone(
        now: Double,
        still: Boolean,
    ): Boolean = !shown && (still || now - since >= TUNE_OUT_MS)

    private object Unset
}

/**
 * The bodies a tuning host draws as mosaics: every [MutantSurface] composed inside it registers its slot here, and with
 * every host around it ([parent]): a host inside another that tunes in with it (a [MutantAppear] inside a
 * [MutantScreen], composed together) is drawn into the outer one's empty clip, so the outer host draws its bodies' mosaics
 * too (B-180).
 */
internal class MutantTuneBodies(
    private val parent: MutantTuneBodies? = null,
) {
    val bodies: MutableList<MutantTunedBody> = mutableListOf()

    /** Registers [body] here and with every host around this one. */
    fun add(body: MutantTunedBody) {
        bodies += body
        parent?.add(body)
    }

    fun remove(body: MutantTunedBody) {
        bodies -= body
        parent?.remove(body)
    }

    /** The host's own coordinates, which the slots are drawn in. */
    var host: androidx.compose.ui.layout.LayoutCoordinates? = null
}

/** The hosts' registry, provided by a tuning host to its content; null outside one. */
internal val LocalMutantTuneBodies =
    androidx.compose.runtime.staticCompositionLocalOf<MutantTuneBodies?> { null }

/** One body's slot as a tuning host draws it: where it stands, its corner, its fill and its seed. */
internal class MutantTunedBody {
    var coords: androidx.compose.ui.layout.LayoutCoordinates? = null
    var fill: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
    var radius: Double = 0.0
    var seed: String = ""

    private var laidFor: List<Any>? = null
    private var paints: IntArray = IntArray(0)

    /**
     * Each block's paint over a slot [cols] × [rows] blocks of [side] dp: an index into the body's fill and the
     * cocoon's two paints, from the stream `rng(<cassette>/<key>, tune<side>)`, laid out once per size.
     */
    fun paintsOf(
        cols: Int,
        rows: Int,
        side: Int,
    ): IntArray {
        val inputs = listOf(cols, rows, side, seed)
        if (inputs != laidFor) {
            laidFor = inputs
            val q =
                io.github.youndie.mutant.gen
                    .Mulberry32(seed, "tune$side")
            paints =
                IntArray(cols * rows) {
                    val r = q.next()
                    when {
                        r < TUNE_FILL_SHARE -> 0
                        r < TUNE_FILL_SHARE + (1 - TUNE_FILL_SHARE) / 2 -> 1
                        else -> 2
                    }
                }
        }
        return paints
    }
}

/** The share of a tuning mosaic's blocks in the body's own fill; the rest are the cocoon's two paints, half each. */
private const val TUNE_FILL_SHARE = .5

/**
 * Draws the mosaic of blocks of [side] dp over each registered body's slot, in the host's coordinates: the larger slots
 * first, so a body inside another shows its own mosaic over its container's.
 */
internal fun DrawScope.drawTuneMosaics(
    bodies: MutantTuneBodies,
    side: Int,
    colors: io.github.youndie.mutant.tokens.MutantColors,
) {
    val host = bodies.host?.takeIf { it.isAttached } ?: return
    val root = host.findRootCoordinates()
    val shown =
        bodies.bodies
            // A body in a popup (a tooltip's, a menu's) stands in another window: its own host draws it, not this one.
            .mapNotNull { b -> b.coords?.takeIf { it.isAttached && it.findRootCoordinates() == root }?.let { b to it } }
            .sortedByDescending { (_, c) -> c.size.width.toLong() * c.size.height }
    for ((b, c) in shown) {
        val at = host.localPositionOf(c, Offset.Zero)
        translate(at.x, at.y) {
            drawTuneMosaic(b, c.size.width / density, c.size.height / density, side, colors)
        }
    }
}

/**
 * Draws [body]'s mosaic of blocks of [side] dp over its slot, [w] × [h] dp from the origin, clipped to its rounded
 * corners: each block in the body's fill or one of the cocoon's two paints ([MutantTunedBody.paintsOf]).
 */
internal fun DrawScope.drawTuneMosaic(
    body: MutantTunedBody,
    w: Float,
    h: Float,
    side: Int,
    colors: io.github.youndie.mutant.tokens.MutantColors,
) {
    if (w <= 0f || h <= 0f) return
    val cols = kotlin.math.ceil(w / side).toInt()
    val rows = kotlin.math.ceil(h / side).toInt()
    val paints = body.paintsOf(cols, rows, side)
    val palette = arrayOf(body.fill, colors.cocoon1, colors.cocoon2)
    val r = minOf(body.radius.toFloat(), w / 2, h / 2)
    val clip =
        Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    0f,
                    0f,
                    w,
                    h,
                    androidx.compose.ui.geometry
                        .CornerRadius(r, r),
                ),
            )
        }
    // The fill under the whole slot, then each other paint's blocks as one path: blocks drawn one by one at a fractional
    // offset (a riding snackbar) left hairline seams between neighbours of one paint.
    val other = Array(palette.size) { Path() }
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val k = paints[y * cols + x]
            if (k == 0) continue
            other[k].addRect(
                androidx.compose.ui.geometry.Rect(
                    (x * side).toFloat(),
                    (y * side).toFloat(),
                    ((x + 1) * side).toFloat(),
                    ((y + 1) * side).toFloat(),
                ),
            )
        }
    }
    scale(density, Offset.Zero) {
        drawPath(clip, palette[0])
        clipPath(clip) {
            for (k in 1 until palette.size) drawPath(other[k], palette[k])
        }
    }
}
