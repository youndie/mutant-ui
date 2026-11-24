---
id: B-206
title: "A body's default key is its place in the composition"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-206 — A body's default key is its place in the composition

Default keys are a constant or the label: unkeyed cards, rows, dividers, radios and dialogs are clones that boil in step, the dialog's window key cannot be set at all, and a play/pause icon button or a running `REC · 0:42:17` tooltip reshapes its body on every change (F-card-cocoon-04, F-icon-fab-03, F-tooltip-04, F-dialog-10). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 38 ([решение]).

- **The decision and its reason.** By default a body's key is its place in the composition (`currentCompositeKeyHash`) and its body type. In lists an explicit key from the item's id is required; the KDoc and the sample say so, as the kit cannot enforce it.
- Fixtures that relied on the label as a key pass it explicitly, so the goldens hold; any that move are listed and looked at.

- AC:
  - two unkeyed cards side by side boil differently; the same card recomposed keeps its shape
  - an icon button whose icon changes and a tooltip whose text runs keep their bodies
  - the dialog's window takes a key
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantDialog.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCard.kt`
