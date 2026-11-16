package io.github.youndie.mutant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.youndie.mutant.components.MutantBadge
import io.github.youndie.mutant.components.MutantBottomSheet
import io.github.youndie.mutant.components.MutantButton
import io.github.youndie.mutant.components.MutantButtonState
import io.github.youndie.mutant.components.MutantCard
import io.github.youndie.mutant.components.MutantCardSubtitle
import io.github.youndie.mutant.components.MutantCardTitle
import io.github.youndie.mutant.components.MutantCarousel
import io.github.youndie.mutant.components.MutantCarouselItem
import io.github.youndie.mutant.components.MutantCheckbox
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.components.MutantCircularProgress
import io.github.youndie.mutant.components.MutantCocoon
import io.github.youndie.mutant.components.MutantDataTable
import io.github.youndie.mutant.components.MutantDatePicker
import io.github.youndie.mutant.components.MutantDialog
import io.github.youndie.mutant.components.MutantDialogAction
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantDrawerItem
import io.github.youndie.mutant.components.MutantDropdown
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.components.MutantExtendedFab
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantNavigationBar
import io.github.youndie.mutant.components.MutantNavigationDrawer
import io.github.youndie.mutant.components.MutantNavigationItem
import io.github.youndie.mutant.components.MutantOutlinedButton
import io.github.youndie.mutant.components.MutantPullToRefresh
import io.github.youndie.mutant.components.MutantRadioButton
import io.github.youndie.mutant.components.MutantSearchBar
import io.github.youndie.mutant.components.MutantSearchResult
import io.github.youndie.mutant.components.MutantSearchResultLoading
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
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.pattern.MutantPattern
import io.github.youndie.mutant.pattern.mutantPattern
import io.github.youndie.mutant.surface.MutantKeyframeLoss
import io.github.youndie.mutant.surface.mutantTape
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
import io.github.youndie.mutant.tokens.MutantPalettes
import kotlinx.coroutines.launch

/**
 * Every component of the kit, one section each, grouped as the design's pages group them (B-99). A section is the
 * component's name, its public functions, its stage and its purpose, then labelled rows: its variants, its states as the
 * design's table names them, and a live example where a state is only seen by doing something. Every specimen is live.
 */
@Composable
internal fun ComponentsPage(strings: SampleStrings) {
    val scope = rememberCoroutineScope()
    val groups = remember { SpecStage.entries.associateWith { BringIntoViewRequester() } }
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_GAP)) {
        // A rect taller than any window: the page brings its top edge to the window's top, not its bottom.
        Index(strings) { stage -> scope.launch { groups.getValue(stage).bringIntoView(Rect(0f, 0f, 1f, 100_000f)) } }
        for (stage in SpecStage.entries) {
            GroupTitle(stage, strings, Modifier.bringIntoViewRequester(groups.getValue(stage)))
            for (id in SpecId.entries.filter { it.stage == stage }) {
                Spec(id, strings) { SpecBody(id, strings) }
            }
        }
    }
}

private fun SpecStage.title(s: SampleStrings) =
    when (this) {
        SpecStage.Mvp -> s.groupMvp
        SpecStage.V1 -> s.groupV1
        SpecStage.Later -> s.groupLater
        SpecStage.Base -> s.groupBase
    }

private fun SpecStage.line(s: SampleStrings) =
    when (this) {
        SpecStage.Mvp -> s.groupMvpLine
        SpecStage.V1 -> s.groupV1Line
        SpecStage.Later -> s.groupLaterLine
        SpecStage.Base -> s.groupBaseLine
    }

private fun SpecStage.badge(s: SampleStrings) =
    when (this) {
        SpecStage.Mvp -> s.stageMvp
        SpecStage.V1 -> s.stageV1
        SpecStage.Later -> s.stageLater
        SpecStage.Base -> s.stageBase
    }

/** The page's index: one chip per group, each with its count, scrolling to it. */
@Composable
private fun Index(
    s: SampleStrings,
    go: (SpecStage) -> Unit,
) {
    Column(Modifier.testTag("spec-index"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Osd(s.onThisPage, MutantTheme.colors.textSecondary, size = 16)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (stage in SpecStage.entries) {
                val n = SpecId.entries.count { it.stage == stage }
                MutantChip("${stage.title(s)} · $n", false, { go(stage) }, key = "index-${stage.name}")
            }
        }
    }
}

@Composable
private fun GroupTitle(
    stage: SpecStage,
    s: SampleStrings,
    modifier: Modifier,
) {
    Column(
        modifier.padding(top = 24.dp).testTag("group-${stage.name}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(stage.title(s), style = MutantTheme.textStyles.display(40.sp))
        // In the text's own ink: the secondary one reads 4.49:1 on the page's ground of «Копия».
        BasicText(stage.line(s), style = MutantTheme.textStyles.text(15.sp))
    }
}

/**
 * One section: the head (name, functions, stage, purpose) beside the rows on a wide page, over them on a narrow one.
 * Every section has the same widths, padding and gaps.
 */
@Composable
private fun Spec(
    id: SpecId,
    s: SampleStrings,
    rows: @Composable () -> Unit,
) {
    val colors = MutantTheme.colors
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(SECTION_PADDING)
            .testTag("section-${id.name}"),
    ) {
        val wide = maxWidth >= WIDE
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Head(id, s, Modifier.width(HEAD_WIDTH))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) { rows() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
                Head(id, s, Modifier.fillMaxWidth())
                rows()
            }
        }
    }
}

@Composable
private fun Head(
    id: SpecId,
    s: SampleStrings,
    modifier: Modifier,
) {
    val colors = MutantTheme.colors
    Column(modifier.testTag("head-${id.name}"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // A long name wraps, and the stage's badge goes under it rather than out of the head.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(id.name(s), style = MutantTheme.textStyles.display(28.sp))
            MutantBadge(id.stage.badge(s))
        }
        BasicText(id.line(s), style = MutantTheme.textStyles.text(14.sp).copy(color = colors.textSecondary))
        for (api in id.api) Osd(api, colors.textOsd, size = 14, tracking = .5f)
    }
}

/** A labelled row of specimens, each with its caption under it. */
@Composable
private fun SpecRow(
    label: String,
    tag: String,
    content: @Composable FlowRowScope.() -> Unit,
) {
    Column(Modifier.testTag(tag), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Osd(label, MutantTheme.colors.textSecondary, size = 14)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
            verticalArrangement = Arrangement.spacedBy(CELL_GAP),
        ) { content() }
    }
}

@Composable
private fun Variants(
    s: SampleStrings,
    content: @Composable FlowRowScope.() -> Unit,
) = SpecRow(s.rowVariants, "row-variants", content)

@Composable
private fun States(
    s: SampleStrings,
    content: @Composable FlowRowScope.() -> Unit,
) = SpecRow(s.rowStates, "row-states", content)

@Composable
private fun TryIt(
    s: SampleStrings,
    content: @Composable FlowRowScope.() -> Unit,
) = SpecRow(s.rowTry, "row-try", content)

/** A specimen with its caption: the caption stands clear of the drips the body hangs below its slot. */
@Composable
private fun Cell(
    caption: String,
    width: Dp? = null,
    content: @Composable () -> Unit,
) {
    Column(
        (if (width != null) Modifier.width(width) else Modifier.widthIn(min = CELL_MIN)).testTag("cell"),
        verticalArrangement = Arrangement.spacedBy(CAPTION_GAP),
    ) {
        content()
        BasicText(
            caption,
            Modifier.testTag("caption"),
            style = MutantTheme.textStyles.text(12.sp).copy(color = MutantTheme.colors.textSecondary),
        )
    }
}

/**
 * An interaction source held in a state for a specimen: a press (the body boils) or a focus, emitted a frame after the
 * control has subscribed to it.
 */
@Composable
private fun held(
    press: Boolean = false,
    focus: Boolean = false,
): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(source) {
        withFrameNanos { }
        if (press) source.emit(PressInteraction.Press(Offset(24f, 24f)))
        if (focus) source.emit(FocusInteraction.Focus())
    }
    return source
}

/**
 * An overlay's specimen: a screen of its own, so the overlay holds this box's clock, not the page's (B-90). An overlay
 * shown from the first frame takes focus, and the focus asks the page to scroll to it; the stage keeps that request to
 * itself, so the page opens at its top.
 */
@Composable
private fun Stage(
    width: Dp,
    height: Dp,
    content: @Composable () -> Unit,
) {
    MutantScreenClock {
        Box(
            Modifier
                .size(width, height)
                .then(StayPut)
                .background(MutantTheme.colors.page),
        ) { content() }
    }
}

/** Swallows a request to bring something inside into view: the page around does not scroll for it. */
private data object StayPut : ModifierNodeElement<StayPutNode>() {
    override fun create() = StayPutNode()

    override fun update(node: StayPutNode) = Unit
}

private class StayPutNode :
    Modifier.Node(),
    BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) = Unit
}

@Composable
private fun SpecBody(
    id: SpecId,
    s: SampleStrings,
) {
    when (id) {
        SpecId.Button -> Buttons(s)
        SpecId.IconButton -> IconButtons(s)
        SpecId.Fab -> Fabs(s)
        SpecId.Chip -> Chips(s)
        SpecId.Checkbox -> Checkboxes(s)
        SpecId.Radio -> Radios(s)
        SpecId.Switch -> Switches(s)
        SpecId.TextField -> Fields(s)
        SpecId.Card -> Cards(s)
        SpecId.Cocoon -> Cocoons(s)
        SpecId.TopAppBar -> TopBars(s)
        SpecId.Tabs -> TabsSpec(s)
        SpecId.NavigationBar -> NavBars(s)
        SpecId.ListItem -> ListItems(s)
        SpecId.Divider -> Dividers(s)
        SpecId.Badge -> Badges(s)
        SpecId.Snackbar -> Snackbars(s)
        SpecId.LinearProgress -> LinearProgresses(s)
        SpecId.CircularProgress -> CircularProgresses(s)
        SpecId.Dialog -> Dialogs(s)
        SpecId.EmptyState -> EmptyStates(s)
        SpecId.Slider -> Sliders(s)
        SpecId.Segmented -> Segments(s)
        SpecId.Tooltip -> Tooltips(s)
        SpecId.Menu -> Menus(s)
        SpecId.Drawer -> Drawers(s)
        SpecId.Sheet -> Sheets(s)
        SpecId.Pull -> Pulls(s)
        SpecId.Search -> Searches(s)
        SpecId.DatePicker -> DatePickers(s)
        SpecId.TimePicker -> TimePickers(s)
        SpecId.Table -> Tables(s)
        SpecId.Carousel -> Carousels(s)
        SpecId.Theme -> Themes(s)
        SpecId.Icons -> IconsSpec(s)
        SpecId.Patterns -> Patterns(s)
        SpecId.Tape -> Tapes(s)
        SpecId.KeyframeLoss -> KeyframeLosses(s)
    }
}

@Composable
private fun Buttons(s: SampleStrings) {
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
private fun IconButtons(s: SampleStrings) {
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
private fun Fabs(s: SampleStrings) {
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
private fun Chips(s: SampleStrings) {
    var chosen by remember { mutableIntStateOf(1) }
    States(s) {
        Cell(s.stRest) { MutantChip(s.noise, false, {}, key = "chip-rest") }
        Cell(s.stPressed) { MutantChip(s.noise, false, {}, key = "chip-pressed", interactionSource = held(true)) }
        Cell(s.stSelected) { MutantChip(s.recordings, true, {}, key = "chip-selected") }
        Cell(s.stDisabled) { MutantChip(s.favourites, false, {}, enabled = false, key = "chip-off") }
    }
    TryIt(s) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(s.all, s.recordings, s.noise, s.favourites).forEachIndexed { i, label ->
                MutantChip(label, chosen == i, { chosen = i }, key = "chip$i")
            }
        }
    }
}

@Composable
private fun Checkboxes(s: SampleStrings) {
    var a by remember { mutableStateOf(false) }
    var b by remember { mutableStateOf(true) }
    States(s) {
        Cell(s.stUnchecked) { MutantCheckbox(a, { a = it }, label = s.headNoise, key = "cb-un") }
        Cell(s.stChecked) { MutantCheckbox(b, { b = it }, label = s.autoTracking, key = "cb-on") }
        Cell(s.stDisabled) { MutantCheckbox(true, {}, label = s.frameLoss, enabled = false, key = "cb-off") }
    }
}

@Composable
private fun Radios(s: SampleStrings) {
    var speed by remember { mutableIntStateOf(1) }
    States(s) {
        Cell(s.stUnselected) { MutantRadioButton(speed == 0, { speed = 0 }, label = s.longPlay, key = "rb-a") }
        Cell(s.stSelected) { MutantRadioButton(speed == 1, { speed = 1 }, label = s.standardPlay, key = "rb-b") }
        Cell(s.stDisabled) { MutantRadioButton(true, {}, label = "EP", enabled = false, key = "rb-off") }
    }
}

@Composable
private fun Switches(s: SampleStrings) {
    var a by remember { mutableStateOf(false) }
    var b by remember { mutableStateOf(true) }
    States(s) {
        Cell(s.stOff) { MutantSwitch(a, { a = it }, label = s.clean, key = "sw-off") }
        Cell(s.stOn) { MutantSwitch(b, { b = it }, label = s.frameLoss, key = "sw-on") }
        Cell(s.stDisabled) { MutantSwitch(true, {}, label = s.autoTracking, enabled = false, key = "sw-dis") }
    }
}

@Composable
private fun Fields(s: SampleStrings) {
    var value by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(s.nightTue) }
    States(s) {
        Cell(s.stRest, FIELD) {
            MutantTextField(
                value,
                { value = it },
                Modifier.fillMaxWidth(),
                prefix = s.search,
                placeholder = s.findRecording,
                key = "tf-rest",
            )
        }
        Cell(s.stFocused, FIELD) {
            MutantTextField(
                focused,
                { focused = it },
                Modifier.fillMaxWidth(),
                prefix = s.name,
                key = "tf-focus",
                interactionSource = held(focus = true),
            )
        }
        Cell(s.stError, FIELD) {
            MutantTextField("99", {}, Modifier.fillMaxWidth(), prefix = s.channel, isError = true, key = "tf-error")
        }
        Cell(s.stDisabled, FIELD) {
            MutantTextField(s.untitled, {}, Modifier.fillMaxWidth(), prefix = s.name, enabled = false, key = "tf-off")
        }
    }
}

@Composable
private fun Cards(s: SampleStrings) {
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
private fun Cocoons(s: SampleStrings) {
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
private fun TopBars(s: SampleStrings) {
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
private fun TabsSpec(s: SampleStrings) {
    var tab by remember { mutableIntStateOf(0) }
    States(s) {
        Cell(s.stRest, BAR) {
            MutantTabs(listOf(MutantTab(s.tracks), MutantTab(s.notes), MutantTab(s.copies)), tab, { tab = it })
        }
    }
}

@Composable
private fun NavBars(s: SampleStrings) {
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
private fun ListItems(s: SampleStrings) {
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
private fun Dividers(s: SampleStrings) {
    States(s) { Cell(s.stRest, BAR) { MutantDivider(key = "d0") } }
}

@Composable
private fun Badges(s: SampleStrings) {
    States(s) {
        Cell(s.stRest) { MutantBadge(s.brandNew) }
        Cell(s.stRest) { MutantBadge("CH 03") }
    }
}

@Composable
private fun Snackbars(s: SampleStrings) {
    var shown by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stShown, BAR) { MutantSnackbar(s.queued, actionLabel = s.undo, key = "snack-rest") }
    }
    TryIt(s) {
        Column(Modifier.width(BAR), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MutantTextButton(s.showSnackbar, { shown = true }, key = "show-snack")
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                MutantSnackbar(s.queued, visible = shown, actionLabel = s.undo, onAction = { shown = false })
            }
        }
    }
}

@Composable
private fun LinearProgresses(s: SampleStrings) {
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
private fun CircularProgresses(s: SampleStrings) {
    States(s) { Cell(s.stIndeterminate) { MutantCircularProgress() } }
}

@Composable
private fun Dialogs(s: SampleStrings) {
    var open by remember { mutableStateOf(false) }
    States(s) {
        Cell(s.stOpen, STAGE_W) {
            Stage(STAGE_W, 300.dp) {
                MutantDialog(true, s.eraseQuestion, {}, text = s.eraseText) {
                    MutantDialogAction(s.cancel, {})
                    MutantDialogAction(s.erase, {}, primary = true)
                }
            }
        }
    }
    TryIt(s) {
        Stage(STAGE_W, 300.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MutantCard(Modifier.fillMaxWidth(), key = "dlg-card") {
                    MutantCardTitle(s.untitled)
                    MutantCardSubtitle(s.sideA)
                }
                MutantButton(s.openDialog, { open = true }, key = "open-dialog")
            }
            MutantDialog(open, s.eraseQuestion, { open = false }, text = s.eraseText) {
                MutantDialogAction(s.cancel, { open = false })
                MutantDialogAction(s.erase, { open = false }, primary = true)
            }
        }
    }
}

@Composable
private fun EmptyStates(s: SampleStrings) {
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
private fun Sliders(s: SampleStrings) {
    var tracking by remember { mutableIntStateOf(3) }
    var sharp by remember { mutableIntStateOf(-2) }
    States(s) {
        Cell(s.stRest, FIELD) {
            MutantSlider(tracking, { tracking = it }, Modifier.fillMaxWidth(), label = s.tracking, key = "sl-rest")
        }
        Cell(s.stDragging, FIELD) {
            MutantSlider(
                sharp,
                { sharp = it },
                Modifier.fillMaxWidth(),
                label = s.sharpness,
                key = "sl-drag",
                interactionSource = held(press = true),
            )
        }
        Cell(s.stDisabled, FIELD) {
            MutantSlider(-4, {}, Modifier.fillMaxWidth(), label = s.sharpness, enabled = false, key = "sl-off")
        }
    }
}

@Composable
private fun Segments(s: SampleStrings) {
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

@Composable
private fun Tooltips(s: SampleStrings) {
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
private fun Menus(s: SampleStrings) {
    var chosen by remember { mutableIntStateOf(0) }
    TryIt(s) { MutantDropdown(s.setting, s.menuItems, chosen, { chosen = it }) }
}

@Composable
private fun Drawers(s: SampleStrings) {
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
private fun Sheets(s: SampleStrings) {
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
            Stage(STAGE_W, 320.dp) {
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

@Composable
private fun Pulls(s: SampleStrings) {
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

@Composable
private fun Searches(s: SampleStrings) {
    var query by remember(s) { mutableStateOf(s.night) }
    var empty by remember { mutableStateOf("") }
    Variants(s) {
        Cell(s.vSuggestions, FIELD) {
            MutantSearchBar(empty, { empty = it }, Modifier.fillMaxWidth(), prefix = s.search, clearLabel = s.clear) {
                MutantSearchSuggestions(listOf(s.nightToTuesday, s.nightAir), { empty = it })
            }
        }
        Cell(s.vResults, FIELD) {
            MutantSearchBar(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                prefix = s.search,
                clearLabel = s.clear,
                key = "search-b",
            ) {
                Column {
                    MutantSearchResult(
                        s.nightTuesday,
                        query,
                        supporting = s.recordedOver,
                        leadingIcon = MutantIcons.Schedule,
                        onClick = {},
                    )
                    MutantDivider(key = "search")
                    MutantSearchResultLoading(key = "search")
                }
            }
        }
    }
    States(s) {
        Cell(s.stFocused, FIELD) {
            MutantSearchBar(
                s.night,
                {},
                Modifier.fillMaxWidth(),
                prefix = s.search,
                clearLabel = s.clear,
                key = "search-f",
                interactionSource = held(focus = true),
            )
        }
    }
}

@Composable
private fun DatePickers(s: SampleStrings) {
    var day by remember { mutableIntStateOf(13) }
    var month by remember { mutableIntStateOf(11) }
    var year by remember { mutableIntStateOf(2026) }
    States(s) {
        Cell(s.stRest) {
            MutantDatePicker(
                year,
                month,
                day.coerceAtMost(31),
                { day = it },
                { y, m ->
                    year = y
                    month = m
                },
                monthNames = s.monthNames,
                weekdayNames = s.weekdayNames,
                previousLabel = s.previousMonth,
                nextLabel = s.nextMonth,
            )
        }
    }
}

@Composable
private fun TimePickers(s: SampleStrings) {
    var hour by remember { mutableIntStateOf(21) }
    var minute by remember { mutableIntStateOf(30) }
    States(s) {
        Cell(s.stRest) {
            MutantTimePicker(hour, minute, { h, m ->
                hour = h
                minute = m
            }, hourLabel = s.hours, minuteLabel = s.minutes)
        }
    }
}

@Composable
private fun Tables(s: SampleStrings) {
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

@Composable
private fun Carousels(s: SampleStrings) {
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

/** Each palette under its own theme, on its own ground. */
@Composable
private fun Themes(s: SampleStrings) {
    Variants(s) {
        for ((name, colors) in listOf(s.themeEfir to MutantPalettes.Efir, s.themeKopiya to MutantPalettes.Kopiya)) {
            Cell(name, CARD) {
                MutantTheme(colors = colors) {
                    MutantScreenClock {
                        Column(
                            Modifier.fillMaxWidth().background(MutantTheme.colors.page).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            MutantCard(Modifier.fillMaxWidth(), onClick = {}, key = "theme-card-$name") {
                                MutantCardTitle(s.untitled)
                                MutantCardSubtitle(s.sideA)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MutantButton(s.play, {}, key = "theme-b-$name")
                                MutantChip(s.noise, true, {}, key = "theme-c-$name")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconsSpec(s: SampleStrings) {
    Variants(s) {
        for ((name, icon) in MutantIcons.all) Cell(name, ICON_CELL) { MutantIcon(icon, name, size = 24.dp) }
    }
}

@Composable
private fun Patterns(s: SampleStrings) {
    Variants(s) {
        for (p in MutantPattern.entries) {
            Cell("${p.name} · ${s.vLoud}") { Box(Modifier.size(96.dp).mutantPattern(p)) }
            Cell("${p.name} · ${s.vQuiet}") { Box(Modifier.size(96.dp).mutantPattern(p, quiet = true)) }
        }
    }
}

@Composable
private fun Tapes(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, CARD) {
            Box(Modifier.fillMaxWidth().mutantTape()) {
                MutantCard(Modifier.fillMaxWidth(), key = "tape-card") {
                    MutantCardTitle(s.tapeOver)
                    MutantCardSubtitle(s.sideA)
                }
            }
        }
    }
}

@Composable
private fun KeyframeLosses(s: SampleStrings) {
    States(s) {
        Cell(s.stRest, STAGE_W) {
            Stage(STAGE_W, 200.dp) {
                MutantKeyframeLoss(Modifier.fillMaxSize(), MutantTheme.colors.page) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MutantCard(Modifier.fillMaxWidth(), key = "loss-card") {
                            MutantCardTitle(s.nightTuesday)
                            MutantCardSubtitle(s.recordedOver)
                        }
                        MutantButton(s.play, {}, key = "loss-button")
                    }
                }
            }
        }
    }
}

/** Between sections, and between a group's title and its first section. */
private val SECTION_GAP = 24.dp
private val SECTION_PADDING = 28.dp
private val ROW_GAP = 28.dp

/** Between specimens, and from a specimen to its caption: clear of the drips a body hangs below its slot. */
private val CELL_GAP = 32.dp
private val CAPTION_GAP = 26.dp
private val CELL_MIN = 96.dp

/** A section's head beside its rows from this width. */
private val WIDE = 900.dp
private val HEAD_WIDTH = 260.dp

/**
 * The specimens' widths: a field, a card, a bar, a staged overlay, a table, an icon. Two fields or two cards stand in a
 * row of a section's rows on a 1 024 dp window (620 dp across).
 */
private val FIELD = 280.dp
private val CARD = 290.dp
private val BAR = 360.dp
private val STAGE_W = 360.dp
private val TABLE = 520.dp
private val ICON_CELL = 72.dp
