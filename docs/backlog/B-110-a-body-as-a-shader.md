---
id: B-110
title: "A body drawn by a GPU shader: a spike"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-110 — A body drawn by a GPU shader: a spike

The owner, 2026-11-17: why is performance a problem when the graphics are simple; is it because it is computed on the
CPU; can acceleration, shaders, be used. [B-108](B-108-a-phones-budget-for-the-boil.md) found Chrome's main thread on a
Pixel 6a spending 65 % in Skia's own CPU work over ten cards and 3 % in WebGL.

A body is a signed-distance field on a grid of 0.9 or 1.5 dp (`distanceField`), marched twice into its outline and its
highlight and smoothed (`march`, `smoothPath`), then filled and stroked with antialiasing by Skia, or rasterised into an
image once per tick (B-82). Thresholding a field per pixel is what a fragment shader does.

- **The decision to test.** Keep the generator and its field on the CPU, as they are; hand the field to a shader as a
  small texture once per tick; let the shader draw the silhouette per pixel: the misregistered shadow, the fill shifted
  by the slip, the highlight and its crescent inside their clips, the outline. No `march`, no `smoothPath`, no path
  raster, no image ahead. Skiko's runtime effects (SkSL) on desktop and in the browser, where Skia runs it through
  WebGL; the spike leaves iOS and Android (`RuntimeShader`, API 33) to the CPU path.
- Rejected for the spike: the whole field on the GPU (the noise, the warps, the drips in SkSL), which is a larger port
  and is only worth it if the smaller one pays; the decorations (speckle, mosaic, dead pixels, flow lines), which the
  spike leaves out of both arms alike (`LocalMutantSkipBodyParts`).

- **Pre-registered on 2026-11-17, before the first number.**
  - **Q1, what it draws.** A desktop test renders the same bodies at a frozen time (5000 ms) both ways, with the same
    parts: a 328 × 80 card, a 160 × 48 button and a 56 × 56 FAB, in «Эфир» and «Копия», at density 1 and 2.625. The
    measure is the share of pixels of the body's box (its slot plus drips, shadow and slip) that differ by more than 32
    in any channel. **Green** ≤ 2 % in every case; **red** > 6 % in any; grey otherwise. A contact sheet of both is kept
    and looked at.
  - **Q2, what it costs.** `TenCards8` in Chrome on the Pixel 6a, cool and on mains, two production builds paired in one
    run by B-107's protocol (five rounds of 8 s, round 1 not counted): the kit as it is (raster ahead) and the shader,
    both leaving out the same decorations. The ruler is a new `Blank` scene (the ground and the Tape, no bodies), which
    neither arm can reach. A difference counts when every counted round's work p50 moves the same way and the mean is
    more than twice the ruler's spread. **Green**: the shader's work p50 is lower by that rule and at most half the
    kit's; **red**: not lower by that rule; grey otherwise. Frames a second reported beside.
  - **Kill.** If SkSL does not compile or draw in the browser's Skia, Q2 is not measured, and the item says why.
  - **Not claimed.** iOS, Android, the decorations, pressed and pouring bodies, transitions.
- AC: the test and its sheet; the paired log under `logs/b-110/`; a verdict per question; what adopting it would take.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Frame.kt`
