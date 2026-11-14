package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-37: the Demo page's three screens, rebuilt from the public API as it stands, draw at the page's 360 × 720 in both
 * themes and both languages. Their references are for the eye (the owner's answer of 2026-11-14): each one's sheet,
 * the page's beside the kit's, is written to `build/demo/` to be looked at, not counted.
 */
@OptIn(ExperimentalTestApi::class)
class DemoScreensTest {
    private val tags =
        mapOf(
            DemoScreen.Library to "demo-library",
            DemoScreen.Recording to "demo-recording",
            DemoScreen.Settings to "demo-settings",
        )

    @Test
    fun `the three screens draw at 360 by 720 in both themes and both languages, and are written beside the page's`() {
        File("build/demo").mkdirs()
        var drawn = 0
        for (print in listOf(false, true)) {
            for (language in SampleLanguage.entries) {
                // One screen at a time: with its title it stands taller than the test's window.
                for (screen in DemoScreen.entries) {
                    runComposeUiTest {
                        mainClock.autoAdvance = false
                        setContent {
                            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                                MutantTheme(colors = if (print) MutantPalettes.Kopiya else MutantPalettes.Efir) {
                                    DemoScreenFrame(screen, SampleStrings(language))
                                }
                            }
                        }
                        // Long enough for the settings' dialog to open (320 ms) and the snackbar to ride in.
                        mainClock.advanceTimeBy(600)
                        val theme = if (print) "Kopiya" else "Efir"
                        val shot =
                            onNodeWithTag(
                                tags.getValue(screen),
                                useUnmergedTree = true,
                            ).captureToImage().toAwtImage()
                        assertEquals(360, shot.width, "${screen.name} width")
                        assertEquals(720, shot.height, "${screen.name} height")
                        val colours = HashSet<Int>()
                        for (y in 0 until 720 step 7) for (x in 0 until 360 step 7) colours += shot.getRGB(x, y)
                        assertTrue(
                            colours.size > 40,
                            "${screen.name}, $theme, $language: drawn, ${colours.size} colours",
                        )
                        if (language == SampleLanguage.Russian) sheet(screen.name, theme, shot)
                        drawn++
                    }
                }
            }
        }
        assertEquals(12, drawn, "three screens, two themes, two languages")
    }

    /** The page's screen beside the kit's, for the eye. */
    private fun sheet(
        name: String,
        theme: String,
        kit: BufferedImage,
    ) {
        val page = ImageIO.read(File("../mutant-core/src/desktopTest/snapshots/design/Demo_${name}_$theme.png"))
        val out = BufferedImage(page.width + 24 + kit.width, maxOf(page.height, kit.height), BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(page, 0, 0, null)
        g.drawImage(kit, page.width + 24, 0, null)
        g.dispose()
        ImageIO.write(out, "png", File("build/demo/${name}_$theme.png"))
    }
}
