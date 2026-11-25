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
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantDatePicker
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantDropdown
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSegmentedButton
import io.github.youndie.mutant.components.MutantSlider
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTimePicker
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.MutantScreen
import io.github.youndie.mutant.theme.MutantTheme

@Composable
internal fun Settings(
    s: SampleStrings,
    nav: DemoNav,
    bar: @Composable BoxScope.() -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(s.nightTue) }
    var speed by rememberSaveable { mutableIntStateOf(0) }
    var tracking by rememberSaveable { mutableStateOf(true) }
    var clean by rememberSaveable { mutableStateOf(false) }
    var noise by rememberSaveable { mutableStateOf(true) }
    var loss by rememberSaveable { mutableStateOf(false) }
    var trackingLevel by rememberSaveable { mutableIntStateOf(-2) }
    var picture by rememberSaveable { mutableIntStateOf(0) }
    var sleep by rememberSaveable { mutableIntStateOf(0) }
    var timer by remember { mutableStateOf(false) }
    // B-174: the timer's day is kept with its month and year; the calendar's month is only where it stands.
    var day by rememberSaveable { mutableIntStateOf(24) }
    var dayMonth by rememberSaveable { mutableIntStateOf(11) }
    var dayYear by rememberSaveable { mutableIntStateOf(2026) }
    var month by rememberSaveable { mutableIntStateOf(11) }
    var year by rememberSaveable { mutableIntStateOf(2026) }
    var hour by rememberSaveable { mutableIntStateOf(2) }
    var minute by rememberSaveable { mutableIntStateOf(40) }
    Box(Modifier.fillMaxSize()) {
        // B-180: the screen tunes in as the navigation bar's pill starts for it; the bar stands outside, and moves.
        MutantScreen(nav.tab) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                TabBar(s.settings, s, nav)
                TabScroll {
                    Spacer(Modifier.height(12.dp))
                    MutantTextField(name, { name = it }, Modifier.fillMaxWidth(), prefix = s.name, key = "fieldC0")
                    Spacer(Modifier.height(30.dp))
                    MutantTextField(
                        "99",
                        {},
                        Modifier.fillMaxWidth(),
                        prefix = s.channel,
                        isError = true,
                        key = "fieldC1",
                    )
                    Spacer(Modifier.height(22.dp))
                    // The page's error line: 18 px, tracked .5.
                    Osd(s.noSignal99, MutantTheme.colors.textError, size = 18, tracking = .5f)
                    Spacer(Modifier.height(16.dp))
                    Osd(s.speed)
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        // B-206: an item of a list takes its key from what it is, here its label. The kit's default is
                        // the body's place, which in a loop is its index: a removal above would hand its body on.
                        listOf("SP", "LP", "EP").forEachIndexed { i, label ->
                            MutantRadioButton(speed == i, { speed = i }, label = label, key = "speed-$label")
                        }
                    }
                    MutantSwitch(
                        tracking,
                        { tracking = it },
                        Modifier.fillMaxWidth(),
                        label = s.autoTracking,
                        key = "switch",
                    )
                    MutantSwitch(clean, { clean = it }, Modifier.fillMaxWidth(), label = s.clean, key = "switch2")
                    MutantCheckbox(noise, { noise = it }, label = s.headNoise, key = "check")
                    MutantCheckbox(loss, { loss = it }, label = s.frameLoss, key = "check2")
                    Spacer(Modifier.height(20.dp))
                    MutantSlider(
                        trackingLevel,
                        { trackingLevel = it },
                        Modifier.fillMaxWidth(),
                        label = s.tracking,
                        key = "demo-tracking",
                    )
                    Spacer(Modifier.height(24.dp))
                    MutantDropdown(s.picture, s.pictureModes, picture, { picture = it }, key = "demo-picture")
                    Spacer(Modifier.height(24.dp))
                    Osd(s.sleepTimer)
                    Spacer(Modifier.height(10.dp))
                    MutantSegmentedButton(
                        s.sleepOptions,
                        sleep,
                        { sleep = it },
                        Modifier.fillMaxWidth(),
                        key = "demo-sleep",
                    )
                    Spacer(Modifier.height(16.dp))
                    MutantListItem(
                        s.timerRecording,
                        supporting = s.timerLine(day, dayMonth, hour, minute),
                        leadingIcon = MutantIcons.Schedule,
                        onClick = { timer = true },
                        key = "demo-timer",
                    )
                    Spacer(Modifier.height(8.dp))
                    MutantTextButton(s.resetSettings, {
                        tracking = true
                        clean = false
                        noise = true
                        loss = false
                        speed = 0
                        trackingLevel = -2
                    }, key = "reset")
                }
            }
        }
        bar()
        MutantBottomSheet(timer, { timer = false }, windowInsets = WindowInsets(0), key = "demo-timer-sheet") {
            MutantDatePicker(
                year,
                month,
                SampleDate(dayYear, dayMonth, day).dayIn(year, month),
                {
                    day = it
                    dayMonth = month
                    dayYear = year
                },
                { y, m ->
                    year = y
                    month = m
                },
                monthNames = s.monthNames,
                weekdayNames = s.weekdayNames,
                previousLabel = s.previousMonth,
                nextLabel = s.nextMonth,
                key = "demo-cal",
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                MutantTimePicker(hour, minute, { h, m ->
                    hour = h
                    minute = m
                }, key = "demo-tp")
                Spacer(Modifier.weight(1f))
                MutantButton(s.done, { timer = false }, key = "timer-done")
            }
        }
        // B-174: the dialog stands open as the screen opens, so no control opened it; the focus goes to the screen's first.
        MutantDialog(
            nav.erasing,
            s.eraseQuestion,
            { nav.erasing = false },
            text = s.eraseNamed,
            returnFocusTo = nav.menu,
        ) {
            MutantDialogAction(s.cancel, { nav.erasing = false })
            MutantDialogAction(s.erase, { nav.erasing = false }, primary = true)
        }
    }
}
