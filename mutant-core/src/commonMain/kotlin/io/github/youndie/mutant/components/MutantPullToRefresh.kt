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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import io.github.youndie.mutant.surface.stepped
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
 * `pill` with [doneLabel] stands for a second, then the content comes back up.
 *
 * The design draws three frames; the gesture is designed here (D15): a finger's drag past the content's top, through
 * nested scroll, and a mouse's drag from wherever the content stands at its top, half the distance dragged, the content
 * back up over 200 ms if let go short. Under reduce motion it moves at once and the boil stands. A reader's custom
 * action refreshes. Over its content stands the header's tear line (the tracking divider, [divider]), drawn over the
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
    // The gap the content stands under, dp: the finger's half while held; the phases' own otherwise.
    var dragged by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var atTop by remember { mutableStateOf(true) }
    val phase = remember { RefreshPhase() }
    val still = reduce || clock.frozen
    // Read by the gestures, which are set up once: always this composition's.
    val refreshingNow by rememberUpdatedState(refreshing)
    val stillNow by rememberUpdatedState(still)
    // The pill's second is the wall's, not the screen's clock, which stands under reduce motion.
    var doneShown by remember { mutableStateOf(false) }
    val was = remember { booleanArrayOf(refreshing) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            was[0] = true
            return@LaunchedEffect
        }
        if (!was[0]) return@LaunchedEffect
        was[0] = false
        doneShown = true
        delay(DONE_MS.toLong())
        doneShown = false
        phase.back(DONE_GAP, clock.nowMs, still)
    }
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

    fun letGo() {
        holding = false
        if (dragged >= REFRESH_AT && !refreshingNow) refresh()
        // Back up from where it was let go; once the caller is refreshing, the long tongue holds it at 74 dp.
        phase.back(dragged, clock.nowMs, stillNow)
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
                        holding = true
                        dragged = (dragged + available.y / density * RESISTANCE).coerceAtMost(MAX_PULL)
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
    val gap =
        when {
            pull != null -> pull
            holding -> dragged
            done || doneShown -> DONE_GAP
            refreshing -> REFRESH_AT
            else -> phase.gap(clock)
        }
    if (phase.moving(clock)) clock.frame
    val showDone = done || doneShown
    Layout(
        content = {
            Box(Modifier.nestedScroll(connection)) { content() }
            // The line first in the list, last in the drawing: placed after the tongue, it lies over it.
            if (divider) MutantDivider(key = "0")
            if (showDone) {
                DonePill(doneLabel, "$key-done", hidden)
            } else if (gap >= SHOWN_FROM) {
                Tongue(gap, refreshing, refreshingLabel, key, boil, heat, hidden)
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
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            val p = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!p.pressed) break
                            val dy = p.position.y - down.position.y
                            if (!pulling && dy > slop) pulling = true
                            if (pulling) {
                                holding = true
                                dragged = ((dy - slop) / density * RESISTANCE).coerceIn(0f, MAX_PULL)
                                // The child's click is cancelled: its press was a pull.
                                p.consume()
                            }
                        }
                        if (pulling) letGo()
                    }
                },
    ) { measurables, c ->
        val line = if (divider) (LINE * density).roundToInt() else 0
        val g = (gap * density).roundToInt()
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
                // The tongue hangs 2 dp up into the line; the pill stands in the middle of its 52 dp.
                val y = if (showDone) line + (g - it.height) / 2 else line - (HANG * density).roundToInt()
                it.place(x, y)
            }
            rule?.place(0, 0)
        }
    }
}

/** The tongue at [gap] dp: the page's short one at 40, its long one at 74, between them each number in turn. */
@Composable
private fun Tongue(
    gap: Float,
    boiling: Boolean,
    label: String,
    key: String,
    boil: MutableInteractionSource,
    heat: Double?,
    hidden: Boolean,
) {
    val f = ((gap - SHORT_AT) / (REFRESH_AT - SHORT_AT)).coerceIn(0f, 1f)
    val grow = (gap / SHORT_AT).coerceAtMost(1f)
    // Whole dp: a body is rebuilt for each size, so the tongue grows by steps, not by every pixel of the drag.
    val w = (lerp(SHORT_W, LONG_W, f) * grow).roundToInt()
    val labelStyle =
        MutantTheme.textStyles
            .osd(LABEL_SIZE.sp)
            .copy(color = if (hidden) Color.Transparent else INK, platformStyle = LocalMutantTextRaster.current)
    // The long tongue keeps its 7 dp of room around its word: a taller word (font scale 2) makes it taller.
    val word =
        with(androidx.compose.ui.platform.LocalDensity.current) {
            rememberTextMeasurer()
                .measure(label, labelStyle, maxLines = 1)
                .size.height
                .toDp()
                .value
        }
    val longH = maxOf(LONG_H, word + 2 * LABEL_ROOM)
    val h = (lerp(SHORT_H, longH, f) * grow).roundToInt().coerceAtLeast(MIN_H)
    MutantSurface(
        MutantBody.Btn,
        key,
        Modifier.size(w.dp, h.dp),
        interactionSource = boil,
        heat = heat,
        profile =
            MutantBody.Btn.profile.with(
                radius = lerp(SHORT_R, LONG_R, f).toDouble(),
                overhang = lerp(SHORT_OVER, LONG_OVER, f).roundToInt().toDouble(),
                dripShare = if (f >= 1f) 1.0 else 0.0,
                outline = OUTLINE,
                marks = false,
                mosaic = false,
            ),
    ) {
        if (boiling && f >= 1f) {
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

/** The end: a clean pill, 28 dp, its label 14 dp in. */
@Composable
private fun DonePill(
    label: String,
    key: String,
    hidden: Boolean,
) {
    val style =
        MutantTheme.textStyles
            .osd(LABEL_SIZE.sp)
            .copy(color = if (hidden) Color.Transparent else INK, platformStyle = LocalMutantTextRaster.current)
    // As wide as Chrome makes it: the label's own width, unrounded, and the padding, rounded once. A box around the
    // label's rounded-up width was a pixel wider.
    val measured = rememberTextMeasurer().measure(label, style, maxLines = 1, softWrap = false)
    val width =
        with(androidx.compose.ui.platform.LocalDensity.current) { measured.multiParagraph.maxIntrinsicWidth.toDp() }
    MutantSurface(
        MutantBody.Pill,
        key,
        Modifier.width((width.value + 2 * PILL_PAD).roundToInt().dp).height(PILL_H.dp),
        state = BodyState.Success,
    ) {
        Box(Modifier.padding(start = PILL_PAD.dp).height(PILL_H.dp), contentAlignment = Alignment.CenterStart) {
            BasicText(label, Modifier.mutantCleanZone(), style = style, maxLines = 1, softWrap = false)
        }
    }
}

/** The content going back up, after the pill or after a pull let go short; times are the screen clock's. */
private class RefreshPhase {
    private var from = 0f
    private var since = Double.NaN
    private var instant = false

    fun back(
        gap: Float,
        now: Double,
        still: Boolean,
    ) {
        from = gap
        since = now
        instant = still
    }

    /** Still going back, in the kit's steps: a step short of [BACK_MS] is not yet back. */
    fun moving(clock: io.github.youndie.mutant.clock.MutantClock): Boolean =
        !since.isNaN() && !instant && stepped(clock.nowMs - since, BACK_MS) < 1

    /** The gap, dp, outside a drag, the pill and a refresh. */
    fun gap(clock: io.github.youndie.mutant.clock.MutantClock): Float {
        if (since.isNaN()) return 0f
        val p = if (instant) 1.0 else stepped(clock.nowMs - since, BACK_MS)
        if (p >= 1) {
            since = Double.NaN
            return 0f
        }
        return (from * (1 - p)).toFloat()
    }
}

private fun lerp(
    a: Float,
    b: Float,
    f: Float,
) = a + (b - a) * f

/** The page's frames: the short tongue under a 40 dp gap, the long one under 74, the pill in the middle of 52. */
private const val SHORT_AT = 40f
private const val REFRESH_AT = 74f
private const val DONE_GAP = 52f
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

/** Designed here: half the distance dragged; no tongue under 16 dp, none shorter than 4; up to 120 dp. */
private const val RESISTANCE = .5f
private const val SHOWN_FROM = 16f
private const val MIN_H = 4
private const val MAX_PULL = 120f

/** Designed here: the pill stands a second, and the content goes back up over 200 ms. */
private const val DONE_MS = 1000.0
private const val BACK_MS = 200.0
