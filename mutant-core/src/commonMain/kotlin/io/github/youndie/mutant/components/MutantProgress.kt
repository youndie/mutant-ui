package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.jsRound
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/**
 * A determinate linear progress: a 12 dp black track ringed 2 dp in black, filled to [progress] (0…1) with acid
 * segments 6 dp long and 2 apart, and, given [showValue], the value in the on-screen-display amber 12 dp after it
 * (Handjet 20, three digits: `042%`). It reads as a progress bar of that value.
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
            scale(density, Offset.Zero) {
                val w = size.width / density
                drawTrack(w)
                clipRect(0f, 0f, w * value, TRACK.value) { drawSegments(w * value, acid) }
            }
        }
        if (showValue) {
            BasicText(
                "${(value * 100).roundToInt().toString().padStart(3, '0')}%",
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
 * 15 % acid, a quarter at 40 %, half solid — 12 dp a scene tick, from beyond its start to beyond its end and round.
 * Under reduce motion it stands where the page's still frame has it (the engine's 5 s). It reads as indeterminate
 * progress.
 */
@Composable
public fun MutantLinearProgress(modifier: Modifier = Modifier) {
    val clock = readMutantClock()
    val reduce = LocalMutantReduceMotion.current
    val acid = MutantTheme.colors.primary
    Canvas(
        modifier
            .fillMaxWidth()
            .height(TRACK)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        // Paused, it stands where the page's still frame has it: the clock's own tick 0 would leave it before the track.
        val tick = if (reduce) STILL_TICK else clock.sceneTick
        scale(density, Offset.Zero) {
            // The engine measures the track and the band whole (`offsetWidth`).
            val w = jsRound(size.width / density.toDouble()).toFloat()
            drawTrack(size.width / density)
            val x = ((tick * BAND_STEP) % (w + BAND).toLong()).toFloat() - BAND
            clipRect(0f, 0f, size.width / density, TRACK.value) {
                drawRect(acid, Offset(x, 0f), Size(BAND / 4, TRACK.value), alpha = .15f)
                drawRect(acid, Offset(x + BAND / 4, 0f), Size(BAND / 4, TRACK.value), alpha = .4f)
                drawRect(acid, Offset(x + BAND / 2, 0f), Size(BAND / 2, TRACK.value))
            }
        }
    }
}

/**
 * A circular progress, the reel: a 36 dp spool ringed 2.5 dp in black with three holes, turning 30° each scene tick
 * (`reel`), indeterminate. It stands under reduce motion.
 */
@Composable
public fun MutantCircularProgress(modifier: Modifier = Modifier) {
    val clock = readMutantClock()
    Canvas(modifier.size(REEL).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        val tick = clock.sceneTick
        scale(density, Offset.Zero) {
            val r = REEL.value / 2
            val c = Offset(r, r)
            // The ring is the page's `box-shadow`: a black disc under the spool, so their edges share no seam.
            drawCircle(BLACK, r + REEL_RING, c)
            drawCircle(SPOOL, r, c)
            rotate(((tick * REEL_STEP) % 360).toFloat(), c) {
                for (hole in HOLES) drawCircle(BLACK, HOLE / 2, Offset(hole.x + HOLE / 2, hole.y + HOLE / 2))
            }
        }
    }
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

/** The band: 96 dp, 12 dp a tick (`translateX((fi * 12) % (pw + w) - w)`); paused, at the still frame's tick. */
private const val BAND = 96f
private const val BAND_STEP = 12L
private val STILL_TICK = (STILL_MS / MutantClock.SCENE_TICK_MS).toLong()

/** The reel: 36 dp, ringed 2.5, its holes 8 dp at 14,4 · 5,20 · 23,20, 30° a tick. */
private val REEL = 36.dp
private const val REEL_RING = 2.5f
private const val HOLE = 8f
private val HOLES = listOf(Offset(14f, 4f), Offset(5f, 20f), Offset(23f, 20f))
private const val REEL_STEP = 30L

/**
 * The page's literals in both themes, which «Копия» does not repaint: the black `#05060c` and the spool's paper
 * `#e9f2e4`. Its acid `#b6ff1a` is both themes' primary.
 */
private val BLACK = Color(0xFF05060C)
private val SPOOL = Color(0xFFE9F2E4)
