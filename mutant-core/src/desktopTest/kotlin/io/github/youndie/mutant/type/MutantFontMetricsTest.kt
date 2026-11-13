package io.github.youndie.mutant.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-02: the metrics later items rest on, pinned to the files. A changed font file changes one of
 * these numbers and fails here first.
 */
@OptIn(ExperimentalTestApi::class)
class MutantFontMetricsTest {
    /** At 1000 px a metric reads in thousandths of an em, whatever the file's units per em. */
    private fun font(face: Typeface) = Font(face, 1000f)

    private fun advances(
        face: Typeface,
        text: String,
    ): List<Float> = font(face).getWidths(face.getStringGlyphs(text)).toList()

    @Test
    fun `Handjet has a 647 cap height, a 471 x-height and advances on its 480-unit module`() {
        for (face in listOf(BundledFonts.handjetRegular, BundledFonts.handjetSemiBold)) {
            val f = font(face)
            // 5280 and 3840 of 8160 units per em.
            assertEquals(647.06f, f.metrics.capHeight, 0.5f, "cap height")
            assertEquals(470.59f, f.metrics.xHeight, 0.5f, "x-height")
            // Every advance is a whole number of 480-unit modules (8160 = 17 modules).
            for (w in advances(face, "AMWi1ЛИСТЖщ.!Н")) {
                val modules = w / (1000f / 17f)
                assertEquals(Math.round(modules).toFloat(), modules, 0.01f, "advance $w is not on the module")
            }
        }
        // A weight change never moves a Handjet layout: the weight is in the ink, not the width.
        assertEquals(
            advances(BundledFonts.handjetRegular, "ЛИСТ ЭФИР 0123"),
            advances(BundledFonts.handjetSemiBold, "ЛИСТ ЭФИР 0123"),
        )
    }

    @Test
    fun `Rubik has a 700 cap height and a 520 x-height, and widens with weight`() {
        for (face in listOf(BundledFonts.rubikRegular, BundledFonts.rubikMedium)) {
            val f = font(face)
            assertEquals(700f, f.metrics.capHeight, 0.5f, "cap height")
            assertEquals(520f, f.metrics.xHeight, 0.5f, "x-height")
        }
        assertEquals(714f, advances(BundledFonts.rubikRegular, "Н").single(), 0.5f)
        assertEquals(729f, advances(BundledFonts.rubikMedium, "Н").single(), 0.5f)
    }

    @Test
    fun `the instances are static and weigh 400, 600, 400 and 500`() {
        assertEquals(listOf(400, 600, 400, 500), BundledFonts.all.map { it.fontStyle.weight })
        // A static instance draws its weight with no axis setting, which is the point on Android 24.
        assertTrue(
            ink(BundledFonts.handjetSemiBold) > ink(BundledFonts.handjetRegular) * 1.15f,
            "Handjet 600 inks more",
        )
        assertTrue(ink(BundledFonts.rubikMedium) > ink(BundledFonts.rubikRegular) * 1.05f, "Rubik 500 inks more")
    }

    /** Dark pixels of "Н" drawn at 100 px in black on white. */
    private fun ink(face: Typeface): Int {
        val surface = Surface.makeRasterN32Premul(120, 120)
        surface.canvas.clear(0xFFFFFFFF.toInt())
        surface.canvas.drawString("Н", 10f, 100f, Font(face, 100f), Paint().apply { color = 0xFF000000.toInt() })
        val pixels = Bitmap().apply { allocN32Pixels(120, 120) }
        surface.readPixels(pixels, 0, 0)
        return (0 until 120).sumOf { y -> (0 until 120).count { x -> (pixels.getColor(x, y) and 0xFF) < 128 } }
    }

    @Test
    fun `the families load through compose resources and lay out with the files' advances`() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    val fonts = mutantFonts()
                    Column {
                        BasicText(
                            "ЭФИР",
                            Modifier.testTag("display"),
                            style =
                                TextStyle(
                                    fontFamily = fonts.display,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                ),
                        )
                        BasicText(
                            "ЭФИР",
                            Modifier.testTag("text"),
                            style =
                                TextStyle(
                                    fontFamily = fonts.text,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 20.sp,
                                ),
                        )
                    }
                }
            }
            for ((tag, face) in listOf("display" to BundledFonts.handjetSemiBold, "text" to BundledFonts.rubikMedium)) {
                val expected = advances(face, "ЭФИР").sum() / 1000f * 20f
                val bounds = onNodeWithTag(tag).getBoundsInRoot()
                val width = (bounds.right - bounds.left).value
                assertTrue(abs(width - expected) <= 1f, "$tag laid out 'ЭФИР' at $width dp, the file says $expected")
            }
        }
}
