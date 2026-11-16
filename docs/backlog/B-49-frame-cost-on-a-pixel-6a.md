---
id: B-49
title: "The cost of a drawn body on a Pixel 6a: ten cards with the Tape, a list, an instance"
status: wip
priority: P3
size: M
stage: stage-1-materials
blocked_by: [B-06, B-10, B-73]
---

# B-49 — The cost of a drawn body on a Pixel 6a: ten cards with the Tape, a list, an instance

**Moved to the end by the owner on 2026-11-15**, with [B-73](B-73-frame-cost-in-the-browser.md): after the kit's components are built.

The brief's target is set on a Pixel 6a: 60 fps with ten cards and the Tape at the kit's tick, a
100-item list scrolling, an instance generated in ≤ 1 ms. [B-06](B-06-frame-cost.md) measured the field
on the desktop JVM and assumed the phone four times slower; this item measures the phone (research D13).

- **The decision and its reason.** As soon as one body is drawn ([B-10](B-10-mutant-surface.md)), a
  throwaway host on the phone draws ten cards and reads `FrameMetrics` (animation + layout + draw; GPU and
  deadline need API 31), with a control that busy-waits a known time inside the same nesting to prove the
  recorder sees it. Pre-registered in this item before the first number, as B-06 was.
- The verdict against the brief's green and red; on red, the brief's retreat order and the handoff's two
  outs, each measured.
- Rejected: measuring last, as plakat-ui did; here the architecture depends on the answer.
- **The order, the owner's (2026-11-14).** The JVM first ([B-72](B-72-frame-cost-on-the-jvm.md)) and its fixes, then the browser ([B-73](B-73-frame-cost-in-the-browser.md)), then this phone: a cost found on the JVM is fixed where it is cheapest to profile, before the phone measures what is left.

- AC: a verdict per target with raw logs under `logs/b-49/` and the control's reading; the factor between
  the M1 and the phone measured, so B-06's assumption is confirmed or refuted in writing.
- Anchors: `docs/backlog/B-06-frame-cost.md`, `reference/design-system/brief.md`

## Pre-registered, 2026-11-16, before the first number

**Stand.** The owner's Pixel 6a (`bluejay`), on USB, screen on, the window asked for 60 Hz. The host `bench-android` is a release build with R8, signed with the debug key, compiled `speed-profile` before the run. Every log's header records the device, its build, the display, the cores, whether it is debuggable and the commit; every round records the thermal status. The M1 side is the same day's run of B-06's `fieldBench` on this Mac (MacBook Air M1, macOS 27.0.1, JDK 25).

**Scenes**, each a screen of the kit under `MutantTheme` («Эфир»), composed from the kit's own components:
- `Blank`: the screen's ground and nothing on it. The base of the control.
- `TenCards12`: ten `MutantCard`s 328 × 80 dp, each with a title and a subtitle, under the screen's Tape (`mutantTape`), on a clock at the brief's 12 resting ticks a second. **The brief's first target.**
- `TenCards8`: the same at the kit's own tick, 8 a second. Reported beside it, no verdict of its own.
- `List100`: a `LazyColumn` of 100 `MutantListItem`s under the Tape, at 12 ticks a second, scrolled by a script at 1 500 dp a second, down to the end and back for the whole scene. **The brief's second target.** The scroll distance is read every round: load delivered.
- `Spin5`: `TenCards12` with a busy-wait of 5 ms inside a graphics layer nested in another inside the screen, on every frame. **The positive control**: its draw must read 5 ms over `TenCards12`'s (pooled p50, within ±1 ms), or the recorder is blind there and no verdict stands (memory of plakat-ui B-06: a recorder ending in the root's draw missed a nested layer).
- `Instance`: no screen. B-06's cases (the vendored golden's instances), each generated 200 times after 50 unmeasured, and a warm frame of each built 200 times after 50; per case the median. **The brief's third target**, and the factor against the M1.

**Protocol.** Ten seconds a scene, six rounds, the order of the screen scenes rotating each round; round 1 and each scene's first second are not counted. Per frame, Android's `FrameMetrics`: animation + layout + draw (the main thread's work), sync, command issue, swap, GPU, total, deadline, and the intended vsync, whose differences are the intervals.

**Verdicts.**
- A screen scene, at 60 Hz: **green** when frames that missed their deadline (`TOTAL > DEADLINE`) are under 1 % and the p95 of `TOTAL` is ≤ 16.7 ms; **red** when the p50 of `TOTAL` is over 16.7 ms or missed frames exceed 5 %; anything else is grey. The frames delivered a second are reported beside it.
- `Instance`: **green** when every case's median is ≤ 1 ms, **red** when any case's is over 1 ms: the brief's line, with no grey.
- The factor: the phone's median over the M1's, per case, for the instance and the warm frame, reported against B-06's assumed 4 with its range over the cases. No verdict; B-06's assumption is confirmed when the cases' median factor is within 3–5, refuted otherwise.

**On red**, the brief's retreat order (`brief.md`, «Производительность»), each step an arm measured on the red scene before the next: the Tape as a static tile shifted per tick → the grain off → resting ticks only for the six visible components → the mosaic only on a change of state. «Цель не понижаем»: a red stays red until an arm turns it, and each arm becomes an item if it changes the kit.

**Rejected**: an instrumented test in `mutant-core` for the instance. Its APK is debuggable, and a debuggable runtime's times are not a user's. The host reaches the generator through `-Xfriend-paths`, as a harness, and is not published.
