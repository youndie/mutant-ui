---
id: B-172
title: "A pick during a venom run goes on from where the slime stands"
status: done
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-172 — A pick during a venom run goes on from where the slime stands

[B-168](B-168-look-and-motion-pass.md)'s pass found one defect three times, each reported against the shared
`VenomRun.kt`: F-segmented-05 ([report](../../logs/b-168/segmented.md)), F-menu-05 ([report](../../logs/b-168/menu.md))
and F-drawer-08. A pick during a run started the new run from the slot picked before, not from where the slime stood,
so the window stood there whole in one frame and flowed on from it:

- the segmented button, 0 → 2 and 0 at 304 ms: 8 792 pixels changed in that frame, against 300–1 600 a frame through a
  run; picks 64 ms apart teleported it to 1, 2 and 1, and the venom never showed until the last;
- the menu, the same pick vertically: 17 140 pixels in one frame (300–6 800 a frame through a run);
- the drawer, the same: about 17 000.

The mouse and the keys did the same. **The design does it too**: the page's `bindPicks` reads the pill's `offsetLeft`,
already at the last pick, and the engine draws the venom from there. This item leaves the design on purpose (D20).

## Seen before (2026-11-23)

Contact sheets and per-frame diffs of a pick 0 → 2, then 0 or 1 at 48–688 ms, in `/private/tmp/b172/before/` (not in
the repository; 16 ms frames, a pixel counted as changed where a channel moves by more than 16). The largest frame of
a run from rest (0 → 2, 0 → 1, 2 → 0, 2 → 1), and the largest frame from a second pick on:

| | a run's largest frame | second pick at 48–304 ms | at 400 | at 496 | at 608–688 |
|---|---|---|---|---|---|
| segmented | 2 955 | 8 056–9 772 | 5 397 | 1 886–2 954 | ≤ 2 954 |
| segmented, `steppedMotion` | 3 467 | 8 535–9 989 | 5 587 | 2 481–3 485 | ≤ 3 300 |
| menu | 3 405 | 15 003–18 596 | 9 526 | 4 037 | ≤ 3 279 |
| menu, `steppedMotion` | 5 240 | 16 716–18 583 | 9 991 | 5 265 | ≤ 5 178 |
| drawer | 3 922 | 15 731–19 717 | 10 117 | 5 696 | ≤ 3 917 |
| drawer, `steppedMotion` | 5 824 | 17 466–19 515 | 10 611 | 6 961 | ≤ 5 986 |

Picks 64 ms apart (0, 1, 2, 1, 0): 8 278, 16 101 and 17 528 pixels in a frame. From about 600 ms on the window already
stands whole on the slot picked first, so the jump is no longer a jump.

## Decision

**A run interrupted by a pick settles at its nearer end, then a new run goes from there to the latest pick.** Nearer
by what is left to play: back to where the run came from (`p`, and a whole run from there unless that is the pick)
against on to where it was going (`1 − p`, and a whole run from there unless that is the pick); a tie goes on. So:

- a pick of where the run came from plays the run back from where it stands;
- a pick of where it goes lets it go on;
- any other pick early in a run plays it back and runs on from the start, late in a run lets it land and runs on from
  there;
- a run not yet under way (`p = 0`, the slime whole on its start) starts over toward the pick;
- picks quicker than the runs keep only the latest: the settling run does not change, the run after it goes to the
  last pick.

Every frame drawn is a frame of a run from rest, at its own moment, played forward or back: the generator's
`venomShapes` is asked for no shape it does not draw, and the frame-to-frame change is a run's. While a run settles on
the way to a later pick the window, shown once the venom fills it, stands where that run lands (a translation of the
laid-out window, and of the words' mask with it). The press that steers a run's mass is the one that started it; a
press for a later pick steers the run toward that pick.

- **Cost.** A pick reaches its slot later than from rest: up to half a run (360 ms) of settling before its own 720 ms
  run. A drawer that a consumer closes on a second pick made while the first run plays holds for the first pick's
  720 + 160 ms (B-102), so it can leave before the second run lands; a drawer closed on its first pick takes no second.
- **Not changed.** A run from rest draws the frames it drew (the same progress at every moment, stepped or not);
  reduce motion and a held clock still move the window at once; a pick
  to or from no window (−1) runs nothing.
- **The departure.** The page's jump is the engine's, so no check holds it: no generator golden, transition golden,
  parity cell or library golden draws a pick during a run, and none moves. Recorded here and in `VenomRun`'s KDoc;
  no `asDesigned` switch, as no cell crosses the parity line.
- **Rejected: starting the new run from what is drawn**, the old run's masses at its `p`, which needs `venomShapes` to
  take a start mid-run (F-segmented-05's suggestion). The generator is ported 1:1 (D3) and held by the vendored golden;
  a start the engine never draws is a new shape in it, designed here with numbers the design does not state, and its
  first frame would still differ from the interrupted one unless the shapes were blended — the same jump, smaller.
- **Rejected: letting every run land before the next starts** (a queue). It is continuous too, but a pick early in a
  run waits for the whole run the user has just turned away from: up to 720 ms before the slime heads for it, against
  360 ms here.

## AC

- At a pick at any moment of a run, no frame-to-frame change is above a run from rest's largest, with and without
  `steppedMotion`, in the segmented button, the menu and the drawer.
- Reduce motion unchanged: the window and the words move in the pick's frame.
- Every golden and parity cell unchanged; `SegmentedWordsOnSlimeTest`, `MenuWordsOnSlimeTest` and
  `DrawerWordsOnSlimeTest` pass unchanged, and the words read on the slime while a run settles.

## After (2026-11-23)

The same sheets and counts in `/private/tmp/b172/after/`. The largest frame from the second pick on, over every moment
from 48 to 688 ms and both targets, against a run from rest's largest and the body's largest tick at rest (the two fall
in one frame now and then, as they do through a run from rest):

| | a run's largest frame | the body's tick | largest frame after a second pick | picks 64 ms apart |
|---|---|---|---|---|
| segmented | 2 955 | 471 | 3 269 | 898 |
| segmented, `steppedMotion` | 3 467 | 471 | 3 300 | 1 058 |
| menu | 3 405 | 643 | 3 876 | 1 757 (the first pick's frame) |
| menu, `steppedMotion` | 5 240 | 643 | 5 412 | 1 887 |
| drawer | 3 922 | 1 068 | 4 673 (a 3 000-pixel venom frame on a tick) | 2 335 |
| drawer, `steppedMotion` | 5 824 | 1 068 | 6 749 | 2 185 |

The frames: `after/segmented-repick-2-then-1-at-304.png` (the run lands on «EP · 6 h», the window springs there, then
a run to «LP · 4 h»), `after/menu-repick-2-then-1-at-96.png` (the run plays back to «On air» and runs to «Queue»),
`after/drawer-repick-2-then-1-at-608.png` (the window shows on «Search» while the run lands, then goes to «Queue»).
The words read on the slime through a pick at 48–640 ms, to the start and to a third slot, in the segmented button and
the menu (`SegmentedWordsOnSlimeTest`'s and `MenuWordsOnSlimeTest`'s reading, run by a throwaway test over ten moments:
no word under 3:1). Rebased on batch D (`8f9e583`), the drawer's masked words read the settling run's window as the
menu's do. Reduce motion, the goldens and the parity tests are unchanged (the gate).

- **Held by** `VenomPickMidRunTest`: each of the three, with and without `steppedMotion`, picked 0 → 2 and again at
  48, 160, 304, 400 and 496 and 608 ms (to 0 and to 1; under `steppedMotion` one of the two in turn), and four picks
  64 ms apart; the clock held and moved a frame at a time; no frame from the second pick until the last run lands
  changes more pixels (a channel moved by more than 16) than a run from rest's largest frame plus the body's largest
  tick at rest, both measured in the same test.
- **Held by** `VenomPickMidRunWordsTest`: each of the three picked 0 → 2 and then 1 at 160 ms (played back, run on),
  1 at 400 ms (landed on 2, the window springing there while 1 is chosen, run on) and 0 at 608 ms (the window shown on
  2 played back); every frame from the second pick until the last run lands, each word read as its words test reads it
  against a bare twin, its tenth percentile at least 3:1. The worst words: segmented 9.75:1, menu and drawer 12.06:1
  (the pale words on the body; no word stands in the wrong ink).

## Anchors

- `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/VenomPickMidRunTest.kt`
- `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/VenomPickMidRunWordsTest.kt`
- `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/VenomRun.kt`
- `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantSegmentedButton.kt`
- `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantMenu.kt`
- `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantNavigationDrawer.kt`
