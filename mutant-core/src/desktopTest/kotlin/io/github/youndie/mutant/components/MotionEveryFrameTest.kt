package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-100: by default every kind of movement — a pour, a slide, a melt, a venom run, an overlay's rise, the tabs' bar, the
 * calendar's drop — moves on every frame; with `steppedMotion` it keeps the design's steps. Counted over the first
 * 300 ms of each: the frames that differ from the one before. A resting body still changes only on its tick.
 */
@OptIn(ExperimentalTestApi::class)
class MotionEveryFrameTest {
    private class Kind(
        val name: String,
        val content: @Composable (on: Boolean) -> Unit,
    )

    private val kinds =
        listOf(
            Kind("chip pour") { on -> MutantChip("Шум", on, {}) },
            Kind("switch pour and slide") { on -> MutantSwitch(on, {}, label = "Чистый сигнал") },
            Kind("checkbox melt") { on -> MutantCheckbox(on, {}, label = "Шум головки") },
            Kind("tabs bar") { on ->
                MutantTabs(listOf(MutantTab("Дорожки"), MutantTab("Заметки"), MutantTab("Копии")), if (on) 2 else 0, {})
            },
            Kind("segmented venom") { on -> MutantSegmentedButton(listOf("SP", "LP", "EP"), if (on) 2 else 0, {}) },
            Kind("navigation pill") { on ->
                MutantNavigationBar(
                    listOf(
                        MutantNavigationItem("Эфир", MutantIcons.Home),
                        MutantNavigationItem("Очередь", MutantIcons.Schedule),
                        MutantNavigationItem("Поиск", MutantIcons.Search),
                    ),
                    if (on) 2 else 0,
                    {},
                    windowInsets = WindowInsets(0),
                )
            },
            Kind("date drop") { on -> MutantDatePicker(2026, 11, if (on) 18 else 5, {}, { _, _ -> }) },
            Kind("dialog rise") { on -> MutantDialog(on, "Стереть запись?", {}) { MutantDialogAction("Да", {}) } },
            Kind("sheet rise") { on ->
                MutantBottomSheet(on, {}, windowInsets = WindowInsets(0)) { MutantButton("Готово", {}) }
            },
            Kind("drawer slide") { on ->
                MutantNavigationDrawer(on, {}, listOf(MutantDrawerItem("CH 01", "Эфир")), 0, {})
            },
            Kind("snackbar ride") { on ->
                Box(Modifier.fillMaxWidth().padding(top = 300.dp)) { MutantSnackbar("Добавлено", visible = on) }
            },
        )

    private fun BufferedImage.same(other: BufferedImage): Boolean {
        for (y in 0 until height) for (x in 0 until width) if (getRGB(x, y) != other.getRGB(x, y)) return false
        return true
    }

    /** The frames of the first 300 ms after the change that differ from the frame before. */
    private fun changes(
        kind: Kind,
        stepped: Boolean,
    ): Int {
        var changed = 0
        runComposeUiTest {
            mainClock.autoAdvance = false
            var on by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false, steppedMotion = stepped) {
                        MutantScreenClock {
                            Box(
                                Modifier.size(360.dp, 420.dp).background(MutantTheme.colors.background).testTag("s"),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                Box(Modifier.padding(16.dp)) { kind.content(on) }
                            }
                        }
                    }
                }
            }
            repeat(60) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(2)
            }
            on = true
            var previous = onNodeWithTag("s").captureToImage().toAwtImage()
            repeat(19) {
                mainClock.advanceTimeByFrame()
                val now = onNodeWithTag("s").captureToImage().toAwtImage()
                if (!now.same(previous)) changed++
                previous = now
            }
        }
        return changed
    }

    @Test
    fun `every kind of movement moves on every frame by default and steps with the design's steps`() {
        val failures = ArrayList<String>()
        val table = StringBuilder()
        for (kind in kinds) {
            val free = changes(kind, stepped = false)
            val steps = changes(kind, stepped = true)
            table.append("${kind.name}: $free frames changed on every frame, $steps stepped\n")
            if (free < FREE_AT_LEAST) failures += "${kind.name}: only $free of 19 frames changed by default"
            if (steps >
                STEPPED_AT_MOST
            ) {
                failures += "${kind.name}: $steps of 19 frames changed under the design's steps"
            }
        }
        println("MOTION\n$table")
        assertTrue(failures.isEmpty(), failures.joinToString("\n") + "\n" + table)
    }

    @Test
    fun `a resting body changes only on its tick`() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme(reduceMotion = false) {
                        MutantScreenClock {
                            Box(Modifier.size(360.dp, 200.dp).background(MutantTheme.colors.background).testTag("s")) {
                                MutantCard(Modifier.padding(16.dp).fillMaxWidth(), key = "rest") {
                                    MutantCardTitle("Без подписи №3")
                                }
                            }
                        }
                    }
                }
            }
            repeat(60) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(2)
            }
            var previous = onNodeWithTag("s").captureToImage().toAwtImage()
            var changed = 0
            // A second: 62 frames, eight resting ticks.
            repeat(62) {
                mainClock.advanceTimeByFrame()
                val now = onNodeWithTag("s").captureToImage().toAwtImage()
                if (!now.same(previous)) changed++
                previous = now
            }
            assertTrue(changed in 1..9, "a resting card changed on $changed of 62 frames, its ticks are 8")
        }
    }

    private companion object {
        /** Of the 19 frames in 300 ms: moving on every frame, most change; stepped, three steps and two resting ticks. */
        const val FREE_AT_LEAST = 12
        const val STEPPED_AT_MOST = 7
    }
}
