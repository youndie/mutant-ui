package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantDrawerItem
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationDrawer
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantTooltipBox
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.theme.MutantTheme

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

    /** The tab bar's menu button: the drawer it opens, and the dialog the Settings tab opens under, give focus back to it. */
    val menu = FocusRequester()
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
            returnFocusTo = nav.menu,
            key = "demo-drawer",
        )
    }
}

/** An icon action of a bar with its name over it on hover, a long press or keyboard focus. */
@Composable
internal fun BarAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MutantTooltipBox(label) { MutantIconButton(icon, label, onClick, modifier, key = "bar-$label") }
}

/** A tab's top bar: the menu opens the drawer. */
@Composable
internal fun TabBar(
    title: String,
    s: SampleStrings,
    nav: DemoNav,
    status: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    MutantTopAppBar(
        title,
        navigationIcon = {
            BarAction(
                MutantIcons.Menu,
                s.menu,
                Modifier.focusRequester(nav.menu),
            ) { nav.drawer = true }
        },
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
internal fun TabScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        content()
        Spacer(Modifier.height(112.dp))
    }
}
