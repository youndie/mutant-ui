package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A tooltip (v1): an on-screen hint whose plate flows down onto what it speaks of. Over its [anchor] stands a `blob`
 * plate (r 6, drips of 20 hanging towards the anchor): one line of OSD type on the blue plate, 30 dp tall; with a
 * [supportingText], two lines on the amber plate, 44 dp tall. It is centred over the anchor, its bottom 28 dp above it
 * (22 for the two-line plate), as the page stands them.
 *
 * The design draws it shown, not appearing; this is designed here (D15). It shows at once, with no entrance, while a
 * mouse hovers the anchor, while the anchor holds keyboard focus, and after a finger's long press, until 1.5 s after it
 * lifts. Focus taken by a press shows nothing. The plate takes no input: a press through it reaches what lies under it.
 */
@Composable
public fun MutantTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    key: String = text,
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
    Box(
        modifier
            .onFocusChanged {
                if (it.hasFocus && !focused) byPress = pressed
                if (!it.hasFocus) pressed = false
                focused = it.hasFocus
            }.pointerInput(Unit) {
                coroutineScope {
                    var lift: kotlinx.coroutines.Job? = null
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
                                        lift?.cancel()
                                        lift =
                                            launch {
                                                delay(longPress)
                                                held = true
                                            }
                                    }
                                }

                                PointerEventType.Release -> {
                                    pressing = false
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
        if (hovered || held || (focused && keyboard && !byPress)) {
            TooltipPopup(text, supportingText, key)
        }
    }
}

/** The plate over the anchor in a popup of no size: it draws past its bounds and takes no press (plakat-ui B-72). */
@Composable
private fun TooltipPopup(
    text: String,
    supportingText: String?,
    key: String,
) {
    val gap = if (supportingText == null) GAP_PLAIN else GAP_RICH
    Popup(
        popupPositionProvider = AboveTheMiddle,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Layout(content = { TooltipPlate(text, supportingText, key, Modifier) }) { measurables, _ ->
            val p = measurables.first().measure(Constraints())
            layout(0, 0) { p.place(-p.width / 2, -gap.dp.roundToPx() - p.height) }
        }
    }
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
    MutantSurface(
        MutantBody.Blob,
        key,
        modifier.heightIn(min = if (rich) RICH_HEIGHT.dp else PLAIN_HEIGHT.dp),
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
        // A taller line (font scale 2) grows the plate; at font scale 1 it is the page's 30 or 44 dp.
        Column(
            Modifier.heightIn(min = if (rich) RICH_HEIGHT.dp else PLAIN_HEIGHT.dp).padding(horizontal = PADDING.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (rich) {
                // `line-height: 1`, the two lines Handjet 18 and 16.
                MutantText(
                    text,
                    Modifier.mutantCleanZone(),
                    style =
                        styles
                            .osd(
                                18.sp,
                            ).cssLine(MutantFaceMetrics.Handjet, 1f)
                            .copy(color = ink, platformStyle = raster),
                    maxLines = 1,
                    softWrap = false,
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
                    maxLines = 1,
                    softWrap = false,
                )
            } else {
                Box(Modifier.heightIn(min = PLAIN_HEIGHT.dp), contentAlignment = Alignment.CenterStart) {
                    MutantText(
                        text,
                        Modifier.mutantCleanZone(),
                        style = styles.osd(18.sp).copy(color = ink, platformStyle = raster),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
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

/** How long a long-pressed tooltip stays after the finger lifts (designed here). */
private const val TOUCH_LINGER_MS = 1500L
