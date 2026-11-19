---
id: B-138
title: "A round of frame measurements on every platform"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-138 — A round of frame measurements on every platform

The performance loop (B-110…B-135, #82) and B-126's default (#83) changed how every body is drawn, and each item
measured only its own question, mostly on one phone. The owner, 2026-11-19: «теперь давай сделаем замеры по кругу по
всем платформам». This item is that round: `main`'s code at `102a2dd`, every platform the kit targets, by the
harnesses the repository already has, one after the other.

- **The decision.** A baseline, not a verdict: each platform measured by its own harness at one commit, its control
  read in the same log, the numbers set beside the last run of the same scene where one exists, with the line saying it
  was another day.
- Rejected: one new harness for every platform (the existing ones carry their controls and their traps, B-49, B-73,
  B-107); verdicts against thresholds (no question here has one; a route chosen from the numbers is an item of its own).

- **Pre-registered on 2026-11-19, before the first number.**
  - **Code.** The kit and the sample at `102a2dd`; this branch adds only harness and documents. Each log names its
    commit.
  - **Desktop JVM, this Mac** (MacBook Air, fanless): `./gradlew :sample:frameBench`, its default scenes, 6 rounds of
    10 s, round 1 and each scene's first second not counted (B-72's protocol); its busy-wait control in the log.
  - **wasm, headless Chrome on this Mac**: `scripts/frame-bench-browser.mjs` on the production build, default scenes,
    6 × 10 s, its control in the log (B-73).
  - **Android, the Pixel 6a** over USB, on mains: `scripts/device-bench.sh screens`, release build compiled
    speed-profile, 6 × 10 s; its `Spin5` control (B-49, B-89). Battery and temperature before and after.
  - **wasm in Chrome on the Pixel 6a**: `DEVICE=1 scripts/frame-bench-browser.mjs`, its default device scenes plus
    `TenCards8`, 6 × 10 s, after the native run on the same day (B-107, B-108). Battery and temperature before and after.
  - **iOS, the simulator only** (the owner's choice; the sample has no iOS host and the iPhone is not attached). A thin
    host for `iosSimulatorArm64` with the scenes `TenCards8` and the sample's pages; per frame the interval and the
    main thread's work from the frame's start. **Its numbers are the Mac's CPU and GPU through the simulator, not a
    phone's**: reported as such, with what they can show (every scene draws, nothing throws, the order of the scenes'
    costs), never set beside a phone.
  - **Measures.** What each harness already reports: frames a second, the main thread's work a frame p50 / p95, and on
    Android the render thread and vsyncs skipped. Round 1 not counted everywhere.
  - **Order.** The Mac's two first, with nothing else running on it (no Gradle daemon building), then the phone natively,
    then the phone's Chrome, then the simulator.
- AC: raw logs under `logs/b-138/`; a table a platform; each control read in its own log; a same-host, same-run line
  under every table that sets two runs beside each other.
- Anchors: `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/bench/FrameBench.kt`,
  `scripts/frame-bench-browser.mjs`, `scripts/device-bench.sh`

## Findings, 2026-11-19

Logs: [the JVM](../../logs/b-138/2026-11-19-jvm.log), [headless Chrome on the Mac](../../logs/b-138/2026-11-19-chrome-mac.log),
[the Pixel natively](../../logs/b-138/2026-11-19-native.log) ([frames](../../logs/b-138/2026-11-19-native/)),
[Chrome on the Pixel](../../logs/b-138/2026-11-19-chrome-pixel.log), [the simulator](../../logs/b-138/2026-11-19-ios-sim.log)
and its two void runs ([first](../../logs/b-138/2026-11-19-ios-sim-blind.log),
[second](../../logs/b-138/2026-11-19-ios-sim-blind2.log)). The kit at `102a2dd` everywhere; the wasm build
the same bytes in both browsers (`2be7f9bb49abc303`, `089052ba37e2a6e8`, as where B-126's shader was seen engaged).
Rounds 2–6 pooled, each scene's first second not counted. A verdict word in a harness's own table is that harness's
old rule (B-49, B-73), not this item's: this item has none.

**Every control read in its own log:**

| Platform | Control | Read |
|---|---|---|
| JVM | 10 ms busy-wait in the window's render, over `ComponentsRest` | 9.06, 8.95, 8.75, 8.82, 8.88 ms |
| Chrome, Mac | 10 ms in the frame, over `ComponentsReduce` | 8.0, 8.4, 7.8, 7.7, 7.9 ms |
| Pixel, natively | `Spin5` over `TenCards12`, draw p50, pre-registered 5 ± 1 ms | 4.98–5.00 ms, sees it |
| Chrome, Pixel | 10 ms in the frame, over `ComponentsReduce` | 8.4, 10.0, 14.0, 15.6, 12.1 ms: noisy once hot |
| Simulator | 10 ms in a layer nested in a layer, over `Blank`, to the main thread's frame end | 9.27, 9.24, 9.17, 9.22, 9.13 ms |

**Desktop JVM, this Mac** (M1, fanless, on mains at 76 %). Work: Compose's whole frame around the window's render.

| Scene | f/s | Work p50 / p95, ms |
|---|---|---|
| `ComponentsRest` | 60.3 | 2.81 / 7.36 |
| `ComponentsWheel` | 60.1 | 2.87 / 6.25 |
| `ComponentsClean` | 60.3 | 0.15 / 7.09 |
| `ComponentsReduce` | 60.3 | 0.09 / 3.42 |
| `DemoPage` | 60.2 | 0.23 / 5.60 |
| `DemoLibrary` | 60.2 | 0.18 / 2.25 |
| `DemoRecording` | 60.4 | 0.19 / 1.34 |
| `DemoSettings` | 60.3 | 0.09 / 0.27 |
| `Blank` | 60.3 | 0.11 / 0.16 |

**wasm, headless Chrome on this Mac** (Chrome 157, 1280 × 814 at 2×, on mains at 88 %). Work: the page's main thread a
frame.

| Scene | f/s | Work p50 / p95, ms |
|---|---|---|
| `ComponentsRest` | 59.2 | 9.8 / 17.2 |
| `ComponentsWheel` | 58.1 | 10.0 / 16.7 |
| `ComponentsClean` | 60.5 | 5.2 / 16.2 |
| `ComponentsReduce` | 60.4 | 5.4 / 10.5 |
| `DemoPage` | 50.3 | 4.9 / 8.9 |
| `DemoPageClean` | 60.5 | 4.6 / 7.3 |
| `DemoLibrary` | 54.4 | 2.8 / 7.3 |
| `DemoRecording` | 60.4 | 3.6 / 6.5 |
| `DemoSettings` | 60.4 | 5.2 / 7.2 |

`ControlRest` (the 10 ms over the resting Components page) fell to 38.2 f/s: 10 ms more does not fit in the frame
that page leaves, as in B-73.

**Android, the Pixel 6a natively** (release, speed-profile; on mains; battery 32.7 → 33.8 °C, thermal status 0 at both
ends). B-49's screens; the main thread is animation + layout + draw from `FrameMetrics`.

| Scene | f/s | Vsyncs missed | Main thread p50 / p95, ms | GPU p95, ms |
|---|---|---|---|---|
| `TenCards8` | 60.3 | 0.0 % | 0.45 / 3.18 | 10.33 |
| `TenCards12` | 60.3 | 0.0 % | 0.45 / 2.85 | 10.67 |
| `List100` | 60.3 | 0.0 % | 2.72 / 11.63 | 10.21 |
| `Blank` | 60.3 | 0.0 % | 0.63 / 1.34 | 7.84 |

**wasm in Chrome on the Pixel 6a** (Chrome 157, 411 × 784 at 2.625×; on mains, charging 80 → 92 %, battery 34.5 →
38.3 °C over the run).

| Scene | f/s | Work p50 / p95, ms |
|---|---|---|
| `TenCards8` | 60.9 | 6.2 / 10.6 |
| `ComponentsRest` | **16.4** | 69.8 / 145.7 |
| `ComponentsClean` | **30.1** | 18.6 / 105.8 |
| `ComponentsReduce` | 60.2 | 6.6 / 26.7 |
| `DemoPage` | 49.9 | 7.0 / 16.3 |
| `DemoPageClean` | 60.4 | 6.4 / 14.7 |
| `DemoLibrary` | 49.5 | 7.4 / 14.6 |
| `DemoRecording` | 61.1 | 5.5 / 10.9 |
| `DemoSettings` | 61.1 | 10.2 / 12.9 |

- `TenCards8` beside its earlier runs, **another day, the same phone**: 22.3 f/s at `361b39f` with the bodies as paths
  (B-108, 2026-11-17, 28–31 °C), 60.8 with the shader switched on by hand (B-131, 2026-11-18, 37.9–39.0 °C), 60.9 here
  with the shader by default. Same host, same run: no.
- **The resting Components page is far from 60 once the phone is warm**: 45.5 f/s in round 1 (not counted) at about
  35 °C, 12.7–20.3 in rounds 2–6 at 37.4–38.4 °C, while the phone charged. Every body of that page under the shader, or
  some kept on paths (pressed, hot, a pour, an error and focus keep their paths, B-131), is not read here; a route
  from it is an item of its own.

**iOS, the simulator** (iPhone 17, iOS 27.0, on this Mac; its numbers are the Mac's CPU and GPU through the simulator,
never a phone's). Work: from the bench's frame callback to the end of the main thread's whole frame (a block put on the
main queue at the frame's start, run once the display link's callback has composed, laid out, drawn and encoded it).
The bench asks for every frame, so the f/s column counts vsyncs the main thread kept up with, not frames a scene asks
for.

| Scene | f/s | Work p50 / p95, ms | Root's draw p50, ms |
|---|---|---|---|
| `TenCards8` | 60.0 | 1.5 / 10.0 | 0.1 |
| `ComponentsRest` | 57.3 | 4.2 / 7.6 | 0.6 |
| `ComponentsClean` | 56.4 | 2.3 / 11.2 | never drawn |
| `ComponentsReduce` | 60.0 | 3.0 / 6.5 | never drawn |
| `DemoPage` | 58.9 | 2.1 / 5.3 | never drawn |
| `DemoLibrary` | 53.9 | 1.6 / 16.5 | never drawn |
| `DemoRecording` | 59.9 | 1.7 / 11.2 | never drawn |
| `DemoSettings` | 59.8 | 1.8 / 2.6 | never drawn |
| `Blank` | 60.0 | 1.8 / 2.1 | 0.3 |

The last column is what the void runs measured: `ComponentsRest`'s root's draw read 0.1 or 2.6 ms from round to
round while its frame held at 4.2, and in round 2 the control's read 0.0 while its frame read 11.0.

- **Every scene draws, nothing throws**: the sample runs on iOS for the first time, with its fonts (Handjet, Rubik) and
  bodies; two screenshots a second apart differ on every animated scene.
- **Two runs were void, both by a recorder that ended at the root's draw**, and no number of either is reported:
  - The first: the root's draw runs only when the root is invalidated, so six of ten scenes logged "idle (no frame
    drawn)" for the whole ten minutes while the screen moved (two screenshots a second apart differed). The control, a
    busy-wait in the frame callback, read 10.1 ms and passed. B-49's trap on Android, here on Skiko. Its header names
    the wrong simulator (`iPhone 17 Pro … Shutdown`, a grep fixed since); it ran on the iPhone 17 `F507C6D5`.
  - The second: the root redrawn every frame and the control moved into a layer nested in a layer. The control read
    10.0 ms in four rounds and **0.0 in round 4**, and in that round every scene read near nothing (`ComponentsRest`
    p50 0.1 ms against 2.6 in round 3, `DemoLibrary` p95 0.9 against 12.5): the nested layers' work fell after the
    root's draw in some rounds and inside it in others. Why it switches is not read.
  - The recorder now ends with the main thread's frame, the iOS counterpart of the JVM bench wrapping the window's
    render. The root's draw stays as a column (`draw_ns`, `root draw p50`), and shows where it would have been blind.
    The counted run's binary (`7ce328234a2c1c45`) was built before ktlint split the scenes' enum over lines; the
    committed source differs from it in layout only.
- **The sample's top bar sits under the status bar on iOS**: every page sets its insets to zero, right on the desktop
  and in the browser. [B-139](B-139-the-sample-under-the-status-bar-on-ios.md).

**What this branch adds to CI.** `./gradlew check` now compiles the sample for `iosSimulatorArm64` and links an empty
test binary for it on the macOS runner: the host cannot rot, at the cost of one more native link a run.
