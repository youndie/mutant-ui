---
id: B-207
title: "Focus returns after the snackbar and cycles in the drawer"
status: wip
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-207 — Focus returns after the snackbar and cycles in the drawer

After the snackbar's action focus lands nowhere (F-snackbar-05); the drawer's Tab stops at its last row, unlike the dialog's (F-drawer-10). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 40 ([ошибка]; the clock's accent is B-196).

- **The decision and its reason.** After the snackbar's action focus returns to where it was before. In an open drawer Tab cycles through its rows, and Escape closes it.

- AC:
  - focus a button, open a snackbar, take its action: the button is focused again
  - Tab from the drawer's last row reaches its first; Shift+Tab from the first reaches the last; Escape closes the drawer
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`
