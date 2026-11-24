package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.theme.LocalMutantColors
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantPalettes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-117: resting bodies under a running clock, drawn from their image of each tick (B-82), with the speckle, mosaic and
 * dead pixels drawn into it each tick and drawn once into an image of their own, at density 1 and the Pixel 6a's 2.625,
 * in both themes. A body's first ticks, before its image ahead arrives, are drawn in the frame and compared too. Under a
 * frozen clock (fixtures, references) nothing changes: the two draw the same pixels. The line, set before the first render: on every tick compared, at most 0.2 % of the stage's pixels
 * differ by more than 16 levels in any channel. A sheet of the last tick of both goes to `build/b-117/`.
 */
@OptIn(ExperimentalTestApi::class)
class StillLayersOnceTest {
    private class Case(
        val name: String,
        val body: MutantBody,
        val w: Int,
        val h: Int,
    )

    private val cases =
        listOf(
            Case("card", MutantBody.Card, 328, 80),
            Case("button", MutantBody.Btn, 160, 48),
            Case("fab", MutantBody.Fab, 56, 56),
        )

    private class Count {
        var asImage = 0
    }

    private fun ComposeUiTest.ticks(): List<BufferedImage> {
        val shots = ArrayList<BufferedImage>()
        repeat(64) { i ->
            mainClock.advanceTimeByFrame()
            Thread.sleep(6)
            if (i % 8 == 7) shots += onNodeWithTag("stage").captureToImage().toAwtImage()
        }
        return shots
    }

    private fun render(
        case: Case,
        once: Boolean,
        density: Float,
        print: Boolean,
        count: Count,
        frozen: Boolean,
    ): List<BufferedImage> {
        var out = emptyList<BufferedImage>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density),
                    LocalMutantStillLayersOnce provides once,
                    LocalMutantFrameProbe provides { _, image, _ -> if (image) count.asImage++ },
                    LocalMutantColors provides if (print) MutantPalettes.Kopiya else MutantPalettes.Efir,
                ) {
                    ProvideMutantClock(
                        remember { if (frozen) MutantClock(startMs = 5000.0, frozen = true) else MutantClock() },
                    ) {
                        Box(Modifier.size((case.w + 48).dp, (case.h + 80).dp).testTag("stage")) {
                            MutantSurface(
                                case.body,
                                "b117/${case.name}",
                                Modifier.offset(24.dp, 16.dp).size(case.w.dp, case.h.dp),
                            ) {}
                        }
                    }
                }
            }
            out = ticks()
        }
        return out
    }

    private fun sameImage(
        a: BufferedImage,
        b: BufferedImage,
    ): Boolean = (0 until a.height).all { y -> (0 until a.width).all { x -> a.getRGB(x, y) == b.getRGB(x, y) } }

    private fun farShare(
        a: BufferedImage,
        b: BufferedImage,
    ): Double {
        var far = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val p = a.getRGB(x, y)
                val q = b.getRGB(x, y)
                var most = 0
                for (sh in intArrayOf(24, 16, 8, 0)) most = maxOf(most, abs((p shr sh and 255) - (q shr sh and 255)))
                if (most > 16) far++
            }
        }
        return 100.0 * far / (a.width * a.height)
    }

    /**
     * The cases of one clock and one theme, at both densities. All four ran as one test in 36 s on a Mac (B-210), half of
     * it the 6 ms a frame given to the images ahead; each is a test of its own now, the same cases between them.
     */
    private fun drawnOnce(
        frozen: Boolean,
        print: Boolean,
    ) {
        val dir = File("build/b-117").apply { mkdirs() }
        val lines = ArrayList<String>()
        var worst = 0.0
        for (d in listOf(1f, 2.625f)) {
            for (c in cases) {
                val counted = Count()
                val each = render(c, once = false, d, print, Count(), frozen)
                val once = render(c, once = true, d, print, counted, frozen)
                // A frozen clock draws in the frame (fixtures, references); a running one from its tick's image.
                if (!frozen) {
                    assertTrue(
                        counted.asImage >= 4,
                        "${c.name} at $d: ticks drawn from an image ${counted.asImage}",
                    )
                }
                // The first tick of an off-frame body draws no body yet (B-50); every tick after it is compared.
                if (frozen) {
                    val same = each.indices.all { i -> sameImage(each[i], once[i]) }
                    assertTrue(same, "${c.name} at $d under a frozen clock: the fixtures' pixels moved")
                }
                val shares = (1 until each.size).map { farShare(each[it], once[it]) }
                worst = maxOf(worst, shares.max())
                val tag = "${c.name}-${if (print) "kopiya" else "efir"}-$d${if (frozen) "-frozen" else ""}"
                lines += "$tag by more than 16, per tick: ${shares.joinToString(" ") { "%.3f".format(it) }} %"
                val a = each.last()
                val sheet = BufferedImage(a.width * 2 + 8, a.height, BufferedImage.TYPE_INT_ARGB)
                sheet.graphics.apply {
                    drawImage(a, 0, 0, null)
                    drawImage(once.last(), a.width + 8, 0, null)
                }
                ImageIO.write(sheet, "png", File(dir, "$tag.png"))
            }
        }
        val part = "${if (print) "kopiya" else "efir"}${if (frozen) "-frozen" else ""}"
        File(dir, "shares-$part.txt").writeText(lines.joinToString("\n") + "\n")
        println(lines.joinToString("\n"))
        assertTrue(worst <= 0.2, "a case over the line of 0.2 %: ${lines.joinToString("; ")}")
    }

    @Test
    fun `the still layers drawn once draw what each tick draws, in Efir`() = drawnOnce(frozen = false, print = false)

    @Test
    fun `the still layers drawn once draw what each tick draws, in Kopiya`() = drawnOnce(frozen = false, print = true)

    @Test
    fun `under a frozen clock the still layers drawn once draw the same pixels, in Efir`() =
        drawnOnce(frozen = true, print = false)

    @Test
    fun `under a frozen clock the still layers drawn once draw the same pixels, in Kopiya`() =
        drawnOnce(frozen = true, print = true)
}
