# B-168 — app-bar-tabs: `MutantTopAppBar`, `MutantTabs`, `MutantTab`

Group 8 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-app-bar-tabs` from `07b2f7d`,
2026-11-22, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). The components live in
`mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTopAppBar.kt` and `MutantTabs.kt`; their
numbers were decided in [B-23](../../docs/backlog/B-23-top-app-bar-and-tabs.md) (the bar's run, the echo, the targets),
[B-60](../../docs/backlog/B-60-tabs-that-do-not-fit.md) (the scrolling row) and
[B-65](../../docs/backlog/B-65-tabs-and-navigation-in-rtl.md) (right to left). Neither draws a body: the bar is a row of
words over [`MutantDivider`](../../mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDivider.kt)
(group 10's), the tabs are words over a 4 dp bar with its echo.

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-app-bar-tabs/` (not in the
repository): `core/` and `sample/` before the fixes, `after/` and `after-sample/` after, the same scenes.

- **The components as the sample composes them** (`core/`): the sample's `TopBars` and `TabsSpec` cells
  (`ComponentsNavigation.kt`: the bar with the menu icon, «On air», `CH 03 · SP` and the search icon; the three tabs) in
  one 360 dp column on the theme's ground, in eight looks: «Эфир», «Копия», reduce motion, «Чистый сигнал»,
  `steppedMotion`, density 2, and the browser's way of drawing (`LocalMutantFirstFrameWaits` false,
  `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false) in both palettes. Per look,
  every frame captured over each scene, sheets of the frames (the tabs zoomed 2×) and a per-frame pixel diff
  (`core/looks.txt`):
  - appear (`<look>-appear`);
  - a mouse click on the second tab, the third, and back to the first (`<look>-click1`, `-click2`, `-clickBack`);
  - fast repeated clicks: the second tab, the third 64 ms later, the first 64 ms after that (`<look>-fast`);
  - the keyboard: Tab, Tab, Enter (`<look>-keyboard`);
  - two seconds at rest (`<look>-rest`).
  And: twelve tabs in 360 dp, the last selected from the first and back, a click, Tab through them and Enter
  (`core/overflow-*`, `core/overflow.txt`); right to left, clicks on the third tab and the second (`core/rtl-*`); the bar
  with a long title at 360, 240, 160 and 120 dp at densities 1 and 2 (`core/title-*`); a tab focused and selected, 4×
  (`core/zoom-focus-*`).
- **The sample itself** (`sample/`): `MutantUiSample(SampleSettings(group = Navigation))` in «Эфир» and «Копия», the page
  scrolled to the bar and the tabs, each tab clicked (`nav-*`, the Tape running over them); the demo's Recording screen
  (`DemoScreenFrame(Recording)`, its bar with the back and rename actions and its three tabs) in both palettes, each tab
  clicked and a fast pair of clicks (`rec-*`, `sample/rec.txt`). The Library screen has no `MutantTabs`: its tabs are the
  navigation bar (group 9); B-151 measured those.

## Findings

### F-app-bar-tabs-01 — a click during the bar's run jumps it to the tab it was running to — local

- **What.** The bar runs from the tab it left (`TabSlide.from`), and `from` is the previous *selection*, not where the
  bar stands. A second selection during a run starts the new run from the first one's target: the bar leaps there in one
  frame, then runs. The same class as B-93 (a selection's slime jumping instead of moving).
- **Frames.**
  - `core/Efir-fast.png`: 4656 → 4672 ms the bar leaps from between «Tracks» and «Notes» to under «Notes» (1 282 pixels in
    that frame against 273 the frame after); 4720 → 4736 ms from between «Notes» and «Copies» to under «Copies» (1 319),
    then runs back to «Tracks». The same in «Копия», «Чистый сигнал», the browser looks, density 2 (4 428 and 4 540) and
    under `steppedMotion` (1 320, 1 361).
  - The demo: `sample/rec-Efir-fast.png`, 4480 → 4496 ms.
  - Read by a test: the bar's left edge, frame by frame after a click 48 ms into a run, `[36, 101, 113, 124, …]` — 65 px
    in one frame where a run's fastest frame moves 14 % of its way.
- **The design.** The page draws the bar at rest; B-23 decided the run (330 ms, eased out by the cube, the echo trailing).
  Nothing decided a jump; the brief's failure class says a selection moves.
- **Fixed** (`44d4d59`). `TabSlide` keeps where the run starts, not which tab: at a new selection it takes the bar's pose
  at that moment (under the tab it left at rest, partway along a run still going), read without subscribing the
  composition to the layout's marks. Nothing changes at rest or for a single run; -1 still brings the bar with no run.
  `BarTabsBehaviourTest` «a click during a run sends the bar on from where it stands»: a mouse click on the second tab,
  then on the third 48 ms on, each edge read with its time; no frame moves the bar's left edge back or more than 20 px
  (a run's fastest frame is 14 % of its way), and it lands under the third label. Mutant, killed by that test
  (`<failure>` read in the JUnit XML): the run's start taken at the old run's end (`progress` → `1.0` in `follow`) —
  «the bar's left edge, (ms, px): [(124, 36), (140, 46), (140, 46), (156, 101), …]».
- **After.** `after/Efir-fast.png` (the same scene): 4656 → 4672 ms the bar runs on from between «Tracks» and «Notes»
  toward «Copies» (852 pixels in that frame, the labels' colours and the echo's side included, against 1 282 before);
  4720 → 4736 it turns back to «Tracks» from where it stands (922 against 1 319). Every look the same; under reduce
  motion each click still lands in one frame.

### F-app-bar-tabs-02 — the echo is cut at the tabs' own edges — local

- **What.** Since B-60 the row is a `horizontalScroll`, which clips its content to the row. The bar and its echo are drawn
  inside it, so the echo past the last tab is cut 12 dp past its label (where the last target ends): of its three strokes
  (10, 8 and 6 dp, starting 4, 18 and 30 dp past the bar) 10 px of 24 remain at density 1. The echo trailing to the left of
  a run from the first tab is cut the same way at the row's start. A row that fits draws exactly as it did before B-60
  otherwise, which is what B-60 promised; the parity cell selects the first tab, whose echo falls inside the row, so no
  check saw it.
- **Frames.**
  - `core/Efir-rest.png` («Copies» selected): no echo past the bar; `core/zoom-focus-copies.png`, 4×: one 8 dp stroke.
  - The sample: `sample/nav-Efir-Copies.png` and `nav-Kopiya-Copies.png` from 2720 ms, one stub; the demo's Recording
    screen, `sample/rec-Efir-Copies.png` at rest.
  - The left side: `core/Efir-click1.png` 1536–1600 ms, the trailing strokes cut at the row's left edge.
  - Right to left: `core/rtl-click-copies.png`, 512 ms, the first tab (at the right) with its echo cut.
- **The design.** The page draws the bar with its whole echo (`left: 66, 80, 92` past a 62 dp bar); B-23 decided it stands
  past the bar whatever the tab. A row that scrolls (B-60) still clips at its viewport: what is past the window's edge is
  not there.
- **Fixed** (`5e72523`, `271a6d9`). The bar is drawn over the scroll instead of in it, at the scrolled row's place (the
  row's coordinates within its window, read as the row scrolls), and cut at the row's edges only while the row can
  scroll — as B-60's clip did. The echo of a row that fits now hangs past its ends as a body's drips hang past their
  slot; the KDoc says so.
  - **A new face of F-buttons-02** came up on the way: the tabs' words followed their colour only because the bar,
    drawn inside the row, redrew the row's layer every time. With the bar outside, a click left «Дорожки» in the accent
    (0 pixels of the secondary colour, `BarTabsBehaviourTest` «a click selects a tab…»). The label now hands
    `MutantText` its colour as a producer as well, which redraws the word on a colour change by itself; the cause stays
    in `MutantText.kt` (reported, F-buttons-02).
  - `BarTabsBehaviourTest` «the last tab's echo is drawn whole past the row, and a row that scrolls still clips its bar»:
    the three tabs wrapped in a 400 dp box, the last selected: 24 lit pixels past its ring on the echo's row (10 before);
    twelve tabs in a 200 dp row with the third selected: nothing of the bar past 200 dp, and the bar's first column
    under the scrolled label.
  - Mutants, each killed (`<failure>` read in the JUnit XML):
    - always cut at the row's edges (the old look): «the echo's three strokes past the last label ==> expected: <24> but
      was: <8>»;
    - never cut: «a row that scrolls draws nothing of its bar past its width ==> expected: <0> but was: <16>»;
    - the bar drawn at the window's origin, not the scrolled row's: the same test, no bar under the scrolled label
      (`NoSuchElementException` from the search for its first column; it survived until `271a6d9` added that check);
    - no colour producer on the label: «Дорожки in its colour: 0».
- **After.** `after/Efir-rest.png` («Copies» with its three strokes past the row), `after-sample/rec-Efir-Copies.png`
  (the demo, from 2176 ms), `after/rtl-click-copies.png` (right to left, «Tracks» at the right with its whole echo at
  512 ms); `after/overflow-select-last.png` (twelve tabs: the bar still cut at the row's edges, as before). The labels'
  accent pixels per click are the same as before in every look, reduce motion included (`after/looks.txt`).
  No golden or parity cell moved: `BarTabsParityTest` passes unchanged (its cells select the first tab, whose echo
  stood inside the row).

### F-app-bar-tabs-03 — the bar runs in from out of view when the row scrolls to a new tab — reported (owner)

- **What.** Twelve tabs in 360 dp, the last selected from the first: the row jumps to the last tab at once (B-60's
  decision), and the bar sets off from the first tab, now 800 dp out of view. No bar shows for 96 ms, then it flies in
  from the left edge for the remaining 230 ms (`core/overflow-select-last.png`, 528–624 ms: 0 pixels change in five
  frames; `overflow-select-first.png` 1520–1632 the same, from the right).
- **Why reported.** The run's start under a scroll is a number B-60 did not decide: land at once when the old tab is out of
  view, run from the viewport's edge, or animate the scroll with the bar. The sample and the demo never overflow.

### F-app-bar-tabs-04 — the words change at the click, the bar 330 ms later — reported (owner)

- **What.** On a selection the new label turns accent and the old one secondary in the first frame, while the bar is still
  under the old label; the echo crosses to the trailing side in that same frame (648 px in the first frame against ~250 in
  the next). `core/Efir-click1.png` 1536–1600 ms; `accent` pixel counts per label before and after in `core/looks.txt`.
- **Why reported.** The design draws only the rest; B-23 decided the bar's run and the echo's side ("on a run to the right
  the echo trails on the left"), not the words. A word following its bar (or the bar's arrival) is a look to decide.
  Contrast is never at stake: the labels stand on the page's ground.

### F-app-bar-tabs-05 — right to left, the echo still stands to the right — reported (owner)

- **What.** In a right-to-left layout the tabs mirror (B-65), but at rest the echo stays to the right of the bar, toward the
  row's start (`core/rtl-click-copies.png`, 896 ms on: under «Copies», now the leftmost, three strokes to its right).
- **Why reported.** B-65 mirrored the places, not the echo; the design draws only left to right.

### F-app-bar-tabs-06 — a bar too narrow for its parts drops the title and then the actions — reported (owner)

- **What.** With the navigation icon, `CH 03 · SP` and one action, under about 170 dp the title has no width at all (not
  even an ellipsis), and the action is pushed past the bar's end; at 120 dp the status is cut too
  (`core/title-160-d1.png`, `title-120-d1.png`; at 240 and 360 dp the long title ends in an ellipsis before the whole
  status, as B-23 decided: `title-240-d2.png`).
- **Why reported.** B-23 decided the title gives way to the status; what gives way once the title is gone (the status, or
  the actions) is not decided. No screen of the sample is that narrow.

### F-app-bar-tabs-07 — the Recording screen's tab content swaps in one frame — reported (sample)

- **What.** A click on «Notes» in the demo's Recording screen replaces the list with the notes text in one frame (19 760
  pixels at 1056 ms, `sample/rec-Efir-Notes.png`), while the bar takes 330 ms to arrive. The content under a tab is the
  sample's `when (tab)` in `DemoRecording.kt`; the design draws no tab content transition.

## Looked at and found right

- **At rest** nothing moves in either component over two seconds in any look (0 pixels change a frame): no body, the
  divider's strokes still; the Tape over the sample moves over them, not in them.
- **The run**: 330 ms on every frame (21 frames change, then none), in five 83 ms steps under `steppedMotion`, at once
  under reduce motion (one frame, then none) — in every look, both palettes and the browser's way of drawing (which no
  part of this group reads: neither draws a body).
- **The keyboard**: Tab lands on each tab in turn with the corner brackets, Enter selects it and the bar runs; in a row
  that scrolls, Tab brings each tab into view (`core/overflow-keyboard.png`). A mouse click draws no brackets.
- **Density 2**: the bar, its ring, the echo and the brackets where they stand at density 1, at twice the pixels.
- **«Копия»**: the bar's acid with the page's black ring; the echo at 60, 30 and 15 % of the acid is faint on the paper,
  as the page's literal `#b6ff1a` is.
- **Right to left**: the first tab at the right, the bar under the selected tab, the run leftward with the echo trailing
  to the right.
- **The bar**: a long title ends in an ellipsis before the whole status at 240 and 360 dp; the divider spans the bar at
  both densities.
