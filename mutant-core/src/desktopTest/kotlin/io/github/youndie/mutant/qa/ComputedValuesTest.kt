package io.github.youndie.mutant.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * B-40, the computed-values axis: progress at NaN, the infinities, below 0 and above 1; keys with odd characters;
 * a cocoon of no size; a cassette seed nobody would pick.
 */
@OptIn(ExperimentalTestApi::class)
class ComputedValuesTest {
    @Test
    fun `progress outside 0 to 1 reads and shows as its nearest end, NaN as 0`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val values =
                listOf(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -1f, 2f, -0f, Float.MIN_VALUE)
            val expected = listOf(0f, 0f, 1f, 0f, 1f, 0f, Float.MIN_VALUE)
            setContent {
                Still {
                    Column(Modifier.size(400.dp, 400.dp)) {
                        values.forEachIndexed {
                            i,
                            v,
                            ->
                            MutantLinearProgress(v, Modifier.width(300.dp).testTag("p$i"))
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            values.indices.forEach { i ->
                val info = onNodeWithTag("p$i").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
                assertEquals(ProgressBarRangeInfo(expected[i], 0f..1f), info, "at ${values[i]}")
            }
            val shown =
                allNodes()
                    .flatMap { n ->
                        n.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map {
                            n.boundsInRoot.top to
                                it.text
                        }
                    }.sortedBy { it.first }
                    .map { it.second }
            assertEquals(listOf("000%", "000%", "100%", "000%", "100%", "000%", "000%"), shown)
        }

    @Test
    fun `odd keys, an odd cassette and a cocoon of no size draw`() {
        val keys = listOf("", "/", "//", "\u0000", "ключ с пробелом", "😀", "a".repeat(10_000), "%s%d", "\n")
        val failures =
            keys.mapNotNull { k ->
                attempt {
                    MutantTheme(cassette = k) {
                        Column {
                            MutantButton("Пуск", {}, key = k)
                            MutantTextField("", {}, key = k)
                            MutantCocoon(Modifier.size(120.dp, 40.dp), key = k)
                            MutantCocoon(Modifier.size(0.dp), key = k)
                            MutantCocoon(Modifier.size(0.dp, 40.dp), key = k)
                            MutantCocoon(Modifier.size(1.dp), key = k)
                        }
                    }
                }?.let { "key '${k.take(12)}': ${it.brief()}" }
            }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun `a body of an extreme size draws`() =
        assertNull(
            attempt {
                MutantTheme {
                    Box(Modifier.size(400.dp, 800.dp)) {
                        MutantButton("Пуск", {}, Modifier.size(4000.dp, 48.dp))
                        MutantCocoon(Modifier.size(4000.dp, 4000.dp))
                    }
                }
            }?.brief(),
        )

    /** A consumer's own icon, named as one of the kit's: a square, nothing like the kit's house. */
    private val square =
        ImageVector
            .Builder("Home", 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes("M4 4h16v16H4z"), fill = SolidColor(androidx.compose.ui.graphics.Color.Black))
            .build()

    private fun fab(icon: ImageVector): List<androidx.compose.ui.graphics.Color> {
        var px = emptyList<androidx.compose.ui.graphics.Color>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Still {
                    Box(
                        Modifier.size(100.dp),
                    ) { MutantFab(icon, null, {}, Modifier.testTag("f"), key = "k") }
                }
            }
            mainClock.advanceTimeBy(300)
            px = onNodeWithTag("f").captureToImage().pixels()
        }
        return px
    }

    @Test
    fun `F19 the FAB draws the consumer's icon, whatever it is named`() =
        assertNotEquals(fab(MutantIcons.Home), fab(square))
}
