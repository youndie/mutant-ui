package io.github.youndie.mutant.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A slider (v1): liquid in a dark channel, and a drop of amber as the handle with the value inside. Over a 14 dp channel
 * an 8 dp fill runs to the handle, a `fab` body of 34 dp (r 17, drips of 12) that boils while it is held; above the
 * channel stand five ticks, and above them the [label] on the left and the range on the right, in OSD type.
 *
 * The value is a whole number in [valueRange], shown signed in the handle (`+3`, `−4`, `0`) and stepped one at a time.
 * The handle follows the pointer while it is held and settles on the value's place when let go. A press anywhere on
 * the 60 dp row takes the handle there; the row is the touch target. Arrows step the value, Home and End go to the
 * ends. Disabled, the handle loses its signal and the fill and the words grey.
 *
 * Its width is the one given; with none (a scrolling row), the page's 300 dp. A range of one value or a reversed one
 * draws the handle at the start and takes no input.
 */
@Composable
public fun MutantSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    valueRange: IntRange = -10..10,
    enabled: Boolean = true,
    key: String = label ?: "slider",
    interactionSource: MutableInteractionSource? = null,
) {
    val span = valueRange.last - valueRange.first
    val v = value.coerceIn(valueRange.first, maxOf(valueRange.first, valueRange.last))
    SliderBody(
        fraction = if (span > 0) (v - valueRange.first).toFloat() / span else 0f,
        valueText = signed(v),
        onFraction = { f -> if (span > 0) onValueChange(valueRange.first + (f * span).roundToInt()) },
        step = { by -> if (span > 0) onValueChange((v + by).coerceIn(valueRange.first, valueRange.last)) },
        modifier = modifier,
        label = label,
        rangeText = "${signed(valueRange.first)} … ${signed(valueRange.last)}",
        enabled = enabled && span > 0,
        key = key,
        interactionSource = interactionSource,
        widest = listOf(signed(valueRange.first), signed(valueRange.last)),
        range = {
            if (span >
                0
            ) {
                Triple(v.toFloat(), valueRange.first.toFloat()..valueRange.last.toFloat(), span - 1)
            } else {
                null
            }
        },
    )
}

/** A whole number as the design signs it: `+3`, `−4` (a minus sign, U+2212), `0`. */
internal fun signed(v: Int): String =
    when {
        v > 0 -> "+$v"
        v < 0 -> "−${-v}"
        else -> "0"
    }

/**
 * [MutantSlider] by its handle's place: [fraction] of the travel, 0…1. The handle is wide enough for its value and for
 * each of [widest], the values the range can show at its widest. [hidden] draws the words transparent, a bare twin's.
 */
@Composable
internal fun SliderBody(
    fraction: Float,
    valueText: String,
    onFraction: (Float) -> Unit,
    step: (Int) -> Unit,
    modifier: Modifier,
    label: String?,
    rangeText: String,
    enabled: Boolean,
    key: String,
    interactionSource: MutableInteractionSource?,
    widest: List<String> = emptyList(),
    range: () -> Triple<Float, ClosedFloatingPointRange<Float>, Int>? = { null },
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val styles = MutantTheme.textStyles
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    // While held, the handle follows the pointer; let go, it stands where the value puts it.
    var held by remember { mutableStateOf<Float?>(null) }
    val at = (held ?: fraction).let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) }
    val onMove by rememberUpdatedState(onFraction)
    val atNow by rememberUpdatedState(at)
    val words =
        if (hidden) {
            Color.Transparent
        } else if (enabled) {
            colors.textOsd
        } else {
            colors.textDisabled
        }
    // The platform's raster even in a parity fixture: ChromePlacedText already places the line at Chrome's fraction,
    // and the fixture's subpixel positioning would shift it a second time (B-85).
    val header = styles.osd(HEADER_SIZE.sp).copy(color = words)
    val valueStyle =
        styles
            .display(VALUE_SIZE.sp)
            .cssLine(MutantFaceMetrics.Handjet, 1f)
            .copy(color = if (hidden) Color.Transparent else colors.onPrimary)
    // The handle is the page's 34 dp drop while its value fits with room around it; a value that does not (font scale
    // 2) widens it, and a number is never cut (designed here, D15). It fits the range's widest value, not the one
    // shown, so it keeps its width while the value changes under a finger (B-168).
    val measurer = rememberTextMeasurer()
    val px = LocalDensity.current
    val fit =
        (widest + valueText)
            .map { measurer.measure(it, valueStyle, maxLines = 1).size }
            .reduce { a, b -> IntSize(maxOf(a.width, b.width), maxOf(a.height, b.height)) }
    val handleW = with(px) { maxOf(HANDLE.dp.roundToPx(), fit.width + 2 * VALUE_ROOM.dp.roundToPx()) }
    val handleH = with(px) { maxOf(HANDLE.dp.roundToPx(), fit.height + 2 * VALUE_ROOM.dp.roundToPx()) }
    val handleNow by rememberUpdatedState(handleW)
    Column(
        modifier
            .layout { m, c ->
                // No width at all (a scrolling row): the page's.
                val w = if (c.hasBoundedWidth) c.maxWidth else PAGE_WIDTH.dp.roundToPx().coerceAtLeast(c.minWidth)
                val p = m.measure(c.copy(minWidth = w, maxWidth = w))
                layout(p.width, p.height) { p.place(0, 0) }
            }.semantics(mergeDescendants = true) {
                label?.let { contentDescription = it }
                stateDescription = valueText
                if (!enabled) disabled()
                range()?.let { (v, r, steps) ->
                    progressBarRangeInfo = ProgressBarRangeInfo(v, r, steps)
                    if (enabled) {
                        setProgress { target ->
                            onMove(((target - r.start) / (r.endInclusive - r.start)).coerceIn(0f, 1f))
                            true
                        }
                    }
                }
            }.onKeyEvent {
                if (!enabled || it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionRight, Key.DirectionUp -> step(1)
                    Key.DirectionLeft, Key.DirectionDown -> step(-1)
                    Key.MoveHome -> onMove(0f)
                    Key.MoveEnd -> onMove(1f)
                    else -> return@onKeyEvent false
                }
                true
            }.focusable(enabled, source),
        verticalArrangement = Arrangement.spacedBy(HEADER_GAP.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // The range is numbers and keeps its width; a label that does not fit beside it ends in an ellipsis, 8 dp
            // short of the range (designed here, B-168).
            MutantText(
                label.orEmpty(),
                Modifier.weight(1f, fill = false).padding(end = HEADER_ROOM.dp),
                style = header,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            ChromePlacedText(rangeText, header, Modifier, InkPlace.End)
        }
        val fill = if (enabled) colors.primary else colors.textDisabled
        Layout(
            content = {
                MutantSurface(
                    MutantBody.Fab,
                    key,
                    Modifier,
                    fill = colors.amber,
                    state = if (enabled) BodyState.Rest else BodyState.Disabled,
                    focused = focused && keyboard,
                    interactionSource = source,
                    // The page's disabled handle sets only its radius; the one to drag also its drips, no marks, no
                    // mosaic.
                    profile =
                        if (enabled) {
                            MutantBody.Fab.profile.with(
                                radius = HANDLE_RADIUS,
                                overhang = HANDLE_OVERHANG,
                                marks = false,
                                mosaic = false,
                            )
                        } else {
                            MutantBody.Fab.profile.with(radius = HANDLE_RADIUS)
                        },
                ) {
                    Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                        ChromePlacedText(valueText, valueStyle, Modifier.mutantCleanZone(), InkPlace.Centre, handleW)
                    }
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT.dp)
                    .mutantHand(enabled)
                    .drawBehind {
                        val d = density
                        val handle = handleW.toFloat()
                        val left = at * (size.width - handle).coerceAtLeast(0f)
                        // The channel, its inset ring of the second surface, and the fill up to the handle's middle.
                        val top = CHANNEL_TOP * d
                        val h = CHANNEL_HEIGHT * d
                        drawRoundRect(colors.onPrimary, Offset(0f, top), Size(size.width, h), CornerRadius(h / 2))
                        val ring = RING * d
                        drawRoundRect(
                            colors.surfaceVariant,
                            Offset(ring / 2, top + ring / 2),
                            Size(size.width - ring, h - ring),
                            CornerRadius((h - ring) / 2),
                            style = Stroke(ring),
                        )
                        val fh = FILL_HEIGHT * d
                        drawRoundRect(
                            fill,
                            Offset(FILL_INSET * d, top + FILL_INSET * d),
                            Size(left + handle / 2 - FILL_SHORT * d, fh),
                            CornerRadius(fh / 2),
                        )
                    }.drawWithContent {
                        drawContent()
                        // Five ticks over the channel, 16 dp in from each end, long and short in turn.
                        val d = density
                        val w = TICK_WIDTH * d
                        val from = TICK_INSET * d
                        val gap = (size.width - 2 * from - w) / (TICKS - 1)
                        for (i in 0 until TICKS) {
                            val th = (if (i % 2 == 0) TICK_LONG else TICK_SHORT) * d
                            // Chrome lays the row out at fractions of a pixel and paints each box snapped to whole ones.
                            val x = floor(from + i * gap + .5f)
                            drawRect(colors.textSecondary, Offset(x, TICK_BOTTOM * d - th), Size(w, th))
                        }
                    }.then(
                        if (enabled) {
                            Modifier.pointerInput(source) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val handle = handleNow.toFloat()
                                    val travel = (size.width - handle).coerceAtLeast(1f)
                                    val left = atNow * travel
                                    // Held by the handle where it was taken; a press elsewhere takes it by its middle.
                                    val grip =
                                        if (down.position.x in
                                            left..(left + handle)
                                        ) {
                                            down.position.x - left
                                        } else {
                                            handle / 2
                                        }
                                    val press = PressInteraction.Press(down.position)
                                    source.tryEmit(press)

                                    fun follow(x: Float) {
                                        val f = ((x - grip) / travel).coerceIn(0f, 1f)
                                        held = f
                                        onMove(f)
                                    }
                                    follow(down.position.x)
                                    down.consume()
                                    val ended =
                                        drag(down.id) {
                                            follow(it.position.x)
                                            it.consume()
                                        }
                                    held = null
                                    source.tryEmit(
                                        if (ended) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                                    )
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
        ) { measurables, c ->
            val p = measurables.first().measure(Constraints.fixed(handleW, handleH))
            val travel = (c.maxWidth - handleW).coerceAtLeast(0)
            // Centred on the channel, as the page's 34 dp handle at 10 dp down is.
            val top = (CHANNEL_TOP + CHANNEL_HEIGHT / 2).dp.roundToPx() - handleH / 2
            layout(c.maxWidth, c.maxHeight) { p.place((at * travel).roundToInt(), top) }
        }
    }
}

/** Where a line stands in the box a layout gave it: centred in it, or against its end. */
private enum class InkPlace { Centre, End }

/**
 * One line drawn where Chrome draws it. Compose places a text's box at a whole pixel and rounds its width up, so a line
 * centred in the handle, or pushed to the end of the header, stands up to a pixel off Chrome's, whose glyphs start at a
 * fraction: a stem of «4» lay whole in one column here and across two on the page. The line is drawn shifted by what
 * the rounding took.
 */
@Composable
private fun ChromePlacedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier,
    place: InkPlace,
    room: Int = 0,
) {
    var line by remember { mutableFloatStateOf(Float.NaN) }
    MutantText(
        text,
        modifier.drawWithContent {
            val w = line
            if (w.isNaN()) return@drawWithContent drawContent()
            val dx =
                when (place) {
                    // Centred in the handle by Alignment.Center, which rounds half the room left.
                    InkPlace.Centre -> {
                        (room - w) / 2 - ((room - size.width) / 2f).roundToInt()
                    }

                    // Pushed to the end: the box is the line's width rounded up.
                    InkPlace.End -> {
                        size.width - w
                    }
                }
            translate(left = dx) { this@drawWithContent.drawContent() }
        },
        style = style,
        maxLines = 1,
        onTextLayout = { line = it.getLineRight(0) - it.getLineLeft(0) },
    )
}

/** The page's slider: 300 wide, its header Handjet 20 over a 60 dp row, 2 dp apart. */
private const val PAGE_WIDTH = 300
private const val HEADER_SIZE = 20
private const val HEADER_GAP = 2
private const val ROW_HEIGHT = 60

/** The room a label keeps before the range when the two do not both fit (designed here, B-168). */
private const val HEADER_ROOM = 8

/** The channel: from 20 dp down, 14 tall, a 2 dp inset ring. The fill: 3 dp in, 8 tall, to 3 dp short of the handle's middle. */
private const val CHANNEL_TOP = 20f
private const val CHANNEL_HEIGHT = 14f
private const val RING = 2f
private const val FILL_INSET = 3f
private const val FILL_HEIGHT = 8f
private const val FILL_SHORT = 3f

/** The handle: 34 dp, centred on the channel (10 dp down), r 17, drips of 12 (`data-r="17" data-over="12"`); its value Handjet 18. */
private const val HANDLE = 34
private const val HANDLE_RADIUS = 17.0
private const val HANDLE_OVERHANG = 12.0
private const val VALUE_SIZE = 18

/** The room a value keeps on each side inside the handle before the handle widens for it (designed here). */
private const val VALUE_ROOM = 4

/** Ticks: five, 2 dp wide, 10 and 6 tall in turn, standing on 12 dp, 16 dp in from each end. */
private const val TICKS = 5
private const val TICK_WIDTH = 2f
private const val TICK_LONG = 10f
private const val TICK_SHORT = 6f
private const val TICK_BOTTOM = 12f
private const val TICK_INSET = 16f
