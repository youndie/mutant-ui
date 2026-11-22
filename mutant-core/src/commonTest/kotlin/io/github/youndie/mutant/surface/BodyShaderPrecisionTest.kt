package io.github.youndie.mutant.surface

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B-167: the body shader never builds a number past 2048 out of a texture's channels in one expression. A channel is a
 * `half`, and a phone's GPU (the Pixel 6a's, in Chrome) works out an expression of halves at half precision, where such
 * a number loses its low bits: the field's flags were lost so, and every outline with them, on the phone alone — the
 * desktop, every golden and headless Chrome (with or without its precision emulation) compute at full precision and
 * drew them. So a channel becomes a number only inside `c5`, `c6` and `c8`, which hold it under 256, and the words are
 * summed from what they return.
 */
class BodyShaderPrecisionTest {
    private val helpers = setOf("c5", "c6", "c8")

    /** The shader's functions, by name, with their bodies. */
    private fun functions(): Map<String, String> {
        val out = HashMap<String, String>()
        val head =
            Regex("""^\s*(?:float|half|half4|float2|bool|half2)\s+(\w+)\s*\([^)]*\)\s*\{""", RegexOption.MULTILINE)
        for (m in head.findAll(BODY_SKSL)) {
            var depth = 1
            var i = m.range.last + 1
            while (depth > 0 && i < BODY_SKSL.length) {
                when (BODY_SKSL[i]) {
                    '{' -> depth++
                    '}' -> depth--
                }
                i++
            }
            out[m.groupValues[1]] = BODY_SKSL.substring(m.range.last + 1, i - 1)
        }
        return out
    }

    @Test
    fun `a channel becomes a number only through the small helpers`() {
        val fns = functions()
        assertTrue(helpers.all { it in fns }, "the helpers c5, c6 and c8 are in the shader: ${fns.keys}")
        // A half turned into a float, `float(x)` of anything but a whole float expression, outside the helpers.
        val cast = Regex("""float\(\s*[\w.]+\s*\)""")
        val bad =
            fns.filterKeys { it !in helpers }.flatMap { (name, body) ->
                cast
                    .findAll(body)
                    .map { "$name: ${it.value}" }
                    .filter {
                        !it.contains("float(di)") &&
                            !it.contains("float(dj)") &&
                            !it.contains("float(k)")
                    }.toList()
            }
        assertTrue(bad.isEmpty(), "a half made a number outside c5, c6, c8: $bad")
    }

    @Test
    fun `the words and the field are read through the helpers`() {
        val fns = functions()
        for (name in listOf("val", "flagsOf", "word", "u16")) {
            val body = fns[name] ?: error("$name is not in the shader")
            assertTrue(helpers.any { "$it(" in body }, "$name reads its channels through a helper: $body")
        }
    }
}
