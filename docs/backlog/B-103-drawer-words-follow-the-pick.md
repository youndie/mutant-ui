---
id: B-103
title: "The drawer's words stay on the row picked before"
status: done
priority: P1
size: S
stage: stage-6-later
---

# B-103 — The drawer's words stay on the row picked before

The owner, 2026-11-17: «в ките с выбором nav drawer item проблемы — выделяется не тот, что кликнул».

- **Seen before.** A frame strip of a drawer that stays open, every 48 ms from a pick of `On air` with `Search`
  chosen: the venom runs and the window lands on `On air`, but from the first frame to the last the words of
  `Search` stay black, as a chosen row's, on the drawer's dark ground, and the words of `On air` stay light on the
  lime window. The row left behind reads as the chosen one, and the chosen one cannot be read.
- **Cause.** `DrawerRow` passed its words' colour as `color = ::ink`, a reference to a local function. Kotlin's
  function references compare equal by the function they name, not by what its closure captured, so the text's node
  saw the new producer equal to the old one and kept the first: it coloured the rows by the selection the drawer was
  first composed with. The menu and the segmented button pass a lambda and were never affected; no other component
  passes a reference.
- **Fix.** A lambda, which Compose makes anew when the selection it captures changes.
- AC:
  - a second after a pick, the new row's words are black on the window and the old row's are in the text's colour
    (`DrawerPickWordsTest`, read from the pixels);
  - the test fails with the reference put back.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`
