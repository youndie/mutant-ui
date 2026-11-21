---
id: B-161
title: "In the browser, a body's build lets the page's frames through every few milliseconds"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-155]
---

# B-161 — In the browser, a body's build lets the page's frames through every few milliseconds

With every resting tick built between frames ([B-155](B-155-no-tick-built-in-a-frame.md)), the builds themselves held the
thread: in the phone proxy a tick's build ran 5 ms at p50, 12 at p90, 146 of 800 over 10 ms, and a body's first build
held it 29 ms a tab (its static layers and boil noise in one go). A frame that comes during a task waits for its end.

- **The decision**: the generator's field is built a few columns at a time (`FieldSteps`, `FrameSteps`), and the instance's
  static layers (`StaticSteps`) and a boil phase's noise a few rows at a time; built in one go they are what they were, to
  the bit. In the browser a build takes 8 columns or rows at a time and lets the thread go every 4 ms (`inSteps`), each
  resumption a task of its own after the page's frames.
- AC:
  - the generator's, transition and keyframe loss goldens unchanged on every target (the one-go build is the stepped one
    run to its end);
  - measured against the arm before it.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Field.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Frame.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyAhead.kt`

## Findings
