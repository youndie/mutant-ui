---
id: B-63
title: "Desktop: the hand cursor and a hover look"
status: wip
priority: P2
size: M
stage: stage-5-product
blocked_by: [B-40]
---

# B-63 — Desktop: the hand cursor and a hover look

Found in [B-40](B-40-qa-pass-and-real-window.md) (F13), and confirmed in the real window: over `Play` the cursor stays the arrow and the button does not change. No component sets `pointerHoverIcon` or reads hover. [B-43](B-43-undesigned-behaviour.md) decided that what the design does not draw is designed here (D15), and hover is in that list.

- **The decision.** Every clickable component shows the hand cursor. A hover look is designed here, for `PointerType.Mouse` only, with every number the design does not state listed in this item; its goldens are recorded when built and revised in the owner's review round. The rejected alternative is the cursor alone: a mouse user then learns what is live only by pressing.
- AC: the cursor read through `pointerHoverIcon` in a test per component; the hover look held by a pixel test; a finger's press leaves no hover.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantButton.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/qa/OverlayAndMouseTest.kt`
