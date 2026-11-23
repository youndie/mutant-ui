---
id: B-174
title: "The sample's own defects that the look-and-motion pass reported"
status: wip
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
| F-dialog-08, F-drawer-11 | No `returnFocusTo` in five overlays; and on the page the «open» specimens take the focus as they appear and keep it, so «Try it»'s dialog and drawer never get it (Escape reaches the specimen, whose dismissal does nothing) | fix: openers named; the still specimens take no focus |
| F-drawer-12 | The tab changes at the pick, under the drawer's 880 ms hold | left: when to change it is the owner's, and the kit gives no `onHidden` |
| F-menu-09 | The sort menu stands at the button's start and its 216 dp run past the 360 dp phone (the row's words from 311 dp, the report's 123 dp over) | fix: stood at the button's end |
| F-tooltip-03 | The stepped-motion hint is one 1 405 dp line; the mosaic hint, beside it, another of 700 dp | fix: both hints shortened to a line no wider than their titles |
| F-pickers-09 | The 13th chosen in November is chosen in every month | fix: the chosen day is kept with its month |
| F-data-table-02 | No `rowKey` | fix: `rowKey = { it.title }` |
| F-foundations-06 | The Theme specimens' `MutantTheme(colors)` resets the header's switches: 10 455 pixels change in 500 ms under reduce motion | fix: the specimens take the header's four switches |
| F-foundations-07 | The page's «Эфир» Tape over the «Копия» specimen and over the Tape specimen's own | fix: the page's Tape leaves those specimens out; each palette specimen carries its own palette's Tape |
| F-screens-02 | A fling at a tab's top scrolls the page: 289 → 150 px at the lift, → 0 after | fix: the phone keeps the fling it leaves |
| F-screens-03 | Enter on a navigation item leaves nothing focused | fix: the focus is put back on the chosen item |
| F-screens-05 | Every recording reads «Side A · 46 MIN» | fix: the side and the line from the record; the tracks left |
| F-screens-06 | The progress reads «026%» where the design reads «12:04» | fix: the elapsed time beside the bar |
