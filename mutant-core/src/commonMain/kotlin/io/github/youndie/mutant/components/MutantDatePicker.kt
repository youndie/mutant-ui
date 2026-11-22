package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.gen.jsToFixed
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.SlideRequest
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantMotion
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A date picker, the recording timer's calendar: a dark `bar` (the surface's fill, radius 12, no marks) 284 dp wide,
 * and in it the month of [year] and [month] (1–12) in the on-screen-display amber (Handjet 22, tracked 1) with a back
 * and a forward arrow at its end, the weekdays (Handjet 16, secondary, Monday first) and the days, seven to a row in
 * cells of 36 dp (Rubik 14). The [selectedDay] stands in a clean drop (body `pill`, radius 18), its number in black.
 *
 * A press on a day hands it to [onSelect]; the arrows go to [onMonthChange] with the month before or after, their
 * targets 48 dp at least. A day's cell is its whole target, 36 dp: an owner-named exception to the 48 dp rule
 * (research D19); the cells tile the grid. The arrow keys move the keyboard's focus a day or a week; Enter or Space
 * picks. The design is a still: all of this is designed here (D15), the drop standing at once on its new day.
 */
@Composable
public fun MutantDatePicker(
    year: Int,
    month: Int,
    selectedDay: Int?,
    onSelect: (Int) -> Unit,
    onMonthChange: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier,
    monthNames: List<String> = MONTHS,
    weekdayNames: List<String> = WEEKDAYS,
    previousLabel: String = "Предыдущий месяц",
    nextLabel: String = "Следующий месяц",
    key: String = "cal",
) {
    DatePickerBody(
        year,
        month,
        selectedDay,
        onSelect,
        onMonthChange,
        modifier,
        monthNames,
        weekdayNames,
        previousLabel,
        nextLabel,
        key,
    )
}

/** [MutantDatePicker]; [hidden] draws its words and arrows transparent for a bare twin. */
@Composable
internal fun DatePickerBody(
    year: Int,
    month: Int,
    selectedDay: Int?,
    onSelect: (Int) -> Unit,
    onMonthChange: (Int, Int) -> Unit,
    modifier: Modifier,
    monthNames: List<String>,
    weekdayNames: List<String>,
    previousLabel: String,
    nextLabel: String,
    key: String,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current

    fun ink(c: Color) = if (hidden) Color.Transparent else c
    val m = month.coerceIn(1, 12)
    val days = daysIn(year, m)
    val lead = firstWeekday(year, m)
    val pick by rememberUpdatedState(onSelect)
    val change by rememberUpdatedState(onMonthChange)
    MutantSurface(
        MutantBody.Bar,
        key,
        // The page's 284 dp; a larger font scale widens it with its cells.
        modifier.widthIn(min = WIDTH.dp),
        fill = colors.surface,
        profile = MutantBody.Bar.profile.with(radius = RADIUS, marks = false),
    ) {
        Column(
            // As wide as its days (B-150): the header fills the column, not whatever width the calendar is offered.
            Modifier
                .padding(start = SIDE.dp, end = SIDE.dp, top = TOP.dp, bottom = FOOT.dp)
                .width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(GAP.dp),
        ) {
            // The page's row is as tall as the arrows' glyphs, in the host's face, make it: 25 dp.
            Row(Modifier.fillMaxWidth().heightIn(min = HEADER.dp), verticalAlignment = Alignment.CenterVertically) {
                MutantText(
                    "${monthNames.getOrElse(m - 1) { "$m" }} $year",
                    Modifier.weight(1f).mutantCleanZone(),
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = MONTH_SIZE.sp,
                            letterSpacing = TRACKING.sp,
                            color = ink(colors.textOsd),
                            platformStyle = raster,
                        ).cssLine(MutantFaceMetrics.Handjet),
                    maxLines = 1,
                    softWrap = false,
                )
                Arrow(back = true, previousLabel, ink(colors.textSecondary)) {
                    if (m == 1) change(year - 1, 12) else change(year, m - 1)
                }
                Spacer(Modifier.width(ARROW_GAP.dp))
                Arrow(back = false, nextLabel, ink(colors.textSecondary)) {
                    if (m == 12) change(year + 1, 1) else change(year, m + 1)
                }
                Spacer(Modifier.width(ARROW_END.dp))
            }
            Row {
                val week =
                    TextStyle(
                        fontFamily = fonts?.display,
                        fontSize = WEEKDAY_SIZE.sp,
                        color = ink(colors.textSecondary),
                        textAlign = TextAlign.Center,
                        platformStyle = raster,
                    ).cssLine(MutantFaceMetrics.Handjet)
                for (i in 0 until 7) {
                    MutantText(
                        weekdayNames.getOrElse(i) { "" },
                        Modifier.width(CELL.dp).mutantCleanZone(),
                        style = week,
                        maxLines = 1,
                    )
                }
            }
            DayGrid(days, lead, selectedDay, key, hidden) { pick(it) }
        }
    }
}

/** The days: blank cells before the first, then each day a 36 dp cell; the chosen one's drop under its number. */
@Composable
private fun DayGrid(
    days: Int,
    lead: Int,
    selectedDay: Int?,
    key: String,
    hidden: Boolean,
    onPick: (Int) -> Unit,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val chosen = selectedDay != null && selectedDay in 1..days
    val focus = remember(days) { List(days) { FocusRequester() } }
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    // B-93: the drop runs from the day it stood on to the new one, as the navigation bar's pill does, through both axes.
    // A new month starts it where it lands. Each number is black where the drop is drawn under it and light where the
    // bar is (B-168), read from the drop's place on every frame of its run.
    val clock = readMutantClock()
    val instant = LocalMutantReduceMotion.current
    val steps = LocalMutantSteppedMotion.current
    val from = remember { DropFrom(selectedDay ?: 0, lead, days) }
    from.follow(selectedDay ?: 0, lead, days, clock.nowMs, instant, steps)
    val cellDp = remember { floatArrayOf(CELL.toFloat()) }
    val place = if (chosen) DropPlace(from, clock, lead + selectedDay - 1, cellDp, instant, steps) else null
    val dayStyle = { weight: FontWeight, color: Color ->
        TextStyle(
            fontFamily = fonts?.text,
            fontSize = DAY_SIZE.sp,
            fontWeight = weight,
            color = if (hidden) Color.Transparent else color,
            textAlign = TextAlign.Center,
            platformStyle = raster,
        ).let {
            // On the cell's 36 dp line at the user's own size; a larger font scale sets the number on the face's own
            // line and the cell grows to it (as the text field's, B-61).
            if (scale >
                1f
            ) {
                it.cssLine(MutantFaceMetrics.Rubik)
            } else {
                it.cssLine(
                    MutantFaceMetrics.Rubik,
                    CELL.toFloat() / DAY_SIZE,
                )
            }
        }
    }
    val light = dayStyle(FontWeight.Normal, colors.text)
    val dark = dayStyle(FontWeight.Bold, colors.onPrimary)
    Layout(
        content = {
            if (chosen) {
                MutantSurface(
                    MutantBody.Pill,
                    "$key-day",
                    Modifier.size(CELL.dp),
                    profile = MutantBody.Pill.profile.with(radius = DROP_RADIUS),
                    slide = SlideRequest(from.run, from.cols * cellDp[0], from.rows * cellDp[0]),
                ) {}
            }
            for (d in 1..days) {
                val i = lead + d - 1
                DayCell(
                    d,
                    chosen && d == selectedDay,
                    light,
                    dark,
                    { place?.over(i) },
                    Modifier.focusRequester(focus[d - 1]).onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val to =
                            when (it.key) {
                                Key.DirectionRight -> d + 1
                                Key.DirectionLeft -> d - 1
                                Key.DirectionDown -> d + 7
                                Key.DirectionUp -> d - 7
                                else -> return@onPreviewKeyEvent false
                            }
                        if (to in 1..days) focus[to - 1].requestFocus()
                        true
                    },
                ) { onPick(d) }
            }
        },
    ) { measurables, _ ->
        // Square cells of 36 dp, or of the largest number's own size under a larger font scale.
        val items = measurables.drop(if (chosen) 1 else 0)
        val least = (CELL * density).roundToInt()
        val cell =
            maxOf(
                least,
                items.maxOfOrNull { maxOf(it.maxIntrinsicWidth(least), it.maxIntrinsicHeight(least)) } ?: least,
            )
        cellDp[0] = cell / density
        val rows = (lead + days + 6) / 7
        val drop = if (chosen) measurables.first().measure(Constraints.fixed(cell, cell)) else null
        val cells = items.map { it.measure(Constraints.fixed(cell, cell)) }
        layout(cell * 7, cell * rows) {
            if (drop != null) {
                val i = lead + selectedDay!! - 1
                drop.place(i % 7 * cell, i / 7 * cell)
            }
            cells.forEachIndexed { k, p ->
                val i = lead + k
                p.place(i % 7 * cell, i / 7 * cell)
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    selected: Boolean,
    light: TextStyle,
    dark: TextStyle,
    drop: () -> Rect?,
    moves: Modifier,
    onPick: () -> Unit,
) {
    val colors = MutantTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        moves
            .mutantHand(true)
            .selectable(selected, source, indication = null, role = Role.Button, onClick = onPick)
            .drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(1f, 1f, size.width / density - 1, size.height / density - 1, colors.focus)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // The number twice over the same place: light where the bar is under it, black and bold where the drop is.
        MutantText(
            "$day",
            Modifier.underDrop(drop, onDrop = false).mutantCleanZone(),
            style = light,
            maxLines = 1,
            softWrap = false,
        )
        // As large as the cell and never measured into it, so the bold number does not size the grid.
        Box(Modifier.matchParentSize().clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            MutantText("$day", Modifier.underDrop(drop, onDrop = true), style = dark, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * The part of a number on the drop ([onDrop]) or off it, as the drop stands now: [drop] is its oval from the number's
 * middle, dp, or null where there is none. A number wholly on one side is drawn as it is, unclipped, so the resting
 * calendar draws as before; one the drop's edge crosses is cut along the oval.
 */
private fun Modifier.underDrop(
    drop: () -> Rect?,
    onDrop: Boolean,
): Modifier =
    drawWithContent {
        val oval =
            drop()?.let {
                Rect(
                    size.width / 2 + it.left * density,
                    size.height / 2 + it.top * density,
                    size.width / 2 + it.right * density,
                    size.height / 2 + it.bottom * density,
                )
            }
        // The digits' ink: the number's width, and its figures' height about the line's middle.
        val hw = size.width / 2
        val hh = DIGIT_HALF.dp.toPx()
        val cx = size.width / 2
        val cy = size.height / 2
        val inside =
            oval != null &&
                inOval(oval, cx - hw, cy - hh) &&
                inOval(oval, cx + hw, cy - hh) &&
                inOval(oval, cx - hw, cy + hh) &&
                inOval(oval, cx + hw, cy + hh)
        val outside =
            oval == null ||
                !inOval(oval, oval.center.x.coerceIn(cx - hw, cx + hw), oval.center.y.coerceIn(cy - hh, cy + hh))
        when {
            inside -> {
                if (onDrop) drawContent()
            }

            outside -> {
                if (!onDrop) drawContent()
            }

            else -> {
                val path = Path().apply { addOval(oval) }
                clipPath(
                    path,
                    if (onDrop) ClipOp.Intersect else ClipOp.Difference,
                ) { this@drawWithContent.drawContent() }
            }
        }
    }

private fun inOval(
    o: Rect,
    x: Float,
    y: Float,
): Boolean {
    val dx = (x - o.center.x) / (o.width / 2)
    val dy = (y - o.center.y) / (o.height / 2)
    return o.width > 0 && o.height > 0 && dx * dx + dy * dy <= 1
}

/**
 * Where the drop is drawn now, from the middle of cell [over], dp: its oval as the body slides it (`slid`: run home by
 * `1 − (1 − p)²`, stretched `1 + .4 sin πp` along its run and squeezed `1 − .18 sin πp` across), half its outline inside.
 * While it runs, the reader reads every frame of the [clock].
 */
private class DropPlace(
    private val from: DropFrom,
    private val clock: MutantClock,
    private val chosen: Int,
    private val cell: FloatArray,
    private val instant: Boolean,
    private val steps: Boolean,
) {
    fun over(i: Int): Rect {
        val p = if (instant) 1.0 else stepped(clock.nowMs - from.since, MutantMotion.SLIDE_MS, steps)
        if (p < 1) clock.frame
        val q = sin(PI * p)
        val left = (1 - p).pow(2)
        val c = cell[0].toDouble()
        val x = c * (chosen % 7 - i % 7 + from.cols * left)
        val y = c * (chosen / 7 - i / 7 + from.rows * left)
        val along = jsToFixed(1 + .4 * q, 2)
        val across = jsToFixed(1 - .18 * q, 2)
        val down = abs(from.rows) > abs(from.cols)
        val inset = MutantBody.Pill.profile.outline / 2
        val hw = c / 2 * (if (down) across else along) - inset
        val hh = c / 2 * (if (down) along else across) - inset
        return Rect((x - hw).toFloat(), (y - hh).toFloat(), (x + hw).toFloat(), (y + hh).toFloat())
    }
}

/** A month arrow: a filled triangle, the page's `◄`/`►`, in a target of 48 dp at least. */
@Composable
private fun Arrow(
    back: Boolean,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    val colors = MutantTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        Modifier
            // Laid out at the glyph's size, where the page's glyph stands. Its target is 48 dp square and meets the
            // other arrow's halfway across the 6 dp between them, reaching outward: the glyphs stand 18 dp apart,
            // and targets centred on them would overlap, the later one taking the earlier one's presses.
            .layout { m, _ ->
                val target = ARROW_TARGET.dp.roundToPx()
                val p = m.measure(Constraints.fixed(target, target))
                val w = ARROW_W.dp.roundToPx()
                val h = ARROW_H.dp.roundToPx()
                val half = (ARROW_GAP.dp / 2).roundToPx()
                layout(w, h) { p.place(if (back) w + half - target else -half, (h - target) / 2) }
            }.mutantHand(true)
            .selectable(false, source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .drawWithContent {
                drawContent()
                if (focused && keyboard) {
                    scale(density, Offset.Zero) {
                        drawCornerBrackets(-2f, -2f, size.width / density + 2, size.height / density + 2, colors.focus)
                    }
                }
            },
        contentAlignment = if (back) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        // A pixel under the row's middle, where the host's glyph sits on the page; at its end of the target.
        Canvas(
            Modifier
                .offset(y = 1.dp)
                .padding(start = if (back) 0.dp else (ARROW_GAP / 2).dp, end = if (back) (ARROW_GAP / 2).dp else 0.dp)
                .size(ARROW_W.dp, ARROW_H.dp)
                .mutantCleanZone(),
        ) {
            val w = size.width
            val h = size.height
            val p =
                Path().apply {
                    if (back) {
                        moveTo(w, 0f)
                        lineTo(0f, h / 2)
                        lineTo(w, h)
                    } else {
                        moveTo(0f, 0f)
                        lineTo(w, h / 2)
                        lineTo(0f, h)
                    }
                    close()
                }
            drawPath(p, color)
        }
    }
}

/**
 * A time picker, the recording timer's: the hours and the minutes, each in a field (body `field`, 76 × 72 dp, drips to
 * 14 dp) in Handjet 52 semibold, two digits, a colon between them in the on-screen-display amber, 10 dp apart. The field
 * being set is acid with black digits (the page's «часы выбраны, минуты ждут»); a press on the other makes it the one.
 * Up and Down step the one focused, wrapping (23 → 0, 59 → 0), and each reads to a screen reader as a value in its
 * range that can be set; [hourLabel] and [minuteLabel] name them. Designed here (D15): the design is a still.
 */
@Composable
public fun MutantTimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    hourLabel: String = "Часы",
    minuteLabel: String = "Минуты",
    key: String = "tp",
) {
    TimePickerBody(hour, minute, onTimeChange, modifier, hourLabel, minuteLabel, key)
}

/** [MutantTimePicker]; [active] starts on the hours (0) or the minutes (1), [hidden] draws the digits transparent. */
@Composable
internal fun TimePickerBody(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    modifier: Modifier,
    hourLabel: String,
    minuteLabel: String,
    key: String,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    var active by remember { mutableIntStateOf(0) }
    var colonWidth by remember { mutableFloatStateOf(0f) }
    val change by rememberUpdatedState(onTimeChange)
    val h = hour.mod(24)
    val m = minute.mod(60)
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(TIME_GAP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeField(h, 24, active == 0, hourLabel, "$key-h", hidden, { active = 0 }) { change(it, m) }
        MutantText(
            ":",
            // Chrome lays the colon at its fractional width and the next field on the pixel nearest; Compose would
            // round the width up and the minutes would stand a pixel late (as B-44's chips).
            Modifier
                .layout { m, c ->
                    val p = m.measure(c)
                    val w = colonWidth.takeIf { it > 0f }?.roundToInt() ?: p.width
                    layout(w, p.height) { p.place(0, 0) }
                }.mutantCleanZone(),
            onTextLayout = { colonWidth = it.getLineRight(0) - it.getLineLeft(0) },
            style =
                TextStyle(
                    fontFamily = fonts?.display,
                    fontSize = DIGITS_SIZE.sp,
                    color = if (hidden) Color.Transparent else colors.textOsd,
                    platformStyle = raster,
                ).cssLine(MutantFaceMetrics.Handjet),
        )
        TimeField(m, 60, active == 1, minuteLabel, "$key-m", hidden, { active = 1 }) { change(h, it) }
    }
}

@Composable
private fun TimeField(
    value: Int,
    count: Int,
    active: Boolean,
    label: String,
    key: String,
    hidden: Boolean,
    onActivate: () -> Unit,
    onValue: (Int) -> Unit,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    MutantSurface(
        MutantBody.Field,
        key,
        // The page's 76 × 72; a larger font scale grows the field to its digits.
        Modifier
            .widthIn(min = FIELD_W.dp)
            .heightIn(min = FIELD_H.dp)
            .mutantHand(true)
            .selectable(active, source, indication = null, role = Role.Button, onClick = onActivate)
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (it.key) {
                    Key.DirectionUp -> onValue((value + 1).mod(count))
                    Key.DirectionDown -> onValue((value - 1).mod(count))
                    else -> return@onPreviewKeyEvent false
                }
                true
            }.semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..(count - 1).toFloat(), count - 2)
                setProgress { v ->
                    onValue(v.roundToInt().coerceIn(0, count - 1))
                    true
                }
            },
        fill = if (active) colors.primary else null,
        focused = focused && keyboard,
        interactionSource = source,
        profile = MutantBody.Field.profile.with(overhang = FIELD_OVERHANG),
    ) {
        Box(Modifier.widthIn(min = FIELD_W.dp).heightIn(min = FIELD_H.dp), contentAlignment = Alignment.Center) {
            MutantText(
                value.toString().padStart(2, '0'),
                Modifier.mutantCleanZone(),
                style =
                    TextStyle(
                        fontFamily = fonts?.display,
                        fontSize = DIGITS_SIZE.sp,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            when {
                                hidden -> Color.Transparent
                                active -> colors.onPrimary
                                else -> colors.text
                            },
                        platformStyle = raster,
                    ).cssLine(MutantFaceMetrics.Handjet),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** How many days [month] (1–12) of [year] has, leap years in the Gregorian calendar. */
internal fun daysIn(
    year: Int,
    month: Int,
): Int =
    when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

/** The weekday [month] (1–12) of [year] begins on, Monday 0 … Sunday 6 (Sakamoto's method). */
internal fun firstWeekday(
    year: Int,
    month: Int,
): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val y = if (month < 3) year - 1 else year
    val sunday0 = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + 1) % 7
    return (sunday0 + 6) % 7
}

private val MONTHS =
    listOf(
        "ЯНВАРЬ",
        "ФЕВРАЛЬ",
        "МАРТ",
        "АПРЕЛЬ",
        "МАЙ",
        "ИЮНЬ",
        "ИЮЛЬ",
        "АВГУСТ",
        "СЕНТЯБРЬ",
        "ОКТЯБРЬ",
        "НОЯБРЬ",
        "ДЕКАБРЬ",
    )
private val WEEKDAYS = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

/** The calendar (`width: 284px; padding: 14px 16px 18px; gap: 8px`, `data-r="12"`) and its cells (36). */
private const val WIDTH = 284
private const val TOP = 14
private const val SIDE = 16
private const val FOOT = 18
private const val GAP = 8
private const val RADIUS = 12.0
private const val CELL = 36
private const val DROP_RADIUS = 18.0

/** Half the height of the days' figures about their line's middle, dp: Rubik 14's figures, 10 dp, and a pixel. */
private const val DIGIT_HALF = 5.5f

/**
 * Follows the chosen day from composition without writing state (B-93): where the drop's run starts, in cells from the
 * chosen cell, and since when. A pick while the drop is under way starts the new run from the place it has reached, not
 * from the day picked before (B-168). A new month moves the drop without a run.
 */
private class DropFrom(
    private var now: Int,
    private var lead: Int,
    private var days: Int,
) {
    /** The run's start, columns and rows from the chosen cell; 0 when it does not run. */
    var cols: Double = 0.0
        private set
    var rows: Double = 0.0
        private set

    /** When the run started, the screen's clock ms. */
    var since: Double = Double.NEGATIVE_INFINITY
        private set

    /** Which run this is: the body's slide restarts on a new one only. */
    var run: Int = 0
        private set

    fun follow(
        to: Int,
        lead: Int,
        days: Int,
        nowMs: Double,
        instant: Boolean,
        steps: Boolean,
    ) {
        if (lead != this.lead || days != this.days) {
            // Another month: the drop stands on its day there, and a run under way goes on as the body draws it.
            this.lead = lead
            this.days = days
            now = to
            return
        }
        if (to == now) return
        if (now in 1..days && to in 1..days) {
            // What is left of the run under way, as the body draws it: `(1 − p)²` of its start.
            val left = if (instant) 0.0 else (1 - stepped(nowMs - since, MutantMotion.SLIDE_MS, steps)).pow(2)
            val a = lead + now - 1
            val b = lead + to - 1
            cols = a % 7 - b % 7 + cols * left
            rows = a / 7 - b / 7 + rows * left
            since = nowMs
            run++
        } else {
            cols = 0.0
            rows = 0.0
        }
        now = to
    }
}

/** The month (Handjet `22px`, tracked 1), the weekdays (Handjet `16px`) and the days (`14px`), sp. */
private const val MONTH_SIZE = 22
private const val TRACKING = 1f
private const val WEEKDAY_SIZE = 16
private const val DAY_SIZE = 14

/**
 * The arrows: the page's `◄ ►`, glyphs of the host's face (Handjet has none), measured on its references — each 12 × 8,
 * 6 dp apart, 2 dp from the row's end — and drawn as triangles, each in a 48 dp target; the row they make 25 dp tall.
 */
private const val ARROW_W = 12
private const val ARROW_H = 8
private const val ARROW_TARGET = 48
private const val ARROW_GAP = 6
private const val ARROW_END = 2
private const val HEADER = 25

/** The time fields (`76 × 72`, `data-over="14"`), the digits (Handjet `52px`, 600) and the gap (`gap: 10px`). */
private const val FIELD_W = 76
private const val FIELD_H = 72
private const val FIELD_OVERHANG = 14.0
private const val DIGITS_SIZE = 52
private const val TIME_GAP = 10
