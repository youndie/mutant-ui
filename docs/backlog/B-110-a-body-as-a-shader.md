---
id: B-110
title: "A body drawn by a GPU shader: a spike"
status: done
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

## Findings, 2026-11-17

Logs under [`logs/b-110/`](../../logs/b-110/): the parity shares and three of the sheets, the arms' diff (`arms.diff`:
the two builds differ by the switch alone, both leaving out the decorations), the paired runs on the Mac, the phone proxy
and the Pixel 6a, and a profile of each arm on the phone. The phone was cool (26.9 °C at the start, 30.1 at the end),
full and on mains.

| Question | Verdict | The number |
|---|---|---|
| Q1, what it draws | **GREY** | 1.86–3.55 % of the stage's pixels differ by more than 32, over 12 cases (green ≤ 2 %, red > 6 %) |
| Q2, what it costs on the Pixel 6a | **RED** by the rule | both arms at 60 frames a second; work p50 per counted round −0.4, −0.4, −0.6, −1.1 ms, mean −0.62, against twice the ruler's spread of 1.6 |

- **What Q1's difference is.** Along every edge the shader draws the field's own iso-line where the kit draws its
  smoothed polyline: parts of a pixel. One place differs in kind: where a mosaic block quantises the field along the
  top edge, the kit marches clean steps and the shader draws ragged dark blocks (the card's sheet). Dividing the value by
  its gradient where that is under 1 took the solid blocks away; dividing everywhere drew seams of the field far outside
  the body, and was taken out.
- **Why Q2 is red, and what it did not see.** With the decorations left out of both arms, the kit as it is already
  draws ten cards at 60 frames a second on this phone, so the frame's own work could not tell the arms apart. What
  differs is outside the frame: over 8 s the main thread was busy **90.5 %** with the kit's paths and images and
  **65.2 %** with the shader. By module, the paths arm spent 64.2 % of its busy time in Skia and 27.4 % in Kotlin; the
  shader arm 21.4 % in Skia, 46.9 % in Kotlin (the field, still on the CPU), 16.3 % in the browser (WebGL) and 9.9 % in
  the page's JavaScript (the texture's upload). Reported, not counted: the rule was written for the frame's work.
- **Reported, not counted: where the thread is saturated, the shader pays.** On the phone proxy (Chrome on the Mac, CPU
  slowed 4×), ten cards ran at 5.0 frames a second with paths and images and 20.7 with the shader, work p50 211.9
  against 16.1 ms, every counted round about −194 ms; the ruler moved −0.5. On the Mac both arms run at 60.
- **A finding beside the question.** In B-108 the same ten cards with every decoration ran at 22 frames a second on this
  phone; here, without the speckle, mosaic, dead pixels and flow lines, the kit as it is runs at 60. Not the same run
  (B-108 at 28–31 °C, this at 27–30, the same build otherwise), so it is a lead, not a measurement: what the decorations
  cost, by part, is [B-111](B-111-what-the-decorations-cost-on-a-phone.md).

## What adopting it would take

- The decorations through the shader too, or masked by it: the speckle is a tiled pattern, the mosaic and dead pixels
  are rectangles, the flow lines are paths; all are clipped by the outline today.
- The mosaic's edge drawn as the kit draws it, and the parity line held per body (a departure, D20, with an
  `asDesigned` switch keeping the paths).
- iOS (Skia, the same SkSL) and Android (`RuntimeShader`, API 33; the paths below it).
- Pressed, pouring and sliding bodies, which the spike draws by paths.
