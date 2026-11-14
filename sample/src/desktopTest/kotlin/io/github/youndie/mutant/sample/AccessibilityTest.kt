package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test

/**
 * B-38: over the demo screens, every active word's contrast after the Tape at its worst scene tick (the brief: 4.5:1),
 * the largest luminance change between ticks (the kit's rule: none over a quarter of the screen), and the saturated
 * red's area tick by tick (the red never flashes); the table is printed for the research.
 */
@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    private fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    /** WCAG relative luminance of a `0xRRGGBB`. */
    private fun luminance(rgb: Int): Double =
        0.2126 * channel(rgb shr 16 and 0xFF) + 0.7152 * channel(rgb shr 8 and 0xFF) + 0.0722 * channel(rgb and 0xFF)

    private fun ratio(
        a: Int,
        b: Int,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + .05) / (min(la, lb) + .05)
    }

    /** A text's contrast in [img] within [box]: its strongest pixel against the box's commonest, its ground. */
    private fun contrast(
        img: BufferedImage,
        box: IntArray,
    ): Double {
        val counts = HashMap<Int, Int>()
        val px = ArrayList<Int>()
        for (y in max(0, box[1]) until min(img.height, box[3])) {
            for (x in max(0, box[0]) until min(img.width, box[2])) {
                val c = img.getRGB(x, y) and 0xFFFFFF
                px += c
                counts[c] = (counts[c] ?: 0) + 1
            }
        }
        if (px.isEmpty()) return 99.0
        val ground = counts.maxBy { it.value }.key
        return px.maxOf { ratio(it, ground) }
    }

    private fun ComposeUiTest.texts(): List<Pair<Rect, String>> =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot to it.config[SemanticsProperties.Text].joinToString { s -> s.text } }

    private fun saturatedRed(c: Int) = (c shr 16 and 0xFF) > 200 && (c shr 8 and 0xFF) < 90 && (c and 0xFF) < 90

    @Test
    fun `active words hold 4_5 to 1 after the Tape at the worst tick, no tick flashes a quarter, the red stays`() {
        val lines = ArrayList<String>()
        val failures = ArrayList<String>()
        val strings = SampleStrings(SampleLanguage.Russian)
        val dialogWords = setOf(strings.eraseQuestion, strings.eraseNamed, strings.cancel, strings.erase)
        // The settings screen twice: its dialog's words over the scrim, and its form with the dialog closed — words under
        // a modal's scrim are darkened on purpose, and no reader reads them.
        val cases =
            listOf(
                DemoScreen.Library to true,
                DemoScreen.Recording to true,
                DemoScreen.Settings to true,
                DemoScreen.Settings to false,
            )
        for (print in listOf(false, true)) {
            val theme = if (print) "Копия" else "Эфир"
            for ((screen, open) in cases) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            MutantTheme(colors = if (print) MutantPalettes.Kopiya else MutantPalettes.Efir) {
                                DemoScreenFrame(screen, strings, dialogOpen = open)
                            }
                        }
                    }
                    mainClock.advanceTimeBy(600)
                    val tag = "demo-" + screen.name.lowercase()
                    val o = onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                    val boxes =
                        texts()
                            .filter { (b, _) ->
                                b.left >= o.left && b.top >= o.top && b.bottom <= o.bottom &&
                                    b.width > 0
                            }.filter { (_, t) -> screen != DemoScreen.Settings || !open || t in dialogWords }
                            .map { (b, t) ->
                                val box =
                                    intArrayOf(
                                        (b.left - o.left).toInt(),
                                        (b.top - o.top).toInt(),
                                        (b.right - o.left).toInt() + 1,
                                        (b.bottom - o.top).toInt() + 1,
                                    )
                                t to box
                            }
                    val worst = HashMap<String, Double>()
                    var previous: BufferedImage? = null
                    var largest = 0
                    var red = 0
                    val reds = ArrayList<String>()
                    val redArea = ArrayList<Int>()
                    // A Tape cycle is 7 s: 56 scene ticks.
                    repeat(56) {
                        val img = onNodeWithTag(tag, useUnmergedTree = true).captureToImage().toAwtImage()
                        for ((t, box) in boxes) worst[t] = min(worst[t] ?: 99.0, contrast(img, box))
                        var area = 0
                        for (y in 0 until img.height) {
                            for (x in 0 until img.width) {
                                if (saturatedRed(img.getRGB(x, y) and 0xFFFFFF)) {
                                    area++
                                }
                            }
                        }
                        redArea += area
                        previous?.let { p ->
                            var changed = 0
                            var r = 0
                            for (y in 0 until img.height) {
                                for (x in 0 until img.width) {
                                    val a = p.getRGB(x, y) and 0xFFFFFF
                                    val b = img.getRGB(x, y) and 0xFFFFFF
                                    val la = luminance(a)
                                    val lb = luminance(b)
                                    if (abs(la - lb) >= .1 && min(la, lb) < .8) changed++
                                    if (saturatedRed(a) != saturatedRed(b)) {
                                        r++
                                        if (reds.size < 3) reds += "#${a.toString(16)}→#${b.toString(16)} at $x,$y"
                                    }
                                }
                            }
                            largest = max(largest, changed)
                            red = max(red, r)
                        }
                        previous = img
                        mainClock.advanceTimeBy(125)
                    }
                    val case = if (screen == DemoScreen.Settings) (if (open) " (dialog)" else " (form)") else ""
                    val share = "%.1f".format(largest * 100.0 / (360 * 720))
                    lines +=
                        "$theme ${screen.name}$case: largest change $share %; red changing $red, red area ${redArea.min()}–${redArea.max()} px"
                    for ((t, r) in worst) {
                        if (r <
                            4.5
                        ) {
                            failures += "$theme ${screen.name}$case: «$t» %.2f under 4.5".format(r)
                        }
                    }
                    if (largest >
                        360 * 720 / 4
                    ) {
                        failures += "$theme ${screen.name}$case: a tick changed $share % of the screen"
                    }
                    // Steady red: the error's offset outline and the radio's dot move with their bodies, never on and off.
                    if (screen == DemoScreen.Settings && !open && redArea.min() < redArea.max() * 3 / 4) {
                        failures += "$theme settings: the red's area ran ${redArea.min()}–${redArea.max()}"
                    }
                    for ((t, r) in worst.entries.sortedBy { it.value }.take(5)) {
                        lines +=
                            "   %.2f  %s".format(r, t.take(40))
                    }
                }
            }
        }
        println("A11Y\n" + lines.joinToString("\n"))
        kotlin.test.assertTrue(failures.isEmpty(), failures.joinToString("\n") + "\n" + lines.joinToString("\n"))
    }
}
