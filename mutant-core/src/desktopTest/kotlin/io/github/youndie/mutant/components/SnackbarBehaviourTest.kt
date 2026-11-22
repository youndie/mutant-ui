package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-26: the snackbar's action, its ride in and out, its leaving the composition, and what a consumer does to it. */
@OptIn(ExperimentalTestApi::class)
class SnackbarBehaviourTest {
    private fun ComposeUiTest.pixels(tag: String): List<Color> =
        onNodeWithTag(tag).captureToImage().toPixelMap().let { px ->
            List(px.width * px.height) { px[it % px.width, it / px.width] }
        }

    private var visible by mutableStateOf(true)
    private var actions by mutableIntStateOf(0)
    private var hiddenCalls by mutableIntStateOf(0)

    private fun ComposeUiTest.snack(reduceMotion: Boolean = false) =
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduceMotion) {
                    Box(Modifier.testTag("host").size(328.dp, 160.dp)) {
                        MutantSnackbar(
                            "Добавлено в очередь",
                            Modifier.width(328.dp).testTag("s"),
                            visible = visible,
                            actionLabel = "ОТМЕНИТЬ",
                            onAction = { actions++ },
                            onHidden = { hiddenCalls++ },
                        )
                    }
                }
            }
        }

    /**
     * The bar's top as drawn, its ride included: the first row of the host mostly in the bar's blue (the tag stands
     * outside the layer that carries the ride, so its bounds do not move). 160 when nothing is drawn.
     */
    private fun ComposeUiTest.top(): Float {
        val px = onNodeWithTag("host").captureToImage().toPixelMap()
        val blue = MutantPalettes.Efir.noSignal
        return (0 until px.height)
            .firstOrNull { y ->
                (0 until px.width).count { x ->
                    val c = px[x, y]
                    abs(c.red - blue.red) + abs(c.green - blue.green) + abs(c.blue - blue.blue) < .06f
                } > 150
            }?.toFloat() ?: 160f
    }

    @Test
    fun `the action fires on release, a target 48 tall and at least 48 wide, and the bar is a polite live region`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            snack()
            mainClock.advanceTimeBy(400)
            onNodeWithTag("s")
                .assertHeightIsEqualTo(48.dp)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            onNodeWithText("ОТМЕНИТЬ").assertHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
            onNodeWithText("ОТМЕНИТЬ").performTouchInput { down(center) }
            waitForIdle()
            assertEquals(0, actions, "pressed, not released")
            onNodeWithText("ОТМЕНИТЬ").performTouchInput { up() }
            waitForIdle()
            assertEquals(1, actions, "released")
            val bar = onNodeWithTag("s").fetchSemanticsNode().boundsInRoot
            val word = onNodeWithText("ОТМЕНИТЬ", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(16f, bar.right - word.right, "the word 16 dp from the bar's end")
        }

    @Test
    fun `it rides in from its height and 8 dp below in 240 ms, and out in 200, then leaves and says so once`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            snack()
            mainClock.advanceTimeByFrame()
            val first = top()
            // Its height and 8 dp below its place, under its outline's row.
            assertTrue(first in 55f..60f, "entering from 56 dp below: $first")
            mainClock.advanceTimeBy(100)
            val mid = top()
            // 116 ms in is one 83 ms step of 240: eased out by the cube, (1 - 0.35)^3 of the way still to go, about 16 dp.
            assertTrue(mid in 10f..24f, "under way, eased: $mid")
            mainClock.advanceTimeBy(200)
            // At its place the first full row of blue is 1 to 3, under the outline as the running body wobbles.
            assertTrue(top() <= 4f, "at its place by 240 ms: ${top()}")
            visible = false
            mainClock.advanceTimeByFrame()
            // From the moment it starts to leave, its action no longer answers.
            onNodeWithText("ОТМЕНИТЬ").assertIsNotEnabled()
            mainClock.advanceTimeBy(100)
            assertTrue(top() > 6f, "leaving: ${top()}")
            mainClock.advanceTimeBy(200)
            assertEquals(0, onAllNodesWithText("ОТМЕНИТЬ").fetchSemanticsNodes().size, "gone, nothing composed")
            assertEquals(1, hiddenCalls, "onHidden once")
            mainClock.advanceTimeBy(500)
            assertEquals(1, hiddenCalls, "and only once")
        }

    @Test
    fun `turned back half-way, it rides back from where it stands, never jumping to the far end`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            visible = false
            snack()
            mainClock.advanceTimeBy(200)
            visible = true
            val tops = ArrayList<Float>()

            fun frames(n: Int) =
                repeat(n) {
                    mainClock.advanceTimeByFrame()
                    tops += top()
                }
            // Hidden three frames into its entrance, shown again three frames into its leaving (B-168).
            frames(3)
            visible = false
            frames(3)
            visible = true
            frames(20)
            // A whole ride's first frame moves it 11 dp (in) or 12 (out); the restart from the far end moved it 37.
            val moves = tops.zipWithNext { a, b -> b - a }
            assertTrue(moves.all { abs(it) <= 13f }, "no jump: $tops")
            // The frame of each change stands where the ride it leaves had come to; the new ride moves from there.
            assertTrue(moves.subList(3, 5).all { it > 0f }, "rides down from where it stood: $tops")
            assertTrue(moves.subList(6, 8).all { it < 0f }, "and back up: $tops")
            assertTrue(tops.last() <= 4f, "at its place: $tops")
            assertEquals(0, hiddenCalls, "it never left")
        }

    @Test
    fun `a short action is still a target at least 48 wide`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(
                        reduceMotion = true,
                    ) { MutantSnackbar("Готово", Modifier.width(328.dp), actionLabel = "Ж") }
                }
            }
            mainClock.advanceTimeByFrame()
            onNodeWithText("Ж").assertWidthIsAtLeast(48.dp).assertHeightIsEqualTo(48.dp)
        }

    @Test
    fun `composed hidden, it draws nothing and reports no leaving`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            visible = false
            snack()
            mainClock.advanceTimeBy(500)
            assertEquals(0, onAllNodesWithText("ОТМЕНИТЬ").fetchSemanticsNodes().size)
            assertEquals(0, hiddenCalls, "never shown, never left")
        }

    @Test
    fun `the bar keeps clean what the engine keeps clean, and draws no flow lines`() {
        val s =
            io.github.youndie.mutant.DesignParity
                .reference("MutantSnackbar_Rest_Efir")["slot"]!!
                .jsonObject
        val zone = (s["tz"] as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.double }
        var bar: io.github.youndie.mutant.gen.Instance? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        CompositionLocalProvider(
                            io.github.youndie.mutant.type.LocalMutantTextRaster provides
                                io.github.youndie.mutant.ChromeText,
                        ) {
                            val keep = { it: io.github.youndie.mutant.gen.Instance -> bar = it }
                            SnackbarBody(
                                "Добавлено в очередь",
                                Modifier.width(328.dp),
                                true,
                                "ОТМЕНИТЬ",
                                {},
                                {},
                                "snack",
                                probe = keep,
                            )
                        }
                    }
                }
            }
            // A running clock builds a body's first instance off the frame and draws it from the next (B-50).
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
        }
        val tz = bar!!.slot.textZone.toList()
        assertTrue(tz.zip(zone).all { (k, e) -> abs(k - e) <= 1.0 }, "kit $tz, engine $zone")
        assertTrue(!bar.slot.marks, "the page's data-marks=0")
    }

    @Test
    fun `under reduce motion it stands at its place at once and leaves at once, its body standing still`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            snack(reduceMotion = true)
            mainClock.advanceTimeByFrame()
            assertTrue(top() <= 4f, "at once: ${top()}")
            val a = pixels("s")
            repeat(2000 / 16) { mainClock.advanceTimeByFrame() }
            assertEquals(a, pixels("s"), "the bar stands")
            visible = false
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            assertEquals(0, onAllNodesWithText("ОТМЕНИТЬ").fetchSemanticsNodes().size, "gone at once")
            assertEquals(1, hiddenCalls)
        }

    @Test
    fun `keyboard focus draws the brackets around the action`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            snack(reduceMotion = true)
            mainClock.advanceTimeByFrame()
            val px0 = onNodeWithTag("s").captureToImage().toPixelMap()
            val action = onNodeWithText("ОТМЕНИТЬ").fetchSemanticsNode().boundsInRoot
            val f = MutantPalettes.Efir.focus

            fun focusAt(c: Color) = abs(c.red - f.red) + abs(c.green - f.green) + abs(c.blue - f.blue) < .05f
            assertTrue(!focusAt(px0[action.left.toInt() + 4, 1]))
            onNodeWithText("ОТМЕНИТЬ").requestFocus()
            mainClock.advanceTimeByFrame()
            val px = onNodeWithTag("s").captureToImage().toPixelMap()
            assertTrue(focusAt(px[action.left.toInt() + 4, 1]), "the bracket at the action's corner")
        }

    @Test
    fun `no width, a long message and a doubled font neither throw nor push the action out`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val long = "Добавлено в очередь после второй стороны и до конца третьей кассеты"
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 2f)) {
                    MutantTheme(reduceMotion = true) {
                        Column(Modifier.testTag("all")) {
                            MutantSnackbar(long, Modifier.width(0.dp), actionLabel = "ОТМЕНИТЬ")
                            MutantSnackbar(long, Modifier.width(300.dp).testTag("s"), actionLabel = "ОТМЕНИТЬ")
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()
            pixels("all")
            onNodeWithTag("s").assertHeightIsEqualTo(48.dp)
            val bar = onNodeWithTag("s").fetchSemanticsNode().boundsInRoot
            val word = onAllNodesWithText("ОТМЕНИТЬ", useUnmergedTree = true).fetchSemanticsNodes().last().boundsInRoot
            assertTrue(word.right <= bar.right - 16f + .5f && word.width > 0, "the action whole: $word in $bar")
            val message = onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes().last()
            assertTrue(message.boundsInRoot.right <= word.left, "the message cut before the action")
            assertTrue(message.boundsInRoot.bottom <= bar.bottom, "inside the bar")
            val layouts = mutableListOf<TextLayoutResult>()
            message.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertEquals(TextOverflow.Ellipsis, layouts.single().layoutInput.overflow)
        }
}
