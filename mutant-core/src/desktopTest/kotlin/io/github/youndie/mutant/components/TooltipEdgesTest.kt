package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-200, the designer's answers 26 and 27: at the window's edges the plate shifts along the edge to stand 8 dp inside the
 * window, still over its anchor, and stands below the anchor only where it does not fit above; a long hint wraps at
 * 240 dp to at most three lines, 30, 44 and 58 dp tall, and ends in an ellipsis.
 *
 * The test window is 1024 × 768 px; the stage fills it at the density given. Where the plate stands is read from its
 * words (12 dp in from each side of the plate, the plate's 30 dp around their 18), and, for the window's edges, from the
 * pixels: no pixel of the plate's fill in the window's outer 6 px, a band clear of the 8 dp by more than any edge's blend.
 */
@OptIn(ExperimentalTestApi::class)
class TooltipEdgesTest {
    private val window = 1024f to 768f

    /** One anchor, a 48 dp icon button at [x], [y] dp, hovered; the stage fills the window at [d]. */
    private fun ComposeUiTest.hovered(
        x: (Float) -> Float,
        y: (Float) -> Float,
        text: String = WORD,
        supporting: String? = null,
        d: Float = 1f,
    ) {
        mainClock.autoAdvance = false
        val w = window.first / d
        val h = window.second / d
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors = MutantPalettes.Efir, reduceMotion = true) {
                    Box(Modifier.testTag("stage").size(w.dp, h.dp).background(MutantPalettes.Efir.background)) {
                        MutantTooltipBox(
                            text,
                            Modifier.offset(x(w).dp, y(h).dp).testTag("anchor"),
                            supportingText = supporting,
                        ) {
                            MutantIconButton(MutantIcons.Share, "Поделиться", {})
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        onNodeWithTag("anchor").performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(300)
    }

    /** The bounds of the words' nodes, in window pixels. */
    private fun ComposeUiTest.words(vararg texts: String): List<Rect> =
        texts.map { t -> onAllNodesWithText(t, useUnmergedTree = true).fetchSemanticsNodes().single().boundsInWindow }

    private fun ComposeUiTest.layouts(text: String): List<TextLayoutResult> =
        onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().map { node ->
            val out = mutableListOf<TextLayoutResult>()
            node.config
                .getOrNull(SemanticsActions.GetTextLayoutResult)
                ?.action
                ?.invoke(out)
            out.single()
        }

    /** Pixels of the plate's fill in the window's outer [band] px. */
    private fun ComposeUiTest.fillAtTheEdges(band: Int = 6): Int {
        val px = onNodeWithTag("stage").captureToImage().toPixelMap()
        val fill = MutantPalettes.Efir.noSignal
        var n = 0
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val edge = x < band || y < band || x >= px.width - band || y >= px.height - band
                if (edge && near(px[x, y], fill)) n++
            }
        }
        return n
    }

    private fun near(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) < .06f && abs(a.green - b.green) < .06f && abs(a.blue - b.blue) < .06f

    private class Spot(
        val name: String,
        val x: (Float) -> Float,
        val y: (Float) -> Float,
    )

    private val spots =
        listOf(
            Spot("left edge", { 0f }, { 300f }),
            Spot("right edge", { it - 48f }, { 300f }),
            Spot("bottom edge", { 400f }, { it - 48f }),
            Spot("top edge", { 400f }, { 0f }),
            Spot("top-left corner", { 0f }, { 0f }),
            Spot("top-right corner", { it - 48f }, { 0f }),
            Spot("bottom-left corner", { 0f }, { it - 48f }),
            Spot("bottom-right, 16 dp in", { it - 64f }, { it - 64f }),
        )

    @Test
    fun `along every edge the plate stands 8 dp inside the window, over its anchor`() {
        for (d in listOf(1f, 2f)) {
            for (s in spots) {
                runComposeUiTest {
                    hovered(s.x, s.y, d = d)
                    val anchor = onNodeWithTag("anchor").fetchSemanticsNode().boundsInWindow
                    val words = words(WORD).single()
                    val margin = 8 * d
                    val pad = 12 * d
                    val plate = Rect(words.left - pad, words.top - 6 * d, words.right + pad, words.top + 24 * d)
                    val at = "${s.name} at density $d: plate $plate, anchor $anchor"
                    assertTrue(plate.left >= margin - d, "left, $at")
                    assertTrue(plate.right <= window.first - margin + d, "right, $at")
                    assertTrue(plate.top >= margin - d, "top, $at")
                    assertTrue(plate.bottom <= window.second - margin + d, "bottom, $at")
                    // Still the anchor's: over or under it, its span taking in the anchor's middle.
                    assertTrue(anchor.center.x in plate.left..plate.right, "over the anchor's middle, $at")
                    assertTrue(plate.bottom <= anchor.top || plate.top >= anchor.bottom, "clear of the anchor, $at")
                    assertEquals(0, fillAtTheEdges(), "fill pixels in the window's outer 6 px, $at")
                }
            }
        }
    }

    @Test
    fun `an anchor in the top 58 dp shows the plate below it, lower down above it`() {
        for (top in listOf(0f, 10f, 30f, 57f)) {
            for (supporting in listOf(null, "ОСТАЛОСЬ 3 МИН")) {
                runComposeUiTest {
                    hovered({ 400f }, { top }, supporting = supporting)
                    val anchor = onNodeWithTag("anchor").fetchSemanticsNode().boundsInWindow
                    val all =
                        words(WORD, *listOfNotNull(supporting).toTypedArray()).reduce { a, b ->
                            Rect(
                                minOf(a.left, b.left),
                                minOf(a.top, b.top),
                                maxOf(a.right, b.right),
                                maxOf(a.bottom, b.bottom),
                            )
                        }
                    assertTrue(
                        all.top >= anchor.bottom + 8,
                        "anchor at $top, ${supporting ?: "plain"}: words $all, anchor $anchor",
                    )
                    assertTrue(abs(all.center.x - anchor.center.x) <= 1f, "centred under it: $all, $anchor")
                }
            }
        }
        // Where the plate fits above, it stays above: the page's place, 28 dp over the anchor.
        runComposeUiTest {
            hovered({ 400f }, { 120f })
            val anchor = onNodeWithTag("anchor").fetchSemanticsNode().boundsInWindow
            val first = words(WORD).single()
            assertTrue(first.bottom <= anchor.top - 28, "above: $first, $anchor")
        }
    }

    @Test
    fun `a long hint wraps at 240 dp to at most three lines at 30, 44 and 58 dp, and ends in an ellipsis`() {
        // The plate alone, as the popup measures it: its slot is the plate's 30, 44 or 58 dp.
        val cases =
            listOf(
                Triple(WORD, null, 1),
                Triple(TWO_LINES, null, 2),
                Triple(THREE_LINES, null, 3),
                Triple(LONG, null, 3),
                Triple(WORD, "ОСТАЛОСЬ 3 МИН", 2),
                Triple(WORD, LONG, 3),
            )
        for ((text, supporting, lines) in cases) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(reduceMotion = true) {
                            Box(Modifier.size(1000.dp, 400.dp)) {
                                TooltipPlate(text, supporting, "tip", Modifier.testTag("plate"))
                            }
                        }
                    }
                }
                mainClock.advanceTimeBy(100)
                val plate = onNodeWithTag("plate").fetchSemanticsNode().boundsInWindow
                val at = "${text.take(20)} / ${supporting?.take(20)}: plate $plate"
                assertTrue(plate.width <= 240f, "at most 240 dp wide, $at")
                assertEquals(30f + 14f * (lines - 1), plate.height, .5f, "$lines lines tall, $at")
                val laid = layouts(text) + (supporting?.let { layouts(it) } ?: emptyList())
                assertEquals(lines, laid.sumOf { it.lineCount }, "lines, $at")
                for (r in laid) assertEquals(TextOverflow.Ellipsis, r.layoutInput.overflow, "ellipsis set, $at")
                // A hint longer than three lines is cut at the third, with the dots (looked at: B-200's frames).
                if (text == LONG || supporting == LONG) assertTrue(laid.last().didOverflowHeight, "cut, $at")
            }
        }
        // And in the window: a long hint over an anchor in the middle wraps there too.
        runComposeUiTest {
            hovered({ it / 2 - 24f }, { 300f }, text = LONG)
            val r = layouts(LONG).single()
            assertEquals(3, r.lineCount)
            assertTrue(r.size.width <= 216, "the words 216 dp wide at most: ${r.size}")
        }
    }

    private companion object {
        const val WORD = "ПОДЕЛИТЬСЯ"
        const val TWO_LINES = "Off, they move in steps of the tick and boil"
        const val THREE_LINES = "Off, they move in steps of the tick: the slime boils, the plates slide"
        const val LONG =
            "Off, they move in steps of the tick: the slime boils, the plates slide, the drips run, the cards tune " +
                "in, and nothing glides"
    }
}
