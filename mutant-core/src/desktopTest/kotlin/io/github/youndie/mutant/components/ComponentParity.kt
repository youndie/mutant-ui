package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import io.github.youndie.mutant.surface.LocalMutantSlotSize
import io.github.youndie.mutant.surface.LocalMutantTextZone
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import io.github.youndie.mutant.type.LocalMutantTextRaster
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
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

    /** One body as the engine built it: its key, origin, size, outline and text zone. */
    private class EngineSlot(
        val key: String,
        val origin: Offset,
        val size: androidx.compose.ui.geometry.Size,
        val outline: Float,
        val zone: DoubleArray?,
    ) {
        companion object {
            fun of(o: JsonObject) =
                EngineSlot(
                    o["key"]!!.jsonPrimitive.content,
                    Offset(o["ox"]!!.jsonPrimitive.float, o["oy"]!!.jsonPrimitive.float),
                    androidx.compose.ui.geometry
                        .Size(o["w"]!!.jsonPrimitive.float, o["h"]!!.jsonPrimitive.float),
                    o["drawnOutline"]!!.jsonPrimitive.float,
                    // The engine records "no text" as [-99, -99, 0, 0] and then keeps no clean zone at all.
                    (o["tz"] as? JsonArray)?.map { it.jsonPrimitive.double }?.toDoubleArray()?.takeIf { it[2] > 0 },
                )
        }
    }

    /** Every body the engine built on [stem]'s stage, by key: the cell's own and the stage's others. */
    fun stageSlots(stem: String): Map<String, Any> {
        val ref = DesignParity.reference(stem)
        val all = listOf(ref["slot"]!!.jsonObject) + (ref["stageSlots"] as? JsonArray).orEmpty().map { it.jsonObject }
        return all.map { EngineSlot.of(it) }.associateBy { it.key }
    }

    /** Draws [content]'s bodies where the engine built them: [slots] from [stageSlots]. */
    @Composable
    fun ProvideSlots(
        slots: Map<String, Any>,
        content: @Composable () -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        val s = slots as Map<String, EngineSlot>
        CompositionLocalProvider(
            LocalMutantOutlineOverride provides { s[it]?.outline },
            LocalMutantSlotOrigin provides { s[it]?.origin },
            LocalMutantSlotSize provides { s[it]?.size },
            LocalMutantTextZone provides { s[it]?.zone },
            content = content,
        )
    }

    @Composable
    private fun Fixture(
        ref: JsonObject,
        theme: String,
        content: @Composable () -> Unit,
    ) {
        val slot = ref["slot"]!!.jsonObject
        val own = EngineSlot.of(slot)
        // The cell's own slot, and the bodies the page nests inside it (a switch's thumb), each by its key: a page may
        // give several cells one key, so only the slots within this cell's box are taken.
        val inner =
            (ref["stageSlots"] as? JsonArray).orEmpty().map { EngineSlot.of(it.jsonObject) }.filter {
                it.key != own.key &&
                    it.origin.x >= own.origin.x && it.origin.x + it.size.width <= own.origin.x + own.size.width + 1 &&
                    it.origin.y >= own.origin.y && it.origin.y + it.size.height <= own.origin.y + own.size.height + 1
            }
        val slots = (inner + own).associateBy { it.key }
        MutantTheme(colors = palette(theme)) {
            CompositionLocalProvider(
                LocalMutantOutlineOverride provides { slots[it]?.outline },
                LocalMutantSlotOrigin provides { slots[it]?.origin },
                LocalMutantSlotSize provides { slots[it]?.size },
                LocalMutantTextZone provides { slots[it]?.zone },
                LocalMutantTextRaster provides ChromeText,
                LocalMutantStage provides slot["stage"]!!.jsonPrimitive.content,
            ) {
                ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) { content() }
            }
        }
    }

    /**
     * The cell [stem] drawn by [draw], given a modifier that places and sizes it at the page's slot. A cell sized by
     * its label ([fit]) gets the slot's width as a minimum: the engine records Chrome's 103.4 dp as 103, and Compose,
     * which rounds a text's width up, would cut the label to fit it.
     */
    fun cell(
        stem: String,
        theme: String,
        fit: Boolean = false,
        outset: Int = 0,
        dx: Int = 0,
        dy: Int = 0,
        draw: @Composable (Modifier) -> Unit,
    ): DesignParity.Result {
        val s = DesignParity.reference(stem)["slot"]!!.jsonObject
        val origin = (s["origin"] as JsonArray).map { it.jsonPrimitive.int }
        val x = s["ox"]!!.jsonPrimitive.int - origin[0] + dx
        val y = s["oy"]!!.jsonPrimitive.int - origin[1] + dy
        val ground = DesignParity.rgb(s["background"]!!.jsonPrimitive.content)
        return DesignParity.compare(stem, ground) {
            Fixture(DesignParity.reference(stem), theme) {
                Box(Modifier.fillMaxSize().background(ground)) {
                    val w = s["w"]!!.jsonPrimitive.int.dp
                    // A component whose touch target is taller than its body ([outset] above and below) is placed by
                    // its target, so the body lands on the slot only if the component centres it there.
                    val h = (s["h"]!!.jsonPrimitive.int + 2 * outset).dp
                    val at = Modifier.offset(x.dp, (y - outset).dp)
                    draw(if (fit) at.widthIn(min = w).height(h) else at.size(w, h))
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
        /** Pixels differing inside the clean zone: the content's, for content that is not text (an icon). */
        val content: Int = 0,
        /** A label beside the slot (a checkbox's row): where its ink lies and its colour, here and on the page. */
        val labelKit: Extent? = null,
        val labelPage: Extent? = null,
        val labelKitInk: Int = 0,
        val labelPageInk: Int = 0,
        /** Where the cell was placed, within a pixel of its slot. */
        val placed: Pair<Int, Int> = 0 to 0,
        /** The bare twin, for a test that reads the content part by part (a navigation bar's items). */
        val bare: DesignParity.Result? = null,
    ) {
        /** The label beside, when there is one, stands where the page's does and is its colour. */
        val labelHolds: Boolean
            get() =
                (labelKit == null && labelPage == null) ||
                    (
                        labelKit != null && labelPage != null && labelKit.near(labelPage, px = 1, topPx = 0) &&
                            channelGap(labelKitInk, labelPageInk) <= INK_COLOUR
                    )

        /** The content stands where the page's does — its top row exactly — and is the page's colour. */
        val contentHolds: Boolean
            get() =
                (kit == null && page == null) ||
                    (
                        kit != null && page != null && kit.near(page, px = 1, topPx = 0) &&
                            channelGap(kitInk, pageInk) <= INK_COLOUR
                    )

        override fun toString(): String =
            "$result; body $body px; content $content px, kit $kit, page $page; ink kit #${kitInk.toString(16)}, " +
                "page #${pageInk.toString(16)}; label kit $labelKit, page $labelPage; placed $placed"
    }

    /**
     * The cell `<base>_<theme>` and its twin `<base>Bare_<theme>`, drawn by [draw] with its content shown and hidden.
     * [threshold] is what the content must add to count: text's 64, or less over a grey body (the disabled ones).
     */
    fun split(
        base: String,
        theme: String,
        threshold: Int = TEXT_INK,
        fit: Boolean = false,
        outset: Int = 0,
        labelBeside: Boolean = false,
        draw: @Composable (Modifier, Boolean) -> Unit,
    ): Split {
        val stem = "${base}_$theme"
        // Chrome snaps a stage's layer of bodies by a rule the DOM does not show (research §1.13): the whole cell is
        // placed where its bare twin matches best within a pixel; a part out of place inside it still differs.
        val (dx, dy) =
            (-1..1)
                .flatMap { dx -> (-1..1).map { dy -> dx to dy } }
                .minBy { (dx, dy) -> cell("${base}Bare_$theme", theme, fit, outset, dx, dy) { draw(it, true) }.pixels }
        val r = cell(stem, theme, fit, outset, dx, dy) { draw(it, false) }
        val bare = cell("${base}Bare_$theme", theme, fit, outset, dx, dy) { draw(it, true) }
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
        // A label beside the slot starts past the body's right edge and its outline.
        val labelFrom = s["ox"]!!.jsonPrimitive.int - origin[0] + s["w"]!!.jsonPrimitive.int + LABEL_CLEAR

        fun label(i: Int): Boolean = labelBeside && i % r.width >= labelFrom

        // Above a component's own row (one placed by its target) lies the row above it, whose drips hang into the crop.
        val rowTop = s["oy"]!!.jsonPrimitive.int - origin[1] - outset + dy

        fun foreign(i: Int): Boolean = outset > 0 && i / r.width < rowTop
        var body = 0
        var content = 0
        for (i in 0 until r.width * r.height) {
            if (channelGap(r.actual[i], r.reference[i]) <= 16) continue
            if (inside(i)) {
                content++
            } else if (!label(i) && !foreign(i)) {
                body++
            }
        }
        val kit = extentOf(r.width, r.height) { inside(it) && channelGap(r.actual[it], bare.actual[it]) > threshold }
        val page =
            extentOf(r.width, r.height) {
                inside(it) &&
                    channelGap(r.reference[it], bare.reference[it]) > threshold
            }
        val zone = (0 until r.width * r.height).filter(::inside)
        // An empty zone (a slot with nothing to keep clean, an unchecked box) has no content and no ink.
        val kitInk = zone.maxByOrNull { channelGap(r.actual[it], bare.actual[it]) }?.let { r.actual[it] } ?: 0
        val pageInk = zone.maxByOrNull { channelGap(r.reference[it], bare.reference[it]) }?.let { r.reference[it] } ?: 0
        if (!labelBeside) return Split(r, body, kit, page, kitInk, pageInk, content, placed = dx to dy, bare = bare)
        val labelKit =
            extentOf(r.width, r.height) { label(it) && channelGap(r.actual[it], bare.actual[it]) > threshold }
        val labelPage =
            extentOf(r.width, r.height) {
                label(it) &&
                    channelGap(r.reference[it], bare.reference[it]) > threshold
            }
        val beside = (0 until r.width * r.height).filter(::label)
        val labelKitInk = r.actual[beside.maxBy { channelGap(r.actual[it], bare.actual[it]) }]
        val labelPageInk = r.reference[beside.maxBy { channelGap(r.reference[it], bare.reference[it]) }]
        return Split(
            r,
            body,
            kit,
            page,
            kitInk,
            pageInk,
            content,
            labelKit,
            labelPage,
            labelKitInk,
            labelPageInk,
            dx to dy,
            bare,
        )
    }

    /** The body's floor in pixels (research §1.8). */
    const val FLOOR_PIXELS = 5

    /** From a slot's right edge to where a label beside it is counted: past the outline, short of a 12 dp gap. */
    const val LABEL_CLEAR = 6

    /** Content's colour at a pixel both cover whole: the pixel threshold. */
    const val INK_COLOUR = 16

    /** Half of what dark ink adds over the disabled body's grey (about 100). */
    const val WEAK_INK = 50
}
