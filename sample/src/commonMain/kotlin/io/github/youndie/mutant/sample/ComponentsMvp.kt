package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantButtonState
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCircularProgress
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.components.MutantExtendedFab
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind

// The sections of the design's MVP page, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

@Composable
internal fun Buttons(s: SampleStrings) {
    Variants(s) {
        Cell(s.vFilled) { MutantButton(s.play, {}, key = "b-filled") }
        Cell(s.vOutlined) { MutantOutlinedButton(s.record, {}, key = "b-outlined") }
        Cell(s.vText) { MutantTextButton(s.reset, {}, key = "b-text") }
    }
    States(s) {
        Cell(s.stRest) { MutantButton(s.play, {}, key = "b-rest") }
        Cell(s.stPressed) { MutantButton(s.play, {}, key = "b-pressed", interactionSource = held(press = true)) }
        Cell(s.stLoading) { MutantButton(s.loading, {}, state = MutantButtonState.Loading, key = "b-loading") }
        Cell(s.stSuccess) { MutantButton(s.play, {}, state = MutantButtonState.Success, key = "b-success") }
        Cell(s.stDisabled) { MutantButton(s.play, {}, enabled = false, key = "b-off") }
    }
}

@Composable
internal fun IconButtons(s: SampleStrings) {
    States(s) {
        Cell(s.stRest) { MutantIconButton(MutantIcons.Edit, s.rename, {}, key = "ib-rest") }
        Cell(s.stPressed) {
            MutantIconButton(
                MutantIcons.Refresh,
                s.refreshingWord,
                {},
                key = "ib-pressed",
                interactionSource = held(true),
            )
        }
        Cell(s.stDisabled) { MutantIconButton(MutantIcons.Delete, s.erase, {}, enabled = false, key = "ib-off") }
    }
}

@Composable
internal fun Fabs(s: SampleStrings) {
    Variants(s) {
        Cell(s.vFab) { MutantFab(MutantIcons.Add, s.newRecording, {}, key = "fab") }
        Cell(s.vExtended) { MutantExtendedFab(s.record, MutantIcons.Add, {}, key = "fab-ext") }
    }
    States(s) {
        Cell(s.stRest) { MutantFab(MutantIcons.Add, s.newRecording, {}, key = "fab-rest") }
        Cell(s.stPressed) {
            MutantFab(MutantIcons.Add, s.newRecording, {}, key = "fab-pressed", interactionSource = held(true))
        }
    }
}

@Composable
internal fun Chips(s: SampleStrings) {
    var chosen by remember { mutableIntStateOf(1) }
    States(s) {
        Cell(s.stRest) { MutantChip(s.noise, false, {}, key = "chip-rest") }
        Cell(s.stPressed) { MutantChip(s.noise, false, {}, key = "chip-pressed", interactionSource = held(true)) }
        Cell(s.stSelected) { MutantChip(s.recordings, true, {}, key = "chip-selected") }
        Cell(s.stDisabled) { MutantChip(s.favourites, false, {}, enabled = false, key = "chip-off") }
    }
    TryIt(s) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(s.all, s.recordings, s.noise, s.favourites).forEachIndexed { i, label ->
                MutantChip(label, chosen == i, { chosen = i }, key = "chip$i")
            }
        }
    }
}

@Composable
internal fun Checkboxes(s: SampleStrings) {
    var a by remember { mutableStateOf(false) }
    var b by remember { mutableStateOf(true) }
    States(s) {
        Cell(s.stUnchecked) { MutantCheckbox(a, { a = it }, label = s.headNoise, key = "cb-un") }
        Cell(s.stChecked) { MutantCheckbox(b, { b = it }, label = s.autoTracking, key = "cb-on") }
        Cell(s.stDisabled) { MutantCheckbox(true, {}, label = s.frameLoss, enabled = false, key = "cb-off") }
    }
}

@Composable
internal fun Radios(s: SampleStrings) {
    var speed by remember { mutableIntStateOf(1) }
    States(s) {
        Cell(s.stUnselected) { MutantRadioButton(speed == 0, { speed = 0 }, label = s.longPlay, key = "rb-a") }
        Cell(s.stSelected) { MutantRadioButton(speed == 1, { speed = 1 }, label = s.standardPlay, key = "rb-b") }
        Cell(s.stDisabled) { MutantRadioButton(true, {}, label = "EP", enabled = false, key = "rb-off") }
    }
}

@Composable
internal fun Switches(s: SampleStrings) {
    var a by remember { mutableStateOf(false) }
    var b by remember { mutableStateOf(true) }
    States(s) {
        Cell(s.stOff) { MutantSwitch(a, { a = it }, label = s.clean, key = "sw-off") }
        Cell(s.stOn) { MutantSwitch(b, { b = it }, label = s.frameLoss, key = "sw-on") }
        Cell(s.stDisabled) { MutantSwitch(true, {}, label = s.autoTracking, enabled = false, key = "sw-dis") }
    }
}

@Composable
internal fun Fields(s: SampleStrings) {
    var value by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(s.nightTue) }
    States(s) {
        Cell(s.stRest, FIELD) {
            MutantTextField(
                value,
                { value = it },
                Modifier.fillMaxWidth(),
                prefix = s.search,
                placeholder = s.findRecording,
                key = "tf-rest",
            )
        }
        Cell(s.stFocused, FIELD) {
            MutantTextField(
                focused,
                { focused = it },
                Modifier.fillMaxWidth(),
                prefix = s.name,
                key = "tf-focus",
                interactionSource = held(focus = true),
            )
        }
        Cell(s.stError, FIELD) {
            MutantTextField("99", {}, Modifier.fillMaxWidth(), prefix = s.channel, isError = true, key = "tf-error")
        }
        Cell(s.stDisabled, FIELD) {
            MutantTextField(s.untitled, {}, Modifier.fillMaxWidth(), prefix = s.name, enabled = false, key = "tf-off")
        }
    }
}

@Composable
internal fun Cards(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, CARD) {
            MutantCard(Modifier.fillMaxWidth(), onClick = {}, key = "card-rest") {
                MutantCardTitle(s.untitled)
                MutantCardSubtitle(s.sideA)
            }
        }
        Cell(s.stDisabled, CARD) {
            MutantCard(Modifier.fillMaxWidth(), enabled = false, key = "card-off") {
                MutantCardTitle(s.nightTuesday)
                MutantCardSubtitle(s.recordedOver)
            }
        }
    }
}

@Composable
internal fun Cocoons(s: SampleStrings) {
    Variants(s) {
        Cell(s.vBlock, CARD) { MutantCocoon(Modifier.fillMaxWidth().height(80.dp), key = "0") }
        Cell(s.vRow, CARD) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MutantCocoon(Modifier.size(40.dp), key = "1")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MutantCocoon(Modifier.width(200.dp).height(16.dp), key = "2")
                    MutantCocoon(Modifier.width(130.dp).height(16.dp), key = "3")
                }
            }
        }
    }
    States(s) { Cell(s.stLoading) { MutantCocoon(Modifier.width(160.dp).height(48.dp), key = "4") } }
}

@Composable
internal fun TopBars(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, BAR) {
            MutantTopAppBar(
                s.onAir,
                navigationIcon = { MutantIcon(MutantIcons.Menu, s.menu) },
                status = s.status,
                windowInsets = WindowInsets(0),
                actions = { MutantIcon(MutantIcons.Search, s.find) },
            )
        }
    }
}

@Composable
internal fun TabsSpec(s: SampleStrings) {
    var tab by remember { mutableIntStateOf(0) }
    States(s) {
        Cell(s.stRest, BAR) {
            MutantTabs(listOf(MutantTab(s.tracks), MutantTab(s.notes), MutantTab(s.copies)), tab, { tab = it })
        }
    }
}

@Composable
internal fun NavBars(s: SampleStrings) {
    var nav by remember { mutableIntStateOf(0) }
    States(s) {
        Cell(s.stRest, BAR) {
            MutantNavigationBar(
                listOf(
                    MutantNavigationItem(s.onAir, MutantIcons.Home),
                    MutantNavigationItem(s.queue, MutantIcons.Schedule, badge = "3"),
                    MutantNavigationItem(s.find, MutantIcons.Search),
                    MutantNavigationItem(s.settings, MutantIcons.Settings),
                ),
                nav,
                { nav = it },
                windowInsets = WindowInsets(0),
            )
        }
    }
}

@Composable
internal fun ListItems(s: SampleStrings) {
    Variants(s) {
        Cell(s.vTrailingText, BAR) {
            MutantListItem(
                s.testCard,
                supporting = s.fourthGeneration,
                leadingIcon = MutantIcons.Home,
                trailingText = "12:04",
                onClick = {},
                key = "tile1",
            )
        }
        Cell(s.vTrailingBadge, BAR) {
            MutantListItem(
                s.nightTuesday,
                supporting = s.recordedOver,
                leadingIcon = MutantIcons.Home,
                trailing = { MutantBadge(s.brandNew) },
                onClick = {},
                key = "tile2",
            )
        }
    }
    States(s) {
        Cell(s.stRest, BAR) { MutantListItem(s.untitled, supporting = s.sideA, onClick = {}, key = "tile3") }
    }
}

@Composable
internal fun Dividers(s: SampleStrings) {
    States(s) { Cell(s.stRest, BAR) { MutantDivider(key = "d0") } }
}

@Composable
internal fun Badges(s: SampleStrings) {
    States(s) {
        Cell(s.stRest) { MutantBadge(s.brandNew) }
        Cell(s.stRest) { MutantBadge("CH 03") }
    }
}

@Composable
internal fun Snackbars(s: SampleStrings) {
    var shown by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stShown, BAR) { MutantSnackbar(s.queued, actionLabel = s.undo, key = "snack-rest") }
    }
    TryIt(s) {
        Column(Modifier.width(BAR), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MutantTextButton(s.showSnackbar, { shown = true }, key = "show-snack")
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                MutantSnackbar(s.queued, visible = shown, actionLabel = s.undo, onAction = { shown = false })
            }
        }
    }
}

@Composable
internal fun LinearProgresses(s: SampleStrings) {
    var value by remember { mutableFloatStateOf(.42f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(300)
            value = if (value >= 1f) 0f else (value + .02f).coerceAtMost(1f)
        }
    }
    States(s) {
        Cell(s.stDeterminate, BAR) { MutantLinearProgress(value) }
        Cell(s.stIndeterminate, BAR) { MutantLinearProgress(Modifier.fillMaxWidth()) }
    }
}

@Composable
internal fun CircularProgresses(s: SampleStrings) {
    States(s) { Cell(s.stIndeterminate) { MutantCircularProgress() } }
}

@Composable
internal fun Dialogs(s: SampleStrings) {
    var open by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stOpen, STAGE_W) {
            Stage(STAGE_W, 300.dp) {
                MutantDialog(true, s.eraseQuestion, {}, text = s.eraseText) {
                    MutantDialogAction(s.cancel, {})
                    MutantDialogAction(s.erase, {}, primary = true)
                }
            }
        }
    }
    TryIt(s) {
        Stage(STAGE_W, 300.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MutantCard(Modifier.fillMaxWidth(), key = "dlg-card") {
                    MutantCardTitle(s.untitled)
                    MutantCardSubtitle(s.sideA)
                }
                MutantButton(s.openDialog, { open = true }, key = "open-dialog")
            }
            MutantDialog(open, s.eraseQuestion, { open = false }, text = s.eraseText) {
                MutantDialogAction(s.cancel, { open = false })
                MutantDialogAction(s.erase, { open = false }, primary = true)
            }
        }
    }
}

@Composable
internal fun EmptyStates(s: SampleStrings) {
    Variants(s) {
        for (kind in MutantIllustrationKind.entries.drop(1)) {
            Cell(kind.name) { MutantIllustration(kind) }
        }
    }
    States(s) {
        Cell(s.stRest, CARD) {
            MutantEmptyState(
                MutantIllustrationKind.Cassette,
                s.emptyTitle,
                Modifier.fillMaxWidth(),
                text = s.emptyText,
                action = { MutantButton(s.record, {}, key = "empty-record") },
            )
        }
    }
}
