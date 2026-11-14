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

    private fun sweep(density: Density?): List<String> {
        val failures = mutableListOf<String>()
        for (s in subjects) {
            for (p in places) {
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
    fun `every component in every place lays out and draws`() = assertEquals(emptyList(), sweep(null))

    @Test
    fun `every component in every place at font scale 3`() =
        assertEquals(emptyList(), sweep(Density(1f, fontScale = 3f)))

    @Test
    fun `every component in every place at density 3`() = assertEquals(emptyList(), sweep(Density(3f)))

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
