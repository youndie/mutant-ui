package io.github.youndie.mutant.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-213: `?scene=app`'s scene, the Demo's phone alone for a page to embed, in a 390 × 844 window — the size
 * kotlin.website embeds it at. Each screen in each palette fills the window under the chips, and the screen runs.
 * Each shot is written to `build/demo-app/` to be looked at.
 */
@OptIn(ExperimentalTestApi::class)
class DemoAppSceneTest {
    private val s = SampleStrings(SampleLanguage.English)

    private fun scene(block: ComposeUiTest.() -> Unit) =
        runDesktopComposeUiTest(390, 844) {
            mainClock.autoAdvance = false
            setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { DemoAppScene() } }
            mainClock.advanceTimeBy(600)
            block()
        }

    /** A chip of the scene's row, apart from the words the screen under it may also show. */
    private fun ComposeUiTest.chip(label: String) =
        onNode(hasText(label) and hasAnyAncestor(hasTestTag("demo-app-chips")), useUnmergedTree = true)

    private fun ComposeUiTest.shot(screen: DemoScreen): BufferedImage =
        onNodeWithTag(demoScreenTag(screen), useUnmergedTree = true).captureToImage().toAwtImage()

    @Test
    fun `each screen in each palette fills the 390 by 844 window under the chips`() =
        scene {
            File("build/demo-app").mkdirs()
            val chips = onNodeWithTag("demo-app-chips").getBoundsInRoot()
            assertEquals(0.dp, chips.top, "the chips at the window's top")
            assertEquals(104.dp, chips.bottom, "two rows of chips, 48 dp each")
            for (label in listOf(s.demoLibrary, s.demoRecording, s.settings, s.themeEfir, s.themeKopiya)) {
                assertTrue(
                    chip(label).getBoundsInRoot().right <= 382.dp,
                    "«$label» inside the window, 8 dp from its edge",
                )
            }
            val mean = HashMap<String, Int>()
            var drawn = 0
            for ((palette, name) in listOf(s.themeEfir to "Efir", s.themeKopiya to "Kopiya")) {
                chip(palette).performClick()
                mainClock.advanceTimeBy(600)
                for (screen in DemoScreen.entries) {
                    val label =
                        when (screen) {
                            DemoScreen.Library -> s.demoLibrary
                            DemoScreen.Recording -> s.demoRecording
                            DemoScreen.Settings -> s.settings
                        }
                    chip(label).performClick()
                    // Long enough for the screen to tune in and the settings' dialog to open.
                    mainClock.advanceTimeBy(800)
                    val node = onNodeWithTag(demoScreenTag(screen), useUnmergedTree = true)
                    val at = node.getBoundsInRoot()
                    assertEquals(0.dp, at.left, "${screen.name}, $name: from the window's left")
                    assertEquals(390.dp, at.right, "${screen.name}, $name: to the window's right")
                    assertEquals(chips.bottom, at.top, "${screen.name}, $name: right under the chips")
                    assertEquals(844.dp, at.bottom, "${screen.name}, $name: to the window's foot")
                    val image = shot(screen)
                    val colours = HashSet<Int>()
                    var sum = 0L
                    var n = 0
                    for (y in 0 until image.height step 7) {
                        for (x in 0 until image.width step 7) {
                            val rgb = image.getRGB(x, y)
                            colours += rgb
                            sum += ((rgb shr 16) and 255) + ((rgb shr 8) and 255) + (rgb and 255)
                            n++
                        }
                    }
                    assertTrue(colours.size > 40, "${screen.name}, $name: drawn, ${colours.size} colours")
                    mean["${screen.name}/$name"] = (sum / (3 * n)).toInt()
                    ImageIO.write(image, "png", File("build/demo-app/${screen.name}_$name.png"))
                    drawn++
                }
            }
            assertEquals(6, drawn, "three screens, two palettes")
            // «Эфир» is dark, «Копия» light: each screen shows the palette picked.
            for (screen in DemoScreen.entries) {
                val efir = mean.getValue("${screen.name}/Efir")
                val kopiya = mean.getValue("${screen.name}/Kopiya")
                assertTrue(kopiya - efir > 50, "${screen.name}: mean level $efir in «Эфир», $kopiya in «Копия»")
            }
        }

    @Test
    fun `the screen runs, reduce motion off`() =
        scene {
            // The Tape and the bodies move on the screen's clock; under reduce motion it stands and the frames are one.
            val before = shot(DemoScreen.Library)
            mainClock.advanceTimeBy(250)
            val after = shot(DemoScreen.Library)
            var moved = 0
            for (y in 0 until before.height) {
                for (x in 0 until before.width) {
                    if (before.getRGB(x, y) !=
                        after.getRGB(x, y)
                    ) {
                        moved++
                    }
                }
            }
            assertTrue(moved > before.width * before.height / 100, "$moved pixels changed in 250 ms")
        }
}
