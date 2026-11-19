---
id: B-141
title: "The Tape draws in a layer of its own"
status: wip
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-141 — The Tape draws in a layer of its own

Found by [B-140](B-140-components-page-in-a-phones-chrome.md): since [B-136](B-136-the-tape-band-on-every-frame.md) the
Tape's draw reads the clock's frame, and `mutantTape()` places that draw before its layer
(`drawWithContent { … }.graphicsLayer()`), in the layer of whatever holds the Tape. A Tape inside content — the
Components page's Tape section, in the scrolling column — records that content again on every frame. On the Pixel 6a in
Chrome the resting Components page went from 6.1 ms of work a frame (B-135's build) to 20.9 (same run).

- **The decision.** `mutantTape()` becomes `graphicsLayer().drawWithContent { … }.graphicsLayer()`: the Tape's own draw
  in a layer of its own, the content in the inner one, so a frame of the Tape records the outer layer only. Measured in
  B-140 as an arm (`logs/b-140/2026-11-19-layer-ab.log`): 7.5 ms against 20.6, under in every counted round.
- **Rejected:** stepping the band and the noise at the tick again (B-136's owner wanted them on every frame); moving the
  sample's Tape section out of the column (it fixes the sample and leaves every consumer who nests a Tape).
- AC:
  - a test on the JVM: a Tape nested in content that has a draw counter; over frames of the band passing, the content
    draws once, and a mutant dropping the outer layer fails it;
  - goldens and parity unchanged, or each moved one named and looked at;
  - B-73's bench on the phone, `ComponentsRest` against `main`, interleaved, with the Control scene: work p50 under by
    more than 2 ms in every counted round; then one 60 s load, its levels read per second (B-140's open mode).
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantTape.kt`

## Findings

### Iteration 1, 2026-11-19

- `mutantTape()` is `graphicsLayer().drawWithContent { … }.graphicsLayer()`. `MutantTapeTest`'s old counter of the
  Tape's draws was a `drawBehind` before `mutantTape()` on the same node, in the holder's layer: it moved on every frame
  only because of this defect, so it is replaced by an internal probe in the Tape's own draw
  (`LocalMutantTapeProbe`, as `LocalMutantTapeSkip` is for B-77's bench).
- New test, `the Tape's frames draw neither the node holding it nor its siblings again`: a Tape under a sibling in a
  column, from 200 ms (the band passing) and from 2200 ms (away), 40 frames each. Before the fix (`edee2f2`, which is the
  mutant dropping the outer layer): failed, the holder drawn 41 times where 2 were expected. With it: green, with the
  other nine of `MutantTapeTest`.

**Pre-registered before the run**, the AC's comparison as it will be read: B-73's bench on the Pixel 6a in Chrome,
`ComponentsRest`, `ComponentsReduce` and `Control` (10 ms in the frame over `ComponentsReduce`), this branch's build
against `main`'s (`63008d5`, built in a worktree), interleaved as `~b`, 6 × 10 s, on mains, the temperature logged.
Held if `ComponentsRest`'s work p50 is under `main`'s by more than 2 ms in every counted round and the control reads
10 ± 3 ms in every counted round (wider than B-107's ± 1, which B-138's warm phone broke at 8.4–15.6; a
run outside it is void); then one 60 s load of `ComponentsRest` on this build alone, read per
second, for the mode B-140 saw once (levels and steps).
