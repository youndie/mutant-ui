---
id: B-199
title: "No mosaic block in the clean zone, pressed or at rest"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-199 — No mosaic block in the clean zone, pressed or at rest

While pressed, the edge mosaic shifts tiles into the clean zone — two of seven keys, every tick, 176–186 px — against D5; the engine does the same (F-foundations-04). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 25 ([ошибка]).

- **The decision and its reason.** A mosaic block that crosses the clean zone is not drawn, at rest or pressed. The designer's next package will do the same; under D20 this is made here first. Only frames with heat move: the generator's goldens for them are listed as this item's known entries, as `design-golden.mjs`'s `KNOWN` lists its own.

- AC:
  - no mosaic pixel inside the clean zone at any heat, over every key of the foundations page
  - the generator's goldens without heat are unchanged; those with heat are named with this item, computed by the test, and looked at
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Shapes.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/CleanZone.kt`, `scripts/design-golden.mjs`
