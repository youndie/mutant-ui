---
id: B-126
title: "Bodies on the GPU, with every decoration"
status: question
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

### Stage 0 results, 2026-11-18

[The run](../../logs/b-126/2026-11-18-pixel6a-stage0.log), the phone at 35.9–36.4 °C, on mains; the shader build differs
from the paths build as [gpu-none4.diff](../../logs/b-126/gpu-none4.diff) says. Pooled frames a second, the decorations
left out of all three: **paths 61.2, the shader 61.0, the kit before the loop 60.0**; every round of every build at 60–61
but one of the kit's (55.4). The shader's work p50 is lower in every counted round, by 2.1 to 2.7 ms (4.5 against 6.8).

By the rule set before it ran: 55 or more, but not faster in frames a second in every round, since both stand at the
display's 60. **Grey.** What it settles is larger than the rule: **at 36 °C bodies without decorations hold 60 frames a
second drawn by paths**, so the GPU silhouette has nothing to win there, and B-125's reading that the bodies themselves are
over the frame was taken at 38.2–38.6 °C, where the phone throttles in steps (one build swung from 10 to 62 frames a
second). It is corrected there. What keeps the page from 60 at this heat is the decorations and the still layers' image.
The silhouette on the GPU is set aside; the decorations go first ([B-127](B-127-speckle-and-fill-as-one-tile.md)).
