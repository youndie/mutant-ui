---
id: B-176
title: "A transition the pause landed stays landed when the pause lifts"
status: wip
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
| button, released under the pause | warmth 801 of 1 000 (amber) | 801 | cools from the lift, lime 450 ms later |

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

- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyMoves.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyHeat.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`
