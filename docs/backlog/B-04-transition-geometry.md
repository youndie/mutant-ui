---
id: B-04
title: "Transition geometry: venom, pour and error blocks ported and held to goldens"
status: wip
priority: P0
size: M
stage: stage-0-risks
blocked_by: [B-03]
---

# B-04 — Transition geometry: venom, pour and error blocks ported and held to goldens

Three functions draw outside the resting frame: `venomShapes` (the selection's tentacles and masses in Segmented button, Menu and Drawer), `pourPath` (a new colour running down) and `errBlocks` (corruption around an error). The vendored golden holds nine venom shapes and nothing else of these.

- **The decision and its reason.** Port all three like [B-03](B-03-generator-port.md), output as path nodes and rounded rectangles; goldens over the three venom cases at p = 0…1 in 1/24 steps, `pourPath` at its 12 fps frames, `errBlocks` over the profiles that show an error.
- Rejected: drawing venom by hand from the motion spec page; its timing lives in the function.
- Not covered: the blur and alpha threshold that soften the venom's joins ([B-12](B-12-transitions.md)).

- AC: goldens regenerate byte-identically; the port matches them on all targets within ±0.1 dp; the nine vendored venom entries pass; a mutant in the venom's phase boundaries (0.25, 0.2–0.8, 0.96) is caught.
- Anchors: `reference/design-system/mutant-generator.js`, `reference/design-system/mutant-golden.json`, `reference/design-system/mutant-ui Анимационные спеки.dc.html`
