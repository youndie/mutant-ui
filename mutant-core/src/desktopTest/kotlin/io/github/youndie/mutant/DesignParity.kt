package io.github.youndie.mutant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * Text as Chrome draws it in the references: antialiased outlines (the script turns macOS smoothing off), glyphs
 * at fractional x, no hinting. Provided to the kit's text through `LocalMutantTextRaster` in parity fixtures only.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
internal val ChromeText =
    androidx.compose.ui.text.PlatformTextStyle(
        null,
        androidx.compose.ui.text.PlatformParagraphStyle(
            androidx.compose.ui.text.FontRasterizationSettings(
                androidx.compose.ui.text.FontSmoothing.AntiAlias,
                androidx.compose.ui.text.FontHinting.None,
                subpixelPositioning = true,
                autoHintingForced = false,
            ),
        ),
    )

/** First and last row and column holding ink, or null when there is none. */
internal data class Extent(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
) {
    /**
     * Every side within [px] of [other]'s, the top within [topPx], and further at most [above] rows higher than
     * [other]'s, never lower ([HANDJET_ROWS_ABOVE], B-74).
     */
    fun near(
        other: Extent,
        px: Int = 1,
        topPx: Int = px,
        above: Int = 0,
    ): Boolean =
        top - other.top in -(topPx + above)..topPx && abs(bottom - other.bottom) <= px &&
            abs(left - other.left) <= px && abs(right - other.right) <= px
}

/**
 * B-74: how many rows higher than Chrome's this host's rasteriser may put the top of Handjet's ink. 0 on macOS 27
 * and later, where the references were rendered and every text check was held exactly; 1 on macOS 26 (the hosted
 * `macos-latest` runner, 26.7.2), whose CoreText puts Handjet's glyphs a row higher with the kit's line box unchanged.
 * Applied by the three checks that failed there (TextFloorTest's Handjet labels, the Demo page's OSD line, the
 * dialog's words) and by no other; the exact form runs on this Mac before every merge.
 */
internal val HANDJET_ROWS_ABOVE: Int = handjetRowsAbove(System.getProperty("os.name"), System.getProperty("os.version"))

internal fun handjetRowsAbove(
    os: String?,
    version: String?,
): Int {
    if (os == null || !os.startsWith("Mac")) return 0
    val major = version?.substringBefore('.')?.toIntOrNull() ?: return 0
    return if (major == 26) 1 else 0
}

/**
 * Where text puts its ink: the rows and columns where [inked] holds for a pixel. Two renderers anti-alias a
 * glyph's edge apart (Compose's light-on-dark mask reads 186 where Chrome's reads 102), so text is held by where
 * its strong pixels lie, not by how many differ (B-52).
 */
internal fun extentOf(
    w: Int,
    h: Int,
    inked: (Int) -> Boolean,
): Extent? {
    var top = Int.MAX_VALUE
    var bottom = -1
    var left = Int.MAX_VALUE
    var right = -1
    for (i in 0 until w * h) {
        if (!inked(i)) continue
        val x = i % w
        val y = i / w
        top = minOf(top, y)
        bottom = maxOf(bottom, y)
        left = minOf(left, x)
        right = maxOf(right, x)
    }
    return if (bottom < 0) null else Extent(top, bottom, left, right)
}

/** The largest channel difference of two `0xRRGGBB` pixels. */
internal fun channelGap(
    a: Int,
    b: Int,
): Int =
    maxOf(
        abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF)),
        abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF)),
        abs((a and 0xFF) - (b and 0xFF)),
    )

/** A strong pixel of text: more than 64 levels off its ground in some channel, which both renderers agree on. */
internal const val TEXT_INK = 64

/**
 * A composable against one of the Chrome references (`snapshots/design/`): rendered at density 1 at the
 * reference's size, a pixel differing when a channel is more than 16 apart, the diff written to
 * `build/parity/<stem>_DIFF.png` beside the kit's `_ACTUAL.png` to be looked at.
 */
internal object DesignParity {
    private val dir = File("src/desktopTest/snapshots/design")
    private val manifest by lazy { Json.parseToJsonElement(File(dir, "manifest.json").readText()).jsonObject }

    fun reference(stem: String): JsonObject =
        manifest["references"]!!.jsonArray.map { it.jsonObject }.single { it["stem"]!!.jsonPrimitive.content == stem }

    /** A CSS `rgb(…)` or `rgba(…)` as a colour. */
    fun rgb(s: String): Color {
        val v = Regex("""[\d.]+""").findAll(s).map { it.value.toFloat() }.toList()
        return Color(v[0] / 255, v[1] / 255, v[2] / 255, v.getOrElse(3) { 1f })
    }

    class Result(
        val stem: String,
        val pixels: Int,
        val percent: Double,
        /** The largest difference of any channel anywhere, 0…255: what a pixel count under 16 cannot see. */
        val maxChannel: Int,
        /** Pixels either image inks — off the ground by more than 16 — when a ground was given; else 0. */
        val ink: Int = 0,
        val width: Int = 0,
        val height: Int = 0,
        /** The kit's pixels and the reference's, `0xRRGGBB`, row by row. */
        val actual: IntArray = IntArray(0),
        val reference: IntArray = IntArray(0),
    ) {
        /** Differing pixels against inked ones, %: a text's share, which a cell's share hides. */
        val inkPercent: Double get() = if (ink == 0) 0.0 else 100.0 * pixels / ink

        override fun toString(): String = "$stem: ${"%.3f".format(percent)} %, $pixels px, max $maxChannel"
    }

    @OptIn(ExperimentalTestApi::class)
    fun compare(
        stem: String,
        ground: Color? = null,
        content: @Composable () -> Unit,
    ): Result {
        val ref = reference(stem)
        val w = ref["w"]!!.jsonPrimitive.int
        val h = ref["h"]!!.jsonPrimitive.int
        val bitmap = Bitmap.makeFromImage(Image.makeFromEncoded(File(dir, "$stem.png").readBytes()))
        var differ = 0
        var most = 0
        var inked = 0
        val kit = IntArray(w * h)
        val chrome = IntArray(w * h)
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Box(Modifier.size(w.dp, h.dp).testTag("parity")) { content() }
                }
            }
            val shot = onNodeWithTag("parity").captureToImage()
            val actual = shot.toPixelMap()
            val diff = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val a = actual[x, y]
                    val c = bitmap.getColor(x, y)
                    kit[y * w + x] =
                        ((a.red * 255).toInt() shl 16) or ((a.green * 255).toInt() shl 8) or (a.blue * 255).toInt()
                    chrome[y * w + x] = c and 0xFFFFFF
                    val m =
                        maxOf(
                            abs((a.red * 255).toInt() - (c shr 16 and 0xFF)),
                            abs((a.green * 255).toInt() - (c shr 8 and 0xFF)),
                            abs((a.blue * 255).toInt() - (c and 0xFF)),
                        )
                    if (m > 16) differ++
                    most = maxOf(most, m)
                    if (ground != null) {
                        val g =
                            intArrayOf(
                                (ground.red * 255).toInt(),
                                (ground.green * 255).toInt(),
                                (ground.blue * 255).toInt(),
                            )
                        val offA =
                            maxOf(
                                abs((a.red * 255).toInt() - g[0]),
                                abs((a.green * 255).toInt() - g[1]),
                                abs((a.blue * 255).toInt() - g[2]),
                            )
                        val offC =
                            maxOf(
                                abs((c shr 16 and 0xFF) - g[0]),
                                abs((c shr 8 and 0xFF) - g[1]),
                                abs((c and 0xFF) - g[2]),
                            )
                        if (offA > 16 || offC > 16) inked++
                    }
                    diff.setRGB(x, y, if (m > 16) 0xFF2020 else (c and 0xFFFFFF).let { (it shr 2) and 0x3F3F3F })
                }
            }
            File("build/parity").mkdirs()
            ImageIO.write(shot.toAwtImage(), "png", File("build/parity/${stem}_ACTUAL.png"))
            ImageIO.write(diff, "png", File("build/parity/${stem}_DIFF.png"))
        }
        return Result(stem, differ, 100.0 * differ / (w * h), most, inked, w, h, kit, chrome)
    }
}
