---
id: B-211
title: "Two levels of frame work in the phone's Chrome"
status: open
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-211 — Two levels of frame work in the phone's Chrome

Found by [B-197](B-197-the-tape-over-every-layer.md)'s Chrome run on the Pixel 6a
(`logs/b-197/2026-11-25-pixel-ab.log`, `frames.csv` beside it): every held scene of both builds sits, for whole
seconds, at one of two levels of work a frame, about 5.5 or about 9.5 ms. The upper level fell on `main`'s resting
Components page, unchanged by B-197, in three of five counted rounds (5.4, 9.2, 8.9, 6.3, 9.9 ms) and on the branch's
menu page in one. The control (10 ms busy-waited over `ComponentsReduce`) read 8.2–8.6 ms throughout, so the recorder
was not what moved. With it, an A/B of two builds in the phone's Chrome cannot resolve 1–2 ms at five counted rounds:
B-197's thresholds of 1 and 2 ms came out red on either side.

- **The hypotheses**, none tested: Chrome's renderer main thread moving between the phone's mid and big cores (per
  scene, or by the heat: the phone ran 34.7 → 39.0 °C, charging); ADPF hints in Chrome's compositor; B-140's "levels and
  steps" seen once on a 60 s load.
- AC:
  - which of the hypotheses moves the level, by one variable changed on our side (the renderer's core read per second
    beside the work; the phone cool and not charging against warm and charging), with a log per arm in `logs/b-211/`
  - the bench's A/B either resolves 2 ms on the resting page against itself (`ComponentsRest` against `ComponentsRest~b`
    from the same build, every counted round within 1 ms), or its header says what it cannot resolve
- Anchors: `scripts/frame-bench-browser.mjs`
