---
id: B-142
title: "A stall every nine seconds on the resting Components page"
status: done
priority: P2
size: S
stage: stage-6-later
blocked_by: []
---

# B-142 — A stall every nine seconds on the resting Components page

Found by [B-141](B-141-the-tape-draws-in-a-layer-of-its-own.md)'s 60 s load on the Pixel 6a in Chrome
(`logs/b-141/2026-11-19-pixel-60s/frames.csv`): with the Tape fixed, the resting Components page holds 6–10 ms of work
a frame, but one second in nine (0, 9, 18, … 54 s) draws 7–12 frames at a median of 94–165 ms. `main` before the fix
shows the same period (`logs/b-140/2026-11-19-rest-60s/frames.csv`: dips at 8, 17, 26, … s).

- **The hypothesis**, not yet tested: the keyframe loss ([B-48](B-48-keyframe-loss.md),
  `MutantKeyframeLoss.kt`) fires every 9 s (`EVERY_MS = 9000.0`), with a step each 125 ms, and its blocks drag a copy of
  the picture on each step (B-136's note). Nothing else in the kit or the sample was found with a nine-second period.
- **The decision.** To be decided from the numbers; first the mechanism, by one variable: the same build with the
  keyframe loss left out, against it, interleaved, 60 s loads read per second.
- AC: the stall's period and depth measured with and without the keyframe loss; the mechanism named only where that
  one variable moved it; a route, if any, measured on the phone with B-73's control.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

### Iteration 1, 2026-11-19

From the code: the only keyframe loss on the Components page is the sample's own section (`KeyframeLosses` in
`ComponentsPage.kt`, one 200 dp stage). A loss starts in `MoshScene.drawUnder` once a 9 s window is 3.5 s old, and
`start()` paints every body of the scene into strip snapshots on the CPU (`StripPicture.paint`, the bodies by paths,
`inSnapshot`) inside that frame, then steps each 125 ms for 900–1400 ms and recovers in 375 ms.

**Pre-registered before the run.** `main`'s build (`3a5f759`; the build of B-141's run, `fa05977cdbb9be9f`) against one
variable: `KeyframeLossBody` with `scene.live` forced false, so no loss ever starts (a worktree commit, not pushed). B-73's
bench on the Pixel 6a in Chrome, on mains, `ComponentsRest`, 3 rounds of 60 s interleaved as `~b` (round 1 not counted),
the temperature logged. A stall second is one with 20 frames or fewer in `frames.csv`.
- The loss is the stall if, in both counted loads, `main` has 5 or more stall seconds 9 s apart (± 1 s) and the arm
  without the loss has none at that period (at most one stall second in a load).
- Otherwise the hypothesis is refuted, and the stall's period is something else's.

[The run](../../logs/b-142/2026-11-19-loss-ab.log), the Pixel 6a in Chrome 157, on mains, 34.2–36.2 °C, the big cores
capped at 1745–2401 MHz, same host and run, `main` (`fa05977cdbb9be9f`) against `~b` without the loss
(`1719bd18bb513da9`), three 60 s loads each, interleaved. Stall seconds (20 frames or fewer) from `frames.csv`:

| load | `main` | without the loss |
|---|---|---|
| round 1 (not counted) | 9 | none |
| round 2 | 8, 17, 26, 35, 44, 53 | none |
| round 3 | 8, 17, 26, 35, 44, 53 | none |

**The keyframe loss is the stall, by the rule written**: six stall seconds 9 s apart in both counted loads of `main`,
none in the arm without it. Inside one (round 2, 16.6–17.75 s) every frame takes 110–220 ms for the loss's whole run,
then the page is back at 5–30 ms: the cost is the loss running, not only its start. Pooled work p50 9.1 against 9.3 ms
and frames a second 48.4 against 53.9: the loss is about a second in nine, the rest of the page the same.

[A named profile of `main`](../../logs/b-142/2026-11-19-profile-main.txt), 12 s, 36.6 °C, caught a loss at 2.75–4.25 s.
Of the 735 ms spent under `KeyframeLossBody`'s draw: **472 ms in `StripPicture.drawPart` → `drawImageRect`**, the
steps' blocks reading the strips; about 170 ms in the bodies' snapshot at the start (`gen.frame`, `drawBody`); 7 ms
drawing the picture each frame (`StripPicture.draw`). The likely cause, not shown by a variable: each `drawPart` reads
its source by drawing a mutable strip, and Compose on Skiko makes an image of a mutable bitmap whole on each draw
(B-113, B-119) — a strip 1079 px wide copied once for every block of every step, eight steps a second.

**The route**, its own item: [B-143](B-143-the-keyframe-loss-reads-its-blocks-without-copying-strips.md). Pinned: the
arm's change is the one line above, in a worktree from `3a5f759`, not pushed.
