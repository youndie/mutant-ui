---
id: B-125
title: "What the decorations cost on the phone, over the loop's build"
status: done
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

## Results, 2026-11-18

[The run](../../logs/b-125/2026-11-18-pixel6a.log), on mains, the phone from 32.1 to 38.2 °C; the builds in
[arms.diff](../../logs/b-125/arms.diff). `Blank`'s work moved by at most 2.3 ms, the line 4.6. Against the loop's build at
23.1 frames a second, ms a frame saved per counted round:

| Left out of the loop's build | Saved | Frames a second |
|---|---|---|
| the flow lines | −0.8, 2.6, 2.3, 7.9, −5.6 | 23.8, below resolution |
| the speckle, mosaic and dead pixels (their image drawn empty) | −2.2, 1.3, 1.6, 15.0, −26.2 | 22.9, below resolution |
| all four | −1.5, 5.2, 2.3, 13.4, 16.8 | 26.6, below resolution |
| *the kit before the loop without the four* | 18.7, 22.9, 21.1, 16.8, 20.4 | **47.3** |

**The decorations are not what is left.** The loop's build without any of them runs at 26.6, the kit before the loop
without them at 47.3: about 20 ms a frame sit in the loop's own code, as the proxy hinted (B-118, a floor of some 12 ms).
What the loop's code draws every frame that the kit did not is the still layers' image, the size of the body's box, under
the contour's clip, drawn even when it holds nothing.
