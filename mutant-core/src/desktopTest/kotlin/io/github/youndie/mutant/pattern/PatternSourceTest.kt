package io.github.youndie.mutant.pattern

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B-14: the patterns' alphas a swatch cannot show (`PatternAlpha`), held to the numbers in the Icons page's own
 * `patterns()`, read from the vendored page.
 */
class PatternSourceTest {
    private val page = File("../reference/design-system/mutant-ui Иконки и заготовки.dc.html").readText()

    private fun pair(pattern: String): Pair<Float, Float> {
        val m =
            Regex(Regex.escape(pattern) + """\$\{q \? ([\d.]+) : ([\d.]+)\}""").find(page)
                ?: error("not on the page: $pattern")
        return m.groupValues[1].toFloat() to m.groupValues[2].toFloat()
    }

    @Test
    fun `the lines' alphas are the page's in both themes`() {
        assertEquals(PatternAlpha.LINES_QUIET to PatternAlpha.LINES, pair("rgba(0,0,0,"))
        assertEquals(PatternAlpha.LINES_PRINT_QUIET to PatternAlpha.LINES_PRINT, pair("rgba(20,17,15,"))
    }

    @Test
    fun `the quiet mosaic's opacity is the page's`() {
        val m =
            Regex("""\$\{q \? ' opacity="([\d.]+)"' : ''\}""").find(page)
                ?: error("no quiet mosaic opacity on the page")
        assertEquals(PatternAlpha.MOSAIC_QUIET, m.groupValues[1].toFloat())
    }
}
