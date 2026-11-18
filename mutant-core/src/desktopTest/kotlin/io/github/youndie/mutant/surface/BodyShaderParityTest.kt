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
 * B-110, Q1 of the spike, held since B-128 to the line set before its first render: the same bodies at a frozen time
 * drawn by paths and by the shader, with the same parts (the decorations left out of both), at density 1 and 2.625, in
 * both themes; at most 0.5 % of the stage's pixels differ by more than 32 in any channel, in every case. B-110's shader,
 * reading its edge off the field, was at 1.86–3.55 %. A sheet of both renders goes to `build/b-110/`.
 */
@OptIn(ExperimentalTestApi::class)
class BodyShaderParityTest {
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
    private val skip = setOf(BodyPart.Speckle, BodyPart.Mosaic, BodyPart.DeadPixels, BodyPart.FlowLines)

    private fun render(
        case: Case,
        onGpu: Boolean,
        density: Float,
        print: Boolean,
    ): BufferedImage {
        var out: BufferedImage? = null
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density),
                    LocalMutantBodyOnGpu provides onGpu,
                    LocalMutantSkipBodyParts provides skip,
                    LocalMutantColors provides if (print) MutantPalettes.Kopiya else MutantPalettes.Efir,
                ) {
                    ProvideMutantClock(remember { MutantClock(startMs = 5000.0, frozen = true) }) {
                        Box(Modifier.size((case.w + 48).dp, (case.h + 80).dp).testTag("stage")) {
                            MutantSurface(
                                case.body,
                                "b110/${case.name}",
                                Modifier.offset(24.dp, 16.dp).size(case.w.dp, case.h.dp),
                            ) {}
                        }
                    }
                }
            }
            out = onNodeWithTag("stage").captureToImage().toAwtImage()
        }
        return out!!
    }

    private fun share(
        a: BufferedImage,
        b: BufferedImage,
    ): Double {
        var n = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val p = a.getRGB(x, y)
                val q = b.getRGB(x, y)
                var far = false
                for (sh in intArrayOf(24, 16, 8, 0)) if (abs((p shr sh and 255) - (q shr sh and 255)) > 32) far = true
                if (far) n++
            }
        }
        return 100.0 * n / (a.width * a.height)
    }

    @Test
    fun `the shader draws what the paths draw, within half a percent`() {
        val dir = File("build/b-110").apply { mkdirs() }
        val lines = ArrayList<String>()
        var worst = 0.0
        for (print in listOf(false, true)) {
            for (d in listOf(1f, 2.625f)) {
                for (c in cases) {
                    val paths = render(c, onGpu = false, d, print)
                    val shader = render(c, onGpu = true, d, print)
                    val s = share(paths, shader)
                    worst = maxOf(worst, s)
                    val tag = "${c.name}-${if (print) "kopiya" else "efir"}-$d"
                    lines += "$tag ${"%.2f".format(s)} %"
                    val sheet = BufferedImage(paths.width * 2 + 8, paths.height, BufferedImage.TYPE_INT_ARGB)
                    sheet.graphics.apply {
                        drawImage(paths, 0, 0, null)
                        drawImage(shader, paths.width + 8, 0, null)
                    }
                    ImageIO.write(sheet, "png", File(dir, "$tag.png"))
                }
            }
        }
        File(dir, "shares.txt").writeText(lines.joinToString("\n") + "\n")
        println(lines.joinToString("\n"))
        assertTrue(worst <= 0.5, "a case over the line of 0.5 %: ${lines.joinToString("; ")}")
    }
}
