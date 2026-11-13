package io.github.youndie.mutant

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * B-01, research D1: the generator's package imports nothing from Compose, so the generator stays a
 * pure function of numbers and could be moved into a module of its own. Reads the sources, which the
 * module's build declares as an input of this test.
 */
class GeneratorBoundaryTest {
    private val genDir = File("src/commonMain/kotlin/io/github/youndie/mutant/gen")

    @Test
    fun `the generator package imports nothing from Compose`() {
        val sources = genDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        if (sources.isEmpty()) fail("no sources under ${genDir.absolutePath}: the test would pass on nothing")
        val offending =
            sources.flatMap { file ->
                file.readLines().mapIndexedNotNull { i, line ->
                    "${file.name}:${i + 1}: $line".takeIf { forbidden(line) }
                }
            }
        assertTrue(offending.isEmpty(), "Compose in the generator package:\n" + offending.joinToString("\n"))
    }

    @Test
    fun `the rule sees a Compose import`() {
        assertTrue(forbidden("import androidx.compose.ui.graphics.Path"))
        assertTrue(forbidden("import org.jetbrains.compose.resources.Font"))
        assertTrue(!forbidden("import kotlin.math.sin"))
    }

    private fun forbidden(line: String): Boolean {
        val trimmed = line.trimStart()
        return COMPOSE_IMPORTS.any { trimmed.startsWith(it) }
    }

    private companion object {
        val COMPOSE_IMPORTS = listOf("import androidx.compose", "import org.jetbrains.compose")
    }
}
