package io.github.youndie.mutant.sample

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import org.jetbrains.skia.Data
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** B-37: the sample walks every section in each theme and language, and every word it shows has its glyphs. */
@OptIn(ExperimentalTestApi::class)
class SampleAppTest {
    @Test
    fun `every section composes and draws in each theme and language, the clock held`() {
        for (print in listOf(false, true)) {
            for (language in SampleLanguage.entries) {
                runComposeUiTest {
                    // The kit's clock asks for frames for ever: held, the walk is the same every time.
                    mainClock.autoAdvance = false
                    val settings = SampleSettings(print = print, language = language)
                    setContent { MutantUiSample(settings) }
                    mainClock.advanceTimeByFrame()
                    onNodeWithTag("sample-root").assertIsDisplayed()
                    // B-99: one section per entry of the catalogue, every component of the kit and the foundations.
                    for (id in SpecId.entries) {
                        onNodeWithTag(
                            "section-${id.name}",
                        ).assertExists("${id.name}, ${if (print) "Копия" else "Эфир"}, $language")
                    }
                    // The other page draws too.
                    settings.page = SamplePage.Demo
                    mainClock.advanceTimeByFrame()
                    onNodeWithTag("sample-root").assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun `it opens in English, the owner's choice`() {
        assertTrue(SampleSettings().language == SampleLanguage.English)
    }

    @Test
    fun `every word in both languages has its glyphs in both bundled faces`() {
        val fonts =
            File("../mutant-core/src/commonMain/composeResources/font").listFiles()!!.filter {
                it.extension ==
                    "ttf"
            }
        assertTrue(fonts.size == 4, "Handjet and Rubik, two cuts each: $fonts")
        val words =
            SampleLanguage.entries.flatMap { language ->
                val s = SampleStrings(language)
                SampleStrings::class.java.declaredFields
                    .filter { it.type == String::class.java }
                    .map { f ->
                        f.isAccessible = true
                        f.get(s) as String
                    }
            } + SpecId.entries.flatMap { listOf(it.en, it.ru, it.enLine, it.ruLine) + it.api }
        assertTrue(words.size > 100, "both languages' words: ${words.size}")
        val missing = ArrayList<String>()
        for (font in fonts) {
            val face =
                org.jetbrains.skia.FontMgr.default
                    .makeFromData(Data.makeFromBytes(font.readBytes()))!!
            for (cp in words.flatMap { w -> w.codePoints().toArray().toList() }.distinct()) {
                if (Character.isWhitespace(cp)) continue
                if (face.getUTF32Glyph(cp) ==
                    0.toShort()
                ) {
                    missing += "${font.name}: ${String(Character.toChars(cp))} U+${cp.toString(16)}"
                }
            }
        }
        assertTrue(missing.isEmpty(), "missing glyphs:\n" + missing.joinToString("\n"))
        // The check can fail: a character none of the faces carries reads as missing in each.
        for (font in fonts) {
            val face =
                org.jetbrains.skia.FontMgr.default
                    .makeFromData(Data.makeFromBytes(font.readBytes()))!!
            assertTrue(face.getUTF32Glyph(0x5B57) == 0.toShort(), "${font.name} should lack U+5B57")
        }
    }
}
