package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.MutantAppear
import io.github.youndie.mutant.surface.MutantScreen
import io.github.youndie.mutant.theme.MutantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-206 (the designer's answer 38): a body's default key is its place in the composition and its body type. Read in
 * pixels under reduce motion, where the clock stands and a body changes only when its key or its size does. Density 1.
 * A body against another body counts in thousands of pixels; a body against itself in none, or in the few its own words
 * change.
 */
@OptIn(ExperimentalTestApi::class)
class DefaultKeysByPlaceTest {
    private fun ComposeUiTest.shot(tag: String): PixelMap = onNodeWithTag(tag).captureToImage().toPixelMap()

    private fun differ(
        a: PixelMap,
        b: PixelMap,
        skip: (Int, Int) -> Boolean = { _, _ -> false },
    ): Int {
        var n = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) if (!skip(x, y) && a[x, y] != b[x, y]) n++
        }
        return n
    }

    private fun ComposeUiTest.still(content: @androidx.compose.runtime.Composable () -> Unit) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = true) { content() }
            }
        }
        mainClock.advanceTimeBy(FRAMES)
    }

    @Test
    fun `two unkeyed cards side by side are two bodies, and each keeps its own as it recomposes`() =
        runComposeUiTest {
            var tick by mutableIntStateOf(0)
            var branch by mutableStateOf(false)
            still {
                Column {
                    // A branch appearing before the cards: their places in the composition stay.
                    if (branch) Spacer(Modifier.height(4.dp))
                    Row {
                        Box(Modifier.padding(12.dp).testTag("a")) {
                            // A new click handler each tick: the card itself recomposes.
                            MutantCard(
                                Modifier.width(150.dp),
                                onClick = { tick.hashCode() },
                            ) { MutantCardTitle("Кассета") }
                        }
                        Box(Modifier.padding(12.dp).testTag("b")) {
                            MutantCard(Modifier.width(150.dp)) { MutantCardTitle("Кассета") }
                        }
                    }
                }
            }
            val a = shot("a")
            val b = shot("b")
            val apart = differ(a, b)
            // About 4 000 of 12 000 measured; 0 when both were `card`.
            assertTrue(apart > CLONE_FLOOR, "two unkeyed cards differ in $apart px, a clone in none")
            tick++
            branch = true
            mainClock.advanceTimeBy(FRAMES)
            val again = differ(a, shot("a"))
            println("B-206 cards: apart $apart px, recomposed $again px")
            assertTrue(again < SAME_CEILING, "the card recomposed under a new branch moved $again px")
        }

    @Test
    fun `a card is its place's body, through a move by movable content, a screen shown again and an appearance`() =
        runComposeUiTest {
            var screen by mutableStateOf("a")
            var left by mutableStateOf(true)
            var tick by mutableIntStateOf(0)
            var shown by mutableStateOf(true)
            still {
                val card =
                    remember {
                        movableContentOf { n: Int ->
                            MutantCard(
                                Modifier.width(150.dp),
                                onClick = { n.hashCode() },
                            ) { MutantCardTitle("Кассета") }
                        }
                    }
                Column {
                    Box(Modifier.padding(12.dp).testTag("screen")) {
                        MutantScreen(screen) { MutantCard(Modifier.width(150.dp)) { MutantCardTitle("Кассета") } }
                    }
                    if (left) {
                        Box(Modifier.padding(12.dp).testTag("moved")) { card(tick) }
                    } else {
                        Row { Box(Modifier.padding(12.dp).testTag("moved")) { card(tick) } }
                    }
                    Box(Modifier.padding(12.dp).size(174.dp, 104.dp).testTag("appear")) {
                        MutantAppear(shown) { MutantCard(Modifier.width(150.dp)) { MutantCardTitle("Кассета") } }
                    }
                }
            }
            val onA = shot("screen")
            val moved = shot("moved")
            val appeared = shot("appear")
            // Two places, two bodies: the screen's card and the movable one.
            val places = differ(onA, moved)
            assertTrue(places > CLONE_FLOOR, "the screen's card and the movable one differ in $places px")
            screen = "b"
            mainClock.advanceTimeBy(FRAMES)
            val onB = differ(onA, shot("screen"))
            assertTrue(onB > CLONE_FLOOR, "another screen's card differs in $onB px")
            screen = "a"
            mainClock.advanceTimeBy(FRAMES)
            val back = differ(onA, shot("screen"))
            assertTrue(back < SAME_CEILING, "the first screen shown again moved its card $back px")
            left = false
            mainClock.advanceTimeBy(FRAMES)
            tick++
            mainClock.advanceTimeBy(FRAMES)
            val after = differ(moved, shot("moved"))
            assertTrue(after < SAME_CEILING, "the card moved and recomposed changed $after px")
            // Under reduce motion it leaves at once and comes back whole, composed anew at the same place.
            shown = false
            mainClock.advanceTimeBy(FRAMES)
            shown = true
            mainClock.advanceTimeBy(FRAMES)
            val reappeared = differ(appeared, shot("appear"))
            println("B-206 card: screen b $onB px, a again $back px, moved $after px, appeared again $reappeared px")
            assertTrue(reappeared < SAME_CEILING, "the card appearing again changed $reappeared px")
        }

    @Test
    fun `an icon button whose icon and description change keeps its body`() =
        runComposeUiTest {
            var playing by mutableStateOf(false)
            still {
                Box(Modifier.size(96.dp).testTag("i")) {
                    Box(Modifier.offset(24.dp, 24.dp)) {
                        MutantIconButton(
                            if (playing) MutantIcons.Close else MutantIcons.Play,
                            if (playing) "Стоп" else "Играть",
                            { playing = !playing },
                        )
                    }
                }
            }
            val play = shot("i")
            playing = true
            mainClock.advanceTimeBy(FRAMES)
            val stop = shot("i")
            // The 24 dp icon stands at 36…60 in the 96 dp box; everything else is the body.
            val icon = 32 until 64
            val iconChanged = differ(play, stop)
            val body = differ(play, stop) { x, y -> x in icon && y in icon }
            println("B-206 icon button: icon $iconChanged px, body $body px")
            assertTrue(iconChanged > ICON_FLOOR, "the icon changed in $iconChanged px")
            // About 1 000 px outside the icon when the key was the description; none now.
            assertTrue(body < SAME_CEILING, "the body around the icon moved $body px")
        }

    @Test
    fun `a tooltip whose text runs keeps its plate`() =
        runComposeUiTest {
            var second by mutableIntStateOf(17)
            still {
                Box(Modifier.size(400.dp, 300.dp)) {
                    MutantTooltipBox("REC · 0:42:$second", Modifier.offset(150.dp, 160.dp).testTag("anchor")) {
                        MutantIconButton(MutantIcons.Play, "Запись", {}, key = "rec")
                    }
                }
            }
            onNodeWithTag("anchor").performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(FRAMES)
            val roots = onAllNodes(isRoot())

            fun plate() = roots[roots.fetchSemanticsNodes().size - 1].captureToImage().toPixelMap()
            var last = plate()
            for (s in 18..20) {
                second = s
                mainClock.advanceTimeBy(FRAMES)
                val now = plate()
                val moved = differ(last, now)
                println("B-206 tooltip 0:42:$s: $moved px")
                // The digits alone: 10…64 px measured; about 2 000 when the key was the text.
                assertTrue(moved in 1 until TEXT_CEILING, "0:42:$s changed $moved px of the plate's window")
                last = now
            }
        }

    @Test
    fun `the dialog's window takes a key, and two unkeyed dialogs are two windows`() {
        // The window tunes in as a mosaic drawn from its key (B-179): read 50 ms after it opens, on the first step.
        fun windows(
            first: String?,
            second: String?,
        ): Int {
            var out = 0
            runComposeUiTest {
                mainClock.autoAdvance = false
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(reduceMotion = false) {
                            Row {
                                for ((tag, key) in listOf("d1" to first, "d2" to second)) {
                                    Box(Modifier.size(320.dp, 240.dp).testTag(tag)) {
                                        if (key == null) {
                                            MutantDialog(true, "Стереть?", {}) { MutantDialogAction("Отмена", {}) }
                                        } else {
                                            MutantDialog(
                                                true,
                                                "Стереть?",
                                                {},
                                                key = key,
                                            ) { MutantDialogAction("Отмена", {}) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                mainClock.advanceTimeBy(48)
                out = differ(shot("d1"), shot("d2"))
            }
            return out
        }
        val same = windows("w", "w")
        val two = windows("w", "v")
        val unkeyed = windows(null, null)
        println("B-206 dialog windows: one key $same px, two keys $two px, unkeyed $unkeyed px")
        assertTrue(same < SAME_CEILING, "one key, two windows: $same px apart")
        assertTrue(two > CLONE_FLOOR, "two keys, two windows: $two px apart")
        assertTrue(unkeyed > CLONE_FLOOR, "two unkeyed dialogs: $unkeyed px apart")
    }

    private companion object {
        /** Three frames: what changed has drawn. */
        const val FRAMES = 48L

        /** Fewer pixels than this between two bodies would be a clone: two bodies measured 2 600…4 500. */
        const val CLONE_FLOOR = 1_000

        /** More than this between two renders of one body would be another body: measured 0. */
        const val SAME_CEILING = 100

        /** More than this when a tooltip's digits change would be another plate: measured 10…64, against ~2 000. */
        const val TEXT_CEILING = 400

        /** The icon's own change, Play to Close: measured 214 px. */
        const val ICON_FLOOR = 60
    }
}
