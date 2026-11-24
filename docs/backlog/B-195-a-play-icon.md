---
id: B-195
title: "A play icon"
status: wip
priority: P2
size: XS
stage: stage-6-later
blocked_by: []
---

# B-195 — A play icon

The kit's 16 icons have no play triangle; the design draws one three times (F-screens-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 20 ([решение]).

- **The decision and its reason.** A 17th icon, `M8 5l11 7-11 7z` on the 24 grid, stroke 2, with no damage accent. The icon check against the design lists it as this item's entry (D20).

- AC:
  - a new `Play` icon in `MutantIcons` draws the path at 24 dp with a 2 dp stroke, a golden recorded and looked at
  - the icon check passes with the new icon named as a known entry; the sample uses it where the design drew a triangle
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/icons/MutantIconPaths.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/icons/MutantIcons.kt`, `scripts/icons.mjs`
