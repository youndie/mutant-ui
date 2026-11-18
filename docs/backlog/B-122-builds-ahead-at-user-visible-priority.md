---
id: B-122
title: "A body's builds ahead posted at user-visible priority in the browser"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-122 — A body's builds ahead posted at user-visible priority in the browser

Found by [B-121](B-121-the-outline-stroked-once-a-tick.md): slowed ×2, as the phone runs, no body reaches its tick image;
every one is built in the frame and drawn as vectors every frame. The builds ahead are posted one task each at
`scheduler.postTask`'s `background` priority (B-87), which runs only when the page is idle, and a page drawing every frame
on a thread 100 % busy never is.

- **The decision to test.** Post them at `user-visible` priority, still one task each, so they run between frames with the
  page's other tasks. Rejected: `user-blocking`, which would run them ahead of input.
- **Measured before it runs.** The proxy slowed ×2, in one rotation: B-119's build, this item; `TenCards8` and `Blank`, six
  rounds of ten seconds, round 1 not counted. It **buys its cost** if every counted round is cheaper than B-119's build in
  ms a frame by more than twice the largest move of `Blank`'s work between the same builds. Beside it, the red control of
  B-121 on this item's code, to see whether the tick images are reached.
- AC: the logs, the control's shots; both gates green.
