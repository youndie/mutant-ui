package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.icons.MutantIcons
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-24: the navigation bar the page draws — bar, pill, icons, badge and labels — in both themes. */
class NavigationBarParityTest {
    /** The page's four destinations, the second with its count. */
    internal val items =
        listOf(
            MutantNavigationItem("Эфир", MutantIcons.Home),
            MutantNavigationItem("Очередь", MutantIcons.Schedule, badge = "3"),
            MutantNavigationItem("Поиск", MutantIcons.Search),
            MutantNavigationItem("Настройка", MutantIcons.Settings),
        )

    @Test
    fun `the navigation bar matches the page, in both themes`() {
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (theme in listOf("Efir", "Kopiya")) {
            val stem = "MutantNavigationBar_Rest_$theme"
            val c =
                ComponentParity.split("MutantNavigationBar_Rest", theme, TEXT_INK) { m, hidden ->
                    // The badge where the page stands it, 6 dp in from the icon box's right (B-204's departure, research D20).
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalMutantNavigationBarAsDesigned provides true,
                    ) {
                        NavigationBarBody(items, 0, {}, m, "navbar", "navpill", hidden)
                    }
                }
            lines += "$stem: $c"
            val items = items(stem, c, lines)
            val kept = kept(stem, c)
            val icons = icons(stem, c)
            lines += "  kept in the zone $kept px; icons $icons px"
            val bodyOver = c.body > (RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS)
            val keptOver =
                kept > (KEPT_RESIDUAL[stem] ?: ComponentParity.FLOOR_PIXELS) ||
                    icons.withIndex().any { (i, n) -> n > (ICON_RESIDUAL["$stem/$i"] ?: ComponentParity.FLOOR_PIXELS) }
            if (bodyOver || !c.contentHolds || !items || keptOver) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "off: $off\n" + lines.joinToString("\n"))
    }

    /**
     * Each item part by part — its icon box (with the pill and the badge) and its label — where the page's ink lies and
     * in its colour, ink being what the cell adds over its bare twin: one extent over four items would hide a word out
     * of place.
     */
    private fun items(
        stem: String,
        c: ComponentParity.Split,
        lines: MutableList<String>,
    ): Boolean {
        val r = c.result
        val bare = c.bare!!
        val s =
            io.github.youndie.mutant.DesignParity
                .reference(stem)["slot"]!!
                .jsonObject
        val origin = (s["origin"] as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.int }
        val bx = s["ox"]!!.jsonPrimitive.int - origin[0] + c.placed.first
        val by = s["oy"]!!.jsonPrimitive.int - origin[1] + c.placed.second
        val cell = s["w"]!!.jsonPrimitive.int / 4
        // The icon box 11 dp down, 30 tall; the label from 3 dp under it to the zone's bottom.
        val parts = listOf("icon" to (by + 11 until by + 41), "label" to (by + 44 until by + 60))
        var ok = true
        for (i in 0 until 4) {
            val xs = bx + i * cell until bx + (i + 1) * cell
            for ((name, ys) in parts) {
                fun inPart(p: Int) = p % r.width in xs && p / r.width in ys
                val kit =
                    extentOf(r.width, r.height) {
                        inPart(it) &&
                            channelGap(r.actual[it], bare.actual[it]) > TEXT_INK
                    }
                val page =
                    extentOf(r.width, r.height) {
                        inPart(it) &&
                            channelGap(r.reference[it], bare.reference[it]) > TEXT_INK
                    }
                val zone = (0 until r.width * r.height).filter(::inPart)
                val kitInk = r.actual[zone.maxBy { channelGap(r.actual[it], bare.actual[it]) }]
                val pageInk = r.reference[zone.maxBy { channelGap(r.reference[it], bare.reference[it]) }]
                val colour = channelGap(kitInk, pageInk)
                val part = "$stem/$i/$name"
                val holds =
                    kit != null && page != null && kit.near(page, px = 1, topPx = TOP_RESIDUAL[part] ?: 0) &&
                        colour <= (COLOUR_RESIDUAL[part] ?: 16)
                if (!holds) {
                    ok = false
                    lines += "  $stem item $i $name: kit $kit page $page colour $colour"
                }
            }
        }
        return ok
    }

    /** The slot and where the cell placed it: (left, top) of the bar in the cell, and the bar's width. */
    private fun bar(
        stem: String,
        c: ComponentParity.Split,
    ): Triple<Int, Int, Int> {
        val s =
            io.github.youndie.mutant.DesignParity
                .reference(stem)["slot"]!!
                .jsonObject
        val origin = (s["origin"] as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.int }
        return Triple(
            s["ox"]!!.jsonPrimitive.int - origin[0] + c.placed.first,
            s["oy"]!!.jsonPrimitive.int - origin[1] + c.placed.second,
            s["w"]!!.jsonPrimitive.int,
        )
    }

    /**
     * What the bare twins keep inside the clean zone — the pill, the badge's plate and ring — against each other: the
     * content's check reads only what the text adds over them.
     */
    private fun kept(
        stem: String,
        c: ComponentParity.Split,
    ): Int {
        val (bx, by, _) = bar(stem, c)
        val bare = c.bare!!
        val s =
            io.github.youndie.mutant.DesignParity
                .reference(stem)["slot"]!!
                .jsonObject
        val cz = (s["cz"] as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.float }
        var n = 0
        for (i in 0 until bare.width * bare.height) {
            val x = i % bare.width - bx
            val y = i / bare.width - by
            if (x < cz[0] || x >= cz[0] + cz[2] || y < cz[1] || y >= cz[1] + cz[3]) continue
            if (channelGap(bare.actual[i], bare.reference[i]) > 16) n++
        }
        return n
    }

    /**
     * Each item's 22 dp icon, kit against page, pixel by pixel as the icons are held (research §1.13); the second
     * item's without the badge's plate over its corner, whose digit is held by place above.
     */
    private fun icons(
        stem: String,
        c: ComponentParity.Split,
    ): List<Int> {
        val (bx, by, w) = bar(stem, c)
        val r = c.result
        val cell = w / 4
        return (0 until 4).map { i ->
            val x0 = bx + i * cell + (cell - 22) / 2
            val y0 = by + 11 + 4
            var n = 0
            for (y in y0 until y0 + 22) {
                for (x in x0 until x0 + 22) {
                    // The badge's plate: from 2 dp above the icon box, its right 6 dp in, about 18 wide and 16 tall.
                    if (i == 1 && y < by + 11 + 16 && x >= bx + i * cell + (cell - 56) / 2 + 56 - 6 - 20) continue
                    if (channelGap(r.actual[y * r.width + x], r.reference[y * r.width + x]) > 16) n++
                }
            }
            n
        }
    }

    /** The engine's own record of this slot, read through the kit's probes at the page's size. */
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test
    fun `the bar and the pill keep clean what the engine keeps clean, and the bar draws no flow lines`() {
        val s =
            io.github.youndie.mutant.DesignParity
                .reference("MutantNavigationBar_Rest_Efir")["slot"]!!
                .jsonObject
        val zone = (s["tz"] as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.double }
        val pillZone =
            (
                io.github.youndie.mutant.DesignParity.reference(
                    "MutantNavigationBar_Rest_Efir",
                )["stageSlots"] as kotlinx.serialization.json.JsonArray
            ).map { it.jsonObject }
                .single { it["key"]!!.jsonPrimitive.content == "navpill" }["tz"]!!
                .let { (it as kotlinx.serialization.json.JsonArray).map { v -> v.jsonPrimitive.double } }
        var bar: io.github.youndie.mutant.gen.Instance? = null
        var pill: io.github.youndie.mutant.gen.Instance? = null
        androidx.compose.ui.test.v2.runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit
                            .Density(1f),
                ) {
                    io.github.youndie.mutant.theme.MutantTheme {
                        androidx.compose.runtime.CompositionLocalProvider(
                            io.github.youndie.mutant.type.LocalMutantTextRaster provides
                                io.github.youndie.mutant.ChromeText,
                        ) {
                            NavigationBarBody(
                                items,
                                0,
                                {},
                                androidx.compose.ui.Modifier
                                    .width(328.dp),
                                "navbar",
                                "navpill",
                                probe = { bar = it },
                                pillProbe = { pill = it },
                            )
                        }
                    }
                }
            }
            // A running clock builds a body's first instance off the frame and draws it from the next (B-50).
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
        }
        val tz = bar!!.slot.textZone.toList()
        val ptz = pill!!.slot.textZone.toList()
        assertTrue(tz.zip(zone).all { (k, e) -> kotlin.math.abs(k - e) <= 1.0 }, "bar: kit $tz, engine $zone")
        assertTrue(
            ptz.zip(pillZone).all { (k, e) ->
                kotlin.math.abs(k - e) <= 1.0
            },
            "pill: kit $ptz, engine $pillZone",
        )
        assertTrue(!bar.slot.marks, "the page's data-marks=0: no flow lines on the bar")
        // With no pill and no badge, the icons are the zone's top: 4 dp into the 30 dp box 11 dp down, less the 2 dp
        // margin — the engine's measure of a bar like this one.
        var plain: io.github.youndie.mutant.gen.Instance? = null
        androidx.compose.ui.test.v2.runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit
                            .Density(1f),
                ) {
                    io.github.youndie.mutant.theme.MutantTheme {
                        NavigationBarBody(
                            items.map { MutantNavigationItem(it.label, it.icon) },
                            -1,
                            {},
                            androidx.compose.ui.Modifier
                                .width(328.dp),
                            "navbar",
                            "navpill",
                            probe = { plain = it },
                        )
                    }
                }
            }
            // The first instance arrives in the second frame (B-50).
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
        }
        assertEquals(13.0, plain!!.slot.textZone[1], "the icons' top, 15, less 2")
    }

    private companion object {
        val RESIDUAL = emptyMap<String, Int>()

        /**
         * Over the floor, measured on 2026-11-14 and never to be raised: inside «Копия»'s clean zone the speckle under
         * the print slip, a pixel on its 40 dp grid along row 31 (research §1.15), and the pill's outline over the bar's
         * fill at its right edge, one shape's edge over another's fill (§1.13).
         */
        val KEPT_RESIDUAL = mapOf("MutantNavigationBar_Rest_Kopiya" to 14)

        /**
         * The settings icon's two 2 dp knobs: the kit's arcs are cubics where Chrome's Skia draws conics, the cause
         * `IconParityTest` lists for Settings at 48 and 16 (6 and 5 px); at 22 dp, 11 and 13 px. Measured 2026-11-14.
         */
        val ICON_RESIDUAL = mapOf("MutantNavigationBar_Rest_Efir/3" to 11, "MutantNavigationBar_Rest_Kopiya/3" to 13)

        /**
         * A top row apart, measured on 2026-11-14: the ascender of «ф» and the breve of «й» lie on the ink threshold in
         * «Эфир», where the kit's light-on-dark mask crosses it a row above Chrome's; the labels without them lie
         * exactly, and in «Копия» all four do.
         */
        val TOP_RESIDUAL =
            mapOf(
                "MutantNavigationBar_Rest_Efir/0/label" to 1,
                "MutantNavigationBar_Rest_Efir/3/label" to 1,
            )

        /**
         * The badge's «3» in Handjet 14 never covers a pixel whole in either renderer, and Chrome's dark-on-light mask
         * is the darker (its strongest pixel 48 against the kit's 92 in «Эфир»), in both themes, as the badge is the page's
         * literal amber and black in both: the inkiest pixel is no measure of the colour
         * there. Its plate and strokes stand where the page's do. Measured on 2026-11-14.
         */
        val COLOUR_RESIDUAL =
            mapOf("MutantNavigationBar_Rest_Efir/1/icon" to 44, "MutantNavigationBar_Rest_Kopiya/1/icon" to 44)
    }
}
