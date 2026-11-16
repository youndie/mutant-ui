---
id: B-49
title: "The cost of a drawn body on a Pixel 6a: ten cards with the Tape, a list, an instance"
status: question
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

**Amended 2026-11-16, before the first number.**
- `List100`'s rows are laid out as the demo's Library lays them: each with a leading icon, whose tile is a drawn body, a time on the right and a `MutantDivider` under it, inside 16 dp of padding. A row of the kit has no body of its own, so bare rows would scroll nothing the kit draws.
- `Instance` follows B-06's own protocol rather than the counts written above, so that the factor compares like with like. The arms `instance` and `warm` alternate per call, 200 calls a round. There are five rounds, round 1 not counted, with a 3 s warm-up a case. Each round reports the median per arm, and B-06's control (`warm` plus 200 µs) runs on `card-a`. The M1 figures come from `fieldBench` at the same commit.

## Results, 2026-11-16

Run at `dcc1794` on the stand above. The phone ran Android 17 (API 37) on a 1080 × 2400 panel at 60 Hz and density 2.625, release, not debuggable. Thermal status was 0 through every screen scene and 1 in the instance arms' last two rounds. Logs:
- `logs/b-49/2026-11-16-pixel6a/` holds `metrics.csv`, every frame; `scenes.csv`, a line per scene; and `instance.txt`;
- `logs/b-49/2026-11-16-m1-fieldbench.log` is the M1 at the same commit.

`scripts/b49_summary.py` prints every number below from them.

**The control sees the nested layer.** `Spin5`'s draw p50 over `TenCards12`'s read 5.00, 5.00, 5.01, 5.00 and 5.00 ms per counted round, against 5 ± 1. B-06's control on the phone read 197–207 µs for its 200 µs.

**Load was delivered.** `List100` scrolled 38 947–39 015 px a scene against the 39 375 asked (1 500 dp a second for 10 s).

| Scene | frames/s | `TOTAL` p50 / p95 | missed deadline | vsyncs skipped (gap > 25 ms) | main-thread work p95 | work > 16.7 ms | Pre-registered verdict |
|---|---|---|---|---|---|---|---|
| Blank | 60.3 | 28.1 / 28.9 | 0.0 % | 0 % | 1.25 | 0 % | (red: the base) |
| TenCards12 | 59.4 | 29.1 / 55.0 | 1.5 % | 1.51 % | 27.03 | 6.8 % | **RED** |
| TenCards8 | 60.3 | 12.7 / 30.7 | 0.0 % | 0 % | 3.22 | 0 % | (grey: the kit's tick) |
| List100 | 60.3 | 17.7 / 32.9 | 0.0 % | 0 % | 13.32 | 0.3 % | **RED** |
| Spin5 | 59.2 | 29.2 / 56.4 | 1.7 % | 1.74 % | 29.42 | 6.8 % | (red: the control) |

Times are in ms, from rounds 2–6 and each scene's second second on; same host, same run.

**The pre-registered rule for a screen fails its own base.** `TOTAL` runs from the intended vsync to the frame's end, through a pipeline of buffers: the empty `Blank` screen reads 28 ms at p50, and Android's own `DEADLINE` for these frames is 33–66 ms. A p95 of `TOTAL` under 16.7 ms is a latency no screen on this phone reaches, so both targets read RED by a condition the empty screen also fails. The threshold was chosen badly. That is a limitation of the rule, written here beside the number, not a moved line.

What the run does show:
- **`List100` holds 60 frames a second.** No vsync was skipped, and 0.3 % of frames took over 16.7 ms on the main thread.
- **`TenCards12` drops 1.5 % of its frames**, all in its ticks. 6.8 % of frames, about a third of its 12 ticks a second, took 17–30 ms of main-thread work. At the kit's own 8 ticks a second the same ten cards drop nothing and stay under 3.3 ms at p95. A frame built ahead that is not ready in time is built in the frame (B-75). On the phone, at 12 ticks a second, that is a third of them → [B-89](B-89-twelve-ticks-built-in-the-frame-on-a-phone.md).

**The instance** (B-06's cases, by B-06's protocol): the worst case is `fab-b` at 283.2 µs, `card-a` 157.7 µs. **GREEN**: every case is under 1 ms.

**The factor between the M1 and the phone**, the phone's median over the M1's per case:
- **Instance: median 3.53**, range 2.23–18.00. By the pre-registered rule (3–5), it **confirms** B-06's 4. The range's top is five cases the M1 runs in 0.1–0.4 µs, at its timer's resolution; without the cases under 1 µs on the M1 the median is about 2.9.
- **Warm frame: median 2.68**, range 2.43–3.85. This **refutes** B-06's 4: the phone is slower by under three times, not four. B-06's grey card (532.5 µs on the M1, "about 21 ms on a Pixel 6a" for ten) reads 1 371 µs here, so ten cards take 13.7 ms of field a tick.

The harness printed its instance numbers in the phone's locale, with a decimal comma. The summary reads them either way, and the harness now prints in `Locale.ROOT`.

## Question for the owner, 2026-11-16

The pre-registered rule makes both screen targets RED, and it makes the empty screen RED as well. The pre-registration says a red starts the brief's retreat order. No step of that order can bring `TOTAL` under the 28 ms the empty screen already reads. A late amendment is the owner's to make, and only to fix an incoherence like this one. The choices:

1. **Amend late**: judge a screen by the vsyncs it skips. Green when under 1 % and the main thread's p95 is ≤ 16.7 ms; red when over 5 %. `List100` reads GREEN on that, with 0 % skipped and a p95 of 13.3 ms. `TenCards12` reads GREY, with 1.51 % skipped, and B-89 takes its cause.
2. **Keep the rule as frozen**: both RED, and the retreat arms measured on `TenCards12` and `List100`, knowing none can reach the line.
3. **Close as measured**: the numbers above stand, with no screen verdict, and B-89 goes on.
