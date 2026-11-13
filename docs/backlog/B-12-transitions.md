---
id: B-12
title: "Transitions: pour, melt, slide, and the venom with its soft joins"
status: open
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-04, B-11]
---

# B-12 — Transitions: pour, melt, slide, and the venom with its soft joins

A colour change pours down (`clamp(260 + 5·h, 300, 620)` ms at 12 fps); an indicator melts in or out (330 ms); a switch thumb slides 330 ms with a 40 % stretch; a selection moves by venom (720 ms at 24 fps) whose fat tentacle and masses are softened by a blur of σ 3.2 dp and an alpha threshold `a' = 18·a − 8.2` (H4).

- **The decision and its reason.** Each transition is a closed form of elapsed time on the kit clock, its geometry from [B-04](B-04-transition-geometry.md). The soft joins are a layer with a blur `RenderEffect` and a colour-matrix filter where the platform has them; on Android below 12 the venom draws its geometric shapes as they are, which the handoff allows.
- Under reduce motion every transition lands in one frame (D7).

- AC: each transition's frames at its tick boundaries match the motion page's references within the floor; the threshold layer reproduces the page's venom within the floor on desktop; on API < 31 the venom still draws; reduce motion lands each in one frame.
- Anchors: `reference/design-system/mutant-ui Анимационные спеки.dc.html`, `reference/design-system/mutant-engine.js`, `reference/design-system/mutant-generator.js`
