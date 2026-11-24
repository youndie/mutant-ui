package io.github.youndie.mutant.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The kit's own words: `BasicText` with its layouts kept (B-164). A screen composed anew — a tab shown again — laid out
 * every label of its components afresh, the paragraph shaped by Skia each time; here a layout is kept, by its words,
 * style and constraints, in a measurer shared by every label of a density, direction and font resolver, and a label
 * shown again takes it. What is drawn is the layout's paragraph, as `BasicText` draws it: the same size, baselines,
 * clipping and colour; its semantics carry the words and the layout.
 *
 * The layout is kept without the style's colour, which [TextDraw] lays on as it draws (B-169): a change of the colour
 * alone measures nothing again, and redraws the words in the next frame. A span's colour is in the paragraph's runs, so
 * a change of a span lays the words out again, as `BasicText` does, and redraws them too.
 */
@Composable
internal fun MutantText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    color: ColorProducer? = null,
) {
    MutantText(
        remember(text) { AnnotatedString(text) },
        modifier,
        style,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        color,
    )
}

/** [MutantText] for words with spans. */
@Composable
internal fun MutantText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    color: ColorProducer? = null,
) {
    val direction = LocalLayoutDirection.current
    val measurer = sharedMeasurer(LocalFontFamilyResolver.current, LocalDensity.current, direction)
    val held = remember { arrayOfNulls<TextLayoutResult>(1) }
    val measured = remember(style, direction) { style.withoutColour().inOwnDirection(direction) }
    val policy =
        remember(measurer, text, measured, overflow, softWrap, maxLines, onTextLayout) {
            TextPolicy(measurer, text, measured, overflow, softWrap, maxLines) { r ->
                held[0] = r
                onTextLayout?.invoke(r)
            }
        }
    Layout(
        modifier
            .semantics {
                this.text = text
                getTextLayoutResult { out ->
                    val r = held[0] ?: return@getTextLayoutResult false
                    out.add(r)
                    true
                }
            }.then(TextDraw(held, text, style, color, overflow)),
        policy,
    )
}

/**
 * Right to left, words in their own direction (B-205, the designer's answer 37): a left-to-right word is laid out left to
 * right and ends in its ellipsis at its own end, the right, where Skia's paragraph took the layout's direction and drew
 * the ellipsis at the word's start (F-data-table-06 of B-168). It still stands where the layout puts it: its start's
 * alignment becomes the right, its end's the left. A style that names its direction keeps it; left to right nothing
 * changes.
 */
private fun TextStyle.inOwnDirection(direction: LayoutDirection): TextStyle {
    if (direction != LayoutDirection.Rtl || textDirection != TextDirection.Unspecified) return this
    val align =
        when (textAlign) {
            TextAlign.Unspecified, TextAlign.Start -> TextAlign.Right
            TextAlign.End -> TextAlign.Left
            else -> textAlign
        }
    return copy(textDirection = TextDirection.Content, textAlign = align)
}

/** The style the layout is kept by: the colour is laid on at draw, so two colours of the same words share a layout. */
private fun TextStyle.withoutColour(): TextStyle = if (color.isSpecified) copy(color = Color.Unspecified) else this

/**
 * Draws the kept layout in [color], or the [style]'s colour where no producer is given or it produces none. Any change of
 * what it draws by — the words (a span's colour), the style (its colour, which measures nothing again), the producer —
 * updates the node, and the update redraws the words (B-169).
 */
private class TextDraw(
    val held: Array<TextLayoutResult?>,
    val text: AnnotatedString,
    val style: TextStyle,
    val color: ColorProducer?,
    val overflow: TextOverflow,
) : ModifierNodeElement<TextDrawNode>() {
    override fun create() = TextDrawNode(held, style.color, color, overflow)

    override fun update(node: TextDrawNode) = node.update(style.color, color, overflow)

    override fun InspectorInfo.inspectableProperties() {
        name = "mutantText"
    }

    override fun equals(other: Any?): Boolean =
        other is TextDraw &&
            held === other.held &&
            text == other.text &&
            style == other.style &&
            color === other.color &&
            overflow == other.overflow

    override fun hashCode(): Int {
        var h = text.hashCode()
        h = 31 * h + style.hashCode()
        h = 31 * h + (color?.hashCode() ?: 0)
        return 31 * h + overflow.hashCode()
    }
}

private class TextDrawNode(
    private val held: Array<TextLayoutResult?>,
    private var tint: Color,
    private var color: ColorProducer?,
    private var overflow: TextOverflow,
) : Modifier.Node(),
    DrawModifierNode {
    override val shouldAutoInvalidate: Boolean get() = false

    fun update(
        tint: Color,
        color: ColorProducer?,
        overflow: TextOverflow,
    ) {
        this.tint = tint
        this.color = color
        this.overflow = overflow
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val r = held[0]
        if (r != null) {
            val ink = (color?.invoke() ?: Color.Unspecified).takeOrElse { tint }
            if (r.hasVisualOverflow && overflow != TextOverflow.Visible) {
                clipRect(0f, 0f, size.width, size.height) { drawText(r, ink) }
            } else {
                drawText(r, ink)
            }
        }
        drawContent()
    }
}

private class TextPolicy(
    private val measurer: TextMeasurer,
    private val text: AnnotatedString,
    private val style: TextStyle,
    private val overflow: TextOverflow,
    private val softWrap: Boolean,
    private val maxLines: Int,
    private val laidOut: (TextLayoutResult) -> Unit,
) : MeasurePolicy {
    private fun layout(constraints: Constraints) =
        measurer.measure(text, style, overflow, softWrap, maxLines, constraints = constraints)

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val r = layout(constraints)
        laidOut(r)
        return layout(
            r.size.width,
            r.size.height,
            mapOf(FirstBaseline to r.firstBaseline.roundToInt(), LastBaseline to r.lastBaseline.roundToInt()),
        ) {}
    }

    private fun intrinsics() = layout(Constraints()).multiParagraph.intrinsics

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = ceil(intrinsics().minIntrinsicWidth).toInt()

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = ceil(intrinsics().maxIntrinsicWidth).toInt()

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = layout(Constraints(maxWidth = width)).size.height

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = layout(Constraints(maxWidth = width)).size.height
}

/** One measurer, and so one cache of layouts, a density, direction and font resolver: every label of the kit's shares it. */
private val measurers = HashMap<Triple<FontFamily.Resolver, Density, LayoutDirection>, TextMeasurer>()

private fun sharedMeasurer(
    resolver: FontFamily.Resolver,
    density: Density,
    direction: LayoutDirection,
): TextMeasurer =
    measurers.getOrPut(Triple(resolver, density, direction)) {
        TextMeasurer(resolver, density, direction, cacheSize = MEASURED)
    }

/** B-164: the layouts kept, about two screens' labels several widths each. */
private const val MEASURED = 512
