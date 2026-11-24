package io.github.youndie.mutant.sample

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.youndie.mutant.tokens.MutantPalettes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B-99: the Components page has one structure for every section — a head (name, purpose, the public functions it shows)
 * and labelled rows of captioned specimens — its states are the design's table's, it covers every public function of the
 * kit, and it opens at its top. B-148: its index chooses one group of sections, the one composed.
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
        settings: SampleSettings = SampleSettings(language = SampleLanguage.English),
        scroll: ScrollState = ScrollState(0),
        block: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { MutantUiSample(settings, scroll) }
        mainClock.advanceTimeBy(1_000)
        block()
    }

    /**
     * B-148: the page composes one group at a time; [block] runs on each in turn, on a page opened at it. B-210: a page of
     * its own for each group, where one page switched through all seven in 7 s on a Mac, a body of 1 s each.
     */
    private fun everyGroup(block: ComposeUiTest.(SpecGroup) -> Unit) {
        for (group in SpecGroup.entries) {
            page(SampleSettings(language = SampleLanguage.English, group = group)) { block(group) }
        }
    }

    private val sections =
        SemanticsMatcher("a section") {
            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("section-")
        }

    private fun ComposeUiTest.composedSections(): List<String> =
        onAllNodes(sections, useUnmergedTree = true).fetchSemanticsNodes().map {
            it.config[SemanticsProperties.TestTag].removePrefix("section-")
        }

    private fun SemanticsNodeInteractionCollection.texts(): List<String> =
        fetchSemanticsNodes().flatMap { n ->
            n.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
        }

    private fun inSection(id: SpecId) = hasAnyAncestor(hasTestTag("section-${id.name}"))

    @Test
    fun `every section has its head and labelled rows of captioned specimens`() {
        val failures = ArrayList<String>()
        everyGroup { group ->
            for (id in SpecId.entries.filter { it.group == group }) {
                val head = onAllNodes(hasAnyAncestor(hasTestTag("head-${id.name}")), useUnmergedTree = true).texts()
                for (want in listOf(id.en, id.enLine) + id.api) {
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
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `each section shows the states the design's table names`() {
        val failures = ArrayList<String>()
        everyGroup { group ->
            for (id in SpecId.entries.filter { it.group == group }) {
                val shown =
                    onAllNodes(
                        hasTestTag("caption") and hasAnyAncestor(hasTestTag("row-states") and inSection(id)),
                        useUnmergedTree = true,
                    ).texts()
                val want = states.getValue(id)
                if (shown != want) failures += "${id.name}: $shown, the design's $want"
            }
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
        // Each group opened on its own: the overlays shown from the first frame are in Overlays, Navigation and
        // Communication, none of them the default group.
        for (group in SpecGroup.entries) {
            val scroll = ScrollState(0)
            page(SampleSettings(language = SampleLanguage.English, group = group), scroll) {
                assertEquals(0, scroll.value, "${group.name}: the page's scroll after a second")
            }
        }
    }

    /** B-148: the groups as the owner's round decided them, each in its order; the catalogue must say the same. */
    private val decided =
        mapOf(
            SpecGroup.Foundations to "Theme Icons Patterns Tape KeyframeLoss",
            SpecGroup.Actions to "Button IconButton Fab Segmented",
            SpecGroup.Selection to "Checkbox Radio Switch Slider Chip TextField Search DatePicker TimePicker",
            SpecGroup.Containment to "Card ListItem Divider Carousel Table",
            SpecGroup.Navigation to "TopAppBar Tabs NavigationBar Drawer Menu",
            SpecGroup.Communication to
                "Badge LinearProgress CircularProgress Cocoon Snackbar Tooltip EmptyState Pull",
            SpecGroup.Overlays to "Dialog Sheet",
        ).mapValues { it.value.split(" ") }

    @Test
    fun `the index chooses a group, and the page composes that group's sections alone`() {
        val settings = SampleSettings(language = SampleLanguage.English)
        page(settings) {
            assertEquals(SpecGroup.Foundations, settings.group, "the default group")
            assertEquals(decided.getValue(SpecGroup.Foundations), composedSections(), "the page as it opens")
            val seen = HashMap<String, MutableList<SpecGroup>>()
            for (group in SpecGroup.entries) {
                val n = SpecId.entries.count { it.group == group }
                onNode(hasText("${group.title(s)} · $n")).performClick()
                mainClock.advanceTimeBy(1_000)
                assertEquals(group, settings.group, "the chip of ${group.name} chose it")
                for (other in SpecGroup.entries) {
                    val chip = onNode(hasText("${other.title(s)} · ${SpecId.entries.count { it.group == other }}"))
                    if (other == group) chip.assertIsSelected() else chip.assertIsNotSelected()
                }
                onNodeWithTag("group-${group.name}").assertExists()
                val composed = composedSections()
                assertEquals(decided.getValue(group), composed, "the sections composed under ${group.name}")
                for (id in composed) seen.getOrPut(id) { ArrayList() } += group
            }
            assertEquals(SpecId.entries.map { it.name }.toSet(), seen.keys, "every section is in a group")
            assertEquals(emptyMap(), seen.filterValues { it.size != 1 }, "sections in more than one group")
        }
    }

    /**
     * The texts of the page opened at each group, or at each demo screen when [demo], in [language], that name a design
     * stage. B-210: each place opens a page of its own, as `the page opens at its top` does, where one page per language
     * switched through all ten in 12–18 s on a Mac and passed its minute under load (PR #136's CI, run 45262647941). A
     * page opened at a place reads the texts a page switched to it reads, compared place by place when this changed.
     */
    private fun stagesNamed(
        language: SampleLanguage,
        demo: Boolean,
    ): List<String> {
        val stage = Regex("""(?i)\bmvp\b|\bv1\b|\blater\b|позже""")
        val places =
            if (demo) {
                DemoScreen.entries.map {
                    "Demo, ${it.name}" to SampleSettings(language = language, page = SamplePage.Demo, demoScreen = it)
                }
            } else {
                SpecGroup.entries.map { it.name to SampleSettings(language = language, group = it) }
            }
        val failures = ArrayList<String>()
        for ((where, settings) in places) {
            page(settings) {
                val texts = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                for (t in texts.texts()) if (stage.containsMatchIn(t)) failures += "$language, $where: «$t»"
            }
        }
        return failures
    }

    @Test
    fun `no design stage is named on the Components page, in English`() =
        assertEquals(emptyList(), stagesNamed(SampleLanguage.English, demo = false))

    @Test
    fun `no design stage is named on the Components page, in Russian`() =
        assertEquals(emptyList(), stagesNamed(SampleLanguage.Russian, demo = false))

    @Test
    fun `no design stage is named on the Demo page, in English`() =
        assertEquals(emptyList(), stagesNamed(SampleLanguage.English, demo = true))

    @Test
    fun `no design stage is named on the Demo page, in Russian`() =
        assertEquals(emptyList(), stagesNamed(SampleLanguage.Russian, demo = true))

    @Test
    fun `the page's own words read at 4_5 to 1 on a section's ground in both palettes`() {
        // The page's words: its titles and heads in text, its purposes and captions in the secondary text, the functions
        // in the OSD amber, all on a section's ground, the page's on the group titles.
        val failures = ArrayList<String>()
        for ((name, c) in listOf("Efir" to MutantPalettes.Efir, "Kopiya" to MutantPalettes.Kopiya)) {
            for ((what, ink, ground) in listOf(
                Triple("name", c.text, c.background),
                Triple("purpose, caption, row label", c.textSecondary, c.background),
                Triple("function", c.textOsd, c.background),
                Triple("group title", c.text, c.page),
                Triple("group line", c.text, c.page),
            )) {
                val r = ratio(ink, ground)
                if (r < 4.5) failures += "$name, $what: ${"%.2f".format(r)}:1"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun ratio(
        a: androidx.compose.ui.graphics.Color,
        b: androidx.compose.ui.graphics.Color,
    ): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (maxOf(la, lb) + .05f) / (minOf(la, lb) + .05f)
    }
}
