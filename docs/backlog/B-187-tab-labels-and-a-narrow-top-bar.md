---
id: B-187
title: "Tab labels hand the accent over late; a narrow top bar drops its status first"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-187 — Tab labels hand the accent over late; a narrow top bar drops its status first

The tabs' labels turn in the click's first frame while the bar takes 330 ms (F-app-bar-tabs-04); a top bar under about 170 dp gives its title zero width and pushes its action out (F-app-bar-tabs-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 9 and 31 ([решение]).

- **The decision and its reason.** The old label loses its accent in the first frame, the new takes it after 280 ms, as the menu and the segmented button do (the designer's words; see the Result for what those two actually do). In a narrow top bar the status on the right (`CH 03 · SP`) goes first, then the title is cut with an ellipsis; the navigation icon stays.

- AC:
  - after a click the new tab's label shows its accent from 280 ms, the old none from the first frame
  - at widths from 360 down to 120 dp: the navigation icon and the action in full, the status dropped before the title is cut down to the status's own width (its whole width, when shorter), the title ellipsised, no node out of the bar — rewritten from «the status dropped before the title is cut», see the Result
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt`

## Result, 2026-11-24

`MutantTabs.kt` and `MutantTopAppBar.kt` only. On `feat/b-187-tab-labels-and-a-narrow-top-bar`, on this Mac, desktop JVM,
`runComposeUiTest` with the clock driven by hand.

- **The tabs' labels** (answer 9). A label's colour is now read at draw time: the selected label is in the accent only
  once `TAB_ACCENT_HANDOVER_MS` (280) has passed on the screen's clock since the bar set off (`TabSlide.since`, the
  composition that saw the selection, the one in which the bar's run starts), and until then in the secondary colour,
  as the old one is from the selection's first frame. While it waits the word reads the clock's frames, so it turns in
  the first frame 280 ms on (288 ms at the test clock's 16 ms frames) and reads nothing after. Under reduce motion, and
  when the bar arrives with no run (from no selection, `-1`), the accent is there at once: the finished frame (D24), and
  no bar to wait for. A second click during the wait starts the new label's 280 ms from that click; the label clicked
  away never takes the accent. The 280 is a named constant beside the bar's `slide` 330, as D24 says for a number an
  answer names; no token holds it. **The menu and the segmented button have no 280 ms hand-over of their own** to reuse:
  their words turn by where the venom's fill is drawn under them (`WordPass.OnSlime`, `MenuWordPass.OnSlime`), not on a
  timer. The answer's «как в Menu и Segmented button» reads as the look (the word turns late, after the selection has
  arrived), and the number is the answer's.
- **The narrow top bar** (answer 31). The bar's row is a layout of its own (`TopAppBarRow`) in place of a `Row` with a
  weighted title: the navigation icon and the actions keep their widths, the title takes what is left, and the status
  stands only while the title keeps at least the status's own width, or its whole width when that is less. Past that the
  status is not placed (neither drawn nor found by a reader), and the title takes its room, ending in an ellipsis once it
  no longer fits. With the sample's bar (the menu icon, «On air», `CH 03 · SP`, the search icon) the status goes
  between 240 and 200 dp at density 1 and «On air» stands whole down to about 130 dp, «On …» at 120. Alignment, gaps and the
  rounding of the half pixel are the `Row`'s, so nothing moves where everything fits.
- **The rewritten AC.** Read literally, «the status dropped before the title is cut» would remove the status from every
  bar whose title is long, at any width: B-23 decided that a long title ends in an ellipsis before the whole status, and
  the question (F-app-bar-tabs-06) asked only what gives way once the title has no room left. So the status goes before
  the title is cut below the status's own width (designed here, D15: no new number, the status's width is the floor),
  and a short title, which is narrower than the status, is never cut while the status stands: for it the AC holds as
  written. Numbers the answer does not state: that floor, and the reduce-motion and no-run cases of the hand-over.

**Tests** (`BarTabsAnswersTest`, each failing on the code before, run with the two files at `origin/main`):

| AC | Test | Before |
|---|---|---|
| the new label's accent from 280 ms, the old none from the first frame | «a click takes the accent off the old label in the first frame and gives it to the new one from 280 ms»: every frame for 480 ms after the click, the accent's pixels in each label's own box on the theme's ground; the old one 0 from the frame that composes the click, the new one under 10 in every frame before 280 ms (18 frames) and over 60 from it (133 measured); under reduce motion and from no selection, the new one in the accent in the first frame | the new label in the accent from the first frame |
| from 360 to 120 dp: icons whole, the status before the title, the title ellipsised, nothing out of the bar | «from 360 to 120 dp the icons stay, the status goes before the title is cut, nothing leaves the bar»: every 4 dp, densities 1 and 2, «On air» and a long title; both icons 24 dp and inside the bar, the title at least 40 dp (or whole) and inside, `TextOverflow.Ellipsis` set on its layout, the status while placed inside the bar and leaving the title at least min(whole, its own width), never back once gone, gone by 120 dp | «On air» at 204 dp: the title cut to 48 dp, the status kept; under 170 dp the title 0 wide and the action pushed out |

`BarTabsBehaviourTest`'s first test read the new label's accent one frame after the click; it now reads it 320 ms on.
`ScreenTunesInTest` (B-180's timing of the tabs' bar) passes unchanged. The ellipsis is held by the layout's
`overflow` (Skiko's `isLineEllipsized` answers false) and by the drawn dots, looked at in the frames below.

**Mutants**, each after the commit (`037db62`), one line changed, the aimed test named among the failures in the JUnit
XML, the file copied back, `git status` clean:

| Mutant | Killed by |
|---|---|
| the accent at once (`>= 0.0` for `>= TAB_ACCENT_HANDOVER_MS`) | «a click takes the accent …»: the new label in the accent from the first frame (163 px) |
| reduce motion waits too (`reduce ||` removed) | «a click takes the accent …»: «reduce true, from 0: the new label at once» |
| a bar with no run waits too (`slide.fromX == null ||` removed) | «a click takes the accent …»: «reduce false, from -1: the new label at once» |
| the status always kept (`takeIf { true }`) | «from 360 to 120 dp …»: «On air at 204 dp, density 1.0: the title cut to 48.0, the status kept» |

**Goldens and parity cells.** None moved: `BarTabsParityTest` (`MutantTopAppBar_Rest_*`, `MutantTabs_Rest_*`) passes
unchanged, and no golden was re-recorded. The cells draw the bar at rest at its full width and the tabs at rest, where
the new layout places every part where the `Row` did and the selected label is in the accent from its first composition.
No departure, so research D20's registry gains no row.

**Frames looked at** (throwaway sheets, deleted; frames in `/private/tmp/b187/before` and `/private/tmp/b187/after`, not
in the repository): a click from the first tab to the second, 26 frames of 16 ms, in «Эфир» and «Копия», at density 1
and 2, with and without reduce motion; the top bar with the menu icon, `CH 03 · SP` and the search icon, «On air» and a
long title, at 360, 240, 200, 180, 170, 160, 150, 140, 130 and 120 dp, in both palettes at densities 1 and 2. Before: the
new label lime (blue on «Копия») in the frame after the click; under 170 dp the title gone, at 140 the search icon squeezed
to 20 dp, at 120 not shown at all. After: the old label grey from that frame, the new one grey under the running
bar and lime 288 ms after the bar set off (the first 16 ms frame past 280); under reduce motion both turn in the click's frame;
the bar drops `CH 03 · SP` between 240 and 200 dp, «On air» whole to 130, «On …» at 120, the long title's dots drawn at
every width, the icons whole and inside.

**Gates**, on this Mac: `./gradlew check` green (169 desktop test classes, every `@Test` executed, no `<failure>` in the
JUnit XML; `checkKotlinAbi` unchanged, the public API is the same), `./gradlew :mutant-core:compileKotlinIosArm64` green,
`make check` and `make docs-against BASE=origin/main` green (`make check`'s one warning, `no-repo-url` on `services/mutant-core.md`, stands on `main` too).

**Not verified.** No phone and no browser: Android, iOS and wasm only as `./gradlew check` compiles and runs them, and
`compileKotlinIosArm64`. A screen reader not finding a dropped status is read from the node not being placed, not from
a reader. The echo changing side in the click's frame (the other half of F-app-bar-tabs-04) is not in this item's ACs
and is unchanged.
