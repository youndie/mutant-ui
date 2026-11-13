package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle

/**
 * The sample's root, shared by the desktop window and the browser. Until the tokens (B-08) and the
 * theme (B-09) exist it is the «Эфир» background with the kit's name; the colours are the tokens'
 * `background` and `text` of «Эфир», written here once and replaced by the tokens.
 */
@Composable
fun MutantUiSample() {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF10142A)).testTag("sample-root"),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("mutant-ui", style = TextStyle(color = Color(0xFFE9F2E4)))
    }
}
