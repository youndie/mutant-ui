---
id: B-156
title: "A body left behind is kept, and a screen visited again draws it at once"
status: done
priority: P1
size: M
stage: stage-6-later
blocked_by: [B-155]
---

# B-156 — A body left behind is kept, and a screen visited again draws it at once

From [B-151](B-151-a-real-screen-under-a-finger.md): the Demo's tabs are composed anew at each switch (the app keeps
only their saved state), so every body of a tab is built again each time it is shown — its instance, its static field,
its four boil phases, its still layers and its first frame — though nothing that shapes it has changed. The owner named
a global cache among the means.

- **The decision**: what a surface keeps of its body across ticks (its instance and field cache, its frame ahead with
  the latest frame done, its still layers: `BodyKeep`) goes, when the surface leaves the composition, to a pool by the
  body's signature (cassette, key, size, wear, profile, clean zone, still or not), the most recently left last, at most
  48 bodies. A surface composed for the same signature takes it back in its first draw and draws its last frame at
  once. In the browser by default, where a body cannot be built on another thread; elsewhere behind a test seam
  (`LocalMutantPoolBodies`). Rejected: keeping every tab composed, which is the Demo's to decide and is not to change.
- AC:
  - a test: a surface composed again for its signature draws the same instance, not a new one; another size is built
    anew;
  - every other test, parity and golden unchanged;
  - measured by B-151's protocol against `main` on the phone.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyAhead.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/TestSeams.kt`

## Findings

### 2026-11-21

- On the phone, [arms 2](../../logs/b-151/2026-11-21-arms-2.log) (valid), against B-155's arm: tab switches longest p50 67 against 50 ms, p95 183 against 150,
  missed 3.7 % against 4.1 %: no gain above the run's spread. The time a revisit took was elsewhere: the navigation bar's
  body built anew at most switches ([B-162](B-162-a-surface-keeps-its-recent-instances.md)).
