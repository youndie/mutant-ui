---
id: B-111
title: "What a body's decorations cost on a phone"
status: wip
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-111 — What a body's decorations cost on a phone

Found by [B-110](B-110-a-body-as-a-shader.md): in Chrome on a Pixel 6a, ten cards with every decoration ran at 22 frames
a second (B-108), and without the speckle, mosaic, dead pixels and flow lines at 60 (B-110), in two runs on two
occasions. If one part carries most of it, a cheaper way to draw that part keeps what the kit shows and may be worth
more than any other change found so far.

- **The decision to test.** Measure by part, as B-78 did on the desktop: `TenCards8` in Chrome on the phone, cool and on
  mains, the kit as it is against builds each leaving out one part (`LocalMutantSkipBodyParts`), paired in one run by
  B-107's protocol, `Blank` as the ruler. The main thread's busy share over the scene beside frames a second, since the
  frame's own work can miss what is built between frames (B-110).
- AC: the logs; the cost of each part; for the costliest, how it is drawn and what drawing it once per tick would take.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`
