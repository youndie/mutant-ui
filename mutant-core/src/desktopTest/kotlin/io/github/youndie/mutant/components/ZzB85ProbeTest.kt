package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.FontHinting
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.FontSmoothing
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.surface.BodyState
import io.github.youndie.mutant.surface.MutantSurface
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * B-85 scratch, run on the macOS 26 runner: the slider's value drawn with each raster over each ground, and how many
 * of its pixels are neither the ink nor the ground (0 = aliased). Not to be merged.
 */
@OptIn(ExperimentalTestApi::class, androidx.compose.ui.text.ExperimentalTextApi::class)
class ZzB85ProbeTest {
    private fun raster(
        s: FontSmoothing,
        h: FontHinting,
        sub: Boolean,
    ) = PlatformTextStyle(null, PlatformParagraphStyle(FontRasterizationSettings(s, h, sub, false)))

    private val rasters: List<Pair<String, PlatformTextStyle?>> =
        listOf(
            "default" to null,
            "aa-none-sub" to raster(FontSmoothing.AntiAlias, FontHinting.None, true),
            "aa-slight-nosub" to raster(FontSmoothing.AntiAlias, FontHinting.Slight, false),
            "lcd-slight-nosub" to raster(FontSmoothing.SubpixelAntiAlias, FontHinting.Slight, false),
            "lcd-none-nosub" to raster(FontSmoothing.SubpixelAntiAlias, FontHinting.None, false),
            "aa-normal-nosub" to raster(FontSmoothing.AntiAlias, FontHinting.Normal, false),
        )

    private val grounds = listOf("plain-grey", "plain-amber", "disabled-body", "rest-body")

    @Composable
    private fun Ground(
        g: String,
        content: @Composable () -> Unit,
    ) {
        val c = MutantTheme.colors
        when (g) {
            "plain-grey" -> {
                Box(Modifier.size(40.dp).background(c.textDisabled), contentAlignment = Alignment.Center) { content() }
            }

            "plain-amber" -> {
                Box(Modifier.size(40.dp).background(c.amber), contentAlignment = Alignment.Center) { content() }
            }

            else -> {
                MutantSurface(
                    MutantBody.Fab,
                    "slider-thumb",
                    Modifier.size(40.dp),
                    fill = c.amber,
                    state = if (g == "disabled-body") BodyState.Disabled else BodyState.Rest,
                ) { Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { content() } }
            }
        }
    }

    /** The slider's value as the parity fixture draws it, against its hidden twin. */
    private fun slider(
        enabled: Boolean,
        value: String,
    ): String {
        val shots = ArrayList<java.awt.image.BufferedImage>()
        var box = androidx.compose.ui.geometry.Rect.Zero
        for (hidden in listOf(false, true)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(colors = ComponentParity.palette("Efir")) {
                            ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                                Box(Modifier.size(320.dp, 120.dp).background(Color(0xFF101010)).testTag("s")) {
                                    SliderBody(
                                        .3f,
                                        value,
                                        {},
                                        {},
                                        Modifier.size(220.dp, 100.dp),
                                        "ЧЁТКОСТЬ",
                                        "−10 … +10",
                                        enabled,
                                        "slider-thumb",
                                        null,
                                        hidden = hidden,
                                    )
                                }
                            }
                        }
                    }
                }
                mainClock.advanceTimeByFrame()
                shots += onNodeWithTag("s").captureToImage().toAwtImage()
                box =
                    onAllNodes(
                        androidx.compose.ui.test
                            .hasText(value),
                        useUnmergedTree = true,
                    ).fetchSemanticsNodes()
                        .first()
                        .boundsInRoot
            }
        }
        val (img, bare) = shots
        val ink =
            ComponentParity.palette("Efir").onPrimary.let { c ->
                ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
            }
        var differ = 0
        var full = 0
        for (y in box.top.toInt() until box.bottom.toInt()) {
            for (x in box.left.toInt() until box.right.toInt()) {
                val a = img.getRGB(x, y) and 0xFFFFFF
                if (a != (bare.getRGB(x, y) and 0xFFFFFF)) {
                    differ++
                    val d =
                        maxOf(
                            kotlin.math.abs((a shr 16 and 255) - (ink shr 16 and 255)),
                            kotlin.math.abs((a shr 8 and 255) - (ink shr 8 and 255)),
                            kotlin.math.abs((a and 255) - (ink and 255)),
                        )
                    if (d <= 2) full++
                }
            }
        }
        val dir = File("build/parity/b85").apply { mkdirs() }
        ImageIO.write(img, "png", File(dir, "slider-$enabled-$value.png"))
        return "B85 slider enabled=$enabled '$value' differ=$differ fullInk=$full partial=${differ - full}"
    }

    @Test
    fun probe() {
        val out = StringBuilder()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(colors = ComponentParity.palette("Efir")) {
                        ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                            val styles = MutantTheme.textStyles
                            Column(Modifier.background(Color(0xFF101010))) {
                                for ((rn, r) in rasters) {
                                    for (bare in listOf(false, true)) {
                                        Row {
                                            for (g in grounds) {
                                                Box(Modifier.padding(12.dp)) {
                                                    Ground(g) {
                                                        BasicText(
                                                            "−4",
                                                            Modifier.testTag("$rn/$g/$bare"),
                                                            style =
                                                                styles.display(18.sp).copy(
                                                                    color = if (bare) Color.Transparent else MutantTheme.colors.onPrimary,
                                                                    platformStyle = r,
                                                                ),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            for ((rn, _) in rasters) {
                for (g in grounds) {
                    val img = onNodeWithTag("$rn/$g/false").captureToImage().toAwtImage()
                    val bare = onNodeWithTag("$rn/$g/true").captureToImage().toAwtImage()
                    val ink =
                        ComponentParity.palette("Efir").onPrimary.let { c ->
                            ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
                        }
                    var differ = 0
                    var full = 0
                    for (y in 0 until img.height) {
                        for (x in 0 until img.width) {
                            val a = img.getRGB(x, y) and 0xFFFFFF
                            if (a != (bare.getRGB(x, y) and 0xFFFFFF)) {
                                differ++
                                val dr = kotlin.math.abs((a shr 16 and 255) - (ink shr 16 and 255))
                                val dg = kotlin.math.abs((a shr 8 and 255) - (ink shr 8 and 255))
                                val db = kotlin.math.abs((a and 255) - (ink and 255))
                                if (maxOf(dr, dg, db) <= 2) full++
                            }
                        }
                    }
                    out.append("B85 $rn $g differ=$differ fullInk=$full partial=${differ - full}\n")
                    val dir = File("build/parity/b85").apply { mkdirs() }
                    ImageIO.write(img, "png", File(dir, "$rn-$g.png"))
                }
            }
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File("build/parity/b85/all.png"))
        }
        for (enabled in listOf(true, false)) for (value in listOf("−4", "+3")) out.append(slider(enabled, value) + "\n")
        println(out)
        println("B85 os ${System.getProperty("os.version")}")
    }
}
