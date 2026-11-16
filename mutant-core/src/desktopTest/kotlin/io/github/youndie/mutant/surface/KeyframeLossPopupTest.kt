package io.github.youndie.mutant.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.clock.ProvideMutantClock
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantMenu
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-98: a popup's bodies (a menu, a tooltip) stand in another layout hierarchy than the screen under a keyframe loss,
 * yet read its scene through the composition. While a loss runs they draw as they are, and nothing throws: the loss
 * covers the screen, not what floats over it.
 */
@OptIn(ExperimentalTestApi::class)
class KeyframeLossPopupTest {
    @Test
    fun `a menu open over a screen under a running loss draws through the loss`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var ran = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false) {
                        ProvideMutantClock(MutantClock()) {
                            KeyframeLossBody(
                                Modifier
                                    .size(
                                        300.dp,
                                        400.dp,
                                    ).testTag("s"),
                                MutantTheme.colors.page,
                                null,
                                {
                                    Column {
                                        MutantCard(
                                            Modifier.size(260.dp, 80.dp),
                                            key = "under",
                                        ) { MutantCardTitle("Без подписи №3") }
                                        Box { MutantMenu(true, {}, listOf("ТРЕКИНГ", "ЦВЕТНОСТЬ", "ЧЁТКОСТЬ"), 0, {}) }
                                    }
                                },
                                probe = { if (it) ran = true },
                            )
                        }
                    }
                }
            }
            // Across the first loss (3,5 s into the window), every frame drawn, the popup's root included.
            mainClock.advanceTimeBy(3_300)
            repeat(60) {
                mainClock.advanceTimeByFrame()
                for (root in onAllNodes(
                    isRoot(),
                ).fetchSemanticsNodes().indices) {
                    onAllNodes(isRoot())[root].captureToImage()
                }
            }
            assertTrue(ran, "a loss ran while the menu was open")
            assertTrue(onAllNodes(isRoot()).fetchSemanticsNodes().size == 2, "the menu stands in a root of its own")
        }
}
