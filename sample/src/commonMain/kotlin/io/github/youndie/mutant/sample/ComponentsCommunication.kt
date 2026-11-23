package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCircularProgress
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantPullToRefresh
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind

// The sections of Communication, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

@Composable
internal fun Badges(s: SampleStrings) {
    States(s) {
        Cell(s.stRest) { MutantBadge(s.brandNew) }
        Cell(s.stRest) { MutantBadge("CH 03") }
    }
}

@Composable
internal fun LinearProgresses(s: SampleStrings) {
    var value by remember { mutableFloatStateOf(.42f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(300)
            value = if (value >= 1f) 0f else (value + .02f).coerceAtMost(1f)
        }
    }
    States(s) {
        Cell(s.stDeterminate, BAR) { MutantLinearProgress(value) }
        Cell(s.stIndeterminate, BAR) { MutantLinearProgress(Modifier.fillMaxWidth()) }
    }
}

@Composable
internal fun CircularProgresses(s: SampleStrings) {
    States(s) { Cell(s.stIndeterminate) { MutantCircularProgress() } }
}

@Composable
internal fun Cocoons(s: SampleStrings) {
    Variants(s) {
        Cell(s.vBlock, CARD) { MutantCocoon(Modifier.fillMaxWidth().height(80.dp), key = "0") }
        Cell(s.vRow, CARD) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MutantCocoon(Modifier.size(40.dp), key = "1")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MutantCocoon(Modifier.width(200.dp).height(16.dp), key = "2")
                    MutantCocoon(Modifier.width(130.dp).height(16.dp), key = "3")
                }
            }
        }
    }
    States(s) { Cell(s.stLoading) { MutantCocoon(Modifier.width(160.dp).height(48.dp), key = "4") } }
}

@Composable
internal fun Snackbars(s: SampleStrings) {
    var shown by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stShown, BAR) { MutantSnackbar(s.queued, actionLabel = s.undo, key = "snack-rest") }
    }
    TryIt(s) {
        Column(Modifier.width(BAR), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MutantTextButton(s.showSnackbar, { shown = true }, key = "show-snack")
            // The kit leaves hiding the ride to its holder: the bar rides its height and 8 dp below its place (B-26), the
            // holder's 56 dp, so the holder's foot is where the screen's would be (B-174).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .snackHolderFoot()
                    .testTag("snack-holder"),
            ) {
                MutantSnackbar(s.queued, visible = shown, actionLabel = s.undo, onAction = { shown = false })
            }
        }
    }
}

@Composable
internal fun Tooltips(s: SampleStrings) {
    Variants(s) {
        Cell("${s.vPlain}: ${s.hover}") {
            MutantTooltipBox(s.shareWord) { MutantIconButton(MutantIcons.Share, s.shareWord, {}, key = "tt-a") }
        }
        Cell("${s.vSupporting}: ${s.hover}") {
            MutantTooltipBox("REC · 0:42:17", supportingText = s.recLeft) {
                MutantFab(MutantIcons.Add, s.record, {}, key = "tt-b")
            }
        }
    }
}

@Composable
internal fun EmptyStates(s: SampleStrings) {
    Variants(s) {
        for (kind in MutantIllustrationKind.entries.drop(1)) {
            Cell(kind.name) { MutantIllustration(kind) }
        }
    }
    States(s) {
        Cell(s.stRest, CARD) {
            MutantEmptyState(
                MutantIllustrationKind.Cassette,
                s.emptyTitle,
                Modifier.fillMaxWidth(),
                text = s.emptyText,
                action = { MutantButton(s.record, {}, key = "empty-record") },
            )
        }
    }
}

@Composable
internal fun Pulls(s: SampleStrings) {
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            kotlinx.coroutines.delay(2000)
            refreshing = false
        }
    }
    States(s) {
        Cell(s.stRefreshing, STAGE_W) {
            Stage(STAGE_W, 200.dp) {
                MutantPullToRefresh(
                    true,
                    {},
                    Modifier.fillMaxSize(),
                    refreshingLabel = s.refreshingWord,
                    doneLabel = s.refreshedWord,
                    key = "ptr-held",
                ) {
                    Column { repeat(3) { MutantListItem("${s.untitled} ${it + 1}", key = "held-row-$it") } }
                }
            }
        }
    }
    TryIt(s) {
        Cell(s.pullHint, STAGE_W) {
            Stage(STAGE_W, 260.dp) {
                MutantPullToRefresh(
                    refreshing,
                    { refreshing = true },
                    Modifier.fillMaxSize(),
                    refreshingLabel = s.refreshingWord,
                    doneLabel = s.refreshedWord,
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        repeat(8) { MutantListItem("${s.untitled} ${it + 1}", key = "pull-row-$it") }
                    }
                }
            }
        }
    }
}

/**
 * Cuts what is drawn below this element's foot, as a screen's bottom edge cuts a snackbar riding off it; above and to
 * the sides nothing is cut, so the bar's shadow and drips at rest stay whole.
 */
private fun Modifier.snackHolderFoot(): Modifier =
    drawWithContent {
        clipRect(-SNACK_HOLDER_REACH, -SNACK_HOLDER_REACH, size.width + SNACK_HOLDER_REACH, size.height) {
            this@drawWithContent.drawContent()
        }
    }

/** How far past the holder's sides and top its clip reaches: past anything the bar draws there. */
private const val SNACK_HOLDER_REACH = 10_000f
