package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.Extent
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-23: the nav stage's top app bar and tabs. What the engine and the bar draw under the words — the tracking divider,
 * the tab bar with its ring and echo — is held whole to the floor; the words above by where each one's ink lies and
 * its colour (research §1.14).
 */
class BarTabsParityTest {
    private fun cell(
        stem: String,
        theme: String,
        content: @Composable (dy: Float) -> Unit,
    ): DesignParity.Result {
        val ref = DesignParity.reference(stem)
        val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
        // The element stands a fraction of a pixel down the page (0.19); Chrome draws these rows whole, where it stands
        // rounded, and so does the fixture.
        val dy = round((ref["subpixel"] as JsonArray)[1].jsonPrimitive.double).toFloat()
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(LocalMutantStage provides "nav", LocalMutantTextRaster provides ChromeText) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        Box(Modifier.fillMaxSize().background(ground)) { content(dy) }
                    }
                }
            }
        }
    }

    /** Runs of inked columns over rows [0, rows), split where [gap] or more columns stand empty. */
    private fun words(
        r: DesignParity.Result,
        px: IntArray,
        rows: Int,
        g: Int,
        gap: Int,
    ): List<IntRange> {
        val inked =
            BooleanArray(r.width) { x ->
                (0 until rows).any { y ->
                    channelGap(px[y * r.width + x], g) > TEXT_INK
                }
            }
        val out = ArrayList<IntRange>()
        var start = -1
        var empty = 0
        for (x in 0 until r.width) {
            if (inked[x]) {
                if (start < 0) start = x
                empty = 0
            } else if (start >= 0 && ++empty >= gap) {
                out += start..(x - empty)
                start = -1
            }
        }
        if (start >= 0) out += start..(r.width - 1 - empty)
        return out
    }

    /** The extent of one word's ink in [px], within [range] widened by a few columns, over rows [0, rows). */
    private fun extent(
        r: DesignParity.Result,
        px: IntArray,
        rows: Int,
        g: Int,
        range: IntRange,
    ): Extent? =
        extentOf(r.width, rows) { i ->
            i % r.width in (range.first - 4)..(range.last + 4) && channelGap(px[i], g) > TEXT_INK
        }

    /** The colour of the inkiest pixel in [range]: a glyph's whole-coverage pixel, the text's colour in either. */
    private fun ink(
        r: DesignParity.Result,
        px: IntArray,
        rows: Int,
        g: Int,
        range: IntRange,
    ): Int =
        (0 until r.width * rows)
            .filter { it % r.width in range }
            .maxBy { channelGap(px[it], g) }
            .let { px[it] }

    /** The words above [rows] and what is drawn from there down: (verdict lines, whether all holds). */
    private fun hold(
        r: DesignParity.Result,
        ground: Color,
        rows: Int,
        gap: Int,
        expected: Int,
        lines: MutableList<String>,
    ): Boolean {
        val side = SIDE_RESIDUAL[r.stem] ?: 1
        val topPx = TOP_RESIDUAL[r.stem] ?: 0
        val g =
            ((ground.red * 255).toInt() shl 16) or ((ground.green * 255).toInt() shl 8) or (ground.blue * 255).toInt()
        var under = 0
        for (i in rows * r.width until r.width * r.height) if (channelGap(r.actual[i], r.reference[i]) > 16) under++
        val pageWords = words(r, r.reference, rows, g, gap)
        val kitWords = words(r, r.actual, rows, g, gap)
        var ok = under <= ComponentParity.FLOOR_PIXELS && pageWords.size == expected && kitWords.size == expected
        val parts = ArrayList<String>()
        for (range in pageWords) {
            val page = extent(r, r.reference, rows, g, range)
            val kit = extent(r, r.actual, rows, g, range)
            val colour = channelGap(ink(r, r.actual, rows, g, range), ink(r, r.reference, rows, g, range))
            parts += "kit $kit page $page colour $colour"
            if (kit == null || page == null || !kit.near(page, px = side, topPx = topPx) || colour > 16) ok = false
        }
        val counts = "words kit ${kitWords.size} page ${pageWords.size}"
        lines += "${r.stem}: under the words $under px; $counts; ${parts.joinToString("; ")}"
        return ok
    }

    @Test
    fun `the top app bar is the page's - its divider, and its icon, title and status where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantTopAppBar_Rest_$theme"
            val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
            val r =
                cell(stem, theme) { dy ->
                    MutantTopAppBar(
                        "Эфир",
                        Modifier.offset(y = dy.dp),
                        navigationIcon = { MutantIcon(MutantIcons.Menu, null) },
                        status = "CH 03 · SP",
                        windowInsets = WindowInsets(0),
                        key = "0",
                    )
                }
            // The 56 dp row's icon, title and status, 12 dp or more apart; the divider from row 56.
            if (!hold(r, ground, 56, 12, 3, lines)) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    @Test
    fun `the tabs are the page's - the bar with its ring and echo, and the labels where the page puts them`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantTabs_Rest_$theme"
            val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
            val r =
                cell(stem, theme) { dy ->
                    // The tabs' targets reach 12 dp past the first label and 1 dp above the row the page draws.
                    CompositionLocalProvider(LocalTabBarWidth provides 62f) {
                        MutantTabs(
                            listOf(MutantTab("Дорожки"), MutantTab("Заметки"), MutantTab("Копии")),
                            0,
                            {},
                            // Unbounded: a child taller or wider than its cell is otherwise centred on it.
                            Modifier
                                .wrapContentSize(Alignment.TopStart, unbounded = true)
                                .offset((-12).dp, (dy - 1).dp)
                                .width(352.dp),
                        )
                    }
                }
            // The labels' 36 dp row, 24 dp apart; from row 38 the bar (its ring from 40.5).
            if (!hold(r, ground, 38, 16, 3, lines)) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /**
         * Over a pixel to a side, measured on 2026-11-14 and never to be raised: Compose advances Rubik 14/500 by whole
         * pixels per glyph (the labels 65, 60 and 44 wide) where Chrome's advances are fractional, and the drift adds up
         * along the row — the third label 2 px right in «Эфир» (1 in «Копия»).
         */
        val SIDE_RESIDUAL = mapOf("MutantTabs_Rest_Efir" to 2)

        /**
         * A top row apart, measured on 2026-11-14: in «Копия» the kit's dark-on-light mask keeps the top of the title's
         * «Э» under the ink threshold where Chrome's crosses it; «Эфир», light on dark, lies exactly. The rest of the
         * title is where the page's is.
         */
        val TOP_RESIDUAL = mapOf("MutantTopAppBar_Rest_Kopiya" to 1)
    }
}
