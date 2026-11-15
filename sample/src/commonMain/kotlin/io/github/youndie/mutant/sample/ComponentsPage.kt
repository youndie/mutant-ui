package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantButtonState
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCircularProgress
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantDataTable
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
import io.github.youndie.mutant.components.MutantPullToRefresh
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSearchBar
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchResultLoading
import io.github.youndie.mutant.components.MutantSearchSuggestions
import io.github.youndie.mutant.components.MutantSegmentedButton
import io.github.youndie.mutant.components.MutantSlider
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTableCellStyle
import io.github.youndie.mutant.components.MutantTableColumn
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.pattern.MutantPattern
import io.github.youndie.mutant.pattern.mutantPattern
import io.github.youndie.mutant.surface.mutantTape

/** Every component, in sections as the design's pages group them, each live: it can be pressed, toggled, typed in. */
@Composable
internal fun ComponentsPage(strings: SampleStrings) {
    Column {
        Section(strings.sectionButtons) { Buttons(strings) }
        Section(strings.sectionIconButtons) { IconButtons(strings) }
        Section(strings.sectionChips) { Chips(strings) }
        Section(strings.sectionSelection) { Selection(strings) }
        Section(strings.sectionSlider) { Sliders(strings) }
        Section(strings.sectionSegmented) { Segmented() }
        Section(strings.sectionTooltip) { Tooltips(strings) }
        Section(strings.sectionField) { Field(strings) }
        Section(strings.sectionSearch) { Search(strings) }
        Section(strings.sectionTable) { Table(strings) }
        Section(strings.sectionCard) { Cards(strings) }
        Section(strings.sectionNav) { Navigation(strings) }
        Section(strings.sectionList) { ListRows(strings) }
        Section(strings.sectionSnackbar) { Snackbar(strings) }
        Section(strings.sectionProgress) { Progress() }
        Section(strings.sectionDialog) { Dialog(strings) }
        Section(strings.sectionSheet) { Sheet(strings) }
        Section(strings.sectionPull) { Pull(strings) }
        Section(strings.sectionEmpty) { EmptyStates(strings) }
        Section(strings.sectionIcons) { IconsAndPatterns(strings) }
    }
}

@Composable
private fun Buttons(s: SampleStrings) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MutantButton(s.play, {})
        MutantButton(s.loading, {}, state = MutantButtonState.Loading, key = "loading")
        MutantButton(s.play, {}, state = MutantButtonState.Success, key = "success")
        MutantButton(s.disabled, {}, enabled = false, key = "disabled")
        MutantOutlinedButton(s.record, {})
        MutantTextButton(s.reset, {})
    }
}

@Composable
private fun IconButtons(s: SampleStrings) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        MutantIconButton(MutantIcons.Edit, null, {})
        MutantIconButton(MutantIcons.Refresh, null, {})
        MutantIconButton(MutantIcons.Delete, null, {}, enabled = false)
        MutantFab(MutantIcons.Add, null, {})
        MutantExtendedFab(s.record, MutantIcons.Add, {})
    }
}

@Composable
private fun Chips(s: SampleStrings) {
    var chosen by remember { mutableIntStateOf(1) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(s.all, s.recordings, s.noise, s.favourites).forEachIndexed { i, label ->
            MutantChip(label, chosen == i, { chosen = i }, key = "chip$i")
        }
        MutantChip(s.disabled, false, {}, enabled = false, key = "chip-off")
    }
}

@Composable
private fun Selection(s: SampleStrings) {
    var check by remember { mutableStateOf(true) }
    var check2 by remember { mutableStateOf(false) }
    var radio by remember { mutableIntStateOf(1) }
    var clean by remember { mutableStateOf(false) }
    var loss by remember { mutableStateOf(true) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            MutantCheckbox(check2, { check2 = it }, label = s.headNoise)
            MutantCheckbox(check, { check = it }, label = s.autoTracking)
            MutantCheckbox(true, {}, label = s.disabled, enabled = false, key = "check-off")
        }
        Column {
            MutantRadioButton(radio == 0, { radio = 0 }, label = s.longPlay)
            MutantRadioButton(radio == 1, { radio = 1 }, label = s.standardPlay)
            MutantRadioButton(true, {}, label = s.disabled, enabled = false, key = "radio-off")
        }
        Column {
            MutantSwitch(clean, { clean = it }, label = s.clean)
            MutantSwitch(loss, { loss = it }, label = s.frameLoss)
            MutantSwitch(true, {}, label = s.disabled, enabled = false, key = "switch-off")
        }
    }
}

@Composable
private fun Sliders(s: SampleStrings) {
    var tracking by remember { mutableIntStateOf(3) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        MutantSlider(tracking, { tracking = it }, Modifier.width(300.dp), label = s.tracking)
        MutantSlider(-4, {}, Modifier.width(220.dp), label = s.sharpness, enabled = false, key = "slider-off")
    }
}

@Composable
private fun Segmented() {
    var speed by remember { mutableIntStateOf(0) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        MutantSegmentedButton(listOf("SP · 2 ч", "LP · 4 ч", "EP · 6 ч"), speed, { speed = it }, Modifier.width(312.dp))
        MutantSegmentedButton(
            listOf("SP · 2 ч", "LP · 4 ч"),
            0,
            {},
            Modifier.width(208.dp),
            enabled = false,
            key = "seg-off",
        )
    }
}

/** Hover, focus or long-press the anchors: the design draws the plates shown, not appearing. */
@Composable
private fun Tooltips(s: SampleStrings) {
    Row(horizontalArrangement = Arrangement.spacedBy(56.dp), modifier = Modifier.padding(top = 72.dp)) {
        MutantTooltipBox(s.share) { MutantIconButton(MutantIcons.Share, s.share, {}) }
        MutantTooltipBox("REC · 0:42:17", supportingText = s.recLeft) { MutantFab(MutantIcons.Add, null, {}) }
    }
}

@Composable
private fun Field(s: SampleStrings) {
    var value by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MutantTextField(value, { value = it }, Modifier.width(328.dp), prefix = s.search, placeholder = s.findRecording)
        MutantTextField(s.untitled, {}, Modifier.width(328.dp), isError = true, key = "field-error")
        MutantTextField("", {}, Modifier.width(328.dp), placeholder = s.disabled, enabled = false, key = "field-off")
    }
}

/** Type to filter, press a suggestion to take it; the row of cocoons stands for results still coming. */
@Composable
private fun Search(s: SampleStrings) {
    var query by remember(s) { mutableStateOf(s.night) }
    MutantSearchBar(query, { query = it }, Modifier.width(328.dp), prefix = s.search, clearLabel = s.clear) {
        MutantSearchSuggestions(listOf(s.nightToTuesday, s.nightAir), { query = it }, selected = query)
        Column {
            MutantSearchResult(
                s.nightTuesday,
                query,
                supporting = s.recordedOver,
                leadingIcon = MutantIcons.Schedule,
                onClick = {},
            )
            MutantDivider(key = "search")
            MutantSearchResultLoading(key = "search")
        }
    }
}

/** Press a row to select it: the row is the target, the box shows it. */
@Composable
private fun Table(s: SampleStrings) {
    var selected by remember { mutableStateOf(setOf(1)) }
    val rows =
        listOf(
            Triple(s.untitled, "46:00", "CH 03"),
            Triple(s.nightTuesday, "12:04", "CH 03"),
            Triple(s.testCard, "02:10", "CH 01"),
            Triple(s.noSignal, "00:42", "CH 99"),
        )
    MutantDataTable(
        rows,
        listOf(
            MutantTableColumn(s.columnRecord) { it.first },
            MutantTableColumn(s.columnLength, 90.dp, MutantTableCellStyle.Osd) { it.second },
            MutantTableColumn(s.columnChannel, 80.dp, MutantTableCellStyle.OsdSecondary) { it.third },
        ),
        Modifier.width(560.dp),
        selected,
        { i, on -> selected = if (on) selected + i else selected - i },
    )
}

@Composable
private fun Cards(s: SampleStrings) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        MutantCard(Modifier.width(328.dp), onClick = {}, key = "card") {
            MutantCardTitle(s.untitled)
            MutantCardSubtitle(s.sideA)
        }
        MutantCard(Modifier.width(328.dp), enabled = false, key = "card-off") {
            MutantCardTitle(s.nightTuesday)
            MutantCardSubtitle(s.recordedOver)
        }
        Column(Modifier.width(328.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MutantCocoon(Modifier.fillMaxWidth().height(80.dp), key = "0")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MutantCocoon(Modifier.size(40.dp), key = "1")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MutantCocoon(Modifier.width(200.dp).height(16.dp), key = "2")
                    MutantCocoon(Modifier.width(130.dp).height(16.dp), key = "3")
                }
            }
        }
    }
}

@Composable
private fun Navigation(s: SampleStrings) {
    var tab by remember { mutableIntStateOf(0) }
    var nav by remember { mutableIntStateOf(0) }
    Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        MutantTopAppBar(
            s.onAir,
            navigationIcon = { MutantIcon(MutantIcons.Menu, null) },
            status = s.status,
            windowInsets = WindowInsets(0),
        )
        MutantTabs(listOf(MutantTab(s.tracks), MutantTab(s.notes), MutantTab(s.copies)), tab, { tab = it })
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

@Composable
private fun ListRows(s: SampleStrings) {
    Column(Modifier.width(360.dp)) {
        MutantListItem(
            s.testCard,
            supporting = s.fourthGeneration,
            leadingIcon = MutantIcons.Home,
            trailingText = "12:04",
            onClick = {},
            key = "tile1",
        )
        MutantDivider(key = "0")
        MutantListItem(
            s.nightTuesday,
            supporting = s.recordedOver,
            leadingIcon = MutantIcons.Home,
            trailing = { MutantBadge(s.brandNew) },
            onClick = {},
            key = "tile2",
        )
        MutantDivider(key = "1")
    }
}

@Composable
private fun Snackbar(s: SampleStrings) {
    var shown by remember { mutableStateOf(true) }
    Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MutantTextButton(s.showSnackbar, { shown = true }, key = "show-snack")
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            MutantSnackbar(s.queued, visible = shown, actionLabel = s.undo, onAction = { shown = false })
        }
    }
}

@Composable
private fun Progress() {
    var value by remember { mutableFloatStateOf(.42f) }
    Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MutantLinearProgress(value)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MutantLinearProgress(Modifier.weight(1f))
            MutantCircularProgress()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0f, .42f, 1f).forEach { v ->
                MutantChip("${(v * 100).toInt()}%", value == v, { value = v }, key = "p$v")
            }
        }
    }
}

@Composable
private fun Dialog(s: SampleStrings) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.width(360.dp).height(320.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

@Composable
private fun Sheet(s: SampleStrings) {
    var open by remember { mutableStateOf(false) }
    var speed by remember { mutableIntStateOf(0) }
    Box(Modifier.width(360.dp).height(320.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MutantCard(Modifier.fillMaxWidth(), key = "sheet-card") {
                MutantCardTitle(s.untitled)
                MutantCardSubtitle(s.sideA)
            }
            MutantButton(s.openSheet, { open = true }, key = "open-sheet")
        }
        MutantBottomSheet(open, { open = false }, key = "sample-sheet") {
            MutantSegmentedButton(listOf("SP · 2 ч", "LP · 4 ч"), speed, {
                speed = it
            }, Modifier.fillMaxWidth(), key = "sheet-speed")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                MutantButton(s.done, { open = false }, key = "sheet-done")
            }
        }
    }
}

/** Pull the list down, by a finger or a mouse: it refreshes for two seconds. */
@Composable
private fun Pull(s: SampleStrings) {
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            kotlinx.coroutines.delay(2000)
            refreshing = false
        }
    }
    MutantPullToRefresh(
        refreshing,
        { refreshing = true },
        Modifier.width(360.dp).height(260.dp),
        refreshingLabel = s.refreshingWord,
        doneLabel = s.refreshedWord,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            repeat(8) { MutantListItem("${s.untitled} ${it + 1}", key = "pull-row-$it") }
        }
    }
}

@Composable
private fun EmptyStates(s: SampleStrings) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        MutantEmptyState(
            MutantIllustrationKind.Cassette,
            s.emptyTitle,
            Modifier.width(
                328.dp,
            ),
            text = s.emptyText,
            action = {
                MutantButton(s.record, {}, key = "empty-record")
            },
        )
        for (kind in MutantIllustrationKind.entries.drop(1)) MutantEmptyState(kind, kind.name, Modifier.width(240.dp))
    }
}

@Composable
private fun IconsAndPatterns(s: SampleStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for ((_, icon) in MutantIcons.all) MutantIcon(icon, null, size = 24.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (p in MutantPattern.entries) {
                Box(Modifier.size(96.dp).mutantPattern(p))
                Box(Modifier.size(96.dp).mutantPattern(p, quiet = true))
            }
        }
        Box(Modifier.width(328.dp).mutantTape()) {
            MutantCard(Modifier.fillMaxWidth(), key = "tape-card") {
                MutantCardTitle(s.tapeOver)
                MutantCardSubtitle(s.sideA)
            }
        }
    }
}
