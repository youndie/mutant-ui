---
id: B-65
title: "Tabs and the navigation bar mirror in a right-to-left layout"
status: done
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

## Findings (2026-11-15)

**Fixed.** The tabs and the navigation bar now place their items with `placeRelative`: in a right-to-left layout the first tab and the first destination stand at the right. Three things had to follow the mirrored places:
- the tab bar is drawn from where each tab now stands (`marks.x` reads the mirrored x);
- the navigation pill is placed relative too;
- its slide from the destination it left starts from the mirrored side.

**Tests.**
- `OverlayAndMouseTest.F15` (the order) passes without its `@Ignore`.
- A new F15 test reads the tab bar's and the pill's acid pixels in a right-to-left layout with the first item chosen: all of them in the right half, none in the left.
- The tabs' and the navigation bar's parity and behaviour tests are unchanged.

**Not held by a test.** The direction of the pill's slide under right-to-left. The tests run under reduce motion, where the pill stands at once; the sign is one line beside the placement it follows.

**Mutants.** All four killed by the F15 tests:
- the tab bar reading the unmirrored x;
- tabs placed with `place`;
- destinations placed with `place`;
- the pill placed with `place`.
