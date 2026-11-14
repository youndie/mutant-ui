package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
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
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-15: every button the Components page draws, label in, in both themes, drawn by the kit's own button on the
 * page's stage, each body generated where the engine stood it and at its outline width (D16).
 */
class ButtonParityTest {
    private val cells = listOf("Rest", "Pressed", "Loading", "Success", "Disabled", "OutlinedRest", "OutlinedDisabled")

    private fun palette(theme: String): MutantColors =
        if (theme ==
            "Kopiya"
        ) {
            MutantPalettes.Kopiya
        } else {
            MutantPalettes.Efir
        }

    @Composable
    private fun Fixture(
        ref: JsonObject,
        theme: String,
        content: @Composable () -> Unit,
    ) {
        // The cell's own slot: the page gives its five filled buttons one key, so a lookup by key would find another.
        val slot = ref["slot"] as? JsonObject
        val outlines =
            slot?.let { mapOf(it["key"]!!.jsonPrimitive.content to it["drawnOutline"]!!.jsonPrimitive.float) }
                ?: emptyMap()
        val origins =
            slot?.let {
                mapOf(
                    it["key"]!!.jsonPrimitive.content to
                        Offset(it["ox"]!!.jsonPrimitive.float, it["oy"]!!.jsonPrimitive.float),
                )
            }
                ?: emptyMap()
        MutantTheme(colors = palette(theme)) {
            CompositionLocalProvider(
                LocalMutantOutlineOverride provides { outlines[it] },
                LocalMutantSlotOrigin provides { origins[it] },
                LocalMutantTextRaster provides ChromeText,
                LocalMutantStage provides "button",
            ) {
                ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) { content() }
            }
        }
    }

    internal fun cell(
        name: String,
        theme: String,
        dx: Int = 0,
        dy: Int = 0,
        hideLabel: Boolean = false,
    ): DesignParity.Result {
        // The bare twin is the page's cell with its text hidden, drawn here without a label.
        val stem = "MutantButton_${name}${if (hideLabel) "Bare" else ""}_$theme"
        val ref = DesignParity.reference(stem)
        val s = ref["slot"]!!.jsonObject
        val origin = (s["origin"] as JsonArray).map { it.jsonPrimitive.int }
        val x = s["ox"]!!.jsonPrimitive.int - origin[0] + dx
        val y = s["oy"]!!.jsonPrimitive.int - origin[1] + dy
        val w = s["w"]!!.jsonPrimitive.int
        val ground = DesignParity.rgb(s["background"]!!.jsonPrimitive.content)
        val colors = palette(theme)
        return DesignParity.compare(stem, ground) {
            Fixture(ref, theme) {
                Box(Modifier.fillMaxSize().background(ground)) {
                    val m = Modifier.offset(x.dp, y.dp).width(w.dp)
                    val outlined = name.startsWith("Outlined")
                    val enabled = !name.endsWith("Disabled")
                    val text =
                        when (name) {
                            "Loading" -> "Перемотка…"
                            "Success" -> "Готово"
                            "OutlinedRest", "OutlinedDisabled" -> "В очередь"
                            else -> "Воспроизвести"
                        }
                    val state =
                        when (name) {
                            "Loading" -> MutantButtonState.Loading
                            "Success" -> MutantButtonState.Success
                            else -> MutantButtonState.Idle
                        }
                    val key = if (outlined) "button-o" else "button"
                    when {
                        // Through the public buttons, so their own colours and icons are what is held.
                        !hideLabel && name != "Pressed" && !outlined -> {
                            MutantButton(text, {}, m, enabled = enabled, state = state, key = key)
                        }

                        !hideLabel && name != "Pressed" -> {
                            MutantOutlinedButton(text, {}, m, enabled = enabled, key = key)
                        }

                        // A held boil (the page's `data-heat`) and the bare twins need the body itself.
                        else -> {
                            ButtonBody(
                                text = text,
                                onClick = {},
                                modifier = m,
                                enabled = enabled,
                                state = state,
                                icon = if (name == "Success") io.github.youndie.mutant.icons.MutantIcons.Done else null,
                                key = key,
                                interactionSource = null,
                                label =
                                    when {
                                        hideLabel -> androidx.compose.ui.graphics.Color.Transparent
                                        !outlined -> colors.onPrimary
                                        enabled -> colors.textAccent
                                        else -> colors.textDisabled
                                    },
                                outlined = outlined,
                                heat = if (name == "Pressed") 1.0 else null,
                            )
                        }
                    }
                }
            }
        }
    }

    /** A cell split at the engine's clean zone: the body around it, the label inside it. */
    internal class Split(
        val result: DesignParity.Result,
        /** Pixels differing outside the clean zone: the body's. */
        val body: Int,
        /** Where the label's ink lies — what it adds over the same button drawn without it — here and on the page. */
        val kit: Extent?,
        val page: Extent?,
        /** A label over the disabled body's grey, taken at a lower threshold. */
        val weak: Boolean = false,
    ) {
        override fun toString(): String =
            "$result; body $body px; label kit $kit, page $page${if (weak) " (weak)" else ""}"
    }

    internal fun split(
        name: String,
        theme: String,
    ): Split {
        val r = cell(name, theme)
        val bare = cell(name, theme, hideLabel = true)
        val s = DesignParity.reference("MutantButton_${name}_$theme")["slot"]!!.jsonObject
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

        // What each renderer's label adds over its own bare body: 64 levels, as text is held (research §1.14). A disabled
        // label lies over the no-signal body's grey stripes, where dark ink adds about 100 at most and a glyph's faint top
        // rows add 25–60, which 64 cuts apart in the two renderers: there the threshold is half of 100.
        val weak = name.endsWith("Disabled")
        val threshold = if (weak) WEAK_INK else TEXT_INK
        val kit = extentOf(r.width, r.height) { inside(it) && channelGap(r.actual[it], bare.actual[it]) > threshold }
        val page =
            extentOf(r.width, r.height) {
                inside(it) &&
                    channelGap(r.reference[it], bare.reference[it]) > threshold
            }
        return Split(r, body, kit, page, weak)
    }

    @Test
    fun `every button the page draws matches it, body and label, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            for (name in cells) {
                val stem = "MutantButton_${name}_$theme"
                val c = split(name, theme)
                lines += c.toString()
                val labelHere =
                    c.kit != null && c.page != null && c.kit.near(c.page, px = 1, topPx = 0)
                if (c.body > (RESIDUAL[stem] ?: FLOOR_PIXELS) || !labelHere) off += stem
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    @Test
    fun `the text button's divider is the page's and its label stands where the page's does`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantButton_Text_$theme"
            val ref = DesignParity.reference(stem)
            val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
            val r =
                DesignParity.compare(stem, ground) {
                    Fixture(ref, theme) {
                        Box(Modifier.fillMaxSize().background(ground)) {
                            TextButtonBody("Сбросить", {}, Modifier, true, "0", null, dividerWidth = 76.dp)
                        }
                    }
                }
            val g =
                ((ground.red * 255).toInt() shl 16) or ((ground.green * 255).toInt() shl 8) or
                    (ground.blue * 255).toInt()
            // The label above row 30, the divider under it (the cell: 48 tall, the divider 2 dp, 5 under the label).
            val kit = extentOf(r.width, 30) { channelGap(r.actual[it], g) > TEXT_INK }
            val page = extentOf(r.width, 30) { channelGap(r.reference[it], g) > TEXT_INK }
            var divider = 0
            for (i in 30 * r.width until r.width * r.height) if (channelGap(r.actual[i], r.reference[i]) > 16) divider++
            lines += "$r; divider $divider px; label kit $kit, page $page"
            // The label's line box is centred in 48 at 11.5 dp, plus the element's own 0.19 on the page: a half pixel
            // the two renderers round apart, so its top is held within a pixel here.
            if (divider > FLOOR_PIXELS || kit == null || page == null || !kit.near(page, px = 1, topPx = 1)) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    private companion object {
        /** The body's floor in pixels (research §1.8). */
        const val FLOOR_PIXELS = 5

        /** Half of what a dark label adds over the disabled body's grey (106 − 5 on «Эфир»). */
        const val WEAK_INK = 50

        /**
         * Over the floor, measured on 2026-11-14 and never to be raised. «Копия» slips the fill 1.5 dp off the outline,
         * and Chrome draws the speckle as an SVG pattern, rasterised once and resampled under that fractional
         * translation, where the kit draws each square exactly — single pixels on the speckle's 40 dp grid. The
         * outlined button's outline lies over its own misregistered shadow, one shape's edge over another's fill
         * (as the illustrations' parts, research §1.13): isolated pixels 17–36 levels apart.
         */
        val RESIDUAL =
            mapOf(
                "MutantButton_Rest_Kopiya" to 18,
                "MutantButton_Pressed_Kopiya" to 16,
                "MutantButton_Loading_Kopiya" to 15,
                "MutantButton_OutlinedRest_Efir" to 6,
                "MutantButton_OutlinedRest_Kopiya" to 11,
            )
    }
}
