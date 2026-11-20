package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantDrawerItem
import io.github.youndie.mutant.components.MutantDropdown
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantNavigationDrawer
import io.github.youndie.mutant.components.MutantPullToRefresh
import io.github.youndie.mutant.components.MutantSegmentedButton
import io.github.youndie.mutant.components.MutantSlider
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.icons.MutantIcons

// The sections of the design's v1 page, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

@Composable
internal fun Sliders(s: SampleStrings) {
    var tracking by remember { mutableIntStateOf(3) }
    var sharp by remember { mutableIntStateOf(-2) }
    States(s) {
        Cell(s.stRest, FIELD) {
            MutantSlider(tracking, { tracking = it }, Modifier.fillMaxWidth(), label = s.tracking, key = "sl-rest")
        }
        Cell(s.stDragging, FIELD) {
            MutantSlider(
                sharp,
                { sharp = it },
                Modifier.fillMaxWidth(),
                label = s.sharpness,
                key = "sl-drag",
                interactionSource = held(press = true),
            )
        }
        Cell(s.stDisabled, FIELD) {
            MutantSlider(-4, {}, Modifier.fillMaxWidth(), label = s.sharpness, enabled = false, key = "sl-off")
        }
    }
}

@Composable
internal fun Segments(s: SampleStrings) {
    var speed by remember { mutableIntStateOf(0) }
    val hours =
        listOf("SP · 2 h", "LP · 4 h", "EP · 6 h").map {
            if (s.language ==
                SampleLanguage.Russian
            ) {
                it.replace(" h", " ч")
            } else {
                it
            }
        }
    States(s) {
        Cell(s.stRest, FIELD) { MutantSegmentedButton(hours, speed, { speed = it }, Modifier.fillMaxWidth()) }
        Cell(s.stDisabled, FIELD) {
            MutantSegmentedButton(hours.take(2), 0, {}, Modifier.fillMaxWidth(), enabled = false, key = "seg-off")
        }
    }
}

@Composable
internal fun Tooltips(s: SampleStrings) {
    Variants(s) {
        Cell("${s.vPlain}: ${s.hover}") {
            MutantTooltipBox(s.shareWord) { MutantIconButton(MutantIcons.Share, s.shareWord, {}, key = "tt-a") }
        }
        Cell("${s.vSupporting}: ${s.hover}") {
            MutantTooltipBox("REC · 0:42:17", supportingText = s.recLeft) {
                MutantFab(MutantIcons.Add, s.record, {}, key = "tt-b")
            }
        }
    }
}

@Composable
internal fun Menus(s: SampleStrings) {
    var chosen by remember { mutableIntStateOf(0) }
    TryIt(s) { MutantDropdown(s.setting, s.menuItems, chosen, { chosen = it }) }
}

@Composable
internal fun Drawers(s: SampleStrings) {
    var channel by remember { mutableIntStateOf(2) }
    var open by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stOpen, STAGE_W) {
            Stage(STAGE_W, 320.dp) {
                MutantNavigationDrawer(
                    true,
                    {},
                    s.channelNames.mapIndexed { i, name -> MutantDrawerItem("CH 0${i + 1}", name) },
                    channel,
                    { channel = it },
                    title = s.channels,
                    key = "spec-drawer",
                )
            }
        }
    }
    TryIt(s) {
        Stage(STAGE_W, 320.dp) {
            Box(Modifier.padding(16.dp)) { MutantButton(s.openDrawer, { open = true }, key = "open-drawer") }
            MutantNavigationDrawer(
                open,
                { open = false },
                s.channelNames.mapIndexed { i, name -> MutantDrawerItem("CH 0${i + 1}", name) },
                channel,
                {
                    channel = it
                    open = false
                },
                title = s.channels,
                key = "sample-drawer",
            )
        }
    }
}

@Composable
internal fun Sheets(s: SampleStrings) {
    var speed by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(false) }
    val speeds =
        listOf("SP · 2 h", "LP · 4 h").map {
            if (s.language ==
                SampleLanguage.Russian
            ) {
                it.replace(" h", " ч")
            } else {
                it
            }
        }
    States(s) {
        Cell(s.stOpen, STAGE_W) {
            Stage(STAGE_W, 320.dp) {
                MutantBottomSheet(true, {}, windowInsets = WindowInsets(0), key = "spec-sheet") {
                    Osd(s.speed, size = 16)
                    MutantSegmentedButton(speeds, speed, { speed = it }, Modifier.fillMaxWidth(), key = "sheet-speed")
                }
            }
        }
    }
    TryIt(s) {
        Stage(STAGE_W, 320.dp) {
            Box(Modifier.padding(16.dp)) { MutantButton(s.openSheet, { open = true }, key = "open-sheet") }
            MutantBottomSheet(open, { open = false }, windowInsets = WindowInsets(0), key = "sample-sheet") {
                MutantSegmentedButton(speeds, speed, { speed = it }, Modifier.fillMaxWidth(), key = "sheet-speed2")
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    MutantButton(s.done, { open = false }, key = "sheet-done")
                }
            }
        }
    }
}

@Composable
internal fun Pulls(s: SampleStrings) {
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            kotlinx.coroutines.delay(2000)
            refreshing = false
        }
    }
    States(s) {
        Cell(s.stRefreshing, STAGE_W) {
            Stage(STAGE_W, 200.dp) {
                MutantPullToRefresh(
                    true,
                    {},
                    Modifier.fillMaxSize(),
                    refreshingLabel = s.refreshingWord,
                    doneLabel = s.refreshedWord,
                    key = "ptr-held",
                ) {
                    Column { repeat(3) { MutantListItem("${s.untitled} ${it + 1}", key = "held-row-$it") } }
                }
            }
        }
    }
    TryIt(s) {
        Cell(s.pullHint, STAGE_W) {
            Stage(STAGE_W, 260.dp) {
                MutantPullToRefresh(
                    refreshing,
                    { refreshing = true },
                    Modifier.fillMaxSize(),
                    refreshingLabel = s.refreshingWord,
                    doneLabel = s.refreshedWord,
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        repeat(8) { MutantListItem("${s.untitled} ${it + 1}", key = "pull-row-$it") }
                    }
                }
            }
        }
    }
}
