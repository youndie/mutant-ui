package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.youndie.mutant.theme.MutantTheme
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-174: what the demo shows follows the record it belongs to, not the place it stands in — the queue's rows, the
 * table's selection, the chosen day and the recording's own side, length and time.
 */
@OptIn(ExperimentalTestApi::class)
class DemoRecordsTest {
    private val s = SampleStrings(SampleLanguage.English)

    /**
     * The Library phone with its Tape off (the clean signal), so a pixel is what the screen draws; with [reduce] its clock
     * stands (reduce motion).
     */
    private fun phone(
        reduce: Boolean = false,
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MutantTheme(reduceMotion = reduce, cleanSignal = true) {
                    DemoScreenFrame(DemoScreen.Library, s, keyframeLoss = false)
                }
            }
        }
        mainClock.advanceTimeBy(600)
        block()
    }

    private fun ComposeUiTest.screen(): BufferedImage =
        onNodeWithTag("demo-library", useUnmergedTree = true).captureToImage().toAwtImage()

    /** A list row with its divider under it: the row's clickable bounds and 6 dp below. */
    private fun ComposeUiTest.row(title: String): Rect =
        onNode(hasText(title) and hasClickAction()).fetchSemanticsNode().boundsInRoot.let {
            Rect(it.left, it.top, it.right, it.bottom + 6)
        }

    private fun BufferedImage.cut(r: Rect): BufferedImage =
        getSubimage(r.left.toInt(), r.top.toInt() - PHONE_TOP, r.width.toInt(), r.height.toInt())

    private fun unlike(
        a: BufferedImage,
        b: BufferedImage,
    ): Int {
        var n = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                if (a.getRGB(x, y) !=
                    b.getRGB(x, y)
                ) {
                    n++
                }
            }
        }
        return n
    }

    @Test
    fun `a tape put in at the top of the queue leaves every row below its own tile and divider`() =
        phone(reduce = true) {
            onNodeWithText(s.queue).performClick()
            mainClock.advanceTimeBy(800)
            val titles = listOf(s.rain, s.weather, s.lateRepeat)
            val before = titles.associateWith { screen().cut(row(it)) }
            onNodeWithText(s.rain, useUnmergedTree = true).performTouchInput {
                swipeDown(startY = 4f, endY = 260f, durationMillis = 400)
            }
            // The refresh brings «Fresh off the air» in after 2 s; then the rows have moved a row down.
            mainClock.advanceTimeBy(4_000)
            onNodeWithText(s.freshOffTheAir, useUnmergedTree = true).assertExists()
            val failures = ArrayList<String>()
            for (t in titles) {
                val was = before.getValue(t)
                val now = screen().cut(row(t))
                val n = unlike(was, now)
                // Under reduce motion and with the Tape off nothing moves but the rows, so a row keyed by its record draws
                // the same picture a row lower: 0 pixels unlike on 2026-11-23, the limit leaving B-101's 0.2 % for a body's
                // edges cut to the band it stands in (B-86). Keyed by its place, it takes the tile and divider above.
                if (n > was.width * was.height * MAX_SHARE) failures += "$t: $n of ${was.width * was.height} px unlike"
            }
            assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        }

    /**
     * The acid pixels in the selection box of the table's row that reads [title]: its 40 dp column, 48 dp tall. The box's
     * acid reads #cfe411 over the phone's ground, so an acid pixel is a bright yellow-green one: no other ink of the row's
     * box column is (the ground #10142a, the outline #05060c, the tick white).
     */
    private fun ComposeUiTest.acidBox(title: String): Int {
        val words = onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val shot = screen()
        val mid = words.center.y.toInt() - PHONE_TOP
        var n = 0
        for (y in mid - 24 until mid + 24) {
            for (x in TABLE_LEFT until TABLE_LEFT + 40) {
                val p = shot.getRGB(x, y)
                if ((p shr 16 and 255) > 150 && (p shr 8 and 255) > 190 && (p and 255) < 90) n++
            }
        }
        return n
    }

    @Test
    fun `a sort of the archive's table takes the chosen record's box with it in its first frame`() =
        phone {
            onNodeWithText(s.find).performClick()
            mainClock.advanceTimeBy(800)
            onNode(hasText(s.testCard) and isToggleable()).performClick()
            mainClock.advanceTimeBy(1_000)
            val chosen = acidBox(s.testCard)
            assertTrue(chosen > 200, "the chosen record's box at rest shows its acid: $chosen")
            // As filed, «Test card» is the third row; the longest first, «Rain over the transmitter» takes that place and
            // «Test card» goes last.
            val third = onNodeWithText(s.testCard, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
            onNodeWithText(s.sortBy(0), useUnmergedTree = true).performClick()
            mainClock.advanceTimeBy(400)
            onNodeWithText(s.sortOrders[1], useUnmergedTree = true).performClick()
            mainClock.advanceTimeByFrame()
            assertEquals(
                third,
                onNodeWithText(s.rain, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top,
                "the sort has put «Rain over the transmitter» where «Test card» stood",
            )
            val left = acidBox(s.rain)
            val reached = acidBox(s.testCard)
            // Keyed by place the box stood whole on «Rain…» and empty on «Test card» for this frame, then poured over.
            assertTrue(left < 28, "the place the chosen record left shows no acid in the first frame: $left of $chosen")
            assertTrue(reached > chosen * 9 / 10, "the place it reached shows its whole box: $reached of $chosen")
        }

    /**
     * The days shown chosen in the date picker under [within]: its selected nodes that read a number with no leading zero
     * (the time picker beside it reads «02»).
     */
    private fun ComposeUiTest.chosenDays(within: SemanticsMatcher): List<String> =
        onAllNodes(isSelected() and hasAnyAncestor(within))
            .fetchSemanticsNodes()
            .map { n -> n.config.getOrElse(SemanticsProperties.Text) { emptyList() }.joinToString { it.text } }
            .filter { it.isNotEmpty() && it.all(Char::isDigit) && !it.startsWith("0") }

    @Test
    fun `the chosen day stands chosen in its own month alone, on the page and in the demo's timer`() {
        val scroll = ScrollState(0)
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { MutantUiSample(SampleSettings(group = SpecGroup.Selection), scroll) }
            mainClock.advanceTimeBy(1_000)
            val section = hasTestTag("section-DatePicker")
            val top = onNode(section, useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y
            runOnUiThread { scroll.dispatchRawDelta(top) }
            mainClock.advanceTimeBy(100)
            assertEquals(listOf("13"), chosenDays(section), "November, as the page opens")
            onNode(hasContentDescription(s.nextMonth) and hasAnyAncestor(section)).performClick()
            mainClock.advanceTimeBy(600)
            assertEquals(emptyList(), chosenDays(section), "December: the 13th of November is not chosen in it")
            onNode(hasText("20") and hasAnyAncestor(section)).performClick()
            mainClock.advanceTimeBy(600)
            onNode(hasContentDescription(s.previousMonth) and hasAnyAncestor(section)).performClick()
            mainClock.advanceTimeBy(600)
            assertEquals(emptyList(), chosenDays(section), "November once the 20th of December is chosen")
        }
        phone {
            onNodeWithText(s.settings).performClick()
            mainClock.advanceTimeBy(800)
            // The timer's row stands below the phone's fold; its click is the semantics' (performScrollTo can hang the JVM
            // with the clock held).
            onNode(hasText(s.timerRecording) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
            mainClock.advanceTimeBy(800)
            val sheet = hasTestTag("demo-library")
            assertEquals(listOf("24"), chosenDays(sheet), "the timer's November")
            onNode(hasContentDescription(s.nextMonth) and hasAnyAncestor(sheet)).performClick()
            mainClock.advanceTimeBy(600)
            assertEquals(emptyList(), chosenDays(sheet), "the timer's December")
            onNodeWithText(s.timerLine(24, 11, 2, 40), useUnmergedTree = true).assertExists()
        }
    }

    /** How many texts of the phone read [text]. */
    private fun ComposeUiTest.shows(text: String) =
        onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `a recording reads its own side, length and channel, and its elapsed time where the kit's percent stood`() =
        phone {
            onNodeWithText(s.untitled).performClick()
            mainClock.advanceTimeBy(800)
            // The design's tape, in the design's words: «Side A», its line, and 12:04 played.
            assertEquals(1, shows(s.sideAShort), "the design's side")
            assertEquals(1, shows(s.sideALine), "the design's line")
            // The first track reads 12:04 too, as in the design.
            assertEquals(2, shows("12:04"), "the design's elapsed time beside the track's")
            assertEquals(0, shows("026%"), "no percent")
            onNodeWithContentDescription(s.back).performClick()
            mainClock.advanceTimeBy(800)
            // Another tape, opened by its card on the shelf below the fold (the carousel above shows it too).
            onAllNodes(hasText(s.rain) and hasClickAction())[1].performSemanticsAction(SemanticsActions.OnClick)
            mainClock.advanceTimeBy(800)
            assertEquals(1, shows("Side B"), "the tape's own side")
            assertEquals(1, shows("31:12 · CH 05"), "the tape's own length and channel")
            assertEquals(0, shows(s.sideALine), "not the design's line")
            // 12:04 of 46:00 is 26.2 %: as far into this tape's 31:12, 8:11.
            assertEquals(1, shows("8:11"), "as far into its own length")
        }

    private companion object {
        /** Where the phone stands in the test's window: under its 28 dp title and the 16 dp gap. */
        const val PHONE_TOP = 48

        /** Where the table's rows start in the phone: its 16 dp padding. */
        const val TABLE_LEFT = 16

        /** B-101's share for a body's edges where its image is cut to its band (B-86). */
        const val MAX_SHARE = .002
    }
}
