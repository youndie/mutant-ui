package io.github.youndie.mutant.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-204: the navigation badge grows right from the icon's top-right corner, over none of its ink, and stops at «99+». */
@OptIn(ExperimentalTestApi::class)
class NavigationBarBadgeTest {
    private fun near(
        a: Color,
        b: Color,
        within: Float,
    ) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) < within

    /** Four destinations, the last three carrying [badge] (or none): the clock, the lens and the knobs, whose ink reaches the corner. */
    private fun items(badge: String?) =
        listOf(
            MutantNavigationItem("Эфир", MutantIcons.Home),
            MutantNavigationItem("Очередь", MutantIcons.Schedule, badge = badge),
            MutantNavigationItem("Поиск", MutantIcons.Search, badge = badge),
            MutantNavigationItem("Настройка", MutantIcons.Settings, badge = badge),
        )

    /** One bar per entry of [badges], 328 dp wide at [density], stacked; the bars' pixels. */
    private fun bars(
        badges: List<String?>,
        density: Float,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ): PixelMap {
        var px: PixelMap? = null
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density),
                    LocalLayoutDirection provides direction,
                ) {
                    MutantTheme(reduceMotion = true) {
                        Column(Modifier.testTag("c")) {
                            for (b in badges) {
                                MutantNavigationBar(
                                    items(b),
                                    0,
                                    {},
                                    Modifier.width(328.dp),
                                    windowInsets = WindowInsets(0),
                                )
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            px = onNodeWithTag("c").captureToImage().toPixelMap()
        }
        return px!!
    }

    /** The amber plate's first and last column inside item [i]'s cell of bar [row], or null. */
    private fun plate(
        px: PixelMap,
        density: Float,
        row: Int,
        i: Int,
    ): IntRange? {
        val amber = MutantPalettes.Efir.amber
        val cell = (82 * density).roundToInt()
        val h = (68 * density).roundToInt()
        val ys = row * h until (row + 1) * h
        // The cell and the next half cell: a plate that grows right may leave its own.
        val xs =
            (i * cell until minOf(px.width, (i + 1) * cell + cell / 2)).filter { x ->
                ys.any { y -> near(px[x, y], amber, .03f) }
            }
        return if (xs.isEmpty()) null else xs.first()..xs.last()
    }

    @Test
    fun `badges of 1, 12 and 99+ start at the icon's top-right corner and grow right, at density 1 and 2`() {
        val texts = listOf("1", "12", "99+")
        for (d in listOf(1f, 2f)) {
            val px = bars(texts, d)
            val cell = (82 * d).roundToInt()
            val icon = (22 * d).roundToInt()
            for (i in 1..3) {
                // The icon centred in its cell: its right edge, where the plate starts.
                val iconRight = i * cell + (cell - icon) / 2 + icon
                val plates = texts.indices.map { row -> plate(px, d, row, i) }
                plates.forEachIndexed { row, p ->
                    assertTrue(p != null, "density $d item $i «${texts[row]}»: an amber plate")
                    assertEquals(
                        iconRight,
                        p.first,
                        "density $d item $i «${texts[row]}»: the plate $p starts at the icon's right edge",
                    )
                }
                val rights = plates.map { it!!.last }
                assertTrue(
                    rights[0] < rights[1] && rights[1] < rights[2],
                    "density $d item $i: wider counts grow right, $rights",
                )
            }
        }
    }

    @Test
    fun `no badge covers its icon's ink`() {
        for (d in listOf(1f, 2f)) {
            // Row 0 bare, then 1, 12 and 99+.
            val px = bars(listOf(null, "1", "12", "99+"), d)
            val cell = (82 * d).roundToInt()
            val icon = (22 * d).roundToInt()
            val h = (68 * d).roundToInt()
            val top = ((68 - 30) / 2f * d).roundToInt() + (4 * d).roundToInt()
            val ink = MutantPalettes.Efir.textSecondary
            for (i in 1..3) {
                val x0 = i * cell + (cell - icon) / 2
                // The icon's ink, bare: the pixels in its square at the icon's colour.
                val inked =
                    (top until top + icon)
                        .flatMap { y -> (x0 until x0 + icon).map { x -> x to y } }
                        .filter { (x, y) -> near(px[x, y], ink, .12f) }
                assertTrue(inked.size > 20 * d * d, "density $d item $i: the bare icon's ink, ${inked.size} px")
                for (row in 1..3) {
                    val lost = inked.count { (x, y) -> !near(px[x, row * h + y], ink, .12f) }
                    assertEquals(0, lost, "density $d item $i badge row $row: ink pixels the badge covered")
                }
            }
        }
    }

    @Test
    fun `right to left, the badge grows left from the icon's top-left corner`() {
        val px = bars(listOf("1", "99+"), 1f, LayoutDirection.Rtl)
        val amber = MutantPalettes.Efir.amber
        // Mirrored, item 3 stands in the first cell, 0 to 82, its icon 30 to 52; its plate left of the icon, in the cell.
        for (row in 0..1) {
            val xs =
                (0 until 82).filter { x ->
                    (row * 68 until (row + 1) * 68).any { y -> near(px[x, y], amber, .03f) }
                }
            assertTrue(xs.isNotEmpty(), "row $row: a plate")
            assertEquals(29, xs.last(), "row $row: the plate ${xs.first()}..${xs.last()} ends on the icon's left edge")
        }
    }

    @Test
    fun `a count above 99 reads 99+`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                MutantTheme(reduceMotion = true) {
                    MutantNavigationBar(
                        listOf(
                            MutantNavigationItem("А", MutantIcons.Home, badge = "100"),
                            MutantNavigationItem("Б", MutantIcons.Schedule, badge = "99"),
                            MutantNavigationItem("В", MutantIcons.Search, badge = "123456789012345678901234567890"),
                            MutantNavigationItem("Г", MutantIcons.Settings, badge = "НОВОЕ"),
                        ),
                        0,
                        {},
                        Modifier.width(328.dp),
                        windowInsets = WindowInsets(0),
                    )
                }
            }
            mainClock.advanceTimeBy(300)
            onAllNodesWithText("99+", useUnmergedTree = true).assertCountEquals(2)
            onAllNodesWithText("99", useUnmergedTree = true).assertCountEquals(1)
            onAllNodesWithText("100", useUnmergedTree = true).assertCountEquals(0)
            onAllNodesWithText("НОВОЕ", useUnmergedTree = true).assertCountEquals(1)
        }

    @Test
    fun `the cap reads whole numbers by their digits and leaves other text`() {
        assertEquals("0", navigationBadgeText("0"))
        assertEquals("99", navigationBadgeText("99"))
        assertEquals("099", navigationBadgeText("099"))
        assertEquals("99+", navigationBadgeText("100"))
        assertEquals("99+", navigationBadgeText("0100"))
        assertEquals("99+", navigationBadgeText("9".repeat(40)))
        assertEquals("-500", navigationBadgeText("-500"))
        assertEquals("1 000", navigationBadgeText("1 000"))
        assertEquals("", navigationBadgeText(""))
        assertEquals("99+", navigationBadgeText("99+"))
    }
}
