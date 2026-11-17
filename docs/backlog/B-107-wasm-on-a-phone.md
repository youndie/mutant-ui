---
id: B-107
title: "The sample stutters in a phone's browser"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-107 — The sample stutters in a phone's browser

The owner, 2026-11-17: «wasm-версия на телефоне подлагивает — надо ещё пробовать оптимизировать (возможно, эффект
плёнки)».

B-73 measured the browser on the Mac at 1280 × 814 at 2x, and B-87 took every scene from red to grey there. A phone has
a narrower screen at 3x and a slower main thread. B-73 found the Tape costing nothing on the Mac («Чистый сигнал»
changed nothing); on a phone it is three times the pixels per point and a slower thread, so the owner's suspicion is
measured, not assumed.

- **The decision and its reason.** Measure on a phone proxy in B-73's harness, find the top cost by a profile, fix it,
  and measure again by the same protocol. The owner's phone is not on this stand; the proxy is the browser's own
  emulation, and its limits are written beside every number.
- Rejected: optimising the Tape blind. B-73's last profile put the Tape out of the top costs on the Mac.

- **Pre-registered on 2026-11-17, before the first number.**
  - **Stand.** B-73's: MacBook Air M1, macOS, on mains power, logged each run; Google Chrome headless over CDP; the
    production wasm build at the commit in the log's header, its digest logged.
  - **The phone proxy** (`PHONE=1`): a 390 × 844 viewport at 3x, emulated as mobile with touch, and the main thread
    slowed 4 times by Chrome's CPU throttling. The GPU is the Mac's, so a cost that is the GPU's fill is not seen;
    what is seen is the main thread's.
  - **Scenes.** `ComponentsRest`, `ComponentsClean` (the Tape off), `DemoPage`, `DemoPageClean` (the Tape off on every
    phone), `ComponentsReduce`, and the control.
  - **Protocol.** Five rounds of 8 s a scene, the order rotating, round 1 not counted, each scene's first second not
    counted. Each scene loads afresh.
  - **Measures.** B-73's: the interval between frames and the main thread's work per frame; long animation frames.
  - **Control.** `ComponentsReduce` with 10 ms of busy-waiting in every frame: its work p50 over the reduce page's must
    read 10 ± 1 ms in every counted round, or the run is void.
  - **Verdict per scene.** B-73's rules: green when work p95 ≤ 8 ms and intervals over 16.7 ms under 1 %; red when work
    p50 is over 16.7 ms or intervals over 33 ms exceed 5 %; grey otherwise.
  - **Does the Tape cost.** Per counted round, the work p50 of `ComponentsRest` minus `ComponentsClean`, and of
    `DemoPage` minus `DemoPageClean`. The Tape costs on a page when every counted round's difference is positive and
    their mean is more than twice the spread (max − min) of the clean scene's own per-round work p50. Otherwise it is
    below resolution, reported with that spread.
  - **Then.** A CDP profile of the worst scene; a fix of its top cost; the same protocol before and after, on the same
    day and stand, the build's digest in each log.
- AC: raw logs under `logs/b-107/`; the control's reading; a verdict per scene; the Tape's answer per page; the
  profile; the fix with its before and after.
- Anchors: `scripts/frame-bench-browser.mjs`, `sample/src/wasmJsMain/kotlin/io/github/youndie/mutant/sample/BenchScene.kt`
