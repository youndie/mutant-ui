package io.github.youndie.mutant.surface

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.blockMeetsCleanZone
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-199 (D20, the designer's answer 25): a mosaic block that meets the clean zone is not drawn, at rest or pressed, at
 * any heat. The engine shifts a pressed body's tiles along their side into the zone, over the label
 * (F-foundations-04); the kit leaves such a tile out.
 */
@OptIn(ExperimentalTestApi::class)
class PressedMosaicCleanZoneTest {
    private val gen = MutantGenerator(cassette = "5")

    /** The keys of the sample's Foundations page (`ComponentsFoundations.kt`), in both of its languages. */
    private val pageKeys =
        listOf("On air", "Copy", "Эфир", "Копия").flatMap { listOf("theme-card-$it", "theme-b-$it", "theme-c-$it") } +
            listOf("tape-card", "loss-card", "loss-button")

    private fun slot(
        body: MutantBody,
        w: Double,
        h: Double,
        content: DoubleArray,
    ): Slot {
        val p = body.profile
        // The zone as the surface measures it: the content's box plus 6 dp across and 2 down, then 2 to spare.
        val tz = doubleArrayOf(content[0] - 6, content[1] - 2, content[2] + 12, content[3] + 4)
        return Slot(
            ox = 20.0,
            oy = 30.0,
            w = w,
            h = h,
            r = minOf(p.radius, w / 2, h / 2),
            over = p.overhang,
            dripShare = p.dripShare,
            wobble = p.wobble,
            round = p.round,
            calm = 1.0,
            dead = p.deadPixels,
            marks = p.marks,
            edge = p.mosaic,
            textZone = tz,
            cleanZone = doubleArrayOf(tz[0] - 2, tz[1] - 2, tz[2] + 4, tz[3] + 4),
        )
    }

    /** The page's bodies with their content where the components put it: a card's at 16, 14; a label centred. */
    private val bodies =
        listOf(120.0, 160.0, 200.0).flatMap { cw ->
            listOf(258.0, 290.0).map { w -> Triple(MutantBody.Card, w to 80.0, doubleArrayOf(16.0, 14.0, cw, 40.0)) }
        } +
            listOf(
                Triple(MutantBody.Btn, 160.0 to 48.0, doubleArrayOf(32.0, 15.0, 96.0, 18.0)),
                Triple(MutantBody.Chip, 90.0 to 32.0, doubleArrayOf(16.0, 8.0, 58.0, 16.0)),
            )

    private fun Instance.inZone(
        x: Double,
        y: Double,
        size: Double,
    ) = slot.blockMeetsCleanZone(x - slot.ox, y - slot.oy, size)

    @Test
    fun `no mosaic tile meets the clean zone at rest or at any heat, over every key of the Foundations page`() {
        val lines = ArrayList<String>()
        var asDesigned = 0
        for ((body, size, content) in bodies) {
            for (key in pageKeys) {
                val g = gen.instance(slot(body, size.first, size.second, content), key, false)
                val rest = g.mosaicTiles.count { g.inZone(it.x, it.y, it.size) }
                var hot = 0
                for (step in 1..20) {
                    val heat = step / 20.0
                    for (k in 0 until 120) {
                        val t = 5000 + k * 83.33
                        hot += runningMosaic(g, t, heat).count { (p, _) -> g.inZone(p.x, p.y, p.size) }
                        asDesigned +=
                            runningMosaic(g, t, heat, asDesigned = true).count { (p, _) -> g.inZone(p.x, p.y, p.size) }
                    }
                }
                if (rest + hot >
                    0
                ) {
                    lines += "$body ${size.first} × ${size.second} $key: at rest $rest, hot $hot tiles in the zone"
                }
            }
        }
        println("B-199: the engine's way puts $asDesigned tiles in the zone over the sweep")
        // The control: the sweep can see a tile in the zone, as the engine draws one.
        assertTrue(asDesigned > 0, "the engine's way put no tile in the zone: the sweep sees nothing")
        assertTrue(lines.isEmpty(), lines.joinToString("\n"))
    }

    /** The drawn pixels of a pressed card's mosaic inside its clean zone, per hot tick, as a press boils it from rest. */
    private fun mosaicInZone(
        key: String,
        density: Float,
        colors: MutantColors,
    ): List<Int> {
        val w = 258
        val h = 80
        val counts = ArrayList<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            val source = MutableInteractionSource()
            var g: Instance? = null
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density)) {
                    MutantTheme(colors = colors, reduceMotion = false) {
                        Box(Modifier.size((w + 60).dp, (h * 2 + 120).dp).testTag("root")) {
                            // The same body twice, pressed by one source: drawn with its mosaic and without it.
                            for (i in 0..1) {
                                CompositionLocalProvider(
                                    LocalMutantSkipBodyParts provides
                                        if (i ==
                                            1
                                        ) {
                                            setOf(BodyPart.Mosaic)
                                        } else {
                                            emptySet()
                                        },
                                ) {
                                    MutantSurface(
                                        MutantBody.Card,
                                        key,
                                        Modifier.offset(30.dp, (30 + i * (h + 60)).dp).size(w.dp, h.dp),
                                        interactionSource = source,
                                        probe = { if (i == 0) g = it },
                                    ) {
                                        Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                            Column(
                                                Modifier.wrapContentSize(Alignment.TopStart).mutantCleanZone(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                BasicText("Без подписи №3", style = TextStyle(fontSize = 16.sp))
                                                BasicText("Сторона A · 46 мин", style = TextStyle(fontSize = 13.sp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            repeat(10) { mainClock.advanceTimeByFrame() }
            runBlocking { source.emit(PressInteraction.Press(Offset(10f, 10f))) }
            // Three seconds from the press, one capture every 80 ms: the heat's rise, then heat 1.
            repeat(36) {
                repeat(5) { mainClock.advanceTimeByFrame() }
                val img = onNodeWithTag("root").captureToImage().toAwtImage()
                val tz = g!!.slot.textZone
                // The zone, 1 px in from each side, so an edge's anti-aliasing never counts.
                val x0 = ((30 + tz[0]) * density).toInt() + 1
                val y0 = ((30 + tz[1]) * density).toInt() + 1
                val x1 = ((30 + tz[0] + tz[2]) * density).toInt() - 1
                val y1 = ((30 + tz[1] + tz[3]) * density).toInt() - 1
                val dy = ((h + 60) * density).toInt()
                var n = 0
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        val a = img.getRGB(x, y)
                        val b = img.getRGB(x, y + dy)
                        val m =
                            maxOf(
                                abs((a shr 16 and 255) - (b shr 16 and 255)),
                                abs((a shr 8 and 255) - (b shr 8 and 255)),
                                abs((a and 255) - (b and 255)),
                            )
                        if (m > 40) n++
                    }
                }
                counts += n
            }
        }
        return counts
    }

    @Test
    fun `a pressed card draws no mosaic pixel in its clean zone, in both palettes and at density 2`() {
        // Keys whose running mosaic the engine's way shifts into a card's zone (F-foundations-04's sweep): before
        // B-199, 28 to 208 px on three to seven of 36 ticks.
        val runs =
            listOf(
                Triple("card-3", 1f, MutantPalettes.Efir),
                Triple("card-3", 2f, MutantPalettes.Kopiya),
                Triple("card-6", 1f, MutantPalettes.Kopiya),
                Triple("card-6", 2f, MutantPalettes.Efir),
            )
        val lines =
            runs.map { (key, d, colors) ->
                "$key d$d ${if (colors.print) "Копия" else "Эфир"}: ${mosaicInZone(key, d, colors)}"
            }
        println(lines.joinToString("\n"))
        // A tile in the zone is 4 or 8 dp, 16 px and more; none at all is drawn there now.
        for (line in lines) {
            val worst =
                line
                    .substringAfter("[")
                    .removeSuffix("]")
                    .split(", ")
                    .maxOf { it.toInt() }
            assertTrue(worst <= 2, line)
        }
    }
}
