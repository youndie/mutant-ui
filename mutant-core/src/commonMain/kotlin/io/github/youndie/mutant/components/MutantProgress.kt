package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.jsRound
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantMotion
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/**
 * A determinate linear progress: a 12 dp black track ringed 2 dp in black, filled to [progress] (0…1) with acid
 * segments 6 dp long and 2 apart, and, given [showValue], the value in the on-screen-display amber 12 dp after it
 * (Handjet 20, three digits: `042%`), whole even past the end of a row too narrow for it. It reads as a progress bar
 * of that value. Right to left it mirrors: the fill grows
 * from the right, the value stands on the left.
 */
@Composable
public fun MutantLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    showValue: Boolean = true,
) {
    val value = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)
    val acid = MutantTheme.colors.primary
    Row(
        modifier.ownWidthWhenUnbounded().semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
        horizontalArrangement = Arrangement.spacedBy(VALUE_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // At least TRACK_MIN wide when the row has no width of its own to share (B-64).
        Canvas(Modifier.weight(1f).defaultMinSize(minWidth = TRACK_MIN).height(TRACK)) {
            fromStart {
                scale(density, Offset.Zero) {
                    val w = size.width / density
                    drawTrack(w)
                    clipRect(0f, 0f, w * value, TRACK.value) { drawSegments(w * value, acid) }
                }
            }
        }
        if (showValue) {
            MutantText(
                "${(value * 100).roundToInt().toString().padStart(3, '0')}%",
                // Whole in any row: a number cut (`042%` to `04`) is another number. Past a narrow row's end it overflows.
                Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                style =
                    TextStyle(
                        fontFamily = LocalMutantFonts.current?.display,
                        fontSize = VALUE_SIZE.sp,
                        color = MutantTheme.colors.textOsd,
                        platformStyle = LocalMutantTextRaster.current,
                    ).cssLine(MutantFaceMetrics.Handjet),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * An indeterminate linear progress (`rewind`): the black track with a 96 dp band running through it — a quarter at
 * 15 % acid, a quarter at 40 %, half solid — 12 dp a 125 ms tick, from beyond its start to beyond its end and round.
 * It moves on every frame at that speed, and in 12 dp steps at the tick under [MutantTheme]'s `steppedMotion` (D22,
 * B-191). Under reduce motion it stands centred on the track, so the track is never empty (B-191). Right to left it
 * runs from the right. With no width to fill (a horizontally scrolling row) it takes 200 dp, as the determinate track
 * does. It reads as indeterminate progress.
 */
@Composable
public fun MutantLinearProgress(modifier: Modifier = Modifier) {
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val steps = LocalMutantSteppedMotion.current
    val acid = MutantTheme.colors.primary
    Canvas(
        modifier
            .fillMaxWidth()
            .trackMinWhenUnbounded()
            .height(TRACK)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        fromStart {
            scale(density, Offset.Zero) {
                val track = size.width / density
                drawTrack(track)
                // Paused, it stands centred on the track, its solid half from the middle on (B-191).
                val x = if (reduce) (track - BAND) / 2 else progressBandX(track, progressTicks(clock, steps))
                clipRect(0f, 0f, size.width / density, TRACK.value) {
                    drawRect(acid, Offset(x, 0f), Size(BAND / 4, TRACK.value), alpha = .15f)
                    drawRect(acid, Offset(x + BAND / 4, 0f), Size(BAND / 4, TRACK.value), alpha = .4f)
                    drawRect(acid, Offset(x + BAND / 2, 0f), Size(BAND / 2, TRACK.value))
                }
            }
        }
    }
}

/**
 * A circular progress, the reel: a 36 dp spool ringed 2.5 dp in black with three holes, turning 30° a 125 ms tick
 * (`reel`), indeterminate: on every frame at that speed, and in 30° steps at the tick under [MutantTheme]'s
 * `steppedMotion` (D22, B-191). It stands under reduce motion. Given a box of another size, it keeps its 36 dp, centred.
 */
@Composable
public fun MutantCircularProgress(modifier: Modifier = Modifier) {
    val clock = readMutantClock()
    val steps = LocalMutantSteppedMotion.current
    Canvas(modifier.size(REEL).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        val turn = progressReelAngle(progressTicks(clock, steps))
        scale(density, Offset.Zero) {
            val r = REEL.value / 2
            // Given a box of another size, the spool keeps its 36 dp and stands in the box's middle.
            val c = Offset(size.width / density / 2, size.height / density / 2)
            // The ring is the page's `box-shadow`: a black disc under the spool, so their edges share no seam.
            drawCircle(BLACK, r + REEL_RING, c)
            drawCircle(SPOOL, r, c)
            rotate(turn, c) {
                for (hole in HOLES) {
                    drawCircle(
                        BLACK,
                        HOLE / 2,
                        c + Offset(hole.x + HOLE / 2 - r, hole.y + HOLE / 2 - r),
                    )
                }
            }
        }
    }
}

/**
 * How many 125 ms ticks into the clock the band and the reel are drawn, read in the draw so it is drawn again when it
 * changes (B-191, D22):
 * - frozen (the engine's `data-mu-t`) or under the theme's [steps] (`steppedMotion`), the scene's tick, whole, as the
 *   engine's `stageStep` draws them;
 * - else the time now over the tick, on every frame, so they move at the same speed without steps.
 */
internal fun progressTicks(
    clock: MutantClock,
    steps: Boolean,
): Double {
    val tick = clock.sceneTick
    if (clock.frozen || steps) return tick.toDouble()
    clock.frame
    return clock.nowMs / MutantMotion.TICK_MS
}

/**
 * The band's start on a [track] dp wide, [ticks] in: 12 dp a tick from a band's width before the track's start, round
 * the track and the band (`translateX((fi * 12) % (pw + w) - w)`). The engine measures the track whole (`offsetWidth`).
 */
internal fun progressBandX(
    track: Float,
    ticks: Double,
): Float {
    val round = jsRound(track.toDouble()) + BAND
    return ((ticks * BAND_STEP) % round).toFloat() - BAND
}

/** The reel's turn [ticks] in, degrees: 30° a tick, round. */
internal fun progressReelAngle(ticks: Double): Float = ((ticks * REEL_STEP) % 360.0).toFloat()

/**
 * Draws [block] from the layout's start: as it is left to right, mirrored right to left, so a fill grows from the start
 * and the band runs toward the end, as the rest of the kit mirrors (B-65).
 */
private inline fun DrawScope.fromStart(block: DrawScope.() -> Unit) {
    if (layoutDirection == LayoutDirection.Rtl) scale(-1f, 1f, center, block) else block()
}

/**
 * With no width to fill (a horizontally scrolling row), exactly [TRACK_MIN] wide, as the determinate track (B-64): a
 * `Canvas` given only a minimum measures 0 and stands centred in it, undrawn.
 */
private fun Modifier.trackMinWhenUnbounded(): Modifier =
    layout { m, c ->
        val p =
            if (c.hasBoundedWidth) {
                m.measure(c)
            } else {
                val w = maxOf(TRACK_MIN.roundToPx(), c.minWidth)
                m.measure(c.copy(minWidth = w, maxWidth = w))
            }
        layout(p.width, p.height) { p.place(0, 0) }
    }

/** The track, its ring outside it (`box-shadow: 0 0 0 2px`). */
private fun DrawScope.drawTrack(w: Float) {
    drawRect(BLACK, Offset(-TRACK_RING, -TRACK_RING), Size(w + 2 * TRACK_RING, TRACK.value + 2 * TRACK_RING))
}

/** The fill's segments across [w] dp: acid 6, black 2, from the start (`repeating-linear-gradient`). */
private fun DrawScope.drawSegments(
    w: Float,
    acid: Color,
) {
    var x = 0f
    while (x < w) {
        drawRect(acid, Offset(x, 0f), Size(SEGMENT, TRACK.value))
        x += SEGMENT + SEGMENT_GAP
    }
}

/** The track (`height: 12px`), its ring (2), its segments (6 + 2) and the value 12 dp after it (Handjet 20). */
private val TRACK = 12.dp

/** The track's width in a row with none to give it (B-64): a third of the page's 600 dp column. */
private val TRACK_MIN = 200.dp
private const val TRACK_RING = 2f
private const val SEGMENT = 6f
private const val SEGMENT_GAP = 2f
private val VALUE_GAP = 12.dp
private const val VALUE_SIZE = 20

/** The band: 96 dp, 12 dp a tick (`translateX((fi * 12) % (pw + w) - w)`); paused, centred on the track (B-191). */
private const val BAND = 96f
private const val BAND_STEP = 12.0

/** The reel: 36 dp, ringed 2.5, its holes 8 dp at 14,4 · 5,20 · 23,20, 30° a tick. */
private val REEL = 36.dp
private const val REEL_RING = 2.5f
private const val HOLE = 8f
private val HOLES = listOf(Offset(14f, 4f), Offset(5f, 20f), Offset(23f, 20f))
private const val REEL_STEP = 30.0

/**
 * The page's literals in both themes, which «Копия» does not repaint: the black `#05060c` and the spool's paper
 * `#e9f2e4`. Its acid `#b6ff1a` is both themes' primary.
 */
private val BLACK = Color(0xFF05060C)
private val SPOOL = Color(0xFFE9F2E4)
