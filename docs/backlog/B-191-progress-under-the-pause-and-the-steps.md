---
id: B-191
title: "The progress band under the pause and under the steps"
status: wip
priority: P1
size: S
stage: stage-6-later
blocked_by: []
---

# B-191 — The progress band under the pause and under the steps

Under the pause the indeterminate band stands at tick 40, which leaves the track empty at 29 of 177 widths (F-progress-05). `steppedMotion` does not step the band and the reel (F-progress-07). [B-168](B-168-look-and-motion-pass.md)'s designer's answers, answers 13 and 14 ([ошибка]).

- **The decision and its reason.** Under the pause the band stands centred on the track, so the track is never empty. Under `steppedMotion` the band and the reel step at the 125 ms tick: the reel 30° a tick, the band 12 dp a tick. By default they keep moving on every frame at the same speed (D22).

- AC:
  - under reduce motion, at every width from 48 to 600 dp, the band is centred on the track
  - under `steppedMotion`, the band's position and the reel's angle change only at 125 ms ticks, by 12 dp and 30°
  - each new test fails on the code before and kills a mutant after the commit; goldens and parity cells at rest unchanged, or the moved ones listed with this item and looked at
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantProgress.kt`
