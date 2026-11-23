---
id: B-193
title: "Hover keeps the fill and only stirs the outline"
status: open
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-193 — Hover keeps the fill and only stirs the outline

A hovered card's subtitle reads 3.22:1 against 6.9:1 at rest (F-card-cocoon-05). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 16 ([ошибка]).

- **The decision and its reason.** Hover does not change the fill. Its look is only a stronger tremble of the outline, as heat 0.25 draws it, with no amber mix and no bubbles. It is the hover look, so every body that hovers takes it, not the card alone.
- The hover goldens move and are re-recorded and looked at.

- AC:
  - for every hovering component, the fill's pixels hovered equal those at rest and the outline trembles as at heat 0.25
  - a hovered card's subtitle reads the contrast it reads at rest
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantHover.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCard.kt`
