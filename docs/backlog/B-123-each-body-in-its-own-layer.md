---
id: B-123
title: "Each body drawn in a layer of its own"
status: dropped
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-123 — Each body drawn in a layer of its own

Found by [B-121](B-121-the-outline-stroked-once-a-tick.md) and [B-122](B-122-builds-ahead-at-user-visible-priority.md): at the
phone's load every body is drawn as vectors in the frame, and its tick image is out of reach. A body's drawing changes once a
tick, but a frame between two ticks runs its draw code again, in Kotlin, call by call into Skia, wherever something else on
the screen asks for a frame.

- **The decision to test.** A `graphicsLayer` around each body's drawing, so a frame between ticks replays the layer's
  recorded picture instead of running the body's draw code. Nothing drawn changes. Rejected: one layer for the whole
  screen, which a tick of any body would record again whole.
- **Measured before it runs.** The proxy slowed ×2, in one rotation: B-119's build, this item; `TenCards8` and `Blank`, six
  rounds of ten seconds, round 1 not counted. It **buys its cost** if every counted round is cheaper than B-119's build in
  ms a frame by more than twice the largest move of `Blank`'s work between the same builds.
- AC: both gates green with nothing raised (the drips hang outside the body's box, and a layer must not cut them).

## Results, 2026-11-18: nothing, and reverted

Both gates green with the layer, and a mutant clipping it to the body's box failed `CardParityTest` and `ButtonParityTest`.
On [the run](../../logs/b-123/2026-11-18-proxy-x2.log), the proxy slowed ×2: −0.2, 0.2, 0.4, 0.5, 0.0 ms a frame against
B-119's build (23.4 and 23.5 frames a second), under the line of 2.2. Reverted (`49d08a7`). It is the third cut in a row that
bought nothing (B-121, B-122), so the loop that ran B-113 to B-123 stops here.

## The loop's end, measured on its last commit

[The run](../../logs/b-123/2026-11-18-final-against-kit-x2.log), the proxy slowed ×2, on mains, `bdc5f76` (B-115, B-117,
B-119 kept, everything else reverted) against the kit before the loop (B-111's build): **21.1–23.6 frames a second against
15.8–16.0** (one round of the kit dipped to 10.0), 15.5 to 20.5 ms a frame saved in every counted round; the kit without the
four decorations ran at 25.6–25.9 in the same run. Every decoration is kept, and a frozen clock draws the same pixels as
before. The phone has not seen it.
