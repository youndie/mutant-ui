package io.github.youndie.mutant.illustration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.surface.LocalMutantOutlineOverride
import io.github.youndie.mutant.surface.LocalMutantSlotOrigin
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-14: the six illustrations against the Icons page at the still time. The bodies are held to the body's floor
 * against a reference whose text, kept parts and reels the script hid. What the whole picture adds to them —
 * labels, kept parts, reels — is held where text is held (B-52): by where its strong pixels lie, within a pixel.
 */
class IllustrationParityTest {
    private fun draw(
        kind: MutantIllustrationKind,
        body: Boolean,
        dx: Int = 0,
        dy: Int = 0,
    ): DesignParity.Result {
        val stem = "Illustration_${kind.name}${if (body) "Body" else ""}_Efir"
        val ref = DesignParity.reference(stem)
        val art = (ref["art"] as JsonArray).map { it.jsonPrimitive.int }
        val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
        // Each body at the outline width the engine drew it (D16: the tremble's seed is the engine's slot count).
        val slots = (ref["stageSlots"] as JsonArray).associateBy { it.jsonObject["key"]!!.jsonPrimitive.content }
        val outlines =
            slots.mapValues {
                it.value.jsonObject["drawnOutline"]!!
                    .jsonPrimitive.float
            }
        // … and generated where the engine stood it on its stage (D16: the engine's noise is in stage coordinates).
        val origins =
            slots.mapValues {
                Offset(
                    it.value.jsonObject["ox"]!!
                        .jsonPrimitive.float,
                    it.value.jsonObject["oy"]!!
                        .jsonPrimitive.float,
                )
            }
        return DesignParity.compare(stem) {
            MutantTheme {
                CompositionLocalProvider(
                    LocalMutantOutlineOverride provides { outlines[it] },
                    LocalMutantSlotOrigin provides { origins[it] },
                    LocalMutantTextRaster provides ChromeText,
                ) {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        Box(Modifier.fillMaxSize().background(ground)) {
                            IllustrationArt(
                                kind,
                                Modifier.offset((art[0] + dx).dp, (art[1] + dy).dp),
                                if (body) IllustrationParts.Bodies else IllustrationParts.All,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Where the page's layer of bodies landed in the crop. The engine draws a stage's bodies in an `<svg>` at the
     * stage's corner, and Chrome snaps it to a whole pixel by a rule the DOM does not show: of the page's three
     * columns, at fractions 0.33, 0.98 and 0.66 of a pixel, the last lands a pixel left of the art's rounded box.
     * The whole picture is placed where it matches best within a pixel; a part out of place still differs.
     */
    private fun placed(kind: MutantIllustrationKind): Pair<Int, Int> =
        (-1..1)
            .flatMap { dx ->
                (-1..1).map { dy -> dx to dy }
            }.minBy { (dx, dy) -> draw(kind, body = true, dx, dy).pixels }

    @Test
    fun `every illustration's bodies match the page's within the body's floor`() {
        val results =
            MutantIllustrationKind.entries.map { kind ->
                val (dx, dy) = placed(kind)
                kind to draw(kind, body = true, dx, dy)
            }
        val lines = results.map { (_, r) -> r.toString() }
        println(lines.joinToString("\n"))
        val over = results.filter { (kind, r) -> r.pixels > (RESIDUAL[kind] ?: FLOOR_PIXELS) }.map { it.second }
        assertTrue(over.isEmpty(), "over: $over\n" + lines.joinToString("\n"))
    }

    @Test
    fun `an illustration draws the same twice`() {
        for (kind in MutantIllustrationKind.entries) {
            val a = draw(kind, body = true).pixels
            val b = draw(kind, body = true).pixels
            assertEquals(a, b, "$kind")
        }
    }

    @Test
    fun `the labels, kept parts and reels stand where the page draws them, within a pixel`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (kind in MutantIllustrationKind.entries) {
            val (dx, dy) = placed(kind)
            val whole = draw(kind, body = false, dx, dy)
            val bodies = draw(kind, body = true, dx, dy)
            // What the whole picture adds to its bodies, in each renderer: its text, kept parts and reels.
            val kit = extentOf(whole.width, whole.height) { channelGap(whole.actual[it], bodies.actual[it]) > TEXT_INK }
            val page =
                extentOf(whole.width, whole.height) {
                    channelGap(whole.reference[it], bodies.reference[it]) >
                        TEXT_INK
                }
            lines += "$whole; parts: kit $kit, page $page"
            val near = if (kit == null || page == null) kit == page else kit.near(page)
            if (!near) off += kind.name
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "out of place: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /** The body's floor in pixels (research §1.8). */
        const val FLOOR_PIXELS = 5

        /**
         * Over the floor, measured on 2026-11-14 and never to be raised: one body's outline lying over another
         * body's fill (the label on the cassette's shell, the arms across the mast), anti-aliased a little apart by
         * the two renderers — up to 50 levels on single pixels, never a run. The floor's probes are single bodies.
         */
        val RESIDUAL =
            mapOf(
                MutantIllustrationKind.Cassette to 16,
                MutantIllustrationKind.Floppy to 9,
                MutantIllustrationKind.Antenna to 6,
            )
    }
}
