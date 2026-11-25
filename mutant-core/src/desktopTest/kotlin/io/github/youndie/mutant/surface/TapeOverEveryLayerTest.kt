package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantMenu
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-197, the designer's answers 22 and 23: the Tape is the window's topmost layer, over menus, tooltips and dialogs, and it
 * runs while a dialog is open.
 *
 * A popup is a root of its own, so the Tape over it is drawn there, in register with the window's. Each case reads one
 * part of the Tape alone (the band, or the head's noise) as the pixels it changes against the same scene with no Tape
 * part drawn, over the popup's body: with the popup open, and with it gone, where the part lands on the ground under it.
 * Over the popup the part must change at least half the pixels it changes on the ground there (text the same colour as
 * the band's ink stays as it is), and almost nothing the ground's does not: a Tape out of register, or past the Tape's own
 * edge, changes pixels the ground's does not. The clock is frozen at a moment the band crosses the popup.
 */
@OptIn(ExperimentalTestApi::class)
class TapeOverEveryLayerTest {
    private val rows = listOf("СБРОС", "ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ")

    /** The four pictures of one case: the popup open or not, the Tape's part drawn or not. */
    private class Shots(
        val popupBare: PixelMap,
        val popupTaped: PixelMap,
        val groundBare: PixelMap,
        val groundTaped: PixelMap,
        /** The popup's body, inset clear of its outline, window pixels. */
        val body: Rect,
        /** Beside the popup, past its outline and shadow but within what its layer takes in, window pixels. */
        val beside: Rect,
    )

    /**
     * The window filled at [d], a Tape of [TAPE_W] × [tapeH] dp at its top left on a frozen clock at [ms], and [popup]
     * standing over its bottom edge when shown; only [part] of the Tape is drawn when the Tape is on. [beside] is the
     * strip [besideFrom] to 26 dp right of the popup's [body], along it: past the popup's own pixels, inside the 28 dp
     * its layer takes in.
     */
    private fun shots(
        colors: MutantColors,
        d: Float,
        ms: Double,
        tapeH: Float,
        part: TapePart,
        popup: @Composable (shown: Boolean) -> Unit,
        open: ComposeUiTest.() -> Unit,
        body: ComposeUiTest.() -> Rect,
        besideFrom: Float,
    ): Shots {
        lateinit var shots: Shots
        runComposeUiTest {
            mainClock.autoAdvance = false
            var shown by mutableStateOf(true)
            var skip by mutableStateOf(TapePart.entries.toSet())
            val clock = MutantClock(startMs = ms, frozen = true)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(d), LocalMutantTapeSkip provides skip) {
                    MutantTheme(colors = colors, reduceMotion = false) {
                        ProvideMutantClock(clock) {
                            Box(Modifier.testTag("w").size((1024 / d).dp, (768 / d).dp).background(colors.background)) {
                                Box(Modifier.size(TAPE_W.dp, tapeH.dp).mutantTape()) { popup(shown) }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(100)
            open()
            mainClock.advanceTimeBy(100)
            val b = body()

            fun shot(): PixelMap {
                mainClock.advanceTimeBy(48)
                return onNodeWithTag("w").captureToImage().toPixelMap()
            }
            val popupBare = shot()
            skip = TapePart.entries.toSet() - part
            val popupTaped = shot()
            shown = false
            val groundTaped = shot()
            skip = TapePart.entries.toSet()
            val groundBare = shot()
            val beside = Rect(b.right + besideFrom * d, b.top, b.right + 26 * d, b.bottom)
            shots = Shots(popupBare, popupTaped, groundBare, groundTaped, b, beside)
        }
        return shots
    }

    private fun changed(
        a: PixelMap,
        b: PixelMap,
        x: Int,
        y: Int,
    ): Boolean {
        val p = a[x, y]
        val q = b[x, y]
        return abs(p.red - q.red) * 255 >= 3 || abs(p.green - q.green) * 255 >= 3 || abs(p.blue - q.blue) * 255 >= 3
    }

    /** Over the popup's body: what the part changes on the ground, over the popup, both, and over the popup alone. */
    private fun read(s: Shots): IntArray {
        var ground = 0
        var over = 0
        var both = 0
        for (y in s.body.top.roundToInt() until s.body.bottom.roundToInt()) {
            for (x in s.body.left.roundToInt() until s.body.right.roundToInt()) {
                val g = changed(s.groundBare, s.groundTaped, x, y)
                val p = changed(s.popupBare, s.popupTaped, x, y)
                if (g) ground++
                if (p) over++
                if (g && p) both++
            }
        }
        return intArrayOf(ground, over, both, over - both)
    }

    private fun assertOver(
        name: String,
        s: Shots,
        least: Int,
    ) {
        val (ground, over, both, stray) = read(s).toList()
        val what = "$name: on the ground $ground px, over the popup $over, both $both, over the popup alone $stray"
        assertTrue(ground >= least, "the part lands under the popup's body at all — $what")
        assertTrue(both * 2 >= ground, "the part is drawn over the popup where it lands under it — $what")
        assertTrue(
            stray * 20 <= over.coerceAtLeast(1),
            "the part over the popup is in register with the one under it — $what",
        )
        // Beside the popup the window's Tape shows once: the popup's own is drawn over the popup's pixels only.
        var twice = 0
        var all = 0
        for (y in s.beside.top.roundToInt() until s.beside.bottom.roundToInt()) {
            for (x in s.beside.left.roundToInt() until s.beside.right.roundToInt()) {
                all++
                if (changed(s.popupTaped, s.groundTaped, x, y)) twice++
            }
        }
        assertTrue(twice * 100 <= all, "$name: beside the popup $twice of $all px differ from the ground's Tape")
    }

    /** A menu at 120 dp in a Tape 200 dp tall: its rows from 120 dp on, past the Tape's foot at 200. */
    private fun menu(
        colors: MutantColors,
        d: Float,
        part: TapePart,
    ): Shots =
        shots(
            colors,
            d,
            // The band's top at 150 dp of the Tape's 200: 2100 × 50 / 230 ms into the cycle.
            ms = 456.5,
            tapeH = 200f,
            part = part,
            popup = { shown ->
                MutantMenu(shown, {}, rows, -1, {}, offset = DpOffset(20.dp, 120.dp))
            },
            open = {},
            body = {
                val r =
                    rows
                        .map { onAllNodesWithText(it).fetchSemanticsNodes().single().boundsInWindow }
                        .reduce {
                            a,
                            b,
                            ->
                            Rect(
                                minOf(a.left, b.left),
                                minOf(a.top, b.top),
                                maxOf(a.right, b.right),
                                maxOf(a.bottom, b.bottom),
                            )
                        }
                r.deflate(2 * d)
            },
            // The rows stop 6 dp inside the window's edge; its shadow stands 4 dp past that.
            besideFrom = 14f,
        )

    /** A tooltip's plate from 82 to 112 dp, over the foot of a Tape 100 dp tall. */
    private fun tooltip(
        colors: MutantColors,
        d: Float,
        part: TapePart,
    ): Shots =
        shots(
            colors,
            d,
            // The band's top at 80 dp of the Tape's 100: 2100 × 20 / 130 ms into the cycle.
            ms = 323.0,
            tapeH = 100f,
            part = part,
            popup = { shown ->
                if (shown) {
                    MutantTooltipBox(TIP, Modifier.offset(140.dp, 140.dp).testTag("anchor")) {
                        MutantIconButton(MutantIcons.Share, "Поделиться", {})
                    }
                }
            },
            open = { onNodeWithTag("anchor").performMouseInput { moveTo(center) } },
            body = {
                val word = onAllNodesWithText(TIP, useUnmergedTree = true).fetchSemanticsNodes().single().boundsInWindow
                // The plate is 12 dp either side of its word and 30 dp tall around it; 2 dp clear of its outline.
                val mid = (word.top + word.bottom) / 2
                Rect(word.left - 10 * d, mid - 13 * d, word.right + 10 * d, mid + 13 * d)
            },
            // The plate's edge 2 dp past the body read, its shadow 4 dp past that.
            besideFrom = 10f,
        )

    @Test
    fun `the Tape's band is drawn over an open menu, in register with the band under it`() {
        assertOver("Efir, density 1", menu(MutantPalettes.Efir, 1f, TapePart.Band), least = 2_000)
        assertOver("Kopiya, density 2", menu(MutantPalettes.Kopiya, 2f, TapePart.Band), least = 8_000)
    }

    @Test
    fun `the Tape's noise is drawn over an open menu, in register with the noise under it`() {
        assertOver("Efir, density 1", menu(MutantPalettes.Efir, 1f, TapePart.Noise), least = 100)
        assertOver("Kopiya, density 2", menu(MutantPalettes.Kopiya, 2f, TapePart.Noise), least = 400)
    }

    @Test
    fun `the Tape's band is drawn over a tooltip's plate, in register with the band under it`() {
        assertOver("Efir, density 2", tooltip(MutantPalettes.Efir, 2f, TapePart.Band), least = 2_000)
        assertOver("Kopiya, density 1", tooltip(MutantPalettes.Kopiya, 1f, TapePart.Band), least = 500)
    }

    @Test
    fun `the Tape's noise is drawn over a tooltip's plate, in register with the noise under it`() {
        assertOver("Efir, density 2", tooltip(MutantPalettes.Efir, 2f, TapePart.Noise), least = 100)
        assertOver("Kopiya, density 1", tooltip(MutantPalettes.Kopiya, 1f, TapePart.Noise), least = 30)
    }

    /**
     * The rows of [cols] of a picture that changed between [a] and [b]: those where at least 60 % of the columns did. A
     * band passing changes its rows across the screen alike, the dialog's window included.
     */
    private fun changedRows(
        a: PixelMap,
        b: PixelMap,
        cols: IntRange,
        rows: IntRange,
    ): Set<Int> = rows.filter { y -> cols.count { x -> changed(a, b, x, y) } * 10 >= cols.count() * 6 }.toSet()

    @Test
    fun `the Tape runs while a dialog is open, its band over the window in register with the band beside it`() {
        // A screen of 360 × 400 dp under its Tape, the dialog's window 40 to 320 dp across; only the band is drawn. The
        // clock runs (no reduce motion), and the dialog holds it from its first frame.
        val shots = ArrayList<PixelMap>()
        var window = Rect.Zero
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalMutantTapeSkip provides (TapePart.entries.toSet() - TapePart.Band)) {
                    MutantTheme(colors = MutantPalettes.Efir, reduceMotion = false) {
                        Box(
                            Modifier
                                .testTag(
                                    "w",
                                ).size(360.dp, 400.dp)
                                .background(MutantPalettes.Efir.background)
                                .mutantTape(),
                        ) {
                            MutantDialog(true, "Стереть?", {}) { MutantDialogAction("Да", {}) }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            window =
                onAllNodesWithText("Стереть?", useUnmergedTree = true).fetchSemanticsNodes().single().boundsInWindow
            repeat(8) {
                shots += onNodeWithTag("w").captureToImage().toPixelMap()
                mainClock.advanceTimeBy(240)
            }
        }
        // Beside the window: the screen's left 4–20 dp, under the scrim; in it: its right 290–312 dp, clear of its words.
        val beside = 4 until 20
        val inside = 290 until 312
        // The window's rows: from its title to well above its foot, inside its frame.
        val windowRows = (window.top.roundToInt() - 8) until (window.top.roundToInt() + 70)
        var crossed = 0
        var moved = 0
        for (k in 1 until shots.size) {
            val all = 0 until 400
            val b = changedRows(shots[k - 1], shots[k], beside, all)
            val w = changedRows(shots[k - 1], shots[k], inside, windowRows)
            if (b.isNotEmpty()) moved++
            val bw = b intersect windowRows.toSet()
            crossed += w.size
            val off = (bw - w) + (w - bw)
            assertTrue(
                off.size <= 2,
                "frame $k: the band's rows in the window ${w.sorted()} against beside it ${bw.sorted()}",
            )
        }
        assertTrue(
            moved >= 5,
            "the band moves while the dialog is open: it moved between $moved of ${shots.size - 1} shots",
        )
        assertTrue(crossed >= 20, "the band crosses the dialog's window: $crossed rows changed there")
    }

    private companion object {
        const val TAPE_W = 320f
        const val TIP = "Поделиться"
    }
}
