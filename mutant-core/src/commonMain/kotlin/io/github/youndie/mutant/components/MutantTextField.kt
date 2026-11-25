package io.github.youndie.mutant.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.readMutantClock
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.surface.mutantCleanZone
import io.github.youndie.mutant.surface.mutantPlaceKey
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantReduceMotion
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A text field: a line of the on-screen menu (body `field`, 52 dp tall). [prefix] is the OSD label before the
 * value (Handjet, the OSD colour), [placeholder] stands in while the value is empty. Focused, the damage drops to 30 %,
 * the corner brackets lock on and a block caret blinks 2 dp after the text, or a thin one stands in the gap between the
 * letters inside it or before the placeholder, the blink starting over lit at focus and at every input; [isError]
 * corrupts the body (the red offset outline and blocks) and tells a screen reader what is wrong, [errorDescription]
 * (the generic `Ошибка` when the caller says nothing); disabled, it loses its signal and both texts grey. A press anywhere on the body focuses it and puts the cursor at the nearest place in the text. The text
 * and the caret are never moved by the body. One line; its direction is the text's.
 *
 * [key] seeds its body: by default its place in the composition and its body type (B-206), so two unkeyed ones are
 * two bodies and one keeps its body whatever its words or state; in a list, give it from the item's id.
 */
@Composable
public fun MutantTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    key: String = mutantPlaceKey("field"),
    interactionSource: MutableInteractionSource? = null,
    errorDescription: String = ERROR_WORD,
) {
    TextFieldBody(
        value,
        onValueChange,
        modifier,
        prefix,
        placeholder,
        enabled,
        isError,
        keyboardOptions,
        key,
        interactionSource,
        errorDescription = errorDescription,
    )
}

/** [MutantTextField]; [focusedAs] holds a fixture's focus, [hidden] draws the texts and caret transparent. */
@Composable
internal fun TextFieldBody(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    prefix: String?,
    placeholder: String?,
    enabled: Boolean,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    key: String,
    interactionSource: MutableInteractionSource?,
    focusedAs: Boolean? = null,
    hidden: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    errorDescription: String = ERROR_WORD,
) {
    val colors = MutantTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val focused = focusedAs ?: isFocused
    val fonts = LocalMutantFonts.current

    fun tint(c: Color) = if (hidden) Color.Transparent else c
    val textColor = tint(if (enabled) colors.text else colors.textDisabled)
    val plain =
        TextStyle(
            fontFamily = fonts?.text,
            fontSize = VALUE_SIZE.sp,
            color = textColor,
            platformStyle = LocalMutantTextRaster.current,
        )
    // On Blink's line at the user's own size (research §1.14). A larger font scale lays the value on the face's own
    // line: a single-line BasicTextField holds its text to the line it measures itself, which cssLine's rounded line
    // overran by a pixel at font scale 3 and cut (B-61).
    val valueStyle =
        if (androidx.compose.ui.platform.LocalDensity.current.fontScale >
            1f
        ) {
            plain
        } else {
            plain.cssLine(MutantFaceMetrics.Rubik)
        }
    // The value's line, scrolled by the field itself (B-168): its text layout as the line last measured it, and how far
    // it is scrolled.
    val line = remember { ValueLine() }
    // The cursor is the field's own, kept here so a press on the body can place it; a value changed from outside puts
    // it at the end. Focus by Tab finds it at the end too.
    var held by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val field = if (held.text == value) held else TextFieldValue(value, TextRange(value.length))
    val own = remember { FocusRequester() }
    val requester = focusRequester ?: own
    var textBox by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var body by remember { mutableStateOf<LayoutCoordinates?>(null) }
    MutantSurface(
        MutantBody.Field,
        key,
        modifier
            .defaultMinSize(minWidth = MIN_WIDTH)
            .heightIn(min = HEIGHT)
            .onGloballyPositioned { body = it }
            // A press the text itself does not take (the prefix, the padding, above and below the line) focuses the
            // field and puts the cursor at the nearest place on the line (B-58).
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { at ->
                    requester.requestFocus()
                    val l = line.layout ?: return@detectTapGestures
                    val b = body ?: return@detectTapGestures
                    val t = textBox ?: return@detectTapGestures
                    if (!b.isAttached || !t.isAttached) return@detectTapGestures
                    val local = t.localPositionOf(b, at)
                    val n = l.layoutInput.text.length
                    val offset = l.getOffsetForPosition(Offset(local.x, l.size.height / 2f)).coerceIn(0, n)
                    held = field.copy(selection = TextRange(offset))
                }
            }.semantics { if (isError) error(errorDescription) },
        state =
            when {
                !enabled -> BodyState.Disabled
                isError -> BodyState.Error
                else -> BodyState.Rest
            },
        focused = focused,
    ) {
        // The clean zone is the input area, the slot less 14 dp on each side, never the text: the mosaic and the flow
        // lines stand still as the first letter is typed and the last deleted (B-186, the designer's answer 18).
        Spacer(Modifier.matchParentSize().padding(PADDING).mutantCleanZone())
        Row(
            Modifier.fillMaxWidth().padding(horizontal = PADDING),
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefix != null) {
                MutantText(
                    prefix,
                    style =
                        TextStyle(
                            fontFamily = fonts?.display,
                            fontSize = PREFIX_SIZE.sp,
                            color = tint(if (enabled) colors.textOsd else colors.textDisabled),
                            platformStyle = LocalMutantTextRaster.current,
                        ).cssLine(MutantFaceMetrics.Handjet),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    MutantText(
                        placeholder,
                        style =
                            valueStyle.copy(
                                color = tint(if (enabled) colors.textSecondary else colors.textDisabled),
                            ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Layout(
                    content = {
                        BasicTextField(
                            field,
                            {
                                held = it
                                if (it.text != value) onValueChange(it.text)
                            },
                            Modifier.focusRequester(requester).onGloballyPositioned { textBox = it },
                            enabled = enabled,
                            textStyle = valueStyle.copy(textDirection = TextDirection.Content),
                            keyboardOptions = keyboardOptions,
                            keyboardActions = keyboardActions,
                            singleLine = true,
                            interactionSource = source,
                            // The design's caret is a block after the text, drawn below; the field's own is hidden.
                            cursorBrush = SolidColor(Color.Transparent),
                            onTextLayout = { line.layout = it },
                        )
                        // The caret starts its blink over at every change of the value or the cursor; it enters the
                        // composition, lit, with focus.
                        if (focused && enabled) TextFieldCaret(tint(colors.text), field)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    measurePolicy =
                        valueLinePolicy(
                            line,
                            field.selection.start,
                            overPlaceholder = value.isEmpty() && placeholder != null,
                            asDesigned = LocalMutantTextFieldAsDesigned.current,
                        ),
                )
            }
            trailing?.invoke()
        }
    }
}

/** The value's line as its last measure left it: the text's layout and how far the line is scrolled, px. */
private class ValueLine {
    var layout: TextLayoutResult? = null
    var scroll = 0
}

/**
 * The value's line (B-168): the text field measured as wide as its text, scrolled by the line itself so the caret stays
 * in view, and the caret placed in the same measure, from the layout the text has just made. A text field that scrolls
 * itself cannot be read, and a caret drawn over it stood in the text's unscrolled place: past the end of a long value
 * the block covered its last letters, and inside it the bar stood at the field's edge. A caret placed from a layout
 * read a frame later stood at the last text's end for the frame a letter was typed.
 *
 * At the end of the text the caret is the page's block (9 × 20), 2 dp past the last character's box on the side the
 * text runs. Inside the text it is a bar 2 dp wide centred in the gap between the boxes of the letters either side,
 * which meet at the insertion point, drawn over their edges: Rubik 15 leaves 0–2 px between most letters' ink, so the
 * bar covers up to 1 dp of each, never a whole stem (B-186, the designer's answer 17, the owner's word). The text's
 * API gives a letter's box, not its ink (`getPathForRange` is the selection's boxes on Skiko). At the line's start, and
 * over a placeholder, it is the bar standing just before the first letter, which a block would cover (B-58, B-168).
 * The line scrolls as little as keeps the whole block, or the bar's insertion point, in view; the bar may stand in the
 * padding or the gap before the text. The text is clipped to the line only while it overflows it. A value ending at
 * the line's edge leaves the block's room past it, focused or not, so focus does not move the text.
 */
private fun valueLinePolicy(
    line: ValueLine,
    cursor: Int,
    overPlaceholder: Boolean,
    asDesigned: Boolean,
) = MeasurePolicy { measurables, constraints ->
    val bounded = constraints.hasBoundedWidth
    val text =
        measurables[0].measure(
            Constraints(
                minWidth = if (bounded) constraints.maxWidth else 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxHeight,
            ),
        )
    // The layout the text has just made: on the frame a value changes, the one made for it.
    val l = line.layout
    val n = l?.layoutInput?.text?.length ?: 0
    val at = cursor.coerceIn(0, n)
    val block = at == n && !overPlaceholder
    val x = l?.let { if (n == 0) 0f else it.getHorizontalPosition(at, true) } ?: 0f
    val rtl =
        l?.let { n > 0 && it.getParagraphDirection(if (at == n) n - 1 else at) == ResolvedTextDirection.Rtl } ?: false
    val w = (if (block) CARET_W else CARET_BAR).roundToPx()
    val h = CARET_H.roundToPx()
    val clear = CARET_CLEAR.roundToPx()
    val left =
        when {
            l == null || n == 0 -> if (block || rtl) x.roundToInt() else (x - w).roundToInt()

            // The page's block, its flex gap past the end (the departures registry, research D20).
            block && asDesigned -> (if (rtl) x - GAP.toPx() - w else x + GAP.toPx()).roundToInt()

            // The block past the end, 2 dp clear of the last character's box, on the side the text runs.
            block && !rtl -> ceil(x).toInt() + clear

            block -> floor(x).toInt() - clear - w

            // At the line's start the bar stands just before the first letter.
            at == 0 -> if (rtl) x.roundToInt() else (x - w).roundToInt()

            // Inside, the bar centred in the gap between the letters' boxes, which meet at the insertion point.
            else -> (x - w / 2f).roundToInt()
        }
    // What stays in view: the whole block, or the bar's insertion point.
    val lo = if (block) left else x.roundToInt()
    val hi = if (block) left + w else x.roundToInt()
    val start = if (block) minOf(0, left) else 0
    val end = maxOf(text.width, if (block) left + w else 0)
    val view = if (bounded) constraints.maxWidth else end - start
    var scroll = line.scroll
    if (lo < scroll) scroll = lo
    if (hi > scroll + view) scroll = hi - view
    scroll = scroll.coerceIn(start, maxOf(start, end - view))
    line.scroll = scroll
    val caret = measurables.getOrNull(1)?.measure(Constraints.fixed(w, h))
    val height = maxOf(text.height, caret?.height ?: 0)
    val overflows = scroll != 0 || text.width > view
    layout(view, height) {
        val ty = (height - text.height) / 2
        if (overflows) {
            val window = RectangleWindow(scroll.toFloat(), view.toFloat())
            text.placeWithLayer(-scroll, ty) {
                clip = true
                shape = window
            }
        } else {
            text.place(0, ty)
        }
        // The bar may stand in the padding or the gap before the text, the block never leaves the line.
        val min = if (block) 0 else -w
        caret?.place((left - scroll).coerceAtMost(view - w).coerceAtLeast(min), (height - h) / 2)
    }
}

/** The part of a scrolled text that stands in its line: [from] to [from] + [width], px, its whole height. */
private class RectangleWindow(
    private val from: Float,
    private val width: Float,
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Rectangle(Rect(from, 0f, from + width, size.height))
}

/**
 * The caret, lit five scene ticks in eight and dark three, the engine's `fi % 8 < 5` (B-20), counted from the tick
 * of the last restart: it enters the composition lit with focus, and every change of [restartOn] (the value, the
 * cursor) starts the cycle over lit on the frame it is drawn (B-186, the designer's answer 8). The restart's own tick
 * is the first lit one, so the blink keeps the scene's tick and draws nothing between ticks. Under reduce motion it
 * stands lit.
 */
@Composable
private fun TextFieldCaret(
    color: Color,
    restartOn: Any,
) {
    val clock = readMutantClock()
    val still = LocalMutantReduceMotion.current
    val blink = remember { TextFieldBlink() }
    Canvas(Modifier) {
        // The tick is read first, so the draw follows every tick whichever branch it takes.
        val tick = clock.sceneTick
        if (blink.on != restartOn) {
            blink.on = restartOn
            blink.from = tick
        }
        if (still || (tick - blink.from).mod(BLINK_PERIOD) < BLINK_ON) drawRect(color)
    }
}

/** Where the caret's blink last started: the scene tick, and what it started on. Plain fields, written by the draw. */
@Stable
private class TextFieldBlink {
    var on: Any? = null
    var from = 0L
}

/**
 * B-186's departure from the page, for the parity cells (research D20's registry): true stands the block caret 8 dp past
 * the text, as the page's flex gap does; the kit's is 2 dp past the last character's box (the designer's answer 17).
 */
internal val LocalMutantTextFieldAsDesigned = staticCompositionLocalOf { false }

/** What a screen reader says of an error the caller did not describe (B-67). */
private const val ERROR_WORD = "Ошибка"

/** The field's height (the page's `height: 52px`) and the width it takes given none (research D15). */
private val HEIGHT = 52.dp
private val MIN_WIDTH = 200.dp

/** The texts from the sides and from each other (the page's `padding: 0 14px; gap: 8px`). */
private val PADDING = 14.dp
private val GAP = 8.dp

/** The OSD prefix (Handjet 20) and the value (Rubik 15), sp. */
private const val PREFIX_SIZE = 20
private const val VALUE_SIZE = 15

/** The block caret, and its blink: on for 5 of every 8 scene ticks (the engine's `fi % 8 < 5`). */
private val CARET_W = 9.dp

/** The block's gap from the last character's box (B-186, the designer's answer 17). */
private val CARET_CLEAR = 2.dp

/** The caret inside the text: a bar centred in the gap between two letters (B-58, B-186). */
private val CARET_BAR = 2.dp
private val CARET_H = 20.dp

/** The blink's cycle and its lit part, in scene ticks of 125 ms (`MutantMotion.TICK_MS`). */
private const val BLINK_PERIOD = 8L
private const val BLINK_ON = 5L
