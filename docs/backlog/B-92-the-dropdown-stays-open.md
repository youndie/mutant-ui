---
id: B-92
title: "The dropdown closes itself after a pick"
status: open
priority: P2
size: S
stage: stage-6-later
---

# B-92 — The dropdown closes itself after a pick

The owner, 2026-11-16: «dropdown пропадает в демо когда выбираем элемент (его можно вообще не скрывать)».

`MutantDropdown`'s menu closes 720 ms after a pick, once the venom reaches the new row (`LaunchedEffect(picked)` in `MutantMenu.kt`). Its anchor's label is fixed, so after the close the choice shows nowhere.

- **The decision and its reason.** As the owner said: a pick does not close the menu. It closes on a click outside, on Escape, or on the anchor. Rejected: keeping the close and showing the pick in the anchor alone, which is not what was asked. The venom's run to the new row stays visible. It changes the design's controller, so it is a departure (D20).
- AC:
  - After a pick the menu stays open, its venom on the new row, and the change is reported.
  - A click outside, Escape and the anchor close it.
  - Tests for each.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantMenu.kt`
