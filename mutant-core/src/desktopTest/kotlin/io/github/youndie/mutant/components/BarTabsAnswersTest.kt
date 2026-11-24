package io.github.youndie.mutant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-187: the designer's answers 9 and 31 to B-168 — the tabs' labels hand the accent over late; a narrow bar drops its
 * status first.
 */
@OptIn(ExperimentalTestApi::class)
class BarTabsAnswersTest {
    private val three = listOf(MutantTab("Дорожки"), MutantTab("Заметки"), MutantTab("Копии"))

    /** Pixels of [label]'s own box in the accent: a label in it has a hundred or more at 14 sp, one out of it none. */
    private fun ComposeUiTest.accentIn(label: String): Int {
        val accent = MutantPalettes.Efir.textAccent
        val box = onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val px = onNodeWithTag("t").captureToImage().toPixelMap()
        var n = 0
        for (y in box.top.toInt() until box.bottom.toInt()) {
            for (x in box.left.toInt() until box.right.toInt()) {
                val q = px[x, y]
                if (abs(q.red - accent.red) + abs(q.green - accent.green) + abs(q.blue - accent.blue) < .06f) n++
            }
        }
        return n
    }

    @Test
    fun `a click takes the accent off the old label in the first frame and gives it to the new one from 280 ms`() {
        runComposeUiTest {
            mainClock.autoAdvance = false
            var at by mutableIntStateOf(0)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MutantTheme {
                        Box(Modifier.background(MutantTheme.colors.background).testTag("t")) {
                            MutantTabs(three, at, {
                                at =
                                    it
                            })
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            assertTrue(accentIn("Дорожки") > 60, "the first selected, in the accent")
            onNodeWithText("Заметки").performClick()
            // Frame by frame from the one that composes the click: (ms after it, the old label's accent, the new one's).
            val frames = mutableListOf<Triple<Long, Int, Int>>()
            val t0 = mainClock.currentTime + 16
            repeat(30) {
                mainClock.advanceTimeByFrame()
                frames += Triple(mainClock.currentTime - t0, accentIn("Дорожки"), accentIn("Заметки"))
            }
            assertEquals(1, at)
            assertTrue(frames.all { it.second == 0 }, "the old label out of the accent from the first frame: $frames")
            // The bar sets off in the frame that composes the click; the new label turns in the first frame 280 ms on.
            val late = frames.filter { it.first < TAB_ACCENT_HANDOVER_MS.toLong() }
            val turned = frames.filter { it.first >= TAB_ACCENT_HANDOVER_MS.toLong() }
            assertTrue(late.size >= 15 && late.all { it.third < 10 }, "the new label waits: $frames")
            assertTrue(
                turned.isNotEmpty() && turned.all { it.third > 60 },
                "the new label in the accent from 280 ms: $frames",
            )
        }
        // Under reduce motion, and with no bar to run (from no selection), the finished frame at once.
        for ((reduce, from) in listOf(true to 0, false to -1)) {
            runComposeUiTest {
                mainClock.autoAdvance = false
                var at by mutableIntStateOf(from)
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MutantTheme(reduceMotion = reduce) {
                            Box(Modifier.background(MutantTheme.colors.background).testTag("t")) {
                                MutantTabs(three, at, {
                                    at =
                                        it
                                })
                            }
                        }
                    }
                }
                mainClock.advanceTimeBy(500)
                onNodeWithText("Заметки").performClick()
                mainClock.advanceTimeByFrame()
                assertEquals(0, accentIn("Дорожки"), "reduce $reduce, from $from: the old label")
                assertTrue(accentIn("Заметки") > 60, "reduce $reduce, from $from: the new label at once")
            }
        }
    }

    private fun ComposeUiTest.part(
        text: String? = null,
        description: String? = null,
    ) = (
        if (text != null) {
            onNodeWithText(text, useUnmergedTree = true)
        } else {
            onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description!!)), true)
        }
    ).fetchSemanticsNode()

    @Test
    fun `from 360 to 120 dp the icons stay, the status goes before the title is cut, nothing leaves the bar`() {
        val status = "CH 03 · SP"
        for (density in listOf(1f, 2f)) {
            for (title in listOf("On air", "Эфир с начала первой стороны без подписи")) {
                runComposeUiTest {
                    mainClock.autoAdvance = false
                    var w by mutableStateOf(360)
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(density)) {
                            MutantTheme {
                                MutantTopAppBar(
                                    title,
                                    Modifier.width(w.dp).testTag("bar"),
                                    navigationIcon = { MutantIcon(MutantIcons.Menu, "menu") },
                                    status = status,
                                    windowInsets = WindowInsets(0),
                                    actions = { MutantIcon(MutantIcons.Search, "find") },
                                )
                            }
                        }
                    }
                    var dropped: Int? = null
                    for (width in 360 downTo 120 step 4) {
                        w = width
                        mainClock.advanceTimeByFrame()
                        val at = "$title at $width dp, density $density"
                        val bar = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot

                        fun Rect.inside() =
                            left >= bar.left - .5f && right <= bar.right + .5f && top >= bar.top && bottom <= bar.bottom
                        val icon = 24 * density
                        for (d in listOf("menu", "find")) {
                            val b = part(description = d).boundsInRoot
                            assertTrue(abs(b.width - icon) < .5f && b.inside(), "$at: $d whole in the bar, $b in $bar")
                        }
                        val t = part(text = title)
                        val layouts = mutableListOf<TextLayoutResult>()
                        t.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                        val whole =
                            layouts
                                .single()
                                .multiParagraph.intrinsics.maxIntrinsicWidth
                        assertEquals(TextOverflow.Ellipsis, layouts.single().layoutInput.overflow, at)
                        // The title keeps room for at least its first letters and the ellipsis: the bug gave it none.
                        assertTrue(
                            t.boundsInRoot.width >= minOf(whole, 40 * density) && t.boundsInRoot.inside(),
                            "$at: title ${t.boundsInRoot}",
                        )
                        val s = part(text = status)
                        if (s.layoutInfo.isPlaced) {
                            assertEquals(null, dropped, "$at: the status back after it went at $dropped dp")
                            assertTrue(s.boundsInRoot.inside(), "$at: status ${s.boundsInRoot} in $bar")
                            // Standing, it has the title keep its whole width, or as much as the status takes.
                            val room = t.boundsInRoot.width
                            assertTrue(
                                room + .5f >= minOf(whole, s.boundsInRoot.width),
                                "$at: the title cut to $room, the status kept",
                            )
                        } else {
                            dropped = dropped ?: width
                        }
                    }
                    // A short title goes whole until the status has gone; a bar of 120 dp has dropped it either way.
                    assertTrue(dropped != null, "$title, density $density: the status never went")
                }
            }
        }
    }
}
