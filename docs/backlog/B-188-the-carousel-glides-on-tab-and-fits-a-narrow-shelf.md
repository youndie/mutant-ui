---
id: B-188
title: "The carousel glides on Tab and fits a narrow shelf"
status: wip
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-188 — The carousel glides on Tab and fits a narrow shelf

Tab to the next carousel card moves the shelf by Compose's bring-into-view (234 dp in 4–5 frames), not the press's glide (F-carousel-07). Under 304 dp the current card is cut, and many dots run past a narrow shelf (F-carousel-05, F-carousel-06). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 10 ([ошибка]) and 33 ([решение]).

- **The decision and its reason.** A Tab scroll takes 330 ms on the `slide` curve. A card is min(220, the window's width − 84) dp wide. With more than seven cards a counter «03/12» (Handjet 18, the `osd` colour) stands instead of the dots.

- AC:
  - Tab to the next card scrolls the shelf over 330 ms on the slide curve
  - at widths from 400 down to 200 dp the current card is whole and min(220, w − 84) wide
  - eight cards show «01/08» and no dots; seven show seven dots
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCarousel.kt`
