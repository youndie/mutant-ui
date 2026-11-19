---
id: B-138
title: "A round of frame measurements on every platform"
status: wip
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
