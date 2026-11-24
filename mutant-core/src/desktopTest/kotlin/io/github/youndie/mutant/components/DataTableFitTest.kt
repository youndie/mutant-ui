package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-202: the data table at a large font and in a narrow slot — the numeric columns as wide as their words and a gap, the
 * names ellipsised, and a table narrower than its columns scrolling sideways with its header.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableFitTest {
    private val rows =
        listOf(
            listOf("Без подписи №3", "46:00", "CH 03"),
            listOf("Эфир, ночь на вторник", "12:04", "CH 03"),
            listOf("Настроечная таблица", "02:10", "CH 01"),
            listOf("Нет сигнала", "00:42", "CH 99"),
        )
    private val columns =
        listOf(
            MutantTableColumn<List<String>>("ЗАПИСЬ") { it[0] },
            MutantTableColumn("ДЛИНА", 90.dp, MutantTableCellStyle.Osd) { it[1] },
            MutantTableColumn("КАНАЛ", 80.dp, MutantTableCellStyle.OsdSecondary) { it[2] },
        )
    private val headers = columns.map { it.header }
    private val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    @Test
    fun `at font scale 2 no two cells' ink touches, and the names that do not fit end in an ellipsis`() {
        val cases = listOf(MutantPalettes.Efir to 360, MutantPalettes.Kopiya to 360, MutantPalettes.Efir to 560)
        for ((palette, width) in cases) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                        MutantTheme(colors = palette, reduceMotion = true) {
                            // The table and its bare twin, the same bodies under a standing clock: what differs is ink.
                            Column(Modifier.background(palette.background)) {
                                for (hidden in listOf(false, true)) {
                                    Box(Modifier.testTag(if (hidden) "bare" else "full")) {
                                        DataTableBody(
                                            rows,
                                            columns,
                                            Modifier.width(width.dp),
                                            setOf(1),
                                            { _, _ -> },
                                            true,
                                            "trow",
                                            hidden,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                mainClock.advanceTimeByFrame()
                val full = onNodeWithTag("full").captureToImage().toAwtImage()
                val bare = onNodeWithTag("bare").captureToImage().toAwtImage()
                val table = onNodeWithTag("full").fetchSemanticsNode().boundsInRoot

                // Each word's node in the full table, by its text, relative to the table; on the line through [y] if given.
                fun cell(
                    word: String,
                    y: Float? = null,
                ): Rect =
                    onAllNodes(hasText(word), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .map { it.boundsInRoot.translate(-table.left, -table.top) }
                        .first { it.top >= 0f && it.bottom <= table.height && (y == null || y in it.top..it.bottom) }

                // The columns where a word's ink lies inside its own node, the band of rows given by the node.
                fun ink(r: Rect): IntRange? {
                    val xs =
                        (r.left.toInt() until r.right.toInt().coerceAtMost(full.width)).filter { x ->
                            (r.top.toInt() until r.bottom.toInt().coerceAtMost(full.height)).any { y ->
                                channelGap(full.getRGB(x, y), bare.getRGB(x, y)) > TEXT_INK
                            }
                        }
                    return if (xs.isEmpty()) null else xs.first()..xs.last()
                }
                val lines = listOf(headers) + rows
                val report = ArrayList<String>()
                var least = Int.MAX_VALUE
                for (line in lines) {
                    val y = cell(line[0]).center.y
                    val inks = line.map { ink(cell(it, y)) }
                    assertTrue(inks.all { it != null }, "$palette $width: a word of $line has no ink: $inks")
                    for (j in 0 until inks.size - 1) {
                        val gap = inks[j + 1]!!.first - inks[j]!!.last - 1
                        report += "${line[j]} | ${line[j + 1]}: $gap px"
                        least = minOf(least, gap)
                    }
                }
                // 8 dp at each cell's end, past the glyphs' own spacing: 14 px on this Mac. Without it the header read
                // ДЛИНАКАН… as one word, 7 px apart, a letter's spacing in Handjet at 36 sp.
                assertTrue(least >= MIN_INK_GAP, "$width: the least gap between two cells' ink is $least px\n$report")

                // The numeric columns are as wide as their words: every length, channel and their headers whole.
                for (word in headers.drop(1) + rows.flatMap { it.drop(1) }) {
                    for (node in onAllNodes(hasText(word), useUnmergedTree = true).fetchSemanticsNodes()) {
                        val laid = mutableListOf<TextLayoutResult>()
                        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(laid)
                        val whole =
                            laid
                                .single()
                                .multiParagraph.intrinsics.maxIntrinsicWidth
                        assertTrue(
                            whole <= laid.single().size.width,
                            "$width: $word is cut, $whole in ${laid.single().size}",
                        )
                    }
                }

                // A name wider than its room ends in an ellipsis, not a cut.
                var cut = 0
                for (row in rows) {
                    val node = onAllNodes(hasText(row[0]), useUnmergedTree = true).fetchSemanticsNodes().first()
                    val laid = mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(laid)
                    val layout = laid.single()
                    assertEquals(TextOverflow.Ellipsis, layout.layoutInput.overflow, row[0])
                    if (layout.multiParagraph.intrinsics.maxIntrinsicWidth > layout.size.width) cut++
                }
                if (width == 360) assertTrue(cut >= 2, "at 360 dp the long names are ellipsised: $cut")
            }
        }
    }

    @Test
    fun `a table narrower than its numeric columns scrolls sideways, its header with its rows`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val palette: MutantColors = MutantPalettes.Efir
            setContent {
                MutantTheme(colors = palette, reduceMotion = true) {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        // The slot in a wider box on the page's ground: what the table draws past the slot shows there.
                        Box(Modifier.testTag("page").width(400.dp).background(palette.background)) {
                            Box(Modifier.testTag("slot")) {
                                MutantDataTable(rows, columns, Modifier.width(NARROW.dp), setOf(1), { _, _ -> })
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            // The pixels drawn right of the slot, 4 px clear of its side.
            fun past(): Int {
                val shot = onNodeWithTag("page").captureToImage().toAwtImage()
                val ground = palette.background.toArgb()
                var n = 0
                for (x in NARROW + 4 until shot.width) {
                    for (y in 0 until shot.height) if (channelGap(shot.getRGB(x, y), ground) > TEXT_INK) n++
                }
                return n
            }

            fun left(word: String) =
                onAllNodes(hasText(word), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .first()
                    .boundsInRoot.left

            fun lefts() = listOf("ДЛИНА", "46:00", "00:42", "КАНАЛ", "CH 03", "CH 99").map { left(it) }

            // At rest: the names keep their room, ellipsised, and the header word over them whole.
            val record = onAllNodes(hasText("ЗАПИСЬ"), useUnmergedTree = true).fetchSemanticsNodes().first()
            val laid = mutableListOf<TextLayoutResult>()
            record.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(laid)
            assertTrue(
                record.boundsInRoot.width > 0f && !laid.single().hasVisualOverflow,
                "ЗАПИСЬ: ${record.boundsInRoot}",
            )
            val rest = lefts()
            val channel =
                onAllNodes(
                    hasText("КАНАЛ"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().first().boundsInRoot
            assertTrue(channel.right > NARROW, "the channel column runs past the slot at rest: $channel")
            // ...and is cut at the slot's side: nothing drawn past it (the whole channel column, unclipped, is ~400).
            assertTrue(past() <= 10, "${past()} pixels drawn past the slot's right side")

            // The wheel sideways (a horizontal wheel, as macOS sends shift + wheel): everything moves by the same.
            onNodeWithTag("slot").performMouseInput { scroll(3f, ScrollWheel.Horizontal) }
            repeat(30) { mainClock.advanceTimeByFrame() }
            val wheeled = lefts()
            val moved = rest.zip(wheeled) { a, b -> a - b }
            assertTrue(moved.first() > 0f, "the wheel moved nothing: $rest -> $wheeled")
            assertTrue(moved.all { it == moved.first() }, "header and rows moved apart: $moved")
            assertEquals(left("ДЛИНА"), left("46:00"), "the length's header over its cells")
            assertEquals(left("КАНАЛ"), left("CH 99"), "the channel's header over its cells")

            // A finger's swipe to the end: the channel column stands whole inside the slot, still in register.
            onNodeWithTag("slot").performTouchInput { swipeLeft() }
            repeat(60) { mainClock.advanceTimeByFrame() }
            val end = onAllNodes(hasText("КАНАЛ"), useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
            val boxes = onAllNodes(checkbox).fetchSemanticsNodes().map { it.boundsInRoot }
            assertTrue(end.right <= NARROW && end.left > 0f, "the channel's header inside the slot at the end: $end")
            assertEquals(left("КАНАЛ"), left("CH 03"))
            assertTrue(boxes.all { it.left < 0f && it.right == NARROW.toFloat() }, "rows scrolled with it: $boxes")
        }

    private companion object {
        /** Under the 210 dp the box, length and channel columns take. */
        const val NARROW = 200

        /** Two cells' ink at least this far apart, in pixels at density 1: 14 with the gap, 7 (a letter's spacing) without. */
        const val MIN_INK_GAP = 11
    }
}
