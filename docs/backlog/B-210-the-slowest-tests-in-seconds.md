---
id: B-210
title: "The slowest tests run well under their one-minute limit"
status: wip
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-210 — The slowest tests run well under their one-minute limit

`runComposeUiTest` and `runTest` give a test body a fixed minute. Several tests sit close to it, and a slow macos-latest
runner or a loaded Mac turns them red. PR #136's CI failed on the sample's `ComponentsPageTest` «no design stage is
named on either page, in either language», which passed the minute (run 45262647941) and passed on a rerun. A local
full `./gradlew check` under load timed out six: the sample's `AccessibilityTest` (28–55 s after
[B-209](B-209-the-accessibility-test-in-seconds.md)), two `ComponentsPageTest`s, `DisabledContrastTest`,
`MenuEdgesAndChosenRowTest` and one `LongContentSweepTest` cell. Others are known slow: `LongContentSweepTest` 50 s,
`StillLayersOnceTest` 36 s, `ConstraintsSweepTest` 28–30 s.

- **The decision and its reason.** Each test above about 20 s is timed phase by phase, as B-209 did, and sped up where
  its time goes, without dropping anything it reads: pixels read once into an array with lookup tables, a composition
  reused across cases, a capture cut to the region the test reads, or one long test split into several that together
  hold the same cases.
- Rejected: a longer or wrapped timeout (`timeout =`, `runTest(timeout = …)`), which leaves a test a slow runner away
  from red; fewer screens, themes, languages, cells, ticks or assertions; a faster kit clock or a longer frame, which
  moves what is drawn at each captured moment; `@Ignore`; kit code changed only to make a test faster, unless it is a
  pure speed-up held by the existing goldens.

- AC:
  - every test above 15 s on this Mac is listed here with its time, read from the JUnit XML
  - every listed test runs well under 30 s alone on this Mac, before and after recorded here
  - every changed test holds every assertion it held, and kills a mutant of what it guards
- Anchors: `sample/src/desktopTest/kotlin/io/github/youndie/mutant/sample/AccessibilityTest.kt`
