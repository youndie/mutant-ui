---
id: B-152
title: "The generator's sines are the browser's own"
status: done
priority: P1
size: S
stage: stage-6-later
blocked_by: [B-151]
---

# B-152 — The generator's sines are the browser's own

From [B-151](B-151-a-real-screen-under-a-finger.md)'s profile of a tab switch on the phone: 84 ms of its 543 are `sin`,
most of it Kotlin's own `fdlibm` reducing the lattice hash's large arguments (`fract(sin(127.1·x + 311.7·y) ·
43758.5453)`, `__ieee754_rem_pio2`). Kotlin/wasm ships `fdlibm` in the module; the browser has `Math.sin` natively, and
the design's generator, JavaScript, calls exactly that.

- **The decision**: the generator's `sin` and `cos` go through `jsSin` and `jsCos`: the browser's `Math.sin` and
  `Math.cos` on wasm, Kotlin's elsewhere, as before. Rejected: a table or a cheaper hash, which would change every body.
- AC:
  - the generator, transition and keyframe loss goldens unchanged on every target, wasm included;
  - a test holds `jsSin` and `jsCos` to Kotlin's within one ulp over 40 000 values, small, the hash's large and past
    2^30;
  - measured by B-151's protocol against `main` on the phone.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/JsMath.kt`,
  `mutant-core/src/wasmJsMain/kotlin/io/github/youndie/mutant/gen/JsMath.wasmJs.kt`

## Findings

### 2026-11-21

- V8's `Math.cos` is not Kotlin's `fdlibm` to the bit: 1350 of 40 010 values (sines and cosines) differ, each by one
  ulp (`cos(2.7243243219784565)` the first). The JVM's intrinsic `Math.sin` already differs from `StrictMath` the same
  way on the desktop; the generator's goldens round what reaches the picture to a tenth (D3) and hold on wasm with the
  browser's functions (`GeneratorGoldenTest`, `TransitionGoldenTest`, `KeyframeLossGoldenTest`, `LatticeRowsTest`, all
  green in Chrome).
- On the phone, [arms 1](../../logs/b-151/2026-11-21-arms-1.log) (`main`, then each lever added on the one before, interleaved; 3 counted rounds of 30 s; no
  floor in this run, and the phone throttled to 0.5–1.6 GHz, so a comparison, not a verdict): tab switches longest p50
  150 against `main`'s 216 ms, missed 17.6 % against 23.0 %; scroll no different (both stopped at the loss).
