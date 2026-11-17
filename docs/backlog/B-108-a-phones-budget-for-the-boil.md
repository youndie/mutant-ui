---
id: B-108
title: "What the browser costs against Android, on one phone"
status: done
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

## Findings, 2026-11-17

Logs: [natively](../../logs/b-108/2026-11-17-native/), [in Chrome](../../logs/b-108/2026-11-17-chrome.log), the
profiles of [`TenCards8`](../../logs/b-108/2026-11-17-profile-TenCards8.txt) and of
[the resting Components page](../../logs/b-108/2026-11-17-profile-ComponentsRest.txt). Same phone, same commit
(361b39f), one after the other; battery at 100 %, on mains, 27.7 °C before the native run, 28.4 before Chrome, 31.0
after. Rounds 2–5 counted. The first attempt died when the phone slept and adb lost it; nothing of it was kept.

**The same ten cards, on one phone:**

| | Natively | In Chrome |
|---|---|---|
| Frames a second | 59 (473 in 8 s, every round) | 22.3 |
| Main thread a frame, p50 / p95 | 0.38 / 3.37 ms (animation + layout + draw) | 36.4 / 101.4 ms (work) |
| Render thread a frame, p50 / p95 | 3.76 / 17.1 ms (sync + command issue) | — the page has none |
| Bodies rasterised ahead | 76 a second, 12.9 ms each at p50, on `Dispatchers.Default`: about one core | the same work, on the main thread |

- **Natively the main thread does almost nothing.** The bodies are built and rasterised ahead on background threads
  (about 980 ms of work a second) and the render thread issues the frame; the main thread's share is under half a
  millisecond. Summed over threads that is about 21 ms of CPU a frame at 59 frames a second.
- **In the browser all of it is one thread**, and at 22 frames a second it is busy 36 ms a frame, about 80 % of the
  thread. Per frame that is about 1.7 times the CPU Android spends over all its threads (a rough sum: the two sides
  are not measured by one instrument). The gap in frames a second is the threads more than the code.
- **Where Chrome's main thread goes**, self time by module:

| | Skia (skiko's module) | the app's Kotlin | the browser (WebGL, the engine) | page JS | GC | other |
|---|---|---|---|---|---|---|
| `TenCards8` | **64.9 %** | 28.7 % | 3.4 % | 1.2 % | 1.1 % | 0.7 % |
| Components, resting | 41.5 % | 41.2 % | 4.8 % | 4.0 % | 3.8 % | 4.7 % |

  The WebGL calls themselves are a few per cent; Skia's own CPU work is the largest share, the Kotlin side (Compose and
  the generator) the second. That the Skia share is mostly the software raster of the bodies' images is the likely
  reading, not a measured one: the kit rasterises a resting body into an `ImageBitmap` once per tick (B-82), and in the
  browser an `ImageBitmap` is a CPU raster. That is the route worth measuring next, [B-109](B-109-bodies-as-vectors-in-the-browser.md).
- **A correction to B-107's phone numbers.** The resting Components page read work p50 18.1 ms here (45.9 frames a
  second; 8.4 ms in its first two rounds, 20–23 in the last two) against 116.6 ms in B-107's device run earlier the
  same day, with the code changed only by B-107's own Tape fixes, which moved it by about 6 ms. B-107's runs were
  taken with the battery at 33–58 % and charging, and the phone warming from 31 to 41 °C; this one at 100 %, 28–31 °C.
  The phone's state, not the code, explains most of it. B-107's comparisons were paired within one run and stand; its
  absolute frame rates are those of a throttled phone and are not what this phone does when cool.

## Answer to the owner

- **Why the browser is slower.** Not Compose's code: on Android this kit's work runs on three kinds of thread (main,
  render, background builds), and the main thread is nearly idle; in the browser Kotlin/Wasm has one thread, so the
  bodies' builds and raster, Skia's CPU work and the frame all queue on it. JetBrains' comparison is of the code's
  speed and of ordinary interfaces; this kit's work is mostly geometry and raster that Android takes off the main
  thread. The per-frame CPU is within about 2× of Android's; the threads are not.
- **Can WebGL help.** Skia already draws the page through WebGL, and those calls are a few per cent. What can move is
  the CPU raster: drawing a resting body as vectors each frame, which Skia's WebGL backend fills on the GPU, instead of
  a CPU image per tick. That is B-109. A worker thread is not open to Kotlin/Wasm's Compose today (no shared memory);
  WebGPU is not a Compose web backend.
