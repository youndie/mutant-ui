---
id: B-171
title: "A transition changed mid-way goes on from where the body stands"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: []
---

# B-171 — A transition changed mid-way goes on from where the body stands

[B-168](B-168-look-and-motion-pass.md)'s pass found one class of defect in shared surface code, in many components: a
body's transition changed again while it runs restarts from a place the body was not showing. The foundations report
(`logs/b-168/foundations.md` on `fix/b-168-foundations`, «shared-code classes» 1–3, F-foundations-01/02) located the
causes; the component reports found the faces.

| Face | Findings | Cause | Seen on `main` (density 2) |
|---|---|---|---|
| A pour changed back | F-checks-switch-04, F-chip-03, F-foundations-01 | `BodyMoves.follow`: a change restarts the pour from one plain colour — inside B-94's «first half» the pour's own old fill (what was poured vanishes), past it the fill it was pouring in (what was not yet poured fills) | a chip reversed at 208 ms loses 11 225 of its 11 816 lime pixels in one frame; a card reversed at 336 ms gains 16 288 of 93 891 (17 %) |
| A slide re-aimed | F-nav-bar-03, F-pickers-05 | `BodyMoves.follow` starts every new request from its own `dx` with the clock reset, and `slid` takes the offset and the stretch from that one progress | a 56 × 30 pill re-aimed at 16 ms leaps 320 px; re-aimed at 192 ms its box changes 44 px in a frame (the stretch drops to 1) |
| A melt reversed | F-data-table-03, F-foundations-02 | `MeltFollower.follow` restarts the clock, and the progress counts from 0 in the new direction | a 24 dp mark melting in, reversed at 16 ms, shows all of its 2 304 pixels at once; melting out, reversed, it drops what is left (2 352 px at most) and its edges leap up to 57 px |

The engine does the same in all three (`mutant-engine.js`: `fx.t0 = this.time` with `pour` the logical old fill,
lines 139–150; `meltStep`, 597–611, restarts `t0` in `toggle`, 741–760). Fixing them is leaving the design on purpose
(D20): no golden or parity cell moves (they draw at rest or at frozen progress, never a reversal), so this item is the
dated record, below.

## The decisions

- **The pour: the new fill pours over the picture the body shows.** A change while a pour runs keeps what that pour
  had poured: its fill above its front frozen at the progress it reached (a *band*, `MutantPourBand`), over what it was
  pouring over (`MutantPourUnder`: a base colour and the bands, the later over the earlier). The new pour comes down
  from the top over that picture, as a pour from rest comes down over a plain fill. B-94's half rule goes: the new fill
  never needs to pick one colour to start from.
  - The front only runs down as its progress grows (its base moves ≥ 70 dp per unit of progress, its wave at most
    14.4), so a band the new front has passed is covered: it is dropped at draw (drawn, two coincident clipped edges
    left a one-frame hairline) and at the next change.
  - At most four bands: a fifth change inside one pour's time merges the oldest band into the base — the one jump left,
    at five changes in 300–620 ms.
  - The label's ground (B-94) reads the same picture: under the front, the latest band whose front has passed the
    label's middle, else the base.
  - Rejected: the front running back up (a rewind), which only answers a change *back* and draws a motion the design
    never draws; a change to a third fill has no picture to rewind to. Rejected: keeping B-94's half rule with a cross-
    fade, which shows a colour the design never mixes.
- **The slide: a re-aimed run starts from where the body is drawn, its stretch included.** `MutantSlideRun` holds a
  run's `dx`, `dy`, start and a carried stretch. Re-aimed, the new run's `dx` is the request's (from the old home, as
  before) plus what was left of the old run, `dx · (1 − p)²`; the stretch the body showed, per axis, eases out by
  `1 − p`, joined with the new run's own `sin πp` stretch by the larger where both stretch the same way (so a re-aim
  never stretches past the design's 1.4) and by their sum where they pull apart (a run across re-aimed down). With
  nothing carried, the transform is the engine's to the bit.
  - The navigation bar's pill and the calendar's drop pass their run with the request (`SlideRequest.run`), and their
    icon and number colours (`PillPlace`, `DropPlace`) read the same run, so what they test against is what the body
    draws. Their own carry of the remainder (B-168's `PillFrom`, `DropFrom`) is gone: redundant once the surface does it.
  - Rejected: a flag on the request saying its `dx` already counts from where the body stands, which would have kept two
    copies of the remainder and left the places blind to the carried stretch.
- **The melt: a reversal retraces the motion it was in, from where it stands.** `MeltFollower` keeps which motion is
  drawn (the melt-in's, revealed from the top and dropping; the melt-out's, sinking and eaten from the top) and runs it
  back at the same pace: a melt-in reversed rises and closes again, a melt-out reversed comes back up whole. A change
  from rest starts the target's own motion, as before.
  - Rejected: the foundations report's proposal, the other motion started from the complement of the progress. The
    two motions draw different pictures at the same progress (one shows its top part lifted, the other its bottom part
    sunk): worked out from the two transforms for a 24 dp mark, at half way its ink would leap about 20 dp down, though its
    amount would be close.

What none of this changes: a transition from rest draws as before, frame for frame; under reduce motion everything lands
at once (D7), the bands and the carried stretch included (a change under reduce motion starts from the plain fill).

## Cost (B-151's phone budget)

- **At rest: nothing.** A resting body has no pour and no slide; the melt reads nothing once done, as before.
- **A pour from rest**: one path, as before. **A changed pour**, while it runs: per remaining band, one front path built
  once at the change (kept for the body's instance) and per frame two clips and two rects; the bands drop out as the new
  front passes them. A pouring body is drawn by paths, also in the browser (B-110): the shader is not involved.
- **A slide**: a few multiplications per frame while it runs. **A melt**: none.
- The body shader (`BodyShader.kt`) reads none of this and is untouched.

## Acceptance

- `BodyReversalTest` (density 2, the test's clock advanced by hand, no wall-clock waits), with the design's steps and
  without, sweeping the second change over every frame of the first transition and reading every frame drawn from it on:
  - a chip's pour (112 × 32, every frame of its 420 ms) and a card's (300 × 88, every other frame of its 620 ms):
    pixels turning lime or not, frame to frame;
  - a pill's slide re-aimed back and re-aimed down (every frame of its 330 ms): its box's centre and size;
  - a 24 dp mark melting in and melting out, reversed (every frame of its 330 ms): its ink and its top and bottom rows.

  The bound, stated in the test: from the reversal on, no frame changes more than the same transition changes in a
  frame of its own, uninterrupted (its step under `steppedMotion`), plus 10 % for the boil's wobble (2 px for an edge);
  the reversal's own frame no more than an uninterrupted frame without steps, or the uninterrupted run's own step on
  that very frame. Every case lands at rest (the pour poured back, the slide home, the mark whole or gone).
- Reduce motion unchanged: `SurfaceTransitionsTest`'s paused cases, the navigation bar's and the calendar's.
- Every golden and parity cell unchanged; every behaviour test unchanged, the navigation bar's and the calendar's
  re-aim tests (B-168) included.

## Findings, 2026-11-23

**Before** (on `main`, `BodyReversalTest` without the fix; frames in `/private/tmp/b171/before/`):

| | the reversal's frame, worst | the bound |
|---|---|---|
| chip pour | 11 225 px | 1 921 |
| card pour | 69 499 px (and 16 288 past half way) | 6 379 |
| slide | moves 320 px, resizes 44 px | 30, 7 |
| melt in / out | 2 304 / 2 352 px, edges 57 px | 288, 6 |

**After** (without steps; with steps every reversal frame is within its own step):

| | the reversal's frame, worst | after it, worst | the transition's own frame |
|---|---|---|---|
| chip pour | 1 483 px | 1 900 | 1 921 |
| card pour | 5 390 px | 6 512 | 6 379 |
| slide | moves 30 px, resizes 8 | — | 30, 7 |
| melt in / out | 240 / 288 px, edges 6 | 288 | 288, 6 |

Contact sheets before and after, on the surface alone and in the chip, the switch, the checkbox and the navigation bar:
`/private/tmp/b171/{before,after}/`. The chip reversed at 192 ms keeps its lime and the dark pours down over it; the card
reversed at 256 ms shows the lime band eaten from the top; the pill turned at 160 ms keeps its stretch and comes back;
the mark reversed at 160 ms rises and closes; the navigation bar's pill re-aimed at 176 ms turns back wide and eases.

**The departure** (D20): pour, slide and melt changed mid-way leave the engine's restart. No generator golden, vendored
golden, parity cell or library golden moves: none of them draws a reversal.

**Mutants**, each reverting one line, all killed by `BodyReversalTest` — listed in the commit that records them.

- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyMoves.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyTransitions.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyDrawing.kt`,
  `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/surface/BodyReversalTest.kt`
