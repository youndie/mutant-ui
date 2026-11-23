---
id: B-174
title: "The sample's own defects that the look-and-motion pass reported"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-174 — The sample's own defects that the look-and-motion pass reported

[B-168](B-168-look-and-motion-pass.md)'s agents looked at every component as the sample composes it and reported what
lies in `sample/` rather than in their components. This item takes those findings, one by one, in `sample/` alone:
`mutant-core/` is not touched, and a finding whose fix needs the kit or a look the design does not draw is left with
its reason. B-151's word that the demo is not to be modified was for its performance work only; these are the sample's
own defects.

- **The decision.** Each finding was looked at again as the sample composes it (throwaway tests, frames in
  `/private/tmp/b174/`, not kept), then fixed in the sample where the fix is the sample's, with a kept test in
  `sample/src/desktopTest` for what a user sees and a mutant killed per test. Where the fix needs the kit, or motion or
  a placement the design does not draw (D15), the finding is left here with what it needs.
- Rejected: fixing a left finding in the sample by copying the kit's timings or internal parameters into it (the
  drawer's 880 ms hold, the press's fixed heat) — the sample would drift from the kit the next time either changes.
- AC: every finding below fixed or left with its reason; each fix held by a test that fails with the fix reverted; the
  sample's gate (`:sample:desktopTest`, `:sample:ktlintCheck`, `:mutant-core:desktopTest`, `:sample:compileKotlinWasmJs`)
  green; no golden moved.
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoQueue.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/ComponentsPage.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoApp.kt`

## The findings

What the look found before any change (frames and logs in `/private/tmp/b174/before/`):

| Finding | What the sample does | Decision |
|---|---|---|
| F-buttons-07 | Over 30 s a filled button's torn drops reach 28 dp under its slot; the caption stands 26 dp under it (11 fill pixels in the caption at 9.5 s) | fix: the caption gap clears falling drops |
| F-icon-fab-02 | The pressed cells hold a press, which needs a running clock; the kit's fixed heat is internal | left: needs the kit |
| F-text-field-09 | A click into the rest field leaves two fields with brackets and a caret | fix: the held focus stands down while a field of its section is really focused |
| F-app-bar-tabs-07 | The Recording tabs' content swaps in one frame (20 446 pixels at 240 ms) | left: an undesigned transition, the owner's with F-screens-08 |
| F-list-divider-badge-03 | Queue rows and dividers keyed `queue-$i`, `q$i` | fix: keyed by the record |
| F-snackbar-02 | «Try it»'s 56 dp holder does not clip: the bar rides over the band and vanishes at 224 ms | fix: the holder clips its foot |
| F-snackbar-03 | The design stands the bar 18 dp over the phone's foot, B-26 rides it 8 dp under its own | left: the design's 18 against B-26's 8 |
| F-dialog-08, F-drawer-11 | No `returnFocusTo` in the five dialogs and drawers; and on the page the «open» specimens take the focus as they appear and keep it, so «Try it»'s dialog and drawer never get it (Escape reaches the specimen, whose dismissal does nothing) | fix: openers named; the still specimens take no focus |
| F-drawer-12 | The tab changes at the pick, under the drawer's 880 ms hold | left: when to change it is the owner's, and the kit gives no `onHidden` |
| F-menu-09 | The sort menu stands at the button's start and its 216 dp run past the 360 dp phone (the row's words from 311 dp, the report's 123 dp over) | fix: stood at the button's end |
| F-tooltip-03 | The stepped-motion hint is one 1 405 dp line; the mosaic hint, beside it, a line cut at the window's left edge (523 dp of it inside) | fix: both hints shortened to a line no wider than their titles |
| F-pickers-09 | The 13th chosen in November is chosen in every month | fix: the chosen day is kept with its month |
| F-data-table-02 | No `rowKey` | fix: `rowKey = { it.title }` |
| F-foundations-06 | The Theme specimens' `MutantTheme(colors)` resets the header's switches: 10 455 pixels change in 500 ms under reduce motion | fix: the specimens take the header's four switches |
| F-foundations-07 | The page's «Эфир» Tape over the «Копия» specimen (one row in three a scanline: 185.3 against 200.2) and over the Tape specimen's own | left: the page's Tape is measured phone work (B-140, B-141, B-151); an owner decision needing a phone measurement |
| F-screens-02 | A fling at a tab's top scrolls the page: 289 → 150 px at the lift, → 0 after | fix: the phone keeps the fling it leaves |
| F-screens-03 | Enter on a navigation item leaves nothing focused | fix: the focus is put back on the chosen item |
| F-screens-05 | Every recording reads «Side A · 46 MIN» | fix: the side and the line from the record; the tracks left |
| F-screens-06 | The progress reads «026%» where the design reads «12:04» | fix: the elapsed time beside the bar |

## Findings, 2026-11-23

On `fix/b-174-sample-after-the-pass` from `c6fa836`, on this Mac (the desktop JVM, `runComposeUiTest` with the clock
held). Everything is in `sample/`; `mutant-core/` is untouched and no golden moved (the sample has none of its own;
`DemoScreensTest`'s sheets are for the eye). Frames after the fixes in `/private/tmp/b174/after/`.

**Fixed**, one commit each, each held by a kept test whose mutant was killed (the line reverted after the commit, the
aimed test run, the `<failure>` read from the JUnit XML, the file checked out again):

| Finding | The fix | Held by | Mutant, and what failed |
|---|---|---|---|
| F-list-divider-badge-03 | `DemoQueue.kt`: rows and dividers in `key(t.title)`, the bodies keyed `queue-<title>`, `q-<title>` | `DemoRecordsTest`: under reduce motion and the clean signal, each row below the new tape draws the same picture a row lower, 0 pixels unlike (limit 0.2 %) | keyed by place: 878, 1 082 and 1 004 of 22 960 pixels unlike |
| F-data-table-02 | `DemoSearch.kt`: `rowKey = { it.title }` | `DemoRecordsTest`: «Test card» chosen, sorted longest first; in the first frame the place it left shows 0 acid pixels (limit under 28) and the place it reached 287 of its 277 at rest (limit over 90 %) | `rowKey = null`: 327 acid pixels left on «Rain over the transmitter» |
| F-pickers-09 | `ComponentsSelection.kt`, `DemoSettings.kt`: the chosen day kept with its month and year (`SampleDate`), shown chosen in that month alone; the timer line reads the chosen month | `DemoRecordsTest`: on the page the 13th chosen in November, none in December, none in November once the 20th of December is chosen; in the demo's timer the 24th in November, none in December | `dayIn` returning the day in any month: «December … was: [13]» |
| F-screens-05, F-screens-06 | `DemoRecording.kt`: the side and its line from the tape (the design's tape keeps the design's words); the bar without the kit's value and the elapsed time beside it in the OSD amber, as the design's `12:04` | `DemoRecordsTest`: «Untitled No. 3» reads «Side A», its line and 12:04, no «026%»; «Rain over the transmitter» reads «Side B», «31:12 · CH 05» and 8:11 | every tape the design's: «the tape's own side … was: 0» |
| F-snackbar-02 | `ComponentsCommunication.kt`: the holder cuts what is drawn below its foot, nothing at its sides or top | `SampleLayoutTest`: with the clean signal, no pixel of the 24 dp under the holder changes through the ride out, 16–320 ms | the clip removed: 3 211 to 8 896 pixels a frame |
| F-menu-09 | `DemoSearch.kt`: the menu stands at `DpOffset(button width − 216 dp, 50 dp)`, the kit's width and top by its KDoc | `SampleLayoutTest`: the menu ends inside the phone, at the button's end within 1 dp | the start's offset: «the menu ends at 483.0, the phone at 360.0» |
| F-tooltip-03 | `SampleStrings.kt`: the stepped-motion hint «On: 12 steps a second, as drawn» (RU «Вкл.: 12 шагов в секунду»); the mosaic switch's title «Mosaic past the outline, B-137» and hint «On: blocks cross the outline» | `SampleLayoutTest`: in both languages each hint's line no wider than its title's | the long sentence back: «hint 726 dp, title 203 dp» |
| F-buttons-07 | `ComponentsPage.kt`: `CAPTION_GAP` 26 → 36 dp: drips hang 24 dp at most (`OVERHANG_MAX`), their drops fall up to 11 dp further (§1.15, B-25) | `SampleLayoutTest`: the Button section's eight captions take no fill pixel in 30 s, every 32 ms | 26 dp: 11 fill pixels at 9 408–9 504 ms, 1–3 at 21.7, 22.0 and 28.6 s |
| F-screens-02 | `DemoPage.kt`: a nested-scroll connection on the phone takes what a fling leaves (`onPostScroll` from a side effect, `onPostFling`) | `DemoPhoneTest`: a fling at the Library tab's top leaves the page where the finger left it (111 px, against 0 before); a pull to refresh released moving leaves it at 289 | the connection removed: 0 and 0 |
| F-dialog-08, F-drawer-11 | `ComponentsOverlays.kt`, `ComponentsNavigation.kt`, `DemoSearch.kt`, `DemoSettings.kt`, `DemoApp.kt`: each dialog and drawer names its opener; the erase dialog the sort button once nothing is left to erase; the Settings screen's dialog, open with the screen, and the demo's drawer the tab bar's menu. **Found on the way:** the «open» specimens of the dialog, the drawer and the sheet took the focus as the page opened and held it, so «Try it»'s overlay beside them never got it; `Stage(still = true)` is a focus group the focus never enters | `SampleFocusTest`: nothing focused as each of those sections opens; Enter on «Try it»'s button, Escape, the button focused; the demo's drawer, the archive's erase dialog and the Settings dialog likewise | still stages focusable: «a node with no words» focused; «Try it»'s dialog without `returnFocusTo`: «none»; the demo drawer's: «none» |
| F-text-field-09 | `ComponentsPage.kt`, `ComponentsSelection.kt`: `held(focus)` follows its flag; the text field's and the search bar's focused specimens stand down while a field of their section has the real focus (`SampleFocusWatch`) | `SampleFocusTest`: the specimen's amber (brackets and prefix) 278 → 118 with the rest field focused, back to 278 once the focus is cleared; the search's 314 → 154 → 314 | `held(focus = true)`: 278 and 278 |
| F-screens-03 | `DemoApp.kt`: an item taken while the bar holds the focus gets it back once the bar stands in the new tab (the bar's requester, then the focus moved to the item) | `SampleFocusTest`: Enter on Queue, Settings and On air, each focused after | the item not remembered: «after Enter on Queue: none» |
| F-foundations-06 | `ComponentsFoundations.kt`: each palette's `MutantTheme` takes the header's reduce motion, clean signal (from `MutantTheme`), stepped motion and mosaic past the outline (from `LocalSampleSettings`, provided by `MutantUiSample`) | `ThemeSpecimensTest`: under reduce motion the Theme section changes by 0 pixels in 500 ms | reduce motion not passed: 10 455 pixels, as before the fix |

- **The drag on a phone** still scrolls the page once the tab is at its end (the fling does not): on a narrow screen
  the phone fills most of the window, and a phone that kept every scroll would leave the page nothing to be scrolled by.
- **The date picker's arrows** in the demo's timer took the kit's Russian labels in English; they take the sample's now
  (in the same commit as F-pickers-09).
- **Numbers this item decides** (D15): the caption gap 36 dp; each tape opens 12:04 / 46:00 (26.2 %) into its length,
  where the design's bar is 26 % — the two of the design's numbers do not hold together, 26 % of 46:00 being 11:58; the
  elapsed time reads `m:ss`, or `h:mm:ss` on a tape of hours.

**Left**, with what each needs:

- **F-foundations-07** — the page's «Эфир» Tape still covers the «Копия» specimen (its scanlines over the paper) and the
  Tape specimen (two Tapes). A fix was built on this branch — the page's Tape drawn from an overlay with holes where a
  specimen carries its own Tape — and taken out again on the orchestrator's word: the Components page's Tape is the
  subject of measured phone work (B-140, B-141: the Tape in a layer of its own took the page from 20.9 to 7–8 ms on
  the Pixel 6a in Chrome; B-151 is the phone budget), the overlay changes what that layer draws on every frame, and
  no phone measurement is allowed in this pass. An owner decision that needs a phone measurement; the page's Tape is
  as `main` draws it.

- **F-icon-fab-02** — the pressed cells need a fixed heat, which the kit has only as an internal fixture's parameter
  (`MutantSurface`'s `heat`); a held press cannot boil under reduce motion, as the engine's pause does not. Needs a
  public pressed state or heat on the icon button, the FAB and the button: a kit item.
- **F-app-bar-tabs-07** (and F-screens-08's sample part) — the tabs' content changes in one frame; a transition is not
  drawn by the design, and none is invented here. The owner's, with F-screens-08.
- **F-snackbar-03** — the design stands the Recording screen's bar 18 dp over the foot, B-26 rides it its height and
  8 dp: 10 dp stay in the screen. Moving the 18 leaves the design; the 8 is the kit's (B-26). The owner's.
- **F-drawer-12** — whether the tab changes as the drawer goes or as the pick is made is a look for the owner; changing
  it as the drawer goes needs the drawer to say when it has gone (an `onHidden`, as the snackbar's), or the sample would
  copy B-102's 720 + 160 + 200 ms.
- **F-screens-05's tracks** — every recording still lists the design's three tracks: a list per tape is content the
  design does not give, and not a defect the record can answer.
- **The sheets' `returnFocusTo`** — not among the findings; the sample's five sheets name no opener either. The same
  remedy, for a later item.
- **F-tooltip-03's other half** — whether a long hint should wrap stays the owner's (F-tooltip-02, the plate at the
  window's edge, too). In the 1 024 dp test window both header plates now lie inside it (810–1 013 and 25–200 dp).

**Gate**, on `741e05d`: `./gradlew :sample:desktopTest :sample:ktlintCheck :mutant-core:desktopTest --continue` green,
from the JUnit XML 38 `sample` tests (22 before this item) and 495 `mutant-core`, none failed or skipped
(`DemoTapeOnceTest` passed on the first run); `:sample:compileKotlinWasmJs` built; `make check` green.

**Not verified:** the browser (the gate compiles `:sample:compileKotlinWasmJs`; nothing was run in Chrome), a phone, a
real window, and the benches.
