---
id: B-107
title: "The sample stutters in a phone's browser"
status: question
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

## Amendment, 2026-11-17, after the first numbers and before the paired run

- **What the first runs showed of the stand.** The build before the fix and the build after it, run one after the other
  on the phone proxy, moved the idle page too: `ComponentsReduce`, where the clock stands and the generator builds
  nothing, read work p50 36.0 ms in the first run and 47.4 ms in the second, 31 % slower, the Mac on battery, 52 % then
  42 %, and fanless. Its drift is the same size as any effect sought, so two runs one after the other compare two
  machines ([before](../../logs/b-107/2026-11-17-pair-before.log), [after](../../logs/b-107/2026-11-17-pair-after.log);
  reported, not counted).
- **The control read out of its band.** Over `ComponentsReduce` it read 11.1–12.2 ms in the first run and 8.5–13.5 in
  the next, against 10 ± 1: the 4x throttling stretches the busy-wait itself. By the rule above those runs are void;
  the rule stays, and the verdicts per scene of these runs are not claimed.
- **The paired protocol.** A build is compared with another in one run: the harness serves both and runs each scene
  from each build in the same rotation (`DIST_B`), five rounds, round 1 not counted, so the machine's drift falls on
  both alike. Scenes: `ComponentsRest`, `DemoPage` and `ComponentsReduce`, the last as the ruler of drift: the fix
  cannot touch it.
- **The rule for a fix.** It counts on a page when every counted round's work p50 of the new build minus the old is
  negative and the mean difference is more than twice the spread (max − min) of the same difference on
  `ComponentsReduce`. Otherwise it is below resolution, reported with that spread. No threshold above moved.

## Findings, 2026-11-17

Logs: [the first run](../../logs/b-107/2026-11-17-before.log), the two runs one after the other
([before](../../logs/b-107/2026-11-17-pair-before.log), [after](../../logs/b-107/2026-11-17-pair-after.log)), [the paired
run](../../logs/b-107/2026-11-17-paired.log), and the profiles of the resting Components page
([before](../../logs/b-107/2026-11-17-profile-ComponentsRest-named.txt),
[after](../../logs/b-107/2026-11-17-profile-ComponentsRest-after-named.txt)). The per-frame CSVs were not kept.
Every run's control read outside 10 ± 1 ms, so no verdict per scene is claimed; the numbers below are reported, with
that limit.

- **On the phone proxy the sample is nowhere near its frames.** Every page with motion ran at 5–6.6 frames a second,
  its main thread working 155–220 ms a frame; the idle page (reduce motion) ran at 25–34 with 36–47 ms. A phone is not
  4 times an M1 on every count, and the owner saw a stutter, not six frames a second, so the proxy overstates the
  phone; the order of the costs is what it shows.
- **The Tape: below resolution at the median, and it shows at the tail.** Work p50 of the resting Components page
  minus the clean one, per counted round of the first run: −3.4, 11.0, 9.0, 14.9 ms (not all positive), spread of the
  clean page 4.5 ms; on the Demo page 6.7, −1.5, −0.7, 0.9 ms. By the rule it is below resolution. Its p95 is not: 326
  against 196 ms on the Components page, 470 against 183 on the Demo page. The frames the Tape steps on are the longest
  ones, which is what a stutter is; the rule was written for the median and is kept.
- **Where the time goes.** Sampled every 100 µs over 8 s on the resting Components page: the body generator
  (`gen.frame`) is on the stack 59 % of the time. Its self time showed Kotlin/Wasm's own `sin` (a port of fdlibm in the
  module) at 40 %: argument reduction 19.8 %, `sin` 9.7 %, the kernels 10.4 %.
- **Refuted: the browser's `Math.sin` in its place.** The generator's 16 calls went through the browser's `Math.sin` and
  `Math.cos`, imported as they are. Every generator and transition golden path stayed byte for byte on wasm (checked
  with the path comparison made exact for the run). fdlibm left the profile, and its time reappeared in its callers
  (`valueNoise` 4.2 → 18.2 % self). The paired run: new minus old work p50 per counted round, Components +39.8, −12.1,
  +15.6, −5.8 ms; Demo −6.4, +1.2, −2.8, −16.4 ms; the idle page's drift −1.6…+1.7 ms. Below resolution: the change was
  taken out. The browser's `sin` also differs from Kotlin's in the last bit for 13 430 of 408 012 arguments swept, so
  it would not have been free to keep.
- **The stand learned two things.** Run one after the other, the same Mac on battery drifted 31 % on the idle page
  between two runs; the harness now interleaves two builds in one run (`DIST_B`) and the profiler and the bench both
  take the phone proxy (`PHONE=1`).

## Question for the owner

The cost is the generator itself, built for every visible body on its tick, and on a phone's thread it does not fit.
What it would take is a decision about the kit, not a fix found by a profile:

1. **The Tape at the tail.** Draw its scanlines as one repeating fill and record it once per tick, so the frames it
   steps on stop being the longest (a measured item against the p95 above).
2. **A phone's budget for the boil.** Fewer bodies boiling at once, or a slower resting tick, under a narrow window or
   a coarse pointer, as toon-ui's idle budget does; it changes what a phone shows.
3. **Leave it** until a real phone can be measured.

Which of these, if any?
