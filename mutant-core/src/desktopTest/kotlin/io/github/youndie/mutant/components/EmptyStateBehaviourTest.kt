package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-29: the empty state's spacing and centring, its six illustrations, the pause, and what a consumer gives it. */
@OptIn(ExperimentalTestApi::class)
class EmptyStateBehaviourTest {
    private fun ComposeUiTest.captureToImage(node: androidx.compose.ui.semantics.SemanticsNode) =
        onAllNodesWithText(node.config[SemanticsProperties.Text].first().text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .indexOfFirst { it.id == node.id }
            .let {
                onAllNodesWithText(
                    node.config[SemanticsProperties.Text].first().text,
                    useUnmergedTree = true,
                )[it].captureToImage()
            }

    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    @Test
    fun `the title stands 44 under the art and the text 6 under it, centred, an action 20 under that`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = true) {
                        MutantEmptyState(
                            MutantIllustrationKind.Cassette,
                            "Здесь пока пусто",
                            Modifier.width(328.dp).testTag("e"),
                            text = "Пусто",
                            action = { MutantButton("Записать", {}, Modifier.testTag("a")) },
                        )
                    }
                }
            }
            val e = onNodeWithTag("e").fetchSemanticsNode().boundsInRoot
            val title = onNodeWithText("Здесь пока пусто", useUnmergedTree = true).fetchSemanticsNode()
            val text = onNodeWithText("Пусто", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val action = onNodeWithTag("a").fetchSemanticsNode().boundsInRoot
            assertEquals(132f + 44f, title.boundsInRoot.top - e.top, "44 under the 132 dp cassette")
            assertEquals(6f, text.top - title.boundsInRoot.bottom, "6 under the title")
            assertEquals(20f, action.top - text.bottom, "the action 20 under the text")
            for (b in listOf(title.boundsInRoot, text, action)) {
                assertTrue(abs((b.left + b.right) / 2 - (e.left + e.right) / 2) <= .5f, "centred: $b in $e")
            }
            assertTrue(SemanticsProperties.Heading in title.config, "the title is a heading")
        }

    @Test
    fun `each of the six illustrations stands at its own size above the words`() {
        val heights =
            mapOf(
                MutantIllustrationKind.Cassette to 132f,
                MutantIllustrationKind.Television to 176f,
                MutantIllustrationKind.Antenna to 190f,
                MutantIllustrationKind.Floppy to 160f,
                MutantIllustrationKind.Remote to 176f,
                MutantIllustrationKind.TestCard to 156f,
            )
        // One at a time: six of them stand taller than the test's window.
        for (kind in MutantIllustrationKind.entries) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(reduceMotion = true) { MutantEmptyState(kind, "Заголовок", Modifier.width(300.dp)) }
                    }
                }
                val title = onNodeWithText("Заголовок", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertEquals(heights.getValue(kind) + 44f, title.top, "$kind: its art, then 44")
            }
        }
    }

    private fun frames(reduceMotion: Boolean): Boolean {
        var changed = false
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = reduceMotion) {
                    MutantEmptyState(MutantIllustrationKind.Cassette, "Здесь пока пусто", Modifier.testTag("e"))
                }
            }
            mainClock.advanceTimeByFrame()
            val a = pixels("e")
            repeat(2000 / 16) { mainClock.advanceTimeByFrame() }
            changed = a != pixels("e")
        }
        return changed
    }

    @Test
    fun `reduce motion is the pause - the art stands, and turns its reels while the clock runs`() {
        assertTrue(frames(reduceMotion = false), "running")
        assertTrue(!frames(reduceMotion = true), "paused")
    }

    @Test
    fun `no width, a long title and a doubled font neither throw nor leave the width`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Здесь пока пусто, и кассета не записана ни с одной стороны"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme(reduceMotion = true) {
                        Column(Modifier.testTag("all")) {
                            MutantEmptyState(MutantIllustrationKind.Remote, long, Modifier.width(0.dp), text = long)
                            MutantEmptyState(
                                MutantIllustrationKind.Remote,
                                long,
                                Modifier.width(240.dp).testTag("e"),
                                text = long,
                            )
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            pixels("all")
            val e = onNodeWithTag("e").fetchSemanticsNode().boundsInRoot
            for (node in onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                if (b.top < e.top) continue
                assertTrue(b.left >= e.left - .5f && b.right <= e.right + .5f, "wrapped inside: $b in $e")
            }
            // Wrapped, each line stands centred: the ink leaves as much room on the left as on the right.
            val inside =
                onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes().filter {
                    it.boundsInRoot.top >=
                        e.top
                }
            for (node in inside.filter { it.boundsInRoot.width > 0 }) {
                val px = captureToImage(node).toPixelMap()
                val inked = (0 until px.width).filter { x -> (0 until px.height).any { y -> px[x, y].alpha > .5f } }
                val left = inked.first()
                val right = px.width - 1 - inked.last()
                assertTrue(abs(left - right) <= 2, "centred lines: $left on the left, $right on the right")
            }
        }
}
