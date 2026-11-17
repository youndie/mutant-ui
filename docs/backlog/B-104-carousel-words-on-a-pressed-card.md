---
id: B-104
title: "A pressed carousel card keeps its light words on amber"
status: done
priority: P1
size: S
stage: stage-6-later
---

# B-104 — A pressed carousel card keeps its light words on amber

The owner, 2026-11-17: «в карусели при таче шрифт не меняет цвет — не читаемо».

- **Seen before.** A frame strip of a press on a carousel card, every eight frames: the card boils to amber and its
  title, subtitle and status stay in the text's light colours, the secondary text and the OSD amber on it. The card
  turns its words black on the same press (B-91); the carousel's own card never asked.
- **Cause, the carousel.** `CarouselCard` set its words' colours from the theme, not through `inkOnBody`, so they
  never followed the fill.
- **Cause, under it.** With the carousel fixed, the subtitle and the status still read 2.05:1 for one frame of every
  press. The ground `inkOnBody` picks against was derived on every frame from the heat at that frame, while a boiling
  body is redrawn on the hot tick (83 ms) with the heat of the frame that drew it. Between two ticks the ground ran
  ahead of the fill on screen, and a dim word (the secondary text, the amber status) turned black a frame or more
  before its ground went amber. The card's title happened to flip on a tick and passed B-91; a card's subtitle
  shared the fault, untested.
- **Fix.**
  - The carousel's title, subtitle and status go through `inkOnBody`, as the card's lines do.
  - The ground follows the hot tick while the body boils, and every frame only while a new fill pours, as the body's
    drawing does. Nothing drawn changes; no golden moves.
- AC:
  - `PressedLabelContrastTest` holds the carousel's title, subtitle and status at 3:1 or better on every frame of a
    real press and release, in both themes;
  - it fails with the carousel's words taken from the theme again, and with the ground followed on every frame.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCarousel.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
