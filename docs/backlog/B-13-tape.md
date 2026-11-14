---
id: B-13
title: "The Tape («Лента»): one overlay over the screen"
status: wip
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-07, B-09]
---

# B-13 — The Tape («Лента»): one overlay over the screen

Scanlines, grain, a tracking band 22 dp tall passing for 2100 ms every 7000 ms, and head noise at the bottom edge, per theme (the tokens' `tape`): «Эфир» black scanlines 7.5 % one in three dp, «Копия» none (D10).

- **The decision and its reason.** One layer over the screen root, stepping by ticks, drawn from small tiles where it can; off under «Чистый сигнал», still under reduce motion. Its cost is part of [B-06](B-06-frame-cost.md).
- Rejected: the Tape per component, and a full-screen shader as the first version (the brief's own first retreat).

- AC: the Tape matches the demo page's references at fixed times within the floor; the tracking band passes on the 7 s cycle; contrast under it is measured at the worst tick ([B-38](B-38-accessibility.md)).
- Anchors: `reference/design-system/mutant-tokens.json`, `reference/design-system/mutant-ui Демо.dc.html`, `reference/design-system/mutant-engine.js`
