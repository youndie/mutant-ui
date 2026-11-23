package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.clock.LocalMutantClock
import io.github.youndie.mutant.clock.MutantClock
import io.github.youndie.mutant.gen.FieldCache
import io.github.youndie.mutant.gen.MutantGenerator
import io.github.youndie.mutant.gen.REST_BOIL
import io.github.youndie.mutant.gen.Slot
import io.github.youndie.mutant.gen.frame
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.HOVER_HEAT
import io.github.youndie.mutant.surface.HeatMachine
import io.github.youndie.mutant.surface.contrast
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantBody
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-193 (the designer's answer 16): a hover does not change the fill. Its look is only a stronger tremble of the outline,
 * as heat 0.25 draws it, with no amber mix and no bubbles, for every body that hears the mouse; under the pause it shows
 * that tremble at once and still, at the resting tick's time (D24, F-checks-switch-05). Pixel readings count over the
 * whole stage or a text's box, never a single edge pixel.
 */
@OptIn(ExperimentalTestApi::class)
class HoverKeepsTheFillTest {
    private val bodies: List<Pair<String, @Composable () -> Unit>> =
        listOf(
            "Button" to { MutantButton("Пуск", {}) },
            "OutlinedButton" to { MutantOutlinedButton("Пуск", {}) },
            "IconButton" to { MutantIconButton(MutantIcons.Search, "Поиск", {}) },
            "Fab" to { MutantFab(MutantIcons.Add, "Добавить", {}) },
            "Chip" to { MutantChip("Чип", false, {}) },
            "Checkbox" to { MutantCheckbox(false, {}) },
            "RadioButton" to { MutantRadioButton(false, {}) },
            "Switch" to { MutantSwitch(false, {}) },
            "Card" to { card() },
            "ListItem" to {
                MutantListItem("Строка", Modifier.width(260.dp), leadingIcon = MutantIcons.Home, onClick = {})
            },
            "Dropdown" to { MutantDropdown("Настройка", listOf("ТРЕКИНГ"), 0, {}) },
            "Carousel" to { MutantCarousel(listOf(MutantCarouselItem("Эфир")), Modifier.width(260.dp)) },
        )

    @Composable
    private fun card() =
        MutantCard(Modifier.width(260.dp), onClick = {}) {
            MutantCardTitle("Ночной эфир")
            MutantCardSubtitle(SUBTITLE)
        }

    private val themes = listOf("Эфир" to MutantPalettes.Efir, "Копия" to MutantPalettes.Kopiya)

    private class Stage {
        var clock: MutantClock? = null
    }

    private fun ComposeUiTest.stage(
        colors: MutantColors,
        content: @Composable () -> Unit,
    ): Stage {
        val st = Stage()
        var paused by mutableStateOf(false)
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(D)) {
                MutantTheme(colors = colors, reduceMotion = paused) {
                    st.clock = LocalMutantClock.current
                    Box(Modifier.size(320.dp, 160.dp).background(colors.background).testTag("stage")) {
                        Box(Modifier.padding(24.dp).testTag("body")) { content() }
                    }
                }
            }
        }
        pause = { paused = it }
        return st
    }

    private var pause: (Boolean) -> Unit = {}

    private fun ComposeUiTest.shot() = onNodeWithTag("stage").captureToImage().toPixelMap()

    private fun ComposeUiTest.hover() = onNodeWithTag("body").performMouseInput { moveTo(center) }

    private fun ComposeUiTest.leave() = onNodeWithTag("stage").performMouseInput { moveTo(Offset(-100f, -100f)) }

    private fun near(
        a: Color,
        b: Color,
        within: Float = NEAR,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < within

    private fun count(
        px: PixelMap,
        what: (Color) -> Boolean,
    ): Int {
        var n = 0
        for (y in 0 until px.height) for (x in 0 until px.width) if (what(px[x, y])) n++
        return n
    }

    /** The body's fill: the commonest colour on the stage that is not its ground. */
    private fun fillOf(
        px: PixelMap,
        ground: Color,
    ): Color {
        val seen = HashMap<Color, Int>()
        for (y in 0 until px.height) {
            for (x in 0 until px.width) {
                val c = px[x, y]
                if (!near(c, ground, GROUND_WITHIN)) seen[c] = (seen[c] ?: 0) + 1
            }
        }
        return seen.maxBy { it.value }.key
    }

    /** Pixels that differ at all between two frames. */
    private fun differing(
        a: PixelMap,
        b: PixelMap,
    ): Int {
        var n = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (a[x, y] != b[x, y]) n++
        return n
    }

    @Test
    fun `every hovering body keeps its fill under the mouse and only its outline trembles harder`() {
        val failures = mutableListOf<String>()
        for ((name, content) in bodies) {
            for ((theme, colors) in themes) {
                runComposeUiTest {
                    stage(colors, content)
                    mainClock.advanceTimeBy(WARM_MS)
                    val rest = shot()
                    val fill = fillOf(rest, colors.background)
                    hover()
                    mainClock.advanceTimeBy(HOVERED_MS)
                    val hovered = shot()
                    // A second hovered frame a hot tick on, to see the tremble move rather than one frame's phase.
                    mainClock.advanceTimeBy(HOT_TICK_FRAMES_MS)
                    val hovered2 = shot()
                    val atRest = count(rest) { near(it, fill) }
                    val keptA = count(hovered) { near(it, fill) }
                    val keptB = count(hovered2) { near(it, fill) }
                    val amber = count(hovered) { near(it, colors.amber) } - count(rest) { near(it, colors.amber) }
                    val moved = differing(rest, hovered)
                    val case = "$name/$theme"
                    if (minOf(keptA, keptB) < atRest * KEPT) {
                        failures += "$case: fill pixels at rest $atRest, hovered $keptA and $keptB"
                    }
                    if (amber > atRest * AMBER_SHARE) failures += "$case: $amber more amber pixels hovered"
                    if (moved < MOVED_AT_LEAST) failures += "$case: $moved pixels changed hovered"
                }
            }
        }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun `a hovered card's subtitle reads the contrast it reads at rest`() {
        for ((theme, colors) in themes) {
            runComposeUiTest {
                stage(colors) { card() }
                mainClock.advanceTimeBy(WARM_MS)
                val box =
                    onNodeWithText(SUBTITLE, useUnmergedTree = true).getBoundsInRoot().let {
                        listOf(it.left.value, it.top.value, it.right.value, it.bottom.value).map { v ->
                            (v * D).toInt()
                        }
                    }
                val rest = contrastIn(onNodeWithTag("stage").captureToImage().toPixelMap(), box)
                hover()
                // Through the whole rise of the hover's heat and well past it, at every hot tick.
                var worst = Float.MAX_VALUE
                repeat(SUBTITLE_FRAMES) {
                    mainClock.advanceTimeBy(HOT_TICK_FRAMES_MS)
                    worst = minOf(worst, contrastIn(onNodeWithTag("stage").captureToImage().toPixelMap(), box))
                }
                assertTrue(
                    worst >= rest * (1 - CONTRAST_MARGIN),
                    "$theme: the subtitle reads $rest:1 at rest and $worst:1 at worst under the mouse",
                )
            }
        }
    }

    /**
     * The contrast of a line of words against its ground inside [box] (left, top, right, bottom, px): the ground is the
     * box's commonest colour, the ink its pixel that stands furthest from it.
     */
    private fun contrastIn(
        px: PixelMap,
        box: List<Int>,
    ): Float {
        val seen = HashMap<Color, Int>()
        for (y in box[1] until box[3]) for (x in box[0] until box[2]) seen[px[x, y]] = (seen[px[x, y]] ?: 0) + 1
        val ground = seen.maxBy { it.value }.key
        return seen.keys.maxOf { contrast(it, ground) }
    }

    @Test
    fun `under the pause a hover shows its finished tremble at once, still, at the resting tick's time`() {
        val failures = mutableListOf<String>()
        for ((name, content) in bodies) {
            for ((theme, colors) in themes) {
                // Two runs whose clock stands in the same resting tick and in different hot ticks: a body drawn at the
                // resting tick's time draws them alike.
                val runs =
                    listOf(EARLY_IN_THE_TICK, LATE_IN_THE_TICK).map { share ->
                        var r: PausedRun? = null
                        runComposeUiTest { r = pausedRun(colors, content, share) }
                        r!!
                    }
                val (a, b) = runs
                val case = "$name/$theme"
                if (a.restTick != b.restTick || a.hotTick == b.hotTick) {
                    failures +=
                        "$case: the clock stood at ticks ${a.restTick}/${a.hotTick} and ${b.restTick}/${b.hotTick}"
                    continue
                }
                if (differing(a.rest, b.rest) != 0) failures += "$case: the two runs differ at rest"
                for (run in runs) {
                    if (differing(run.rest, run.hovered) < MOVED_AT_LEAST) {
                        failures += "$case: no tremble shown under the pause"
                    }
                    if (differing(run.hovered, run.later) != 0) failures += "$case: the hover runs under the pause"
                    if (count(run.hovered) { near(it, colors.amber) } > count(run.rest) { near(it, colors.amber) }) {
                        failures += "$case: amber under the pause"
                    }
                    if (differing(run.rest, run.left) != 0) failures += "$case: the leave does not land at once"
                }
                val apart = differing(a.hovered, b.hovered)
                if (apart != 0) failures += "$case: hovered under the pause, the two runs differ by $apart pixels"
            }
        }
        assertEquals(emptyList(), failures)
    }

    private class PausedRun(
        val restTick: Long,
        val hotTick: Long,
        val rest: PixelMap,
        val hovered: PixelMap,
        val later: PixelMap,
        val left: PixelMap,
    )

    private fun ComposeUiTest.pausedRun(
        colors: MutantColors,
        content: @Composable () -> Unit,
        share: Double,
    ): PausedRun {
        val st = stage(colors, content)
        mainClock.advanceTimeByFrame()
        val clock = st.clock!!
        val at = (PAUSE_TICK + share) * clock.stepMs
        while (clock.nowMs < at) {
            mainClock.advanceTimeByFrame()
            Thread.sleep(2)
        }
        pause(true)
        repeat(SETTLE_FRAMES) { mainClock.advanceTimeByFrame() }
        val rest = shot()
        hover()
        mainClock.advanceTimeByFrame()
        val hovered = shot()
        mainClock.advanceTimeBy(HOVERED_MS)
        val later = shot()
        leave()
        mainClock.advanceTimeByFrame()
        return PausedRun(clock.restTick, clock.hotTick, rest, hovered, later, shot())
    }

    @Test
    fun `the hover's heat trembles the outline alone, rises to a quarter and lands at once under the pause`() {
        val heat = HeatMachine()
        assertEquals(.25, HOVER_HEAT)
        heat.hover(1_000.0)
        assertEquals(0.0, heat.at(2_000.0), "a hover heats the fill")
        assertEquals(HOVER_HEAT, heat.trembleAt(2_000.0))
        heat.leave(2_000.0)
        assertEquals(0.0, heat.trembleAt(2_200.0))
        // Under the pause the hover's tremble comes and goes in the frame.
        heat.hold(true, 3_000.0)
        heat.hover(3_000.0)
        assertEquals(HOVER_HEAT, heat.trembleAt(3_000.0))
        heat.leave(3_000.0)
        assertEquals(0.0, heat.trembleAt(3_000.0))
        heat.hover(3_000.0)
        heat.hold(false, 3_000.0)
        assertEquals(HOVER_HEAT, heat.trembleAt(3_016.0), "the pause lifted replays the hover's rise")
        // A press boils past the hover, and cools back to the hover's tremble.
        heat.press(4_000.0)
        assertEquals(1.0, heat.trembleAt(5_000.0))
        heat.release(5_000.0)
        assertEquals(0.0, heat.at(6_000.0))
        assertEquals(HOVER_HEAT, heat.trembleAt(6_000.0))
    }

    @Test
    fun `a frame at the hover's heat trembles as heat 0_25 with no bubbles and no held drops`() {
        val gen = MutantGenerator(cassette = "5")
        val p = MutantBody.Card.profile
        val slot =
            Slot(
                ox = 20.0,
                oy = 30.0,
                w = 260.0,
                h = 80.0,
                r = minOf(p.radius, 40.0),
                over = p.overhang,
                dripShare = p.dripShare,
                wobble = p.wobble,
                round = p.round,
                calm = 1.0,
                dead = false,
                marks = p.marks,
                edge = p.mosaic,
                textZone = doubleArrayOf(16.0, 14.0, 120.0, 40.0),
                cleanZone = doubleArrayOf(14.0, 12.0, 124.0, 44.0),
            )
        val g = gen.instance(slot, "b193/card", false)
        var engineDiffers = 0
        // Over many hot ticks, so a drop letting go and the bubbles' timing are both met.
        for (i in 0 until 96) {
            val t = 1_000.0 + i * HOT_TICK_MS
            val rest = gen.frame(g, FieldCache(), t, 0.0)
            val hovered = gen.frame(g, FieldCache(), t, HOVER_HEAT, pressHeat = 0.0)
            val engine = gen.frame(g, FieldCache(), t, HOVER_HEAT)
            assertEquals(REST_BOIL * 1.5 + .15, hovered.boil, 1e-9, "the tremble's strength at $t")
            assertEquals(engine.boil, hovered.boil, "the tremble at $t")
            assertEquals(engine.boilPhase, hovered.boilPhase, "the tremble's phase at $t")
            assertEquals(
                rest.primitives.map { it.toList() },
                hovered.primitives.map { it.toList() },
                "bubbles or held drops at $t",
            )
            if (engine.primitives.map { it.toList() } != rest.primitives.map { it.toList() }) engineDiffers++
        }
        assertTrue(engineDiffers > 0, "the engine's heat 0.25 adds no bubble and holds no drop: the control is blind")
    }

    private companion object {
        const val D = 2f
        const val SUBTITLE = "Канал 03 · 02:14"
        const val WARM_MS = 2_000L
        const val HOVERED_MS = 400L
        const val HOT_TICK_MS = 1000.0 / 8 / 1.5

        /** A hot tick, in the test clock's whole frames. */
        const val HOT_TICK_FRAMES_MS = 96L
        const val SUBTITLE_FRAMES = 12
        const val NEAR = .04f
        const val GROUND_WITHIN = .04f

        /** The share of the rest fill's pixels a hovered frame keeps: the tremble moves only the edge. */
        const val KEPT = .85

        /** More amber pixels hovered than at rest, as a share of the fill's: heat 0.3 turned most of the fill. */
        const val AMBER_SHARE = .02

        /** Pixels a hover changes, at least: its tremble moves the outline (its strength is held by the frame's test). */
        const val MOVED_AT_LEAST = 20

        /** The subtitle's contrast hovered, as a share below its contrast at rest: 3.22 against 6.9 before. */
        const val CONTRAST_MARGIN = .03f

        /** The resting tick the clock stands in, and where in it: before the hot tick's boundary at 2/3, and after. */
        const val PAUSE_TICK = 8
        const val EARLY_IN_THE_TICK = .05
        const val LATE_IN_THE_TICK = .7
        const val SETTLE_FRAMES = 4
    }
}
