package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes

/**
 * B-213: the Demo's phone alone, for a page to embed — the browser's `?scene=app`. The screen fills the window under
 * one row of chips that picks the screen and the palette; nothing else of the sample's sheet is shown. Reduce motion is
 * off whatever the platform says, as the sample's header has it, so the Tape and the bodies run in the embed.
 */
@Composable
fun DemoAppScene(strings: SampleStrings = remember { SampleStrings(SampleLanguage.English) }) {
    var shown by remember { mutableStateOf(DemoScreen.Library) }
    var kopiya by remember { mutableStateOf(false) }
    MutantTheme(colors = if (kopiya) MutantPalettes.Kopiya else MutantPalettes.Efir, reduceMotion = false) {
        Column(Modifier.fillMaxSize().background(MutantTheme.colors.page).testTag("demo-app")) {
            // Two rows: the five chips in one stand 437 dp wide, past the embed's 390.
            Column(Modifier.fillMaxWidth().testTag("demo-app-chips").padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (each in DemoScreen.entries) {
                        val label =
                            when (each) {
                                DemoScreen.Library -> strings.demoLibrary
                                DemoScreen.Recording -> strings.demoRecording
                                DemoScreen.Settings -> strings.settings
                            }
                        MutantChip(label, each == shown, { shown = each }, key = "app-${each.name}")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MutantChip(strings.themeEfir, !kopiya, { kopiya = false }, key = "app-efir")
                    MutantChip(strings.themeKopiya, kopiya, { kopiya = true }, key = "app-kopiya")
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                key(shown) { DemoScreenFill(shown, strings, Modifier.fillMaxSize()) }
            }
        }
    }
}
