package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-205, the designer's answer 37 to B-168: right to left the segmented button, the tabs' echo, the month arrows and the
 * switch's run mirror with the layout; the arrow keys move the segmented button in reading order; a left-to-right title
 * ends in its ellipsis at its own end. Density 1, «Эфир»: a dp is a pixel. Every pixel measure is over an area.
 */
@OptIn(ExperimentalTestApi::class)
class RightToLeftPartsTest {
    private val efir = MutantPalettes.Efir

    private fun ComposeUiTest.stage(
        direction: LayoutDirection,
        reduceMotion: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) {
                MutantTheme(efir, reduceMotion = reduceMotion) {
                    Box(Modifier.background(efir.background)) { content() }
                }
            }
        }
        repeat(4) { mainClock.advanceTimeByFrame() }
    }

    private fun ComposeUiTest.pixels(tag: String): PixelMap = onNodeWithTag(tag).captureToImage().toPixelMap()

    private fun SemanticsNode.rect(): Rect =
        Rect(positionInRoot.x, positionInRoot.y, positionInRoot.x + size.width, positionInRoot.y + size.height)

    private fun Color.near(
        c: Color,
        by: Float = .06f,
    ) = abs(red - c.red) < by && abs(green - c.green) < by && abs(blue - c.blue) < by

    /** The mean column of the pixels [match] picks, and how many there are. */
    private fun PixelMap.centroid(
        rows: IntRange = 0 until height,
        columns: IntRange = 0 until width,
        match: (Color) -> Boolean,
    ): Pair<Float, Int> {
        var sx = 0.0
        var n = 0
        for (y in rows) {
            for (x in columns) {
                if (match(this[x, y])) {
                    sx += x
                    n++
                }
            }
        }
        return (if (n == 0) Float.NaN else (sx / n).toFloat()) to n
    }

    // --- The segmented button -------------------------------------------------------------------------------------

    private val segments = listOf("SP", "LP", "EP")

    /**
     * The lime window stands over the chosen segment, the first at the right: on a 312 dp button, 4 dp inside, a segment
     * is 101.3 dp, so the first's middle is at 257 right to left and 55 left to right.
     */
    @Test
    fun `right to left, the segmented button's window stands over the first segment at the right`() {
        for ((direction, middle) in listOf(LayoutDirection.Ltr to 54.7f, LayoutDirection.Rtl to 257.3f)) {
            runComposeUiTest {
                stage(direction) { MutantSegmentedButton(segments, 0, {}, Modifier.width(312.dp).testTag("s")) }
                val (x, n) = pixels("s").centroid { it.near(efir.primary) }
                assertTrue(n > 1500, "$direction: the window's lime, $n pixels")
                assertTrue(
                    abs(x - middle) < 6,
                    "$direction: the window's lime centred at $x, the segment's middle at $middle",
                )
                val word = onNodeWithText("SP").fetchSemanticsNode().rect()
                assertTrue(abs(word.center.x - middle) < 6, "$direction: «SP» centred at ${word.center.x}")
            }
        }
    }

    /** Left chooses the segment on the left and Right the one on the right; Down and Up the next and the one before. */
    @Test
    fun `right to left, the arrow keys move the segmented button in reading order`() =
        runComposeUiTest {
            var chosen by mutableIntStateOf(0)
            stage(LayoutDirection.Rtl) {
                MutantSegmentedButton(segments, chosen, { chosen = it }, Modifier.testTag("s"))
            }
            onNodeWithTag("s").requestFocus()
            val steps =
                listOf(
                    Key.DirectionLeft to 1,
                    Key.DirectionLeft to 2,
                    Key.DirectionLeft to 2,
                    Key.DirectionRight to 1,
                    Key.DirectionUp to 0,
                    Key.DirectionDown to 1,
                )
            for ((key, expected) in steps) {
                onNodeWithTag("s").performKeyInput { pressKey(key) }
                mainClock.advanceTimeByFrame()
                assertEquals(expected, chosen, "after $key")
            }
            // The chosen one stands left of the first: reading order runs right to left.
            val first = onNodeWithText("SP").fetchSemanticsNode().rect()
            val chosenWord = onNodeWithText("LP").fetchSemanticsNode().rect()
            assertTrue(
                chosenWord.center.x < first.center.x,
                "«LP» at ${chosenWord.center.x}, «SP» at ${first.center.x}",
            )
        }

    // --- The tabs' echo -------------------------------------------------------------------------------------------

    /**
     * The echo's three strokes, lime at 60, 30 and 15 % over the ground, in the 2 dp under the bar's top: how many of
     * their pixels stand left and right of the bar (the bar's own lime and its ring left out).
     */
    private fun ComposeUiTest.echo(): Pair<Int, Int> {
        val px = pixels("t")
        val bar = (0 until px.width).filter { x -> (0 until px.height).count { y -> px[x, y].near(efir.primary) } >= 3 }
        assertTrue(bar.size > 20, "the bar: ${bar.size} columns")
        val rows = (0 until px.height).filter { y -> bar.count { px[it, y].near(efir.primary) } > bar.size / 2 }
        val tinted = { c: Color ->
            !c.near(efir.primary) && c.green - c.blue > .12f &&
                c.green > efir.background.green + .05f
        }
        var left = 0
        var right = 0
        for (y in rows) {
            for (x in 0 until px.width) {
                if (!tinted(px[x, y])) continue
                if (x < bar.min() - 3) left++
                if (x > bar.max() + 3) right++
            }
        }
        return left to right
    }

    private val tabs = listOf(MutantTab("Эфир"), MutantTab("Архив"), MutantTab("Копии"))

    @Test
    fun `right to left, the tabs' echo rests left of the bar`() {
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            runComposeUiTest {
                stage(direction) { MutantTabs(tabs, 1, {}, Modifier.width(320.dp).testTag("t")) }
                val (left, right) = echo()
                val (ahead, behind) = if (direction == LayoutDirection.Rtl) left to right else right to left
                assertTrue(
                    ahead > 20 && behind == 0,
                    "$direction at rest: $left echo pixels left of the bar, $right right",
                )
            }
        }
    }

    /** A run toward the row's end (to the right, right to left) keeps its echo where it rests: none to the right. */
    @Test
    fun `right to left, a run back toward the first tab trails its echo on the left alone`() =
        runComposeUiTest {
            var chosen by mutableIntStateOf(2)
            stage(LayoutDirection.Rtl, reduceMotion = false) {
                MutantTabs(tabs, chosen, { chosen = it }, Modifier.width(320.dp).testTag("t"))
            }
            mainClock.advanceTimeBy(1_000)
            chosen = 0
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(112)
            val (left, right) = echo()
            assertTrue(left > 10 && right == 0, "mid-run: $left echo pixels left of the bar, $right right")
        }

    // --- The month arrows -----------------------------------------------------------------------------------------

    /**
     * Right to left the back arrow stands right of the forward one, 18 dp apart as left to right, and points right; the
     * forward one points left. Read from each triangle's ink inside its own 48 dp target, on the glyph's rows: its box gives where it stands,
     * and the ink leans to the triangle's flat side, away from where it points.
     */
    @Test
    fun `right to left, the month arrows are mirrored and point their ways`() {
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            runComposeUiTest {
                stage(direction) {
                    MutantDatePicker(2026, 11, 13, {}, { _, _ -> }, Modifier.testTag("c"))
                }
                val px = pixels("c")
                val origin = onNodeWithTag("c").fetchSemanticsNode().positionInRoot
                val target = { label: String ->
                    onNodeWithContentDescription(label).fetchSemanticsNode().rect().translate(-origin)
                }
                val backTarget = target("Предыдущий месяц")
                val forwardTarget = target("Следующий месяц")
                // The glyph's box and how far its ink's middle stands from the box's middle. A glyph stands at its
                // target's inner end, where the two targets meet: only the 16 dp there are read, not the body's marks
                // under the outer end.
                val glyph = { r: Rect, other: Rect ->
                    val inner =
                        if (other.center.x >
                            r.center.x
                        ) {
                            r.right.toInt() - 16 until r.right.toInt()
                        } else {
                            r.left.toInt() until
                                r.left.toInt() + 16
                        }
                    val xs = mutableListOf<Int>()
                    // The glyph's 8 dp rows, a pixel under the target's middle: not the weekdays under the header.
                    for (y in r.center.y.toInt() - 6 until r.center.y.toInt() + 8) {
                        for (x in inner) {
                            if (x in 0 until px.width && px[x, y].near(efir.textSecondary, .12f)) xs += x
                        }
                    }
                    assertTrue(xs.size > 25, "$direction: ${xs.size} pixels of an arrow's ink")
                    val middle = (xs.min() + xs.max()) / 2f
                    middle to xs.average().toFloat() - middle
                }
                val (back, backLean) = glyph(backTarget, forwardTarget)
                val (forward, forwardLean) = glyph(forwardTarget, backTarget)
                val rtl = direction == LayoutDirection.Rtl
                // 18 dp between the glyphs' middles, less the faint tips the colour leaves out: not F-pickers-10's 30.
                assertTrue(abs(forward - back) in 15f..19f, "$direction: the glyphs ${abs(forward - back)} apart")
                assertTrue((back > forward) == rtl, "$direction: back at $back, forward at $forward")
                // Pointing right, the ink leans left of the glyph's middle; pointing left, right of it.
                assertTrue(if (rtl) backLean < -1 else backLean > 1, "$direction: the back arrow's ink leans $backLean")
                assertTrue(
                    if (rtl) forwardLean > 1 else forwardLean < -1,
                    "$direction: the forward arrow's ink leans $forwardLean",
                )
            }
        }
    }

    // --- The switch's run -----------------------------------------------------------------------------------------

    /**
     * Switched on, the thumb slides from where it stood: from the left left to right, from the right right to left. Read
     * 48 ms in, by the thumb's pale ink against its place at rest.
     */
    @Test
    fun `right to left, the switch's thumb slides in from the right`() {
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            runComposeUiTest {
                var on by mutableStateOf(false)
                stage(direction, reduceMotion = false) {
                    MutantSwitch(on, { on = it }, Modifier.testTag("w"))
                }
                mainClock.advanceTimeBy(1_000)
                val pale = { c: Color -> c.red > .8f && c.green > .8f && c.blue > .75f }
                on = true
                mainClock.advanceTimeByFrame()
                mainClock.advanceTimeBy(48)
                val (moving, n) = pixels("w").centroid(match = pale)
                mainClock.advanceTimeBy(1_000)
                val (rest, m) = pixels("w").centroid(match = pale)
                assertTrue(n > 60 && m > 60, "$direction: the thumb's ink, $n and $m pixels")
                val from = moving - rest
                assertTrue(
                    if (direction == LayoutDirection.Rtl) from > 4 else from < -4,
                    "$direction: 48 ms in the thumb stands ${"%.1f".format(from)} px from its place",
                )
            }
        }
    }

    // --- The slider's glide ---------------------------------------------------------------------------------------

    /** A value the app sets glides there: right to left toward the left, through every frame (B-182 with B-205). */
    @Test
    fun `right to left, the slider's handle glides toward the left`() =
        runComposeUiTest {
            var value by mutableIntStateOf(-10)
            stage(LayoutDirection.Rtl, reduceMotion = false) {
                MutantSlider(value, { value = it }, Modifier.width(300.dp), label = "ТРЕКИНГ")
            }
            val handle = {
                onNodeWithText(signed(value), useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .rect()
                    .center.x
            }
            val start = handle()
            value = 10
            val xs = mutableListOf<Float>()
            repeat(30) {
                mainClock.advanceTimeByFrame()
                xs += handle()
            }
            assertTrue(xs.last() < start - 200, "the handle from $start to ${xs.last()}")
            assertTrue(xs.zipWithNext().all { (a, b) -> b <= a + .5f }, "the handle went back right: $xs")
            assertTrue(xs.any { it < start - 20 && it > xs.last() + 20 }, "no frame between the ends: $xs")
        }

    // --- The title's ellipsis -------------------------------------------------------------------------------------

    /**
     * A left-to-right title too long for its bar, right to left as left to right: laid out from its own start, its
     * capital at the left, and ending in its ellipsis at its own end, the right — read by how tall the ink stands over
     * the line's first and last columns. Its box stands mirrored.
     */
    @Test
    fun `a left-to-right title in a right-to-left bar ends in its ellipsis on the right`() {
        val title = "Эфир, ночь на вторник, перемотка кассеты"
        val boxes = mutableMapOf<LayoutDirection, Pair<Rect, IntArray>>()
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            runComposeUiTest {
                stage(direction) {
                    MutantTopAppBar(title, Modifier.width(300.dp).testTag("b"), status = "CH 03 · SP")
                }
                val r = onNodeWithText(title).fetchSemanticsNode().rect()
                val px = pixels("b")
                val crop = IntArray(r.width.toInt() * r.height.toInt())
                for (y in 0 until r.height.toInt()) {
                    for (x in 0 until r.width.toInt()) {
                        val c = px[r.left.toInt() + x, r.top.toInt() + y]
                        crop[y * r.width.toInt() + x] = if (c.red + c.green + c.blue > 1.5f) 1 else 0
                    }
                }
                boxes[direction] = r to crop
            }
        }
        val (l, ltr) = boxes.getValue(LayoutDirection.Ltr)
        val (r, rtl) = boxes.getValue(LayoutDirection.Rtl)
        assertEquals(l.width, r.width, "the title's box")
        assertTrue(
            abs(r.left - (300 - l.right)) < 2,
            "right to left the title's box from ${r.left}, left to right to ${l.right}",
        )
        val w = l.width.toInt()
        val h = l.height.toInt()

        // The ink's rows over a run of columns of the crop: how tall the ink stands there, px.
        fun tall(
            crop: IntArray,
            columns: IntProgression,
        ): Int {
            val rows = (0 until h).filter { y -> columns.any { x -> crop[y * w + x] == 1 } }
            return if (rows.isEmpty()) 0 else rows.max() - rows.min() + 1
        }
        for ((name, crop) in listOf("left to right" to ltr, "right to left" to rtl)) {
            val inked = (0 until w).filter { x -> (0 until h).any { y -> crop[y * w + x] == 1 } }
            assertTrue(inked.size > 100, "$name: the title's ink in ${inked.size} columns")
            val whole = tall(crop, inked.first()..inked.last())
            // The line's end is the ellipsis, three dots on the baseline; its start the capital «Э», the line's height.
            val end = tall(crop, inked.last() - 10..inked.last())
            val start = tall(crop, inked.first()..inked.first() + 10)
            assertTrue(end * 3 < whole, "$name: the line's last 11 columns stand $end px tall of its $whole")
            assertTrue(start * 3 > whole * 2, "$name: the line's first 11 columns stand $start px tall of its $whole")
        }
    }
}
