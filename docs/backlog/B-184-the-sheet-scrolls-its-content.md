---
id: B-184
title: "The bottom sheet scrolls its content and hands a pull at the top to its drag"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: [B-183]
---

# B-184 — The bottom sheet scrolls its content and hands a pull at the top to its drag

The bottom sheet does not scroll its content; B-178 left its content running off the screen's foot (F-bottom-sheet-05, F-bottom-sheet-08). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 30 ([решение]).

- **The decision and its reason.** The sheet scrolls its content itself. With the content at its top, a downward gesture becomes the sheet's drag (B-183).
- The sheet scrolls through a nested-scroll connection rather than wrapping its content in `verticalScroll`, so a consumer's lazy list inside it still works; the item checks that.

- AC:
  - content taller than the sheet scrolls inside it, by touch, wheel and keyboard
  - at the top, a downward drag moves the sheet, by a finger and a mouse; elsewhere a finger scrolls the content, and a mouse drag leaves the sheet where it stands *(rewritten, see the Result: a mouse drag scrolls nothing in Compose, the wheel scrolls)*
  - a `LazyColumn` inside the sheet scrolls and does not throw
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantBottomSheet.kt`

## Result

### 2026-11-24

- **Looked at first**: throwaway contact sheets (`/private/tmp/b184/sheets/`, not in the repository) of a sheet with a title,
  twelve list items and a button in a 360 × 600 dp screen: at rest, after a finger swipes up 240 dp, a finger pulls down
  300 dp and lets go, the wheel five notches down and back, Tab sixteen times and PageUp; in «Эфир» at density 1, «Копия»
  at density 2, «Эфир» under reduce motion, and the same sheet with a `LazyColumn` of 30 rows. Before: nothing scrolled by
  a finger, the wheel or the keys, the rows ran off the screen's foot and the button was never reached; the finger's pull
  dragged the sheet down from anywhere; in the lazy sheet the list scrolled itself, its button was measured 0 tall and a
  pull at the list's top never reached the sheet. After: the content scrolls by all three and is cut under the grabber
  while scrolled, Tab brings the focused row whole into view, a pull at the top drags the sheet and boils its body, and
  in the lazy sheet the list scrolls, hands what is left to the sheet (its button whole at the end) and a pull at its
  top drags the sheet.
- **The scroll** (`SheetContent`, `SheetScroll` in `MutantBottomSheet.kt`): the sheet's content is no longer a `Column`
  but a column of its own that the sheet scrolls, laid out as `verticalScroll` lays its viewport out, without its clip.
  Each child is measured to its own height (its intrinsic height, or the room when that is more); a child that cannot
  say it before it is measured, a lazy list or a `BoxWithConstraints` (its intrinsics throw), is given the room, a
  screenful, and scrolls itself; the sheet scrolls on where it stops, by the nested scroll. So a consumer's `LazyColumn`
  works and does not throw, where a `verticalScroll` around the content would give it no height limit and throw. A
  consumer's own `verticalScroll` is given its whole height now, and the sheet scrolls it. `ColumnScope`'s `align` and
  `weight` work as in a `Column` (a weighted child shares what room the others leave); `alignBy` has no effect, and the
  KDoc says so. The content is cut at its top only while scrolled away from it, so a sheet at rest draws as before, its
  last button's drips included.
- **By the keys**: Compose's bring-into-view stopped short of a focused control below the foot (63 dp short in the test)
  while the scroll answered a consumption a float's rounding off what it was asked; the scroll now answers all of an
  unclamped delta, exactly, as `ScrollState` does. PageUp and PageDown move the content by its room.
- **The hand-over** (`PanelDrag.kt`, `SheetPull`): the drag's gesture now sees a move before the content does. With the
  content at its top a move past the slop down is the sheet's before the content's scroll can take it, and the sheet
  follows 1:1 from the first move as in B-183. A press with the content scrolled away from its top is the content's for
  the whole gesture, by a finger and a mouse. A finger's scroll that brings the content to its top goes on as the sheet's
  drag: what the scroll leaves over down reaches a nested-scroll connection above the content, which pulls the sheet by
  it, 1:1; scrolled back up, the sheet goes back to open before the content moves; the fling at the lift lets the sheet go
  as a release does (B-183's 40 % and 800 dp/s). Only while a finger or a pen presses the sheet: a wheel's scroll, the
  keys and a fling never pull it. With a content that scrolls, a move up is no longer followed through the slop (it
  lifted the sheet 3.6 dp past open and settled it back on every scroll up); a content that does not scroll keeps
  B-183's pull past open. Whether the content is at its top reads the scrolls passing through a second connection
  between the sheet's scroll and the content (`consumed.y < 0` away, `available.y > 0` at it, cmp-ui-component §4).
- **AC 2 rewritten**: a mouse drag scrolls nothing in Compose, on desktop and in the browser, so away from the top a
  mouse drag scrolls nothing here either and leaves the sheet where it stands; the wheel scrolls the content, and at the
  top the mouse drags the sheet (B-183). The answer («жест вниз переходит в перетаскивание листа» at the top) holds for
  both.
- **Tests**, `SheetScrollTest` (new; 360 × 360 dp, the window's top read from its dismiss node, the scroll from the
  title):
  - a finger 120 dp up scrolls the content past the slop, the window still on every frame; eight wheel notches scroll
    it, twenty back stop at the top with the window still on every frame and nothing dismissed; Tab focuses the button
    under the foot and brings it whole onto the screen; PageUp and PageDown move it by 280 dp or more; at densities 1
    and 2 (AC 1);
  - at the top a finger and a mouse move the window 6 dp a frame with the content still; scrolled by the wheel, a pull
    down leaves the window at rest on every frame, a finger scrolling the content back (AC 2);
  - scrolled 80 dp, one finger gesture down: the window still until the content is at its top, then follows within a
    step on every frame, ending at the travel past the content's top less the scroll's slop, within 4 dp; let go at 60 dp it settles back, at 160 dp it is dismissed once
    (AC 2's hand-over);
  - a title, a `LazyColumn` of 40 rows and a button: composed without a throw, a finger scrolls the list and not the
    sheet, the wheel to the list's end and on brings the button whole (40 dp or more) onto the screen, and back at the tops a
    finger's pull moves the window 120 dp after 120 dp; at densities 1 and 2 (AC 3).
  - `SheetBehaviourTest`'s «a content taller than the screen …» now holds the last line whole and on the screen after
    the wheel, plain and in a consumer's `verticalScroll` (before, its plain rows past the room were measured 0 tall).
- **Before the change** (`origin/main`'s `MutantBottomSheet.kt`, `PanelDrag.kt` and `SheetParityTest.kt`), all five
  fail: `a finger: the window stays … actual -3.6`; `a finger: scrolled 0.0 after eight notches` (twice); `the button 0.0
  dp tall`; `scrolls=false: the last line 0 tall`.
- **Mutants**, after `7894743e` and `8a56c22c`, each against its aimed test, read from its JUnit XML, the file copied
  back and the tree clean after each:
  - the scroll answering `to − offset` (the float's rounding back): killed, AC 1 (`Tab: the button's foot at 411.0`);
  - a press scrolled away from the top dragging the sheet (`!atTop()` dropped): killed, AC 2 (`a finger, scrolled: the
    window stays … actual 6.0`);
  - the connection never pulling (`onPostScroll` takes nothing): killed, the hand-over (`expected 57.0 … actual 0.0`);
  - a lazy list given no height limit: killed, AC 3 (`Vertically scrollable component was measured with an infinity
    maximum height`);
  - every child squeezed into the room left, as the page: killed, `SheetBehaviourTest` (`the last line 0 tall`);
  - a move up followed through the slop again: killed, AC 1 (`the window stays … actual -3.6`);
  - the parity switch off: killed, `SheetParityTest` (`off: [MutantSheet_Open_Efir, MutantSheet_Open_Kopiya]`).
- **Parity cells moved**: `MutantSheet_Open_Efir` and `MutantSheet_Open_Kopiya`. The page's content stands 2 dp taller
  than its room in the kit (159 against 157 px); the page's column squeezed its button box from 42 to 40 px, and the kit
  now keeps it whole, so «Готово» lies a row lower (its top at 273 against the page's 272) and the cell is off on the
  word's top row. A departure, research D20's registry: `LocalMutantBottomSheetAsDesigned` gives each child the room the
  ones above it left, as the page's column does, and with it both cells read exactly as on `main` (2.082 % and 2 103 px
  in «Эфир», 1.709 % and 1 726 px in «Копия», the bare twins 0 and 40 px). No golden moved: the kit has no sheet golden
  but these cells.
- **ABI**: unchanged; every new declaration is internal or private (`checkKotlinAbi` green).
- **Gates on this Mac**: `make check` green; `./gradlew check` green (676 tests in `mutant-core`'s and the sample's
  desktop suites, none failed, read from the JUnit XML); `./gradlew :mutant-core:compileKotlinIosArm64` green;
  `make docs-against BASE=origin/main` clean.
- **Not verified**: a phone, a real window and the browser (no device, adb or real browser used); a stylus; an Android
  stretch overscroll (none is applied: what the content leaves over goes to the sheet); a lazy list moved by its own
  state in code is not seen as away from its top until it scrolls (the KDoc says so).

### Findings

- **F-1, a lazy list's two-digit rows lose their first digit** — seen, not investigated. In the contact sheet of the
  sheet holding a `LazyColumn` of `MutantListItem("Строка $it")`, rows 10 to 13 read «Строка 0» to «Строка 3» in some
  frames and right in others; before and after this change alike, and not with the same rows in a plain column. Possibly
  the list item's text under a lazy list's reuse; for an item of its own.
- **F-2, the first scroll up of a consumer's lazy list lifts the sheet** 0.3 of the slop past open and settles it back,
  once: the sheet learns that its content scrolls only from the first scroll it sees pass.
