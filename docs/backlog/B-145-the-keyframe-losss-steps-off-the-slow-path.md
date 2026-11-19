---
id: B-145
title: "The keyframe loss's steps off the slow path"
status: wip
priority: P2
size: M
stage: stage-6-later
blocked_by: []
---

# B-145 — The keyframe loss's steps off the slow path

From [B-144](B-144-the-keyframe-losss-draw-calls-per-step.md): a running keyframe loss holds the Pixel 6a's Chrome at
8–20 frames a second, a second in nine; 405 ms of a loss's 644 is `StripPicture.drawPart`, 206 reading and 189 writing
its blocks. The owner chose route A there: the band's pixels in Kotlin, handed to Skia once a step.

- **The decision.** To be decided from two checks made before route A is built, both in this item:
  - what handing a band to Skia costs in the browser, as route A needs once a step;
  - whether the blocks' draws take Skia's slow path today, which one line would move.
- AC: each check answered with its log; the route that holds measured by B-142's protocol on the phone against `main`,
  with the loss-free arm as the floor; the keyframe loss's parity, goldens and behaviour tests and
  `StripPictureTest` unchanged, or each moved cell named and looked at.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/StripPicture.kt`,
  `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantKeyframeLoss.kt`

## Findings

### Iteration 1, 2026-11-20: the two checks

**What handing a band to Skia costs in the browser.** Read from the build itself (`main`'s Kotlin module rebuilt with
names, disassembled by `wasm-dis`): Skiko's `InteropScope.toInterop(ByteArray)` allocates in Skia's memory and then
loops, one imported call (`skia_memSetByte`) for each byte; the glue has no bulk copy
(`skia_memSet{Byte,Char,Short,Int,Float,Double}` and their `Get`s are all it exports for memory). Route A hands the
band over once a step: 945 × 504 px at the phone's density is 1.9 M bytes, so **1.9 M imported calls a step**, eight
steps a second, against the 200–400 draw calls a step it would replace. Not measured on the phone, as nothing is built;
it is the reason route A is not built first.

**Whether the blocks' draws take Skia's slow path.** From B-144's numbers: reads and writes cost about the same (206
and 189 ms) though a loss makes 1.7–1.8 writes for every read; what they have equal is the pixels that pass through
them. A cost by the pixel inside Skia, not by the call. The candidate, from how Compose draws: `drawImageRect` passes
the paint's `filterQuality` (`Low` by default, linear) and a strict source rectangle; with a source smaller than its
bitmap and a filter, Skia's bitmap device cannot take its sprite blit and draws the rectangle through an image shader.
A block is moved by whole pixels at its own size, so nearest sampling (`FilterQuality.None`) reads the same pixels.

**Pre-registered before the run.** One variable: `FilterQuality.None` on the paints `StripPicture.drawPart` reads and
writes a block with (a worktree commit from `4cca009`, not pushed; build `e73462514fca4f81`). On the JVM, before the
phone: `StripPictureTest` and the keyframe loss's parity, golden, behaviour and snapshot tests green on it, none moved.
B-142's protocol: the Pixel 6a in Chrome, on mains, `ComponentsRest`, 3 rounds of 60 s interleaved (round 1 not
counted): `main` (`fa05977cdbb9be9f`), the arm as `~b`, B-142's loss-free arm (`1719bd18bb513da9`) as `~c`, the floor.
A stall second has 20 frames or fewer.
- **Held** if the arm has at most one stall second in each counted load. `main`'s own stalls are reported as measured:
  B-143's run showed it reaching five in one counted load of two.
- Then a named profile of the arm: `drawPart`'s share of a loss against B-144's 405 ms of 644.
- If not held, route A is weighed again against what the profile shows, with the upload's cost above.
