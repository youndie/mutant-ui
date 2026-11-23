package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantSegmentedButton

// The sections of Overlays, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

@Composable
internal fun Dialogs(s: SampleStrings) {
    var open by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stOpen, STAGE_W) {
            Stage(STAGE_W, 300.dp, still = true) {
                MutantDialog(true, s.eraseQuestion, {}, text = s.eraseText) {
                    MutantDialogAction(s.cancel, {})
                    MutantDialogAction(s.erase, {}, primary = true)
                }
            }
        }
    }
    // B-174: once the dialog has gone, the focus goes back to the button that opened it.
    val opener = remember { FocusRequester() }
    TryIt(s) {
        Stage(STAGE_W, 300.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MutantCard(Modifier.fillMaxWidth(), key = "dlg-card") {
                    MutantCardTitle(s.untitled)
                    MutantCardSubtitle(s.sideA)
                }
                MutantButton(s.openDialog, { open = true }, Modifier.focusRequester(opener), key = "open-dialog")
            }
            MutantDialog(open, s.eraseQuestion, { open = false }, text = s.eraseText, returnFocusTo = opener) {
                MutantDialogAction(s.cancel, { open = false })
                MutantDialogAction(s.erase, { open = false }, primary = true)
            }
        }
    }
}

@Composable
internal fun Sheets(s: SampleStrings) {
    var speed by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(false) }
    val speeds =
        listOf("SP · 2 h", "LP · 4 h").map {
            if (s.language ==
                SampleLanguage.Russian
            ) {
                it.replace(" h", " ч")
            } else {
                it
            }
        }
    States(s) {
        Cell(s.stOpen, STAGE_W) {
            Stage(STAGE_W, 320.dp, still = true) {
                MutantBottomSheet(true, {}, windowInsets = WindowInsets(0), key = "spec-sheet") {
                    Osd(s.speed, size = 16)
                    MutantSegmentedButton(speeds, speed, { speed = it }, Modifier.fillMaxWidth(), key = "sheet-speed")
                }
            }
        }
    }
    TryIt(s) {
        Stage(STAGE_W, 320.dp) {
            Box(Modifier.padding(16.dp)) { MutantButton(s.openSheet, { open = true }, key = "open-sheet") }
            MutantBottomSheet(open, { open = false }, windowInsets = WindowInsets(0), key = "sample-sheet") {
                MutantSegmentedButton(speeds, speed, { speed = it }, Modifier.fillMaxWidth(), key = "sheet-speed2")
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    MutantButton(s.done, { open = false }, key = "sheet-done")
                }
            }
        }
    }
}
