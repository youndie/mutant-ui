package io.github.youndie.mutant.qa

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.youndie.mutant.components.MutantBadge
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
import io.github.youndie.mutant.components.MutantDivider
import io.github.youndie.mutant.components.MutantEmptyState
import io.github.youndie.mutant.components.MutantExtendedFab
import io.github.youndie.mutant.components.MutantFab
import io.github.youndie.mutant.components.MutantIconButton
import io.github.youndie.mutant.components.MutantLinearProgress
import io.github.youndie.mutant.components.MutantListItem
import io.github.youndie.mutant.components.MutantNavigationBar
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
import io.github.youndie.mutant.components.MutantTopAppBar
import io.github.youndie.mutant.components.TooltipPlate
import io.github.youndie.mutant.icons.MutantIcon
import io.github.youndie.mutant.icons.MutantIcons
import io.github.youndie.mutant.illustration.MutantIllustration
import io.github.youndie.mutant.illustration.MutantIllustrationKind
import io.github.youndie.mutant.pattern.MutantPattern
import io.github.youndie.mutant.pattern.mutantPattern
import io.github.youndie.mutant.surface.mutantTape
import kotlin.math.abs

/**
 * The QA pass's catalogue (B-40): every public component, given a modifier and the one text a consumer would make
 * long or empty, drawn with no help from the caller.
 */
internal class Subject(
    val name: String,
    val content: @Composable (Modifier, String) -> Unit,
)

/** 300 characters with no space: no line break can save it. */
internal val NO_SPACES = "Перемотка".repeat(34).take(300)

/** 300 characters of words. */
internal val WORDS = "Плёнка зажёвана, ".repeat(18).take(300)

/** Mixed scripts: emoji, Arabic, Hebrew and a combining mark. */
internal val MIXED = "📼 كاسيت שלום é 👩‍👩‍👧‍👦"

internal val subjects: List<Subject> =
    listOf(
        Subject("Button") { m, t -> MutantButton(t, {}, m) },
        Subject("Button.Loading") { m, t -> MutantButton(t, {}, m, state = MutantButtonState.Loading) },
        Subject("Button.Success") { m, t -> MutantButton(t, {}, m, state = MutantButtonState.Success) },
        Subject("Button.Icon") { m, t -> MutantButton(t, {}, m, icon = MutantIcons.Add) },
        Subject("OutlinedButton") { m, t -> MutantOutlinedButton(t, {}, m) },
        Subject("TextButton") { m, t -> MutantTextButton(t, {}, m) },
        Subject("IconButton") { m, t -> MutantIconButton(MutantIcons.Search, t, {}, m) },
        Subject("Fab") { m, t -> MutantFab(MutantIcons.Add, t, {}, m) },
        Subject("ExtendedFab") { m, t -> MutantExtendedFab(t, MutantIcons.Add, {}, m) },
        Subject("Chip") { m, t -> MutantChip(t, false, {}, m) },
        Subject("Chip.Selected") { m, t -> MutantChip(t, true, {}, m) },
        Subject("Checkbox") { m, t -> MutantCheckbox(true, {}, m, label = t) },
        Subject("RadioButton") { m, t -> MutantRadioButton(true, {}, m, label = t) },
        Subject("Switch") { m, t -> MutantSwitch(true, {}, m, label = t) },
        Subject("Slider") { m, t -> MutantSlider(3, {}, m, label = t) },
        Subject("SearchBar") { m, t ->
            MutantSearchBar(t, {}, m) {
                MutantSearchSuggestions(listOf(t, "ночной эфир"), {})
                MutantSearchResult(t, t.take(4), supporting = t, leadingIcon = MutantIcons.Search, onClick = {})
                MutantSearchResultLoading()
            }
        },
        Subject("SearchSuggestions") { m, t -> MutantSearchSuggestions(listOf(t, t, "ночь"), {}, m) },
        Subject(
            "SearchResult",
        ) { m, t -> MutantSearchResult(t, "ночь", m, supporting = t, leadingIcon = MutantIcons.Search) },
        Subject("SearchResultLoading") { m, _ -> MutantSearchResultLoading(m) },
        Subject("Carousel") { m, t ->
            MutantCarousel(listOf(MutantCarouselItem(t, t, t), MutantCarouselItem(t, t), MutantCarouselItem("Эфир")), m)
        },
        Subject("DataTable") { m, t ->
            MutantDataTable(
                listOf(t, "Нет сигнала"),
                listOf(
                    MutantTableColumn<String>(t) { it },
                    MutantTableColumn(t, 90.dp, MutantTableCellStyle.Osd) { it },
                    MutantTableColumn("КАНАЛ", 80.dp, MutantTableCellStyle.OsdSecondary) { it },
                ),
                m,
                setOf(0),
                { _, _ -> },
            )
        },
        Subject("SegmentedButton") { m, t -> MutantSegmentedButton(listOf(t, "LP", "EP"), 0, {}, m) },
        Subject("Tooltip") { m, t -> TooltipPlate(t, null, "tip", m) },
        Subject("Tooltip.Rich") { m, t -> TooltipPlate(t, "ОСТАЛОСЬ 3 МИН", "tip-rich", m) },
        Subject(
            "PullToRefresh",
        ) { m, t -> MutantPullToRefresh(true, {}, m, refreshingLabel = t) { MutantListItem(t) } },
        Subject("TextField") { m, t -> MutantTextField(t, {}, m, prefix = "CH", placeholder = "Название") },
        Subject("TextField.Placeholder") { m, t -> MutantTextField("", {}, m, prefix = t, placeholder = t) },
        Subject("TextField.Error") { m, t -> MutantTextField(t, {}, m, isError = true) },
        Subject("Card") { m, t ->
            MutantCard(m) {
                MutantCardTitle(t)
                MutantCardSubtitle(t)
            }
        },
        Subject("Card.Clickable") { m, t -> MutantCard(m, onClick = {}) { MutantCardTitle(t) } },
        Subject("Cocoon") { m, _ -> MutantCocoon(m.fillMaxWidth()) },
        Subject("TopAppBar") { m, t ->
            MutantTopAppBar(
                t,
                m,
                navigationIcon = { MutantIconButton(MutantIcons.Back, "back", {}) },
                status = "CH 03 · SP",
            ) { MutantIconButton(MutantIcons.Settings, "settings", {}) }
        },
        Subject("Tabs") { m, t -> MutantTabs(listOf(MutantTab(t), MutantTab("Б"), MutantTab("В")), 0, {}, m) },
        Subject("NavigationBar") { m, t ->
            MutantNavigationBar(
                listOf(
                    MutantNavigationItem(t, MutantIcons.Home, badge = t.take(3)),
                    MutantNavigationItem("Поиск", MutantIcons.Search),
                ),
                0,
                {},
                m,
            )
        },
        Subject("ListItem") { m, t ->
            MutantListItem(
                t,
                m,
                supporting = t,
                leadingIcon = MutantIcons.Schedule,
                trailingText = "12:04",
                onClick = {},
            )
        },
        Subject("ListItem.Badge") { m, t -> MutantListItem(t, m, trailing = { MutantBadge(t) }) },
        Subject("Divider") { m, _ -> MutantDivider(m) },
        Subject("Badge") { m, t -> MutantBadge(t, m) },
        Subject("Snackbar") { m, t -> MutantSnackbar(t, m, actionLabel = "ПОВТОР") },
        Subject("Snackbar.LongAction") { m, t -> MutantSnackbar("Сохранено", m, actionLabel = t) },
        Subject("LinearProgress") { m, _ -> MutantLinearProgress(.42f, m) },
        Subject("LinearProgress.Indeterminate") { m, _ -> MutantLinearProgress(m) },
        Subject("CircularProgress") { m, _ -> MutantCircularProgress(m) },
        Subject("EmptyState") { m, t ->
            MutantEmptyState(MutantIllustrationKind.Television, t, m, text = t) { MutantButton(t, {}) }
        },
        Subject("Illustration") { m, _ -> MutantIllustration(MutantIllustrationKind.TestCard, m) },
        Subject("Icon") { m, t -> MutantIcon(MutantIcons.Info, t, m) },
        Subject("Pattern+Tape") { m, _ -> Box(m.fillMaxWidth().mutantPattern(MutantPattern.Mosaic).mutantTape()) },
    )

/** Where a consumer puts a component: each case wraps it, sizes it, or both. */
internal class Place(
    val name: String,
    val wrap: @Composable (@Composable (Modifier) -> Unit) -> Unit,
)

internal val places: List<Place> =
    listOf(
        Place("width 0") { c -> c(Modifier.width(0.dp)) },
        Place("width 1") { c -> c(Modifier.width(1.dp)) },
        Place("width 24") { c -> c(Modifier.width(24.dp)) },
        Place("width 48") { c -> c(Modifier.width(48.dp)) },
        Place("width 120") { c -> c(Modifier.width(120.dp)) },
        Place("height 0") { c -> c(Modifier.size(200.dp, 0.dp)) },
        Place("box 10") { c -> Box(Modifier.size(10.dp)) { c(Modifier) } },
        Place("unbounded width") { c ->
            Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) { c(Modifier) }
        },
        Place("unbounded height") { c ->
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) { c(Modifier) }
        },
        Place("unbounded both, fill") { c ->
            Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    c(Modifier.fillMaxSize())
                }
            }
        },
    )

/**
 * Composes [content] in a fresh test under [io.github.youndie.mutant.theme.MutantTheme], runs the clock a few
 * frames, draws the whole window, and returns what went wrong: null when nothing threw.
 */
@OptIn(ExperimentalTestApi::class)
internal fun attempt(
    frames: Int = 3,
    check: ComposeUiTest.() -> Unit = {},
    content: @Composable () -> Unit,
): Throwable? =
    try {
        runComposeUiTest {
            mainClock.autoAdvance = false
            // A stage of its own: the window is as large as what it holds, and a zero-sized one cannot be captured.
            setContent { Box(Modifier.size(STAGE_W, STAGE_H)) { content() } }
            repeat(frames) { mainClock.advanceTimeByFrame() }
            mainClock.advanceTimeBy(300)
            onRoot().captureToImage()
            check()
        }
        null
    } catch (t: Throwable) {
        t
    }

internal val STAGE_W = 400.dp
internal val STAGE_H = 800.dp

/** One line naming a throwable and where in the kit it came from. */
internal fun Throwable.brief(): String {
    val kit =
        stackTrace.firstOrNull {
            it.className.startsWith("io.github.youndie.mutant") &&
                !it.className.contains(".qa.")
        }
    return "${this::class.simpleName}: ${message?.lineSequence()?.firstOrNull()?.take(160)} @ $kit"
}

internal fun ImageBitmap.pixels(): List<Color> =
    toPixelMap().let { px -> List(px.width * px.height) { px[it % px.width, it / px.width] } }

internal fun SemanticsNodeInteraction.pixels(): List<Color> = captureToImage().pixels()

/** How many pixels differ between two captures of the same size; all of them when the sizes differ. */
internal fun diff(
    a: List<Color>,
    b: List<Color>,
): Int = if (a.size != b.size) maxOf(a.size, b.size) else a.indices.count { a[it] != b[it] }

/** Every semantics node under the root, unmerged. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.allNodes(): List<SemanticsNode> {
    fun walk(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap { walk(it) }
    return walk(onRoot(useUnmergedTree = true).fetchSemanticsNode())
}

/** Where the focused nodes stand, in the root. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.focusedBounds(): List<Rect> =
    allNodes().filter { it.config.getOrElse(SemanticsProperties.Focused) { false } }.map { it.boundsInRoot }

/** The node whose text or content description starts with [text] (is [text], when [exact]), unmerged. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.boundsOf(
    text: String,
    exact: Boolean = false,
): Rect {
    fun matches(s: String) = if (exact) s == text else s.startsWith(text)
    return allNodes()
        .first { n ->
            n.config.getOrElse(SemanticsProperties.Text) { emptyList() }.any { matches(it.text) } ||
                n.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.any { matches(it) }
        }.boundsInRoot
}

/** Presses a key at the root and lets the clock run a little. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.press(key: Key) {
    onRoot().performKeyInput { pressKey(key) }
    mainClock.advanceTimeBy(100)
}

/** Within 3 % per channel, opaque. */
internal fun near(
    a: Color,
    b: Color,
): Boolean = abs(a.red - b.red) < .03f && abs(a.green - b.green) < .03f && abs(a.blue - b.blue) < .03f && a.alpha > .9f

/** The box of every pixel with any ink, `[left, top, right, bottom]`; right < left when there is none. */
internal fun PixelMap.inkBox(): IntArray {
    var l = Int.MAX_VALUE
    var t = Int.MAX_VALUE
    var r = -1
    var b = -1
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (this[x, y].alpha > .05f) {
                l = minOf(l, x)
                r = maxOf(r, x)
                t = minOf(t, y)
                b = maxOf(b, y)
            }
        }
    }
    return intArrayOf(l, t, r, b)
}

/** The columns where at least [rows] pixels are [color]: a block caret of that colour stands in them. */
internal fun PixelMap.columnsOf(
    color: Color,
    rows: Int = 18,
): List<Int> = (0 until width).filter { x -> (0 until height).count { y -> near(this[x, y], color) } >= rows }

/** At density 1, under the theme with motion paused: what the keyboard and mouse sweeps stand on. */
@Composable
internal fun Still(
    density: Density = Density(1f),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDensity provides density) {
        io.github.youndie.mutant.theme
            .MutantTheme(reduceMotion = true, content = content)
    }
}
