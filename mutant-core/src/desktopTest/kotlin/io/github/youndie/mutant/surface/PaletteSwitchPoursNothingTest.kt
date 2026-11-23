package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-175: a palette switch is no toggle. Every body shows the new palette's fill from the switch's first frame, in both
 * directions and under a dialog's held clock; a toggle still pours. Density 2, the test's clock advanced by hand one
 * frame at a time; every reading is of drawn pixels. Each switch is read over [FRAMES] frames, past the longest pour
 * (620 ms).
 */
@OptIn(ExperimentalTestApi::class)
class PaletteSwitchPoursNothingTest {
    /**
     * Composes [content] in «Эфир», lets it settle, then calls [step] before each of [frames] frames (its index from 1)
     * and [read] after it, with the palette shown then.
     */
    private fun film(
        frames: Int,
        step: (Int, Switches) -> Unit,
        content: @Composable (Switches) -> Unit,
        read: ComposeUiTest.(Int, MutantColors, PixelMap) -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        val s = Switches()
        setContent {
            val colors = if (s.print) MutantPalettes.Kopiya else MutantPalettes.Efir
            CompositionLocalProvider(LocalDensity provides Density(D)) {
                MutantTheme(colors = colors, reduceMotion = false) {
                    Box(Modifier.background(colors.background).testTag("f")) { content(s) }
                }
            }
        }
        repeat(WARM_UP) { mainClock.advanceTimeByFrame() }
        for (i in 1..frames) {
            step(i, s)
            mainClock.advanceTimeByFrame()
            read(
                i,
                if (s.print) MutantPalettes.Kopiya else MutantPalettes.Efir,
                onNodeWithTag("f").captureToImage().toPixelMap(),
            )
        }
    }

    private class Switches {
        var print by mutableStateOf(false)
        var selected by mutableStateOf(false)
    }

    /** Switches the palette at frame 1 and back at frame [FRAMES] + 1. */
    private val there: (Int, Switches) -> Unit = { i, s -> if (i == 1 || i == FRAMES + 1) s.print = !s.print }

    private fun ComposeUiTest.box(
        text: String,
        merged: Boolean = false,
    ): Rect {
        val b = onNodeWithText(text, useUnmergedTree = !merged).getBoundsInRoot()
        return Rect(b.left.value * D, b.top.value * D, b.right.value * D, b.bottom.value * D)
    }

    private fun ComposeUiTest.tagged(tag: String): Rect {
        val b = onNodeWithTag(tag, useUnmergedTree = true).getBoundsInRoot()
        return Rect(b.left.value * D, b.top.value * D, b.right.value * D, b.bottom.value * D)
    }

    @Composable
    private fun aCard(modifier: Modifier = Modifier) {
        MutantCard(modifier.width(300.dp).testTag("card")) {
            MutantCardTitle(TITLE)
            MutantCardSubtitle(SUBTITLE)
        }
    }

    /** The other palette's fill of a card, a surface: the colour a pour from the old palette shows. */
    private fun other(c: MutantColors) = if (c === MutantPalettes.Kopiya) MutantPalettes.Efir else MutantPalettes.Kopiya

    private fun cardWords(
        held: Boolean,
        out: MutableList<String>,
    ) = film(2 * FRAMES, there, { _ ->
        if (held) {
            MutantScreenClock {
                Column(Modifier.size(360.dp, 420.dp)) {
                    Box(Modifier.padding(16.dp)) { aCard() }
                    Box(
                        Modifier.size(360.dp, 280.dp),
                    ) { MutantDialog(true, "Стереть?", {}) { MutantDialogAction("Да", {}) } }
                }
            }
        } else {
            Box(Modifier.padding(16.dp)) { aCard() }
        }
    }) { i, c, px ->
        val title = wordContrast(px, box(TITLE))
        val subtitle = wordContrast(px, box(SUBTITLE))
        val old = count(px, tagged("card"), other(c).surface)
        if (title < WORDS || subtitle < WORDS || old > OLD_FILL_AT_REST) {
            out +=
                "frame $i: title ${"%.2f".format(
                    title,
                )}:1, subtitle ${"%.2f".format(subtitle)}:1, $old px of the old fill"
        }
    }

    @Test
    fun `a card's words stand on the new palette's fill from the switch's first frame, both ways`() {
        // Before: the title 1.46:1 for 5 frames and the subtitle 1.85:1 for 9, the old fill in the body for 620 ms.
        // Now: 13.62 and 5.43 in «Копия», 12.06 and 6.69 in «Эфир», on every frame; the bound is 4.5:1.
        val out = ArrayList<String>()
        cardWords(held = false, out)
        assertTrue(out.isEmpty(), out.joinToString("\n"))
    }

    @Test
    fun `under a dialog's held clock a card takes the new palette's fill at once`() {
        // Before: the held screen never ran its pour: the title stood at 1.46:1 for the dialog's whole life.
        val out = ArrayList<String>()
        cardWords(held = true, out)
        assertTrue(out.isEmpty(), out.joinToString("\n"))
    }

    @Test
    fun `the cassette's reels stay on their window through a palette switch, both ways`() {
        // Before: 1.03:1 for 144 ms into «Копия», 1.06:1 back. Now 15.52 and 15.82; the bound is 4.5:1.
        val out = ArrayList<String>()
        film(2 * FRAMES, there, { _ ->
            Box(Modifier.padding(16.dp)) {
                MutantEmptyState(
                    MutantIllustrationKind.Cassette,
                    "Здесь пока пусто",
                    Modifier.width(328.dp).testTag("e"),
                )
            }
        }) { i, _, px ->
            val e = onNodeWithTag("e").getBoundsInRoot()
            val left = e.left.value + (e.right.value - e.left.value - CASSETTE_W) / 2
            val top = e.top.value
            for ((reelX, name) in listOf(LEFT_REEL_X to "left", RIGHT_REEL_X to "right")) {
                val reel = px[((left + reelX) * D).roundToInt(), ((top + REEL_Y) * D).roundToInt()].copy(alpha = 1f)
                val window =
                    px[((left + WINDOW_X) * D).roundToInt(), ((top + REEL_Y) * D).roundToInt()].copy(
                        alpha = 1f,
                    )
                val c = contrast(reel, window)
                if (c < WORDS) out += "frame $i: the $name reel ${"%.2f".format(c)}:1 on its window"
            }
        }
        assertTrue(out.isEmpty(), out.joinToString("\n"))
    }

    @Test
    fun `a toggle still pours, and a palette switch mid-pour lands it`() {
        // A chip selected at frame 1 pours lime from the top over its 420 ms; the palette switched at frame 8, half way,
        // lands the pour: from that frame no pixel of «Эфир»'s fill is left in the chip.
        val lime = ArrayList<Int>()
        val oldFill = ArrayList<Int>()
        film(FRAMES, { i, s ->
            if (i == 1) s.selected = true
            if (i == MID_POUR) s.print = true
        }, { s ->
            Box(Modifier.padding(16.dp)) { MutantChip(CHIP, s.selected, {}, key = "chip") }
        }) { _, _, px ->
            val chip = box(CHIP, merged = true).inflate(8 * D)
            lime += count(px, chip, MutantPalettes.Efir.primary)
            oldFill += count(px, chip, CHIP_FILL_EFIR)
        }
        val full = lime.last()
        // The pour runs: the first frame after the toggle shows under a fifth of the poured lime, and it grows over
        // at least four frames before the switch (on `main`, too: this is the pour as the engine draws it).
        assertTrue(lime[0] < full / 5, "the toggle's first frame: ${lime[0]} of $full lime px")
        assertTrue((1 until MID_POUR - 1).count { lime[it] > lime[it - 1] } >= 4, "lime by frame: $lime")
        assertTrue(lime[MID_POUR - 2] < full * 9 / 10, "still pouring before the switch: $lime")
        // The switch lands it: the old palette's fill is gone from the switch's frame on, and the lime is whole.
        assertTrue(oldFill[MID_POUR - 2] > 0, "the old fill shows before the switch: $oldFill")
        assertEquals(0, oldFill.drop(MID_POUR - 1).max(), "the old fill after the switch, by frame: $oldFill")
        assertTrue(lime[MID_POUR - 1] >= full * 9 / 10, "whole at the switch: ${lime[MID_POUR - 1]} of $full")
    }

    /** The word's strongest ink against the ground 6 dp left of its box, at its middle. */
    private fun wordContrast(
        px: PixelMap,
        box: Rect,
    ): Float {
        val ground = px[(box.left - 6 * D).roundToInt(), box.center.y.roundToInt()].copy(alpha = 1f)
        var best = 1f
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                best = maxOf(best, contrast(px[x, y].copy(alpha = 1f), ground))
            }
        }
        return best
    }

    /** Pixels in [r] within .12 of [c] (the sum of the channels' differences). */
    private fun count(
        px: PixelMap,
        r: Rect,
        c: Color,
    ): Int {
        var n = 0
        for (y in maxOf(0, r.top.roundToInt()) until minOf(px.height, r.bottom.roundToInt())) {
            for (x in maxOf(0, r.left.roundToInt()) until minOf(px.width, r.right.roundToInt())) {
                val p = px[x, y]
                if (abs(p.red - c.red) + abs(p.green - c.green) + abs(p.blue - c.blue) < .12f) n++
            }
        }
        return n
    }

    private companion object {
        const val D = 2f
        const val WARM_UP = 24

        /** 640 ms: past the longest pour, 620. */
        const val FRAMES = 40

        /** The frame a toggled chip's palette switches at, half its 420 ms pour. */
        const val MID_POUR = 8

        /** WCAG's bound for words, where every face read 1.03–1.87:1 before. */
        const val WORDS = 4.5f
        const val TITLE = "Без подписи №3"
        const val SUBTITLE = "1987 · 180 мин"
        const val CHIP = "Компоненты"

        /**
         * Pixels of the other palette's surface a resting card shows anyway: 36 in «Эфир» (its highlight and speckle come
         * near «Копия»'s cream), 0 in «Копия». A pour from the old fill shows thousands (the card is 600 × 176 px).
         */
        const val OLD_FILL_AT_REST = 100

        /** An unselected chip's fill in «Эфир», the profile's `surfaceVariant`. */
        val CHIP_FILL_EFIR = MutantPalettes.Efir.surfaceVariant

        // The cassette (MutantIllustration.cassette): 216 dp wide, centred; its window at 44, 66, 128 × 40, the reels
        // 26 dp at 8 and 94 in it. Read at the reels' centres (never a hole) and between them, at the window's middle row.
        const val CASSETTE_W = 216f
        const val LEFT_REEL_X = 44f + 8f + 13f
        const val RIGHT_REEL_X = 44f + 94f + 13f
        const val WINDOW_X = 44f + 64f
        const val REEL_Y = 66f + 7f + 13f
    }
}
