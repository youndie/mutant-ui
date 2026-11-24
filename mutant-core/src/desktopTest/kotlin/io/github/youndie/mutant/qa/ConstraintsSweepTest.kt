package io.github.youndie.mutant.qa

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * B-40, the constraints axis: every public component at widths 0, 1, 24, 48 and 120, at height 0, in a 10 dp box,
 * with unbounded width, unbounded height and both, at font scale 3 and density 3 — laid out and drawn, never
 * throwing. The known defects are left out of the sweep and held by their own ignored tests.
 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class ConstraintsSweepTest {
    /** Cases that throw today, each held by an ignored test below (none since B-56). */
    private val known = emptySet<String>()

    /**
     * The places a sweep is split in, each a test of its own: one test for all ten places ran about 520 cells in 28–36 s
     * on a Mac (B-210). Together they are [places], each once.
     */
    private val widths = places.filter { it.name.startsWith("width ") }
    private val boxes = places - widths.toSet()

    private fun sweep(
        density: Density?,
        where: List<Place>,
    ): List<String> {
        val failures = mutableListOf<String>()
        for (s in subjects) {
            for (p in where) {
                val case = "${s.name} / ${p.name}"
                if (case in known) continue
                val e =
                    attempt {
                        val body = @androidx.compose.runtime.Composable {
                            MutantTheme {
                                p.wrap { m ->
                                    s.content(m, "Воспроизвести")
                                }
                            }
                        }
                        if (density ==
                            null
                        ) {
                            body()
                        } else {
                            CompositionLocalProvider(LocalDensity provides density) { body() }
                        }
                    }
                if (e != null) failures += "$case at $density: ${e.brief()}"
            }
        }
        return failures
    }

    @Test
    fun `the sweep's places are every place, each once`() {
        assertEquals(places.map { it.name }.sorted(), (widths + boxes).map { it.name }.sorted())
        assertEquals(5, widths.size, "widths 0, 1, 24, 48 and 120")
    }

    @Test
    fun `every component at every width lays out and draws`() = assertEquals(emptyList(), sweep(null, widths))

    @Test
    fun `every component in every box and unbounded place lays out and draws`() =
        assertEquals(emptyList(), sweep(null, boxes))

    @Test
    fun `every component at every width at font scale 3`() =
        assertEquals(emptyList(), sweep(Density(1f, fontScale = 3f), widths))

    @Test
    fun `every component in every box and unbounded place at font scale 3`() =
        assertEquals(emptyList(), sweep(Density(1f, fontScale = 3f), boxes))

    @Test
    fun `every component at every width at density 3`() = assertEquals(emptyList(), sweep(Density(3f), widths))

    @Test
    fun `every component in every box and unbounded place at density 3`() =
        assertEquals(emptyList(), sweep(Density(3f), boxes))

    @Test
    fun `F01 the navigation bar in a horizontally scrolling row does not throw`() =
        assertNull(
            attempt {
                MutantTheme {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        MutantNavigationBar(
                            listOf(
                                MutantNavigationItem("Дом", MutantIcons.Home),
                                MutantNavigationItem("Поиск", MutantIcons.Search),
                            ),
                            0,
                            {},
                        )
                    }
                }
            }?.brief(),
        )
}
