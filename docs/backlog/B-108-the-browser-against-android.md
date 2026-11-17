---
id: B-108
title: "What the browser costs against Android, on one phone"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-108 — What the browser costs against Android, on one phone

Found by [B-107](B-107-wasm-on-a-phone.md): on a Pixel 6a, in Chrome, the sample's pages with motion run at 8–11
frames a second, while bench-android's ten boiling cards run there natively with no vsync skipped (B-89). It was first
filed as a question, a phone's budget for the boil.

- **The owner, 2026-11-17:** «урезать я не хочу пока ничего по рендеру»; and why is Compose on wasm slower, when
  JetBrains rate Compose Multiplatform's speed comparable; can it go faster through WebGL or the like in the browser.
  So no budget: nothing a phone shows is cut. The item becomes a measurement that answers where the browser's time goes
  against Android's on the same work, and which browser-side routes those numbers leave open.
- Rejected: a budget for the boil (the owner's word above); guessing the browser's cost from a different scene, as the
  comparison so far did (Components in Chrome against ten cards natively).

- **Pre-registered on 2026-11-17, before the first number.**
  - **Work.** `TenCards8`, bench-android's scene: ten cards under the Tape on an 8-tick clock, a layer reading the frame
    time in its draw so every vsync draws. The wasm sample gains the same scene (`?scene=TenCards8`), composed alike
    through the public API (the theme's own clock is 8 ticks a second).
  - **Stand.** The Pixel 6a over USB, on mains power. Natively: `scripts/device-bench.sh screens` (release build,
    speed-profile compiled), its per-frame `FrameMetrics`. In Chrome: `DEVICE=1 scripts/frame-bench-browser.mjs`, the
    production wasm build. Both at this branch's commit, run one after the other on the same day; the phone's
    temperature in each log.
  - **Protocol.** Five rounds of 8 s, round 1 not counted, as both harnesses already count.
  - **Measures.** Natively: the main thread per frame, animation + layout + draw, and the render thread's sync +
    command issue beside it; vsyncs skipped. In Chrome: the main thread's work per frame and the frames a second.
    Reported as p50 and p95 per side and the ratio of the p50s. The browser has no render thread of its own for the
    page: its main thread carries what Android splits, and that asymmetry is what is measured, not corrected for.
  - **Where Chrome's main thread goes.** A sampled profile (every 100 µs, 8 s) of `TenCards8` and of the resting
    Components page on the phone, its self time split by where the code lives: the app's Kotlin module, Skia's module
    (skiko), the page's JavaScript, the browser itself (WebGL, the engine), the garbage collector.
  - **No verdicts.** This item answers the owner's question with numbers; a route chosen from them is an item of its
    own, with its own thresholds.
- AC: raw logs under `logs/b-108/`; both sides' per-frame numbers on the same scene; the split by module; the answer
  to the owner's question.
- Anchors: `scripts/frame-bench-browser.mjs`, `scripts/frame-profile-browser.mjs`, `scripts/device-bench.sh`,
  `sample/src/wasmJsMain/kotlin/io/github/youndie/mutant/sample/BenchScene.kt`
