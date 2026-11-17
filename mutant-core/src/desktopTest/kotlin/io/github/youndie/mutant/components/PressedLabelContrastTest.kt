package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.contrast
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-91 (and B-104, the carousel's cards): a press boils a body's fill to amber; the words and the icon on it stay readable on every frame of the press and
 * the release, by a real press, in both themes. The ground is read beside the label, inside the body; the ink is the
 * label's pixel that stands furthest from it.
 */
@OptIn(ExperimentalTestApi::class)
class PressedLabelContrastTest {
    private class Case(
        val name: String,
        /** The node whose bounds hold the label: by its text, or by its content description for an icon. */
        val label: ComposeUiTest.() -> Rect,
        /** Where the ground is read, dp left of the label's box, at its vertical middle. */
        val groundLeft: Float,
        /** The stage's height, dp: tall enough that nothing of the body is cut. */
        val height: Int = 160,
        val content: @Composable () -> Unit,
    )

    private val cases =
        listOf(
            Case("chip", { text("Шум") }, 6f) { MutantChip("Шум", false, {}) },
            Case("outlined button", { text("В очередь") }, 6f) { MutantOutlinedButton("В очередь", {}) },
            Case("card", { text("Кассета 07") }, 8f) {
                MutantCard(Modifier.size(328.dp, 80.dp), onClick = {}) { MutantCardTitle("Кассета 07") }
            },
            Case("carousel title", { text("Без подписи №3") }, 8f, 260) { Shelf() },
            Case("carousel subtitle", { text("Сторона A · 46 мин") }, 8f, 260) { Shelf() },
            Case("carousel status", { text("CH 03 · SP") }, 8f, 260) { Shelf() },
            Case("icon button", { icon("Поделиться") }, 4f) {
                MutantIconButton(MutantIcons.Share, "Поделиться", {})
            },
        )

    private fun ComposeUiTest.text(s: String) = onNodeWithText(s, useUnmergedTree = true).getBoundsInRoot().toPx()

    private fun ComposeUiTest.icon(s: String) =
        onNodeWithContentDescription(s, useUnmergedTree = true).getBoundsInRoot().toPx()

    private fun androidx.compose.ui.unit.DpRect.toPx() =
        Rect(
            left.value * D,
            top.value * D,
            right.value * D,
            bottom.value * D,
        )

    /** The worst contrast between the label's ink and the ground beside it over a press and a release, frame by frame. */
    private fun worst(
        case: Case,
        colors: MutantColors,
    ): Pair<Float, String> {
        var worst = Float.MAX_VALUE
        var at = ""
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D)) {
                    MutantTheme(colors = colors, reduceMotion = false) {
                        Box(Modifier.size(400.dp, case.height.dp).testTag("stage")) {
                            Box(Modifier.padding(24.dp)) { case.content() }
                        }
                    }
                }
            }
            repeat(40) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(3)
            }
            val box = case.label(this)

            fun frame(phase: String) {
                val px = onNodeWithTag("stage").captureToImage().toPixelMap()
                val c = readContrast(px, box, case.groundLeft * D)
                if (c < worst) {
                    worst = c
                    at = "$phase at ${mainClock.currentTime} ms"
                }
            }
            frame("rest")
            val centre =
                androidx.compose.ui.geometry
                    .Offset(box.center.x, box.center.y)
            onNodeWithTag("stage").performTouchInput { down(centre) }
            repeat(30) {
                mainClock.advanceTimeByFrame()
                frame("press")
            }
            onNodeWithTag("stage").performTouchInput { up() }
            repeat(40) {
                mainClock.advanceTimeByFrame()
                frame("release")
            }
        }
        return worst to at
    }

    private fun readContrast(
        px: PixelMap,
        box: Rect,
        groundLeft: Float,
    ): Float {
        val gy = box.center.y.roundToInt()
        val ground = px[(box.left - groundLeft).roundToInt().coerceAtLeast(0), gy]
        var best = 1f
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                val c = contrast(px[x, y].copy(alpha = 1f), ground.copy(alpha = 1f))
                if (c > best) best = c
            }
        }
        return best
    }

    @Test
    fun `a pressed body's label reads at 3 to 1 on every frame of its press and release`() {
        val failures = ArrayList<String>()
        for ((theme, colors) in listOf("Эфир" to MutantPalettes.Efir, "Копия" to MutantPalettes.Kopiya)) {
            for (case in cases) {
                val (c, at) = worst(case, colors)
                if (c < 3f) failures += "${case.name}, $theme: ${"%.2f".format(c)}:1 $at"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val D = 2f
    }
}

/** One card on a shelf, with its three lines. */
@Composable
private fun Shelf() =
    MutantCarousel(
        listOf(MutantCarouselItem("Без подписи №3", "Сторона A · 46 мин", "CH 03 · SP")),
        Modifier.width(352.dp),
    )
