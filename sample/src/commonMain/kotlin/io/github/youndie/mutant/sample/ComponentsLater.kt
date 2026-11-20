package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantCarousel
import io.github.youndie.mutant.components.MutantCarouselItem
import io.github.youndie.mutant.components.MutantDataTable
import io.github.youndie.mutant.components.MutantDatePicker
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantSearchBar
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchResultLoading
import io.github.youndie.mutant.components.MutantSearchSuggestions
import io.github.youndie.mutant.components.MutantTableCellStyle
import io.github.youndie.mutant.components.MutantTableColumn
import io.github.youndie.mutant.components.MutantTimePicker
import io.github.youndie.mutant.icons.MutantIcons

// The sections of the design's later page, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

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
    var day by remember { mutableIntStateOf(13) }
    var month by remember { mutableIntStateOf(11) }
    var year by remember { mutableIntStateOf(2026) }
    States(s) {
        Cell(s.stRest) {
            MutantDatePicker(
                year,
                month,
                day.coerceAtMost(31),
                { day = it },
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

@Composable
internal fun Tables(s: SampleStrings) {
    var selected by remember { mutableStateOf(setOf(1)) }
    val rows =
        listOf(
            Triple(s.untitled, "46:00", "CH 03"),
            Triple(s.nightTuesday, "12:04", "CH 03"),
            Triple(s.testCard, "02:10", "CH 01"),
            Triple(s.noSignal, "00:42", "CH 99"),
        )
    States(s) {
        Cell("${s.stRest} · ${s.stRowSelected}", TABLE) {
            MutantDataTable(
                rows,
                listOf(
                    MutantTableColumn(s.columnRecord) { it.first },
                    MutantTableColumn(s.columnLength, 90.dp, MutantTableCellStyle.Osd) { it.second },
                    MutantTableColumn(s.columnChannel, 80.dp, MutantTableCellStyle.OsdSecondary) { it.third },
                ),
                Modifier.fillMaxWidth(),
                selected,
                { i, on -> selected = if (on) selected + i else selected - i },
            )
        }
    }
}

@Composable
internal fun Carousels(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, TABLE) {
            MutantCarousel(
                listOf(
                    MutantCarouselItem(s.untitled, s.sideA),
                    MutantCarouselItem(s.nightTuesday, s.recordedOver, s.status),
                    MutantCarouselItem(s.testCard, s.fourthGeneration),
                ),
                Modifier.fillMaxWidth(),
                rememberPagerState(initialPage = 1) { 3 },
            )
        }
    }
}
