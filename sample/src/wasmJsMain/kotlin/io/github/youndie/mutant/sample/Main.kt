package io.github.youndie.mutant.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * The sample in a browser: the same `MutantUiSample` as the desktop window, or one of B-73's scenes by
 * `?scene=<name>` ([benchScene]).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val scene =
        window.location.search
            .removePrefix("?")
            .split("&")
            .firstOrNull { it.startsWith("scene=") }
            ?.removePrefix("scene=")
    val bench = benchScene(scene)
    ComposeViewport(document.body!!) { bench?.invoke() ?: MutantUiSample() }
}
