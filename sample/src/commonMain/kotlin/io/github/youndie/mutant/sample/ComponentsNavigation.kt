package io.github.youndie.mutant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantDrawerItem
import io.github.youndie.mutant.components.MutantDropdown
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationDrawer
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons

// The sections of Navigation, in the catalogue's order (`SpecCatalogue.kt`); `ComponentsPage` lays them out.

@Composable
internal fun TopBars(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, BAR) {
            MutantTopAppBar(
                s.onAir,
                navigationIcon = { MutantIcon(MutantIcons.Menu, s.menu) },
                status = s.status,
                windowInsets = WindowInsets(0),
                actions = { MutantIcon(MutantIcons.Search, s.find) },
            )
        }
    }
}

@Composable
internal fun TabsSpec(s: SampleStrings) {
    var tab by remember { mutableIntStateOf(0) }
    States(s) {
        Cell(s.stRest, BAR) {
            MutantTabs(listOf(MutantTab(s.tracks), MutantTab(s.notes), MutantTab(s.copies)), tab, { tab = it })
        }
    }
}

@Composable
internal fun NavBars(s: SampleStrings) {
    var nav by remember { mutableIntStateOf(0) }
    States(s) {
        Cell(s.stRest, BAR) {
            MutantNavigationBar(
                listOf(
                    MutantNavigationItem(s.onAir, MutantIcons.Home),
                    MutantNavigationItem(s.queue, MutantIcons.Schedule, badge = "3"),
                    MutantNavigationItem(s.find, MutantIcons.Search),
                    MutantNavigationItem(s.settings, MutantIcons.Settings),
                ),
                nav,
                { nav = it },
                windowInsets = WindowInsets(0),
            )
        }
    }
}

@Composable
internal fun Drawers(s: SampleStrings) {
    var channel by remember { mutableIntStateOf(2) }
    var open by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stOpen, STAGE_W) {
            Stage(STAGE_W, 320.dp) {
                MutantNavigationDrawer(
                    true,
                    {},
                    s.channelNames.mapIndexed { i, name -> MutantDrawerItem("CH 0${i + 1}", name) },
                    channel,
                    { channel = it },
                    title = s.channels,
                    key = "spec-drawer",
                )
            }
        }
    }
    TryIt(s) {
        Stage(STAGE_W, 320.dp) {
            Box(Modifier.padding(16.dp)) { MutantButton(s.openDrawer, { open = true }, key = "open-drawer") }
            MutantNavigationDrawer(
                open,
                { open = false },
                s.channelNames.mapIndexed { i, name -> MutantDrawerItem("CH 0${i + 1}", name) },
                channel,
                {
                    channel = it
                    open = false
                },
                title = s.channels,
                key = "sample-drawer",
            )
        }
    }
}

@Composable
internal fun Menus(s: SampleStrings) {
    var chosen by remember { mutableIntStateOf(0) }
    TryIt(s) { MutantDropdown(s.setting, s.menuItems, chosen, { chosen = it }) }
}
