package io.github.youndie.mutant.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Pull-to-refresh (v1): a tongue of slime drawn out of the header above. Pulled down from the top of its [content], a
 * `btn` tongue grows from under the top edge — the page's short one (64 × 10, drips of 14) at 40 dp, its long one
 * (124 × 34, drips of 24 that let drops go) at 74 dp — and the content moves down with it. Let go past 74 dp, it calls
 * [onRefresh]; while [refreshing], the long tongue boils with [refreshingLabel] in it. When refreshing ends, a clean
 * `pill` with [doneLabel] stands for a second, then rides back up with the content.
 *
 * The design draws three frames; the gesture and the moves between them are designed here (D15): a finger's drag past
 * the content's top, through nested scroll, and a mouse's drag from wherever the content stands at its top, half the
 * distance dragged. Outside a drag the content glides, every frame of the screen clock, from where it stands to the
 * phase's gap — back up when let go short, to 74 dp when refreshing, to the pill's 52, back up after it — in 300 ms,
 * eased out. Under reduce motion every move is at once and the boil stands. A reader's custom action refreshes. Over its content stands the header's tear line (the tracking divider, [divider]), drawn over the
 * tongue as the page draws it: the tongue hangs 2 dp up into it.
 */
@Composable
public fun MutantPullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshingLabel: String = "ОБНОВЛЯЕМ",
    doneLabel: String = "ОБНОВЛЕНО",
    key: String = "ptr",
    divider: Boolean = true,
    content: @Composable () -> Unit,
) {
    PullToRefreshBody(refreshing, onRefresh, modifier, refreshingLabel, doneLabel, key, content, divider = divider)
}

/** [MutantPullToRefresh]; a fixture stands it at [pull], at [done], or at [heat], and [hidden] hides its words. */
@Composable
internal fun PullToRefreshBody(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    refreshingLabel: String,
    doneLabel: String,
    key: String,
    content: @Composable () -> Unit,
    divider: Boolean = true,
    pull: Float? = null,
    done: Boolean = false,
    heat: Double? = null,
    hidden: Boolean = false,
) {
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val slop = LocalViewConfiguration.current.touchSlop
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val refresh by rememberUpdatedState(onRefresh)
    // The gap the content stands under, dp, while held: where the pull took it up, plus the finger's half.
    var dragged by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var atTop by remember { mutableStateOf(true) }
    val still = reduce || clock.frozen
    // Outside a drag the gap glides on the screen clock; a screen composed refreshing stands open from its first frame.
    val glide = remember { PullGlide(if (refreshing) REFRESH_AT else 0f) }
    glide.follow(refreshing, clock.nowMs)
    // Read by the gestures, which are set up once: always this composition's.
    val refreshingNow by rememberUpdatedState(refreshing)
    val stillNow by rememberUpdatedState(still)
    // The pill's second is the screen clock's; under reduce motion that clock stands, and the second is waited out.
    var waited by remember { mutableIntStateOf(0) }
    LaunchedEffect(glide.dones, still) {
        if (still && glide.pillStands()) {
            delay(DONE_MS.toLong())
            glide.endPill()
            waited++
        }
    }
    val doneShown = waited.let { glide.pill(clock.nowMs, still) }
    val boil = remember { MutableInteractionSource() }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            boil.emit(
                PressInteraction.Press(Offset.Zero),
            )
        } else {
            boil.emit(PressInteraction.Release(PressInteraction.Press(Offset.Zero)))
        }
    }

    /** Where the content stands now, dp: a new pull takes it up from there, never from 0. */
    fun standing(): Float = if (holding) dragged else glide.at(clock.nowMs, stillNow)

    fun letGo() {
        holding = false
        if (dragged >= REFRESH_AT && !refreshingNow) refresh()
        // From where it was let go: the next composition glides it to the phase's gap.
        glide.hold(dragged)
        dragged = 0f
    }
    val connection =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // Pushed back up while pulled: the tongue shortens before the content scrolls.
                    if (holding && available.y < 0 && dragged > 0) {
                        val d = (available.y / density / RESISTANCE).coerceAtLeast(-dragged)
                        dragged += d
                        return Offset(0f, d * RESISTANCE * density)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (consumed.y < 0) atTop = false
                    if (available.y > 0) atTop = true
                    if (source == NestedScrollSource.UserInput && available.y > 0 && !refreshingNow) {
                        val from = standing()
                        holding = true
                        dragged = (from + available.y / density * RESISTANCE).coerceAtMost(MAX_PULL)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (holding) letGo()
                    return Velocity.Zero
                }
            }
        }
    val target =
        when {
            refreshing -> REFRESH_AT
            doneShown -> DONE_GAP
            else -> 0f
        }
    if (holding) glide.hold(dragged) else glide.toward(target, clock.nowMs, still)
    val gap =
        when {
            pull != null -> pull
            done -> DONE_GAP
            holding -> dragged
            else -> glide.at(clock.nowMs, still)
        }
    // Every frame while it glides or the pill counts its second; nothing once it stands.
    if (glide.moving(clock.nowMs, still) || (doneShown && !still)) clock.frame
    // After its second the pill goes back up with the content, drawn back into the line as the gap closes.
    val riding = pull == null && !done && !holding && !doneShown && glide.riding && gap > 0f
    val showDone = pull == null && (done || doneShown || riding)
    // B-105: refreshing over, the tongue becomes the pill over [MORPH_MS] as the content glides up to the pill's gap.
    val since = glide.sinceDone(clock.nowMs)
    val morph =
        if (pull != null || done || still || !doneShown || since.isNaN()) {
            1f
        } else {
            easeOut((since / MORPH_MS).coerceIn(0.0, 1.0)).toFloat()
        }
    // Going back up, how far the pill has been drawn into the line: 0 at the pill's gap, 1 as the gap closes.
    val exit = if (riding) (1f - gap / DONE_GAP).coerceIn(0f, 1f) else 0f
    val line = if (divider) (LINE * density).roundToInt() else 0
    val g = (gap * density).roundToInt()
    val pillH = (PILL_H * density).roundToInt()
    val hang = line - (HANG * density).roundToInt()
    // The pill stands in the middle of the gap; becoming it, the body comes down from where the tongue hangs.
    val middle = line + (g - pillH) / 2
    val pillY =
        when {
            morph < 1f -> lerp(hang.toFloat(), middle.toFloat(), morph).roundToInt()

            // Going back up it stays in the middle of the closing gap as it shrinks, and meets the line as both end.
            exit > 0f -> line + (g - (PILL_H * (1f - exit) * density).roundToInt()) / 2

            else -> middle
        }
    Layout(
        content = {
            Box(Modifier.nestedScroll(connection)) { content() }
            // The line first in the list, last in the drawing: placed after the tongue, it lies over it.
            if (divider) MutantDivider(key = "0")
            if (showDone || gap > SHOWN_FROM) {
                Hanging(
                    showDone,
                    morph,
                    exit,
                    gap,
                    refreshing,
                    refreshingLabel,
                    doneLabel,
                    key,
                    boil,
                    heat,
                    hidden,
                    if (still) null else clock,
                )
            }
        },
        modifier =
            modifier
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(refreshingLabel) { refresh().let { true } })
                }.pointerInput(Unit) {
                    // A mouse drags nothing in Compose: the pull reads the pointer itself, from the content's top.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (down.type != PointerType.Mouse || !atTop || refreshingNow) return@awaitEachGesture
                        var pulling = false
                        var from = 0f
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            val p = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!p.pressed) break
                            val dy = p.position.y - down.position.y
                            if (!pulling && dy > slop) {
                                pulling = true
                                from = standing()
                            }
                            if (pulling) {
                                holding = true
                                dragged = (from + (dy - slop) / density * RESISTANCE).coerceIn(0f, MAX_PULL)
                                // The child's click is cancelled: its press was a pull.
                                p.consume()
                            }
                        }
                        if (pulling) letGo()
                    }
                },
    ) { measurables, c ->
        // No height at all (a scrolling column): the content's own; no width at all (a scrolling row): its own too.
        val room = if (c.hasBoundedHeight) (c.maxHeight - g - line).coerceAtLeast(0) else Constraints.Infinity
        val body = measurables[0].measure(c.copy(minHeight = 0, maxHeight = room))
        val width = if (c.hasBoundedWidth) c.maxWidth else body.width
        val rule = if (divider) measurables[1].measure(Constraints.fixed(width, line)) else null
        val top = measurables.getOrNull(if (divider) 2 else 1)?.measure(Constraints())
        val height = body.height + g + line
        layout(width, if (c.hasBoundedHeight) height.coerceAtMost(c.maxHeight) else height) {
            body.place(0, line + g)
            top?.let {
                val x = (width - it.width) / 2
                // The tongue hangs 2 dp up into the line.
                val y = if (showDone) pillY else line - (HANG * density).roundToInt()
                it.place(x, y)
            }
            rule?.place(0, 0)
        }
    }
}

/**
 * The tongue at [gap] dp: from nothing at 16 dp to the page's short one at 40, its long one at 74, each number in
 * turn between. Its body is built at whole dp once per hot tick of [clock] (D6; at every whole dp with no running
 * clock) and scaled, about its top, to the gap's exact size between: it grows with every frame, not by steps.
 */
@Composable
private fun Tongue(
    gap: Float,
    boiling: Boolean,
    label: String,
    key: String,
    boil: MutableInteractionSource,
    heat: Double?,
    hidden: Boolean,
    clock: io.github.youndie.mutant.clock.MutantClock?,
    morph: Float? = null,
    morphW: Float = 0f,
    morphH: Float = 0f,
) {
    val labelStyle =
        MutantTheme.textStyles
            .osd(LABEL_SIZE.sp)
            .copy(color = if (hidden) Color.Transparent else INK, platformStyle = LocalMutantTextRaster.current)
    val longH = longHeight(label)
    // Becoming the pill (B-105), it takes the shape it is given, whole: its drips are drawn back up as it goes.
    val size = if (morph != null) TongueSize(morphW, morphH, 1f, 1f) else tongueSize(gap, longH)
    val tick = clock?.hotTick
    val built = remember { TongueBuilt() }
    built.follow(size, tick, morph)
    val w = built.w
    val h = built.h
    val f = built.f
    val m = built.m
    // Between ticks, and under 40 dp, the body built is scaled to the gap's exact size.
    val sx = size.width / w
    val sy = size.height / h
    // Becoming the pill, it fades draw by draw, so its drips, outside its box, fade with it.
    val fade = if (morph != null) 1f - morph else 1f
    Box(
        if (sx == 1f && sy == 1f && fade == 1f) {
            Modifier
        } else {
            Modifier.graphicsLayer {
                scaleX = sx
                scaleY = sy
                alpha = fade
                compositingStrategy = CompositingStrategy.ModulateAlpha
                transformOrigin = TransformOrigin(.5f, 0f)
            }
        },
    ) {
        MutantSurface(
            MutantBody.Btn,
            key,
            Modifier.size(w.dp, h.dp),
            interactionSource = boil,
            heat = heat,
            profile =
                MutantBody.Btn.profile.with(
                    radius = (if (m != null) lerp(LONG_R, PILL_R, m) else lerp(SHORT_R, LONG_R, f)).toDouble(),
                    overhang =
                        (if (m != null) lerp(LONG_OVER, 0f, m) else lerp(SHORT_OVER, LONG_OVER, f))
                            .roundToInt()
                            .toDouble(),
                    dripShare = if (f >= 1f) 1.0 else 0.0,
                    outline = OUTLINE,
                    marks = false,
                    mosaic = false,
                ),
        ) {
            if (boiling && f >= 1f && morph == null) {
                Box(Modifier.size(w.dp, h.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        label,
                        Modifier.mutantCleanZone(),
                        style = labelStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * The tongue's size at a gap: its body's [bodyWidth] × [bodyHeight], dp, drawn at [grow] of it, and [f], its share
 * of the way from the short tongue to the long one.
 */
internal class TongueSize(
    val bodyWidth: Float,
    val bodyHeight: Float,
    /** 0 for the short tongue (40 dp and under), 1 for the long one (74 and over). */
    val f: Float,
    /** How much of its body it has grown to: 0 at 16 dp, 1 from 40. */
    val grow: Float,
) {
    val width: Float get() = bodyWidth * grow
    val height: Float get() = bodyHeight * grow
}

/** The tongue's size at [gap] dp, with [longH] the long tongue's height. */

internal fun tongueSize(
    gap: Float,
    longH: Float,
): TongueSize {
    val f = ((gap - SHORT_AT) / (REFRESH_AT - SHORT_AT)).coerceIn(0f, 1f)
    val grow = ((gap - SHOWN_FROM) / (SHORT_AT - SHOWN_FROM)).coerceIn(0f, 1f)
    return TongueSize(lerp(SHORT_W, LONG_W, f), lerp(SHORT_H, longH, f), f, grow)
}

/** The size the tongue's body is built at: whole dp, the short tongue's at the least, taken anew once per tick. */
private class TongueBuilt {
    var w = 0
    var h = 0
    var f = 0f

    /** How far it had become the pill when built, or null. */
    var m: Float? = null
    private var tick: Long? = null

    fun follow(
        size: TongueSize,
        now: Long?,
        morph: Float?,
    ) {
        // Under 40 dp the short tongue is scaled down whole: no body is built at a size the page never draws.
        if (w == 0 || now == null || now != tick || (morph == null) != (m == null)) {
            w = size.bodyWidth.roundToInt()
            h = size.bodyHeight.roundToInt()
            f = size.f
            m = morph
            tick = now
        }
    }
}

/**
 * What hangs from the line: the tongue, the pill, or both while one becomes the other (B-105). One structure in every
 * phase, so neither body is built anew as the phases hand over: the pill's body under, the tongue over, and the words of
 * a move over both, at their size.
 */
@Composable
private fun Hanging(
    showDone: Boolean,
    morph: Float,
    exit: Float,
    gap: Float,
    refreshing: Boolean,
    refreshingLabel: String,
    doneLabel: String,
    key: String,
    boil: MutableInteractionSource,
    heat: Double?,
    hidden: Boolean,
    clock: io.github.youndie.mutant.clock.MutantClock?,
) {
    val moving = showDone && morph < 1f
    val pillW = pillWidth(doneLabel).toFloat()
    val longH = longHeight(refreshingLabel)
    // While it becomes the pill, both bodies take one shape: from the long tongue's to the pill's.
    val w = lerp(LONG_W, pillW, morph)
    val h = lerp(longH, PILL_H.toFloat(), morph)
    Box(contentAlignment = Alignment.TopCenter) {
        if (showDone) PillBody(morph, exit, w, h, pillW, doneLabel, key, hidden, clock)
        if (!showDone || moving) {
            Tongue(gap, refreshing, refreshingLabel, key, boil, heat, hidden, clock, if (moving) morph else null, w, h)
        }
        if (moving || exit > 0f) {
            val bw = if (moving) w else pillW * (1f - exit)
            val bh = if (moving) h else PILL_H * (1f - exit)
            Box(Modifier.size(bw.dp, bh.dp), contentAlignment = Alignment.Center) {
                // The tongue's word fades out in the first half of the move and the pill's comes in over the second;
                // going back up, the pill's fades in the first 40 %.
                val out = if (moving) (1f - 2 * morph).coerceIn(0f, 1f) else 0f
                val into = if (moving) (2 * morph - 1f).coerceIn(0f, 1f) else (1f - exit / .4f).coerceIn(0f, 1f)
                if (out > 0f) BasicText(refreshingLabel, style = pillWords(hidden, out), maxLines = 1, softWrap = false)
                if (into > 0f) BasicText(doneLabel, style = pillWords(hidden, into), maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * The pill: standing, the page's clean pill, 28 dp, its label 14 dp in. Becoming it ([morph] under 1), it has the shape
 * [w] × [h] the tongue over it has, its corners rounding from the tongue's 7 dp to its own; going back up ([exit] over
 * 0), it shrinks about its top into the line and is gone as the gap closes. Its words stand in it only while it stands.
 */
@Composable
private fun PillBody(
    morph: Float,
    exit: Float,
    w: Float,
    h: Float,
    pillW: Float,
    label: String,
    key: String,
    hidden: Boolean,
    clock: io.github.youndie.mutant.clock.MutantClock?,
) {
    val standing = morph >= 1f && exit <= 0f
    val tw = if (morph < 1f) w else pillW * (1f - exit)
    val th = if (morph < 1f) h else PILL_H * (1f - exit)
    val profile =
        if (morph < 1f) {
            MutantBody.Pill.profile.with(radius = lerp(LONG_R, PILL_R, morph).toDouble())
        } else {
            MutantBody.Pill.profile
        }
    val built = remember { BuiltShape() }
    built.follow(tw, th, profile, if (standing) null else clock?.hotTick, exact = standing)
    val sx = tw / built.w
    val sy = th / built.h
    Box(
        if (sx == 1f && sy == 1f) {
            Modifier
        } else {
            Modifier.graphicsLayer {
                scaleX = sx
                scaleY = sy
                transformOrigin = TransformOrigin(.5f, 0f)
            }
        },
    ) {
        MutantSurface(
            MutantBody.Pill,
            "$key-done",
            Modifier.size(built.w.dp, built.h.dp),
            state = BodyState.Success,
            profile = built.profile!!,
        ) {
            if (standing) {
                Box(Modifier.padding(start = PILL_PAD.dp).height(PILL_H.dp), contentAlignment = Alignment.CenterStart) {
                    BasicText(
                        label,
                        Modifier.mutantCleanZone(),
                        style = pillWords(hidden),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** The size and profile the pill's body is built at: exact while it stands, else whole dp once per tick, at least 1. */
private class BuiltShape {
    var w = 0
    var h = 0
    var profile: io.github.youndie.mutant.tokens.MutantProfile? = null
    private var tick: Long? = null

    fun follow(
        w: Float,
        h: Float,
        profile: io.github.youndie.mutant.tokens.MutantProfile,
        now: Long?,
        exact: Boolean,
    ) {
        if (exact || this.w == 0 || now == null || now != tick) {
            this.w = maxOf(w.roundToInt(), MIN_BUILT)
            this.h = maxOf(h.roundToInt(), MIN_BUILT)
            this.profile = profile
            tick = now
        }
    }
}

/** The pill's label style: the OSD face, black in both themes. */
@Composable
private fun pillWords(
    hidden: Boolean,
    alpha: Float = 1f,
) = MutantTheme.textStyles
    .osd(LABEL_SIZE.sp)
    .copy(
        color = if (hidden) Color.Transparent else INK.copy(alpha = alpha),
        platformStyle = LocalMutantTextRaster.current,
    )

/**
 * The pill's width, dp, as Chrome makes it: the label's own width, unrounded, and the padding, rounded once. A box around
 * the label's rounded-up width was a pixel wider.
 */
@Composable
private fun pillWidth(label: String): Int {
    val measured = rememberTextMeasurer().measure(label, pillWords(false), maxLines = 1, softWrap = false)
    val width =
        with(androidx.compose.ui.platform.LocalDensity.current) { measured.multiParagraph.maxIntrinsicWidth.toDp() }
    return (width.value + 2 * PILL_PAD).roundToInt()
}

/** The long tongue's height, dp: 34, or its word's line and 7 dp of room either side if that is taller (font scale 2). */
@Composable
private fun longHeight(label: String): Float {
    val word =
        with(androidx.compose.ui.platform.LocalDensity.current) {
            rememberTextMeasurer()
                .measure(label, pillWords(false), maxLines = 1)
                .size.height
                .toDp()
                .value
        }
    return maxOf(LONG_H, word + 2 * LABEL_ROOM).roundToInt().toFloat()
}

/**
 * The gap outside a drag, dp, on the screen clock: from where it stood to the phase's gap (0, the long tongue's 74,
 * the pill's 52) along an eased-out cubic over [GLIDE_MS], taken up from wherever it is when the phase changes again,
 * so no hand-over jumps. With [still] (reduce motion, a frozen clock) it stands at the phase's gap at once. It also
 * marks, as composition sees refreshing end, when the pill came.
 */
internal class PullGlide(
    start: Float,
) {
    private var from = start
    var to = start
        private set
    private var since = Double.NaN
    private var seen: Boolean? = null
    private var doneAt = Double.NaN

    /** How many times refreshing has ended: the pill's count, which a still clock's wait is keyed on. */
    var dones = 0
        private set

    /** Going back up from the pill: it rides up with the content. */
    var riding = false
        private set

    /** From composition: refreshing seen to end marks the pill, at that frame, without writing state. */
    fun follow(
        refreshing: Boolean,
        now: Double,
    ) {
        val was = seen
        seen = refreshing
        if (refreshing) {
            doneAt = Double.NaN
        } else if (was == true) {
            doneAt = now
            dones++
        }
    }

    /** The pill stands: its second on the running clock, or, [still], until [endPill] once it has been waited out. */
    fun pill(
        now: Double,
        still: Boolean,
    ): Boolean {
        if (doneAt.isNaN()) return false
        if (still) return true
        if (now - doneAt < DONE_MS) return true
        doneAt = Double.NaN
        return false
    }

    /** Ms since the pill came, on the screen clock; NaN once it has gone or before it came. */
    fun sinceDone(now: Double): Double = now - doneAt

    /** The pill came and has not yet gone. */
    fun pillStands(): Boolean = !doneAt.isNaN()

    /** A still clock's second, waited out: the pill goes. */
    fun endPill() {
        doneAt = Double.NaN
    }

    /** Held at [gap] by a finger, or let go there. */
    fun hold(gap: Float) {
        from = gap
        to = gap
        since = Double.NaN
        riding = false
    }

    /** Glides to [target] from where it stands at [now]; a target it already has changes nothing. */
    fun toward(
        target: Float,
        now: Double,
        still: Boolean,
    ) {
        if (target == to) return
        val at = at(now, still)
        riding = to == DONE_GAP && target == 0f
        from = at
        to = target
        since = now
    }

    fun at(
        now: Double,
        still: Boolean,
    ): Float {
        if (still || since.isNaN()) return to
        val p = ((now - since) / GLIDE_MS).coerceIn(0.0, 1.0)
        return from + (to - from) * easeOut(p).toFloat()
    }

    fun moving(
        now: Double,
        still: Boolean,
    ): Boolean = !still && !since.isNaN() && now - since < GLIDE_MS
}

/** The eased-out cubic, `1 − (1 − p)³`: fastest as it leaves, at rest as it lands. */
internal fun easeOut(p: Double): Double = 1 - (1 - p) * (1 - p) * (1 - p)

private fun lerp(
    a: Float,
    b: Float,
    f: Float,
) = a + (b - a) * f

/** The page's frames: the short tongue under a 40 dp gap, the long one under 74, the pill in the middle of 52. */
private const val SHORT_AT = 40f
internal const val REFRESH_AT = 74f
internal const val DONE_GAP = 52f
private const val SHORT_W = 64f
private const val SHORT_H = 10f
private const val SHORT_R = 5f
private const val SHORT_OVER = 14f
private const val LONG_W = 124f
private const val LONG_H = 34f
private const val LONG_R = 7f
private const val LONG_OVER = 24f
private const val OUTLINE = 2.5
private const val HANG = 2
private const val LINE = 2
private const val PILL_H = 28
private const val PILL_PAD = 14
private const val LABEL_SIZE = 18

/** The room the long tongue keeps above and below its word: (34 − 20) / 2 at font scale 1. */
private const val LABEL_ROOM = 7f

/** The labels' black: the page's literal `#05060c` in both themes. */
private val INK = Color(0xFF05060C)

/** Designed here: half the distance dragged; the tongue grows out of nothing from 16 dp; up to 120 dp. */
private const val RESISTANCE = .5f
internal const val SHOWN_FROM = 16f
internal const val MAX_PULL = 120f

/** Designed here: the pill stands a second, and every move between the phases' gaps takes 300 ms, eased out. */
internal const val DONE_MS = 1000.0
internal const val GLIDE_MS = 300.0

/** Designed here (B-105): the tongue becomes the pill over the glide to the pill's gap, eased out with it. */
internal const val MORPH_MS = GLIDE_MS

/** The pill's corner: half its height (the `pill` body's radius, 99, comes to that). */
private const val PILL_R = PILL_H / 2f

/** The smallest a body is built at, dp; under it, it is scaled. */
private const val MIN_BUILT = 4
