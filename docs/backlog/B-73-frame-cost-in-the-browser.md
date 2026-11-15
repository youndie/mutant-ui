---
id: B-73
title: "The sample's frame cost in the browser (wasm)"
status: wip
priority: P3
size: M
stage: stage-5-product
blocked_by: [B-72, B-75, B-76, B-78, B-82, B-83, B-30, B-31, B-32, B-33, B-34, B-35, B-36, B-44, B-45, B-46, B-47]
---

# B-73 — The sample's frame cost in the browser (wasm)

**Moved to the end by the owner on 2026-11-15**: the JVM's frame cost is acceptable after B-75 and B-82, and the kit's components come first. Blocked by the components still to build, so it is picked after them.

The owner, 2026-11-14: the published sample stutters in the browser to the eye. Measured after the JVM ([B-72](B-72-frame-cost-on-the-jvm.md)), so what is the kit's own cost is found and fixed there first, and what remains here is the browser's.

- **The decision and its reason.** The same scenes as B-72, in the production wasm build, in Chrome driven over CDP (headless, as the references are: a hidden pane draws no frames). Per frame, the interval between `requestAnimationFrame` callbacks and the long tasks the page reports; the Performance trace for any scene not green. The same positive control (a 10 ms busy-wait in a frame). Thresholds written in this item before its first number, against B-72's numbers for the same scene.
- Rejected: measuring the browser first, where a slow frame could be the kit, Skia's wasm build or the browser, and the cheapest of the three to profile is not there.

- **Pre-registered on 2026-11-15, before the first number.**
  - **Stand.** MacBook Air M1 (8 cores, 16 GB), macOS 27.0.1, on mains power, read and logged each run. Google Chrome 157, `--headless=new`. The page is 1280 × 814 at 2×, as B-72's window stood. Headless draws through the GPU here: `SystemInfo.getInfo` reads ANGLE on Metal, Apple M1, with WebGL and GPU compositing enabled. The build is `:sample:wasmJsBrowserDistribution` at the commit in the log's header, served from `127.0.0.1` with its `.wasm` digest logged.
  - **Scenes.** B-72's own: the Components page at rest, scrolled by the wheel, with «Чистый сигнал», with reduce motion; the Demo page; each Demo screen alone. The sample opens each by `?scene=<name>`, composed as B-72's recorder composes it. The wheel sends a 100 px notch every 32 ms, turning every 2.5 s, and the page reports the distance it scrolled (`window.__scrolled`) as the load delivered.
  - **Protocol.** Ten seconds a scene, six rounds, the order rotating, round 1 not counted, each scene's first second not counted. Each scene loads afresh.
  - **The measures.** *Interval*: between two `requestAnimationFrame` timestamps. *Work*: the main thread's frame span, from the frame's timestamp to the first task after its rendering update (a message posted from the frame's callback). Long animation frames (≥ 50 ms) are counted from the page's own observer.
  - **Control.** The resting page with 10 ms of busy-waiting in every frame's callback. Its work over the resting page's must read 10 ms.
  - **Verdicts.** B-72's rules per scene, pooled over the counted rounds. **Green** when work p95 is ≤ 8 ms and intervals over 16.7 ms are under 1 %. **Red** when work p50 is over 16.7 ms, or intervals over 33 ms exceed 5 %. Anything else is grey.
  - **The JVM.** `FrameBench` runs the same scenes at the same commit on the same day, for the ratio of each scene's work p50 and p95 to the JVM's. It is not the same run: the two cannot share one.
  - **Then.** A Performance trace of each scene not green, and an item per cause with its own number.
- AC: raw logs under `logs/b-73/`, the control's reading, a verdict per scene, its ratio to the JVM's; for each scene not green, the trace and an item per cause.
- Anchors: `.github/workflows/pages.yaml`, `sample/build.gradle.kts`
