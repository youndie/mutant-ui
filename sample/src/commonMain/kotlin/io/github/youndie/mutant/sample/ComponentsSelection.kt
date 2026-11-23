package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantDatePicker
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSearchBar
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchResultLoading
import io.github.youndie.mutant.components.MutantSearchSuggestions
import io.github.youndie.mutant.components.MutantSlider
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTimePicker
import io.github.youndie.mutant.icons.MutantIcons

// The sections of Selection and input, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

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
internal fun Searches(s: SampleStrings) {
    var query by remember(s) { mutableStateOf(s.night) }
    var empty by remember { mutableStateOf("") }
    Variants(s) {
        Cell(s.vSuggestions, FIELD) {
            MutantSearchBar(empty, { empty = it }, Modifier.fillMaxWidth(), prefix = s.search, clearLabel = s.clear) {
                MutantSearchSuggestions(listOf(s.nightToTuesday, s.nightAir), { empty = it })
            }
        }
        Cell(s.vResults, FIELD) {
            MutantSearchBar(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                prefix = s.search,
                clearLabel = s.clear,
                key = "search-b",
            ) {
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
    }
    States(s) {
        Cell(s.stFocused, FIELD) {
            MutantSearchBar(
                s.night,
                {},
                Modifier.fillMaxWidth(),
                prefix = s.search,
                clearLabel = s.clear,
                key = "search-f",
                interactionSource = held(focus = true),
            )
        }
    }
}

@Composable
internal fun DatePickers(s: SampleStrings) {
    // B-174: the chosen day is a date, kept with its month; the month shown is another state, so the 13th chosen in
    // November is not chosen in every month the arrows show.
    var chosen by remember { mutableStateOf(SampleDate(2026, 11, 13)) }
    var month by remember { mutableIntStateOf(11) }
    var year by remember { mutableIntStateOf(2026) }
    States(s) {
        Cell(s.stRest) {
            MutantDatePicker(
                year,
                month,
                chosen.dayIn(year, month),
                { chosen = SampleDate(year, month, it) },
                { y, m ->
                    year = y
                    month = m
                },
                monthNames = s.monthNames,
                weekdayNames = s.weekdayNames,
                previousLabel = s.previousMonth,
                nextLabel = s.nextMonth,
            )
        }
    }
}

@Composable
internal fun TimePickers(s: SampleStrings) {
    var hour by remember { mutableIntStateOf(21) }
    var minute by remember { mutableIntStateOf(30) }
    States(s) {
        Cell(s.stRest) {
            MutantTimePicker(hour, minute, { h, m ->
                hour = h
                minute = m
            }, hourLabel = s.hours, minuteLabel = s.minutes)
        }
    }
}

/** A chosen day with its month and year, as a date picker's caller keeps it. */
internal data class SampleDate(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    /** The day to show chosen in [year] and [month]: this one's, in its own month only. */
    fun dayIn(
        year: Int,
        month: Int,
    ): Int? = day.takeIf { year == this.year && month == this.month }
}
