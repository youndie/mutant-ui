package io.github.youndie.mutant.pattern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-14: the four patterns and their quiet variants against the Icons page's swatches, text hidden. */
class PatternParityTest {
    @Test
    fun `every pattern and its quiet variant match the page's swatch`() {
        val results =
            MutantPattern.entries.flatMap { pattern ->
                listOf(false, true).map { quiet ->
                    val stem = "Pattern_${pattern.name}${if (quiet) "Quiet" else ""}_Efir"
                    val ref = DesignParity.reference(stem)
                    val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
                    val radius = ref["radius"]!!.jsonPrimitive.double
                    DesignParity.compare(stem) {
                        MutantTheme {
                            // The swatch's own frame on the page, `box-shadow: 0 0 0 2px #05060c`, fills its corners.
                            Box(
                                Modifier.fillMaxSize().background(ground).drawBehind {
                                    drawRoundRect(
                                        Color(0xFF05060C),
                                        Offset(-2f, -2f),
                                        Size(size.width + 4, size.height + 4),
                                        CornerRadius(radius.toFloat() + 2),
                                    )
                                },
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(
                                            RoundedCornerShape(radius.dp),
                                        ).mutantPattern(pattern, quiet),
                                )
                            }
                        }
                    }
                }
            }
        println(results.joinToString("\n"))
        assertTrue(results.all { it.pixels <= FLOOR_PIXELS }, results.joinToString("\n"))
    }

    private companion object {
        /** The body's floor in pixels (research §1.8): a swatch's rounded corner is a filled contour like a body's. */
        const val FLOOR_PIXELS = 5
    }
}
