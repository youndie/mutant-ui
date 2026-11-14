package io.github.youndie.mutant.sample

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import io.github.youndie.mutant.theme.MutantTheme

/** The three demo screens (B-37's second iteration). */
@Composable
internal fun DemoPage(strings: SampleStrings) {
    Section(strings.demoComing) {
        BasicText(strings.demoComing, style = TextStyle(color = MutantTheme.colors.textSecondary))
    }
}
