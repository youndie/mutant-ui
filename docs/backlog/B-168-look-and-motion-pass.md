---
id: B-168
title: "A look-and-motion pass, one agent per component group, on the desktop JVM"
status: wip
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
| 3 | chip | Chip | open |
| 4 | checks-switch | Checkbox, RadioButton, Switch | open |
| 5 | text-field | TextField | open |
| 6 | search | SearchBar, its suggestions and results | open |
| 7 | card-cocoon | Card, Cocoon (skeleton) | open |
| 8 | app-bar-tabs | TopAppBar, Tabs | open |
| 9 | nav-bar | NavigationBar | open |
| 10 | list-divider-badge | ListItem, Divider, Badge | open |
| 11 | snackbar | Snackbar | open |
| 12 | progress | LinearProgress, CircularProgress | open |
| 13 | dialog | Dialog | open |
| 14 | empty-state | EmptyState and its illustrations | open |
| 15 | slider | Slider | open |
| 16 | segmented | SegmentedButton | open |
| 17 | tooltip | TooltipBox | open |
| 18 | menu | Menu, Dropdown | open |
| 19 | drawer | NavigationDrawer | open |
| 20 | bottom-sheet | BottomSheet | open |
| 21 | pull-to-refresh | PullToRefresh | open |
| 22 | pickers | DatePicker, TimePicker | open |
| 23 | data-table | DataTable | open |
| 24 | carousel | Carousel | open |
| 25 | foundations | the Foundations section: surface states, the Tape, keyframe loss (report-only: shared code) | open |
| 26 | screens | the demo's three screens, as composed | open |
