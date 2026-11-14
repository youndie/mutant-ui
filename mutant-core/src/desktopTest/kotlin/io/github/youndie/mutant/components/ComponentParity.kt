package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.Extent
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.surface.LocalMutantOutlineOverride
import io.github.youndie.mutant.surface.LocalMutantSlotOrigin
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A component's page cell against the kit (research §1.15): drawn on the page's stage, its body generated where the
 * engine stood it and at its outline width (D16), text rasterised as Chrome does, the clock still at 5000 ms. The
 * body is held outside the engine's clean zone; the content inside it — label, icon — by what it adds over the
 * cell's bare twin (text and icons hidden), in each renderer: where its ink lies and its colour where it covers a
 * pixel whole.
 */
internal object ComponentParity {
    fun palette(theme: String): MutantColors = if (theme == "Kopiya") MutantPalettes.Kopiya else MutantPalettes.Efir

    @Composable
    private fun Fixture(
        slot: JsonObject,
        theme: String,
        content: @Composable () -> Unit,
    ) {
        val key = slot["key"]!!.jsonPrimitive.content
        val outline = slot["drawnOutline"]!!.jsonPrimitive.float
        val origin = Offset(slot["ox"]!!.jsonPrimitive.float, slot["oy"]!!.jsonPrimitive.float)
        MutantTheme(colors = palette(theme)) {
            CompositionLocalProvider(
                // The cell's own slot: a page may give several cells one key.
                LocalMutantOutlineOverride provides { if (it == key) outline else null },
                LocalMutantSlotOrigin provides { if (it == key) origin else null },
                LocalMutantTextRaster provides ChromeText,
                LocalMutantStage provides slot["stage"]!!.jsonPrimitive.content,
            ) {
                ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) { content() }
            }
        }
    }

    /** The cell [stem] drawn by [draw], given a modifier that places and sizes it at the page's slot. */
    fun cell(
        stem: String,
        theme: String,
        draw: @Composable (Modifier) -> Unit,
    ): DesignParity.Result {
        val s = DesignParity.reference(stem)["slot"]!!.jsonObject
        val origin = (s["origin"] as JsonArray).map { it.jsonPrimitive.int }
        val x = s["ox"]!!.jsonPrimitive.int - origin[0]
        val y = s["oy"]!!.jsonPrimitive.int - origin[1]
        val ground = DesignParity.rgb(s["background"]!!.jsonPrimitive.content)
        return DesignParity.compare(stem, ground) {
            Fixture(s, theme) {
                Box(Modifier.fillMaxSize().background(ground)) {
                    draw(Modifier.offset(x.dp, y.dp).size(s["w"]!!.jsonPrimitive.int.dp, s["h"]!!.jsonPrimitive.int.dp))
                }
            }
        }
    }

    class Split(
        val result: DesignParity.Result,
        /** Pixels differing outside the clean zone: the body's. */
        val body: Int,
        val kit: Extent?,
        val page: Extent?,
        val kitInk: Int,
        val pageInk: Int,
    ) {
        /** The content stands where the page's does — its top row exactly — and is the page's colour. */
        val contentHolds: Boolean
            get() =
                (kit == null && page == null) ||
                    (
                        kit != null && page != null && kit.near(page, px = 1, topPx = 0) &&
                            channelGap(kitInk, pageInk) <= INK_COLOUR
                    )

        override fun toString(): String =
            "$result; body $body px; content kit $kit, page $page; ink kit #${kitInk.toString(
                16,
            )}, page #${pageInk.toString(16)}"
    }

    /**
     * The cell `<base>_<theme>` and its twin `<base>Bare_<theme>`, drawn by [draw] with its content shown and hidden.
     * [threshold] is what the content must add to count: text's 64, or less over a grey body (the disabled ones).
     */
    fun split(
        base: String,
        theme: String,
        threshold: Int = TEXT_INK,
        draw: @Composable (Modifier, Boolean) -> Unit,
    ): Split {
        val stem = "${base}_$theme"
        val r = cell(stem, theme) { draw(it, false) }
        val bare = cell("${base}Bare_$theme", theme) { draw(it, true) }
        val s = DesignParity.reference(stem)["slot"]!!.jsonObject
        val origin = (s["origin"] as JsonArray).map { it.jsonPrimitive.int }
        val cz = (s["cz"] as JsonArray).map { it.jsonPrimitive.float }
        val x0 = s["ox"]!!.jsonPrimitive.int - origin[0] + cz[0]
        val y0 = s["oy"]!!.jsonPrimitive.int - origin[1] + cz[1]

        fun inside(i: Int): Boolean {
            val x = i % r.width
            val y = i / r.width
            return x >= x0 && x < x0 + cz[2] && y >= y0 && y < y0 + cz[3]
        }
        var body = 0
        for (i in 0 until r.width * r.height) if (!inside(i) && channelGap(r.actual[i], r.reference[i]) > 16) body++
        val kit = extentOf(r.width, r.height) { inside(it) && channelGap(r.actual[it], bare.actual[it]) > threshold }
        val page =
            extentOf(r.width, r.height) {
                inside(it) &&
                    channelGap(r.reference[it], bare.reference[it]) > threshold
            }
        val zone = (0 until r.width * r.height).filter(::inside)
        val kitInk = r.actual[zone.maxBy { channelGap(r.actual[it], bare.actual[it]) }]
        val pageInk = r.reference[zone.maxBy { channelGap(r.reference[it], bare.reference[it]) }]
        return Split(r, body, kit, page, kitInk, pageInk)
    }

    /** The body's floor in pixels (research §1.8). */
    const val FLOOR_PIXELS = 5

    /** Content's colour at a pixel both cover whole: the pixel threshold. */
    const val INK_COLOUR = 16

    /** Half of what dark ink adds over the disabled body's grey (about 100). */
    const val WEAK_INK = 50
}
