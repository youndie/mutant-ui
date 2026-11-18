---
id: B-119
title: "The still layers' image marked finished"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-119 — The still layers' image marked finished

Found by [B-118](B-118-what-the-decorations-still-cost.md): over B-117's code, all four decorations left out run at 20.0
frames a second, over the kit's at 26.0. B-117's still image is drawn every frame and never marked finished, so Compose on
Skiko may copy it into a new image on every draw (B-113).

- **The decision to test.** Mark the still image finished once drawn (`freeze`, B-113). Nothing drawn changes.
- **Measured before it ran.** The proxy slowed ×2, in one rotation: B-117's build, this item, B-117's build without the four
  decorations, the kit's without them (B-111); `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not counted. It
  **buys its cost** if every counted round is cheaper than B-117's build in ms a frame by more than twice the largest move of
  `Blank`'s work between the same builds.
- AC: the logs; `StillLayersOnceTest` green; a mutant freezing the image before it is drawn into fails it.

## Results, 2026-11-18

[The run](../../logs/b-119/2026-11-18-proxy-x2.log), the proxy slowed ×2, on mains. `Blank`'s work moved by at most 1.0
ms, the line 2.0. Against B-117's build at 18.3 frames a second, ms a frame saved per counted round:

| Build | Saved | Frames a second |
|---|---|---|
| **this item** | **11.5, 12.0, 11.6, 12.2, 11.4** | **23.3** |
| B-117's without the four decorations | 4.2, 3.6, 5.0, 3.3, 3.4 | 19.7 |
| the kit's without the four decorations (B-111) | 16.3, 15.9, 16.9, 15.9, 16.3 | 26.0 |

**It buys its cost, and more than the decorations themselves were thought to.** B-117's image was copied into a new image
on every frame it was drawn; marked finished, the kit with every decoration runs at 23.3, within about 4.4 ms of the kit
without them. Both gates green on `2f0ed7f`; a mutant marking the image before it is drawn into fails
`StillLayersOnceTest`. What remains of the decorations is the flow lines (B-118, about 4.8 ms).
