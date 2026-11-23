# B-168 — screens: the Demo page's three phones and the Components page

The last group of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-screens` from `662fa7a`,
2026-11-23, on this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand). **Report only**: nothing is
fixed here, neither the kit nor the sample; the sample's findings become one item after the pass. `main` does not yet hold
every group's fixes (groups 16–24 still merging), so what is reported is what `main` draws; a defect of a component that
another group already reported or fixed on its branch is named in a word, not reported again.

The sources: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/` (`DemoPage.kt`, `DemoApp.kt`,
`DemoLibrary.kt`, `DemoQueue.kt`, `DemoRecording.kt`, `DemoSearch.kt`, `DemoSettings.kt`, `ComponentsPage.kt`,
`MutantUiSample.kt`), the design's `reference/design-system/mutant-ui Демо.dc.html` (data) and its references
`mutant-core/src/desktopTest/snapshots/design/Demo_*.png`; the items that decided the screens:
[B-37](../../docs/backlog/B-37-demo-screens-and-sample.md), [B-48](../../docs/backlog/B-48-keyframe-loss.md),
[B-90](../../docs/backlog/B-90-a-dialog-stops-the-whole-demo.md), [B-96](../../docs/backlog/B-96-a-richer-demo.md),
[B-101](../../docs/backlog/B-101-demo-tape-twice.md), [B-148](../../docs/backlog/B-148-the-sample-for-a-consumer.md),
[B-149](../../docs/backlog/B-149-a-body-in-its-first-frame.md).

## What was looked at

One throwaway test in `sample/src/desktopTest` (deleted before committing), composing the page as the sample does:
`MutantUiSample(SampleSettings(page = Demo, demoScreen = …), ScrollState(…))`, the page scrolled so that the phone stands
whole in the 1024 × 768 window, `mainClock.autoAdvance = false`. Seams reached by reflection (`TestSeamsKt`,
`BodyShaderKt`), the pool emptied before and after each run. Frames, sheets (labelled with the clock), per-frame diffs
(`*.diff.txt`: time, pixels changed, pixels changed by more than 120 in the sum of channels, label) in
`/private/tmp/b168-screens/` (about 50 MB, not in the repository). Clicks are the mouse's (`performMouseInput`) where the
target stands in the window; the Demo page's screen chips stand above the window once the phone is in it, so they are
pressed through their semantics action. Overlay runs capture the window cropped to the phone and 140 dp to its right, so a
popup shows.

Looks: «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2 (window captures in parts), and the
browser's way (`LocalMutantFirstFrameWaits` false, `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true,
`LocalMutantBodyAsImage` false). The browser's way runs about 30 times slower here (1.2 s a frame), so it covered the
first frames and chip switches, the navigation walk and the keyframe loss, not every overlay.

- **First frames and screen switches** (`first-*`, `fastchips-*`: «Эфир», «Копия», browser): the page opened on Library,
  every frame for 160 ms; the chips to Recording, Settings, Library, every frame for 320 ms each; five switches 48 ms apart.
- **The navigation bar** (`nav-*`, `navfast-*`: «Эфир», «Копия», stepped, reduce, clean, browser): Library's phone, its bar
  clicked Queue, Search, Settings, On air (448 ms each, a frame every 32), then five clicks 64 ms apart.
- **Ten seconds of every frame** per screen (`ten-*`): «Эфир», «Копия» 10 s; clean 6 s; reduce and stepped 3 s; browser,
  Library 5.6 s (through its first keyframe loss).
- **Scrolling** (`scroll-Efir`, `lossscroll-*`): a touch fling up and back down on each of Library's four tabs; a fling
  during the keyframe loss.
- **The keyframe loss over a tab switch** (`losstab-*`): Settings taken at 3.0 s, On air again at 3.96 s, inside the loss.
- **Overlays** (`ovl-*`, `ovldrawer-*`, `ovlsearch-*`, `ovlsettings-*`: «Эфир», «Копия»): the FAB hovered (tooltip) and
  pressed (sheet, a segment picked, Record); the drawer opened and CH 02 picked; Search: the field focused, «n», «ni»,
  «zzz» (no result), cleared, the sort menu opened and «LONGEST» picked; Settings: the picture dropdown, «SHARP», the timer
  sheet (calendar and time) and Done. The Settings phone opens under its erase dialog.
- **Pull to refresh** (`ptr-*`, `ptr2-*`): Queue pulled 240 px by touch, released moving and released held.
- **Recording** (`rec-*`): Listen (the progress plays), Notes, Copies, Tracks, Back.
- **Focus** (`focus.txt`): a navigation item focused and Enter, Tab; a screen chip by Enter, then Tab into the phone; the
  drawer by Enter on the menu, then Escape.
- **Density 2** (`d2-*`): the Library phone in two halves, its Queue walk, Search and Settings tops; Recording and
  Settings in halves.
- **The Components page** (`comp-*`, `compscroll-*`: «Эфир»): first frames, every group chip in turn
  (192 ms every frame, then at rest), a touch fling and a wheel scroll of the page, Components → Demo → Components, and
  «Копия» from the header.

What held, besides the findings:

- **B-149 on the desktop.** Every screen and every tab draws its bodies whole in its first frame (`first-Efir-*`: frame 0,
  and the frame after each chip; `nav-Efir-*`).
- **No screen-wide flash or freeze.** Over 10 s of every frame no frame changes the whole phone: the largest steps are the
  boil ticks every 128 ms and the keyframe loss, at most 31 821 px of 259 200 (12 %) on Library, 26 467 on Recording
  (`ten-*.diff.txt`); the browser's way on Library over 5.6 s the same (median 1 378 px a frame, at most 32 439, as the
  desktop's 1 378 and 33 895 over the same span). The Settings phone under its dialog stands still (B-90; see
  F-screens-10). Under reduce motion every frame after the first is identical; under stepped motion the Settings dialog
  fades in three steps (254 k px each).
- **One Tape per phone** (B-101): nothing doubled is seen on any phone; `DemoTapeOnceTest` holds it (not run here).
- **Overlays stack in the right order** inside a phone: each tab draws the bar over its content and under its sheets and
  dialog; the drawer over all of it; the FAB under the new-recording sheet; the snackbar under the share sheet.
- **Fast switches** of the screen chips: each switch draws the new screen whole in the next frame (`fastchips-*`).
- **Reduce motion** stills everything but the data the sample advances (the queue's copy, the playback), as D7 says.

## Findings

### F-screens-01 — the keyframe loss drags a stale picture when the screen under it changes — reported (shared code)

- **What.** The loss (B-48) takes the screen's bodies when it starts and drags that picture in its band for 900–1 400 ms
  and a 375 ms recovery. When the screen under the band changes meanwhile — the tab switched, the content scrolled, search
  results pushing the archive down — the band keeps dragging the old picture, and the bodies of what now stands in the band
  give way to it (`MutantKeyframeLoss`'s rule: the band lies under the content, the bodies inside it give way). A body's
  dark word stays where its body was: on the band's ground.
  - **Tab switch** (`losstab-Efir-1.jpg`, `-2.jpg`; full frames `losstab-Efir-a-settings-in-loss.jpg`,
    `-b-onair-64ms.jpg`, `-c-onair-400ms.jpg`): Settings' loss running, On air taken at 3 960 ms. From 3 976 to about
    5 040 ms the band over On air's search field and chips shows blocks of Settings' fields (the red error outline, the
    switch's green); the chosen chip «Recordings» has no body for those 1.08 s and its black word stands on the band's dark
    ground, unreadable (`#05060c` on `#10142a`); the field's body is cut through. The same in the navigation walk
    (`nav-Efir-2.jpg`, 4 160–4 576 ms).
  - **Scroll** (`lossscroll-Efir-1.jpg`, `-2.jpg`; `lossscroll-Efir-b-160ms.jpg`, `-c-640ms.jpg`): a fling during the
    loss at 3 808 ms; once the content settles (4 300 ms) the band, now over «Play all» and «RECENTLY ON AIR», shows an
    acid smear where no body is, «Play all» has no body and its word reads black on the ground, until about 4 880 ms.
  - **Typing** (`ovlsearch-Efir-1.jpg`, 7 268–7 948 ms): the results come in under a running band; the band's old blocks
    stay over the first result row and the search bar's body.
  - **The browser's way** the same (`losstab-Browser-*.jpg`, `lossscroll-Browser-*.jpg`).
- **The design.** The engine's screens neither scroll nor switch tabs (the Demo page's three are static), so its
  `moshStart` snapshot never goes stale; B-48 did not decide what a loss does when the content under it moves. Close to
  F-list-divider-badge-02 (the snapshot takes the whole stage) but a different defect: that one is what the picture holds,
  this one is when it is taken.
- **Owner.** `surface/MutantKeyframeLoss.kt` (shared). The decision is the owner's: end the loss (or drop the band) when
  the content under it changes, or take the picture each scene tick, or keep the band to bodies that did not move.

### F-screens-02 — a fling at a tab's top carries on into the page: the phone slides away — reported (sample)

- **What.** A phone is a scroll inside the page's scroll (`MutantUiSample`'s `verticalScroll`). A downward fling that
  reaches the top of a tab hands its velocity on, and the page scrolls: the whole phone slides down the page, out of the
  window. On Library's first tab the page went from 289 to 3 px (`scroll-Efir.diff.txt`, lines 33–39: the phone's capture
  shrinks from 720 to 455 px tall as it leaves the window).
- **The pull to refresh** is the worst of it: a pull released while moving starts the refresh *and* scrolls the page 286 px
  (`ptr-Efir-1.jpg`, `ptr2-fling-*`, `ptr2.txt`: 289 → 3); the same pull held still before release leaves the page where it
  was (`ptr2-hold-*`: 289 → 289). On a phone's browser the Demo page jumps away under the finger that refreshed.
- **The design.** The design's phones do not scroll. A phone is a screen (B-101's reasoning): it should not scroll its
  page.
- **Owner.** Sample (`DemoPage.kt`, `ScreenBox`): a `nestedScroll` connection that consumes what the phone leaves.
  Question for the kit (`MutantPullToRefresh`, a word for its group): a release that sets `refreshing` passes its fling
  on to the parent; Material's consumes it while the indicator shows.

### F-screens-03 — Enter on a navigation item drops keyboard focus to nowhere — reported (sample)

- **What.** A navigation item focused by the keyboard and activated by Enter changes the tab, and then nothing on the
  page is focused: the next Tab and Enter go nowhere (`focus.txt`: «queue requested» focused, «after Enter on queue» none,
  «nothing focused: Tab goes nowhere»). A keyboard user starts again from the top of the page after every tab.
- **Cause.** `DemoApp.kt` moves the bar between the tabs whole (`movableContentWithReceiverOf`), each tab placing it in
  its own `Box`; the focused item is detached and attached again, and its focus is not kept. The bar alone keeps its focus
  on Enter (the nav-bar group's key walk).
- **Owner.** Sample: the bar drawn once over the tabs (it is already the same across them), or focus requested back on the
  chosen item after the move.

### F-screens-04 — the cassette's label is a fixed Russian word — owner decision (illustration/)

- **What.** `MutantIllustration(Cassette)` draws «БЕЗ ПОДПИСИ» (`illustration/MutantIllustration.kt:223`) whatever the
  language and the tape: the English Recording screen shows a Russian label under «Untitled No. 3», and every recording
  opened shows the same label, as the Library's empty state does (`first-Efir-1.jpg`, `rec-Efir-1.jpg`,
  `d2-Recording-top.jpg`).
- **The design.** The design's Recording screen is Russian and its tape is «Без подписи №3»: the label matches there.
- **Owner.** Owner decision: a label parameter (or none) for the illustration; until then the English sample shows Russian
  text.

### F-screens-05 — every recording reads «Side A · 46 MIN» — reported (sample)

- **What.** `DemoRecording.kt` takes the title from the tape and everything else from fixed strings: «Side A», «46 MIN ·
  SP · 4TH-GENERATION COPY», the same three tracks. Opening «Rain over the transmitter» (Side B, 31:12) or «Late-night
  repeat» (LP, 1:58:10) reads Side A, 46 min. Read from the code and from the screen opened on «Untitled No. 3»; the other
  tapes were not opened in the frames.
- **Owner.** Sample: `t.side`, `t.length`, `t.channel` in the side line.

### F-screens-06 — Recording's progress reads a percentage where the design reads the time — reported (sample)

- **What.** The design's Recording screen draws the playback bar with the elapsed time beside it in the OSD amber,
  «12:04» (`mutant-ui Демо.dc.html`: the bar and a separate Handjet 20 span); the sample's shows the kit's value, «026%»,
  and with Listen it counts percent (`rec-Efir-1.jpg`, `Demo_Recording_Efir.png` beside it).
- **Owner.** Sample: `MutantLinearProgress(played, showValue = false)` and its own time beside it.

### F-screens-07 — the design's play triangle has no icon in the kit: tracks show a house — owner decision

- **What.** The design's Recording draws a play triangle on «Слушать» and on each track row (three times, path
  `M8 5l11 7-11 7z`). The kit's 16 icons have none; the sample leaves Listen without an icon and gives the track rows
  `MutantIcons.Home` (⌂), which reads as «home», not «play» (`rec-Efir-1.jpg`, `d2-Recording-top.jpg`).
- **Owner.** Owner decision: a 17th icon in the set (the handoff names 16), or the sample's own `ImageVector`
  (`MutantButton` and `MutantListItem` take any).

### F-screens-08 — content swaps in one frame while what chose it moves for 330 ms — owner decision

- **What.** Nothing on a screen moves when its content changes: a tab's content is replaced in one frame while the
  navigation pill slides 330 ms to it (`nav-Efir-1.jpg`: Queue's content at 1 100 ms, the pill still on On air until
  1 164 and on Queue at 1 420); the Components page's group in one frame while the group chip pours (`comp-Efir-1.jpg`,
  956 → 972 ms); a screen chip on the Demo page the same (`first-Efir-*`). The tab's labels and the content tell the new
  tab from frame 1, the pill the old one for about 100 ms.
- **The design.** Silent: the design's demo does not navigate. B-96 asked that «motion is visible on entry»; the Recording
  tabs' cut is F-app-bar-tabs-07 (sample), the bar's colours in frame 1 are F-app-bar-tabs-04.
- **Owner.** Owner decision: a screen transition (the tune-in the brief's event table names for an appearing card would
  be the kit's own word for it) or the cut kept and written down.

### F-screens-09 — a screen chip on the Demo page throws away the phone's whole state — owner decision

- **What.** `DemoPage` composes the chosen screen under `key(screen)` and `DemoScreenFrame` makes its navigation with
  `remember(screen, …)`: switching to Recording and back to Library opens Library at On air, at the top, its chip
  «Recordings», its query empty, a refreshed Queue's new tape gone; the Settings phone opens its dialog again every time
  (`first-Efir-2.jpg`, 2 832 ms; `fastchips-*`).
- **Owner.** Owner decision (B-148 chose one screen at a time and said nothing of their state): a `SaveableStateHolder` per
  screen in `DemoPage`, or as now.

### F-screens-10 — the Tape stands still over the Settings phone while its dialog is open — owner decision

- **What.** The erase dialog holds its screen's clock (B-90), and the Tape reads that clock: on the Settings phone the
  Tape's lines, grain and noise rows stand for as long as the dialog is open. Median change per frame 0 px against 1 466 on
  the other two phones; what changes is the dialog's own button (at most 1 344 px; `ten-*-Settings.diff.txt`).
- **The design.** «Настройка — форма под диалогом: фон на паузе»: the form is paused; whether the Tape over the whole
  screen pauses with it is not said. A still Tape over a still phone reads as a frozen screen.
- **Owner.** Owner decision (`surface/MutantTape.kt` reads the screen clock).

### F-screens-11 — the browser's way: a screen chip draws the new phone in stand-ins for its first frame — reported (= F-buttons-03)

- **What.** Through the browser's seams each screen chosen by a chip draws all its bodies as flat stand-ins for one frame,
  then the slime: the whole phone's bodies flip together. 25 594 px change on the frame after Recording appears,
  25 916 after Settings, 53 332 on the page's first frames (`first-Browser.diff.txt`; the desktop's same frames: 0, 0, 0);
  `first-Browser-1.jpg` 976 → 992 ms. This is F-buttons-03's stand-in at screen scale, a phone-wide blink on every chip.
  A tab taken again comes back from the pool and draws its slime at once (`navfast-Browser.diff.txt`: 4 067 px on the
  frame after Settings, against 3 300 on the desktop); a tab's first visit was captured 32 ms apart and is not resolved.
  Reported, not again.

### Already reported, seen on these screens (a word each)

- F-card-cocoon-01: a finger starting a fling boils «Play all» and the cards it lands on (`scroll-Efir-1.jpg`, 1 168–1 232).
- F-chip-02: «Копия» from the header pours every chip of the Components page and the header from the dark palette, about
  100 ms of dark bodies on the light page (`compscroll-Efir-3.jpg`, 9 310–9 406).
- F-dialog-05: the Settings phone's dialog fades with its scrim, the form's words read through the window's for ~200 ms
  every time the screen is shown (`first-Efir-2.jpg`, 1 936–2 208).
- F-drawer-11 / F-dialog-08: Escape closes the drawer and nothing is focused after (`focus.txt`, «Escape»).
- F-drawer-12: the drawer's pick switches the tab under it and holds 880 ms (`ovldrawer-Efir-1.jpg`, 4 940–5 004).
- F-empty-state-02: at density 2 the cassette's reels are drawn half size (`d2-Recording-top.jpg`).
- F-empty-state-05 / F-search-05: Search's results replace the suggestions in a cut and push the archive down
  ~300 px in one frame (`ovlsearch-Efir-1.jpg`, 7 588 → 7 620).
- F-menu-09 and F-tooltip-02 faces: the sort menu hangs past the phone's right edge, and the FAB's tooltip plate stands
  ~10 dp past it, over the page (`ovl-Efir-1.jpg`, `ovlsearch-Efir-3.jpg`): a popup is the window's, not the phone's.
- F-menu-10: the Tape does not cover the sort menu, the dropdown or the tooltip.
- F-nav-bar-02 / F-nav-bar-03: fast bar clicks jump the pill (`navfast-Efir-1.jpg`; fixed on the nav-bar branch).
- F-card-cocoon-04 face: the Settings screen's three radios share the key `radio` (`DemoSettings.kt`) and boil as
  clones; Recording's dividers `0`, `1`, `2` and Library's cocoon `0` the same kind of default.

## Not verified

- The browser's way beyond first frames, the navigation walk, the loss and 5.6 s of Library: Recording's and Settings'
  ten seconds, overlays, scrolling, pull to refresh, density 2 and the Components page were not run through the seams
  (30 times slower here).
- The Components page under reduce motion, clean, stepped and density 2; its sections are the other groups' and were not
  looked at one by one.
- A real phone and a real browser (never in this pass), the GPU shader (B-167), right to left, Russian (looked at in
  `DemoScreensTest`'s sheets only).
- The dropdown and the timer sheet were opened through their semantics action without scrolling the Settings tab first:
  the dropdown's anchor stood under the navigation bar, so where its menu stands over the bar is the test's doing, not a
  finding.
- F-screens-05 was read from the code; the other tapes' recordings were not captured.
