package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.drawCornerBrackets
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.stepped
import io.github.youndie.mutant.surface.with
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.roundToInt

/**
 * A dialog: a clean window over a paused screen. Place it last in a screen-filling box. While it shows, the screen's
 * clock is held — everything behind it stands still (the brief's `pause`) — under a 62 % black scrim and a pause band
 * across the screen 56 dp above its foot; the window, the `bar` body as a clean frame with 12 dp corners, 280 dp wide
 * at most and 24 from the screen's sides, centred, holds the [title] (Handjet 22), the [text] (Rubik 14) and the
 * [actions] at its end, usually [MutantDialogAction]s.
 *
 * With [visible] true, from the start or set later, it opens: scrim, band and window come up together in 320 ms
 * (`pause`); set false they go in 200 (`play`), the screen's clock running again, and it is gone from the composition
 * after. Both step twelve times a second, and under reduce motion are at once. A click on the scrim, Escape or the
 * system back gesture calls [onDismissRequest]. Focus moves into the dialog as it opens and stays there; on closing it
 * goes to [returnFocusTo], a requester on whatever opened it, or with none is left to the window.
 */
@Composable
public fun MutantDialog(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    returnFocusTo: FocusRequester? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    DialogFrame(visible, title, onDismissRequest, modifier, text, returnFocusTo, actions)
}

/**
 * [MutantDialog]; [playIn] false stands it open from the first frame and [windowTop] places its window there instead of
 * centring it (a fixture's still, at the page's place); [hidden] draws its words transparent for a bare twin.
 */
@Composable
internal fun DialogFrame(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    text: String?,
    returnFocusTo: FocusRequester?,
    actions: @Composable RowScope.() -> Unit,
    playIn: Boolean = true,
    windowTop: Dp? = null,
    hidden: Boolean = false,
) {
    ReturnFocus(visible, returnFocusTo)
    val reduce = LocalMutantReduceMotion.current
    val screen = readMutantClock()
    // The dialog's own clock: the screen's stands while it shows, and the window and its button must still move.
    // A frozen screen (a still, a fixture) freezes it too.
    val own = remember { MutantClock(startMs = screen.nowMs, frozen = screen.frozen) }
    SideEffect { own.paused = reduce }
    val follow = remember { DialogFollow(visible, if (playIn) own.nowMs else Double.NEGATIVE_INFINITY) }
    ProvideMutantClock(own) {
        val clock = readMutantClock()
        follow.follow(visible, clock.nowMs)
        val duration = if (visible) OPEN_MS else CLOSE_MS
        val p = if (reduce) 1.0 else stepped(clock.nowMs - follow.since, duration)
        if (p < 1) clock.frame
        val shown = if (visible) p else 1 - p
        val gone = !visible && p >= 1
        // Held while anything of it shows: the screen behind stands still.
        DisposableEffect(screen, gone) {
            if (!gone) screen.holds++
            onDispose { if (!gone) screen.holds-- }
        }
        if (gone) return@ProvideMutantClock
        DialogScene(visible, title, onDismissRequest, modifier, text, actions, shown.toFloat(), windowTop, hidden)
    }
}

@Composable
private fun DialogScene(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    text: String?,
    actions: @Composable RowScope.() -> Unit,
    shown: Float,
    windowTop: Dp?,
    hidden: Boolean,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val dismiss by rememberUpdatedState(onDismissRequest)
    val focus = remember { FocusRequester() }
    LaunchedEffect(visible) { if (visible) focus.requestFocus() }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = visible) { dismiss() }
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer { alpha = shown }
            // Focus stays in here while it is open, and what is under the scrim cannot be reached by the keys.
            .focusProperties { onExit = { if (visible) cancelFocusChange() } }
            .focusGroup()
            .focusRequester(focus)
            .onPreviewKeyEvent {
                if (visible && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                    dismiss()
                    true
                } else {
                    false
                }
            }.focusable(),
    ) {
        // The scrim: it takes every click under the window, and a click on it dismisses.
        Box(
            Modifier
                .fillMaxSize()
                .background(SCRIM)
                .clickable(remember { MutableInteractionSource() }, indication = null, enabled = visible) { dismiss() },
        )
        PauseBand(Modifier.align(Alignment.BottomStart).offset(BAND_SHIFT, -BAND_FROM_FOOT))
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = SIDE),
            contentAlignment = if (windowTop == null) Alignment.Center else Alignment.TopCenter,
        ) {
            MutantSurface(
                MutantBody.Bar,
                WINDOW_KEY,
                (if (windowTop == null) Modifier else Modifier.offset(y = windowTop))
                    .widthIn(max = WINDOW)
                    .fillMaxWidth()
                    // Its own clicks: none reaches the scrim under it.
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .semantics {
                        dialog()
                        paneTitle = title
                        dismiss {
                            dismiss()
                            true
                        }
                    },
                // The engine's `data-state="clean"`: the clean frame, with no print slip.
                state = BodyState.Success,
                slip = 0.0,
                profile = MutantBody.Bar.profile.with(radius = WINDOW_RADIUS),
            ) {
                Column(
                    Modifier.padding(start = PAD, top = PAD, end = PAD, bottom = PAD_FOOT),
                    verticalArrangement = Arrangement.spacedBy(GAP),
                ) {
                    BasicText(
                        title,
                        Modifier.semantics { heading() }.mutantCleanZone(),
                        style =
                            TextStyle(
                                fontFamily = fonts?.display,
                                fontSize = TITLE_SIZE.sp,
                                color = if (hidden) Color.Transparent else colors.text,
                                platformStyle = raster,
                            ).cssLine(MutantFaceMetrics.Handjet),
                    )
                    if (text != null) {
                        CssParagraph(
                            text,
                            TextStyle(
                                fontFamily = fonts?.text,
                                fontSize = TEXT_SIZE.sp,
                                color = if (hidden) Color.Transparent else colors.textSecondary,
                                platformStyle = raster,
                            ).cssLine(MutantFaceMetrics.Rubik, TEXT_LINE),
                            TEXT_SIZE * TEXT_LINE,
                        )
                    }
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalDialogHidden provides hidden,
                        LocalDialogOpen provides visible,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = ACTIONS_TOP),
                            horizontalArrangement = Arrangement.spacedBy(ACTIONS_GAP, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A paragraph whose box is as tall as Chrome's: its lines × [lineDp] (a fractional `line-height`, 20.3 for 14 × 1.45),
 * rounded as Chrome paints what follows it. Compose lays each line whole, so two lines stood 40 where Chrome's stand
 * 40.6, and the dialog's button a pixel high.
 */
@Composable
private fun CssParagraph(
    text: String,
    style: TextStyle,
    lineDp: Float,
) {
    val lines = remember { IntArray(1) }
    Layout(
        content = {
            BasicText(text, Modifier.mutantCleanZone(), style = style, onTextLayout = { lines[0] = it.lineCount })
        },
    ) { measurables, constraints ->
        // The text tells its line count as it measures, before this box takes its height.
        val p = measurables.single().measure(constraints)
        val h = maxOf(p.height, (lines[0] * lineDp * density).roundToInt())
        layout(p.width, h) { p.place(0, 0) }
    }
}

/** The pause band: a faint light strip with a light edge above and a dark one below, the page's literals. */
@Composable
private fun PauseBand(modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(BAND + 2.dp)
            .drawBehind {
                scale(density, Offset.Zero) {
                    val w = size.width / density
                    drawRect(BAND_EDGE_LIGHT, Offset(0f, 0f), Size(w, 1f))
                    drawRect(BAND_FILL, Offset(0f, 1f), Size(w, BAND.value))
                    drawRect(BAND_EDGE_DARK, Offset(0f, 1f + BAND.value), Size(w, 1f))
                }
            },
    )
}

/**
 * An action of [MutantDialog]: [primary], the engine's `btn` body in the acid, at least 104 × 40 dp in a 48 dp target,
 * its label in black (the page's `Стереть`); otherwise the label alone in the accent, 8 dp in, a target 48 tall and at
 * least 48 wide (`Отмена`). Both fire on release; a press boils the body; keyboard focus draws the corner brackets.
 */
@Composable
public fun MutantDialogAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    key: String = "dialog-ok",
) {
    val colors = MutantTheme.colors
    val hidden = LocalDialogHidden.current
    // From the moment its dialog starts to close, an action no longer answers.
    val open = LocalDialogOpen.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val style =
        TextStyle(
            fontFamily = LocalMutantFonts.current?.text,
            fontWeight = FontWeight.Medium,
            fontSize = ACTION_SIZE.sp,
            platformStyle = LocalMutantTextRaster.current,
        ).cssLine(MutantFaceMetrics.Rubik)
    Box(
        modifier
            .clickable(source, indication = null, enabled = open, role = Role.Button, onClick = onClick)
            .height(TOUCH)
            .widthIn(min = TOUCH)
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
        if (primary) {
            MutantSurface(
                MutantBody.Btn,
                key,
                Modifier
                    .wrapContentSize()
                    .defaultMinSize(
                        minWidth = BUTTON_W,
                        minHeight = BUTTON_H,
                    ).height(BUTTON_H)
                    .mutantCleanZone(),
                interactionSource = source,
                // The page's `data-over="8"`: the dialog keeps its button's drips short.
                profile = MutantBody.Btn.profile.with(overhang = BUTTON_OVERHANG),
            ) {
                Box(
                    Modifier.defaultMinSize(minWidth = BUTTON_W).height(BUTTON_H).padding(horizontal = BUTTON_PAD),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        label,
                        Modifier.mutantCleanZone(),
                        style = style.copy(color = if (hidden) Color.Transparent else colors.onPrimary),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            BasicText(
                label,
                Modifier.padding(horizontal = PLAIN_PAD).mutantCleanZone(),
                style = style.copy(color = if (hidden) Color.Transparent else colors.textAccent),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Gives focus to [target] when an overlay that held it closes. Compose offers no way to read where focus stood before the
 * overlay took it, so the opener has to be named, or focus goes nowhere (plakat-ui's B-60).
 */
@Composable
internal fun ReturnFocus(
    visible: Boolean,
    target: FocusRequester?,
) {
    var opened by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) {
            opened = true
        } else if (opened) {
            opened = false
            target?.requestFocus()
        }
    }
}

/** Follows [visible] from composition without writing state: a change starts the open or the close then. */
internal class DialogFollow(
    private var shown: Boolean,
    start: Double,
) {
    var since: Double = if (shown) start else Double.NEGATIVE_INFINITY
        private set

    fun follow(
        visible: Boolean,
        now: Double,
    ) {
        if (visible == shown) return
        shown = visible
        since = now
    }
}

/** Whether the dialog around an action is open, not closing: its actions answer only then. */
internal val LocalDialogOpen = androidx.compose.runtime.staticCompositionLocalOf { true }

/** A fixture's bare twin, for the actions it composes: their words transparent. False in the kit. */
internal val LocalDialogHidden = androidx.compose.runtime.staticCompositionLocalOf { false }

/** The window: 280 dp at most, 24 from the screen's sides, 12 dp corners (`data-r="12"`), the engine's key. */
private val WINDOW = 280.dp
private val SIDE = 24.dp
private const val WINDOW_RADIUS = 12.0
private const val WINDOW_KEY = "dialog"

/**
 * The window's padding (`20px 20px 18px`) and its parts' gap (`gap: 10px`). The actions' row is the 48 dp target
 * around the 40 dp button the page's row is, so it stands 4 dp higher than the page's `margin-top: 8px` and the foot
 * gives back the other 4.
 */
private val PAD = 20.dp
private val PAD_FOOT = 14.dp
private val GAP = 10.dp
private val ACTIONS_TOP = 4.dp
private val ACTIONS_GAP = 12.dp

/** The title (Handjet `22px`), the text (`14px`, `line-height: 1.45`) and the actions' words (`14px`, 500), sp. */
private const val TITLE_SIZE = 22
private const val TEXT_SIZE = 14
private const val TEXT_LINE = 1.45f
private const val ACTION_SIZE = 14

/** The primary action's body (`104 × 40`, `data-over="8"`), its words 16 dp in when longer; the plain one's 8. */
private val BUTTON_W = 104.dp
private val BUTTON_H = 40.dp
private const val BUTTON_OVERHANG = 8.0
private val BUTTON_PAD = 16.dp
private val PLAIN_PAD = 8.dp
private val TOUCH = 48.dp

/** The scrim: the page's `rgba(5,6,12,.62)`, in both themes. */
private val SCRIM = Color(0x9E05060C)

/**
 * The pause band (the page's `height: 20px` with a 1 dp edge above and below, `translateX(5px)`), 56 dp above the
 * screen's foot as the page's stands in its 262 dp frame: `rgba(233,242,228,.1)`, its edges `.35` and `rgba(5,6,12,.6)`.
 */
private val BAND = 20.dp
private val BAND_SHIFT = 5.dp
private val BAND_FROM_FOOT = 34.dp
private val BAND_FILL = Color(0x1AE9F2E4)
private val BAND_EDGE_LIGHT = Color(0x59E9F2E4)
private val BAND_EDGE_DARK = Color(0x9905060C)

/** The open (`pause`) and the close (`play`), ms. */
private const val OPEN_MS = 320.0
private const val CLOSE_MS = 200.0
