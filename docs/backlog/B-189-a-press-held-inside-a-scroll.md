---
id: B-189
title: "A press inside a scroll heats after 100 ms"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-189 — A press inside a scroll heats after 100 ms

A finger that starts a scroll on a body boils it: off Android, Compose's `clickable` presses on the finger's down (F-card-cocoon-01). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 11 ([решение]).

- **The decision and its reason.** A body inside a scrollable container starts its heat 100 ms after the down, on every platform. If the gesture becomes a scroll, the heat never starts; heat already started is removed by the ordinary 480 ms cooling. Outside a scroll there is no delay.
- Inside a scroll is read as Compose reads it for Android's press delay (`isInScrollableContainer`); the item checks that this works in common code.

- AC:
  - a finger held still on a card in a list heats it from 100 ms; one that moves past the slop within 100 ms never heats it
  - a body outside any scroll heats on the down, as now
  - on desktop, Android, iOS and wasm alike (desktop and wasm by test, the others by compile)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
