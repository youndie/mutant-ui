---
id: B-73
title: "The sample's frame cost in the browser (wasm)"
status: done
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
  - **Amended 2026-11-15, after a smoke run of the harness and before the first counted run.** The smoke run had two rounds of 3 s on four scenes and is not reported. It found the resting page at 22 frames a second, with work p50 near 60 ms. There a 10 ms busy-wait takes its place in a frame already late: the control read −36 ms. A control must stand on a page that keeps its frames. **The control is the reduce-motion page with the busy-wait**: its clock stops, so its frames are only the recorder's. Its work over that page's must read 10 ms. The resting page with the busy-wait is still run and reported (`ControlRest`), not as a condition. The same run showed the pooled summary counting intervals over 16.7 ms from the log's rounded values, where an on-time frame stands at 16.7. It counts them unrounded now, as each round's line and B-72 do. No threshold moved.
  - **The JVM.** `FrameBench` runs the same scenes at the same commit on the same day, for the ratio of each scene's work p50 and p95 to the JVM's. It is not the same run: the two cannot share one.
  - **Then.** A Performance trace of each scene not green, and an item per cause with its own number.
- AC: raw logs under `logs/b-73/`, the control's reading, a verdict per scene, its ratio to the JVM's; for each scene not green, the trace and an item per cause.
- Anchors: `.github/workflows/pages.yaml`, `sample/build.gradle.kts`

## Results, 2026-11-16

Run on the stand above, on mains power, from the build of `ab88d77` (the sample and the kit unchanged through `e1dcaf8`, the harness's commit in the log's header). Five counted rounds of ten seconds a scene: [the log](../../logs/b-73/2026-11-15-browser.log) and [every frame](../../logs/b-73/2026-11-15-browser/frames.csv). The JVM ran next, at `4f04f96`, which changes nothing in the sample or the kit: [the log](../../logs/b-73/2026-11-16-desktop.log). Same host, not the same run.

**The control reads the busy-wait.** On the idle page its work over the page's own, at p50 per counted round: 9.80, 9.90, 9.70, 9.80 and 9.70 ms against 10 ms. On the resting page (`ControlRest`, reported, not a condition) it read −39 to −41 ms, as the smoke run had found: the page is saturated, and a wait in the frame moves the rest of the work later. **Load was delivered.** The wheel scrolled 19 164–20 154 px a round. The harness sent 100–104 notches a round, not the 300 it asks for: each notch waits for the busy page to take it.

| Scene | frames/s | interval p95 | > 16.7 ms | > 33 ms | work p50 | work p95 | Verdict | JVM: frames/s, work p50 / p95 |
|---|---|---|---|---|---|---|---|---|
| Components, at rest | 24.3 | 100.0 | 75.9 % | 53.4 % | 60.6 | 117.1 | **RED** | 59.8, 0.03 / 6.29 |
| Components, wheel | 26.0 | 100.0 | 67.9 % | 35.6 % | 12.5 | 30.7 | **RED** | 59.9, 0.26 / 6.26 |
| Components, «Чистый сигнал» | 24.8 | 100.0 | 78.6 % | 57.7 % | 62.3 | 112.8 | **RED** | 60.2, 0.03 / 6.01 |
| Components, reduce motion | 60.5 | 16.8 | 45.4 % | 0.00 % | 2.3 | 2.6 | GREY | idle |
| Demo page (three screens) | 60.6 | 16.7 | 45.7 % | 0.00 % | 12.0 | 14.7 | GREY | 60.1, 0.02 / 0.16 |
| Demo, Library alone | 48.2 | 33.4 | 56.2 % | 16.4 % | 3.4 | 46.2 | **RED** | 60.0, 0.03 / 2.25 |
| Demo, Recording alone | 60.0 | 16.8 | 47.4 % | 0.93 % | 3.3 | 32.3 | GREY | 60.2, 0.03 / 1.26 |
| Demo, Settings alone | 60.5 | 16.8 | 44.9 % | 0.00 % | 3.8 | 9.6 | GREY | 60.3, 0.04 / 0.32 |

Work and interval are in ms.

**The ratio to the JVM is not a ratio of the same work.** At work p95, the browser's frame is 19 times the JVM's on the resting Components page (117.1 against 6.29), 5 on the wheel, 19 with the clean signal and 21 on the Library screen. At p50 the JVM's main thread does almost nothing (0.02–0.26 ms). Its tick is built on other threads (B-75), and its frame is drawn on Skia's render thread (B-76), and the JVM's *work* times neither. The browser has one thread for all three. The two *works* are also read differently: the JVM's is Compose's `onRender`, the browser's the frame's span up to the first task after it.

**Green is out of reach of either recorder by this rule.** On a page at 60 frames a second, 45 % of the browser's intervals and 22–28 % of the JVM's stand over 16.7 ms by the timer's jitter around 16.67 ms. B-72 counted its intervals the same way; the rule is kept as it was registered, and this is its limitation.

**Where the time goes**, sampled by the CDP profiler every 100 µs over 8 s. The Kotlin module was rebuilt by the same `wasm-opt` with `-g` to keep its names: the code is the same 3.48 MB, and the name section adds 1.41 MB. Logs: [Components at rest](../../logs/b-73/2026-11-16-profile-ComponentsRest-named.txt), [Library alone](../../logs/b-73/2026-11-16-profile-DemoLibrary-named.txt); the unnamed runs of the production build itself rank the same functions.

1. **The bodies' ticks have one thread, and the Components page needs more than it.** B-75 builds a resting body's next tick ahead on `Dispatchers.Default`, and B-82 rasterises it there. On the JVM those are other threads. In the browser they are the page's own message queue, between frames.
   - **On the resting Components page the main thread is busy 100 % of the time.** `gen.frame` is anywhere on the stack 50.5 % of the time. The builds ahead, in the queue, take 28.3 %. The bodies' draw takes 45.6 %, building in the frame the ticks whose build ahead had not run by then (`FrameAhead.take` finds none, and the next `prepare` cancels it). One thread cannot keep up. On the JVM the same page's tick cost 60–80 ms on the main thread before B-75 moved it (B-72's profile), and it has eight cores. Within the generator, `gen.frame` takes 13.9 % of self time, `march` 6.1 %, and the arrays' accessors around them 13 % more.
   - **On the Library screen the thread is busy 42 % of the time.** 54.2 % of that is `FrameAhead.buildAhead` in the queue, and 33.1 % the bodies' vectors drawn in the frame. Each tick's builds land together between two frames, so one frame in six waits past 33 ms.
   - The clean signal changes nothing (the Tape is not the cost). Why the Demo page, which holds the Library screen, keeps its frames while the screen alone does not, this run does not say.
   → [B-87](B-87-ticks-on-the-one-thread.md)

The JVM's fixes (B-75, B-76, B-82) moved the work off the frame. In the browser there is no other thread to move it to.
