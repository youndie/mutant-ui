package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCarousel
import io.github.youndie.mutant.components.MutantCarouselItem
import io.github.youndie.mutant.components.MutantDataTable
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantTableCellStyle
import io.github.youndie.mutant.components.MutantTableColumn
import io.github.youndie.mutant.icons.MutantIcons

// The sections of Containment, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

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
