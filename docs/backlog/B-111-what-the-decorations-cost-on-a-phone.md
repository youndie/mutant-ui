---
id: B-111
title: "What a body's decorations cost on a phone"
status: done
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

## Results, 2026-11-17

Two runs in Chrome on the Pixel 6a, on mains, at `ebf6d4c` and `5cf10be`: [run 1](../../logs/b-111/2026-11-17-pixel6a-parts.log),
[run 2](../../logs/b-111/2026-11-17-pixel6a-clip.log), the builds' one-line differences in
[arms.diff](../../logs/b-111/arms.diff). Each build is the kit with one default changed, `LocalMutantSkipBodyParts`; a part
left out is left out of the draw only, and the generator still builds the mosaic's edge and the flow lines.

**The lead holds, in one run.** The kit ran `TenCards8` at 21.5 and 18.3 frames a second; without the four decorations
at 44.2 and 45.2, and at 60.7–61.6 in every position where the phone was coolest. Every counted round of both runs moved
the same way, by 10–47 frames a second. `Blank`, the ruler, stayed at 60.6–61.6 in every build.

**The busy share cannot tell the builds apart here.** The renderer's task time read 99.8 % with the 10 ms control against
67 % without it ([the control](../../logs/b-111/2026-11-17-pixel6a-control.log)), so it sees work; but every build with
cards on screen is saturated, at 100 %, and differs by under 0.1 point. Frames a second is the reading, and on a saturated
thread its inverse is the cost: **milliseconds a frame**, `1000 / fps`.

**Read in milliseconds, the parts add up.** In frames a second they did not (leaving out the speckle gave +4 to +7,
the other three about 0, all four +23 to +27), which was the inverse and the 60 Hz ceiling, not an interaction. Run 2, the
kit at 47.8–49.0 ms a frame, saved per counted round:

| Left out | Rounds 2–4 (31–36 °C) | Rounds 5–6 (37 °C) |
|---|---|---|
| all four decorations | 31.6, 23.9, 22.4 — a floor: the build reached the 60 Hz ceiling | 53.2, 25.0 |
| the clip to the contour (every decoration kept, drawn unclipped) | 17.3, 16.0, 16.8 | 12.9, 4.2 |
| the speckle | 12.8, 12.7, 12.1 | 10.3, 24.9 |
| the mosaic, the dead pixels and the flow lines | 10.7, 10.4, 10.3 | 24.0, 23.0 |
| the speckle, the dead pixels and the flow lines | 20.1, 20.6, 18.7 | 28.3, 31.8 |

The speckle (12.5) and the other three (10.5) sum to 23, what all four save in rounds 3–4. So the mosaic alone is about
2–4 ms (all four less the last row), the dead pixels and flow lines together 7–8. Run 1's single parts, in its coolest
positions, agree in order: the mosaic, the dead pixels and the flow lines each 0–3 ms.

**Heat.** The phone went from 28.8 to 36.7 °C in run 1 and from 31.3 to 37.7 °C in run 2, and would not cool under 31.5 °C
on mains. Run 1 ran six heavy builds back to back, and the later in each streak lost up to half their frames, so its
leave-one-out rows are read only in their first positions; run 2 put a light scene after each heavy one and logs the
temperature per scene. Its rounds 5–6, at 37 °C, are noisier; the split by round is made after the fact and shown whole.

**The costliest: the clip and the speckle, the same mechanism as on the desktop.** B-78 found the clip and the speckle
dearest on the desktop's render thread, and the small draws under the contour clip the expensive part. Here the body is
drawn into an image once per tick ahead (B-82), on the one main thread, and the same shows larger: every one of a card's
draws inside the clip goes through the contour's anti-aliased clip, and the speckle alone is about 290 of them, eight
squares in every 40 dp tile over the body's box. The mosaic's tiles and dead pixels are more of the same, a few dozen each.

**What drawing it once would take.** It is drawn once per tick already; what costs is how many draws a tick makes inside the
clip. The speckle depends on the cassette, the stage and the wear only, the mosaic's tiles and the dead pixels on the
instance: none of them moves with the tick. Drawn once into an image per instance, they become one draw a tick, and that
image and the fill can be painted *as the contour path's shader* (`drawPath(contour, ImageShader)`), with no clip at all;
only the flow lines, which boil, still need the clip, as one draw. Whether that buys what leaving them out buys, and keeps
the picture, is [B-112](B-112-static-decorations-drawn-once.md).
