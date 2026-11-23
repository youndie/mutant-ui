---
id: B-179
title: "Bodies tune in as they appear and step out as they leave"
status: wip
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-179 — Bodies tune in as they appear and step out as they leave

Cards, empty states, the tooltip, the cocoon handing over to its content, the search results and the snackbar's words appear and leave in one frame (F-card-cocoon-03, F-search-05, F-empty-state-05, F-snackbar-06, F-pull-to-refresh-04 in `logs/b-168/`). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 1 ([решение]), settle it.

- **The decision and its reason.** `tune-in`: 320 ms in three steps — a 16 dp mosaic over the slot, an 8 dp mosaic, the finished body. Words and icons appear only on the third step. Leaving is two steps over 160 ms: the 8 dp mosaic, then nothing. The badge appears in one frame: it is an on-screen caption that switches on. The snackbar's words appear on the third step of its body's tune-in. It is built once in the surface, so the screen swap (B-180), switching on (B-181) and the dialog (B-185) use the same steps.
- The steps are the token's `stepped(3)` over 320 ms, whatever `steppedMotion` says: they are a picture sequence, not a movement (D22).
- **How the leaving content stays composed: a host, `MutantAppear(visible)`** (research D24). It keeps its content composed for 160 ms after `visible` turns false, deaf to pointer, keys and readers. Every `MutantSurface` inside registers its slot with the host, which draws the slots' mosaics while it tunes and the content (bodies, words, icons) from the third step. A component that keeps its own visibility tunes its own body: the tooltip's plate in its popup, the snackbar through `MutantSurface(tune = …)`. Rejected: each component tuning in on its own first composition — a lazy list's cards would tune in again on every scroll into view, every screen visit would tune every body, and leaving would still need something to hold the content. A component the consumer takes straight out of the composition cannot step out; the KDoc says so.
- **Numbers designed here (D15):** the mosaic's blocks are laid from the slot's origin and clipped to its corner; half of them take the body's fill, a quarter each the cocoon's two paints (`cocoon1`, `cocoon2`), from the stream `rng(<cassette>/<key>, tune<side>)`, fixed for the step. The steps count on the screen's clock from the composition that saw the change: the 16 dp mosaic to 106.7 ms, the 8 dp one to 213.3 ms; leaving, the 8 dp mosaic to 80 ms, nothing to 160 ms. A clock held by a dialog, paused or frozen shows the end at once, as reduce motion does. A turn back half-way goes on from the picture shown (leaving's 8 dp mosaic tunes in from the 8 dp step).
- **The snackbar** keeps B-26's ride: its body tunes in while it rides in (240 ms), its words on the third step, and steps out while it rides down; it stays composed, drawing nothing after 80 ms, until the 200 ms ride ends and `onHidden` is called. A new `message` while it stands tunes it in again, its new words on the third step (F-snackbar-06's cut): this repository's reading of «слова снекбара появляются на третьем шаге tune-in его тела».
- **Not changed:** the pull-to-refresh word (F-pull-to-refresh-04) is a word inside a standing body, not a body appearing; the answer names no step for it, and it stays as it is.
- Rejected: an alpha ramp (no smooth opacity, research D24); the brief's 16 → 8 → 4 → clean with a 40 ms cascade, which the answer replaces.
- Not covered: a whole screen's swap (B-180).

- AC:
  - for each listed component, a frame strip at 0, 107, 213 and 320 ms shows the 16 dp mosaic, the 8 dp mosaic and the finished body, and no glyph or icon pixel before the third step; leaving shows the 8 dp mosaic, then nothing, by 160 ms (read on the test clock's 16 ms frames at 0–96, 112–208 and 224–320 ms: the third step starts at 213.3 ms, so a frame at 213 still shows the 8 dp mosaic; the cocoon's hand-over and the search results take the place of a loading picture, which comes back in one frame, so they have no leaving)
  - the badge's first frame is its finished picture
  - under reduce motion the finished frame shows at once (research D24)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCard.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantEmptyState.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantTooltip.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCocoon.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSearchBar.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSnackbar.kt`
