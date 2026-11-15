package io.github.youndie.mutant.surface

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
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

/** A transition's progress after [elapsed] ms of [duration]: whole steps only, at most 1. */
internal fun stepped(
    elapsed: Double,
    duration: Double,
): Double = min(1.0, floor(max(0.0, elapsed) / TRANSITION_STEP_MS) * TRANSITION_STEP_MS / duration)

/** How long a pour takes on a body [h] dp tall: `clamp(260 + 5 · h, 300, 620)` (tokens `motion.pour`). */
internal fun pourDuration(h: Double): Double = max(MutantMotion.POUR_MIN_MS, min(MutantMotion.POUR_MAX_MS, 260 + 5 * h))

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
 * numbers rounded as the engine writes its transform.
 */
internal inline fun DrawScope.slid(
    g: Instance,
    p: Double,
    dx: Double,
    block: DrawScope.() -> Unit,
) {
    if (p >= 1) return block()
    val e = 1 - (1 - p) * (1 - p)
    val q = sin(PI * p)
    val el = g.slot
    val cx = (el.ox + el.w / 2).toFloat()
    val cy = (el.oy + el.h / 2).toFloat()
    translate(jsToFixed(dx * (1 - e), 1).toFloat(), 0f) {
        scale(jsToFixed(1 + .4 * q, 2).toFloat(), jsToFixed(1 - .18 * q, 2).toFloat(), Offset(cx, cy)) { block() }
    }
}

/**
 * An indicator melting in ([on]) or out (`melt`, `meltStep`): in 330 ms, stepped, it drops 7 dp from above while
 * stretched up to 1.9 times and is revealed from the top; out, it sinks and is eaten from the top, and is gone at
 * the end. Read from the screen's clock; the first state shows at once.
 */
internal fun Modifier.mutantMelt(on: Boolean): Modifier =
    composed {
        val clock = readMutantClock()
        val melt = remember { MeltFollower(on) }
        melt.follow(on, clock.nowMs)
        val instant = io.github.youndie.mutant.theme.LocalMutantReduceMotion.current
        val elapsed = if (instant) Double.POSITIVE_INFINITY else clock.nowMs - melt.since
        val p = stepped(elapsed, MutantMotion.MELT_IN_MS)
        // While it runs, the melt reads every frame, as the engine steps it; done, it reads nothing. Done is the
        // stepped progress at 1, which comes at 333.3 ms, not at the 330 ms duration (B-54).
        if (p < 1) clock.frame
        val q = if (on) 1 - p else p
        this
            .graphicsLayer {
                transformOrigin = TransformOrigin(.5f, 0f)
                translationY = (jsToFixed((if (on) -q else q) * 7, 1) * density).toFloat()
                scaleY = jsToFixed(1 + q * .9, 2).toFloat()
                alpha = if (!on && p >= 1) 0f else 1f
            }.drawWithContent {
                if (q <= 0.0) return@drawWithContent drawContent()
                val inset = if (on) jsRound(q * 100) / 100 else jsRound(q * 85) / 100
                if (on) {
                    clipRect(bottom = size.height * (1 - inset.toFloat())) { this@drawWithContent.drawContent() }
                } else {
                    clipRect(top = size.height * inset.toFloat()) { this@drawWithContent.drawContent() }
                }
            }
    }

/** Follows a melt's target from composition without writing state: a change starts the melt at that moment. */
internal class MeltFollower(
    private var shown: Boolean,
) {
    var since: Double = Double.NEGATIVE_INFINITY
        private set

    fun follow(
        on: Boolean,
        now: Double,
    ) {
        if (on == shown) return
        shown = on
        since = now
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
