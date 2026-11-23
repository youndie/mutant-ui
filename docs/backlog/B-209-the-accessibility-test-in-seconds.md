---
id: B-209
title: "The sample's accessibility test runs in seconds"
status: done
priority: P1
size: XS
stage: stage-6-later
blocked_by: []
---

# B-209 — The sample's accessibility test runs in seconds

`sample`'s `AccessibilityTest` ([B-38](B-38-accessibility.md)) runs 68–84 s on this Mac alone, and inside a full
`./gradlew check` under load it passes its one-minute `runTest` limit (`UncompletedCoroutinesError: After waiting for
1m, the test body did not run to completion`): it failed two local full gates. Main's CI has passed it so far, a slow
runner away from a red main. A similar case, [B-208](B-208-the-paused-press-shader-test-in-seconds.md), was the body
shader on the CPU.

- **The decision and its reason.** The test reads its pixels from one array per capture and its luminances from a
  table of the 256 channel values, and works out each capture's luminance once. Timed phase by phase, two thirds of
  the time was the test's own pixel work, not the kit: three `pow`s per pixel for a luminance, twice per pixel per tick
  for the tick-to-tick change, through one `getRGB` call per pixel. The table holds the same doubles `pow` gave, so
  every number the test reads is unchanged.
- Rejected: a longer timeout, which leaves a minute-long test one slow runner from red; fewer screens, themes or ticks,
  which drops what B-38 reads; fewer frames between the captures (the kit clock's `speed`, or a frame longer than the
  harness's 16 ms, which is fixed in its render loop), which would move what the Tape, the transitions and the bodies
  built ahead draw at each captured moment; a window cut to the screen's 360 × 768, which drew the same pixels in the
  same time.
- Not covered: the kit's own cost per frame in a software-rendered test (below).

- AC:
  - the test runs in seconds on this Mac (read from the JUnit XML)
  - it still holds every assertion it holds: 4.5:1 for every active word at the worst tick, no tick changing a quarter
    of the screen, the settings form's red area steady, over the same four cases in both themes and 56 ticks
- Anchors: `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/AccessibilityTest.kt`

## Result, 2026-11-23

**The cause.** The test's own pixel arithmetic, not the kit. Timed phase by phase over its eight compositions (two
themes, four cases, 56 captures each), the tick-to-tick change took 4.2–6.3 s a case and the contrast 0.4–1.3 s: a
luminance of three `pow`s per pixel, two per pixel per tick for the change, through one `getRGB` call per pixel, over
259 200 pixels a capture. That was 52 of the 80 s.

**The fix.** A table of the 256 channel values (the same doubles `pow` gave), one bulk `getRGB` per capture, and each
capture's luminance computed once and kept as the next tick's previous frame. The printed table (largest change, red
area, the five worst words of each case) is identical to the one before the change, and a hash of every captured pixel
is identical between runs.

**Before and after** (JUnit XML, this Mac): 80.3 s alone before; 28.1–33.9 s alone after, 34.9 s in `:sample:desktopTest`
and 42.8 s inside a full `./gradlew check --continue` with every test task run. Each of its eight `runComposeUiTest`
bodies, the minute's unit, now takes 2.2–4.4 s alone, the first 7.7–9.8 s with the JVM's warm-up (it was 8–14.5 s).

**What is left is the kit's own frames, and the test needs them.** 22 of the remaining seconds are the 3 900 frames the
harness steps at its fixed 16 ms (8 × 486) to bring the clock to the moments the test reads. Since B-136 the Tape draws
on every frame, and a frame of the demo screen costs 2.5–4.6 ms in the software test window, about half of it the
Tape (read once with the Tape left out, as a measurement only). The bodies are the lesser part: «Эфир»'s Settings
dialog, about one body drawn a frame, costs 3.0 ms a frame, its Library, 11–32 a frame, 5.3 ms. "Seconds" in the sense of B-208's 4 s is not
reached without reading other moments.

**Mutant.** «Эфир»'s secondary text darker (`textSecondary` `AAB4CF` → `6E7488` in `MutantTokens.kt`): killed, the aimed
test's `<failure>` in the XML names the Library's words at 2.98 and the Recording's at 3.91 under 4.5. Restored with
`git checkout`, the tree clean.

**Gates on this Mac:** `make check`; `./gradlew :sample:desktopTest ktlintCheck`; `./gradlew check --continue` with the
test tasks' outputs cleaned first (651 tests, 0 failures). Slowest there: `LongContentSweepTest` 50.2 s (many short
bodies, one per subject and place), `AccessibilityTest` 42.8 s, `StillLayersOnceTest` 35.8 s, `ConstraintsSweepTest`
29.6 and 27.7 s.

**Not verified.** The CI runner's time; no phone.
