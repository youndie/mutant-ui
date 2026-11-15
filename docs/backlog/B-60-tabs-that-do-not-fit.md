---
id: B-60
title: "Tabs: tabs past the row's width are invisible yet focusable"
status: done
priority: P2
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-60 — Tabs: tabs past the row's width are invisible yet focusable

Found in [B-40](B-40-qa-pass-and-real-window.md) (F08). `MutantTabs` measures each tab with what is left; once the row is full the rest are 0 × 0 and still focusable and selectable. With 12 tabs in 360 dp, Tab presses 5–12 land on invisible tabs, and a selected one shows no bar.

- **The decision.** When the tabs need more than the row's width, the row scrolls horizontally and scrolls the focused or selected tab into view. The rejected alternative is squeezing every tab equally: at twelve the labels are unreadable.
- AC: `LongContentSweepTest.F08` passes without its `@Ignore`; the tabs' parity cells unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/LongContentSweepTest.kt`

## Findings (2026-11-15)

- **Fixed.** The tabs' row scrolls horizontally, each tab measured at its own width. The scroll itself brings a focused tab into view; a newly selected one out of view is scrolled to at once, without an animation. With no width at all (inside a horizontal scroll) the row takes the tabs' own width, which a scroll needs as its limit; the first try threw there, caught by the QA sweep.
- **Parity.** A row that fits lays out as before: `BarTabsParityTest` unchanged.
- **Tests.** `LongContentSweepTest.F08` runs, its `@Ignore` gone. It now also holds that the selected last of twelve tabs is in view before anything is focused.
- **Mutant.** A selection not scrolled to: killed by F08.
