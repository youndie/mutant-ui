---
id: B-120
title: "What a body's parts cost, over B-119"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-120 — What a body's parts cost, over B-119

After [B-119](B-119-the-still-image-marked-finished.md) ten cards run at 23.3 frames a second on the calibrated proxy,
about 43 ms a frame, and the main thread is Skia's for 63 % of it ([the profile](../../logs/b-119/2026-11-18-proxy-x2-profile.txt));
the decorations are about 4.4 ms of it. Skiko's module carries no names, so the profile cannot say which drawing costs.

- **The decision to test.** By leaving out one part at a time from B-119's build (`LocalMutantSkipBodyParts`, as B-78 and
  B-111): the outline, the highlight, the shadow, the clip to the contour, the flow lines. No code changes; a measurement,
  to aim the next cut.
- **Measured before it ran.** The proxy slowed ×2, in one rotation, B-119's build and the five; `TenCards8` and `Blank`, six
  rounds of ten seconds, round 1 not counted. A part **costs** what leaving it out saves in ms a frame if every counted
  round saves more than twice the largest move of `Blank`'s work between the same builds; under that it is below
  resolution.

## Results, 2026-11-18

[The run](../../logs/b-120/2026-11-18-proxy-x2.log), on mains; the builds in [arms.diff](../../logs/b-120/arms.diff).
`Blank`'s work moved by at most 1.0 ms, the line 2.0. Against B-119's build at 23.0 frames a second, ms a frame saved per
counted round:

| Left out of B-119 | Saved | Verdict |
|---|---|---|
| **the outline's stroke** | **14.0, 10.3, 12.6, 10.3, 10.7** | **costs about 11.6 ms**, 31.3 frames a second without it |
| the highlight | 4.5, 6.8, 8.1, 1.9, 5.1 | below resolution by the rule (one round under 2.0), about 5 on the mean |
| the shadow | 2.3, 0.5, 1.6, 0.2, −0.9 | below resolution |
| the clip to the contour | −7.8 to −11.1 | dearer without it: the layers it bounds are drawn whole |
| the flow lines | 4.0, 4.0, 2.8, 0.4, 3.3 | below resolution here (B-118 read 3.9–6.3) |

The outline's stroke is the dearest part of a body, as on the desktop (B-78, 4.0 ms there). It is the next cut,
[B-121](B-121-the-outline-stroked-once-a-tick.md).
