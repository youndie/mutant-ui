package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.pattern.MutantPattern
import io.github.youndie.mutant.pattern.mutantPattern
import io.github.youndie.mutant.surface.MutantKeyframeLoss
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes

// The sections of Foundations, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

/** Each palette under its own theme, on its own ground, keeping the header's switches (B-174). */
@Composable
internal fun Themes(s: SampleStrings) {
    val header = LocalSampleSettings.current
    Variants(s) {
        for ((name, colors) in listOf(s.themeEfir to MutantPalettes.Efir, s.themeKopiya to MutantPalettes.Kopiya)) {
            Cell(name, CARD) {
                MutantTheme(
                    colors = colors,
                    reduceMotion = MutantTheme.reduceMotion,
                    cleanSignal = MutantTheme.cleanSignal,
                    steppedMotion = header.steppedMotion,
                    mosaicPastContour = header.mosaicPastContour,
                ) {
                    MutantScreenClock {
                        Column(
                            Modifier.fillMaxWidth().background(MutantTheme.colors.page).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            MutantCard(Modifier.fillMaxWidth(), onClick = {}, key = "theme-card-$name") {
                                MutantCardTitle(s.untitled)
                                MutantCardSubtitle(s.sideA)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MutantButton(s.play, {}, key = "theme-b-$name")
                                MutantChip(s.noise, true, {}, key = "theme-c-$name")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IconsSpec(s: SampleStrings) {
    Variants(s) {
        for ((name, icon) in MutantIcons.all) Cell(name, ICON_CELL) { MutantIcon(icon, name, size = 24.dp) }
    }
}

@Composable
internal fun Patterns(s: SampleStrings) {
    Variants(s) {
        for (p in MutantPattern.entries) {
            Cell("${p.name} · ${s.vLoud}") { Box(Modifier.size(96.dp).mutantPattern(p)) }
            Cell("${p.name} · ${s.vQuiet}") { Box(Modifier.size(96.dp).mutantPattern(p, quiet = true)) }
        }
    }
}

@Composable
internal fun Tapes(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, CARD) {
            Box(Modifier.fillMaxWidth().mutantTape()) {
                MutantCard(Modifier.fillMaxWidth(), key = "tape-card") {
                    MutantCardTitle(s.tapeOver)
                    MutantCardSubtitle(s.sideA)
                }
            }
        }
    }
}

@Composable
internal fun KeyframeLosses(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, STAGE_W) {
            Stage(STAGE_W, 200.dp) {
                MutantKeyframeLoss(Modifier.fillMaxSize(), MutantTheme.colors.page) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MutantCard(Modifier.fillMaxWidth(), key = "loss-card") {
                            MutantCardTitle(s.nightTuesday)
                            MutantCardSubtitle(s.recordedOver)
                        }
                        MutantButton(s.play, {}, key = "loss-button")
                    }
                }
            }
        }
    }
}
