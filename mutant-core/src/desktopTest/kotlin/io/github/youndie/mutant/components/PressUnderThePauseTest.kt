package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.LocalMutantBodyOnGpu
import io.github.youndie.mutant.surface.contrast
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantColors
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-190: under the pause (D7, widened by D24) a press shows at once a still frame at heat 1 — the rest frame's shape in
 * heat 1's amber fill, with no bubbles and no tremble — and goes with the release, in its frame. A filled button, the
 * FAB, a switch, a checkbox and the refresh tongue, pressed by a finger (the tongue by refreshing), at density 2 in both
 * themes. Every reading counts pixels over the whole stage or compares two frames of one run, never a single edge pixel.
 */
@OptIn(ExperimentalTestApi::class)
class PressUnderThePauseTest {
    private class Case(
        val name: String,
        /** The tongue boils while refreshing; every other case is pressed through its interaction source. */
        val refresh: Boolean = false,
        val content: @Composable (refreshing: Boolean, source: MutableInteractionSource) -> Unit,
    )

    private val cases =
        listOf(
            Case(
                "filled button",
            ) { _, s -> MutantButton("Воспроизвести", {}, Modifier.width(200.dp), interactionSource = s) },
            Case("FAB") { _, s -> MutantFab(MutantIcons.Home, "Домой", {}, interactionSource = s) },
            Case("switch") { _, s -> MutantSwitch(false, {}, interactionSource = s) },
            Case("checkbox") { _, s -> MutantCheckbox(false, {}, interactionSource = s) },
            Case("refresh tongue", refresh = true) { refreshing, _ ->
                MutantPullToRefresh(refreshing, {}, Modifier.size(320.dp, 140.dp)) {
                    Column { repeat(3) { BasicText("Строка $it", Modifier.size(300.dp, 30.dp)) } }
                }
            },
        )

    /** The frames of one press: at rest, one frame and 500 ms into the press, and the frame after the release. */
    private class Film(
        val rest: PixelMap,
        val pressed: PixelMap,
        val later: PixelMap,
        val released: PixelMap,
        val colors: MutantColors,
        val label: Rect?,
    )

    private fun film(
        case: Case,
        colors: MutantColors,
        reduceMotion: Boolean,
        label: String? = null,
        onGpu: Boolean = false,
    ): Film {
        var film: Film? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            var refreshing by mutableStateOf(false)
            var paused by mutableStateOf(false)
            val source = MutableInteractionSource()
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(D), LocalMutantBodyOnGpu provides onGpu) {
                    MutantTheme(colors = colors, reduceMotion = paused) {
                        Box(Modifier.size(400.dp, 200.dp).background(colors.background).testTag("stage")) {
                            Box(Modifier.padding(24.dp)) { case.content(refreshing, source) }
                        }
                    }
                }
            }
            // The clock runs a while before the pause, so it stands where the resting tick and the hot one differ and a
            // drop has let go: a press drawn at the hot tick, or holding its drops, changes the shape.
            repeat(WARM_UP) {
                mainClock.advanceTimeByFrame()
                Thread.sleep(3)
            }
            paused = reduceMotion
            repeat(SETTLE) { mainClock.advanceTimeByFrame() }
            val box = label?.let { text(it) }
            val rest = stage()
            // Through the source a clickable reports to, not a pointer: a press in a test window also takes focus, and the
            // focus brackets would be read as the press.
            val press = PressInteraction.Press(Offset.Zero)
            if (case.refresh) refreshing = true else runBlocking { source.emit(press) }
            mainClock.advanceTimeByFrame()
            val pressed = stage()
            mainClock.advanceTimeBy(HELD_MS)
            val later = stage()
            if (case.refresh) refreshing = false else runBlocking { source.emit(PressInteraction.Release(press)) }
            mainClock.advanceTimeByFrame()
            film = Film(rest, pressed, later, stage(), colors, box)
        }
        return film!!
    }

    private fun ComposeUiTest.stage() = onNodeWithTag("stage").captureToImage().toPixelMap()

    private fun ComposeUiTest.text(s: String) =
        onNodeWithText(s, useUnmergedTree = true).getBoundsInRoot().let {
            Rect(it.left.value * D, it.top.value * D, it.right.value * D, it.bottom.value * D)
        }

    private fun near(
        a: Color,
        b: Color,
        within: Float,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < within

    /**
     * Pixels of heat 1's fill: the amber the boil mixes to (`boilFill` at heat 1 is the theme's amber), or, with
     * [flicker], also that amber lifted a fifth to white, as the running boil shows it every other 333 ms.
     */
    private fun Film.amber(
        px: PixelMap,
        flicker: Boolean = false,
    ): Int {
        val a = colors.amber
        val lifted = Color(a.red + (1 - a.red) * .2f, a.green + (1 - a.green) * .2f, a.blue + (1 - a.blue) * .2f)
        return count(px) { near(it, a, AMBER_WITHIN) || (flicker && near(it, lifted, AMBER_WITHIN)) }
    }

    private fun count(
        px: PixelMap,
        what: (Color) -> Boolean,
    ): Int {
        var n = 0
        for (y in 0 until px.height) for (x in 0 until px.width) if (what(px[x, y])) n++
        return n
    }

    /** Pixels that differ at all between two frames of one run. */
    private fun differing(
        a: PixelMap,
        b: PixelMap,
    ): Int {
        var n = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (a[x, y] != b[x, y]) n++
        return n
    }

    /**
     * Pixels of the outline's ink in one frame that stand clearly off it in the other: the outline moved, as a tremble or
     * a held drop moves it. A pixel the ink only partly covers, blended with whichever fill lies under it, counts in
     * neither frame.
     */
    private fun Film.outlineMoved(
        a: PixelMap,
        b: PixelMap,
    ): Int {
        var n = 0
        val ink = colors.ink
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val ia = near(a[x, y], ink, INK_WITHIN)
                val ib = near(b[x, y], ink, INK_WITHIN)
                if ((ia && !near(b[x, y], ink, FAR_FROM_INK)) || (ib && !near(a[x, y], ink, FAR_FROM_INK))) n++
            }
        }
        return n
    }

    /** Pixels the body covers at [a] (not the stage's ground). */
    private fun Film.body(a: PixelMap) = count(a) { !near(it, colors.background, GROUND_WITHIN) }

    /**
     * Pixels the body covers in one frame and leaves to the ground in the other: its shape, whatever its fill. A pixel
     * counts only where one frame shows the ground and the other stands far from it, so an edge's anti-aliasing, which
     * blends the outline with whichever fill lies under it, is not read as the shape.
     */
    private fun Film.shapeMoved(
        a: PixelMap,
        b: PixelMap,
    ): Int {
        var n = 0
        val g = colors.background
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val ga = near(a[x, y], g, GROUND_WITHIN)
                val gb = near(b[x, y], g, GROUND_WITHIN)
                if ((ga && !near(b[x, y], g, FAR_FROM_GROUND)) || (gb && !near(a[x, y], g, FAR_FROM_GROUND))) n++
            }
        }
        return n
    }

    private val themes = listOf("Эфир" to MutantPalettes.Efir, "Копия" to MutantPalettes.Kopiya)

    @Test
    fun `under the pause a press shows heat 1's amber at once and still in the rest shape and goes on release`() {
        val report = StringBuilder()
        var bad = false
        for ((theme, colors) in themes) {
            for (case in cases) {
                val f = film(case, colors, reduceMotion = true)
                val rest = f.amber(f.rest)
                val pressed = f.amber(f.pressed)
                val still = differing(f.pressed, f.later)
                val moved = f.shapeMoved(f.rest, f.pressed)
                val outline = f.outlineMoved(f.rest, f.pressed)
                val share = moved.toDouble() / f.body(f.rest)
                val left = f.amber(f.released) - rest
                // The tongue's release ends refreshing: the done pill, not the rest picture, so only its amber is read.
                val back = if (case.refresh) 0 else differing(f.rest, f.released)
                report.append(
                    "${case.name}, $theme: amber at rest $rest, pressed $pressed; 500 ms on $still px differ; " +
                        "shape moved $moved px (${"%.2f".format(
                            share * 100,
                        )} % of the body), outline $outline px; released, " +
                        "$back px off the rest picture and $left amber px over it\n",
                )
                if (pressed - rest < MIN_AMBER || still != 0 || left > 0 || back != 0) bad = true
                // The refresh tongue is not at rest before refreshing: it has no shape to keep.
                if (!case.refresh && (share > MAX_SHAPE_MOVED || outline > MAX_OUTLINE_MOVED)) bad = true
            }
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    @Test
    fun `under the pause a pressed outlined button's label reads on heat 1's amber`() {
        val report = StringBuilder()
        var bad = false
        val case = Case("outlined button") { _, s -> MutantOutlinedButton("В очередь", {}, interactionSource = s) }
        for ((theme, colors) in themes) {
            val f = film(case, colors, reduceMotion = true, label = "В очередь")
            val box = f.label!!
            // The ground beside the label, a run of 8 px on its middle row, all heat 1's amber.
            val y = box.center.y.roundToInt()
            val x0 = (box.left - GROUND_LEFT * D).roundToInt()
            val ground = (x0 until x0 + 8).count { near(f.pressed[it, y], colors.amber, AMBER_WITHIN) }
            val c = readContrast(f.pressed, box, GROUND_LEFT * D)
            report.append("outlined button, $theme: $ground of 8 ground px amber; the label at ${"%.2f".format(c)}:1\n")
            if (ground < 8 || c < MIN_CONTRAST) bad = true
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    @Test
    fun `under the pause a press on the browser's shader path is drawn by paths as a hot body is`() {
        // D23: the shader draws resting bodies only; a pressed one keeps its paths. The same press under the pause, drawn
        // the browser's way and the desktop's, is the same picture.
        val report = StringBuilder()
        var bad = false
        for ((theme, colors) in themes) {
            // Not the switch, whose thumb, not pressed, rests on the shader; nor the tongue, which is not at rest before.
            for (case in cases.filter { !it.refresh && it.name != "switch" }) {
                val gpu = film(case, colors, reduceMotion = true, onGpu = true)
                val paths = film(case, colors, reduceMotion = true, onGpu = false)
                val off = differing(gpu.pressed, paths.pressed)
                report.append("${case.name}, $theme: pressed, the shader's way $off px off the paths'\n")
                if (off != 0) bad = true
            }
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    @Test
    fun `without the pause a press still boils over 320 ms and moves`() {
        val report = StringBuilder()
        var bad = false
        for ((theme, colors) in themes) {
            val f = film(cases[0], colors, reduceMotion = false)
            val first = f.amber(f.pressed, true) - f.amber(f.rest, true)
            val boiled = f.amber(f.later, true) - f.amber(f.rest, true)
            val cooling = f.amber(f.released, true) - f.amber(f.rest, true)
            report.append(
                "filled button, $theme: amber one frame into the press $first, 500 ms in $boiled, " +
                    "a frame after the release $cooling\n",
            )
            // The heat rises from 0, so its first frame is not yet amber; 500 ms in it is, and a release cools it over
            // 480 ms rather than at once.
            if (first > MIN_AMBER / 4 || boiled < MIN_AMBER || cooling < MIN_AMBER) bad = true
        }
        println(report)
        assertTrue(!bad, report.toString())
    }

    /** The best contrast between the label's ink and the ground beside it, as `PressedLabelContrastTest` reads it. */
    private fun readContrast(
        px: PixelMap,
        box: Rect,
        groundLeft: Float,
    ): Float {
        val ground = px[(box.left - groundLeft).roundToInt().coerceAtLeast(0), box.center.y.roundToInt()]
        var best = 1f
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                val c = contrast(px[x, y].copy(alpha = 1f), ground.copy(alpha = 1f))
                if (c > best) best = c
            }
        }
        return best
    }

    private companion object {
        const val D = 2f
        const val WARM_UP = 100
        const val SETTLE = 3
        const val GROUND_LEFT = 6f
        const val HELD_MS = 500L
        const val AMBER_WITHIN = .12f
        const val GROUND_WITHIN = .06f
        const val MIN_AMBER = 600
        const val MAX_SHAPE_MOVED = .01
        const val FAR_FROM_GROUND = .5f
        const val INK_WITHIN = .3f
        const val MAX_OUTLINE_MOVED = 20
        const val FAR_FROM_INK = 1f
        const val MIN_CONTRAST = 3f
    }
}
