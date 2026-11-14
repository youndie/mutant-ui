---
id: B-49
title: "The cost of a drawn body on a Pixel 6a: ten cards with the Tape, a list, an instance"
status: open
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-06, B-10]
---

# B-49 — The cost of a drawn body on a Pixel 6a: ten cards with the Tape, a list, an instance

The brief's target is set on a Pixel 6a: 60 fps with ten cards and the Tape at the kit's tick, a
100-item list scrolling, an instance generated in ≤ 1 ms. [B-06](B-06-frame-cost.md) measured the field
on the desktop JVM and assumed the phone four times slower; this item measures the phone (research D13).

- **The decision and its reason.** As soon as one body is drawn ([B-10](B-10-mutant-surface.md)), a
  throwaway host on the phone draws ten cards and reads `FrameMetrics` (animation + layout + draw; GPU and
  deadline need API 31), with a control that busy-waits a known time inside the same nesting to prove the
  recorder sees it. Pre-registered in this item before the first number, as B-06 was.
- The verdict against the brief's green and red; on red, the brief's retreat order and the handoff's two
  outs, each measured.
- Rejected: measuring last, as plakat-ui did; here the architecture depends on the answer.

- AC: a verdict per target with raw logs under `logs/b-49/` and the control's reading; the factor between
  the M1 and the phone measured, so B-06's assumption is confirmed or refuted in writing.
- Anchors: `docs/backlog/B-06-frame-cost.md`, `reference/design-system/brief.md`
