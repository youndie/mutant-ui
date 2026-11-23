# B-168 — drawer: `MutantNavigationDrawer`, `MutantDrawerItem`

Group 19 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-drawer` from `662fa7a`, 2026-11-22,
on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand, 2 ms of real time between frames so the
bodies built ahead are ready).

## What was looked at

Two throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-drawer/` (not in the repository):
`before/` as found, `after/` with the fixes. Each `.txt` has one line per 16 ms frame: the pixels changed since the last
frame and, where it applies, where the title stands (`titleX`, dp, from `positionInRoot`), the scrim's darkness at the
stage's corner, and the picks and dismissals the stage counted.

- **The drawer as the sample's «Try it» stage composes it** (`core/`): a 360 × 320 screen of its own
  (`MutantScreenClock`), the «Channels» opener and the drawer closing on a pick, with `returnFocusTo` on the opener. Eight
  looks: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2, and the browser's way of drawing
  (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage`
  false) in both palettes.
  - `openclose-*`: opened by a mouse click on the opener, every frame of the slide in, closed by a mouse click on the
    scrim, every frame of the slide out;
  - `reverse-*`: Escape 112 ms into the open, then the drawer reopened 96 ms into the close («Эфир», `steppedMotion`);
  - `pick-*`: an open drawer and its bare twin (`DrawerFrame(hidden = true)`, the same keys on the same clock) side by
    side, a pick by state: 2 → 0, 0 → 1, 1 → 3, and 2 → 0 picked back to 2 at 304 ms; per row, per frame, the ink its
    glyph pixels are drawn in (`on` black, `tx` the text's colour, read against the twin's ground), the tenth percentile
    of their contrast and the share under 3:1 («Эфир», «Копия», `steppedMotion`, density 2, the browser's way);
  - `pickclose-*`: the sample's flow, a row picked by the mouse, held through B-102's run and linger, a second row clicked
    400 ms into the hold, every frame to the end;
  - `keys-rf{true,false}`: Tab to the opener, Enter, Down, Tab ×6, Down, Up, Enter on a row, Escape, Enter during the
    exit; with and without `returnFocusTo`;
  - `clicks.txt`: a mouse click on the window's blank foot, then Enter and Tab; a click on a row 48 ms into the exit;
  - `loss-*`: the drawer opened while the screen's keyframe loss runs (`KeyframeLossBody` around the stage), then a pick;
  - `rtl`, `rtl-open.png`: the open and a pick right to left;
  - `still-*`: the open drawer at rest in «Эфир», «Копия», «Чистый сигнал» and density 2, with the rows' bounds (D19).
- **The sample** (`sample/`): the Components page's Navigation group scrolled to the Navigation drawer section, «Try it»
  opened and «On air» picked by the mouse, in both palettes (`page-*.png`); the demo's Library phone (which carries the
  keyframe loss), its menu clicked and «Search» picked by the mouse, in both palettes (`demo-*.png`).

## Findings

### F-drawer-01 — a drawer reversed mid-way jumps: the close from whole, the reopen from nothing — local

- **What.** The drawer calls `DialogFollow` without its two durations, so a new direction starts from its beginning:
  - Escape 112 ms into the open: the title stood at −104 dp (the window a third in) and at +7 dp the next frame, a
    110 dp jump of the whole window, then slid out from there;
  - reopened 96 ms into the close (title at −20 dp): the window vanished in one frame (−190 dp) and slid in again.
- **Frames.** `before/core/reverse-efir.png`, `.txt` (1104 → 1120 ms, 1218 → 1234 ms); `reverse-stepped` the same.
- **The rule.** cmp-ui-component §1: every change of a discrete input has its interruption; D15/B-34 decided the two
  durations, not a jump between them. The dialog's F-dialog-01, fixed in `DialogFollow` on `main`: the drawer only has to
  pass its durations.

### F-drawer-02 — a keyframe loss running as the drawer opens cuts the window for as long as it is open — local

- **What.** The drawer's window and its pill are bodies inside the screen's `MutantKeyframeLoss`, so they register with
  the screen's loss scene. A loss running when the drawer opens is held mid-run with the screen's clock, and its band keeps
  cutting the window: the opener's slime stands over the title, a black bar lies across the chosen pill, for the drawer's
  whole life.
- **Frames.** `before/core/loss-efir.png` (3904–4672 ms), `loss-kopiya.png`.
- **Reach.** The demo's Library phone carries the loss, and its menu opens this drawer: `before/sample/demo-efir.png`
  caught a loss running as the drawer opened (the mosaic at o876), with its band clear of the window. The 1 dp line across
  the window and the screen there is the Tape's band, standing with the held screen, before and after the fix.
- **The rule.** B-98: the loss covers the screen, not what floats over it; the dialog's F-dialog-02, fixed in its own
  file by providing no loss scene to its bodies.

### F-drawer-03 — focus goes back to the opener as the exit starts, so Enter during the exit reopens the drawer — local

- **What.** `ReturnFocus(visible, …)` gives the opener focus on the close's first frame, while the scrim and the window
  still cover it. Escape, then Enter during the 200 ms exit: the drawer opens again (`before/core/keys-rftrue.txt`:
  `enter-during-exit -> open=true opens=2`).
- **The rule.** The dialog's F-dialog-06: focus returns on the frame the overlay leaves.

### F-drawer-04 — a closing drawer answers its rows: through B-102's hold and through the exit — local

- **What.** The rows' `selectable` is enabled whatever `visible` says.
  - A second row clicked 400 ms into B-102's hold is picked: a second run starts from where the first stood and the hold
    begins again, 0.9 s more (`before/core/pickclose-efir.txt`: `CLICK Settings during hold at 2092, picks=2`).
  - A row clicked 48 ms into the exit after Escape is picked (`before/core/clicks.txt`: `picks=1 at=1 open=false`).
- **The rule.** B-102: «While it stands, the drawer takes no input: the scrim, Escape and back are already closed»;
  cmp-ui-component §4: a closed overlay's actions stop answering as the exit starts.

### F-drawer-05 — opened, no row has the keys, and the window itself is a blind Tab stop — local

- **What.** As it opens the drawer gives focus to its own container, which shows nothing:
  - Down and Up do nothing (`before/core/keys-rftrue.txt`, `down`);
  - the first Tab lands on the window's body (its `clickable {}`, merged with the title, `[CHANNELS]`), with no brackets;
    only the second reaches a row (`tab1`, `tab2`; `keys-rftrue.png`);
  - a mouse click on the window's blank foot focuses the same body, unseen (`clicks.txt`).
- **The rule.** B-34: «Focus moves in as it opens … Up and Down move the focus between rows … Every row draws the focus
  brackets»; B-57: what has the keys shows it. The menu (F-menu-03) and the dialog (F-dialog-03) were fixed the same way:
  the chosen row takes the keys as it opens, and the window takes its clicks as a pointer target only.

### F-drawer-06 — the words change colour on a timer, not with the slime under them: 1.06:1 for half a second — local

- **What.** `DrawerRow` colours its words by `VenomRun.labelOn` (the old row's black until 340 ms, the new one's from
  then), while the lime goes where the venom takes it. In «Эфир» a pale word on the passing lime reads 1.06:1 and a black
  one on the dark body 1.46:1:
  - 2 → 0 (`before/core/pick-2-0-efir.txt`): «Queue», passed over, pale on lime 64–352 ms (up to 52 % of its glyph
    pixels under 3:1); «On air», the new row, pale on the arriving lime 160–336 ms (up to 56 %); «Search», the row left,
    black on the dark body from 352 ms;
  - row-frames with more than a fifth of the glyph under 3:1: 47 of 240 for 2 → 0, 16 for 0 → 1, 46 for 1 → 3, 75 with a
    pick back during the run; the same under `steppedMotion` and in the browser's way; «Копия» 0;
  - the sample: `before/core/pickclose-efir.png`, `before/sample/demo-efir.png` (p1544–p1864).
- **The rule.** B-103 made the words follow the pick at the end of the run; the kit's rule since B-91/B-94 and
  cmp-ui-component §4: a word's colour follows the fill as drawn. The menu (F-menu-01) and the segmented button
  (F-segmented-01) were fixed the same way in their own files.

### F-drawer-07 — right to left the drawer slides in from the left, across the whole screen — local

- **What.** The window is laid out at the screen's start, the right edge right to left, but its slide is a negative
  `translationX` whatever the direction: it enters at the screen's left edge and crosses the screen to the right (the
  title from −32 to 170 dp, `before/core/rtl.txt`, `rtl.png`). Its body runs off by 10 dp at its left, inside the
  screen, and stops at the screen's right edge instead of running off it.
- **The rule.** The KDoc: «a `bar` … standing in from the screen's start … corners square and run off the screen's start
  and top by 10 dp»; cmp-ui-component §4: right to left mirrors the motion. The rows, the pill and the venom are already
  right: the rows' words mirror inside their slots and the window stays on them.

### F-drawer-08 — a pick during a run jumps the window to the row picked first — reported (`VenomRun.kt`, = F-segmented-05)

- The drawer's face of F-segmented-05, vertical: 2 → 0, then 2 again at 304 ms; at 320 ms about 17 000 pixels change in
  one drawer in one frame (up to 6 600 a frame through a run) as the window stands whole on «On air» and flows back from
  there (`before/core/pick-0-2-repick-0-at-304-efir.txt`, the pair's diff 33 980 at 320 ms). In the sample it comes with
  F-drawer-04: a second click during the hold (`pickclose-efir.png`, 2092 ms). The cause is F-segmented-05's.

### F-drawer-09 — the drawer cannot be dragged — reported (owner)

- **What.** Research D15 lists «sheet and drawer sliding and dragging» among the gestures designed here; B-34 built the
  slide and no drag: a drag on the window or the scrim moves nothing, and a drawer cannot be swiped shut or pulled out
  from the edge. A drag needs numbers the design does not state and no item decided (where it starts, how far it follows,
  the velocity or distance that closes it, how it settles), so it is the owner's.

### F-drawer-10 — Tab stops at the last row and goes no further — reported (owner)

- **What.** Inside the open drawer Tab steps the rows and stops on the last one (`keys-rftrue.txt`, `tab5`, `tab6`);
  only Up/Down wrap (B-34). The dialog's Tab comes round from the last action to the first (B-57). Whether the drawer's
  Tab comes round too is not decided anywhere.

### F-drawer-11 — the sample's two drawers give no `returnFocusTo`: after Escape focus is nowhere — reported (sample)

- The drawer's face of F-dialog-08: `ComponentsNavigation.kt` («Try it») and `DemoApp.kt` open the drawer without naming
  the opener (the button, the top bar's menu). After Escape no node is focused (`before/core/keys-rffalse.txt`:
  `after exit: focused=none`).

### F-drawer-12 — the demo swaps the screen under the drawer as the pick is made — reported (sample)

- **What.** `DemoApp` answers a pick with `nav.go`, which changes the tab at once; B-102 then holds the drawer 880 ms
  over the new screen, whose content has swapped in one frame under the scrim (`before/sample/demo-efir.png`: Library at
  p1308, Search at p1416). A look for the owner: the tab taken as the drawer goes, or as it is picked.

## Looked at and as decided

- The slide in (330 ms, ease-out²) and out (200 ms) run on every frame, step at 83 ms under `steppedMotion`, and are at
  once under reduce motion; no frame cut in «Эфир», «Копия», «Чистый сигнал», density 2 or the browser's way
  (`openclose-*.txt`: the title moves 1–19 dp a frame, monotonic). The screen behind stands while the drawer shows.
- The scrim darkens with the slide (`scrim` 196 → 102 in «Копия»; «Эфир»'s page is nearly as dark as the scrim).
- B-102: a pick by the mouse holds the drawer until the run lands and lingers, then it goes; a scrim click goes at once.
- D19: the rows are 180 × 42 and 46 dp from top to top, the whole row their target (`still-efir.txt`), 360 × 84 at
  density 2 (`still-d2.txt`).
- The focused chosen row draws its brackets on the lime, readable (`keys-rftrue.png`, `tab4`).
- «Чистый сигнал»: the window and the pill are clean already.

## Fixes

Each in its own commit, with a kept test in `DrawerMotionTest` (the sample's «Try it» stage: a screen of its own, the
opener with a focus requester, the drawer closing on a pick) or `DrawerWordsOnSlimeTest`. Each mutant was run after
committing against the aimed test, its failure read from the JUnit XML, and the file checked out again. After-frames:
`/private/tmp/b168-drawer/after/` (the same throwaway tests on the fixed code; `before/` was recorded again from
`662fa7a`'s drawer).

| Finding | Commit | Test | Mutant | Result |
|---|---|---|---|---|
| F-drawer-01 | `fix(drawer): turn a slide round from where the window stands` | `turned round mid-way it goes on from where it stands - no frame jumps`: the title frame by frame through a close a third into the open and a reopen a third into the close, every step under 40 dp | the follower without its durations | killed (steps of 101.6 and 190.4 dp) |
| F-drawer-02 | `fix(drawer): keep the window out of the screen's keyframe loss` | `opened while the screen's keyframe loss runs, its window is drawn whole`: a column of the window left of its words, under a loss held mid-run, all its fill | the window keeps the screen's scene | killed (144 rows not the window's fill) |
| F-drawer-03 | `fix(drawer): give focus back once the drawer is gone` | `through its exit the keys reach nothing behind it, and focus is back with the opener after` | `ReturnFocus(visible, …)` again | killed (the Enter during the exit opened it again: 2 opens) |
| F-drawer-04 | `fix(drawer): answer no row while the drawer closes` | `a closing drawer answers no row - through the hold after a pick and through the exit`, by the mouse | the rows enabled always | killed (a row clicked during the hold: picks 2, at 3) |
| F-drawer-05 | `fix(drawer): take the keys to the rows and keep the window no Tab stop` | `opened, Down takes the keys to the chosen row, and the window itself never holds them`: Enter on the opener, Down, a mouse click on the window's foot, the first Tab after a reopen | the window a `clickable` again; Up/Down on the drawer focusing nothing | killed (the chosen row not focused); killed (the chosen row not focused) |
| F-drawer-06 | `fix(drawer): turn each word black where the slime lies under it` | `every row's words read on the slime under them through a run across the drawer`: a bare twin on the same clock, every frame of a run 2 → 0, the tenth percentile of each row's glyph pixels against what lies under them | the words on the slime in the text's colour | killed (row 2 at 1.06:1 from frame 0) |
| F-drawer-07 | `fix(drawer): come in from the right edge right to left` | `right to left it comes in from the right edge and its rows stand 12 dp from it` | the slide always from the left; the rows placed absolutely | killed (the first frame's title at 86 dp); killed (the rows' right edge at 360, not 348) |

The KDoc now says that the slide runs on every frame (in the transition steps only under `steppedMotion`), that a slide
turned round goes on from where it stands, that no row answers once the drawer is dismissed, where Up and Down take the
keys, that focus goes back once the drawer has gone, and how the words follow the slime.

- **After.**
  - `after/core/reverse-efir.txt`: the close after Escape goes on from −104 dp (−124, −152, −182, gone); a reopen a third
    into the close is held by the kept test.
  - `after/core/loss-efir.png`: the window whole over a held loss, and through a pick.
  - `after/core/keys-rftrue.txt`: Down from the drawer lands on the chosen row; Tab steps the rows only; Enter during
    the exit reaches nothing, and the opener has the keys once the drawer is gone.
  - `after/core/clicks.txt`, `pickclose-efir.txt`: a click on the window's foot focuses nothing; a row clicked during
    the hold or the exit picks nothing.
  - `after/core/pick-*.txt`: no row-frame over a fifth under 3:1 in «Эфир», «Копия», `steppedMotion` or the browser's way,
    against 16–75 before.
  - `after/core/rtl.png`, `rtl.txt`: the window comes in from the right edge (title 477 → 275 dp), its body runs off the
    right edge, the rows stand 12 dp from it, the venom runs on them.
  - `after/core/openclose-*`, `after/sample/*`: the slide in and out unchanged in every look.
- **Density 2 in the words' harness.** The pair of drawers (the real one and its twin side by side) is 1 440 px wide at
  density 2, wider than the test's 1 024 px window, so the twin's pixels past the window are not drawn and the harness
  reads a wrong ground there: its density 2 numbers (35–64 row-frames, before and after alike) say nothing. A single
  drawer at density 2 mid-run, `after/core/d2probe-d2-{160,224,288}.png` beside `d2probe-d1-*`, shows the words black
  on the slime and pale off it, as at density 1.
- **Shared with other groups.** The drawer now takes the dialog's three fixes through its own calls (`DialogFollow` with
  durations, `ReturnFocus(!gone, …)`, no loss scene for the window). The bottom sheet calls the same two helpers and is
  likely to need the same.

## Not verified

- An index outside `items` (no row chosen): not looked at. The drawer follows `selectedIndex` into `VenomRun` as the menu
  did before F-menu-02, so a run to or from such an index very likely leaves the body as the menu's did; no sample or
  demo passes one.
- The phone and the browser themselves: the browser's way of drawing was looked at through the desktop's seams only, and
  the body shader on a phone's GPU (B-167) not at all.
- A real window: none was opened (the test window only), so the hand cursor over the rows and the system back gesture
  were not seen.

## Gate

`./gradlew :mutant-core:desktopTest :sample:desktopTest :mutant-core:ktlintCheck :sample:ktlintCheck --continue` on this
Mac, at the head of the fixes: green; from the JUnit XML, `mutant-core` 461 tests and `sample` 22, no failure, error or
skip. `:mutant-core:compileKotlinIosSimulatorArm64` green (the words' pass and holder are `DrawerWordPass` and
`DrawerWordInk`, so no private top-level name repeats the menu's or the segmented button's). The public API is unchanged.
