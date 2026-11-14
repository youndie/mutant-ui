package io.github.youndie.mutant.tokens

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-08: a second reading of `mutant-tokens.json`, in Kotlin, independent of `scripts/tokens.mjs`: every
 * generated value equals the file's, and the file's contrast claims recompute from the generated colours.
 */
class MutantTokensTest {
    private val tokens: JsonObject =
        Json
            .parseToJsonElement(
                File("../reference/design-system/mutant-tokens.json").readText(),
            ).jsonObject["mutant"]!!
            .jsonObject

    private val colorOf: Map<String, (MutantColors) -> Color> =
        mapOf(
            "primary" to { it.primary },
            "on-primary" to { it.onPrimary },
            "magenta" to { it.magenta },
            "cyan" to { it.cyan },
            "no-signal" to { it.noSignal },
            "amber" to { it.amber },
            "phosphor" to { it.phosphor },
            "danger" to { it.danger },
            "background" to { it.background },
            "surface" to { it.surface },
            "surface-variant" to { it.surfaceVariant },
            "ink" to { it.ink },
            "page" to { it.page },
            "text" to { it.text },
            "text-secondary" to { it.textSecondary },
            "text-disabled" to { it.textDisabled },
            "text-accent" to { it.textAccent },
            "text-osd" to { it.textOsd },
            "text-error" to { it.textError },
            "on-no-signal" to { it.onNoSignal },
            "on-no-signal-accent" to { it.onNoSignalAccent },
            "focus" to { it.focus },
            "misregister" to { it.misregister },
            "shadow-deep" to { it.shadowDeep },
            "highlight-on-dark" to { it.highlightOnDark },
            "dropout" to { it.dropout },
        )
    private val themes = mapOf("efir" to MutantPalettes.Efir, "kopiya" to MutantPalettes.Kopiya)

    private fun hex(c: Color): String {
        val argb =
            (c.alpha * 255 + .5).toInt() shl 24 or ((c.red * 255 + .5).toInt() shl 16) or
                ((c.green * 255 + .5).toInt() shl 8) or (c.blue * 255 + .5).toInt()
        return "#" + (argb and 0xFFFFFF).toString(16).padStart(6, '0')
    }

    @Test
    fun `every colour of both themes is the file's and every colour of the file is generated`() {
        for ((theme, palette) in themes) {
            val file = tokens["color"]!!.jsonObject[theme]!!.jsonObject
            assertEquals(file.keys, colorOf.keys, "$theme: the file's colours against the generated ones")
            for ((name, get) in colorOf) {
                val want = file[name]!!.jsonPrimitive.content
                assertEquals(want, hex(get(palette)), "$theme $name")
                assertEquals(1f, get(palette).alpha, "$theme $name is opaque")
            }
        }
    }

    /** WCAG 2.x relative luminance of an sRGB colour. */
    private fun luminance(c: Color): Double {
        fun lin(v: Float): Double {
            val x = (v * 255 + .5).toInt() / 255.0
            return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return .2126 * lin(c.red) + .7152 * lin(c.green) + .0722 * lin(c.blue)
    }

    @Test
    fun `the file's contrast claims recompute from the generated colours within 0_05`() {
        var pairs = 0
        for ((theme, palette) in themes) {
            for ((pair, claim) in tokens["contrast"]!!.jsonObject[theme]!!.jsonObject) {
                val (a, b) = pair.split("/").map { it.trim() }
                val la = luminance(colorOf.getValue(a)(palette))
                val lb = luminance(colorOf.getValue(b)(palette))
                val ratio = (max(la, lb) + .05) / (min(la, lb) + .05)
                assertTrue(
                    abs(ratio - claim.jsonPrimitive.double) <= .05,
                    "$theme $pair: claimed ${claim.jsonPrimitive.content}, computed $ratio",
                )
                assertTrue(ratio >= 4.5, "$theme $pair is under 4.5:1 ($ratio)")
                pairs++
            }
        }
        assertEquals(28, pairs, "contrast pairs read")
    }

    @Test
    fun `the grid and the type scale are the file's`() {
        val g = tokens["grid"]!!.jsonObject
        assertEquals(g["unit"]!!.jsonPrimitive.double, MutantGrid.UNIT)
        assertEquals(g["touch-target"]!!.jsonPrimitive.double, MutantGrid.TOUCH_TARGET)
        assertEquals(g["screen-margin"]!!.jsonPrimitive.double, MutantGrid.SCREEN_MARGIN)
        assertEquals(g["mosaic-blocks"]!!.jsonArray.map { it.jsonPrimitive.double.toInt() }, MutantGrid.MosaicBlocks)
        val pad = g["clean-zone-padding"]!!.jsonObject
        assertEquals(pad["horizontal"]!!.jsonPrimitive.double, MutantGrid.CLEAN_ZONE_HORIZONTAL)
        assertEquals(pad["vertical"]!!.jsonPrimitive.double, MutantGrid.CLEAN_ZONE_VERTICAL)
        assertEquals(pad["mosaic-extra"]!!.jsonPrimitive.double, MutantGrid.CLEAN_ZONE_MOSAIC_EXTRA)
        assertEquals(g["overhang-max"]!!.jsonPrimitive.double, MutantGrid.OVERHANG_MAX)
        assertEquals(g["overhang-list"]!!.jsonPrimitive.double, MutantGrid.OVERHANG_LIST)
        assertEquals(g["field-step"]!!.jsonObject["small"]!!.jsonPrimitive.double, MutantGrid.FIELD_STEP_SMALL)
        assertEquals(g["field-step"]!!.jsonObject["large"]!!.jsonPrimitive.double, MutantGrid.FIELD_STEP_LARGE)
        val t = tokens["type"]!!.jsonObject

        fun ints(
            role: String,
            key: String,
        ) = t[role]!!.jsonObject[key]!!.jsonArray.map { it.jsonPrimitive.double.toInt() }
        assertEquals(ints("display", "sizes"), MutantType.DisplaySizes)
        assertEquals(ints("text", "sizes"), MutantType.TextSizes)
        assertEquals(ints("text", "weights"), MutantType.TextWeights)
        assertEquals(ints("osd", "sizes"), MutantType.OsdSizes)
        assertEquals(t["osd"]!!.jsonObject["letter-spacing"]!!.jsonPrimitive.double, MutantType.OSD_LETTER_SPACING)
    }

    @Test
    fun `every profile is the file's but the pill's corners, which follow the engine`() {
        val fill =
            mapOf(
                "green" to MutantFill.Primary,
                "surf" to MutantFill.Surface,
                "surf2" to MutantFill.SurfaceVariant,
                "white" to MutantFill.Phosphor,
                "mag" to MutantFill.Magenta,
            )
        val file = tokens["profile"]!!.jsonObject
        assertEquals(file.keys, MutantBody.entries.map { it.name.lowercase() }.toSet(), "body types")
        for (body in MutantBody.entries) {
            val p = file[body.name.lowercase()]!!.jsonObject
            val at = body.name
            val radius = p["radius"]!!.jsonPrimitive
            assertEquals(if (radius.content == "full") 99.0 else radius.double, body.profile.radius, "$at radius")
            // research §1.5: the tokens call the pill's radius "full"; the engine does not flag it round.
            val round = radius.content == "full" && body != MutantBody.Pill
            assertEquals(round, body.profile.round, "$at round")
            assertEquals(p["overhang"]!!.jsonPrimitive.double, body.profile.overhang, "$at overhang")
            assertEquals(p["drip-share"]!!.jsonPrimitive.double, body.profile.dripShare, "$at drip share")
            assertEquals(fill.getValue(p["fill"]!!.jsonPrimitive.content), body.profile.fill, "$at fill")
            assertEquals(p["outline"]!!.jsonPrimitive.double, body.profile.outline, "$at outline")
            assertEquals(p["marks"]!!.jsonPrimitive.double == 1.0, body.profile.marks, "$at marks")
            assertEquals(p["mosaic"]!!.jsonPrimitive.double == 1.0, body.profile.mosaic, "$at mosaic")
            assertEquals(p["wobble"]!!.jsonPrimitive.double, body.profile.wobble, "$at wobble")
            assertEquals(p["dead-pixels"]!!.jsonPrimitive.double == 1.0, body.profile.deadPixels, "$at dead pixels")
        }
    }

    @Test
    fun `the motion durations and the genes are the file's`() {
        val m = tokens["motion"]!!.jsonObject

        fun ms(k: String) = m[k]!!.jsonObject["ms"]!!
        assertEquals(ms("tick").jsonPrimitive.double, MutantMotion.TICK_MS)
        assertEquals(ms("tick-hot").jsonPrimitive.double, MutantMotion.TICK_HOT_MS)
        assertEquals(ms("boil-over-in").jsonPrimitive.double, MutantMotion.BOIL_OVER_IN_MS)
        assertEquals(ms("boil-over-out").jsonPrimitive.double, MutantMotion.BOIL_OVER_OUT_MS)
        assertEquals(
            ms("pour").jsonArray.map {
                it.jsonPrimitive.double
            },
            listOf(MutantMotion.POUR_MIN_MS, MutantMotion.POUR_MAX_MS),
        )
        assertEquals(ms("seek").jsonPrimitive.double, MutantMotion.SEEK_MS)
        assertEquals(ms("slide").jsonPrimitive.double, MutantMotion.SLIDE_MS)
        assertEquals(ms("venom").jsonPrimitive.double, MutantMotion.VENOM_MS)
        assertEquals(ms("tracking").jsonPrimitive.double, MutantMotion.TRACKING_MS)
        assertEquals(m["tracking"]!!.jsonObject["every"]!!.jsonPrimitive.double, MutantMotion.TRACKING_EVERY_MS)
        assertEquals(m["drop"]!!.jsonObject["gravity"]!!.jsonPrimitive.double, MutantMotion.DROP_GRAVITY)
        val genes =
            mapOf(
                "tick" to MutantGenome.Tick,
                "boil" to MutantGenome.Boil,
                "noise" to MutantGenome.Noise,
                "slip" to MutantGenome.Slip,
                "outline" to MutantGenome.Outline,
                "generation" to MutantGenome.Generation,
            )
        for ((name, gene) in genes) {
            val g = tokens["genome"]!!.jsonObject[name]!!.jsonObject
            val range = g["range"]!!.jsonArray.map { it.jsonPrimitive.double }
            assertEquals(range, listOf(gene.min, gene.max), "$name range")
            assertEquals((g["default"] as? JsonPrimitive)?.doubleOrNull, gene.default, "$name default")
        }
    }
}
