package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.gen.Instance
import io.github.youndie.mutant.tokens.MutantBody
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * B-156: a surface leaving the composition gives its body to the pool, and a surface composed later for the same
 * signature — a tab visited again — takes it back in its first draw instead of building it; another signature does not.
 */
@OptIn(ExperimentalTestApi::class)
class BodyPoolTest {
    @BeforeTest
    fun empty() = BodyPool.clear()

    @AfterTest
    fun emptyAgain() = BodyPool.clear()

    @Test
    fun `a body composed again for its signature is taken from the pool, not built`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var shown by mutableStateOf(true)
            var width by mutableStateOf(328)
            val seen = mutableListOf<Instance>()
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalMutantPoolBodies provides true,
                    LocalMutantBodyAsImage provides false,
                ) {
                    ProvideMutantClock(MutantClock()) {
                        Box(Modifier.size(360.dp, 120.dp).testTag("stage")) {
                            if (shown) {
                                MutantSurface(MutantBody.Card, "card/b156", Modifier.size(width.dp, 80.dp), probe = {
                                    if (seen.lastOrNull() !== it) seen += it
                                }) {
                                    BasicText("Кассета 07", Modifier.padding(16.dp).mutantCleanZone())
                                }
                            }
                        }
                    }
                }
            }
            repeat(10) { mainClock.advanceTimeByFrame() }
            val first = assertNotNull(seen.firstOrNull(), "drawn")
            shown = false
            repeat(5) { mainClock.advanceTimeByFrame() }
            shown = true
            repeat(10) { mainClock.advanceTimeByFrame() }
            assertEquals(1, seen.toSet().size, "the same instance drawn again: ${seen.size} draws of new instances")
            assertSame(first, seen.last())
            // Another size is another signature: built anew.
            shown = false
            repeat(5) { mainClock.advanceTimeByFrame() }
            width = 300
            shown = true
            repeat(10) { mainClock.advanceTimeByFrame() }
            assertEquals(2, seen.toSet().size, "a new instance for the new size")
        }
}
