---
id: B-72
title: "The sample's frame cost on the JVM, in a real window, and where it goes"
status: open
priority: P1
size: M
stage: stage-5-product
blocked_by: [B-41]
---

# B-72 — The sample's frame cost on the JVM, in a real window, and where it goes

The owner, 2026-11-14: the sample in the browser stutters to the eye. Before the browser and before the phone ([B-49](B-49-frame-cost-on-a-pixel-6a.md)), measure the JVM: if it stutters there too, it is the kit's cost, and a fix is found and checked there first, where profiling is cheapest. The order the owner set: the JVM, the fix, wasm ([B-73](B-73-frame-cost-in-the-browser.md)), Android (B-49). [B-06](B-06-frame-cost.md) measured one body's field off screen; nothing yet measured a screen of them with the Tape in a window.

- **The decision and its reason.** A recorder in a throwaway host opens the sample in a real `ComposeWindow` on this Mac and reads, per frame, the interval between frames and the main thread's work (from the first `withFrameNanos` callback of a frame to a draw placed last in it, as plakat-ui's B-51 did). Scenes: the Components page at rest; the Components page scrolled through by the wheel; each Demo screen; the Components page with the clean signal (no Tape); and with reduce motion (no clock). Ten seconds a scene, six rounds with the scene order rotating, round 1 discarded. A positive control busy-waits 10 ms inside the measured frame, and the recorder must read it.
- **Pre-registered, before the first number.** Stand: MacBook Air M1 (8 cores, 16 GB), macOS 27.0.1, JDK 25, the window at 1280 × 900. Per scene: **green** when p95 of the work is ≤ 8 ms and frames over 16.7 ms are under 1 %; **red** when p50 of the work is over 16.7 ms (the window cannot hold 60 fps) or frames over 33 ms exceed 5 %; anything else is grey. A desktop number cannot clear the phone, only fail it in advance.
- **Then the fix, on the JVM.** For any scene not green, a profile of the main thread (sampling) names where the time goes; each cause becomes an item with its own number before and after. Rejected: optimising by guess before a profile.

- AC: raw logs under `logs/b-72/` with the stand in their header; the control's reading; a verdict per scene; for each scene not green, the profile and an item per cause.
- Anchors: `sample/src/desktopMain/kotlin/io/github/youndie/mutant/sample/Main.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
