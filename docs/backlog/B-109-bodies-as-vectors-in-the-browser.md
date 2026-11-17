---
id: B-109
title: "Resting bodies as vectors in the browser"
status: open
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-109 — Resting bodies as vectors in the browser

Found by [B-108](B-108-the-browser-against-android.md): in Chrome on a Pixel 6a, ten boiling cards spend 64.9 % of the
main thread in Skia's own module and 3.4 % in the browser's WebGL. The kit rasterises a resting body into an
`ImageBitmap` once per tick (B-82, `LocalMutantBodyAsImage`), which on Android is done ahead on background threads,
about one core's worth; in the browser an `ImageBitmap` is a CPU raster on the only thread.

- **The decision to test.** In the browser, draw resting bodies as vectors each frame (`LocalMutantBodyAsImage` off on
  wasm), so Skia's WebGL backend fills them on the GPU. It changes no pixel the kit means to draw, only who rasterises.
- Rejected for now: a worker thread (Kotlin/Wasm's Compose has no shared memory); WebGPU (not a Compose web backend).
- AC: the same scene (`TenCards8`, and the resting Components page) in Chrome on the Pixel 6a, both builds paired in one
  run by B-107's protocol, the phone cool and on mains; the verdict by that rule; a profile split by module after; the
  goldens unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `scripts/frame-bench-browser.mjs`
