---
id: B-208
title: "The paused press's shader test runs in seconds"
status: done
priority: P1
size: XS
stage: stage-6-later
blocked_by: []
---

# B-208 — The paused press's shader test runs in seconds

`PressUnderThePauseTest` «under the pause a press on the browser's shader path is drawn by paths as a hot body is»
([B-190](B-190-a-press-under-the-pause.md)) took 119.998 s on this Mac against 2–8 s for its siblings. On the CI runner it
failed an unrelated pull request (#127, run 45182885890) with `UncompletedCoroutinesError: After waiting for 1m, the test
body did not run to completion`: one of its twelve `runComposeUiTest` bodies passed the minute alone.

- **The decision and its reason.** The test pauses from its first frame instead of running the clock 100 frames
  before the pause. The other tests let the clock run so that it stands where the resting tick and the hot one differ
  (a press drawn at the hot tick changes the shape). That reason does not apply here: this test compares the shader's
  way with the paths' for the same press, and which tick the clock stands at does not decide which way a press is drawn.
- Rejected: a longer timeout, which leaves a two-minute test and a CI job a minute from failing again; fewer cases,
  which drops the checkbox's or a theme's reading for nothing.
- Not covered: the cost of the shader in a software-rendered test generally (`BodyShaderParityTest` and the other
  shader tests draw at a frozen clock and are quick).

- AC:
  - the test runs in seconds on this Mac (read from the JUnit XML), its siblings unchanged
  - it still holds what it holds: 0 px between the two ways on the fix, and it kills the mutant «the shader keeps a paused press»
- Anchors: `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/PressUnderThePauseTest.kt`

## Result, 2026-11-23

**The cause.** Nothing waited on a missing event: the time was the shader itself. The test window draws in software,
where the body's runtime shader is evaluated pixel by pixel on the CPU. A film ran the clock 100 frames before the pause
(`WARM_UP`), and on the shader's path every running frame redrew the resting body through the shader. Timed phase by
phase: the warm-up took 11.8 s for the filled button and 4.8 s for the FAB on the shader's path, against 0.6 s by paths,
and everything after the pause took under 0.3 s. Twelve films, six on the shader's path, came to 50–120 s.

**The fix.** `film(…, warmUp = 0)` holds the pause from the first frame; only this test passes it. The test now takes
3.7–4.4 s (JUnit XML, two runs; its siblings 1.4–6.4 s), and reads 0 px between the two ways for the filled button, the FAB and the checkbox in both themes, as
before.

**Mutant.** `pausedPress` dropped from the paths' condition in `MutantSurface` (the shader keeps a paused press): killed,
the aimed test named in the XML, the shader's way 386–4 261 px off the paths'. Restored with `git checkout`.

**Gates on this Mac:** `make check`; `./gradlew :mutant-core:desktopTest --tests '*PressUnderThePause*' ktlintCheck`.

**Not verified.** The CI runner's time; no phone.
