package io.github.youndie.mutant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.SlideRequest
import io.github.youndie.mutant.surface.inkOnBody
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.LocalMutantSteppedMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sign

/** One card of a [MutantCarousel]: its [title], a [subtitle] under it and a [status] line at its foot, in the OSD amber. */
public class MutantCarouselItem(
    public val title: String,
    public val subtitle: String? = null,
    public val status: String? = null,
)

/**
 * A carousel, the page's shelf of cassettes: cards of 220 × 120 dp, 14 dp apart, in a 172 dp view that clips them,
 * the current one 84 dp in so the one before peeks 70 dp and the next fills the rest; under it, 16 dp below, a dot
 * (10 dp) for each card and the pill pointer (26 × 10) at the current one, 8 dp apart.
 *
 * The current card boils as any card; the others stand calm, half its wear (`data-calm=".5"`). A swipe moves the
 * shelf; a press on a card beside the current one brings it to the middle, and a press on the current one goes to
 * [onClick] (designed here, D15: the page is a still); the pointer slides to its new dot as the navigation bar's pill
 * does. Under reduce motion the shelf and the pointer move at once. The cards are a stage of their own, as the page's
 * view is, so their bodies are seeded apart from the screen's.
 */
@Composable
public fun MutantCarousel(
    items: List<MutantCarouselItem>,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState { items.size },
    onClick: ((page: Int) -> Unit)? = null,
    key: String = "car",
    indicatorKey: String = "cd",
) {
    CarouselBody(items, modifier, state, onClick, key, indicatorKey)
}

/** [MutantCarousel]; [hidden] draws the words transparent. */
@Composable
internal fun CarouselBody(
    items: List<MutantCarouselItem>,
    modifier: Modifier,
    state: PagerState,
    onClick: ((Int) -> Unit)?,
    key: String,
    indicatorKey: String,
    hidden: Boolean = false,
) {
    val current = state.currentPage
    BoxWithConstraints(
        modifier
            // With no width given (a scrolling row), the page's view: a pager cannot measure in an endless one.
            .layout { m, c ->
                val p = m.measure(if (c.hasBoundedWidth) c else c.copy(maxWidth = DESIGN_WIDTH.roundToPx()))
                layout(p.width, p.height) { p.place(0, 0) }
            }.semantics { stateDescription = "${current + 1} / ${items.size}" },
    ) {
        CarouselShelf(items, state, onClick, key, indicatorKey, hidden, (maxWidth - START - CARD_W).coerceAtLeast(0.dp))
    }
}

/** The view and the dots under it; [end] is the room after the last card. */
@Composable
private fun CarouselShelf(
    items: List<MutantCarouselItem>,
    state: PagerState,
    onClick: ((Int) -> Unit)?,
    key: String,
    indicatorKey: String,
    hidden: Boolean,
    end: Dp,
) {
    // The cards are seeded by the view's own stage, `<stage>-view`, as the page's `l-carousel-view`.
    val viewStage = "${LocalMutantStage.current}-view"
    val scope = rememberCoroutineScope()
    val instant = LocalMutantReduceMotion.current
    val current = state.currentPage
    // Read by the pointer handlers, which outlive a composition.
    val instantNow by rememberUpdatedState(instant)
    val rtlNow by rememberUpdatedState(LocalLayoutDirection.current == LayoutDirection.Rtl)
    Column(verticalArrangement = Arrangement.spacedBy(INDICATOR_GAP)) {
        CompositionLocalProvider(LocalMutantStage provides viewStage) {
            HorizontalPager(
                state,
                // The view is 172 dp, the card's 120 and the drips' room under it; a larger font scale grows the card
                // and the view with it.
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = VIEW)
                    .clipToBounds()
                    .carouselWheel(state, scope, { instantNow }, { rtlNow }),
                // Room after the last card, so that it too stands 84 dp in when it is the current one (B-168): without it
                // the shelf stopped where the cards end, and in a wide view (three cards over 421 dp) the last card could
                // never become the current one.
                contentPadding = PaddingValues(start = START, top = TOP, end = end, bottom = VIEW - TOP - CARD_H),
                pageSize = PageSize.Fixed(CARD_W),
                pageSpacing = CARD_GAP,
                verticalAlignment = Alignment.Top,
                key = { it },
            ) { page ->
                CarouselCard(items[page], page == current, "$key$page", hidden) {
                    when {
                        page == current -> onClick?.invoke(page)
                        instant -> scope.launch { state.scrollToPage(page) }
                        else -> scope.launch { state.animateScrollToPage(page) }
                    }
                }
            }
        }
        CarouselDots(items.size, current, indicatorKey)
    }
}

/**
 * The dots under the view and the pill pointer at the [current] one. The pointer is one body that slides from the dot
 * it left, as the navigation bar's pill does (B-168; designed here, D15: the page is a still), and a dot stands at once
 * where it was; each keeps the page's key of the dot it stands at.
 */
@Composable
private fun CarouselDots(
    count: Int,
    current: Int,
    indicatorKey: String,
) {
    val clock = readMutantClock()
    val from = remember { PillFrom(current) }
    from.follow(current, count, clock.nowMs, LocalMutantReduceMotion.current, LocalMutantSteppedMotion.current)
    // The screen's direction of reading: the dots run the other way in a right-to-left layout, as the pager does.
    val side = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val chosen = current in 0 until count
    Layout(
        content = {
            if (chosen) {
                MutantSurface(
                    MutantBody.Pill,
                    "$indicatorKey$current",
                    Modifier.size(POINTER_W, DOT),
                    slide = SlideRequest(current, (CAROUSEL_DOT_STEP * (side * from.cells).toFloat()).value.toDouble()),
                ) { Box(Modifier.size(POINTER_W, DOT)) }
            }
            for (i in 0 until count) {
                if (i != current) {
                    key(i) {
                        MutantSurface(MutantBody.Dot, "$indicatorKey$i", Modifier.size(DOT)) { Box(Modifier.size(DOT)) }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val dot = DOT.roundToPx()
        val step = dot + DOT_GAP.roundToPx()
        val wide = POINTER_W.roundToPx() - dot
        // As the page's centred row: a dot per card, 8 dp apart, the pointer 16 dp wider than a dot.
        val row = if (count == 0) 0 else (count - 1) * step + dot + (if (chosen) wide else 0)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else row
        layout(width, if (count == 0) 0 else dot) {
            val start = (width - row) / 2

            fun x(i: Int) = start + i * step + if (chosen && i > current) wide else 0
            var next = 0
            if (chosen) placeables[next++].placeRelative(x(current), 0)
            for (i in 0 until count) if (i != current) placeables[next++].placeRelative(x(i), 0)
        }
    }
}

/**
 * One notch of the wheel steps the shelf one card (B-168): the pager's own wheel scrolled 11 dp a notch and snapped back,
 * so a notch never reached the next card. A trackpad's small deltas are summed to whole notches, the sum dropped when
 * it turns. A sideways turn steps, or shift with the wheel where the platform leaves it vertical; a plain vertical turn
 * is left to the page.
 */
private fun Modifier.carouselWheel(
    state: PagerState,
    scope: CoroutineScope,
    instant: () -> Boolean,
    rtl: () -> Boolean,
): Modifier =
    pointerInput(state) {
        var notches = 0f
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Scroll) continue
                val delta = event.changes.firstOrNull()?.scrollDelta ?: continue
                val along =
                    when {
                        delta.x != 0f -> delta.x
                        event.keyboardModifiers.isShiftPressed -> delta.y
                        else -> 0f
                    }
                if (along == 0f) continue
                event.changes.forEach { it.consume() }
                if (notches != 0f && sign(notches) != sign(along)) notches = 0f
                notches += along
                val steps = notches.toInt()
                if (steps == 0) continue
                notches -= steps
                // Right to left, the next card lies the other way, as the pager's own scroll has it.
                val to =
                    (state.targetPage + if (rtl()) -steps else steps).coerceIn(
                        0,
                        (state.pageCount - 1).coerceAtLeast(0),
                    )
                scope.launch { if (instant()) state.scrollToPage(to) else state.animateScrollToPage(to) }
            }
        }
    }

@Composable
private fun CarouselCard(
    item: MutantCarouselItem,
    current: Boolean,
    key: String,
    hidden: Boolean,
    onPress: () -> Unit,
) {
    val colors = MutantTheme.colors
    val fonts = LocalMutantFonts.current
    val raster = LocalMutantTextRaster.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard

    fun ink(c: Color) = if (hidden) Color.Transparent else c

    fun rubik(
        size: Int,
        weight: FontWeight,
        c: Color,
    ) = TextStyle(
        fontFamily = fonts?.text,
        fontWeight = weight,
        fontSize = size.sp,
        color = ink(c),
        platformStyle = raster,
    ).cssLine(MutantFaceMetrics.Rubik)
    MutantSurface(
        MutantBody.Card,
        key,
        Modifier
            .width(CARD_W)
            // As tall as its words need, 120 dp at least: a status at the foot fills down to it, not to the window.
            .heightIn(min = CARD_H)
            .height(IntrinsicSize.Min)
            .mutantHand(true)
            .clickable(source, indication = null, role = Role.Button, onClick = onPress),
        state = BodyState.Rest,
        focused = focused && keyboard,
        interactionSource = source,
        calm = if (current) 1.0 else SIDE_CALM,
    ) {
        // The words follow the body's fill as a card's do: boiled amber under a press, they turn black (B-104).
        val titleInk = inkOnBody(colors.text)
        val subtitleInk = inkOnBody(colors.textSecondary)
        val statusInk = inkOnBody(colors.textOsd)
        Box(Modifier.padding(horizontal = PADDING_X, vertical = PADDING_Y)) {
            // The words' own box is the clean zone; with a status at the foot it reaches down to it.
            Column(
                (if (item.status != null) Modifier.fillMaxHeight() else Modifier).mutantCleanZone(),
                verticalArrangement = Arrangement.spacedBy(LINE_GAP),
            ) {
                MutantText(
                    item.title,
                    style = rubik(TITLE_SIZE, FontWeight.Medium, titleInk),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle != null) {
                    MutantText(
                        item.subtitle,
                        style = rubik(SUBTITLE_SIZE, FontWeight.Normal, subtitleInk),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.status != null) {
                    Spacer(Modifier.weight(1f))
                    MutantText(
                        item.status,
                        style =
                            MutantTheme.textStyles
                                .display(STATUS_SIZE.sp)
                                .copy(color = ink(statusInk), platformStyle = raster),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The page's shelf (`width: 400px`), taken when none is given. */
private val DESIGN_WIDTH = 400.dp

/** The view (`height: 172px`), the cards' top in it (`top: 8px`), and the current card's start: 70 dp of the one before. */
private val VIEW = 172.dp
private val TOP = 8.dp
private val START = 84.dp

/** A card (`220px × 120px`), the gap between cards (`gap: 14px`), its padding (`14px 16px`) and its lines' gap (4). */
private val CARD_W = 220.dp
private val CARD_H = 120.dp
private val CARD_GAP = 14.dp
private val PADDING_X = 16.dp
private val PADDING_Y = 14.dp
private val LINE_GAP = 4.dp

/** The cards beside the current one (`data-calm=".5"`). */
private const val SIDE_CALM = .5

/** The view to the dots (`gap: 16px`), a dot (10), the pointer (`26px`), and between them (`gap: 8px`). */
private val INDICATOR_GAP = 16.dp
private val DOT = 10.dp
private val POINTER_W = 26.dp
private val DOT_GAP = 8.dp

/** From one dot to the next: where the pointer slides from, per dot it left. */
private val CAROUSEL_DOT_STEP = DOT + DOT_GAP

/** The title (`16px`, 500), the subtitle (`13px`) and the status (Handjet `20px`), sp. */
private const val TITLE_SIZE = 16
private const val SUBTITLE_SIZE = 13
private const val STATUS_SIZE = 20
