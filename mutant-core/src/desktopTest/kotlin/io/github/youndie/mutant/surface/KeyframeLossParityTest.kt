package io.github.youndie.mutant.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.components.ComponentParity
import io.github.youndie.mutant.gen.moshPlan
import io.github.youndie.mutant.theme.LocalMutantFonts
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.type.LocalMutantTextRaster
import io.github.youndie.mutant.type.MutantFaceMetrics
import io.github.youndie.mutant.type.cssLine
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-48: the specs page's keyframe loss — before it, at 45 % and 95 % of the loss, recovering — each its stage of a
 * card and a button, the loss frozen where the page freezes it (`data-mu-mosh-p`).
 */
class KeyframeLossParityTest {
    private val frames = listOf("Before" to null, "At45" to .45, "At95" to .95, "Recovering" to 1.2)

    /** Which loss of the page the stage's was: the one whose plan the engine kept. */
    private fun lossOf(ref: kotlinx.serialization.json.JsonObject): Int {
        val dur = ref["mosh"]!!.jsonObject["dur"]!!.jsonPrimitive.double
        return (1..12).first { moshPlan("5", it, 250, 212).durationMs == dur }
    }

    private fun frame(
        name: String,
        p: Double?,
        hidden: Boolean = false,
    ): DesignParity.Result {
        val stem = "MutantKeyframeLoss_${name}_Efir"
        val ref = DesignParity.reference(stem)
        val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
        val stage = ref["stage"]!!.jsonPrimitive.content
        val first = if (p == null) 1 else lossOf(ref)
        val slots = ComponentParity.stageSlots(stem)
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette("Efir")) {
                val colors = MutantTheme.colors
                val fonts = LocalMutantFonts.current
                CompositionLocalProvider(LocalMutantTextRaster provides ChromeText, LocalMutantStage provides stage) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        ComponentParity.ProvideSlots(slots) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(colors.page)) {
                                KeyframeLossBody(Modifier.fillMaxSize(), colors.page, p, {
                                    Column(
                                        Modifier.fillMaxSize().padding(bottom = 30.dp),
                                        verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        MutantSurface(MutantBody.Card, "spec-card", Modifier.size(214.dp, 64.dp)) {
                                            Column(
                                                Modifier
                                                    .padding(
                                                        horizontal = 14.dp,
                                                        vertical = 12.dp,
                                                    ).mutantCleanZone(),
                                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                            ) {
                                                BasicText(
                                                    "Без подписи №3",
                                                    style =
                                                        TextStyle(
                                                            fontFamily = fonts?.text,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (hidden) Color.Transparent else colors.text,
                                                            platformStyle = ChromeText,
                                                        ).cssLine(MutantFaceMetrics.Rubik),
                                                )
                                                BasicText(
                                                    "Сторона A · 46 мин",
                                                    style =
                                                        TextStyle(
                                                            fontFamily = fonts?.text,
                                                            fontSize = 12.sp,
                                                            color = if (hidden) Color.Transparent else colors.textSecondary,
                                                            platformStyle = ChromeText,
                                                        ).cssLine(MutantFaceMetrics.Rubik),
                                                )
                                            }
                                        }
                                        io.github.youndie.mutant.components.ButtonBody(
                                            "Воспроизвести",
                                            {},
                                            Modifier.size(168.dp, 48.dp),
                                            true,
                                            io.github.youndie.mutant.components.MutantButtonState.Idle,
                                            null,
                                            "spec-btn",
                                            null,
                                            if (hidden) Color.Transparent else colors.onPrimary,
                                            false,
                                        )
                                    }
                                }, first)
                            }
                        }
                    }
                }
            }
        }
    }

    /** The words' boxes (the card's lines, the button's label), and 2 px around: the text floor's, not the loss's. */
    private val words =
        listOf(
            IntArray(4).also {
                it[0] = 30
                it[1] = 27
                it[2] = 220
                it[3] = 71
            },
            intArrayOf(60, 125, 190, 158),
        )

    /** Pixels more than 16 apart in any channel outside the words' boxes. */
    private fun bodyPixels(r: DesignParity.Result): Int =
        (0 until r.width * r.height).count { i ->
            val x = i % r.width
            val y = i / r.width
            words.none { x in it[0] until it[2] && y in it[1] until it[3] } &&
                io.github.youndie.mutant
                    .channelGap(r.actual[i], r.reference[i]) > 16
        }

    /**
     * Pixels more than 2 apart in any channel inside the loss's cleared blocks (`gone`): the engine's canvas is clear
     * there, so the bodies show through it, under what the blocks' drag has drawn in since. A body's edge there is a
     * few levels off the ground, under the 16 the other count reads.
     */
    private fun holePixels(
        r: DesignParity.Result,
        loss: Int,
    ): Int {
        val holes = moshPlan("5", loss, 250, 212).runs.filter { it.gone }
        return (0 until r.width * r.height).count { i ->
            val x = i % r.width
            val y = i / r.width
            holes.any { x in it.x until it.x + it.w && y in it.y until it.y + it.h } &&
                io.github.youndie.mutant
                    .channelGap(r.actual[i], r.reference[i]) > 2
        }
    }

    @Test
    fun `the four frames of the loss as the specs page freezes them, the words untouched`() {
        val results = frames.map { (name, p) -> name to frame(name, p) }
        val inHoles =
            frames.drop(1).associate { (name, _) ->
                name to
                    holePixels(
                        results.first { it.first == name }.second,
                        lossOf(DesignParity.reference("MutantKeyframeLoss_${name}_Efir")),
                    )
            }
        val report =
            results.map { (name, r) ->
                "$name: $r; outside the words ${bodyPixels(r)} px; in the cleared blocks ${inHoles[name] ?: 0} px"
            }
        println(report.joinToString("\n"))
        val off = results.filter { (name, r) -> bodyPixels(r) > RESIDUAL.getValue(name) }.map { it.first }
        assertTrue(off.isEmpty(), "off: $off\n" + report.joinToString("\n"))
        // The words are not the loss's: in every frame of it they stand as the kit drew them before it.
        val before = results.first().second
        // The glyphs: where the frame before the loss differs from itself with its words drawn transparent. Their edges
        // take a few levels from the ground under them, the snapshot's raster of the bodies: measured on 2026-11-15, at
        // most 34 (a pixel of the label recovering), three or four pixels past 16 a frame; a smeared word moves by far
        // more.
        val bare = frame("Before", null, hidden = true)
        val glyphs = (0 until before.width * before.height).filter { before.actual[it] != bare.actual[it] }
        for ((name, r) in results.drop(1)) {
            val moved =
                glyphs.count {
                    io.github.youndie.mutant.channelGap(
                        r.actual[it],
                        before.actual[it],
                    ) > GLYPH_GAP
                }
            assertTrue(moved == 0, "$name: $moved pixels of the words moved")
        }
    }

    /**
     * Over the floor of the frame before the loss, measured on 2026-11-15 and never to be raised: the bodies' own
     * residual (75 px before the loss, glyph-free) copied along by the blocks, each a few steps, as the page copies its
     * own.
     */
    private val RESIDUAL = mapOf("Before" to 75, "At45" to 300, "At95" to 205, "Recovering" to 181)

    /** A glyph pixel moved further than this has been smeared. */
    private val GLYPH_GAP = 48
}
