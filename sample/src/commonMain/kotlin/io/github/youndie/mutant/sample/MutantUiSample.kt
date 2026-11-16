package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes

/** The sample's pages: every component as the design's pages group them, and the three demo screens. */
enum class SamplePage { Components, Demo }

/** What the header switches. */
class SampleSettings(
    print: Boolean = false,
    reduceMotion: Boolean = false,
    cleanSignal: Boolean = false,
    steppedMotion: Boolean = false,
    language: SampleLanguage = SampleLanguage.English,
    page: SamplePage = SamplePage.Components,
) {
    var print by mutableStateOf(print)
    var reduceMotion by mutableStateOf(reduceMotion)
    var cleanSignal by mutableStateOf(cleanSignal)
    var steppedMotion by mutableStateOf(steppedMotion)
    var language by mutableStateOf(language)
    var page by mutableStateOf(page)
}

/**
 * The sample's root, shared by the desktop window and the browser: a header that switches the theme («Эфир» or
 * «Копия»), reduce motion, «Чистый сигнал», the language (English first, research D11) and the page, over the page.
 * The Tape runs over the whole of it. [scroll] is the page's, handed in by a host that reads it (B-72's recorder).
 */
@Composable
fun MutantUiSample(
    settings: SampleSettings = remember { SampleSettings() },
    scroll: ScrollState = rememberScrollState(),
) {
    val strings = remember(settings.language) { SampleStrings(settings.language) }
    MutantTheme(
        colors = if (settings.print) MutantPalettes.Kopiya else MutantPalettes.Efir,
        reduceMotion = settings.reduceMotion,
        cleanSignal = settings.cleanSignal,
        steppedMotion = settings.steppedMotion,
    ) {
        val colors = MutantTheme.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.page)
                // B-101: on the Demo page each phone is a screen and carries its own Tape; the page's over it doubled it.
                .then(if (settings.page == SamplePage.Demo) Modifier else Modifier.mutantTape())
                .testTag("sample-root"),
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(24.dp)) {
                Header(settings, strings)
                when (settings.page) {
                    SamplePage.Components -> ComponentsPage(strings)
                    SamplePage.Demo -> DemoPage(strings)
                }
            }
        }
    }
}

@Composable
private fun Header(
    settings: SampleSettings,
    strings: SampleStrings,
) {
    val colors = MutantTheme.colors
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(strings.kit, style = TextStyle(color = colors.text, fontSize = 28.sp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MutantChip(strings.themeEfir, !settings.print, { settings.print = false }, key = "theme-efir")
            MutantChip(strings.themeKopiya, settings.print, { settings.print = true }, key = "theme-kopiya")
            MutantChip("EN", settings.language == SampleLanguage.English, {
                settings.language = SampleLanguage.English
            }, key = "lang-en")
            MutantChip("RU", settings.language == SampleLanguage.Russian, {
                settings.language = SampleLanguage.Russian
            }, key = "lang-ru")
            MutantChip(strings.pageComponents, settings.page == SamplePage.Components, {
                settings.page =
                    SamplePage.Components
            }, key = "page-c")
            MutantChip(
                strings.pageDemo,
                settings.page == SamplePage.Demo,
                { settings.page = SamplePage.Demo },
                key = "page-d",
            )
            MutantSwitch(
                settings.reduceMotion,
                { settings.reduceMotion = it },
                label = strings.reduceMotion,
                key = "reduce",
            )
            MutantSwitch(
                settings.cleanSignal,
                { settings.cleanSignal = it },
                label = strings.cleanSignal,
                key = "clean",
            )
            MutantSwitch(
                settings.steppedMotion,
                { settings.steppedMotion = it },
                label = strings.designSteps,
                key = "steps",
            )
        }
    }
}
