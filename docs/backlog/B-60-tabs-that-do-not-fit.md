---
id: B-60
title: "Tabs: tabs past the row's width are invisible yet focusable"
status: wip
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
