---
id: B-48
title: "Keyframe loss: a band of the screen breaks into 16 and 8 dp blocks every 9 s"
status: open
priority: P3
size: M
stage: stage-6-later
blocked_by: [B-13, B-06]
---

# B-48 — Keyframe loss: a band of the screen breaks into 16 and 8 dp blocks every 9 s

Every 9 s a band of 9–18 block rows breaks into 16 and 8 dp blocks for 900–1400 ms and recovers in 375 ms; the browser does it with a raster snapshot. The handoff calls it optional (D10).

- **The decision and its reason.** Deferred until the Tape and the frame cost are known; in Compose it is a `GraphicsLayer` snapshot or a shader. Its flash budget is in the tokens.

- AC: the owner decides to build it; then it matches the demo references and stays inside the measured flash budget.
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/mutant-engine.js`
