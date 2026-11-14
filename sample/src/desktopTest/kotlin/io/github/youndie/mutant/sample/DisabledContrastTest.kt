package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test

/**
 * B-38: the disabled words under the Tape at every scene tick of its cycle, both themes. The owner chose WCAG's
 * exemption for inactive components on 2026-11-14 (1.4.3 asks no contrast of them): measured 4.38 («Эфир»'s disabled
 * button) to 4.52, they are held from falling under 4.3, a regression the exemption does not cover.
 */
@OptIn(ExperimentalTestApi::class)
class DisabledContrastTest {
    private fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

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

    @Test
    fun `the disabled words stand where they were measured, under the Tape`() {
        val lines = ArrayList<String>()
        var lowest = 99.0
        for (print in listOf(false, true)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(colors = if (print) MutantPalettes.Kopiya else MutantPalettes.Efir) {
                            val colors = MutantTheme.colors
                            Column(
                                Modifier
                                    .width(360.dp)
                                    .background(colors.background)
                                    .mutantTape()
                                    .testTag("d"),
                            ) {
                                MutantButton("Отключено", {}, enabled = false)
                                MutantCheckbox(true, {}, label = "Шум головки", enabled = false)
                                MutantSwitch(true, {}, label = "Автотрекинг", enabled = false)
                                MutantTextField(
                                    "",
                                    {},
                                    Modifier.width(328.dp),
                                    placeholder = "Найти запись",
                                    enabled = false,
                                )
                                MutantCard(
                                    Modifier.width(328.dp),
                                    enabled = false,
                                ) { MutantCardTitle("Без подписи №3") }
                            }
                        }
                    }
                }
                mainClock.advanceTimeByFrame()
                val o = onNodeWithTag("d").fetchSemanticsNode().boundsInRoot
                val boxes =
                    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .map { it.config[SemanticsProperties.Text].joinToString { s -> s.text } to it.boundsInRoot }
                val worst = HashMap<String, Double>()
                repeat(56) {
                    val img = onNodeWithTag("d").captureToImage().toAwtImage()
                    for ((t, b) in boxes) {
                        val counts = HashMap<Int, Int>()
                        val px = ArrayList<Int>()
                        for (y in max(
                            0,
                            (b.top - o.top).toInt(),
                        ) until min(img.height, (b.bottom - o.top).toInt() + 1)) {
                            for (x in max(0, (b.left - o.left).toInt()) until
                                min(img.width, (b.right - o.left).toInt() + 1)) {
                                val c = img.getRGB(x, y) and 0xFFFFFF
                                px += c
                                counts[c] = (counts[c] ?: 0) + 1
                            }
                        }
                        if (px.isEmpty()) continue
                        val ground = counts.maxBy { it.value }.key
                        worst[t] = min(worst[t] ?: 99.0, px.maxOf { ratio(it, ground) })
                    }
                    mainClock.advanceTimeBy(125)
                }
                val theme = if (print) "Копия" else "Эфир"
                lowest = minOf(lowest, worst.values.min())
                for ((t, r) in worst.entries.sortedBy { it.value }) lines += "$theme   %.2f  %s".format(r, t)
            }
        }
        println("A11Y-DISABLED\n" + lines.joinToString("\n"))
        kotlin.test.assertTrue(lowest >= 4.3, "a disabled word under 4.3: $lowest\n" + lines.joinToString("\n"))
    }
}
