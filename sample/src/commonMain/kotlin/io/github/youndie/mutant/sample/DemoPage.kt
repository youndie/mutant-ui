package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantDivider
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
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantTheme

/**
 * The Demo page's three screens, rebuilt from the public API as it stands (the owner's answer of 2026-11-14): the
 * library, a recording, and settings under a dialog. Where the page composes them with spacing of its own, the
 * components keep theirs.
 */
@Composable
internal fun DemoPage(strings: SampleStrings) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        for (screen in DemoScreen.entries) DemoScreenFrame(screen, strings)
    }
}

/** The page's three screens. */
internal enum class DemoScreen { Library, Recording, Settings }

/** One of the screens with its title over it, as the page and a test draw it. */
@Composable
internal fun DemoScreenFrame(
    screen: DemoScreen,
    strings: SampleStrings,
) {
    when (screen) {
        DemoScreen.Library -> Screen(strings.demoLibrary, "demo-library") { Library(strings) }
        DemoScreen.Recording -> Screen(strings.demoRecording, "demo-recording") { Recording(strings) }
        DemoScreen.Settings -> Screen(strings.demoSettings, "demo-settings") { Settings(strings) }
    }
}

/** A phone's screen as the page draws it: 360 × 720, its corners 28, ringed in black, the Tape over it. */
@Composable
private fun Screen(
    title: String,
    tag: String,
    content: @Composable () -> Unit,
) {
    val colors = MutantTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(title, style = TextStyle(color = colors.text, fontSize = 22.sp))
        Box(
            Modifier
                .size(360.dp, 720.dp)
                .border(2.dp, Color(0xFF05060C), RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(colors.background)
                .mutantTape()
                .testTag(tag),
        ) { content() }
    }
}

@Composable
private fun Osd(
    text: String,
    color: Color = MutantTheme.colors.textOsd,
) {
    // The kit has no public text styles (B-55): the platform's face stands in for Handjet.
    BasicText(text, style = TextStyle(color = color, fontSize = 18.sp))
}

@Composable
private fun Library(s: SampleStrings) {
    var chosen by remember { mutableIntStateOf(1) }
    var query by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            MutantTopAppBar(s.onAir, navigationIcon = {
                MutantIcon(MutantIcons.Menu, null)
            }, status = s.clock, windowInsets = WindowInsets(0))
            Spacer(Modifier.height(8.dp))
            MutantTextField(query, {
                query = it
            }, Modifier.fillMaxWidth(), prefix = s.search, placeholder = s.findRecording, key = "field")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(s.all, s.recordings, s.noise, s.favourites).forEachIndexed { i, label ->
                    MutantChip(label, chosen == i, { chosen = i }, key = "chipA$i")
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
                MutantCard(Modifier.fillMaxWidth(), onClick = {}, key = "cardA0") {
                    MutantCardTitle(s.untitled)
                    MutantCardSubtitle(s.sideA)
                }
                MutantCard(Modifier.fillMaxWidth(), onClick = {}, key = "cardA1") {
                    MutantCardTitle(s.nightTuesday)
                    MutantCardSubtitle(s.recordedOver)
                }
                MutantCocoon(Modifier.fillMaxWidth().height(80.dp), key = "0")
            }
            Spacer(Modifier.height(24.dp))
            MutantButton(s.playAllWide, {}, Modifier.fillMaxWidth(), key = "button")
        }
        MutantFab(MutantIcons.Add, null, {
        }, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 84.dp), key = "fab")
        MutantNavigationBar(
            listOf(
                MutantNavigationItem(s.onAir, MutantIcons.Home),
                MutantNavigationItem(s.queue, MutantIcons.Schedule, badge = "3"),
                MutantNavigationItem(s.find, MutantIcons.Search),
                MutantNavigationItem(s.settings, MutantIcons.Settings),
            ),
            0,
            {},
            Modifier.align(Alignment.BottomCenter).padding(8.dp),
            windowInsets = WindowInsets(0),
        )
    }
}

@Composable
private fun Recording(s: SampleStrings) {
    var tab by remember { mutableIntStateOf(0) }
    var queued by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            MutantTopAppBar(
                s.untitled,
                navigationIcon = { MutantIcon(MutantIcons.Back, null) },
                windowInsets = WindowInsets(0),
                actions = { MutantIcon(MutantIcons.Edit, null) },
            )
            MutantIllustration(
                MutantIllustrationKind.Cassette,
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp),
            )
            Spacer(Modifier.height(42.dp))
            BasicText(s.sideAShort, style = TextStyle(color = MutantTheme.colors.text, fontSize = 24.sp))
            Osd(s.sideALine)
            Spacer(Modifier.height(14.dp))
            MutantLinearProgress(.26f)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MutantButton(s.listen, {}, Modifier.weight(1f), key = "buttonB")
                MutantOutlinedButton(s.toQueue, { queued = true }, key = "button-o")
                MutantIconButton(MutantIcons.Share, null, {}, key = "iconbtn")
            }
            Spacer(Modifier.height(26.dp))
            MutantTabs(listOf(MutantTab(s.tracks), MutantTab(s.notes), MutantTab(s.copies)), tab, { tab = it })
            MutantListItem(
                s.testCard,
                supporting = s.fourthGeneration,
                leadingIcon = MutantIcons.Home,
                trailingText = "12:04",
                key = "tile1",
            )
            MutantDivider(key = "0")
            MutantListItem(
                s.nightTuesday,
                supporting = s.recordedOverShort,
                leadingIcon = MutantIcons.Home,
                trailing = { MutantBadge(s.brandNew) },
                key = "tile2",
            )
            MutantDivider(key = "1")
        }
        MutantSnackbar(
            s.queued,
            Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            visible = queued,
            actionLabel = s.undo,
            onAction = { queued = false },
        )
    }
}

@Composable
private fun Settings(s: SampleStrings) {
    var name by remember { mutableStateOf(s.nightTue) }
    var speed by remember { mutableIntStateOf(0) }
    var tracking by remember { mutableStateOf(true) }
    var clean by remember { mutableStateOf(false) }
    var noise by remember { mutableStateOf(true) }
    var loss by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            MutantTopAppBar(
                s.settings,
                navigationIcon = { MutantIcon(MutantIcons.Back, null) },
                windowInsets = WindowInsets(0),
            )
            Spacer(Modifier.height(12.dp))
            MutantTextField(name, { name = it }, Modifier.fillMaxWidth(), prefix = s.name, key = "fieldC0")
            Spacer(Modifier.height(30.dp))
            MutantTextField("99", {}, Modifier.fillMaxWidth(), prefix = s.channel, isError = true, key = "fieldC1")
            Spacer(Modifier.height(22.dp))
            Osd(s.noSignal99, MutantTheme.colors.textError)
            Spacer(Modifier.height(16.dp))
            Osd(s.speed)
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                listOf("SP", "LP", "EP").forEachIndexed {
                    i,
                    label,
                    ->
                    MutantRadioButton(speed == i, { speed = i }, label = label, key = "radio")
                }
            }
            MutantSwitch(tracking, { tracking = it }, Modifier.fillMaxWidth(), label = s.autoTracking, key = "switch")
            MutantSwitch(clean, { clean = it }, Modifier.fillMaxWidth(), label = s.clean, key = "switch2")
            MutantCheckbox(noise, { noise = it }, label = s.headNoise, key = "check")
            MutantCheckbox(loss, { loss = it }, label = s.frameLoss, key = "check2")
            MutantTextButton(s.resetSettings, {}, key = "reset")
        }
        MutantDialog(open, s.eraseQuestion, { open = false }, text = s.eraseNamed) {
            MutantDialogAction(s.cancel, { open = false })
            MutantDialogAction(s.erase, { open = false }, primary = true)
        }
    }
}
