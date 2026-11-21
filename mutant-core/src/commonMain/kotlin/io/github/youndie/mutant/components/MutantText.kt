package io.github.youndie.mutant.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
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
    val measurer = sharedMeasurer(LocalFontFamilyResolver.current, LocalDensity.current, LocalLayoutDirection.current)
    val held = remember { arrayOfNulls<TextLayoutResult>(1) }
    val policy =
        remember(measurer, text, style, overflow, softWrap, maxLines, onTextLayout) {
            TextPolicy(measurer, text, style, overflow, softWrap, maxLines) { r ->
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
            }.drawBehind {
                val r = held[0] ?: return@drawBehind
                val tint = color?.invoke() ?: Color.Unspecified
                if (r.hasVisualOverflow && overflow != TextOverflow.Visible) {
                    clipRect(0f, 0f, size.width, size.height) { drawText(r, tint) }
                } else {
                    drawText(r, tint)
                }
            },
        policy,
    )
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
