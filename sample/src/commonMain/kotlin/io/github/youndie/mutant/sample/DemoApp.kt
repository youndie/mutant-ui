package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCarousel
import io.github.youndie.mutant.components.MutantCarouselItem
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantDataTable
import io.github.youndie.mutant.components.MutantDatePicker
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantDrawerItem
import io.github.youndie.mutant.components.MutantDropdown
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantMenu
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationDrawer
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantPullToRefresh
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSearchBar
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchSuggestions
import io.github.youndie.mutant.components.MutantSegmentedButton
import io.github.youndie.mutant.components.MutantSlider
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTableCellStyle
import io.github.youndie.mutant.components.MutantTableColumn
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTimePicker
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.theme.MutantTheme
import kotlinx.coroutines.delay

/** The demo app's four destinations, the navigation bar's and the drawer's, in their order. */
internal enum class DemoTab { OnAir, Queue, Search, Settings }

/**
 * Where the demo app stands: its tab, the cassette open over it (an index into [archive], or none), whether the drawer
 * is out, and whether the erase dialog is up — the Settings phone opens with it, as the design's third screen does.
 */
@Stable
internal class DemoNav(
    tab: DemoTab = DemoTab.OnAir,
    open: Int? = null,
    erasing: Boolean = false,
) {
    var tab by mutableStateOf(tab)
    var open by mutableStateOf(open)
    var drawer by mutableStateOf(false)
    var erasing by mutableStateOf(erasing)

    fun go(to: DemoTab) {
        tab = to
        open = null
        drawer = false
    }
}

/** One tape of the archive the demo browses. [kind] is the chip it is filed under: 1 recordings, 2 noise. */
internal class Cassette(
    val title: String,
    val side: String,
    val length: String,
    val channel: String,
    val kind: Int,
    val favourite: Boolean,
)

internal fun archive(s: SampleStrings): List<Cassette> =
    listOf(
        Cassette(s.untitled, s.sideA, "46:00", "CH 03", 1, false),
        Cassette(s.nightTuesday, s.recordedOver, "12:04", "CH 03", 1, true),
        Cassette(s.testCard, s.fourthGeneration, "08:30", "CH 01", 2, false),
        Cassette(s.rain, s.sideB, "31:12", "CH 05", 1, true),
        Cassette(s.staticBetween, s.noiseNine, "09:02", "CH 07", 2, false),
        Cassette(s.weather, s.recordedOverTwenty, "20:40", "CH 02", 1, false),
        Cassette(s.lateRepeat, s.longPlay, "1:58:10", "CH 03", 1, true),
    )

/**
 * The demo app inside one phone (B-96): the navigation bar and the drawer move between the four tabs, a cassette opens
 * the recording over them, and its back arrow returns. Each tab keeps its scroll and its fields while another is shown.
 */
@Composable
internal fun DemoApp(
    nav: DemoNav,
    s: SampleStrings,
) {
    val tapes = remember(s) { archive(s) }
    val queue = remember(s) { mutableStateListOf(tapes[3], tapes[5], tapes[6]) }
    val holder = rememberSaveableStateHolder()
    // Each tab draws the bar over its content and under its own sheets and dialogs; the bar moves between tabs whole,
    // so its pill slides from the tab left to the tab taken.
    val bar: @Composable BoxScope.() -> Unit =
        remember(s, nav) {
            movableContentWithReceiverOf<BoxScope> {
                MutantNavigationBar(
                    listOf(
                        MutantNavigationItem(s.onAir, MutantIcons.Home),
                        MutantNavigationItem(s.queue, MutantIcons.Schedule, badge = "${queue.size}"),
                        MutantNavigationItem(s.find, MutantIcons.Search),
                        MutantNavigationItem(s.settings, MutantIcons.Settings),
                    ),
                    nav.tab.ordinal,
                    { nav.go(DemoTab.entries[it]) },
                    Modifier.align(Alignment.BottomCenter).padding(8.dp),
                    windowInsets = WindowInsets(0),
                )
            }
        }
    Box(Modifier.fillMaxSize()) {
        val open = nav.open
        if (open != null) {
            holder.SaveableStateProvider("recording-$open") { Recording(s, tapes[open], nav) }
        } else {
            holder.SaveableStateProvider(nav.tab) {
                when (nav.tab) {
                    DemoTab.OnAir -> Library(s, tapes, nav, bar)
                    DemoTab.Queue -> Queue(s, queue, nav, tapes, bar)
                    DemoTab.Search -> Search(s, tapes, nav, bar)
                    DemoTab.Settings -> Settings(s, nav, bar)
                }
            }
        }
        MutantNavigationDrawer(
            nav.drawer,
            { nav.drawer = false },
            s.channelNames.mapIndexed { i, name -> MutantDrawerItem("CH 0${i + 1}", name) },
            nav.tab.ordinal,
            { nav.go(DemoTab.entries[it]) },
            title = s.channels,
            key = "demo-drawer",
        )
    }
}

/** An icon action of a bar with its name over it on hover, a long press or keyboard focus. */
@Composable
private fun BarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    MutantTooltipBox(label) { MutantIconButton(icon, label, onClick, key = "bar-$label") }
}

/** A tab's top bar: the menu opens the drawer. */
@Composable
private fun TabBar(
    title: String,
    s: SampleStrings,
    nav: DemoNav,
    status: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    MutantTopAppBar(
        title,
        navigationIcon = { BarAction(MutantIcons.Menu, s.menu) { nav.drawer = true } },
        status = status,
        windowInsets = WindowInsets(0),
        actions = actions,
    )
}

@Composable
internal fun Osd(
    text: String,
    color: Color = MutantTheme.colors.textOsd,
    size: Int = 20,
    tracking: Float = 1f,
) {
    BasicText(text, style = MutantTheme.textStyles.osd(size.sp).copy(color = color, letterSpacing = tracking.sp))
}

/** A tab's content under its bar: scrolls, and leaves room at the end for the navigation bar over it. */
@Composable
private fun TabScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        content()
        Spacer(Modifier.height(112.dp))
    }
}

@Composable
private fun Library(
    s: SampleStrings,
    tapes: List<Cassette>,
    nav: DemoNav,
    bar: @Composable BoxScope.() -> Unit,
) {
    var chosen by rememberSaveable { mutableIntStateOf(1) }
    var query by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var speed by remember { mutableIntStateOf(0) }
    val shown =
        tapes.withIndex().filter { (_, t) ->
            val filed =
                when (chosen) {
                    0 -> true
                    3 -> t.favourite
                    else -> t.kind == chosen
                }
            filed && (query.isBlank() || t.title.contains(query.trim(), ignoreCase = true))
        }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            TabBar(s.onAir, s, nav, status = s.clock)
            TabScroll {
                Spacer(Modifier.height(8.dp))
                MutantTextField(
                    query,
                    { query = it },
                    Modifier.fillMaxWidth(),
                    prefix = s.search,
                    placeholder = s.findRecording,
                    key = "field",
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(s.all, s.recordings, s.noise, s.favourites).forEachIndexed { i, label ->
                        MutantChip(label, chosen == i, { chosen = i }, key = "chipA$i")
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (shown.isEmpty()) {
                    MutantEmptyState(
                        MutantIllustrationKind.Cassette,
                        s.emptyTitle,
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        text = s.emptyText,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
                        for ((i, t) in shown.take(2)) TapeCard(t, "cardA$i") { nav.open = i }
                        // The third tape is still being copied: its card has not hatched yet.
                        MutantCocoon(Modifier.fillMaxWidth().height(80.dp), key = "0")
                    }
                }
                Spacer(Modifier.height(24.dp))
                MutantButton(s.playAllWide, {
                    shown.firstOrNull()?.let { nav.open = it.index }
                }, Modifier.fillMaxWidth(), key = "button")
                Spacer(Modifier.height(32.dp))
                Osd(s.recentlyOnAir, MutantTheme.colors.textSecondary, size = 18)
                Spacer(Modifier.height(12.dp))
                val recent = listOf(6, 3, 2)
                MutantCarousel(
                    recent.map { MutantCarouselItem(tapes[it].title, tapes[it].side, tapes[it].channel) },
                    Modifier.fillMaxWidth(),
                    onClick = { nav.open = recent[it] },
                    key = "demo-car",
                    indicatorKey = "demo-cd",
                )
                if (shown.size > 2) {
                    Spacer(Modifier.height(32.dp))
                    Osd(s.onTheShelf, MutantTheme.colors.textSecondary, size = 18)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
                        for ((i, t) in shown.drop(2)) TapeCard(t, "cardA$i") { nav.open = i }
                    }
                }
            }
        }
        bar()
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 84.dp)) {
            MutantTooltipBox(s.newRecording, supportingText = s.recLeft) {
                MutantFab(MutantIcons.Add, s.newRecording, { sheet = true }, key = "fab")
            }
        }
        MutantBottomSheet(sheet, { sheet = false }, windowInsets = WindowInsets(0), key = "demo-new") {
            BasicText(s.newRecording, style = MutantTheme.textStyles.display(24.sp))
            Osd(s.speed, size = 18)
            MutantSegmentedButton(listOf("SP · 2 h", "LP · 4 h", "EP · 6 h").localised(s), speed, {
                speed = it
            }, Modifier.fillMaxWidth(), key = "demo-speed")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                MutantButton(s.record, { sheet = false }, key = "demo-record")
            }
        }
    }
}

/** The speeds' hours, in the language's letter. */
private fun List<String>.localised(s: SampleStrings) =
    if (s.language == SampleLanguage.Russian) map { it.replace(" h", " ч") } else this

@Composable
private fun TapeCard(
    t: Cassette,
    key: String,
    onClick: () -> Unit,
) {
    MutantCard(Modifier.fillMaxWidth(), onClick = onClick, key = key) {
        MutantCardTitle(t.title)
        MutantCardSubtitle(t.side)
    }
}

@Composable
private fun Queue(
    s: SampleStrings,
    queue: MutableList<Cassette>,
    nav: DemoNav,
    tapes: List<Cassette>,
    bar: @Composable BoxScope.() -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    var copied by remember { mutableFloatStateOf(.42f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            copied = if (copied >= 1f) 0f else (copied + .01f).coerceAtMost(1f)
        }
    }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(2000)
            if (queue.none { it.title == s.freshOffTheAir }) {
                queue.add(0, Cassette(s.freshOffTheAir, s.justNow, "03:15", "CH 04", 1, false))
            }
            refreshing = false
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            TabBar(s.queue, s, nav, status = s.inLine(queue.size))
            Spacer(Modifier.height(8.dp))
            Osd(s.copying, size = 18)
            Spacer(Modifier.height(10.dp))
            MutantLinearProgress(copied)
            Spacer(Modifier.height(18.dp))
            MutantPullToRefresh(
                refreshing,
                { refreshing = true },
                Modifier.fillMaxWidth().weight(1f),
                refreshingLabel = s.refreshingWord,
                doneLabel = s.refreshedWord,
                key = "demo-ptr",
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    queue.forEachIndexed { i, t ->
                        val fresh = t.title == s.freshOffTheAir
                        MutantListItem(
                            t.title,
                            supporting = t.side,
                            leadingIcon = MutantIcons.Schedule,
                            trailingText = if (fresh) null else t.length,
                            trailing = if (fresh) ({ MutantBadge(s.brandNew) }) else null,
                            onClick = { tapes.indexOf(t).takeIf { it >= 0 }?.let { nav.open = it } },
                            key = "queue-$i",
                        )
                        MutantDivider(key = "q$i")
                    }
                    Spacer(Modifier.height(16.dp))
                    Osd(s.pullToRefresh, MutantTheme.colors.textSecondary, size = 16)
                    Spacer(Modifier.height(112.dp))
                }
            }
        }
        bar()
    }
}

@Composable
private fun Search(
    s: SampleStrings,
    tapes: List<Cassette>,
    nav: DemoNav,
    bar: @Composable BoxScope.() -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Int>() }
    var erasing by remember { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
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
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Osd(s.archiveWord, MutantTheme.colors.textSecondary, size = 18)
                    Spacer(Modifier.weight(1f))
                    Box {
                        MutantTextButton(s.sortBy(order), { sorting = true }, key = "demo-sort")
                        MutantMenu(sorting, { sorting = false }, s.sortOrders, order, {
                            order = it
                            sorting = false
                        }, key = "demo-sort-menu")
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
                )
                Spacer(Modifier.height(20.dp))
                MutantOutlinedButton(
                    s.eraseSelected(selected.size),
                    { erasing = true },
                    enabled = selected.isNotEmpty(),
                    key = "demo-erase",
                )
            }
        }
        bar()
        MutantDialog(erasing, s.eraseQuestion, { erasing = false }, text = s.eraseText) {
            MutantDialogAction(s.cancel, { erasing = false })
            MutantDialogAction(s.erase, {
                selected.clear()
                erasing = false
            }, primary = true)
        }
    }
}

@Composable
private fun Recording(
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

@Composable
private fun Settings(
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
    var day by rememberSaveable { mutableIntStateOf(24) }
    var month by rememberSaveable { mutableIntStateOf(11) }
    var year by rememberSaveable { mutableIntStateOf(2026) }
    var hour by rememberSaveable { mutableIntStateOf(2) }
    var minute by rememberSaveable { mutableIntStateOf(40) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            TabBar(s.settings, s, nav)
            TabScroll {
                Spacer(Modifier.height(12.dp))
                MutantTextField(name, { name = it }, Modifier.fillMaxWidth(), prefix = s.name, key = "fieldC0")
                Spacer(Modifier.height(30.dp))
                MutantTextField("99", {}, Modifier.fillMaxWidth(), prefix = s.channel, isError = true, key = "fieldC1")
                Spacer(Modifier.height(22.dp))
                // The page's error line: 18 px, tracked .5.
                Osd(s.noSignal99, MutantTheme.colors.textError, size = 18, tracking = .5f)
                Spacer(Modifier.height(16.dp))
                Osd(s.speed)
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    listOf("SP", "LP", "EP").forEachIndexed { i, label ->
                        MutantRadioButton(speed == i, { speed = i }, label = label, key = "radio")
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
                    supporting = s.timerLine(day, month, hour, minute),
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
        bar()
        MutantBottomSheet(timer, { timer = false }, windowInsets = WindowInsets(0), key = "demo-timer-sheet") {
            MutantDatePicker(
                year,
                month,
                day,
                { day = it },
                { y, m ->
                    year = y
                    month = m
                },
                monthNames = s.monthNames,
                weekdayNames = s.weekdayNames,
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
        MutantDialog(nav.erasing, s.eraseQuestion, { nav.erasing = false }, text = s.eraseNamed) {
            MutantDialogAction(s.cancel, { nav.erasing = false })
            MutantDialogAction(s.erase, { nav.erasing = false }, primary = true)
        }
    }
}
