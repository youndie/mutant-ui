---
id: B-125
title: "What the decorations cost on the phone, over the loop's build"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-125 — What the decorations cost on the phone, over the loop's build

Found by [B-124](B-124-the-loop-on-the-phone.md): on the Pixel 6a the loop's build runs ten cards at 30.0 frames a second,
and without the four decorations at 56.9; on the proxy the gap was 3 frames. The owner's target is 60 frames a second on a
warm phone; heat is not controlled. A profile on the phone puts 66–68 % of the main thread in Skia's own code with and
without the decorations ([final](../../logs/b-124/2026-11-18-pixel6a-profile-final.txt),
[without](../../logs/b-124/2026-11-18-pixel6a-profile-none4.txt)): Skia's CPU work, not a wait on the GPU, though the profile
cannot part frames from the builds ahead that fill an idle page.

- **The decision to test.** By leaving parts out of the loop's build, on the phone: the flow lines; the speckle, the mosaic
  and the dead pixels (their image is still drawn, empty); all four. A measurement, to aim the next cut.
- **Measured before it ran.** Chrome on the Pixel 6a, on mains, in one rotation: the loop's build, the three, and the kit
  without the four decorations (B-111's build); `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not counted. A
  part **costs** what leaving it out saves in ms a frame if every counted round saves more than twice the largest move of
  `Blank`'s work between the same builds.
