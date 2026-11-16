package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-99: the Components page has one structure for every section — a head (name, purpose, stage, the public functions it
 * shows) and labelled rows of captioned specimens — its states are the design's table's, it covers every public
 * function of the kit, it opens at its top, and its index scrolls to a group.
 */
@OptIn(ExperimentalTestApi::class)
class ComponentsPageTest {
    private val s = SampleStrings(SampleLanguage.English)

    /** The states each section shows as the design's table names them; none where a state is only seen by doing. */
    private val states: Map<SpecId, List<String>> =
        mapOf(
            SpecId.Button to listOf(s.stRest, s.stPressed, s.stLoading, s.stSuccess, s.stDisabled),
            SpecId.IconButton to listOf(s.stRest, s.stPressed, s.stDisabled),
            SpecId.Fab to listOf(s.stRest, s.stPressed),
            SpecId.Chip to listOf(s.stRest, s.stPressed, s.stSelected, s.stDisabled),
            SpecId.Checkbox to listOf(s.stUnchecked, s.stChecked, s.stDisabled),
            SpecId.Radio to listOf(s.stUnselected, s.stSelected, s.stDisabled),
            SpecId.Switch to listOf(s.stOff, s.stOn, s.stDisabled),
            SpecId.TextField to listOf(s.stRest, s.stFocused, s.stError, s.stDisabled),
            SpecId.Card to listOf(s.stRest, s.stDisabled),
            SpecId.Cocoon to listOf(s.stLoading),
            SpecId.TopAppBar to listOf(s.stRest),
            SpecId.Tabs to listOf(s.stRest),
            SpecId.NavigationBar to listOf(s.stRest),
            SpecId.ListItem to listOf(s.stRest),
            SpecId.Divider to listOf(s.stRest),
            SpecId.Badge to listOf(s.stRest, s.stRest),
            SpecId.Snackbar to listOf(s.stShown),
            SpecId.LinearProgress to listOf(s.stDeterminate, s.stIndeterminate),
            SpecId.CircularProgress to listOf(s.stIndeterminate),
            SpecId.Dialog to listOf(s.stOpen),
            SpecId.EmptyState to listOf(s.stRest),
            SpecId.Slider to listOf(s.stRest, s.stDragging, s.stDisabled),
            SpecId.Segmented to listOf(s.stRest, s.stDisabled),
            SpecId.Tooltip to emptyList(),
            SpecId.Menu to emptyList(),
            SpecId.Drawer to listOf(s.stOpen),
            SpecId.Sheet to listOf(s.stOpen),
            SpecId.Pull to listOf(s.stRefreshing),
            SpecId.Search to listOf(s.stFocused),
            SpecId.DatePicker to listOf(s.stRest),
            SpecId.TimePicker to listOf(s.stRest),
            SpecId.Table to listOf("${s.stRest} · ${s.stRowSelected}"),
            SpecId.Carousel to listOf(s.stRest),
            SpecId.Theme to emptyList(),
            SpecId.Icons to emptyList(),
            SpecId.Patterns to emptyList(),
            SpecId.Tape to listOf(s.stRest),
            SpecId.KeyframeLoss to listOf(s.stRest),
        )

    private fun page(
        scroll: ScrollState = ScrollState(0),
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { MutantUiSample(SampleSettings(language = SampleLanguage.English), scroll) }
        mainClock.advanceTimeBy(1_000)
        block()
    }

    private fun SemanticsNodeInteractionCollection.texts(): List<String> =
        fetchSemanticsNodes().flatMap { n ->
            n.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
        }

    private fun inSection(id: SpecId) = hasAnyAncestor(hasTestTag("section-${id.name}"))

    @Test
    fun `every section has its head and labelled rows of captioned specimens`() =
        page {
            val failures = ArrayList<String>()
            for (id in SpecId.entries) {
                val head = onAllNodes(hasAnyAncestor(hasTestTag("head-${id.name}")), useUnmergedTree = true).texts()
                val badge =
                    when (id.stage) {
                        SpecStage.Mvp -> s.stageMvp
                        SpecStage.V1 -> s.stageV1
                        SpecStage.Later -> s.stageLater
                        SpecStage.Base -> s.stageBase
                    }
                for (want in listOf(id.en, id.enLine, badge) + id.api) {
                    if (want !in head) failures += "${id.name}: the head lacks «$want»"
                }
                val rows =
                    listOf("row-variants", "row-states", "row-try").filter {
                        onAllNodes(
                            hasTestTag(it) and inSection(id),
                            useUnmergedTree = true,
                        ).fetchSemanticsNodes().isNotEmpty()
                    }
                if (rows.isEmpty()) failures += "${id.name}: no row"
                for (row in rows) {
                    val label =
                        onAllNodes(
                            hasAnyAncestor(hasTestTag(row) and inSection(id)),
                            useUnmergedTree = true,
                        ).texts()
                    val want =
                        when (row) {
                            "row-variants" -> s.rowVariants
                            "row-states" -> s.rowStates
                            else -> s.rowTry
                        }
                    if (want !in label) failures += "${id.name}: $row is not labelled"
                }
            }
            assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        }

    @Test
    fun `each section shows the states the design's table names`() =
        page {
            val failures = ArrayList<String>()
            for (id in SpecId.entries) {
                val shown =
                    onAllNodes(
                        hasTestTag("caption") and hasAnyAncestor(hasTestTag("row-states") and inSection(id)),
                        useUnmergedTree = true,
                    ).texts()
                val want = states.getValue(id)
                if (shown != want) failures += "${id.name}: $shown, the design's $want"
            }
            assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        }

    @Test
    fun `the catalogue names every public function of the kit, and only real ones`() {
        val sources =
            File("../mutant-core/src/commonMain/kotlin")
                .walk()
                .filter { it.extension == "kt" }
                .joinToString("\n") { it.readText() }
        val public =
            Regex("""(?m)^public fun (?:<[^>]+> )?(Mutant[A-Za-z]+)\(""")
                .findAll(sources)
                .map { it.groupValues[1] }
                .toSet()
        val listed = SpecId.entries.flatMap { it.api }.toSet()
        assertEquals(emptySet(), public - listed, "public functions no section names")
        val unreal =
            listed.filter {
                !Regex("""(?m)^public (?:fun (?:<[^>]+> )?(?:Modifier\.)?|(?:data |enum )?class |object |val )$it\b""")
                    .containsMatchIn(sources)
            }
        assertEquals(emptyList(), unreal, "names a section shows that the kit does not have")
    }

    @Test
    fun `the page opens at its top though its overlays are shown from the first frame`() {
        val scroll = ScrollState(0)
        page(scroll) { assertEquals(0, scroll.value, "the page's scroll after a second") }
    }

    @Test
    fun `the index scrolls to a group`() {
        val scroll = ScrollState(0)
        page(scroll) {
            onNode(hasText("${s.groupLater} · ${SpecId.entries.count { it.stage == SpecStage.Later }}")).performClick()
            mainClock.advanceTimeBy(1_000)
            val top = onNodeWithTag("group-Later").fetchSemanticsNode().boundsInRoot.top
            assertTrue(scroll.value > 0, "the page scrolled: ${scroll.value}")
            assertTrue(top in 0f..200f, "the group's title stands near the top: $top")
        }
    }
}
