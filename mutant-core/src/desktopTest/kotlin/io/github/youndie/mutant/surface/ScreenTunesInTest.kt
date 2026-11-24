package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-180, the designer's answer 2: a screen shown through [MutantScreen] goes in one frame when the screen changes, and
 * the new one tunes in — the 16 dp mosaic, the 8 dp mosaic, the finished screen with its words, 320 ms in three steps —
 * from the frame the navigation bar's pill, the tabs' bar or a chip that chose it starts to move. Nothing slides, no
 * frame shows the two screens, and under reduce motion the new screen stands finished at once.
 *
 * The test clock moves in 16 ms frames; frame 0 is the composition that sees the pick. The tune's steps are read at
 * frames 0–6 (the 16 dp mosaic), 7–13 (the 8 dp one) and from 14 (224 ms, finished), as B-179 reads them. Every reading
 * counts pixels over an area or a whole grid of cells, never a few edge pixels.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenTunesInTest {
    private val titles = listOf("Ночной эфир", "Дождь над передатчиком")
    private val subtitles = listOf("Сторона A", "Сторона B")

    /** What picks the screen: a control over the screen, as a consumer lays it out. */
    private enum class Picker { Navigation, Tabs, Chips }

    private var picked by mutableIntStateOf(0)

    @Composable
    private fun Card(i: Int) {
        MutantCard(Modifier.width(300.dp), key = "screen$i") {
            MutantCardTitle(titles[i])
            MutantCardSubtitle(subtitles[i])
        }
    }

    @Composable
    private fun Stage(
        picker: Picker,
        colors: MutantColors,
        screen: Boolean,
    ) {
        // The picker under the screen, as a navigation bar stands: its body's drips hang down, away from the screen.
        Column(Modifier.background(colors.background).width(360.dp)) {
            Box(Modifier.testTag("screen").size(360.dp, 160.dp).padding(16.dp)) {
                when {
                    !screen -> Card(0)

                    // As the demo's shelf: an appearing body inside the screen tunes in with it, drawn by its mosaic.
                    picker == Picker.Chips -> MutantScreen(picked) { MutantAppear { Card(picked) } }

                    else -> MutantScreen(picked) { Card(picked) }
                }
            }
            // The navigation bar's wobbling top edge rises a little over its slot.
            Spacer(Modifier.height(24.dp))
            Box(Modifier.testTag("picker")) {
                when (picker) {
                    Picker.Navigation -> {
                        MutantNavigationBar(
                            listOf(
                                MutantNavigationItem("Эфир", MutantIcons.Home),
                                MutantNavigationItem("Очередь", MutantIcons.Schedule),
                            ),
                            picked,
                            { picked = it },
                            windowInsets = WindowInsets(0),
                        )
                    }

                    Picker.Tabs -> {
                        MutantTabs(listOf(MutantTab("Дорожки"), MutantTab("Заметки")), picked, { picked = it })
                    }

                    Picker.Chips -> {
                        Row(Modifier.padding(16.dp)) {
                            MutantChip("Все", picked == 0, { picked = 0 }, key = "chip0")
                            MutantChip("Шум", picked == 1, { picked = 1 }, key = "chip1")
                        }
                    }
                }
            }
        }
    }

    private class Shot(
        val ms: Int,
        val screen: PixelMap,
        val picker: PixelMap,
        val glyphs: Int,
        val oldWords: Int,
    )

    /** Frame -1 (before the pick), then every frame from the pick to [frames] × 16 ms later. */
    private fun ComposeUiTest.strip(
        picker: Picker,
        colors: MutantColors,
        d: Float,
        reduce: Boolean = false,
        screen: Boolean = true,
        frames: Int = 24,
    ): List<Shot> {
        mainClock.autoAdvance = false
        picked = 0
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d)) {
                MutantTheme(colors = colors, reduceMotion = reduce) { Stage(picker, colors, screen) }
            }
        }
        mainClock.advanceTimeBy(1000)

        fun shot(ms: Int): Shot {
            val px = onNodeWithTag("screen").captureToImage().toPixelMap()
            val origin = onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot.topLeft
            val words =
                titles.indices.associateWith { i ->
                    listOf(titles[i], subtitles[i]).flatMap { w ->
                        onAllNodesWithText(w, useUnmergedTree = true).fetchSemanticsNodes().map {
                            it.boundsInRoot.translate(-origin)
                        }
                    }
                }
            return Shot(
                ms,
                px,
                onNodeWithTag("picker").captureToImage().toPixelMap(),
                glyphs(px, words.getValue(1), listOf(colors.text, colors.textSecondary)),
                words.getValue(0).size,
            )
        }
        val before = shot(-16)
        picked = 1
        return listOf(before) + List(frames) { i -> mainClock.advanceTimeByFrame().let { shot(i * 16) } }
    }

    private fun near(
        a: Color,
        b: Color,
        within: Float,
    ) = abs(a.red - b.red) < within && abs(a.green - b.green) < within && abs(a.blue - b.blue) < within

    private fun glyphs(
        px: PixelMap,
        rects: List<Rect>,
        inks: List<Color>,
    ): Int =
        rects.sumOf { r ->
            var n = 0
            for (y in max(0, r.top.toInt()) until minOf(px.height, r.bottom.toInt())) {
                for (x in max(0, r.left.toInt()) until minOf(px.width, r.right.toInt())) {
                    if (inks.any { near(px[x, y], it, .08f) }) n++
                }
            }
            n
        }

    private fun PixelMap.edge(
        x: Int,
        y: Int,
    ): Boolean {
        val c = this[x, y]
        return (x > 0 && !near(c, this[x - 1, y], .02f)) || (x < width - 1 && !near(c, this[x + 1, y], .02f)) ||
            (y > 0 && !near(c, this[x, y - 1], .02f)) || (y < height - 1 && !near(c, this[x, y + 1], .02f))
    }

    /** Pixels other than the background, and the share of them in a mosaic's paints (the fills and the cocoon's). */
    private fun paintShare(
        px: PixelMap,
        colors: MutantColors,
    ): Pair<Int, Double> {
        val paints =
            listOf(
                colors.primary,
                colors.surface,
                colors.surfaceVariant,
                colors.phosphor,
                colors.magenta,
                colors.noSignal,
                colors.amber,
                colors.cocoon1,
                colors.cocoon2,
            )
        var other = 0
        var painted = 0
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val c = px[x, y]
                if (near(c, colors.background, .02f)) continue
                other++
                if (px.edge(x, y) || paints.any { near(c, it, .02f) }) painted++
            }
        }
        return other to if (other == 0) 0.0 else painted.toDouble() / other
    }

    /** Pixels in the outline's ink: a finished body's, never a mosaic's. */
    private fun inked(
        px: PixelMap,
        colors: MutantColors,
    ): Int = (0 until px.height).sumOf { y -> (0 until px.width).count { x -> near(px[x, y], colors.ink, .04f) } }

    /** The bounds of what is drawn over the background. */
    private fun drawn(
        px: PixelMap,
        bg: Color,
    ): IntArray {
        var x0 = Int.MAX_VALUE
        var y0 = Int.MAX_VALUE
        var x1 = -1
        var y1 = -1
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                if (!near(px[x, y], bg, .02f)) {
                    x0 = minOf(x0, x)
                    y0 = minOf(y0, y)
                    x1 = max(x1, x)
                    y1 = max(y1, y)
                }
            }
        }
        return intArrayOf(x0, y0, x1, y1)
    }

    /** The share of whole [side]-pixel cells laid from where the picture starts that are one colour 2 px in. */
    private fun uniformCells(
        px: PixelMap,
        side: Int,
        bg: Color,
    ): Double {
        val (x0, y0, x1, y1) = drawn(px, bg).toList()
        var cells = 0
        var uniform = 0
        for (r in 0 until (y1 - y0 + 1) / side) {
            for (c in 0 until (x1 - x0 + 1) / side) {
                cells++
                var ref: Color? = null
                var same = true
                for (y in y0 + r * side + 2 until y0 + (r + 1) * side - 2) {
                    for (x in x0 + c * side + 2 until x0 + (c + 1) * side - 2) {
                        val p = px[x, y]
                        if (near(p, bg, .02f) || px.edge(x, y)) continue
                        if (ref == null) {
                            ref = p
                        } else if (!near(p, ref, .02f)) {
                            same = false
                        }
                    }
                }
                if (same) uniform++
            }
        }
        return if (cells == 0) 0.0 else uniform.toDouble() / cells
    }

    private fun differing(
        a: PixelMap,
        b: PixelMap,
    ): Int = (0 until a.height).sumOf { y -> (0 until a.width).count { x -> !near(a[x, y], b[x, y], .02f) } }

    /** Where the [colors]' primary stands across the picker, px: the pill's or the bar's middle, or null if none. */
    private fun primaryX(
        px: PixelMap,
        colors: MutantColors,
        rows: Int,
    ): Double? {
        var sum = 0.0
        var n = 0
        for (y in max(0, px.height - rows) until px.height) {
            for (x in 0 until px.width) {
                if (near(px[x, y], colors.primary, .03f)) {
                    sum += x
                    n++
                }
            }
        }
        return if (n < 20) null else sum / n
    }

    /** The screen's swap: the old one whole before the pick, then B-179's steps from the pick's own frame. */
    private fun checkScreen(
        strip: List<Shot>,
        colors: MutantColors,
        d: Float,
        tag: String,
    ) {
        val before = strip[0]
        assertTrue(before.oldWords > 0 && inked(before.screen, colors) > 100 * d, "$tag: the old screen whole before")
        val shots = strip.drop(1)
        for (s in shots) assertEquals(0, s.oldWords, "$tag: the old screen gone from the pick's frame on, ${s.ms} ms")
        val coarse = shots.filter { it.ms <= 96 }
        val fine = shots.filter { it.ms in 112..208 }
        val whole = shots.filter { it.ms >= 224 }
        for (s in coarse + fine) {
            assertEquals(0, s.glyphs, "$tag: no word before the third step, ${s.ms} ms")
            assertEquals(0, inked(s.screen, colors), "$tag: no outline, old or new, at ${s.ms} ms")
            val (n, share) = paintShare(s.screen, colors)
            assertTrue(n > 400 * d * d, "$tag: a mosaic stands at ${s.ms} ms ($n px)")
            assertTrue(share > .97, "$tag: only the mosaic's paints at ${s.ms} ms: $share")
        }
        for (s in coarse) {
            val c = uniformCells(s.screen, (16 * d).toInt(), colors.background)
            assertTrue(c > .95, "$tag: 16 dp blocks at ${s.ms} ms: $c")
        }
        for (s in fine) {
            val c8 = uniformCells(s.screen, (8 * d).toInt(), colors.background)
            val c16 = uniformCells(s.screen, (16 * d).toInt(), colors.background)
            assertTrue(c8 > .95 && c16 < .8, "$tag: 8 dp blocks at ${s.ms} ms: $c8, $c16")
        }
        // Nothing slides: each step's picture stands still, frame after frame.
        for (step in listOf(coarse, fine)) {
            for (s in step.drop(1)) {
                val moved = differing(step[0].screen, s.screen)
                assertTrue(moved < 50 * d * d, "$tag: the step stands still at ${s.ms} ms ($moved px moved)")
            }
        }
        for (s in whole) {
            assertTrue(s.glyphs > 40 * d * d, "$tag: the new words at ${s.ms} ms (${s.glyphs} px)")
            assertTrue(inked(s.screen, colors) > 100 * d, "$tag: the new body finished at ${s.ms} ms")
        }
    }

    /** The pill's or the bar's run starts in the pick's frame: still where it stood there, moved the frame after. */
    private fun checkRunStarts(
        strip: List<Shot>,
        colors: MutantColors,
        d: Float,
        tag: String,
        movedDp: Double,
        rows: Int = Int.MAX_VALUE,
    ) {
        val x = strip.take(3).map { primaryX(it.picker, colors, rows) ?: error("$tag: no pill or bar at ${it.ms} ms") }
        assertTrue(abs(x[1] - x[0]) < 1.5 * d, "$tag: at the pick's frame the run is at its start (${x[0]} → ${x[1]})")
        assertTrue(abs(x[2] - x[1]) > movedDp * d, "$tag: one frame on it has moved (${x[1]} → ${x[2]})")
    }

    @Test
    fun `a new screen tunes in as the navigation bar's pill starts for it`() {
        for ((colors, d) in listOf(MutantPalettes.Efir to 1f, MutantPalettes.Kopiya to 2f)) {
            runComposeUiTest {
                val strip = strip(Picker.Navigation, colors, d)
                checkScreen(strip, colors, d, "navigation at density $d")
                // The pill runs one cell, 180 dp over two destinations: by 16 ms of 330, eased `1 − (1 − p)²`, 17 dp.
                checkRunStarts(strip, colors, d, "navigation at density $d", movedDp = 4.0)
            }
        }
    }

    @Test
    fun `a new screen tunes in as the tabs' bar starts for it`() {
        for ((colors, d) in listOf(MutantPalettes.Kopiya to 1f, MutantPalettes.Efir to 2f)) {
            runComposeUiTest {
                val strip = strip(Picker.Tabs, colors, d)
                checkScreen(strip, colors, d, "tabs at density $d")
                // The bar runs along the row's foot, under the labels, whose accent is the same paint.
                checkRunStarts(strip, colors, d, "tabs at density $d", movedDp = 2.0, rows = (6 * d).toInt())
            }
        }
    }

    @Test
    fun `a new screen tunes in as the chip starts to pour, the bodies appearing inside it with it`() {
        for ((colors, d) in listOf(MutantPalettes.Efir to 1f, MutantPalettes.Kopiya to 2f)) {
            var with = emptyList<Shot>()
            runComposeUiTest { with = strip(Picker.Chips, colors, d, frames = 32) }
            checkScreen(with, colors, d, "chips at density $d")
            // The chip's pour, frame by frame, is the one it runs with no screen to swap: it starts in the same frame.
            var alone = emptyList<Shot>()
            runComposeUiTest { alone = strip(Picker.Chips, colors, d, screen = false, frames = 32) }
            for ((a, b) in alone.zip(with)) {
                val apart = differing(a.picker, b.picker)
                assertEquals(0, apart, "chips at density $d: the pour as without a screen at ${a.ms} ms")
            }
            assertTrue(
                alone.zipWithNext().any { (a, b) -> differing(a.picker, b.picker) > 50 * d * d },
                "chips at density $d: the chip pours",
            )
        }
    }

    @Test
    fun `under reduce motion the new screen stands finished at once`() {
        for (picker in Picker.entries) {
            runComposeUiTest {
                val colors = MutantPalettes.Efir
                val strip = strip(picker, colors, 1f, reduce = true, frames = 2)
                val first = strip[1]
                assertEquals(0, first.oldWords, "$picker: the old screen gone")
                assertTrue(first.glyphs > 40, "$picker: the new words in the pick's frame (${first.glyphs} px)")
                assertTrue(inked(first.screen, colors) > 100, "$picker: the new body finished in the pick's frame")
            }
        }
    }
}
