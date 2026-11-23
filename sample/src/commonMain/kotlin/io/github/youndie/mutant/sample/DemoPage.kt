package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSnackbar
import io.github.youndie.mutant.components.MutantSwitch
import io.github.youndie.mutant.components.MutantTab
import io.github.youndie.mutant.components.MutantTabs
import io.github.youndie.mutant.components.MutantTextButton
import io.github.youndie.mutant.components.MutantTextField
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.surface.MutantKeyframeLoss
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme

/**
 * The Demo page's phone (B-96): it runs the demo app, opened where one of the design's three screens stands — the
 * library, a recording, and settings under a dialog — and from there it navigates by its bar, its drawer and its cards.
 * B-148: one screen at a time, chosen by a row of chips over it; the screen keeps its frame, its size and its title.
 */
@Composable
internal fun DemoPage(
    strings: SampleStrings,
    screen: DemoScreen,
    choose: (DemoScreen) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        FlowRow(
            Modifier.testTag("demo-index"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (each in DemoScreen.entries) {
                val label =
                    when (each) {
                        DemoScreen.Library -> strings.demoLibrary
                        DemoScreen.Recording -> strings.demoRecording
                        DemoScreen.Settings -> strings.settings
                    }
                MutantChip(label, each == screen, { choose(each) }, key = "demo-${each.name}")
            }
        }
        // The Settings screen's dialog takes focus as it opens, and the focus asks the page to scroll to it; the page stays
        // put, its chips in view.
        Box(StayPut) { key(screen) { DemoScreenFrame(screen, strings) } }
    }
}

/** The page's three screens. Public, as [SampleSettings] remembers the one shown. */
enum class DemoScreen { Library, Recording, Settings }

/** One of the screens with its title over it, as the page and a test draw it. */
@Composable
internal fun DemoScreenFrame(
    screen: DemoScreen,
    strings: SampleStrings,
    dialogOpen: Boolean = true,
    roundedClip: Boolean = true,
    keyframeLoss: Boolean = true,
) {
    // Each phone runs the whole app (B-96), opened where the design's screen stands.
    val nav =
        remember(screen, dialogOpen) {
            when (screen) {
                DemoScreen.Library -> DemoNav()
                DemoScreen.Recording -> DemoNav(open = 0)
                DemoScreen.Settings -> DemoNav(DemoTab.Settings, erasing = dialogOpen)
            }
        }
    val (title, tag) =
        when (screen) {
            DemoScreen.Library -> strings.demoLibrary to "demo-library"
            DemoScreen.Recording -> strings.demoRecording to "demo-recording"
            DemoScreen.Settings -> strings.demoSettings to "demo-settings"
        }
    Screen(title, tag, roundedClip, keyframeLoss && screen == DemoScreen.Library, nav.screen) { DemoApp(nav, strings) }
}

/**
 * A phone's screen as the page draws it: 360 × 720, its corners 28, ringed in black, the Tape over it. Without
 * [roundedClip] the corners are not cut: B-76's bench reads what that clip costs the render thread.
 */
@Composable
private fun Screen(
    title: String,
    tag: String,
    roundedClip: Boolean,
    keyframeLoss: Boolean = false,
    shows: Any? = null,
    content: @Composable () -> Unit,
) {
    // Each phone its own clock (B-90): the Settings screen's dialog stands that screen alone, not its neighbours.
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(title, style = MutantTheme.textStyles.display(28.sp))
        MutantScreenClock { ScreenBox(tag, roundedClip, keyframeLoss, shows, content) }
    }
}

@Composable
private fun ScreenBox(
    tag: String,
    roundedClip: Boolean,
    keyframeLoss: Boolean,
    shows: Any?,
    content: @Composable () -> Unit,
) {
    val colors = MutantTheme.colors
    Box(
        Modifier
            .size(360.dp, 720.dp)
            .border(2.dp, Color(0xFF05060C), RoundedCornerShape(28.dp))
            .then(if (roundedClip) Modifier.clip(RoundedCornerShape(28.dp)) else Modifier)
            .background(colors.background)
            .mutantTape()
            .nestedScroll(PhoneKeepsItsFling)
            .testTag(tag),
    ) {
        // The engine loses one screen's keyframe at a time; the sample gives the loss to its first screen, and tells it
        // which of the app's screens it shows (B-198).
        if (keyframeLoss) {
            MutantKeyframeLoss(Modifier.fillMaxSize(), colors.background, screen = shows) { content() }
        } else {
            content()
        }
    }
}

/**
 * A phone keeps the fling it leaves (B-174): a fling that reaches a tab's top or end, or the release of a pull to
 * refresh, stops there instead of handing its speed on to the page, which slid the phone out of the window. A drag
 * still passes on what the phone leaves, so a finger on the phone can scroll the page on a narrow screen, where the
 * phone fills most of it.
 */
private object PhoneKeepsItsFling : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = if (source == NestedScrollSource.SideEffect) available else Offset.Zero

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = available
}
