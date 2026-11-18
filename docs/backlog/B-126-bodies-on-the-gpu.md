---
id: B-126
title: "Bodies on the GPU, with every decoration"
status: wip
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-126 — Bodies on the GPU, with every decoration

The owner's choice after [B-125](B-125-what-the-decorations-cost-on-the-phone-now.md): hot, the Pixel 6a runs ten cards at
21–33 frames a second whatever the decorations, the kit without them at 30–47, and Skia's CPU work is two thirds of the one
thread. The target is 60 frames a second on a warm phone, every decoration kept. [B-110](B-110-a-body-as-a-shader.md)'s
spike drew a body's silhouette by a runtime shader from its field (decorations off): on a cool phone both arms ran at 60,
the main thread 65 % busy against 91 %.

## Stage 0: does the silhouette alone reach 60 on a hot phone

- **Measured before it ran.** Chrome on the Pixel 6a, hot as it is, on mains, in one rotation, all with the four
  decorations left out: the loop's code drawing bodies by paths without the still image (B-125's `none4noImage`), the same
  code with `BODY_ON_GPU = true`, the kit before the loop; `TenCards8` and `Blank`, six rounds of ten seconds, round 1 not
  counted. **The way is open** if the shader build's pooled frames a second is 55 or more and every counted round of it is
  faster than the paths build; **it is closed** if its pooled rate is under 40. Between, it is grey and the owner decides.
