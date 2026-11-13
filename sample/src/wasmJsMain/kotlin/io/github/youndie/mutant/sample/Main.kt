package io.github.youndie.mutant.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/** The sample in a browser: the same `MutantUiSample` as the desktop window. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) { MutantUiSample() }
}
