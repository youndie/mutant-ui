---
id: B-107
title: "The sample stutters in a phone's browser"
status: done
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

## Amendment, 2026-11-17, a real phone, before its first number

- **The owner attached a phone** after the question below: a Pixel 6a over USB, Android 17, Chrome 157, on mains power,
  the screen kept on while plugged in. Option 3 of the question becomes measurable, so the device runs first.
- **Stand.** The same harness with `DEVICE=1`: the production wasm build of this branch rebased on `main` after B-103…B-106,
  served from the Mac through `adb reverse`, opened as a new tab of the phone's own Chrome over its DevTools socket;
  nothing emulated, nothing throttled. The device, its build, its battery and temperature, the browser and its viewport
  go into each log's header.
- **Scenes.** The pre-registered ones (`ComponentsRest`, `ComponentsClean`, `DemoPage`, `DemoPageClean`,
  `ComponentsReduce` and the control), with `ControlRest` and the other Demo tabs reported beside. The wheel scene is
  left out: a phone scrolls by touch.
- **Protocol, measures, control, verdict per scene and the Tape's rule: as pre-registered above, unchanged.** Six rounds
  of 8 s a scene, round 1 not counted. On the device the busy-wait is not stretched by throttling, so the control's band
  of 10 ± 1 ms applies as written; a run where it reads outside is void.
- **A fix found on the phone** is measured on the phone by the paired protocol and the rule for a fix of the amendment
  above, unchanged: five rounds, `ComponentsRest`, `DemoPage` and `ComponentsReduce` from both builds in one rotation.
  Written after the device run and its profile, before the paired run.

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

## Findings on the Pixel 6a, 2026-11-17

Logs: [the device run](../../logs/b-107/2026-11-17-pixel6a.log), its profiles of the resting Components page with the
Tape ([on](../../logs/b-107/2026-11-17-pixel6a-profile-ComponentsRest-named.txt),
[off](../../logs/b-107/2026-11-17-pixel6a-profile-ComponentsClean-named.txt); the build with names is the one before
B-103…B-106, the code they profile is the same), and [the paired run](../../logs/b-107/2026-11-17-pixel6a-paired.log).
The sample gets 411 × 784 at 2.625x. The phone warmed from 31.3 °C to 39.9 °C over the first run, and its numbers
drift with it: the first counted round ran faster than the four after it on every page.

- **The control read out of its band again**: 8.1–18.9 ms over the reduce page against 10 ± 1, on the phone as well,
  so the run is void by the rule and no verdict per scene is claimed. The busy-wait is not stretched on the phone; the
  reduce page itself is not idle there (49.8 frames a second, work p50 22.8 ms), and a frame of 10 ms more lands on a
  thread already past its budget.
- **On the phone the sample runs at 8–11 frames a second** on the pages with motion (Components 8.8, Demo 10.7, its
  library tab 8.3, settings 60.5), work p50 116–131 ms. The proxy's order held, its scale did not.
- **The Tape: below resolution by the rule, and it costs.** Work p50 of the resting Components page minus the clean one,
  per counted round: 92.1, 9.5, 8.7, 9.8, 8.0 ms, all positive, mean 25.6 against twice the clean page's spread of
  73.7 (the warm-up between the first counted round and the rest). On the Demo page: −65.7, 10.0, 7.5, 12.7, 7.7. In
  the four rounds after the warm-up it is 8–13 ms a frame on both pages, its p95 218–340 ms against 128–196 clean,
  and the clean Components page runs at 10–12 frames a second against 8–9.5. The rule is kept and its verdict stands;
  the numbers are reported beside it.
- **What the profile shows on the phone**, against the proxy: the body generator is 19–20 % of the main thread, not
  59 %; drawing is 77 % (`CanvasRenderer.needRedraw`). The Tape's draw is 13.1 % of the resting Components page
  (`DrawBackgroundModifier.draw`, absent with the Tape off). The cause in the code: B-77 fills the grain from a tile
  only at a whole density, and at 2.625 a tile is 236.25 × 472.5 px, so every frame drew about 1 600 strokes and 36
  clips. The proxy ran at 3x and never took that path.
- **The fix**: at a fractional density the fill repeats the fewest tiles whose span is whole pixels, four across by
  two down at 2.625 (945 × 945 px). `TapeGrainTileTest`: the block exists at 2.625, and at ticks whose offset is whole
  pixels it draws the strokes' own pixels, 0 off in both themes; at other ticks about 0.5 % of a 640 × 400 dp frame's
  pixels differ (up to 18 of 255 on a channel), the strokes moved by a part of a pixel, the frame's ink equal to
  0.001 %.
- **The fix, paired on the phone: below resolution.** New minus old work p50 per counted round, Components −6.1,
  −13.5, +6.3, +2.2 ms; Demo −4.1, −5.1, −7.4, −9.4 ms, all negative, mean −6.5 against twice the ruler's spread of
  5.7. A limit of the ruler: the reduce page draws the Tape too, so this fix reaches it (−2.0…−7.7 ms) and it is not
  the independent ruler it was for the `sin` change. The frame rate moved 8.8 → 9.2 on Components and 7.8 → 8.8 on
  Demo.

## Amendment, 2026-11-17, the scanlines too, before their paired run

- **The owner chose option 1** of the question below: the scanlines as a fill too. Done (`scanlineFill`, held by
  `TapeScanlineFillTest`: 0 pixels off against the rects at density 1, 2, 3 and 2.625, the band passing and away).
- **The ruler, late and narrow.** The rule for a fix named `ComponentsReduce` as the ruler because the `sin` change
  could not touch it. A Tape fix does: the reduce page draws the Tape, and the grain's paired run moved it −2.0…−7.7 ms.
  For the Tape's fix the ruler is `ComponentsClean`, the same page under «Чистый сигнал», which draws no Tape;
  `ComponentsReduce` is reported beside. Declared before the run it governs; nothing else moves.
- **The run.** The build before both Tape fixes against the build with both, on the phone, the paired protocol: five
  rounds, `ComponentsRest`, `DemoPage`, `ComponentsClean`, `ComponentsReduce`.

## Findings: both Tape fixes on the Pixel 6a, 2026-11-17

[The paired run](../../logs/b-107/2026-11-17-pixel6a-paired-tape.log): the build before both fixes against the build
with both, five rounds, round 1 not counted. New minus old work p50 per counted round:

| Page | Per counted round, ms | Mean | Verdict |
|---|---|---|---|
| `ComponentsRest` | −15.1, +0.2, −8.1, −1.9 | −6.2 | below resolution: not every round negative |
| `DemoPage` | −5.8, −5.7, −8.7, −1.4 | −5.4 | below resolution: all negative, under twice the ruler's spread of 10.6 |
| `ComponentsClean` (the ruler, no Tape) | −2.7, +7.9, +2.2, +1.4 | +2.2 | — |
| `ComponentsReduce` (the Tape drawn every frame, the clock still) | −9.5, −10.0, −8.9, −7.7 | −9.0 | distinguishable: all negative, more than twice the ruler's spread; 26.8 → 17.4 ms |

- The frame rate moved 9.0 → 9.8 on Components and 7.8 → 8.8 on Demo. Pooled, the resting Components page now works
  107.9 ms a frame against the clean one's 105.9 (before: 114.0), so what the Tape still costs there is about 2 ms
  against about 8; that is a reading of pooled medians, not the rule.
- **The tail did not move**: work p95 227.0 → 226.4 on Components, 279.6 → 293.5 on Demo. The longest frames are not
  the Tape's on this phone; they are the drawing and the generator's, as the profile has them.
- Same phone, same run for every number in the table: yes. Same build of the driver: the Mac, unchanged.
- Both gates green on the branch (`make check`, `./gradlew check`).

## Outcome

The owner chose option 1 (2026-11-17): the grain and the scanlines are now fills at any density whose block of tiles
is whole pixels, and the Tape's cost on the phone went from about 8 ms a frame to about 2 by pooled medians, below
resolution by the rule on the busy pages and distinguishable on the still one. The sample still runs at 9–10 frames a
second on the Pixel 6a: drawing is three quarters of the main thread and the generator a fifth. What would change that
is a phone's budget for the boil, option 2 below, which changes what a phone shows; it is [B-108](B-108-the-browser-against-android.md),
a question for the owner.

## The question asked of the owner, kept


Asked first after the proxy (options: the Tape's tail, a phone's budget for the boil, leave it until a phone). The owner
attached a phone, and the question is now asked with its numbers:

1. **Keep the grain fix and take the rest of the Tape.** The scanlines are still 261 rects a frame at 2.625, and the
   band and the noise a few dozen more; one repeating fill for the scanlines is the same move as the grain's. The
   Tape off is worth 8–13 ms a frame on this phone; the grain fix took a part of it, below resolution.
2. **A phone's budget for the boil.** Drawing is three quarters of the main thread and the generator a fifth; with
   the Tape off the Components page still runs at 10–12 frames a second, so the Tape alone cannot bring it to 60.
   Fewer bodies boiling at once on a narrow window or a coarse pointer is what would; it changes what a phone shows.
3. **Stop here**, the grain fix kept or dropped.

Which of these, if any?
