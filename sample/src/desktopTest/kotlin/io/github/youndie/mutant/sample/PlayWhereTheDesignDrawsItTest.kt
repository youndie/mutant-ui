package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-195: the sample draws the kit's play triangle where the design draws one — the Recording's «Listen» and its
 * tracks (the demo page), the List cells (the Components page) and the Search bar's result (the later page). Each place
 * is read off the screen: of the kit's icons, the one whose ink matches what is drawn beside the word is Play.
 */
@OptIn(ExperimentalTestApi::class)
class PlayWhereTheDesignDrawsItTest {
    private val s = SampleStrings(SampleLanguage.English)
    private val colors = MutantPalettes.Efir

    /** A place to read: the word the icon stands before, and the ink, ground and size the icon is drawn in there. */
    private class Place(
        val word: String,
        val ink: Color,
        val ground: Color,
        val size: Int,
    )

    private val track = { word: String -> Place(word, colors.text, colors.surface, LIST_ICON) }

    @Test
    fun `the recording's listen and its tracks draw the play icon`() =
        read(
            listOf(
                Place(s.listen, colors.onPrimary, colors.primary, BUTTON_ICON),
                track(s.testCard),
                track(s.nightTuesday),
                track(s.staticBetween),
            ),
        ) {
            DemoScreenFrame(DemoScreen.Recording, s, keyframeLoss = false)
        }

    @Test
    fun `the list cells and the search result draw the play icon`() {
        read(listOf(track(s.testCard), track(s.nightTuesday))) { Column(Modifier.width(400.dp)) { ListItems(s) } }
        read(listOf(track(s.nightTuesday))) { Column(Modifier.width(400.dp)) { Searches(s) } }
    }

    /** [content] at rest (reduce motion, no Tape), and at each place the kit's icon that best matches what is drawn. */
    private fun read(
        places: List<Place>,
        content: @Composable () -> Unit,
    ) {
        val masks =
            places.map { it.ink to it.ground to it.size }.distinct().associateWith { (c, size) ->
                iconMasks(c.first, c.second, size)
            }
        val failures = mutableListOf<String>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true, cleanSignal = true) { Box(Modifier.testTag("shot")) { content() } }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val shot = onNodeWithTag("shot").captureToImage().toAwtImage()
            val origin = onNodeWithTag("shot").fetchSemanticsNode().boundsInRoot.topLeft
            for (p in places) {
                val icons = masks.getValue(p.ink to p.ground to p.size)
                val words = onAllNodes(hasText(p.word), useUnmergedTree = true).fetchSemanticsNodes()
                if (words.isEmpty()) failures += "«${p.word}» is not on the screen"
                for (w in words) {
                    val b = w.boundsInRoot.translate(-origin)
                    // The icon stands before the word: within 80 dp to its left, its middle within 32 dp of the word's.
                    val area = Rect(b.left - 80, b.center.y - 32, b.left, b.center.y + 32)
                    val scores = icons.mapValues { (_, m) -> best(shot, area, p.ink, m, p.size) }
                    val (name, score) = scores.minBy { it.value }
                    // Measured 2026-11-24: Play disagrees on 0.00 of its ink before «Listen» and 0.29 in a list's tile (its rims,
                    // drawn over the tile's fill at 22 dp); the next icon on 0.84 and more.
                    if (name != "Play" || score > MAX_SHARE) {
                        failures +=
                            "«${p.word}» at ${b.left},${b.top}: $name reads ${"%.2f".format(
                                score,
                            )}, Play ${"%.2f".format(scores["Play"])}"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /** Every icon of the kit at [size] dp in [ink] over [ground], as a mask of its ink. */
    private fun iconMasks(
        ink: Color,
        ground: Color,
        size: Int,
    ): Map<String, BooleanArray> {
        val out = mutableMapOf<String, BooleanArray>()
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Row {
                        for ((name, icon) in MutantIcons.all) {
                            Box(Modifier.size(size.dp).background(ground).testTag("icon-$name")) {
                                MutantIcon(icon, null, tint = ink, size = size.dp)
                            }
                        }
                    }
                }
            }
            for ((name) in MutantIcons.all) {
                val img = onNodeWithTag("icon-$name").captureToImage().toAwtImage()
                out[name] = BooleanArray(size * size) { inked(img.getRGB(it % size, it / size), ink) }
            }
        }
        return out
    }

    /** The smallest share of [mask]'s ink that disagrees with [shot] at any place of [mask] inside [area]. */
    private fun best(
        shot: BufferedImage,
        area: Rect,
        ink: Color,
        mask: BooleanArray,
        size: Int,
    ): Double {
        val inkCount = mask.count { it }
        val x0 = area.left.toInt().coerceAtLeast(0)
        val y0 = area.top.toInt().coerceAtLeast(0)
        val x1 = area.right.toInt().coerceAtMost(shot.width) - size
        val y1 = area.bottom.toInt().coerceAtMost(shot.height) - size
        val drawn = Array(shot.height) { y -> BooleanArray(shot.width) { x -> inked(shot.getRGB(x, y), ink) } }
        var least = Int.MAX_VALUE
        for (oy in y0..y1) {
            for (ox in x0..x1) {
                var n = 0
                for (i in mask.indices) if (mask[i] != drawn[oy + i / size][ox + i % size]) n++
                least = minOf(least, n)
            }
        }
        return least.toDouble() / inkCount
    }

    /** A pixel within [INK_REACH] of [ink] in every channel: the icon's strokes, not their anti-aliased rims. */
    private fun inked(
        rgb: Int,
        ink: Color,
    ): Boolean =
        abs((rgb shr 16 and 255) - (ink.red * 255).toInt()) <= INK_REACH &&
            abs((rgb shr 8 and 255) - (ink.green * 255).toInt()) <= INK_REACH &&
            abs((rgb and 255) - (ink.blue * 255).toInt()) <= INK_REACH

    private companion object {
        /** The list row's icon in its 40 dp tile, and the button's before its label. */
        const val LIST_ICON = 22
        const val BUTTON_ICON = 24
        const val INK_REACH = 80

        /** The share of the icon's ink that may disagree: halfway between Play's 0.29 and the next icon's 0.84. */
        const val MAX_SHARE = 0.55
    }
}
