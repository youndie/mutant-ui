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
import kotlin.math.roundToInt

@Composable
internal fun Recording(
    s: SampleStrings,
    t: Cassette,
    nav: DemoNav,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var queued by remember { mutableStateOf(true) }
    var listening by remember { mutableStateOf(false) }
    // The design's screen reads 12:04 of 46:00 played; every tape opens as far into its own length.
    var played by remember { mutableFloatStateOf(DESIGN_ELAPSED_S.toFloat() / DESIGN_LENGTH_S) }
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
                // B-174: the side and its line are the tape's own; the design's tape reads the design's words.
                val design = t.title == s.untitled
                BasicText(
                    if (design) s.sideAShort else t.side.substringBefore(" · "),
                    style = MutantTheme.textStyles.display(24.sp),
                )
                Osd(if (design) s.sideALine else "${t.length} · ${t.channel}")
                Spacer(Modifier.height(14.dp))
                // The design's bar with the elapsed time beside it in the on-screen-display amber, not the kit's percent.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MutantLinearProgress(played, Modifier.weight(1f), showValue = false)
                    Osd(
                        recordingClock(
                            (played * recordingSeconds(t.length)).roundToInt(),
                            t.length.count { it == ':' } > 1,
                        ),
                    )
                }
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

/** The design's Recording screen: 12:04 played of a 46:00 tape. */
private const val DESIGN_ELAPSED_S = 12 * 60 + 4
private const val DESIGN_LENGTH_S = 46 * 60

/** A tape's length, «46:00» or «1:58:10», in seconds. */
internal fun recordingSeconds(length: String): Int =
    length.split(':').fold(0) { total, part ->
        total * 60 +
            part.toInt()
    }

/** [seconds] as a tape's counter reads them: «12:04», or «0:31:12» on a tape of hours ([hours]). */
internal fun recordingClock(
    seconds: Int,
    hours: Boolean,
): String {
    val m = seconds / 60 % 60
    val sec = (seconds % 60).toString().padStart(2, '0')
    return if (hours) "${seconds / 3600}:${m.toString().padStart(2, '0')}:$sec" else "${seconds / 60}:$sec"
}
