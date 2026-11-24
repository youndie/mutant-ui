package io.github.youndie.mutant.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.pattern.MutantPattern
import io.github.youndie.mutant.pattern.mutantPattern
import io.github.youndie.mutant.surface.mutantTape
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-205 (the designer's answer 37 to B-168): right to left, every component's layout mirrors — the segments, the
 * month arrows, the switch's thumb, the slider, the progress, the drawer's side, every row — and what is drawn, not laid
 * out, does not: the slime's shape and drips, the highlight at the top left, the offset shadow (4, 4), on-screen digits
 * and time, the Tape and the reel. Each public component of the QA catalogue stands in a 360 dp stage at density 1, still,
 * once left to right and once right to left.
 */
@OptIn(ExperimentalTestApi::class)
class RightToLeftSweepTest {
    /** What is not laid out by the direction: the time reads as a clock does, a picture is a picture. */
    private val unmirrored = setOf("TimePicker", "Illustration")

    /** The texts of the pictures inside a mirrored component: they stand where they do left to right, mirrored with it. */
    private val pictureWords = setOf("НЕТ\nСИГНАЛА", "НЕТ СИГНАЛА")

    private class Shot(
        val nodes: List<Pair<String, Rect>>,
        val pixels: IntArray,
        val width: Int,
    )

    /** Each subject composed in turn, left to right and then right to left, in one window. */
    private fun shots(of: List<Subject> = subjects): Map<String, Pair<Shot, Shot>> {
        val out = LinkedHashMap<String, Pair<Shot, Shot>>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            var i by mutableIntStateOf(0)
            var dir by mutableStateOf(LayoutDirection.Ltr)
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides dir) {
                    Still {
                        // Centred, so a component narrower than the stage stands at the same place both ways.
                        Box(Modifier.size(STAGE, STAGE_HEIGHT).testTag("stage"), contentAlignment = Alignment.Center) {
                            key(i, dir) { of[i].content(Modifier, "Эфир ночь") }
                        }
                    }
                }
            }
            for (k in of.indices) {
                i = k
                dir = LayoutDirection.Ltr
                repeat(4) { mainClock.advanceTimeByFrame() }
                val ltr = shot()
                dir = LayoutDirection.Rtl
                repeat(4) { mainClock.advanceTimeByFrame() }
                out[of[k].name] = ltr to shot()
            }
        }
        return out
    }

    private fun ComposeUiTest.shot(): Shot {
        val stage = onNodeWithTag("stage").fetchSemanticsNode().positionInRoot
        val nodes =
            allNodes()
                .filter { it.size.width > 0 && it.size.height > 0 }
                .mapNotNull { n -> n.word()?.let { w -> w to n.rect(stage) } }
        val px = onNodeWithTag("stage").captureToImage()
        val buffer = IntArray(px.width * px.height)
        px.readPixels(buffer)
        return Shot(nodes, buffer, px.width)
    }

    private fun SemanticsNode.word(): String? =
        config.getOrElse(SemanticsProperties.Text) { emptyList() }.firstOrNull()?.text
            ?: config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.firstOrNull()

    private fun SemanticsNode.rect(stage: androidx.compose.ui.geometry.Offset): Rect {
        val p = positionInRoot - stage
        return Rect(p.x, p.y, p.x + size.width, p.y + size.height)
    }

    /** The overlays the catalogue leaves out, open in the stage: a dialog with two actions and a sheet with a row. */
    private val overlays =
        listOf(
            Subject("Dialog") { m, t ->
                MutantDialog(true, t, {}, m, text = t) {
                    MutantDialogAction("ОТМЕНА", {})
                    MutantDialogAction("ЗАПИСАТЬ", {}, primary = true)
                }
            },
            Subject("BottomSheet") { m, t ->
                MutantBottomSheet(true, {}, m) {
                    MutantListItem(t, trailingText = "12:04")
                    MutantButton(t, {})
                }
            },
        )

    @Test
    fun `right to left, every component's words, icons and targets stand mirrored`() {
        val w = STAGE.value
        val wrong = mutableListOf<String>()
        var compared = 0
        for ((name, pair) in shots(subjects + overlays)) {
            if (name in unmirrored) continue
            val (ltr, rtl) = pair
            compared += ltr.nodes.size
            if (ltr.nodes.size != rtl.nodes.size) {
                wrong += "$name: ${ltr.nodes.size} nodes left to right, ${rtl.nodes.size} right to left"
                continue
            }
            for ((a, b) in ltr.nodes.zip(rtl.nodes)) {
                val (word, l) = a
                val r = b.second
                val picture = word in pictureWords
                val expected = if (picture) l.left else w - l.right
                // Within 2 px: a line's width right to left is laid out on the other side of a fractional pixel.
                if (abs(r.left - expected) > 2f || abs(r.top - l.top) > 1f) {
                    wrong +=
                        "$name «${word.take(
                            16,
                        )}»: left ${r.left} right to left, $expected expected (${l.left} left to right)"
                }
            }
        }
        assertTrue(compared > 120, "$compared words, icons and targets compared")
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun `right to left, the time and the pictures stand as they do left to right`() {
        val wrong = mutableListOf<String>()
        for ((name, pair) in shots(subjects.filter { it.name in unmirrored })) {
            val (ltr, rtl) = pair
            for ((a, b) in ltr.nodes.zip(rtl.nodes)) {
                if (abs(a.second.left - b.second.left) > 1f) {
                    wrong +=
                        "$name «${a.first.take(
                            16,
                        )}»: left ${b.second.left} right to left, ${a.second.left} left to right"
                }
            }
        }
        assertEquals(emptyList(), wrong)
    }

    /**
     * The bodies are drawn as they are left to right: an empty card, a body with an icon (the FAB, the icon button), the
     * reel, the Tape over a pattern, a picture, the time picker. Read over the whole stage: the two directions differ in under 1 % of
     * its pixels, and the stage right to left differs from the mirror image of the one left to right in over 3 % — what a
     * mirrored slime, drip, highlight or shadow would show.
     */
    @Test
    fun `right to left, the slime, its drips, highlight and shadow, the reel and the pictures are not mirrored`() {
        val names = setOf("Fab", "IconButton", "CircularProgress", "Illustration", "TimePicker")
        // The reel is near its own mirror image: it is held to stand as it does, not read against the mirror.
        val symmetric = setOf("CircularProgress")
        val tape =
            Subject("Pattern+Tape") { m, _ ->
                Box(m.size(240.dp, 120.dp).mutantPattern(MutantPattern.Mosaic).mutantTape())
            }
        // A card with nothing in it: its fill's marks, drips, highlight and shadow with no words to tell the ways apart.
        val card = Subject("Card.Empty") { m, _ -> MutantCard(m.size(220.dp, 120.dp)) {} }
        val wrong = mutableListOf<String>()
        for ((name, pair) in shots(subjects.filter { it.name in names } + tape + card)) {
            val (ltr, rtl) = pair
            val n = ltr.pixels.size
            val w = ltr.width
            var same = 0
            var flipped = 0
            var ink = 0
            for (i in 0 until n) {
                val x = i % w
                val y = i / w
                val a = ltr.pixels[i]
                if (a ushr 24 != 0) ink++
                if (!close(a, rtl.pixels[i])) same++
                if (!close(a, rtl.pixels[y * w + (w - 1 - x)])) flipped++
            }
            // The stage's pixels the component inks: the mirror's share of them, and the directions' share.
            val sameShare = same.toDouble() / ink
            val flipShare = flipped.toDouble() / ink
            if (ink == 0 || sameShare > .01 || (name !in symmetric && flipShare < .03)) {
                wrong +=
                    "$name: ${"%.2f".format(sameShare * 100)} % differ right to left, " +
                    "${"%.2f".format(flipShare * 100)} % from the mirror image, of $ink inked"
            }
        }
        assertEquals(emptyList(), wrong)
    }

    /** Within 8 per channel. */
    private fun close(
        a: Int,
        b: Int,
    ): Boolean = (0 until 4).all { s -> abs(((a ushr (s * 8)) and 0xFF) - ((b ushr (s * 8)) and 0xFF)) <= 8 }

    private companion object {
        val STAGE = 360.dp
        val STAGE_HEIGHT = 700.dp
    }
}
