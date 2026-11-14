---
id: B-65
title: "Tabs and the navigation bar mirror in a right-to-left layout"
status: open
priority: P3
size: S
stage: stage-5-product
blocked_by: [B-40]
---

# B-65 — Tabs and the navigation bar mirror in a right-to-left layout

Found in [B-40](B-40-qa-pass-and-real-window.md) (F15). Both place their items with `place`, not `placeRelative`, so under `LayoutDirection.Rtl` the first tab and the first destination stay on the left. The list item, snackbar and top app bar mirror.

- **The decision.** Both mirror; the bar and the pill follow the placed cells. The rejected alternative is documenting LTR only.
- AC: `OverlayAndMouseTest.F15` passes without its `@Ignore`.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTabs.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationBar.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/OverlayAndMouseTest.kt`
