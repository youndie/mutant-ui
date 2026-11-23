---
id: B-190
title: "A press under the pause shows a still frame at heat 1"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-190 — A press under the pause shows a still frame at heat 1

Under reduce motion a press shows nothing on buttons, the FAB, toggles and the refresh tongue (F-buttons-06, F-icon-fab-05, F-pull-to-refresh-03); the engine's paused mode does the same. [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 12 ([ошибка]).

- **The decision and its reason.** A press under the pause shows at once a still frame at heat 1: the fill mixed with amber, no bubbles and no tremble. It goes on release. This widens D7 (research D24).

- AC:
  - under reduce motion, pressed: the fill carries the amber mix of heat 1, and two frames 500 ms apart are identical; released: the rest picture on the next frame
  - without reduce motion the press is unchanged
  - each new test of the first criterion fails on the code before, and every new test kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at (rewritten 2026-11-23: the second criterion says nothing changes, so its guard passes on the code before by construction; it is held by the mutant that drops the pause from the rule)
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/theme/MutantTheme.kt`

## Result, 2026-11-23

`surface/MutantSurface.kt` and `surface/BodyHeat.kt` only. Every pressed component, the refresh tongue included (it
presses its own body while refreshing), boils through the surface's `HeatMachine`, so the change is made once there.

- **The still frame.** Under the pause a pressed body (`pausedPress`: reduce motion, no fixture's `heat`, the press on)
  is drawn as the rest frame: the resting tick's time, heat 0 for the shape, no held drops, no bubbles, no running
  mosaic. Only its fill changes, to `mutantPausedPressFill`: `boilFill` at heat 1 at time 0, where the boil's flicker is
  off, which is the theme's amber. It goes with the release, in its frame.
- **The words.** The ground a body gives its words (`LocalMutantBodyGround`) takes the same amber while the press stands,
  so a word on the body reads against what is drawn (B-91's rule).
- **The browser.** A paused press on the shader's path is drawn by paths, as a hot body is (D23).
- **Without the pause** nothing changes.
- **The design.** The engine's paused mode shows nothing on a press; the kit now leaves it (research §1.10's row and D24).
  No parity cell or golden draws a live press under the pause (fixtures pass `heat`), so none moved and nothing enters
  the departures registry: a change in live behaviour only.

**Frames looked at** (throwaway sheets, deleted): a filled button, the FAB, a switch, a checkbox and the refresh tongue,
at rest, one frame, 160 and 500 ms into a press, a frame and 480 ms after the release, under the pause and without it,
«Эфир» and «Копия» at density 1 and 2. Before: under the pause nothing amber, and the button's hanging drop vanished for
the press (the held drops of a pressed body), the refreshing tongue lime. After: every pressed body amber from its first
frame, its drips and drops as at rest, back to the rest picture on the release's frame; without the pause the sheets
show the same boil as before.

**Tests** (`mutant-core/src/desktopTest`, the components at density 2, both themes, pressed through the interaction source
a clickable reports to, the clock run 1.6 s before the pause so that the resting and the hot tick differ):

| AC | Test |
|---|---|
| pressed: heat 1's amber, two frames 500 ms apart identical, the rest shape; released: the rest picture on the next frame | `PressUnderThePauseTest` «under the pause a press shows heat 1's amber at once and still in the rest shape and goes on release»: about 1 400–35 000 amber px pressed against 0 at rest, 0 px between the frames 500 ms apart, the outline's ink moved 0 px, 0 px between the rest and the released frame (the tongue: no amber after refreshing ends); on the code before, 0 amber px |
| … the words on it read on the amber | `PressUnderThePauseTest` «under the pause a pressed outlined button's label reads on heat 1's amber»: the ground beside the label 8 of 8 px amber, the label at 11.04:1; on the code before, 0 of 8 |
| … drawn by paths on the shader's path (D23) | `PressUnderThePauseTest` «under the pause a press on the browser's shader path is drawn by paths as a hot body is»: 0 px between the two ways |
| without the pause the press is unchanged | `PressUnderThePauseTest` «without the pause a press still boils over 320 ms and moves»: no amber on the press's first frame, 37 000 px 500 ms in, still amber a frame after the release; with `SurfaceStatesTest`, `PauseLiftedTest`, `PressedLabelContrastTest` as before |

`SurfaceStatesTest` «under reduce motion a press does not boil» (B-11's reading of the engine) became «under reduce motion
a press stands at heat 1's amber and the release takes it back».

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored with
`git checkout`:

| Mutant | Killed by |
|---|---|
| no paused press (`pausedPress = false`) | «… goes on release»: 0 amber px pressed |
| a paused press drawn at the hot tick (`!pausedPress` dropped from `hot`) | «… goes on release»: the outline moved 339 and 665 px on the button |
| a paused press holding its drops (`pressedShape = boil.on`) | «… goes on release»: the outline moved 29–2 923 px |
| a paused press shaped at heat 1 (`k = 1.0`) | «… goes on release»: the outline moved 89–6 497 px, the body 0.4–9.2 % |
| the ground left at rest (`k = heatNow ?: boil.at(now)`) | «… label reads …»: 1.58:1 and 4.47:1 |
| the shader keeps a paused press (`pausedPress` dropped from the paths' condition) | «… drawn by paths …»: 384–4 760 px |
| the rule without the pause (`instant` dropped) | «without the pause …»: 35 658 amber px on the first frame |

**The ACs rewritten.** The third: «each new test fails on the code before» cannot hold for the second criterion, which
says nothing changes; its guard passes on the code before by construction and is held by the last mutant above. The
first: for the refresh tongue «released» ends refreshing and shows the done pill, so its release is read as no amber, not
as the rest picture.

**Gates on this Mac:** `make check`, `./gradlew check` (desktop, Android, the iOS simulator's tests, wasm),
`./gradlew :mutant-core:compileKotlinIosArm64`.

**Findings, not fixed here.**

- Under the pause a hover still draws the body at the hot tick's time, a different boil phase, and the release of a
  mouse press leaves it there while the mouse stays (F-checks-switch-05); hover is [B-193](B-193-hover-keeps-the-fill.md)'s.
- A body's stand-in in the browser (B-177) still shows a press under the pause at rest until its build is taken; the
  surface's frames after it are right.

**Not verified.** No phone and no real browser: the shader's path was run on the desktop only, Android, iOS and wasm only
as `./gradlew check` runs them. A real pointer press under the pause was not read (a press in a test window also takes
focus, whose brackets the tests leave out by pressing through the interaction source).
