package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-105: when refreshing ends the long tongue becomes the pill frame by frame, with no jump in the hanging body's size,
 * and after its second the pill is drawn back into the line, narrowing to nothing, instead of standing whole until the
 * line cuts it. The body is read from the pixels: its slime, lime or boiled amber, above the list's first row, and the
 * rows wide enough to be the body proper rather than its drips.
 */
@OptIn(ExperimentalTestApi::class)
class PullToRefreshMorphTest {
    /** The hanging body's box, dp at density 1, or null when none of it shows. */
    private class Box2(
        val w: Int,
        val h: Int,
        val top: Int,
    )

    private fun ComposeUiTest.body(): Box2? {
        val img = onNodeWithTag("s").captureToImage().toAwtImage()
        // Per row, the slime's span: blue low and red or green over the page's, lime or amber, faded or not; the page,
        // the line, the outline and the words are not. A faded tongue still counts while any of it shows: read only at
        // full strength, the boiled tongue fading over the pill dropped out of the reading at once, 10 dp in a frame
        // that draws no jump (B-168). A row of the body proper has at least [SOLID] of it: the drips under it, a few dp
        // wide each, step on the hot tick as every drip of the kit does, and are not the body's size.
        val page = MutantPalettes.Efir.background.toArgb()
        var top = -1
        var bottom = -1
        var w = 0
        for (y in 0 until TOP_ROWS) {
            var x0 = Int.MAX_VALUE
            var x1 = -1
            var n = 0
            for (x in 0 until img.width) {
                val p = img.getRGB(x, y)
                val warm = (p shr 16 and 255) - (page shr 16 and 255) > FAINT
                val green = (p shr 8 and 255) - (page shr 8 and 255) > FAINT
                if ((p and 255) < 110 && (warm || green)) {
                    n++
                    if (x < x0) x0 = x
                    if (x > x1) x1 = x
                }
            }
            if (n >= SOLID) {
                if (top < 0) top = y
                bottom = y
                w = maxOf(w, x1 - x0 + 1)
            }
        }
        return if (top < 0) null else Box2(w, bottom - top + 1, top)
    }

    private fun run(
        composedRefreshing: Boolean = true,
        block: ComposeUiTest.(set: (Boolean) -> Unit) -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        var refreshing by mutableStateOf(composedRefreshing)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme {
                    MutantScreenClock {
                        Box(Modifier.size(300.dp, 200.dp).background(MutantTheme.colors.background).testTag("s")) {
                            MutantPullToRefresh(
                                refreshing,
                                {},
                                Modifier.fillMaxSize(),
                                refreshingLabel = "REFRESHING",
                                doneLabel = "REFRESHED",
                            ) {
                                Column { repeat(3) { MutantListItem("Untitled ${it + 1}", key = "r$it") } }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_500)
        block { refreshing = it }
    }

    @Test
    fun `the long tongue becomes the pill with no jump in its size`() =
        run { set ->
            val before = body() ?: error("no tongue while refreshing")
            set(false)
            var last = before
            val steps = ArrayList<String>()
            repeat(30) {
                mainClock.advanceTimeByFrame()
                val now = body() ?: error("the body is gone at ${mainClock.currentTime} ms")
                val dw = abs(now.w - last.w)
                val dh = abs(now.h - last.h)
                if (dw > MAX_STEP ||
                    dh > MAX_STEP
                ) {
                    steps += "${mainClock.currentTime} ms: ${last.w}×${last.h} → ${now.w}×${now.h}"
                }
                last = now
            }
            assertTrue(before.w > last.w + 10, "it narrowed from the tongue's ${before.w} to the pill's ${last.w}")
            assertTrue(
                steps.isEmpty(),
                "frames where the body jumped more than $MAX_STEP dp:\n" + steps.joinToString("\n"),
            )
        }

    @Test
    fun `after its second the pill narrows into the line and is gone`() =
        run { set ->
            set(false)
            mainClock.advanceTimeBy(DONE_MS.toLong() - FRAME_MS.toLong())
            val standing = body() ?: error("no pill standing")
            val widths = ArrayList<Int>()
            repeat(30) {
                mainClock.advanceTimeByFrame()
                val b = body()
                widths += b?.w ?: 0
                if (b != null) assertTrue(b.top >= 0, "it stays below the component's top: ${b.top}")
            }
            val going = widths.takeWhile { it > 0 }
            assertTrue(widths.last() == 0, "gone once back: $widths")
            assertTrue(going.zipWithNext().all { (a, b) -> b <= a }, "it only narrows: $widths")
            assertTrue(
                going.count { it in 1 until standing.w * 3 / 4 } >= 3,
                "it narrows towards nothing over several frames, from ${standing.w}: $widths",
            )
        }

    /**
     * The share of the hanging slime that is the clean pill's lime rather than the boiled tongue's amber: of the
     * bright, saturated pixels above the list, those greener than red by [LIME_LEAD]. NaN when none hangs.
     */
    private fun ComposeUiTest.limeShare(): Double {
        val img = onNodeWithTag("s").captureToImage().toAwtImage()
        var slime = 0
        var lime = 0
        for (y in 0 until TOP_ROWS) {
            for (x in 0 until img.width) {
                val p = img.getRGB(x, y)
                val r = p shr 16 and 255
                val g = p shr 8 and 255
                val b = p and 255
                if (maxOf(r, g) > 150 && b < 110) {
                    slime++
                    if (g > r + LIME_LEAD) lime++
                }
            }
        }
        // No slime at all reads as no share, which no assertion takes.
        return if (slime == 0) Double.NaN else lime.toDouble() / slime
    }

    @Test
    fun `the boiling tongue fades out over the pill as it becomes it, instead of standing until the pill cuts in`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var refreshing by mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        MutantScreenClock {
                            Box(Modifier.size(300.dp, 200.dp).background(MutantTheme.colors.background).testTag("s")) {
                                MutantPullToRefresh(
                                    refreshing,
                                    { refreshing = true },
                                    Modifier.fillMaxSize(),
                                    refreshingLabel = "REFRESHING",
                                    doneLabel = "REFRESHED",
                                ) {
                                    Column(Modifier.verticalScroll(rememberScrollState()).testTag("list")) {
                                        repeat(8) { MutantListItem("Untitled ${it + 1}", key = "r$it") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            // A finger past the line: a pulled tongue boils as refreshing starts.
            onNodeWithTag("list").performTouchInput {
                down(Offset(150f, 40f))
                repeat(10) { moveBy(Offset(0f, 20f)) }
                up()
            }
            mainClock.advanceTimeBy(1_500)
            val boiling = limeShare()
            refreshing = false
            val shares =
                List(MORPH_FRAMES) {
                    mainClock.advanceTimeByFrame()
                    limeShare()
                }
            mainClock.advanceTimeBy(200)
            val standing = limeShare()
            assertTrue(boiling < .1, "refreshing, the tongue boils amber: lime $boiling")
            assertTrue(standing > .9, "the standing pill is lime: $standing")
            // Two frames in, the tongue is still mostly there; ten frames in (160 ms), it has faded over the pill.
            assertTrue(shares[1] < .5, "the tongue does not vanish at once: lime ${shares[1]} at 32 ms, $shares")
            assertTrue(shares[9] > .8, "the tongue has faded over the pill by 160 ms: lime ${shares[9]}, $shares")
        }

    @Test
    fun `refreshing boils the tongue on a screen composed refreshing and when started with no pull`() {
        run { _ ->
            val share = limeShare()
            assertTrue(share < .1, "composed refreshing, the tongue boils amber: lime share $share")
        }
        run(composedRefreshing = false) { set ->
            // Started by code or by a reader's action: no tongue hangs yet when it starts.
            set(true)
            mainClock.advanceTimeBy(1_000)
            val share = limeShare()
            assertTrue(share < .1, "started from rest, the tongue boils amber: lime share $share")
        }
    }

    private companion object {
        /** The rows the body hangs in: above the list once the gap closes to the pill's. */
        const val TOP_ROWS = 72

        /** The least slime in a row of the body proper, dp. */
        const val SOLID = 24

        /** How far over the page's red or green a pixel is to be slime: a tongue faded to a tenth still shows. */
        const val FAINT = 24

        /** The most the body's width or height may change from one frame to the next, dp. */
        const val MAX_STEP = 8
        const val FRAME_MS = 16.0

        /** How much greener than red a pixel is to count as the clean pill's lime, not the boil's amber or yellow. */
        const val LIME_LEAD = 30

        /** The frames read through the move: past its 300 ms. */
        const val MORPH_FRAMES = 20
    }
}
