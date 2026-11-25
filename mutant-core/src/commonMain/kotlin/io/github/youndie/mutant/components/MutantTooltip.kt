package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.MutantTune
import io.github.youndie.mutant.surface.MutantTuned
import io.github.youndie.mutant.surface.composed
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.mutantPlaceKey
import io.github.youndie.mutant.surface.mutantTapeOver
import io.github.youndie.mutant.surface.tuneStands
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A tooltip (v1): an on-screen hint whose plate flows down onto what it speaks of. Over its [anchor] stands a `blob`
 * plate (r 6, drips of 20 hanging towards the anchor): one line of OSD type on the blue plate, 30 dp tall; with a
 * [supportingText], two lines on the amber plate, 44 dp tall. It is centred over the anchor, its bottom 28 dp above it
 * (22 for the two-line plate), as the page stands them.
 *
 * At the window's edges (B-200, designed here): the plate shifts along the edge to stand 8 dp inside the window, still
 * over its anchor; where it does not fit above, it stands below the anchor, its top 8 dp under it, its drips still
 * hanging down. It is at most 240 dp wide (and 16 dp narrower than the window): a longer hint wraps, to at most three
 * lines in all — 30, 44 and 58 dp tall — and the last ends in an ellipsis. On the amber plate the first line stays one
 * line and the supporting text takes the other two.
 *
 * The design draws it shown, not appearing; this is designed here (D15). It shows while a mouse hovers the anchor, while
 * the anchor holds keyboard focus, and after a finger's long press, until 1.5 s after it lifts. Its plate tunes in as
 * it shows (a 16 dp mosaic, an 8 dp mosaic, then the plate and its words, 320 ms in three steps) and steps out as it
 * hides (the 8 dp mosaic, then nothing, 160 ms), as [io.github.youndie.mutant.surface.MutantAppear] does; under reduce
 * motion it shows and goes at once; a finger that moves past the touch slop first (a scroll starting on the anchor) is no long press. Focus taken by
 * a press shows nothing. The plate takes no input: a press through it reaches what lies under it.
 *
 * [key] seeds its body: by default its place in the composition and its body type (B-206), so two unkeyed ones are
 * two bodies and one keeps its body whatever its words or state; in a list, give it from the item's id.
 */
@Composable
public fun MutantTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    key: String = mutantPlaceKey("tip"),
    anchor: @Composable () -> Unit,
) {
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val longPress = LocalViewConfiguration.current.longPressTimeoutMillis
    var hovered by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    var pressing by remember { mutableStateOf(false) }
    // Whether the focus the anchor holds came with a press: a pointer, not the keyboard.
    var pressed by remember { mutableStateOf(false) }
    var byPress by remember { mutableStateOf(false) }
    // B-200: where the anchor stands in the window, unclipped, for the plate to keep inside the window.
    var anchorRect by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier
            .onGloballyPositioned {
                anchorRect = Rect(it.positionInWindow(), it.size.toSize())
            }.onFocusChanged {
                if (it.hasFocus && !focused) byPress = pressed
                if (!it.hasFocus) pressed = false
                focused = it.hasFocus
            }.pointerInput(Unit) {
                coroutineScope {
                    var lift: kotlinx.coroutines.Job? = null
                    // Where a finger came down: past the touch slop it scrolls or drags, and is no long press.
                    var down = Offset.Unspecified
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            val p = e.changes.firstOrNull() ?: continue
                            when (e.type) {
                                PointerEventType.Enter -> {
                                    if (p.type == PointerType.Mouse) hovered = true
                                }

                                PointerEventType.Exit -> {
                                    if (p.type == PointerType.Mouse) hovered = false
                                }

                                PointerEventType.Press -> {
                                    pressing = true
                                    pressed = true
                                    if (p.type == PointerType.Touch) {
                                        down = p.position
                                        lift?.cancel()
                                        lift =
                                            launch {
                                                delay(longPress)
                                                held = true
                                            }
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (pressing && !held && down.isSpecified &&
                                        (p.position - down).getDistance() > viewConfiguration.touchSlop
                                    ) {
                                        lift?.cancel()
                                        down = Offset.Unspecified
                                    }
                                }

                                PointerEventType.Release -> {
                                    pressing = false
                                    down = Offset.Unspecified
                                    lift?.cancel()
                                    if (held) {
                                        lift =
                                            launch {
                                                delay(TOUCH_LINGER_MS)
                                                held = false
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            },
    ) {
        anchor()
        // B-179: the plate tunes in and steps out (the designer's answer 1), kept in its popup while it leaves.
        val shown = hovered || held || (focused && keyboard && !byPress)
        val clock = readMutantClock()
        val still = tuneStands(clock)
        val tune = remember { MutantTune(shown, clock.nowMs, play = true) }
        tune.follow(shown, clock.nowMs, still)
        if (tune.composed(clock, still)) TooltipPopup(text, supportingText, key, tune, leaving = !shown) { anchorRect }
    }
}

/**
 * The plate over the anchor in a popup of no size: it draws past its bounds and takes no press (plakat-ui B-72). The
 * popup's point is the anchor's top middle; the plate is placed from it so that it stands inside the window (B-200).
 */
@Composable
private fun TooltipPopup(
    text: String,
    supportingText: String?,
    key: String,
    tune: MutantTune,
    leaving: Boolean,
    anchor: () -> Rect?,
) {
    val gap = if (supportingText == null) GAP_PLAIN else GAP_RICH
    val window = LocalWindowInfo.current.containerSize
    Popup(
        popupPositionProvider = AboveTheMiddle,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Layout(content = {
            // B-197: the window's Tape over the plate, in register with it (the designer's answer 22).
            Box(Modifier.mutantTapeOver(TOOLTIP_TAPE_REACH.dp)) {
                MutantTuned(tune, leaving = leaving) { TooltipPlate(text, supportingText, key, Modifier) }
            }
        }) { measurables, _ ->
            val margin = EDGE_MARGIN.dp.roundToPx()
            // Never wider than the window keeps room for: the plate's own 240 dp limit is the plate's.
            val room = if (window.width > 0) max(0, window.width - 2 * margin) else Constraints.Infinity
            val p = measurables.first().measure(Constraints(maxWidth = room))
            layout(0, 0) {
                val above = IntOffset(-p.width / 2, -gap.dp.roundToPx() - p.height)
                val at = anchor()
                val origin = coordinates?.positionInWindow()
                if (at == null || origin == null || window.width <= 0 || window.height <= 0) {
                    p.place(above)
                } else {
                    val spot =
                        tooltipSpot(
                            at,
                            IntSize(p.width, p.height),
                            window,
                            margin,
                            gapAbove = gap.dp.roundToPx(),
                            gapBelow = GAP_BELOW.dp.roundToPx(),
                        )
                    p.place(spot.x - origin.x.roundToInt(), spot.y - origin.y.roundToInt())
                }
            }
        }
    }
}

/**
 * Where a plate of [plate] stands in a window of [window] for an anchor at [anchor], all in window pixels (B-200): centred
 * over the anchor and [gapAbove] above it; shifted along the edge to stand [margin] inside the window; below the anchor,
 * [gapBelow] under it, only where it does not fit above and does below.
 */
internal fun tooltipSpot(
    anchor: Rect,
    plate: IntSize,
    window: IntSize,
    margin: Int,
    gapAbove: Int,
    gapBelow: Int,
): IntOffset {
    val middle = (anchor.left + anchor.width / 2).roundToInt()
    val x = (middle - plate.width / 2).coerceIn(margin, max(margin, window.width - margin - plate.width))
    val top = anchor.top.roundToInt() - gapAbove - plate.height
    val under = anchor.bottom.roundToInt() + gapBelow
    val y = if (top < margin && under + plate.height <= window.height - margin) under else top
    return IntOffset(x, y)
}

/** The popup's point: the anchor's top middle. */
private object AboveTheMiddle : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(anchorBounds.left + anchorBounds.width / 2, anchorBounds.top)
}

/** The plate alone: blue with one line, or amber with two. [hidden] draws its words transparent, a bare twin's. */
@Composable
internal fun TooltipPlate(
    text: String,
    supportingText: String?,
    key: String,
    modifier: Modifier,
    hidden: Boolean = false,
) {
    val colors = MutantTheme.colors
    val styles = MutantTheme.textStyles
    val rich = supportingText != null
    val ink =
        if (hidden) {
            Color.Transparent
        } else if (rich) {
            colors.onPrimary
        } else {
            colors.onNoSignal
        }
    val raster = LocalMutantTextRaster.current
    // B-200: the lines the words were laid out in, read as they are measured, to set the plate's height by them.
    val lines = remember { IntArray(2) }
    val first = remember { { r: TextLayoutResult -> lines[0] = r.lineCount } }
    val second = remember { { r: TextLayoutResult -> lines[1] = r.lineCount } }
    MutantSurface(
        MutantBody.Blob,
        key,
        modifier.widthIn(max = MAX_WIDTH.dp).heightIn(min = if (rich) RICH_HEIGHT.dp else PLAIN_HEIGHT.dp),
        fill = if (rich) colors.amber else colors.noSignal,
        // The page's `data-r="6" data-over="20" data-cf="0" data-marks="0" data-edge="0" data-ow="2.5"`.
        profile =
            MutantBody.Blob.profile.with(
                radius = RADIUS,
                overhang = OVERHANG,
                dripShare = 0.0,
                outline = OUTLINE,
                marks = false,
                mosaic = false,
            ),
    ) {
        // `line-height: 1`, Handjet 18 (and 16 for the supporting text). The plate is 30, 44 or 58 dp tall for one, two
        // or three lines, the words centred in it; a taller line (font scale 2) grows it past that.
        Column(
            Modifier
                .layout { m, c ->
                    val p = m.measure(c.copy(minHeight = 0))
                    val n = if (rich) 1 + lines[1] else lines[0]
                    val least = (PLAIN_HEIGHT + LINE_STEP * (n.coerceIn(1, MAX_LINES) - 1)).dp.roundToPx()
                    val h = max(p.height, least).coerceIn(c.minHeight, c.maxHeight)
                    layout(p.width, h) { p.place(0, (h - p.height) / 2) }
                }.padding(horizontal = PADDING.dp),
        ) {
            if (rich) {
                MutantText(
                    text,
                    Modifier.mutantCleanZone(),
                    style =
                        styles
                            .osd(
                                18.sp,
                            ).cssLine(MutantFaceMetrics.Handjet, 1f)
                            .copy(color = ink, platformStyle = raster),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                MutantText(
                    supportingText.orEmpty(),
                    Modifier.mutantCleanZone(),
                    style =
                        styles
                            .osd(
                                16.sp,
                            ).cssLine(MutantFaceMetrics.Handjet, 1f)
                            .copy(color = ink, platformStyle = raster),
                    onTextLayout = second,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = MAX_LINES - 1,
                )
            } else {
                MutantText(
                    text,
                    Modifier.mutantCleanZone(),
                    style =
                        styles
                            .osd(
                                18.sp,
                            ).cssLine(MutantFaceMetrics.Handjet, 1f)
                            .copy(color = ink, platformStyle = raster),
                    onTextLayout = first,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = MAX_LINES,
                )
            }
        }
    }
}

/** The plates: 30 and 44 dp tall, text 12 dp in, r 6, drips of 20, outline 2.5. */
private const val PLAIN_HEIGHT = 30
private const val RICH_HEIGHT = 44
private const val PADDING = 12
private const val RADIUS = 6.0
private const val OVERHANG = 20.0
private const val OUTLINE = 2.5

/** From the plate's bottom to the anchor's top, as the page stands its two tooltips. */
private const val GAP_PLAIN = 28
private const val GAP_RICH = 22

/** B-200, the designer's answers 26 and 27: 8 dp from the window; at most 240 dp wide and three lines, 30/44/58 dp. */
private const val EDGE_MARGIN = 8
private const val MAX_WIDTH = 240
private const val MAX_LINES = 3
private const val LINE_STEP = 14

/** Below the anchor (designed here): the plate's top 8 dp under it, the room its drips' tips keep above it. */
private const val GAP_BELOW = 8

/** How far the plate draws past its box, dp, for the Tape over it (B-197): drips of 20 and the outline. */
private const val TOOLTIP_TAPE_REACH = 28

/** How long a long-pressed tooltip stays after the finger lifts (designed here). */
private const val TOUCH_LINGER_MS = 1500L
