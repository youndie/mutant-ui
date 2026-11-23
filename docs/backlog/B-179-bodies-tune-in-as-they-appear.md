---
id: B-179
title: "Bodies tune in as they appear and step out as they leave"
status: open
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-179 — Bodies tune in as they appear and step out as they leave

Cards, empty states, the tooltip, the cocoon handing over to its content, the search results and the snackbar's words appear and leave in one frame (F-card-cocoon-03, F-search-05, F-empty-state-05, F-snackbar-06, F-pull-to-refresh-04 in `logs/b-168/`). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 1 ([решение]), settle it.

- **The decision and its reason.** `tune-in`: 320 ms in three steps — a 16 dp mosaic over the slot, an 8 dp mosaic, the finished body. Words and icons appear only on the third step. Leaving is two steps over 160 ms: the 8 dp mosaic, then nothing. The badge appears in one frame: it is an on-screen caption that switches on. The snackbar's words appear on the third step of its body's tune-in. It is built once in the surface, so the screen swap (B-180), switching on (B-181) and the dialog (B-185) use the same steps.
- The steps are the token's `stepped(3)` over 320 ms, whatever `steppedMotion` says: they are a picture sequence, not a movement (D22).
- Rejected: an alpha ramp (no smooth opacity, research D24); the brief's 16 → 8 → 4 → clean with a 40 ms cascade, which the answer replaces.
- Not covered: a whole screen's swap (B-180).

- AC:
  - for each listed component, a frame strip at 0, 107, 213 and 320 ms shows the 16 dp mosaic, the 8 dp mosaic and the finished body, and no glyph or icon pixel before the third step; leaving shows the 8 dp mosaic, then nothing, by 160 ms
  - the badge's first frame is its finished picture
  - under reduce motion the finished frame shows at once (research D24)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCard.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantEmptyState.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTooltip.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCocoon.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSearchBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`
