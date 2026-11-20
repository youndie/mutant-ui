package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantButtonState
import io.github.youndie.mutant.components.MutantExtendedFab
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantSegmentedButton
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.icons.MutantIcons

// The sections of Actions, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

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
