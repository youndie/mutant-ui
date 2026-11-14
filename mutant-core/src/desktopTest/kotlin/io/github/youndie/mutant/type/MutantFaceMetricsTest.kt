package io.github.youndie.mutant.type

import java.io.File
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** B-52: the metrics [cssLine] lays a line with are the bundled faces' own, read from the files' `hhea` and `OS/2`. */
class MutantFaceMetricsTest {
    private class Read(
        val ascent: Float,
        val descent: Float,
        val typoAgrees: Boolean,
    )

    private fun read(file: String): Read {
        val b = ByteBuffer.wrap(File("src/commonMain/composeResources/font/$file").readBytes())
        val tables =
            (0 until b.getShort(4).toInt()).associate { i ->
                String(ByteArray(4) { b.get(12 + 16 * i + it) }) to
                    b.getInt(12 + 16 * i + 8)
            }
        val head = tables.getValue("head")
        val hhea = tables.getValue("hhea")
        val os2 = tables.getValue("OS/2")
        val upem = b.getShort(head + 18).toInt() and 0xFFFF
        val ascent = b.getShort(hhea + 4).toInt()
        val descent = -b.getShort(hhea + 6).toInt()
        val useTypo = b.getShort(os2 + 62).toInt() and 128 != 0
        val typo = b.getShort(os2 + 68).toInt() == ascent && -b.getShort(os2 + 70).toInt() == descent
        return Read(ascent / upem.toFloat(), descent / upem.toFloat(), useTypo && typo)
    }

    @Test
    fun `Handjet's and Rubik's metrics are the files', in every cut`() {
        for ((file, face) in listOf(
            "handjet_regular.ttf" to MutantFaceMetrics.Handjet,
            "handjet_semibold.ttf" to MutantFaceMetrics.Handjet,
            "rubik_regular.ttf" to MutantFaceMetrics.Rubik,
            "rubik_medium.ttf" to MutantFaceMetrics.Rubik,
        )) {
            val r = read(file)
            assertEquals(r.ascent, face.ascent, 1e-6f, "$file ascent")
            assertEquals(r.descent, face.descent, 1e-6f, "$file descent")
            // Blink takes hhea on the Mac and the typo metrics where USE_TYPO_METRICS is set: one answer here.
            assertTrue(r.typoAgrees, "$file: hhea and OS/2 typo metrics disagree")
        }
    }
}
