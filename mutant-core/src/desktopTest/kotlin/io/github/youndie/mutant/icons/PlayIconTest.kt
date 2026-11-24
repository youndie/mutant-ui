package io.github.youndie.mutant.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.DesignParity
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * B-195: the kit's own play triangle, the designer's answer 20 — `M8 5l11 7-11 7z` on the 24 grid, a 2 dp line, square
 * caps and no accent of damage. The Icons page draws no play, so there is no Chrome reference for it ([IconParityTest]
 * lists it as a known entry); the design draws it on a list row and a search result, where `ListItemParityTest` and
 * `SearchBarParityTest` hold it against the pages. Here it is held by its vector and by goldens of the kit's own,
 * recorded on 2026-11-24 and looked at: in the Icons page's cell (its ground and ink) at 48 and 16 dp, and at 24 dp on
 * density 2.
 */
class PlayIconTest {
    @Test
    fun `the play icon is the answer's path at 24 dp with a 2 dp line`() {
        val icon = MutantIcons.Play
        assertEquals(24.dp, icon.defaultWidth)
        assertEquals(24.dp, icon.defaultHeight)
        assertEquals(24f, icon.viewportWidth)
        assertEquals(24f, icon.viewportHeight)
        val paths = (0 until icon.root.size).map { icon.root[it] }
        assertEquals(1, paths.size, "one path")
        val path = paths.single() as VectorPath
        assertEquals(addPathNodes("M8 5l11 7-11 7z"), path.pathData)
        assertEquals(2f, path.strokeLineWidth)
        assertEquals(StrokeCap.Square, path.strokeLineCap)
        // The page's `<svg>` sets no join: SVG's miter, at its limit of 4.
        assertEquals(StrokeJoin.Miter, path.strokeLineJoin)
        assertEquals(4f, path.strokeLineMiter)
        assertNull(path.fill, "no fill")
        assertTrue(path.stroke != null, "a stroke")
    }

    @Test
    fun `the play icon matches its golden at 48 and 16 dp and on density 2`() {
        // The Icons page's cell: its ground and its ink, as every icon of the page is compared in.
        val cell = DesignParity.reference("Icon_Done48_Efir")
        val ground = DesignParity.rgb(cell["background"]!!.jsonPrimitive.content)
        val ink = DesignParity.rgb(cell["color"]!!.jsonPrimitive.content)
        val failures = mutableListOf<String>()
        for ((stem, size, density) in listOf(
            Triple("Icon_Play48", 48, 1f),
            Triple("Icon_Play16", 16, 1f),
            Triple("Icon_Play24_x2", 24, 2f),
        )) {
            val shot = render(size, density, ground) { MutantIcon(MutantIcons.Play, null, tint = ink, size = size.dp) }
            val golden = File(GOLDENS, "$stem.png")
            if (!golden.exists()) {
                File("build/kit-goldens").mkdirs()
                ImageIO.write(shot, "png", File("build/kit-goldens/$stem.png"))
                failures += "$stem: no golden; written to build/kit-goldens/$stem.png to be looked at"
                continue
            }
            val want = ImageIO.read(golden)
            if (want.width != shot.width || want.height != shot.height) {
                failures += "$stem: ${shot.width}×${shot.height} against the golden's ${want.width}×${want.height}"
                continue
            }
            // Parity's rule: a pixel differs when a channel is more than 16 apart, so an edge's anti-aliasing on another
            // Mac's rasteriser does not count, and a stroke moved by a pixel does.
            var differ = 0
            for (y in 0 until shot.height) {
                for (x in 0 until shot.width) {
                    val a = shot.getRGB(x, y)
                    val b = want.getRGB(x, y)
                    if ((0..2).any { abs((a shr (8 * it) and 255) - (b shr (8 * it) and 255)) > 16 }) differ++
                }
            }
            if (differ > 0) {
                File("build/kit-goldens").mkdirs()
                ImageIO.write(shot, "png", File("build/kit-goldens/${stem}_ACTUAL.png"))
                failures += "$stem: $differ px differ from the golden"
            }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    /** [content] in a box of [size] dp over [ground], at [density], as captured. */
    @OptIn(ExperimentalTestApi::class)
    private fun render(
        size: Int,
        density: Float,
        ground: Color,
        content: @Composable () -> Unit,
    ): BufferedImage {
        var out: BufferedImage? = null
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    Box(Modifier.size(size.dp).background(ground).testTag("play")) { content() }
                }
            }
            val image = onNodeWithTag("play").captureToImage()
            assertEquals((size * density).toInt(), image.toPixelMap().width)
            out = image.toAwtImage()
        }
        return out!!
    }

    private companion object {
        /** The kit's own goldens: what the kit draws where the design has no reference, recorded here and looked at. */
        val GOLDENS = File("src/desktopTest/snapshots/kit")
    }
}
