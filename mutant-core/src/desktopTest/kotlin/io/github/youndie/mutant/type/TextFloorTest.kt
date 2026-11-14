package io.github.youndie.mutant.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.ChromeText
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.TEXT_INK
import io.github.youndie.mutant.channelGap
import io.github.youndie.mutant.extentOf
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-52: a label alone against Chrome, in each face at sizes the type roles use, laid on Blink's line box
 * ([cssLine]) and rasterised as Chrome rasterises ([ChromeText]). What is left is the text floor.
 */
class TextFloorTest {
    private val stems =
        listOf("Handjet400_16", "Handjet400_20", "Handjet400_24", "Rubik400_13", "Rubik400_16", "Rubik500_14").map {
            "Text_${it}_Efir"
        }

    internal fun probe(
        stem: String,
        raster: Boolean = true,
    ): DesignParity.Result {
        val r = DesignParity.reference(stem)

        fun n(k: String) = r[k]!!.jsonPrimitive.double
        val family = r["family"]!!.jsonPrimitive.content
        val ground = Color(0xFF10142A)
        return DesignParity.compare(stem, ground) {
            MutantTheme {
                val fonts = io.github.youndie.mutant.theme.LocalMutantFonts.current!!
                val face = if (family == "Handjet") MutantFaceMetrics.Handjet else MutantFaceMetrics.Rubik
                Box(Modifier.fillMaxSize().background(ground)) {
                    BasicText(
                        r["text"]!!.jsonPrimitive.content,
                        Modifier.offset(r["left"]!!.jsonPrimitive.int.dp, r["top"]!!.jsonPrimitive.int.dp),
                        style =
                            TextStyle(
                                fontFamily = if (family == "Handjet") fonts.display else fonts.text,
                                fontWeight = FontWeight(r["weight"]!!.jsonPrimitive.int),
                                fontSize = n("size").sp,
                                letterSpacing = (n("spacing") / n("size")).em,
                                color = Color(0xFFE9F2E4),
                                platformStyle = if (raster) ChromeText else null,
                            ).cssLine(face, n("line").toFloat()),
                        softWrap = false,
                    )
                }
            }
        }
    }

    @Test
    fun `every label puts its ink where Chrome does, within a pixel`() {
        val ground = 0x10142A
        val lines = ArrayList<String>()
        val off = ArrayList<String>()
        for (stem in stems) {
            val a = probe(stem)
            val kit = extentOf(a.width, a.height) { channelGap(a.actual[it], ground) > TEXT_INK }!!
            val chrome = extentOf(a.width, a.height) { channelGap(a.reference[it], ground) > TEXT_INK }!!
            lines += "$a, ${"%.1f".format(a.inkPercent)} % of the ink; kit $kit, Chrome $chrome"
            if (!kit.near(chrome)) off += stem
        }
        println(lines.joinToString("\n"))
        assertTrue(off.isEmpty(), "text out of place: $off\n" + lines.joinToString("\n"))
    }

    @Test
    fun `the floor's share is printed with Compose's default raster beside it`() {
        println(
            stems.joinToString("\n") { stem ->
                val a = probe(stem)
                val b = probe(stem, raster = false)
                "$stem: ${"%.1f".format(
                    a.inkPercent,
                )} % of the ink as Chrome rasterises, ${"%.1f".format(b.inkPercent)} % by default"
            },
        )
    }
}
