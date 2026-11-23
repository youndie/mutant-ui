---
id: B-193
title: "Hover keeps the fill and only stirs the outline"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-193 — Hover keeps the fill and only stirs the outline

A hovered card's subtitle reads 3.22:1 against 6.9:1 at rest (F-card-cocoon-05). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answer 16 ([ошибка]).

- **The decision and its reason.** Hover does not change the fill. Its look is only a stronger tremble of the outline, as heat 0.25 draws it, with no amber mix and no bubbles. It is the hover look, so every body that hovers takes it, not the card alone.
- The hover goldens move and are re-recorded and looked at.

- AC:
  - for every hovering component, the fill's pixels hovered equal those at rest and the outline trembles as at heat 0.25
  - a hovered card's subtitle reads the contrast it reads at rest
  - under the pause a hover shows its finished frame at once, at the resting tick's time (D24; folded in from [B-190](B-190-a-press-under-the-pause.md)'s note on F-checks-switch-05, added 2026-11-23)
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantHover.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantCard.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/Frame.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/HoverKeepsTheFillTest.kt`

## Result, 2026-11-23

`surface/BodyHeat.kt`, `surface/MutantSurface.kt`, `surface/BodyAhead.kt` (the frame memo's key) and `gen/Frame.kt`.
Every body that hears the mouse does so through the surface's `HeatMachine` (`MutantHover.kt` only sets the hand), so the
change is made once there, for every hovering component.

- **Two heats.** A body's heat was one number, which a hover raised to 0.3 and which the fill, the bubbles, the held
  drops, the running mosaic and the tremble all followed. It is now two: the press's (`HeatMachine.at`), which the
  fill, the bubbles, the held drops, the running mosaic and the words' ground follow, and the tremble's
  (`trembleAt`), the higher of the press's and the hover's. The hover's heat (`HOVER_HEAT`) is 0.25, rising and
  cooling at the press's token rates (320 ms and 480 ms to full).
- **The frame.** `MutantGenerator.frame` takes `pressHeat` beside `heat`: the bubbles and the held drops follow it, the
  tremble (`boil`, the top wave, the hot tick's phase and so the outline's width) follows `heat`. Equal, as every
  fixture and every press passes them, the frame is the engine's, so no generator golden moves.
- **What a hover now draws.** Its own fill, its drips and drops as at rest, and the outline trembling as heat 0.25 draws
  it, at the hot tick. The running mosaic is left out too: the answer says the look is **only** the outline's tremble,
  and the mosaic's recolouring is not the outline. A press over a hover boils as before and cools back to the hover's
  tremble.
- **Under the pause** (D24, F-checks-switch-05). A hover shows its tremble at once and still, at the resting tick's time,
  and the leave takes it in the frame; lifting the pause replays nothing. Before, the hover's heat stood at 0 under the
  pause while the body was redrawn at the hot tick's time: its drops jumped and nothing of a hover showed. The mouse's
  arrival is read as state in the draw whatever the pause, so it redraws the body under it.
- **The browser.** A hovered body on the shader's path is drawn by paths, as a hot body is (D23); before, its running
  mosaic sent it there, and the hover no longer has one.
- **The words.** A hovered card's subtitle reads 6.69:1 in «Эфир», as at rest; it read 3.22:1 under the mouse.

**Frames looked at** (throwaway sheets, deleted): the card with both lines, a filled button, a chip, a checkbox, a
switch and the FAB, at rest, every 48 ms for 288 ms under the mouse and 96–288 ms after it left, with and without the
pause, «Эфир» and «Копия» at density 1 and 2. Before: the card's navy and the button's lime warming to brown and olive
within 100 ms; under the pause no amber, but the card's falling drop standing elsewhere from the mouse's arrival and
back on its leave. After: every fill as at rest through the hover, the outline and the drips' necks stirring at the hot
tick, back on the leave; under the pause the outline's still tremble at once, the drops where they hung.

**Tests** (`mutant-core/src/desktopTest`, density 2, both themes, the mouse moved over the component):

| AC | Test |
|---|---|
| every hovering component: the fill's pixels hovered equal those at rest | `HoverKeepsTheFillTest` «every hovering body keeps its fill under the mouse and only its outline trembles harder», over the filled and outlined button, icon button, FAB, chip, checkbox, radio, switch, card, list row's tile, dropdown and carousel card: the rest fill's pixels kept at 97–108 % in two hovered frames a hot tick apart (at least 85 % asked), no added amber pixel, 700–12 000 px changed; on the code before, 0 of 1 100–70 700 fill pixels kept |
| … and the outline trembles as at heat 0.25 | the same test's changed pixels, and `HoverKeepsTheFillTest` «a frame at the hover's heat trembles as heat 0_25 with no bubbles and no held drops»: over 96 hot ticks the card's frame at `heat` 0.25, `pressHeat` 0 has the engine's heat-0.25 boil (0.8 · 1.5 + 0.15) and phase and the rest frame's primitives, while the engine's own heat 0.25 adds bubbles or holds a drop on some (the control); and «the hover's heat trembles the outline alone, rises to a quarter and lands at once under the pause» on `HeatMachine` |
| a hovered card's subtitle reads the contrast it reads at rest | `HoverKeepsTheFillTest` «a hovered card's subtitle reads the contrast it reads at rest»: the subtitle's box, its commonest colour as the ground and its furthest pixel as the ink, at 12 hot ticks under the mouse, within 3 % of rest; on the code before, 6.69:1 at rest and 3.22:1 under the mouse |
| under the pause, the hover's finished frame at once (D24, folded in from B-190's note) | `HoverKeepsTheFillTest` «under the pause a hover shows its finished tremble at once, still, at the resting tick's time»: two runs whose clock stands in the same resting tick and different hot ticks draw the hovered bodies alike (0 px apart; the control: 0 px apart at rest), 400–5 500 px changed from rest on the hover's first frame, 0 px between it and 400 ms later, no amber, the rest picture on the leave's frame; on the code before, the two runs 1 100–1 300 px apart |

`HoverAndCursorTest` (B-63) «under the mouse the body simmers at the hot tick …» became «… trembles at the hot tick in
its own fill …»: the button's lime stays above 85 % of rest under the mouse, where it asked under half.
`ButtonBehaviourTest` (B-168, F-buttons-01) «a loading button shows no press, no simmer and no hand, and simmers again
under the mouse when done» read the hover by its amber; it became «… no hover … trembles again …» and reads it by the
hot tick the body redraws at: a second of frames changes 13 times under the mouse on an idle button, 9 or 10 while
loading under it (asked: 12 or more, 10 or fewer).

**Mutants**, each after the commit, the aimed test named among the failures in the JUnit XML, the file restored with
`git checkout` and `git status` clean after:

| Mutant | Killed by |
|---|---|
| the fill drawn at the tremble's heat (`pictureOf(g, frame, t, kt)`) | «every hovering body keeps its fill …»: 0 fill pixels hovered; «… subtitle …»: 3.69:1 |
| the press's heat taking the hover's (`at` = the higher of both) | «the hover's heat trembles the outline alone …»: `at` 0.25; `HoverAndCursorTest`: 0 lime pixels hovered |
| a hover under the pause drawn at the hot tick (`!instant` dropped from `hovering`) | «under the pause …»: the two runs 3 300–3 400 px apart |
| a hover under the pause not landing (`hover.land` dropped from `hover`) | «the hover's heat …»: 0.0 for 0.25; «under the pause …»: no tremble shown |
| a hover holding the drops (`primitives(g, t, heat > 0 …)`) | «a frame at the hover's heat …»: other primitives at 1 000 ms |
| a busy button letting the hover through (`QuietWhileBusy` without its `HoverInteraction.Enter` filter) | `ButtonBehaviourTest` «a loading button shows no press, no hover …»: 12 changes a second loading under the mouse |

One survived: the mouse's arrival read as state only off the pause (`mouseOver = !instant && boil.hovered`). Under the
pause the test still saw the hover's frame at once, so something else in the test window redraws the body when the mouse
arrives; the read stays, as the guard that does not depend on it.

**Goldens and parity cells.** None moved: no fixture or reference hovers (B-63), and fixtures pass one `heat` that both
heats take, so the generator's goldens, the library's goldens and every parity cell are unchanged (`./gradlew check`).
A change in live behaviour only, so nothing enters the departures registry.

**The ACs.** The first held as written; «as at heat 0.25» is read as the answer's tremble: the boil, the top wave and
the hot tick's phase of heat 0.25, without its amber and bubbles (the answer), its held drops and its running mosaic
(«only the outline»). The third: the two `HeatMachine` and frame tests do not compile on the code before (they call
`trembleAt` and `pressHeat`, which it lacks), and `HOVER_HEAT` was 0.3 there; the three pixel tests fail on it by
behaviour, as the table says.

**Not verified.** The browser and the phone: a hovered body on wasm is drawn by paths, as a pressed one is, and no
phone was used (a mouse has no hover on a phone). The other hovering bodies not in the test's list (a picker's days, a
dialog's actions) share the same surface code and were not read one by one.

**Gates on this Mac:** `make check`, `make docs-against BASE=origin/main`, `./gradlew check` (desktop, Android, the iOS
simulator's tests, wasm; its first run failed on `ButtonBehaviourTest`'s amber reading, rewritten above),
`./gradlew :mutant-core:compileKotlinIosArm64`.
