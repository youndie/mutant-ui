package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-186, the designer's answer 18: the text field's clean zone is its input area, the slot less 14 dp on each side,
 * never the text, so the body — its mosaic, flow lines and marks — is the same picture whatever is typed. Read as the
 * bare body (texts, caret and icon drawn transparent) under an empty field, one letter and a full line, at rest and
 * focused, in both palettes, at density 1 and 2. The search bar shares the field and its clear icon comes with the
 * first letter.
 */
@OptIn(ExperimentalTestApi::class)
class TextFieldCleanZoneTest {
    private val values = listOf("", "N", "Night to Tuesday from the start of side one")

    private fun differences(
        component: String,
        draw: @androidx.compose.runtime.Composable (String, Boolean, Modifier) -> Unit,
    ): List<String> {
        val off = ArrayList<String>()
        for (focused in listOf(false, true)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                var value by mutableStateOf("")
                var colors by mutableStateOf<MutantColors>(MutantPalettes.Efir)
                var density by mutableStateOf(1f)
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(density)) {
                        MutantTheme(colors = colors, reduceMotion = true) {
                            Box(Modifier.padding(12.dp)) { draw(value, focused, Modifier.width(240.dp).testTag("f")) }
                        }
                    }
                }
                for ((name, palette) in listOf("Efir" to MutantPalettes.Efir, "Kopiya" to MutantPalettes.Kopiya)) {
                    for (d in listOf(1f, 2f)) {
                        colors = palette
                        density = d
                        val shots =
                            values.map { v ->
                                value = v
                                // The zone is measured as the field is placed, and the body drawn from it a frame on.
                                repeat(3) { mainClock.advanceTimeByFrame() }
                                onNodeWithTag("f").captureToImage().toPixelMap()
                            }
                        val empty = shots[0]
                        val state = if (focused) "focused" else "at rest"
                        shots.drop(1).forEachIndexed { i, px ->
                            val n = differing(px, empty)
                            if (n != 0) off += "$component $state $name d$d «${values[i + 1].take(5)}»: $n px"
                        }
                    }
                }
            }
        }
        return off
    }

    /** The pixels two pictures differ in, or −1 when their sizes do. */
    private fun differing(
        a: PixelMap,
        b: PixelMap,
    ): Int =
        if (a.width != b.width || a.height != b.height) {
            -1
        } else {
            (0 until a.height).sumOf { y -> (0 until a.width).count { x -> a[x, y] != b[x, y] } }
        }

    @Test
    fun `the text field's body is the same picture empty, with one letter and with a full line`() {
        val off =
            differences("field") { value, focused, m ->
                TextFieldBody(
                    value,
                    {},
                    m,
                    "ПОИСК",
                    "Найти запись",
                    true,
                    false,
                    KeyboardOptions.Default,
                    "field",
                    null,
                    focusedAs = focused,
                    hidden = true,
                )
            }
        assertTrue(off.isEmpty(), "the body moved with the text against the empty field's:\n" + off.joinToString("\n"))
    }

    @Test
    fun `the search bar's body is the same picture empty, with one letter and with a full line`() {
        val off =
            differences("search bar") { value, focused, m ->
                SearchBarBody(
                    value,
                    {},
                    m,
                    "ПОИСК",
                    "Найти запись",
                    null,
                    "Очистить",
                    true,
                    "search",
                    null,
                    focusedAs = focused,
                    hidden = true,
                ) {}
            }
        assertTrue(off.isEmpty(), "the body moved with the text against the empty bar's:\n" + off.joinToString("\n"))
    }
}
