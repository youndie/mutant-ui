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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.youndie.mutant.components.MutantChip
import io.github.youndie.mutant.theme.MutantScreenClock
import io.github.youndie.mutant.theme.MutantTheme
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
internal fun Variants(
    s: SampleStrings,
    content: @Composable FlowRowScope.() -> Unit,
) = SpecRow(s.rowVariants, "row-variants", content)

@Composable
internal fun States(
    s: SampleStrings,
    content: @Composable FlowRowScope.() -> Unit,
) = SpecRow(s.rowStates, "row-states", content)

@Composable
internal fun TryIt(
    s: SampleStrings,
    content: @Composable FlowRowScope.() -> Unit,
) = SpecRow(s.rowTry, "row-try", content)

/** A specimen with its caption: the caption stands clear of the drips the body hangs below its slot. */
@Composable
internal fun Cell(
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
internal fun held(
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
internal fun Stage(
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
internal val FIELD = 280.dp
internal val CARD = 290.dp
internal val BAR = 360.dp
internal val STAGE_W = 360.dp
internal val TABLE = 520.dp
internal val ICON_CELL = 72.dp
