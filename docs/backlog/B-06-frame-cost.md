---
id: B-06
title: "The cost of the field: genItem and one frame on the desktop JVM, then a body on a Pixel 6a"
status: open
priority: P0
size: M
stage: stage-0-risks
blocked_by: [B-03]
---

# B-06 — The cost of the field: genItem and one frame on the desktop JVM, then a body on a Pixel 6a

The handoff names this the main risk: the signed-distance field is recomputed on the CPU each tick for every visible instance, and the speed on a device was never measured. The brief's target is 60 fps on a Pixel 6a with ten cards and the Tape, a 100-item list scrolling, and an instance generated in ≤ 1 ms (research D13, H3).

- **The decision and its reason.** Measure where the architecture can still change: `genItem` and one frame per profile on the desktop JVM right after [B-03](B-03-generator-port.md); then the first drawn body ([B-10](B-10-mutant-surface.md)) on a Pixel 6a, ten cards, `FrameMetrics` (animation + layout + draw), with a control that busy-waits a known time inside the same nesting to prove the recorder sees it.
- Verdict against the brief's green and red; on red, the brief's retreat order (static Tape tile, no grain, idle ticks only for six visible components, mosaic only on state change) and the handoff's two outs (a coarser grid, a shader field), each measured.
- Rejected: measuring last (plakat-ui's order); there the architecture did not depend on the answer.

- AC: a verdict per target in the research with raw numbers and the control's reading; the instance budget (≤ 1 ms) and the frame budget each green, red or not measured.
- Anchors: `reference/design-system/mutant-generator.js`, `reference/design-system/brief.md`, `reference/design-system/mutant-engine.js`
