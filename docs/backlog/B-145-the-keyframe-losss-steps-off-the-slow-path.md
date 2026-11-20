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

**The sampling arm: not held.** [The run](../../logs/b-145/2026-11-20-nearest-abc.log), 28.5 → 34.8 °C, same host and
run. Stall seconds per load, round 1 not counted: `main` 1 and 7, the arm 3 and 7, the floor none. Frames over 50 ms in a
load: `main` 79 and 79, the arm 77 and 84, the floor 0. Nearest sampling does not move it; on the JVM it had moved no
pixel either.
- The stall's depth follows the phone's heat, the count of slow frames does not: in the first, coolest load the worst
  second has 31–43 frames in both builds, in the third 10–17, while a load holds 63–84 frames over 50 ms throughout. From
  here the slow frames are counted beside the stall seconds.

### Iteration 2, 2026-11-20: what the hot functions are

`skiko.wasm` disassembled (`wasm-dis`): the functions holding the loss's time in every profile since B-142
(`wasm-function[3081]`, `[3095]`, `[3108]`–`[3111]`) all have the signature `(i32, i32, f32, f32, f32, f32)`: stages of
Skia's raster pipeline in its scalar form, one pixel a pass in floats (the browser's build has no SIMD). So a block's
pixels go through that pipeline twice, read and written, which is B-144's cost by the pixel. Skia copies a sprite
without the pipeline only where nothing is blended: the blend mode `Src`, or `SrcOver` from a source marked opaque; a
block is drawn by `SrcOver` from a bitmap that may hold transparent pixels, sampled or not, so the sampling arm changed
nothing.

**The change.** `StripPicture.drawPart` moves a block as pixels where that is the same picture (`BlockCopy.kt`):
- the read, into an empty part: `Canvas.readPixels` of the strip, a copy; drawing it there blended nothing;
- the write of a part with no transparent pixel (`Bitmap.computeIsOpaque`, asked once a block) under a plain
  source-over paint: `Canvas.writePixels`, a copy; drawn, it would have replaced what was under it;
- a part with any transparent pixel is drawn as before, and so is everything on Android, whose pipeline is vectorised
  and whose three functions answer "not here".

On the JVM: `StripPictureTest` (now with opaque ground under part of its picture, so both ways are taken and blocks
cross between them) and the keyframe loss's parity, golden, behaviour and snapshot tests green, none moved. Mutants,
each killed by `StripPictureTest` and by `KeyframeLossParityTest` both: every part written as a copy; the copy written
one row down; the read copied from one column right.

**Pre-registered before the run.** B-142's protocol again: `main` (`fa05977cdbb9be9f`), this branch as `~b`, the
loss-free arm (`1719bd18bb513da9`) as `~c`; 3 rounds of 60 s interleaved, round 1 not counted.
- **Held** if, in both counted loads, this branch has at most one stall second (20 frames or fewer) and fewer than half
  of `main`'s frames over 50 ms in the same round.
- Then a named profile of the branch: `drawPart`'s time in a loss against B-144's 405 ms of 644, and what is left.
- If not held: the profile says where the pixels still go through the pipeline (the share of parts with a transparent
  pixel is not known beforehand), and the item stops for the owner with it.

### Parked, 2026-11-20

The pixel copy is committed on this branch (`4f15619`) with its tests; on it, `./gradlew check` is green (472 tests),
iOS and Android compile, and the wasm sample builds (`3bc3817acac49f7c`). **The phone run pre-registered above was not
made**: the owner took the phone back the same day («телефон мне нужен - снимаю с тестов»). Without it the item's
acceptance cannot be read, so the branch is not merged. Resuming: build `3bc3817acac49f7c` again from this branch,
run the protocol as written, read it by the rule written.

### Resumed, 2026-11-20: the run, void

[The run](../../logs/b-145/2026-11-20-copy-abc.log), as pre-registered: `main` (`fa05977cdbb9be9f`), the pixel copy as
`~b` (`3bc3817acac49f7c`), the loss-free floor as `~c` (`1719bd18bb513da9`), three 60 s loads interleaved. The phone
started at 35.8 °C and ended at 38.0, its big cores capped from 2802 to 1277 MHz; the owner's Telegram took about 40 %
of a core at the start.

- **Void.** In the counted loads the floor itself has a stall second nearly every second (rounds 2 and 3), and so do
  the other two: the run cannot tell the loss's stall from a phone that stalls anyway. The floor is this protocol's
  control; it failed, so no verdict is read from the counted loads.
- Round 1, not counted, before the throttling: `main` stalls at 8, 17, 35, 44, 53 s; the pixel copy at 6, 15, 24, 51 s
  (nine apart, as the loss); the floor at 12, 28, 29, 41 s (no period). One load each, not counted: it says nothing yet,
  but it does not show the copy removing the stall.
- Next: the same protocol with the phone under 30 °C, nothing else running on it, and the floor's counted loads free of
  stalls as the condition of a valid run.
