---
id: B-124
title: "The performance loop's result on the phone"
status: done
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

## Results, 2026-11-18

[The run](../../logs/b-124/2026-11-18-pixel6a.log): Chrome 157 on the Pixel 6a, on mains, from 27.2 to 35.0 °C over the run.
`Blank`'s work moved by at most 0.6 ms between the kit and the loop's build, the line 1.2.

| Build | Frames a second | ms a frame saved against the kit, per counted round |
|---|---|---|
| the kit before the loop | 22.2 | — |
| **the loop's last commit** | **30.0** | **7.9, 12.1, 11.3, 15.6, 13.1** |
| the kit without the four decorations | 56.9 | 22.1, 28.5, 30.4, 24.3, 31.1 |

**The result holds on the phone**: every counted round faster, by 6.7 to 9.4 frames a second (work p50 by 4.9 to 11.4 ms).
But on the phone the decorations still cost far more than on the proxy: without them the page runs at 56.9, against 30.0
with them (on the proxy 25.7 against 22–23). The proxy is calibrated to the kit's cost as a whole, not to how that cost
divides between the parts, so what the decorations still cost is a question for the phone itself.
