---
id: B-209
title: "The sample's accessibility test runs in seconds"
status: wip
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

- **The decision and its reason.** To be written once the time is measured phase by phase.
- Rejected: a longer timeout, which leaves a minute-long test one slow runner from red; fewer screens, themes or ticks,
  which drops what B-38 reads.

- AC:
  - the test runs in seconds on this Mac (read from the JUnit XML)
  - it still holds every assertion it holds: 4.5:1 for every active word at the worst tick, no tick changing a quarter
    of the screen, the settings form's red area steady, over the same four cases in both themes and 56 ticks
- Anchors: `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/AccessibilityTest.kt`
