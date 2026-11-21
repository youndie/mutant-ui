---
id: B-166
title: "A chosen chip flicks back to its old fill"
status: done
priority: P0
size: S
stage: stage-6-later
blocked_by: []
---

# B-166 — A chosen chip flicks back to its old fill

The owner, 2026-11-22, on the site: «выбираю чип с типом он становится активным потом резко предыдущий и опять он» —
on the Components page a group chip chosen fills, goes back to the look it had, and fills again.

**Why**: a regression of B-155 and B-156. In the browser a body draws the latest frame built ahead, and in its brush the
look it was built with — the fill among it — is baked. The page around the chips is composed anew when a group is
chosen; the chips leave and come back from the pool (B-156) with their frames built ahead before they were chosen, and
a frame drawn from one of them, checked only for being that very frame, showed the old fill until the next tick's. A
pour or a change of theme in place could show the same once over.

- **The decision**: a brush built ahead carries the look it was built with (`lookOf`: the fills, the ink, the outline,
  the slip, the shadow, the palette, the state, the focus, the density, the wear and the rest an image built ahead was
  already held to, B-82), and is drawn only while the look is still that; else the frame's brush is built in the frame,
  once, as before B-155.
- AC:
  - `FillChangeOnGpuTest`: a body back from the pool with another fill draws it from its first frame back — on `main` it
    drew the old one in all of its first 6 frames;
  - in the phone proxy, a tap on a group chip on the site's first page, every frame read from Chrome's screencast: the
    chip changed state more than once after the tap in 5 of 6 runs on `main`, and once in 6 of 6 with this;
  - every other test, parity and golden unchanged.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`

## Findings
