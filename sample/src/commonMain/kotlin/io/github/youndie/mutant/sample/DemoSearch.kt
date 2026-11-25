package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantDataTable
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantMenu
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantSearchBar
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchSuggestions
import io.github.youndie.mutant.components.MutantTableCellStyle
import io.github.youndie.mutant.components.MutantTableColumn
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.surface.MutantAppear
import io.github.youndie.mutant.surface.MutantScreen
import io.github.youndie.mutant.theme.MutantTheme

@Composable
internal fun Search(
    s: SampleStrings,
    tapes: List<Cassette>,
    nav: DemoNav,
    bar: @Composable BoxScope.() -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Int>() }
    var erasing by remember { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    // B-174: the dialog gives the focus back to the button that opened it, or, once the erase has left nothing to erase
    // and the button is disabled, to the sort button beside the table.
    val eraseButton = remember { FocusRequester() }
    val sortButton = remember { FocusRequester() }
    var order by rememberSaveable { mutableIntStateOf(0) }
    val rows =
        when (order) {
            1 -> {
                tapes.sortedByDescending {
                    it.length.count { c -> c == ':' } * 100_000 +
                        it.length.filter(Char::isDigit).toInt()
                }
            }

            2 -> {
                tapes.sortedBy { it.channel }
            }

            else -> {
                tapes
            }
        }
    val found =
        tapes.withIndex().filter {
            query.isNotBlank() &&
                it.value.title.contains(query.trim(), ignoreCase = true)
        }
    Box(Modifier.fillMaxSize()) {
        // B-180: the screen tunes in as the navigation bar's pill starts for it; the bar stands outside, and moves.
        MutantScreen(nav.tab) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                TabBar(s.find, s, nav)
                TabScroll {
                    Spacer(Modifier.height(8.dp))
                    MutantSearchBar(
                        query,
                        { query = it },
                        Modifier.fillMaxWidth(),
                        prefix = s.search,
                        placeholder = s.findRecording,
                        clearLabel = s.clear,
                        key = "demo-search",
                    ) {
                        if (query.isBlank()) {
                            MutantSearchSuggestions(listOf(s.night, s.rainWord, s.noiseWord), { query = it })
                        } else if (found.isEmpty()) {
                            Osd(s.noSignal.uppercase(), MutantTheme.colors.textSecondary, size = 18)
                        } else {
                            Column {
                                for ((i, t) in found) {
                                    // B-179: a result tunes in as it arrives; one that stays does not tune in again.
                                    key(i) {
                                        MutantAppear {
                                            MutantSearchResult(
                                                t.title,
                                                query.trim(),
                                                supporting = t.side,
                                                leadingIcon = MutantIcons.Schedule,
                                                onClick = { nav.open = i },
                                                key = "sr$i",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Osd(s.archiveWord, MutantTheme.colors.textSecondary, size = 18)
                        Spacer(Modifier.weight(1f))
                        // B-174: the menu stands from the button's end, inside the screen; from its start, at the screen's
                        // right, its 216 dp ran past the phone.
                        var sortWidth by remember { mutableStateOf(0.dp) }
                        val density = LocalDensity.current
                        Box(Modifier.onSizeChanged { sortWidth = with(density) { it.width.toDp() } }) {
                            MutantTextButton(
                                s.sortBy(order),
                                { sorting = true },
                                Modifier.focusRequester(sortButton),
                                key = "demo-sort",
                            )
                            MutantMenu(
                                sorting,
                                { sorting = false },
                                s.sortOrders,
                                order,
                                {
                                    order = it
                                    sorting = false
                                },
                                offset = DpOffset(sortWidth - SORT_MENU_WIDTH, SORT_MENU_TOP),
                                key = "demo-sort-menu",
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    MutantDataTable(
                        rows,
                        listOf(
                            MutantTableColumn(s.columnRecord) { it.title },
                            MutantTableColumn(s.columnLength, 70.dp, MutantTableCellStyle.Osd) { it.length },
                            MutantTableColumn(s.columnChannel, 60.dp, MutantTableCellStyle.OsdSecondary) { it.channel },
                        ),
                        Modifier.fillMaxWidth(),
                        selected = selected.map { rows.indexOf(tapes[it]) }.toSet(),
                        onSelectedChange = { i, on ->
                            val at = tapes.indexOf(rows[i])
                            if (on) selected += at else selected -= at
                        },
                        key = "demo-row",
                        // B-174: a sort moves each row's box, its pour and its slime with the record, not the place.
                        rowKey = { it.title },
                    )
                    Spacer(Modifier.height(20.dp))
                    MutantOutlinedButton(
                        s.eraseSelected(selected.size),
                        { erasing = true },
                        Modifier.focusRequester(eraseButton),
                        enabled = selected.isNotEmpty(),
                        key = "demo-erase",
                    )
                }
            }
        }
        bar()
        MutantDialog(
            erasing,
            s.eraseQuestion,
            { erasing = false },
            text = s.eraseText,
            returnFocusTo = if (selected.isEmpty()) sortButton else eraseButton,
            key = "dialog",
        ) {
            MutantDialogAction(s.cancel, { erasing = false })
            MutantDialogAction(s.erase, {
                selected.clear()
                erasing = false
            }, primary = true, key = "dialog-ok")
        }
    }
}

/** The kit's menu, as its KDoc gives it: 216 dp wide, standing 50 dp under its parent's top. */
private val SORT_MENU_WIDTH = 216.dp
private val SORT_MENU_TOP = 50.dp
