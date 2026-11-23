---
id: B-168
title: "A look-and-motion pass, one agent per component group, on the desktop JVM"
status: done
priority: P1
size: L
stage: stage-6-later
blocked_by: []
---

# B-168 — A look-and-motion pass, one agent per component group, on the desktop JVM

[B-40](B-40-qa-pass-and-real-window.md)'s pass broke what a consumer does to the kit (widths, long text, focus, the
mouse). The owner's rounds after it (B-90…B-105, B-150, B-166, B-167) found what it did not look at: how each component
looks and moves as the sample composes it, and in the browser's way of drawing. Nearly every one of those was visible
in a frame strip.

- **The decision and its reason.** The owner's (2026-11-22): a full QA session of the design system, as field-ui ran
  its B-89 — every component group to its own agent, which composes it as the sample does on the desktop JVM,
  photographs it frame by frame, checks it against the design, and fixes it until it works. Each agent works in its own
  worktree, two at a time on this 16 GB Mac, in the test runtime (`runComposeUiTest`, `captureToImage`), never a window
  on the owner's screen and never the phone. It first lists its findings with frames, then fixes only what is local to
  its components; a cause in shared code or a look the design does not draw is reported, and triaged here.
- Rejected: one agent over the whole kit (B-40's shape) — it reads the axes, not the frames, of 30-odd components.
  Rejected: an agent that fixes as it finds — it confirms its own fix, and two agents fixing the surface or the
  generator collide.

- AC: every row of the inventory below looked at, its report in `logs/b-168/<slug>.md`; every local finding fixed with a
  kept test, a killed mutant and before/after frames, merged on green; every reported finding an item or a line here;
  the looks the design does not settle in one list for the owner.

## The brief every agent gets

One component group, a worktree and a branch `fix/b-168-<slug>` from `main`. Never push. Never the owner's screen (no
`:sample:run`, no real `Window`) and never the phone.

1. **Read first**: `CLAUDE.md`, `docs/research/research-architecture.md` (§1 and the decisions, D15 and D20–D23 in
   full), this item, and the component's own items (`grep -l <Name> docs/backlog/*.md`: they hold the numbers the design
   does not state — those are decided, not to be re-decided). Then the design for the components: their slot on the
   `reference/design-system/mutant-ui Компоненты*.dc.html` page, their row in `reference/design-system/README.md`
   «Компоненты», their type profile in `mutant-tokens.json`, and how `mutant-engine.js` draws and moves them
   (`drawItem`). The design is the oracle; where it is silent, D15 says the kit decides and an item records the number.
2. **Look**, before changing anything. A throwaway test (deleted before committing) composes the components **as the
   sample composes them** (its `Components*.kt` section inside `MutantUiSample(SampleSettings(group = …))`, or the demo
   screen that uses them), with `mainClock.autoAdvance = false`, and for every state and transition — appear, press,
   release, select, open, close, drag, scroll, disable, both palettes («Эфир», «Копия»), reduce motion, «Чистый сигнал»,
   `steppedMotion` — writes **contact sheets** of frames 16–48 ms apart and a **per-frame pixel diff** over two seconds
   to `/private/tmp/b168-<slug>/`. Look at every sheet. Also at density 2, since tests at 1 hide unit errors. And
   **in the browser's way of drawing**, through the seams in `surface/TestSeams.kt` and their neighbours
   (`LocalMutantFirstFrameWaits provides false`, `LocalMutantPoolBodies provides true`, `LocalMutantBodyOnGpu provides
   true`, `LocalMutantBodyAsImage`), as `FillChangeOnGpuTest` does: B-166 lived only there.
3. **The failure classes** the owner has found in this kit: a body or word holding a stale look for a frame or flicking
   back (the chip's old fill from the pool, B-166); words unreadable on a pressed or chosen fill in some frame (B-91,
   B-104), or following the pick late (B-103); an overlay that does not close (B-92) or stops what it should not (B-90);
   a selection's slime jumping instead of moving (B-93), a tongue that should become the pill (B-105); a label off its
   pill (B-94); a gesture that is not smooth (B-95); a body torn or wedged as it rises (B-97); a crash under an ordinary
   overlay (B-98); venom shown where it should not be (B-102); a layout that changes width on a state change (B-150).
   And anything else a user would call broken: an animation missing on enter or exit, geometry growing from the wrong
   side, a frame of the wrong state, drips or flow lines entering the clean zone (D5), a seed or animation moving a
   glyph, an icon, the slot or the touch target, the red flashing.
4. **Write the report** `logs/b-168/<slug>.md` before fixing: per finding `F-<slug>-NN`, what is wrong, the frames that
   show it (paths), the design's word or the item that decided it, and **local** (in the group's own files under
   `components/`) or **reported** (in `surface/`, `gen/`, `clock/`, `theme/`, `tokens/`, `type/`, `icons/`,
   `illustration/`, `pattern/`, `MutantText.kt`, `MutantHover.kt`, `OwnWidth.kt`, `VenomRun.kt`, the sample, another
   group's component, or a look the design does not draw). The body shader (`BodyShader.kt`) runs on a phone's GPU,
   which no JVM test sees (B-167): anything about it is reported. Commit the report.
5. **Fix the local ones**, one commit each where they are independent: the cause, a kept test that reads what is drawn
   or the real path (`performTouchInput`, `performMouseInput`, semantics, bounds, pixels), a mutant killed after
   committing (change the line, run the aimed test, read the `<failure>` in the JUnit XML, `git checkout -- <file>`),
   after-frames looked at. Never edit a golden, a reference or the vendored design to pass; a golden or parity cell that
   moves because the look was wrong is re-recorded only with before/after frames and the reason in the report, and a
   departure from the design is listed as CLAUDE.md «The code is primary» says. Commits: English Conventional Commits,
   subject ≤ 72 characters, footer `Refs: B-168`, no tool signature.
6. **Gate the worktree** before answering: `./gradlew :mutant-core:desktopTest :sample:desktopTest
   :mutant-core:ktlintCheck :sample:ktlintCheck --continue` (on this Mac; the repository is not synced), and
   `:mutant-core:updateKotlinAbi` only if the public API changed (then say so). Failures are read from the JUnit XML.
   Run Gradle in the foreground with a long timeout and wait for it.
7. **Answer** with the report's path, the findings by number with local or reported, the commits, the mutants and
   whether killed, and what could not be verified. Scratch tests deleted; `git status` clean.

## Inventory

| # | Group (slug) | Components | Status |
|---|---|---|---|
| 1 | buttons | Button, TextButton, OutlinedButton | done: [report](../../logs/b-168/buttons.md), 1 fixed, 6 reported |
| 2 | icon-fab | IconButton, Fab, ExtendedFab | done: [report](../../logs/b-168/icon-fab.md), 1 fixed, 5 reported |
| 3 | chip | Chip | done: [report](../../logs/b-168/chip.md), 1 fixed (KDoc), 2 reported |
| 4 | checks-switch | Checkbox, RadioButton, Switch | done: [report](../../logs/b-168/checks-switch.md), 1 fixed, 4 reported |
| 5 | text-field | TextField | done: [report](../../logs/b-168/text-field.md), 3 fixed, 6 reported |
| 6 | search | SearchBar, its suggestions and results | done: [report](../../logs/b-168/search.md), 3 fixed (one a KDoc), 3 reported, 1 withdrawn to text-field |
| 7 | card-cocoon | Card, Cocoon (skeleton) | done: [report](../../logs/b-168/card-cocoon.md), 0 fixed, 5 reported |
| 8 | app-bar-tabs | TopAppBar, Tabs | done: [report](../../logs/b-168/app-bar-tabs.md), 2 fixed, 5 reported |
| 9 | nav-bar | NavigationBar | done: [report](../../logs/b-168/nav-bar.md), 2 fixed, 2 reported |
| 10 | list-divider-badge | ListItem, Divider, Badge | done: [report](../../logs/b-168/list-divider-badge.md), 1 fixed, 3 reported |
| 11 | snackbar | Snackbar | done: [report](../../logs/b-168/snackbar.md), 1 fixed, 6 reported |
| 12 | progress | LinearProgress, CircularProgress | done: [report](../../logs/b-168/progress.md), 4 fixed, 3 reported |
| 13 | dialog | Dialog | done: [report](../../logs/b-168/dialog.md), 4 fixed and a KDoc, 5 reported |
| 14 | empty-state | EmptyState and its illustrations | done: [report](../../logs/b-168/empty-state.md), 1 fixed, 4 reported |
| 15 | slider | Slider | done: [report](../../logs/b-168/slider.md), 4 fixed, 2 reported |
| 16 | segmented | SegmentedButton | done: [report](../../logs/b-168/segmented.md), 4 fixed, 2 reported |
| 17 | tooltip | TooltipBox | done: [report](../../logs/b-168/tooltip.md), 1 fixed, 4 reported |
| 18 | menu | Menu, Dropdown | done: [report](../../logs/b-168/menu.md), 4 fixed and a KDoc, 5 reported |
| 19 | drawer | NavigationDrawer | done: [report](../../logs/b-168/drawer.md), 7 fixed, 5 reported |
| 20 | bottom-sheet | BottomSheet | done: [report](../../logs/b-168/bottom-sheet.md), 6 fixed, 3 reported |
| 21 | pull-to-refresh | PullToRefresh | done: [report](../../logs/b-168/pull-to-refresh.md), 2 fixed, 2 reported |
| 22 | pickers | DatePicker, TimePicker | done: [report](../../logs/b-168/pickers.md), 4 fixed, 7 reported |
| 23 | data-table | DataTable | done: [report](../../logs/b-168/data-table.md), 2 fixed (one adds `rowKey`), 4 reported |
| 24 | carousel | Carousel | done: [report](../../logs/b-168/carousel.md), 4 fixed, 3 reported |
| 25 | foundations | the Foundations section: surface states, the Tape, keyframe loss (report-only: shared code) | done: [report](../../logs/b-168/foundations.md), report only: 8 findings, 13 shared-code classes |
| 26 | screens | the demo's three screens, as composed | done: [report](../../logs/b-168/screens.md), report only: 11 findings |

## Result (2026-11-23)

All 26 groups were looked at frame by frame. Every group's report is in `logs/b-168/<slug>.md`, with its findings, before- and after-frames, fixes, mutants and gate. The groups were merged in one PR each, then in batches of four to six, through gated integrations (#106–#117). Each batch ran `make check` and `./gradlew check` on the merged tree on the Mac before its PR, and was merged on a green verdict over the head commit.

- **Local fixes.** 66 findings were fixed in the components' own files. Each has a kept test and a mutant killed after committing; the few mutants that survived are explained in their reports (equivalent, or timing a jump test cannot see).
  - No golden or parity cell moved.
  - Public API: `MutantDataTable(rowKey = …)`.
  - The ABI dumps took the carousel's composable singletons.
- **Fixed in shared code, one item each.** These are the findings the agents reported out of their files:
  - [B-169](B-169-text-redraws-on-a-colour-change.md): a word redrawn when its colour alone changes.
  - [B-170](B-170-illustrations-drawn-in-dp.md): the illustrations' reels and rods were drawn in pixels.
  - [B-171](B-171-transitions-reversed-from-where-they-stand.md): a pour, a slide or a melt reversed mid-way now goes on from where it stands.
  - [B-172](B-172-the-venom-picked-mid-run.md): the venom picked mid-run.
  - [B-173](B-173-tests-off-the-wall-clock.md): two CI flakes taken off the wall clock.
  - [B-174](B-174-the-sample-after-the-look-pass.md): the sample's own defects.
  - [B-175](B-175-a-palette-switch-pours-nothing.md): a palette switch pours nothing.
  - [B-176](B-176-the-pause-lifted-replays-nothing.md): lifting the pause replays nothing.
  - [B-177](B-177-the-stand-in-keeps-the-state.md): the browser's stand-in keeps the body's state.
  - [B-178](B-178-the-calendar-short-of-height.md): the calendar in a slot shorter than it.
- **Found by integrating, not by an agent.**
  - The segmented button and the menu each declared `private class WordInk` in one package. The JVM accepts this; Kotlin/Native reports it as a redeclaration. It is fixed by a prefix, and later briefs asked for prefixed names and one iOS compile.
  - A picker test sat 1 point under its limit and failed on CI. It was rewritten to hold what the fix guarantees (B-168 batch C).
  - Agents' desktop-only gates missed `checkKotlinAbi`.
- **Left for later items, not decided here.**
  - Under reduce motion, hover redraws the body at the hot tick (F-checks-switch-05).
  - On the desktop a press draws its rest picture for two frames (F-icon-fab-06).
  - A press released into a toggle drops its heat for the pour's first steps (F-checks-switch-03).
  - The browser keeps the old body for one frame on a new signature (F-text-field-06; pre-building the variants costs about 10 ms of CPU per card on the M1 and needs a phone measurement, B-177).
  - The pour's front is not given to content that wants to split its words along it (F-pickers-11).
  - A body moved by `Modifier.offset` further than its box plus 64 dp counts as off its box and replays its last picture (seen in B-176).
  - Components with their own transitions (dialog, sheet, drawer, snackbar, tabs, menu, progress, pull to refresh) were not checked for B-176's replay.
- **The owner's queue.** The looks, numbers and decisions the design does not settle are listed below, numbered for the designer.
- **Not verified.**
  - The phone. It was not used in this pass, at the owner's word.
  - The real browser: wasm was compiled and its tests run, and the browser's way of drawing was checked only through the JVM seams.
  - The body shader on a GPU (B-167).
  - Real touch, trackpads, screen readers and a real window.
  - B-177's stand-in now draws more in the browser's first frame (no instance, draw calls only), and B-172's venom settles before it runs on (a pick lands up to 360 ms later). Neither was measured on the phone.

## The owner's queue

Each is a decision for the owner, most of them a question for the designer; none is fixed until answered. Grouped by theme; the finding ids lead to the reports in logs/b-168/.

### Motion the design does not draw
1. **Appearing and leaving.** Cards, empty states, a badge, the snackbar's words, a tooltip, a cocoon handing over to its content, a search result list: all appear and leave in one frame. The brief's event table has `appear → tune-in` (320 ms, stepped(3)) for a card; the animation-spec page says the engine has none. Build `tune-in` for every appearing body, or keep the cut? (F-card-cocoon-03, F-search-05, F-empty-state-05, F-snackbar-06, F-pull-to-refresh-04)
2. **Screen content swaps in one frame** while the navigation pill, the tabs' bar or a chip moves for 330 ms. Is there a screen transition? (F-screens-08, F-app-bar-tabs-04, F-app-bar-tabs-07)
3. **Enabled ↔ disabled, focus in ↔ out** are a one-frame cut on every body (drips shorten or lengthen at once); the engine is static too. A cross-fade, or keep the cut? (F-icon-fab-04, F-text-field-08, F-search-06)
4. **What the slider moves by itself** — a tap on the channel (up to 234 dp), an arrow step, the settle at release, and a value set by the app while held — is a cut. A duration and curve? (F-slider-05, F-slider-06)
5. **The drawer and the bottom sheet cannot be dragged** (touch, fling, mouse), though D15 names their dragging. Build it, with which numbers? (F-drawer-09, F-bottom-sheet-07)
6. **The list's "scroll / shear"** of the brief (a lag of at most 4 dp by speed, rest ticks standing during a scroll) is not built; the engine lacks it. (F-list-divider-badge-04)
7. **The dialog's window fades with its scrim as one alpha** (B-28), so for 200 ms the screen's words read through the window's text. Rise the window opaque? (F-dialog-05)
8. **The caret's blink** runs on the screen's tick and does not restart on focus or typing (dark up to 375 ms after a click). Restart it? (F-text-field-04)
9. **The tabs' labels** turn in the click's first frame while the bar takes 330 ms; the echo changes side in that frame. (F-app-bar-tabs-04)
10. **Tab to the next carousel card** moves the shelf by Compose's bring-into-view (234 dp in 4–5 frames), not the press's 330 ms glide. (F-carousel-07)
11. **A finger that starts a scroll on a body boils it** (off Android, Compose's `clickable` presses on the finger's down): a hold before the press shows (Android's is 100 ms)? (F-card-cocoon-01)

### Reduce motion
12. **A press shows nothing under the pause** (buttons, FAB, toggles, the refresh tongue's boil). The engine's paused mode does the same. Should a press show at once? (F-buttons-06, F-icon-fab-05, F-pull-to-refresh-03)
13. **The indeterminate band stands at tick 40** under the pause, which leaves the track empty at 29 of 177 widths. Another standing picture? (F-progress-05)
14. **`steppedMotion` does not step the band and the reel** (12 dp every 125 ms either way); B-100 does not name them. (F-progress-07)

### Look
15. **The text button and the snackbar's action have no pressed look** (the design gives no `data-press`). (F-buttons-05, F-snackbar-04)
16. **A hovered card's subtitle reads 3.22:1** (6.9:1 at rest): over the kit's 3:1, under WCAG's 4.5:1 for 13 sp. (F-card-cocoon-05)
17. **The caret's 2 dp bar** (B-58) hides the right stem of a letter ("Nigh|t" reads "Nig|t"): 1 dp, or centred on the insertion point? (F-text-field-05)
18. **The mosaic moves** when the first letter is typed or the last deleted (the clean zone switches from the placeholder's box to the line). (F-text-field-07)
19. **The cassette's label is «БЕЗ ПОДПИСИ»** in every language and on every tape. A parameter? (F-screens-04)
20. **No play triangle** among the kit's 16 icons; the design draws one three times. (F-screens-07)
21. **The pickers have no today mark, no disabled days, no typed time digits**; the design draws none. (F-pickers-08)
22. **The Tape does not cover popups** (an open menu, a tooltip): popups float above it. (F-menu-10)
23. **The Tape stands still over the Settings phone** while its dialog is open (the dialog's pause, B-90). (F-screens-10)
24. **The keyframe loss** keeps dragging the picture it took when it started after the screen under it changed (a tab switch: a chosen chip without its body for 1.08 s, its word on the dark band; a scroll), and leaves the dividers out of its picture (the engine's takes the whole stage). Re-take the picture on a change, end the loss, or keep it? (F-screens-01, F-foundations-05, F-list-divider-badge-02)
25. **While pressed, the edge mosaic shifts tiles into the clean zone** (two of seven keys, every tick, 176–186 px), against D5; the engine does the same. Clip it to the zone? (F-foundations-04)

### Layout at the edges
26. **The tooltip's plate is cut at the window's edges**, and wholly above the window for an anchor in its top 58 dp (a top bar's action). Shift it along the edge, or flip it below (with drips hanging away)? (F-tooltip-02)
27. **The tooltip never wraps** (`nowrap`): a long hint runs off the window. A maximum width? (F-tooltip-03)
28. **The menu near the window's bottom** slides up over its anchor (Compose's popup clamping); a menu taller than the window loses its first rows and does not scroll. Open upwards? Scroll? (F-menu-06, F-menu-07)
29. **A dialog taller than the screen** stands flush with no vertical margin, its outline clipped and its last line cut with no sign it scrolls (B-59). A margin? (F-dialog-07)
30. **Whether the bottom sheet scrolls its own content**, with a gap at the top; if it does, consumers cannot put a lazy list in it. (F-bottom-sheet-08)
31. **A top bar narrower than about 170 dp** gives its title zero width and pushes the action out. What gives way? (F-app-bar-tabs-06)
32. **The calendar's height changes with the month** (4–6 rows), and its body regenerates. Always six rows? (F-pickers-06)
33. **The carousel under 304 dp** cuts its current card (B-47's 84 dp); **many dots** run past a narrow shelf. (F-carousel-05, F-carousel-06)
34. **The data table's fixed columns** keep their dp at font scale 2 and the grid has no gap ("таблица02:10"); a table narrower than its fixed columns drops every name with no ellipsis. (F-data-table-04, F-data-table-05)
35. **A button with no width grows 32 dp on Success** (the check), its body regenerated. Reserve the check's room? (F-buttons-04)
36. **The navigation badge grows left** from its right edge: "12" covers half the icon; in 30 dp cells it reaches the next. (F-nav-bar-04)

### Right to left
37. **The segmented button** does not mirror and its arrows are not mapped; **the tabs' echo** stays right of the bar; **the date picker's month arrows** keep their left-to-right look. B-65 mirrored the tabs and the navigation bar only. Mirror every component? And a left-to-right title in a right-to-left layout is cut at its end with the ellipsis drawn at its start (the platform's). (F-segmented-06, F-app-bar-tabs-05, F-pickers-10, F-data-table-06)

### API
38. **Default keys** are a constant or the label: unkeyed cards, rows, dividers, radios and dialogs are clones that boil in step (the dialog's window key cannot be set at all); a play/pause icon button or a running `REC · 0:42:17` tooltip reshapes its body on every change. Default from the composition's place (`currentCompositeKeyHash`), or say it in the KDoc? (F-card-cocoon-04, F-icon-fab-03, F-tooltip-04, F-dialog-10)
39. **A press on the menu's chosen row** passes nothing to `onSelect` (as the engine), so the demo's sort menu cannot be closed by picking the current order. (F-menu-08)
40. **Focus after the snackbar's action** lands nowhere — a `returnFocusTo`? (F-snackbar-05) **The drawer's Tab** stops at its last row, unlike the dialog's. (F-drawer-10) **The time picker's acid** stays on the hours while Up/Down set the focused minutes. (F-pickers-07)
