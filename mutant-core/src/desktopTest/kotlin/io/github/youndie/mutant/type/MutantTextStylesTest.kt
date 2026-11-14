package io.github.youndie.mutant.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.HANDJET_ROWS_ABOVE
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.components.ComponentParity
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** B-55: the design's type roles as public text styles, in the bundled faces and laid out as Chrome lays the line. */
@OptIn(ExperimentalTestApi::class)
class MutantTextStylesTest {
    @Test
    fun `each role is the design's face, size, tracking and colour, its line as Chrome's`() =
        runComposeUiTest {
            var styles: List<TextStyle> = emptyList()
            var fonts: MutantFonts? = null
            setContent {
                MutantTheme(colors = MutantPalettes.Kopiya) {
                    fonts = LocalMutantFonts.current
                    val t = MutantTheme.textStyles
                    styles = listOf(t.display(), t.osd(), t.text(), t.text(13.sp, FontWeight.Medium), t.osd(14.sp))
                }
            }
            waitForIdle()
            val (display, osd, text, label, small) = styles
            assertNotNull(fonts)
            assertEquals(fonts!!.display, display.fontFamily, "display: Handjet")
            assertEquals(26.sp, display.fontSize)
            assertEquals(MutantPalettes.Kopiya.text, display.color)
            assertEquals(fonts!!.display, osd.fontFamily, "OSD: Handjet")
            assertEquals(20.sp, osd.fontSize)
            assertEquals(1.sp, osd.letterSpacing, "OSD: tracked 1")
            assertEquals(MutantPalettes.Kopiya.textOsd, osd.color, "OSD: the theme's amber")
            assertEquals(fonts!!.text, text.fontFamily, "text: Rubik")
            assertEquals(15.sp, text.fontSize)
            assertEquals(FontWeight.Medium, label.fontWeight)
            // Blink's normal line: Handjet's ascent and descent rounded each, 18 + 5 at 20 px, 12 + 3 at 14 px; Rubik's
            // 14 + 4 at 15 px.
            assertEquals(23f, osd.lineHeight.value, "OSD 20: a 23 px line")
            assertEquals(15f, small.lineHeight.value, "OSD 14: a 15 px line")
            assertEquals(18f, text.lineHeight.value, "text 15: an 18 px line")
        }

    @Test
    fun `the Demo page's OSD line under the cassette puts its ink where the page does, in both themes`() {
        val lines = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "Demo_OsdLine_$theme"
            val ground = DesignParity.rgb(DesignParity.reference(stem)["background"]!!.jsonPrimitive.content)
            val r =
                DesignParity.compare(stem, ground) {
                    MutantTheme(colors = ComponentParity.palette(theme)) {
                        CompositionLocalProvider(LocalMutantTextRaster provides ChromeText) {
                            Box(Modifier.fillMaxSize().background(ground)) {
                                BasicText("46 МИН · SP · КОПИЯ 4-ГО ПОКОЛЕНИЯ", style = MutantTheme.textStyles.osd())
                            }
                        }
                    }
                }
            val g = r.reference[0]

            fun ink(px: IntArray) = extentOf(r.width, r.height) { channelGap(px[it], g) > TEXT_INK }
            val kit = ink(r.actual)
            val page = ink(r.reference)
            lines += "$theme: kit $kit, page $page, $r"
            assertNotNull(kit)
            assertNotNull(page)
            // The OSD line is Handjet: a row higher on macOS 26 alone (B-74).
            assertTrue(kit.near(page, 1, 0, above = HANDJET_ROWS_ABOVE), lines.joinToString("\n"))
        }
    }
}
