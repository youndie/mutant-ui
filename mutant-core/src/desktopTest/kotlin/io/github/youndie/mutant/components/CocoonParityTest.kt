package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.DesignParity
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.clock.STILL_MS
import io.github.youndie.mutant.theme.LocalMutantStage
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-22: the Card stage's two cocoons — the card's, and the list row's three — in both themes. */
class CocoonParityTest {
    /**
     * Each cocoon of a cell as the engine places it, x, y, w, h and its index on the stage: the engine draws from
     * whole offsets (`offsetLeft`, `offsetWidth`) in the stage's own picture. The row's column is the 362 dp row less
     * the 40 dp square and its 12 dp gap, 310; its lines 70 % and 45 % of that, 217 and 139.5, which the engine
     * measures whole, 140.
     */
    private val cells =
        mapOf(
            "Card" to listOf(intArrayOf(0, 0, 362, 80, 0)),
            "ListRow" to
                listOf(
                    intArrayOf(0, 20, 40, 40, 1),
                    intArrayOf(52, 20, 217, 16, 2),
                    intArrayOf(52, 44, 140, 16, 3),
                ),
        )

    internal fun cell(
        name: String,
        theme: String,
    ): DesignParity.Result {
        val stem = "MutantCocoon_${name}_$theme"
        val ref = DesignParity.reference(stem)
        val ground = DesignParity.rgb(ref["background"]!!.jsonPrimitive.content)
        // The stage stands a fraction of a pixel down the page, and Chrome draws its picture there.
        val dy = (ref["subpixel"] as JsonArray)[1].jsonPrimitive.double.toFloat()
        return DesignParity.compare(stem, ground) {
            MutantTheme(colors = ComponentParity.palette(theme)) {
                CompositionLocalProvider(LocalMutantStage provides "card") {
                    ProvideMutantClock(MutantClock(startMs = STILL_MS, frozen = true)) {
                        Box(Modifier.fillMaxSize().background(ground)) {
                            for ((x, y, w, h, i) in cells.getValue(name)) {
                                MutantCocoon(Modifier.offset(x.dp, (y + dy).dp).size(w.dp, h.dp), key = "$i")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `both cocoons the page draws match it, in both themes`() {
        val results = listOf("Efir", "Kopiya").flatMap { theme -> cells.keys.map { cell(it, theme) } }
        println(results.joinToString("\n") { "$it, ink ${it.ink}" })
        assertTrue(results.all { it.pixels <= ComponentParity.FLOOR_PIXELS }, results.joinToString("\n"))
    }
}
