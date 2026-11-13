package io.github.youndie.mutant.type

import org.jetbrains.skia.Typeface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-02: every character the design's pages put on screen is in all four bundled instances, except
 * the ones listed here with their reason. Compose never falls back to the next face in a
 * `FontFamily`; a missing glyph is drawn by whatever the host has, differently per machine. Fixture
 * strings are copied from these pages verbatim (research D11), so a fixture that brings a new
 * character brings it here first.
 */
class MutantFontCoverageTest {
    /** Text the pages render: element text and the quoted strings their page logic builds. */
    private val corpus: Set<Int> by lazy {
        val text = Regex(""">([^<]*)<|'([^'\n]*)'""")
        BundledFonts.designPages
            .flatMap { page ->
                text.findAll(page.readText()).map { it.groupValues[1] + it.groupValues[2] }.toList()
            }.joinToString("")
            .codePoints()
            .toArray()
            .filter { it > 0x20 && !Character.isWhitespace(it) && !Character.isISOControl(it) }
            .toSet()
    }

    /**
     * ▶ and ❚ are the pause button of the Demo and Components pages' own chrome, not a component.
     * ◄ and ► are the DatePicker's month arrows (B-45), which the kit draws as icons.
     */
    private val allowed = setOf("▶", "❚", "◄", "►")

    private fun missing(face: Typeface): Set<String> =
        corpus.filter { face.getUTF32Glyph(it).toInt() == 0 }.map { String(Character.toChars(it)) }.toSet()

    @Test
    fun `the corpus is the design, not an empty directory`() {
        assertEquals(10, BundledFonts.designPages.size, "design pages found")
        assertTrue(corpus.containsAll("ЖЩЯжщя".codePoints().toArray().toList()), "Cyrillic in the corpus")
        assertTrue(corpus.containsAll("AZaz09".codePoints().toArray().toList()), "Latin in the corpus")
    }

    @Test
    fun `every instance draws every character of the design but the pages' own transport symbols`() {
        for (face in BundledFonts.all) {
            assertEquals(emptySet(), missing(face) - allowed, "${face.familyName} ${face.fontStyle.weight}")
        }
    }

    @Test
    fun `every instance carries the whole Russian alphabet and the Latin one`() {
        val alphabets = (('А'..'я') + 'Ё' + 'ё' + ('A'..'Z') + ('a'..'z')).map { it.code }
        for (face in BundledFonts.all) {
            assertEquals(emptyList(), alphabets.filter { face.getUTF32Glyph(it).toInt() == 0 }, face.familyName)
        }
    }
}
