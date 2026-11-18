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

## Why the image costs: two diagnostic runs, the phone hot (38.4–38.6 °C)

Builds of the loop's code with all four decorations left out, differing as [none4-no-image.diff](../../logs/b-125/none4-no-image.diff)
and the lines below say; not kept.

[First run](../../logs/b-125/2026-11-18-pixel6a-image-floor.log), pooled frames a second: with the empty image 18.7,
without it 29.4, the kit before the loop without the decorations 29.7. Per round the builds swung from 10 to 62 frames a
second: at this heat the phone's own throttling moves more than any build does, and no round order holds.

[Second run](../../logs/b-125/2026-11-18-pixel6a-image-why.log), pooled: the empty image 24.7; drawn without the contour's
clip 21.0; drawn with nearest sampling 22.1; not drawn 33.0 (rounds 24.3, 23.1, 34.0, 21.8, 61.6). Neither the clip nor the
sampling is what costs; not drawing the image is the only change that moves the mean, and it moves it within the heat's
noise.

**What this leaves.** Hot, the phone runs ten cards at 21–33 frames a second whatever the decorations, and the kit without
them at 30–47: the bodies themselves, built and drawn on the CPU on one thread, are over a 16.7 ms frame before any
decoration. Skia's own code is two thirds of that thread with the decorations or without them.

**Corrected by [B-126](B-126-bodies-on-the-gpu.md)'s stage 0:** at 36 °C, minutes later, the loop's code without the
decorations and without the still image ran at 61.2, the kit without them at 60.0. The paragraph above holds for the
phone above 38 °C, not for a warm phone: there the bodies hold 60, and the decorations with their image are what is over.
