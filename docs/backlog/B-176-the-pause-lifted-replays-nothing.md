---
id: B-176
title: "A transition the pause landed stays landed when the pause lifts"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-176 — A transition the pause landed stays landed when the pause lifts

[B-168](B-168-look-and-motion-pass.md)'s pass, foundations report, F-foundations-03 (`logs/b-168/foundations.md`):
switching reduce motion off replays the transitions the pause had already landed. Under the pause (research D7: the clock
stops, damage stays, toggles are instant) a pour, a slide and a melt land at once, but only because their progress reads
`instant` as 1: the move itself is not ended, and the clock stood at the moment it was paused. When the pause lifts, the
progress is read again from the stopped time, and the move runs again from where it stood when the pause began. A press
released under the pause also keeps its heat, the amber, until the clock moves again: the heat is a function of the
standing clock (`HeatMachine.at`).

**Seen on `main`** (density 2; a 300 × 88 card pouring lime, a 56 × 30 pill sliding 160 dp, a 24 dp mark melting in
and a button pressed for 320 ms; the pause switched on 144 ms into the transitions, the press released under it, the
pause lifted six frames later; `/private/tmp/b176/before/`):

| | paused | the frame the pause lifts | then |
|---|---|---|---|
| card pour | 96 494 lime px | 27 448 | pours again for 350 ms |
| pill slide | centre at 416 px (home) | 315 px, 101 px back | slides home again for 210 ms |
| mark melt | 2 304 px (whole) | 1 536 | melts in again |
| button, released under the pause | warmth 801 of 1 000 (amber) | 801 | cools only once the clock runs again |

Causes: `surface/MutantSurface.kt` reads the pour's and the slide's progress from `moves.pourSince` and the slide run's
`since` with `instant` as the only end; `surface/BodyTransitions.kt`'s `MeltFollower.at` likewise; `surface/BodyHeat.kt`'s
`HeatMachine` keeps the release's `from` and `since` and cools only as the clock runs.

## The decision

- **The pause ends the moves.** In every composition under the pause, after it has followed a change, a body ends its
  running moves: the pour (`BodyMoves.stand`: its start at −∞), the slide (`MutantSlideRun.finish`: done, nothing
  carried) and the melt (`MeltFollower.follow` under the pause: the motion's start at −∞, so it stands at its end, or
  back at its start if it was running back). A move started under the pause is ended in the same composition. When the
  pause lifts there is nothing left to replay, and a change after it starts from rest: no reversal band (B-171), no
  carried stretch, no reversal of a melt.
- **Under the pause a release cools at once.** A press released (or a mouse leaving) under the pause drops the heat to
  where it is going in the same frame: the body shows released while the pause lasts, and has nothing to cool when it
  lifts. A heat still cooling when the pause starts lands the same way. The rest is as before: a press under the pause
  does not boil (B-11), and a press held across the pause keeps its heat, and goes on rising once the pause lifts.
- **Rejected: the clock remembers the pause**, a count of pauses or the moment the last one began, and every reader of a
  `since` reads a move started before it as done. Every follower would have to consult it, the navigation bar's and the
  calendar's places included (they read the slide run's progress themselves), and a move started under the pause would
  still need ending at the moment it starts. Ending the move where it is followed keeps one rule in one place per move.
- **Rejected: the release shown when the pause lifts.** Reduce motion is most often the platform's setting, on for the
  whole session: the body would show a press that is over for as long as the pause lasts.

No golden or parity cell draws a pause toggled mid-move; the item moves none.

## Acceptance

- A transition landed under the pause stays landed when the pause lifts: for a pour, a slide and a melt, paused at
  several moments of their run, with the design's steps and without, no frame from the lift on draws less of the landed
  picture than the pause showed (margins in the test).
- A press released under the pause shows released at once (its warmth back to rest in that frame), and stays released
  when the pause lifts; a press held across the pause stays hot under it.
- A transition started after the lift plays as before: the same pour, slide and melt changed back right after the lift
  draw, frame by frame, what they draw changed back from a rest that was never paused (margins in the test).
- Every golden and parity cell unchanged; `SurfaceTransitionsTest`'s and `SurfaceStatesTest`'s reduce-motion cases and
  B-171's `BodyReversalTest` unchanged.

## Findings, 2026-11-23

**After** (the same scene as above; `/private/tmp/b176/after/`): the pause lands the card at 96 494 lime px, the pill
home at 416 px and the mark whole at 2 304 px, and from the lift on nothing runs again: the card keeps 96 280–96 798 px
(the boil at rest), the pill stays at 415–416, the mark at 2 304. The button released under the pause is back to rest
(warmth 3 of 1 000) in the frame after the release and stays there.

`PauseLiftedTest` (density 2, the test's clock advanced by hand, every reading of drawn pixels):

| | the bound | measured |
|---|---|---|
| landed, paused at 16, 144 and 272 ms, steps on and off | from the lift on, the card keeps all but 2 % of its lime, the pill within 2 px, the mark every pixel | card ≥ 96 304 of 96 365–96 787, pill within 1 px, mark 2 304 |
| released under the pause | warmth within 50 of rest from the release's frame on | 4 against 3 (801 held across the pause) |
| cooling when the pause starts (80 ms into 480) | the same | 4 against 3 (801 before the pause) |
| changed back after the lift, against the same change from a rest never paused | card within 2 % of its full lime frame by frame, pill 2 px, mark exact | card ≤ 592 px, pill ≤ 2, mark 0 |

The 2 % for the card is the boil: at rest it moves 0.6 % of the lime from tick to tick, and two bodies boiled at other
times differ by about as much. The scene places the pill and the button by padding, not by `offset`: a body moved by
`Modifier.offset` further than its own box and 64 dp counts as outside that box (B-87's `standing`) and replays its
last picture, so it neither slides nor boils on screen. Seen in the scratch scene, not followed further here.

**Mutants**, each one line on the commit, each killed by its aimed test (the `<failure>` read from the JUnit XML):

- the pour not ended (`pourSince` kept in `BodyMoves.stand`): paused at 144 ms, the card drops to 27 488 px after the
  lift (16 ms: 293 px);
- the slide not ended (no `slideRun.finish()`): the pill leaves home by up to 290 px;
- the melt not ended (`MeltFollower.follow`'s pause line removed): the mark drops to 1 536 px (16 ms: 192 px);
- the release not settled under the pause (`HeatMachine.release`): warmth 801 under the pause and after the lift;
- the cooling heat not landed at the pause's start (`HeatMachine.hold`): warmth 739 under the pause;
- the moves ended with no pause (`if (instant)` dropped before `moves.stand()`): the card never pours back (160 px
  throughout).

A first heat mutant survived: `hold` settled in every composition under the pause, which read the press state there,
so a release recomposed the body and settled it anyway. `hold` now lands the heat only as the pause starts, and the
release settles itself.

**Gate** on `d6bfaed`, read from fresh JUnit XML: `:mutant-core:desktopTest` 512 tests, `:sample:desktopTest` 22, no
failure; ktlint and the ABI check clean; `compileKotlinIosSimulatorArm64` and `compileKotlinWasmJs` build. No golden or
parity cell moved.

What is not covered: the components that keep transitions of their own on the clock (a dialog's, a sheet's and a
drawer's rise, the snackbar, the tabs' bar, the menus, the segmented button, the progress, the pull to refresh) were
not looked at under a pause lifted mid-way. The navigation bar's pill and the calendar's drop share the body's slide
run (`SlideRequest.run`) and end with it.

- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyMoves.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
