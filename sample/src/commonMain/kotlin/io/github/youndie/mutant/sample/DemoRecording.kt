package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantSlider
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.coroutines.delay

@Composable
internal fun Recording(
    s: SampleStrings,
    t: Cassette,
    nav: DemoNav,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var queued by remember { mutableStateOf(true) }
    var listening by remember { mutableStateOf(false) }
    var played by remember { mutableFloatStateOf(.26f) }
    var volume by rememberSaveable { mutableIntStateOf(6) }
    var share by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(t.title) }
    LaunchedEffect(listening) {
        while (listening) {
            delay(250)
            played = if (played >= 1f) 0f else (played + .004f).coerceAtMost(1f)
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            MutantTopAppBar(
                name,
                navigationIcon = { BarAction(MutantIcons.Back, s.back) { nav.open = null } },
                windowInsets = WindowInsets(0),
                actions = {
                    BarAction(MutantIcons.Edit, s.rename) {
                        queued = false
                        rename = true
                    }
                },
            )
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MutantIllustration(
                    MutantIllustrationKind.Cassette,
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp),
                )
                Spacer(Modifier.height(42.dp))
                BasicText(s.sideAShort, style = MutantTheme.textStyles.display(24.sp))
                Osd(s.sideALine)
                Spacer(Modifier.height(14.dp))
                MutantLinearProgress(played)
                Spacer(Modifier.height(22.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MutantButton(if (listening) s.pause else s.listen, {
                        listening = !listening
                    }, Modifier.weight(1f), key = "buttonB")
                    MutantOutlinedButton(s.toQueue, { queued = true }, key = "button-o")
                    MutantTooltipBox(s.shareWord) {
                        MutantIconButton(MutantIcons.Share, s.shareWord, {
                            queued = false
                            share = true
                        }, key = "iconbtn")
                    }
                }
                Spacer(Modifier.height(26.dp))
                MutantTabs(listOf(MutantTab(s.tracks), MutantTab(s.notes), MutantTab(s.copies)), tab, { tab = it })
                when (tab) {
                    0 -> {
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
                        MutantListItem(
                            s.staticBetween,
                            supporting = s.noiseNine,
                            leadingIcon = MutantIcons.Home,
                            trailingText = "09:02",
                            key = "tile3",
                        )
                        MutantDivider(key = "2")
                    }

                    1 -> {
                        Spacer(Modifier.height(16.dp))
                        BasicText(
                            s.notesText,
                            style = MutantTheme.textStyles.text(15.sp),
                        )
                    }

                    else -> {
                        for ((i, copy) in listOf(s.master, s.secondGeneration, s.fourthGeneration).withIndex()) {
                            MutantListItem(copy, supporting = t.channel, leadingIcon = MutantIcons.Done, key = "copy$i")
                            MutantDivider(key = "c$i")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                MutantSlider(volume, { volume = it }, Modifier.fillMaxWidth(), label = s.volume, valueRange = 0..10)
                Spacer(Modifier.height(96.dp))
            }
        }
        MutantSnackbar(
            s.queued,
            Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            visible = queued,
            actionLabel = s.undo,
            onAction = { queued = false },
        )
        MutantBottomSheet(share, { share = false }, windowInsets = WindowInsets(0), key = "demo-share") {
            Osd(s.shareWord, size = 18)
            for ((i, way) in listOf(s.copyToBlank, s.sendACopy, s.printLabel).withIndex()) {
                MutantListItem(way, onClick = { share = false }, key = "share$i")
            }
        }
        MutantBottomSheet(rename, { rename = false }, windowInsets = WindowInsets(0), key = "demo-rename") {
            MutantTextField(name, { name = it }, Modifier.fillMaxWidth(), prefix = s.name, key = "rename-field")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                MutantButton(s.done, { rename = false }, key = "rename-done")
            }
        }
    }
}
