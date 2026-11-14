---
id: B-50
title: "Build a body's instance and first frame off the frame"
status: wip
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-10]
---

# B-50 — Build a body's instance and first frame off the frame

`MutantSurface` builds the instance and the field's static layers in its draw cache, on the main thread:
B-06 measured that first frame at about ten times a warm one (5.8 ms for a 328 × 80 card on the M1), so
ten bodies appearing together cost about 58 ms there and more on a phone (research §1.9).

- **The decision and its reason.** Build the instance and the first frame on a background dispatcher when
  the slot's size and zone are known, and draw the body from the first frame that has them; until then
  draw nothing, or the plain rounded rectangle of the slot (decided here, with the owner's review round).
  Warm frames stay on the main thread: 0.5 ms for a card.
- Tests and references must stay deterministic: a frozen clock builds synchronously.
- Rejected: building every visible body's static layers inside one frame (today's behaviour).

- AC: ten cards appearing in one frame take no frame longer than a warm frame's budget plus the layout;
  the first drawn frame is pixel-identical to today's; a frozen clock draws in the first frame as before.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `logs/b-06/2026-11-14-desktop-2.summary.txt`
