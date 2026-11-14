---
id: B-72
title: "The sample's frame cost on the JVM, in a real window, and where it goes"
status: done
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

- **Amended 2026-11-14, before the first number.** *Work* is read around Compose's whole frame — the window's `SkikoRenderDelegate.onRender`, which holds the frame callbacks, recomposition, layout and the draw — a superset of the span named above, so it can only read longer; beside it, the AWT event that carries the frame (`onRender` plus Skia's flush and the Metal submit, where they run on the event thread), not a verdict. *A frame over 16.7 ms or 33 ms* is the interval between two frames' starts. Each scene's first second (the switch's own composition) is not counted. The Demo page is a scene as the user sees it (three screens side by side), and each screen alone is one more. Load delivered is read every round: the wheel scene's scroll distance, the frame count of each scene. The window is kept on top so the system does not throttle an occluded one.
- AC: raw logs under `logs/b-72/` with the stand in their header; the control's reading; a verdict per scene; for each scene not green, the profile and an item per cause.
- Anchors: `sample/src/desktopMain/kotlin/io/github/youndie/mutant/sample/Main.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Results, 2026-11-14

Run at `7dc98a6` on the stand above, except the window: 1280 × 900 does not fit this display (1440 × 900 at scale 2), so the window stood 1280 × 814. Metal, 60 Hz. Five counted rounds of ten seconds a scene, 19 148 frames: [the log](../../logs/b-72/2026-11-14-desktop.log) and [every frame](../../logs/b-72/2026-11-14-desktop/frames.csv).

**The control reads the busy-wait.** Its work over the resting page's, at p50 per counted round: 10.04, 10.04, 10.05, 10.04 and 10.23 ms against 10 ms. **Load was delivered.** The wheel scrolled 9 370–10 744 px a round, and every scene asked for frames except under reduce motion, as it should.

| Scene | frames/s | interval p95 | > 16.7 ms | > 33 ms | work p50 | work p95 | Verdict |
|---|---|---|---|---|---|---|---|
| Components, at rest | 31.5 | 85.0 | 47.9 % | 26.9 % | 0.08 | 66.8 | **RED** |
| Components, wheel | 29.2 | 89.0 | 53.0 % | 28.9 % | 0.55 | 67.4 | **RED** |
| Components, «Чистый сигнал» | 33.4 | 84.2 | 47.0 % | 24.8 % | 0.09 | 69.2 | **RED** |
| Components, reduce motion | — | — | — | — | — | — | idle (see below) |
| Demo page (three screens) | 34.5 | 44.6 | 100 % | 12.4 % | 0.09 | 1.9 | **RED** |
| Demo, Library alone | 53.1 | 35.4 | 44.0 % | 8.8 % | 0.08 | 14.0 | **RED** |
| Demo, Recording alone | 59.0 | 22.7 | 43.1 % | 1.1 % | 0.08 | 10.2 | GREY |
| Demo, Settings alone | 59.6 | 20.0 | 40.0 % | 0.6 % | 0.08 | 1.7 | GREY |

Work and interval are in ms. Same host and same run for every row.

Under reduce motion the clock stops and the page asks for no frames: one frame in fifty seconds, 57 ms. Read literally, the red rule's p50 of one sample is over 16.7 ms. That is not a stream of frames, and it is reported as idle rather than as a verdict.

**Where the time goes**, sampled by JFR, at 2 × 12 s a scene ([Components](../../logs/b-72/2026-11-14-profile-components-rest.txt), [Demo](../../logs/b-72/2026-11-14-profile-demo-page.txt)):

1. **The generator's tick runs in the frame, on the main thread.** The page's work is two-humped: 0.1 ms between ticks, 60–80 ms on the frame a tick falls on, eight times a second. On the resting page, 77 % of the event thread's samples are in `gen.frame`, called from the surface's draw: 58 % in `distanceField`, 15 % in `march`. Every body rebuilds its field and outline on every tick. [B-50](B-50-first-frame-off-the-frame.md) moved the first build off the frame, not the ticks. → [B-75](B-75-tick-frames-off-the-main-thread.md)
2. **The Demo page is bound by Skia's render thread, which this recorder does not time.** The main thread is nearly idle (work p95 1.9 ms), yet frames come every 27 ms. Skiko replays the recorded picture on its own thread (`skiko-dispatcher-to-block-on`), where 898 of 1 050 native samples are in `Canvas.drawPicture` and 137 in the flush. That is more than twice the resting Components page's 462 over the same time. → [B-76](B-76-render-thread-replay-on-the-demo-page.md)

The desktop stutters as the browser does to the eye. The owner's order holds: the fixes are made and measured here first, and [B-73](B-73-frame-cost-in-the-browser.md) comes after them.
