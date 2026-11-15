package io.github.youndie.mutant.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-40, the scale axis: at density 2 everything drawn in dp stands twice as large as at 1; at font scale 2 no text
 * is cut by the box it stands in. (Throwing at font scale 3 and density 3 is the constraints sweep's.)
 */
@OptIn(ExperimentalTestApi::class)
class FontScaleDensityTest {
    private fun sized(s: Subject): Modifier =
        if (s.name == "Cocoon" ||
            s.name == "Pattern+Tape"
        ) {
            Modifier.height(40.dp)
        } else {
            Modifier
        }

    private fun inkSize(
        s: Subject,
        density: Float,
    ): Pair<Int, Int> {
        var box = IntArray(4)
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still(Density(density)) {
                    Box(Modifier.size(400.dp, 400.dp)) {
                        Box(Modifier.offset(40.dp, 40.dp).size(320.dp, 320.dp)) { s.content(sized(s), "Пуск") }
                    }
                }
            }
            mainClock.advanceTimeBy(200)
            box = onRoot().captureToImage().toPixelMap().inkBox()
        }
        return (box[2] - box[0] + 1) to (box[3] - box[1] + 1)
    }

    @Test
    fun `at density 2 every component's ink stands twice as large`() {
        val off =
            subjects.mapNotNull { s ->
                val (w1, h1) = inkSize(s, 1f)
                val (w2, h2) = inkSize(s, 2f)
                val rw = w2 / w1.toFloat()
                val rh = h2 / h1.toFloat()
                if (abs(rw - 2) > .12f || abs(rh - 2) > .12f) "${s.name}: ${w1}x$h1 -> ${w2}x$h2" else null
            }
        assertEquals(emptyList(), off)
    }

    /** Every text whose layout ran out of height in the box it was given, at [fontScale]. */
    private fun clipped(
        fontScale: Float,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ): List<String> {
        val out = mutableListOf<String>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still(
                    Density(1f, fontScale),
                ) { Box(Modifier.size(400.dp, 800.dp)) { Box(Modifier.width(360.dp)) { content() } } }
            }
            mainClock.advanceTimeBy(200)
            for (n in allNodes()) {
                val get = n.config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) { null } ?: continue
                val results = mutableListOf<TextLayoutResult>()
                get.action?.invoke(results)
                // Cut top or bottom: a box shorter than its text. Compose's own flag counts a line ended by an ellipsis
                // too, which is the kit's rule for a word that does not fit its width; an empty text has nothing to cut
                // (an empty single-line field's box can be a pixel under its empty line) (B-61).
                results
                    .filter { it.layoutInput.text.isNotEmpty() && it.size.height < it.multiParagraph.height }
                    .forEach {
                        out +=
                            "'${it.layoutInput.text.take(12)}' ${it.multiParagraph.height.toInt()} in ${it.size.height}"
                    }
            }
        }
        return out
    }

    @Test
    fun `at font scale 2 no component cuts its text, but the top app bar`() {
        val cut =
            subjects
                .filter { it.name != "TopAppBar" }
                .flatMap { s -> clipped(2f) { s.content(Modifier, "Пуск") }.map { "${s.name}: $it" } }
        assertEquals(emptyList(), cut)
    }

    @Test
    fun `F10 at font scale 2 the top app bar keeps its title whole`() =
        assertEquals(emptyList(), clipped(2f) { MutantTopAppBar("Кассеты", status = "CH 03") {} })

    @Test
    fun `F09 at font scale 3 no component cuts its text`() {
        val cut = subjects.flatMap { s -> clipped(3f) { s.content(Modifier, "Пуск") }.map { "${s.name}: $it" } }
        assertEquals(emptyList(), cut)
    }

    @Test
    fun `F17 at font scale 2 an illustration's label stays inside the art`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still(
                    Density(1f, 2f),
                ) { Box(Modifier.size(400.dp, 400.dp)) { MutantIllustration(MutantIllustrationKind.TestCard) } }
            }
            mainClock.advanceTimeBy(200)
            val label = boundsOf("CH 03")
            assertTrue(
                label.left >= 0f && label.right <= MutantIllustrationKind.TestCard.let { 236f },
                "the label at $label",
            )
        }
}
