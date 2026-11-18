---
id: B-119
title: "The still layers' image marked finished"
status: wip
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
