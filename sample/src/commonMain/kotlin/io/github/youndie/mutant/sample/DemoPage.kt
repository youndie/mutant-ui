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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
 * The Demo page's three phones (B-96): each runs the demo app, opened where the design's three screens stand — the
 * library, a recording, and settings under a dialog — and from there it navigates by its bar, its drawer and its cards.
 */
@Composable
internal fun DemoPage(strings: SampleStrings) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        for (screen in DemoScreen.entries) DemoScreenFrame(screen, strings)
    }
}

/** The page's three screens. */
internal enum class DemoScreen { Library, Recording, Settings }

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
    Screen(title, tag, roundedClip, keyframeLoss && screen == DemoScreen.Library) { DemoApp(nav, strings) }
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
    content: @Composable () -> Unit,
) {
    // Each phone its own clock (B-90): the Settings screen's dialog stands that screen alone, not its neighbours.
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(title, style = MutantTheme.textStyles.display(28.sp))
        MutantScreenClock { ScreenBox(tag, roundedClip, keyframeLoss, content) }
    }
}

@Composable
private fun ScreenBox(
    tag: String,
    roundedClip: Boolean,
    keyframeLoss: Boolean,
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
            .testTag(tag),
    ) {
        // The engine loses one screen's keyframe at a time; the sample gives the loss to its first screen.
        if (keyframeLoss) MutantKeyframeLoss(Modifier.fillMaxSize(), colors.background) { content() } else content()
    }
}
