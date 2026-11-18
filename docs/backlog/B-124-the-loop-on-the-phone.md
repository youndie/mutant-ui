---
id: B-124
title: "The performance loop's result on the phone"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-124 — The performance loop's result on the phone

The loop B-113…B-123 kept B-115, B-117 and B-119, measured only on the Mac's proxy slowed ×2
([B-123](B-123-each-body-in-its-own-layer.md): 15.9 → 21.1–23.6 frames a second). The owner's Pixel 6a has not run it.

- **Measured before it ran.** Chrome on the Pixel 6a over USB (`DEVICE=1`), cool and on mains, in one rotation with a light
  scene after each heavy one: the kit before the loop (B-111's build), the loop's last commit (`bdc5f76`), the kit without
  the four decorations; `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not counted, the phone's temperature
  logged per scene. The loop's result **holds on the phone** if every counted round of it is cheaper than the kit in ms a
  frame by more than twice the largest move of `Blank`'s work between the same builds.
