package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCarousel
import io.github.youndie.mutant.components.MutantCarouselItem
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantSegmentedButton
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.surface.MutantAppear
import io.github.youndie.mutant.surface.MutantScreen
import io.github.youndie.mutant.theme.MutantTheme

@Composable
internal fun Library(
    s: SampleStrings,
    tapes: List<Cassette>,
    nav: DemoNav,
    bar: @Composable BoxScope.() -> Unit,
) {
    var chosen by rememberSaveable { mutableIntStateOf(1) }
    var query by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var speed by remember { mutableIntStateOf(0) }
    val shown =
        tapes.withIndex().filter { (_, t) ->
            val filed =
                when (chosen) {
                    0 -> true
                    3 -> t.favourite
                    else -> t.kind == chosen
                }
            filed && (query.isBlank() || t.title.contains(query.trim(), ignoreCase = true))
        }
    Box(Modifier.fillMaxSize()) {
        // B-180: the screen tunes in as the navigation bar's pill starts for it; the bar stands outside, and moves.
        MutantScreen(nav.tab) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                TabBar(s.onAir, s, nav, status = s.clock)
                TabScroll {
                    Spacer(Modifier.height(8.dp))
                    MutantTextField(
                        query,
                        { query = it },
                        Modifier.fillMaxWidth(),
                        prefix = s.search,
                        placeholder = s.findRecording,
                        key = "field",
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(s.all, s.recordings, s.noise, s.favourites).forEachIndexed { i, label ->
                            MutantChip(label, chosen == i, { chosen = i }, key = "chipA$i")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // B-180: what a chip files tunes in as the chip starts to pour; what it filed goes in one frame.
                    MutantScreen(chosen) {
                        // B-179: what takes the other's place tunes in; the one it replaces goes in one frame.
                        if (shown.isEmpty()) {
                            MutantAppear {
                                MutantEmptyState(
                                    MutantIllustrationKind.Cassette,
                                    s.emptyTitle,
                                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    text = s.emptyText,
                                    cassetteLabel = s.cassetteLabel,
                                )
                            }
                        } else {
                            MutantAppear {
                                Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
                                    for ((i, t) in shown.take(2)) TapeCard(t, "cardA$i") { nav.open = i }
                                    // The third tape is still being copied: its card has not hatched yet.
                                    MutantCocoon(Modifier.fillMaxWidth().height(80.dp), key = "0")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    MutantButton(s.playAllWide, {
                        shown.firstOrNull()?.let { nav.open = it.index }
                    }, Modifier.fillMaxWidth(), key = "button")
                    Spacer(Modifier.height(32.dp))
                    Osd(s.recentlyOnAir, MutantTheme.colors.textSecondary, size = 18)
                    Spacer(Modifier.height(12.dp))
                    val recent = listOf(6, 3, 2)
                    MutantCarousel(
                        recent.map { MutantCarouselItem(tapes[it].title, tapes[it].side, tapes[it].channel) },
                        Modifier.fillMaxWidth(),
                        onClick = { nav.open = recent[it] },
                        key = "demo-car",
                        indicatorKey = "demo-cd",
                    )
                    MutantScreen(chosen) {
                        if (shown.size > 2) {
                            Column {
                                Spacer(Modifier.height(32.dp))
                                Osd(s.onTheShelf, MutantTheme.colors.textSecondary, size = 18)
                                Spacer(Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
                                    for ((i, t) in shown.drop(2)) TapeCard(t, "cardA$i") { nav.open = i }
                                }
                            }
                        }
                    }
                }
            }
        }
        bar()
        // The button is the screen's own: it tunes in with it.
        MutantScreen(nav.tab, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 84.dp)) {
            MutantTooltipBox(s.newRecording, supportingText = s.recLeft, key = s.newRecording) {
                MutantFab(MutantIcons.Add, s.newRecording, { sheet = true }, key = "fab")
            }
        }
        MutantBottomSheet(sheet, { sheet = false }, windowInsets = WindowInsets(0), key = "demo-new") {
            BasicText(s.newRecording, style = MutantTheme.textStyles.display(24.sp))
            Osd(s.speed, size = 18)
            MutantSegmentedButton(listOf("SP · 2 h", "LP · 4 h", "EP · 6 h").localised(s), speed, {
                speed = it
            }, Modifier.fillMaxWidth(), key = "demo-speed")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                MutantButton(s.record, { sheet = false }, key = "demo-record")
            }
        }
    }
}

/** The speeds' hours, in the language's letter. */
private fun List<String>.localised(s: SampleStrings) =
    if (s.language == SampleLanguage.Russian) map { it.replace(" h", " ч") } else this

@Composable
private fun TapeCard(
    t: Cassette,
    key: String,
    onClick: () -> Unit,
) {
    MutantCard(Modifier.fillMaxWidth(), onClick = onClick, key = key) {
        MutantCardTitle(t.title)
        MutantCardSubtitle(t.side)
    }
}
