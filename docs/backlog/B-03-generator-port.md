---
id: B-03
title: "Generator: a 1:1 common-code port of the resting frame, held to Node goldens on every target"
status: done
priority: P0
size: L
stage: stage-0-risks
blocked_by: [B-01]
---

# B-03 — Generator: a 1:1 common-code port of the resting frame, held to Node goldens on every target

The shape of every component is the output of `mutant-generator.js`. The vendored golden reproduces exactly except three hot flow-line entries, and it is too small to hold a port: six instances, nine frames (research §1.3, §1.4).

- **The decision and its reason.** Port `hash`, `rng`, `h21`, `vn`, `ss`, `smin`, `ucap`, `ty`, `bodyD`, `rrD`, `taper`, `mkHook`, `mkArc`, `markAt`, `marks`, `markPath`, `dyn`, `edgeMos`, `deadPx`, `genItem`, `prims`, `bubbles`, `field`, `march` and `pathBuf` into `io.github.youndie.mutant.gen`, structure and stream order kept, names spelled out (D3). The field is `FloatArray`; rounding where a string is reproduced is `floor(v * 10 + 0.5) / 10`; the field's static-half cache and the march buffer belong to the instance.
- A Node script loads the vendored generator untouched and writes the repository's own goldens: every profile of `static K`, several keys and sizes each (both grid steps, `calm` 0.3 and 1.7, `slip`, `dead`, `edge` off), rest frames at several ticks, hot frames at heat 0.5 and 1. The gate runs it with `--check`. The hot flow lines follow the engine (D4).
- Rejected: porting from the handoff's description, or holding the port only to the vendored golden. Not covered: venom, pour, error blocks ([B-04](B-04-transition-geometry.md)).

- AC: goldens regenerate byte-identically and `make check` fails when they are stale; the port matches them on JVM, Native and wasm, exactly to four decimals for hash, rng, noise and instances and within ±0.1 dp per path coordinate; the vendored golden passes except the three entries `scripts/design-golden.mjs` lists; a mutant in each of FNV-1a, mulberry32, `h21`, `smin` and the march is caught.
- Anchors: `reference/design-system/mutant-generator.js`, `reference/design-system/mutant-golden.json`, `reference/design-system/mutant-engine.js`, `scripts/design-golden.mjs`

## Result (2026-11-14)

- The port: `JsMath.kt` (JS `Math.round`, V8 `Math.hypot`), `Random.kt` (FNV-1a, mulberry32, smoothstep, smooth minimum, lattice hash, value noise), `Shapes.kt` (capsule, top wave, body and rectangle distances), `Instances.kt` (`instance` = `genItem` with `edgeMos` and `marks`; `deadPixels`, `dripState`, `primitives`, `bubbles`, `flowLinePath`), `Field.kt` (`distanceField` in `FloatArray` with the per-instance cache, `march`, `smoothPath`), `Frame.kt` (`frame` as `drawItem` composes them). Everything is `internal`; the ABI dump is unchanged.
- `scripts/gen-goldens.mjs` runs the vendored generator untouched and writes 28 Kotlin files (1.5 MB): hashes of seven strings including Cyrillic and a surrogate pair, twelve draws of four streams, noise at eight points, and 27 instances — every one of the 13 profiles at two sizes, wears and origins (both grid steps, `calm` 0.3, 1 and 1.7, a disabled `still` button, a dead-pixel card) — each with three frames (rest, rest, heat 1 or 0.5). `make check` runs it with `--check`; a second run is byte-identical.
- `GeneratorGoldenTest` (6 tests) passes on the JVM, the iOS simulator and wasm in the browser; on all three, **243 of 243 paths are byte-identical** to the JS, so the 0.1 dp tolerance is never used. `GeneratorExactnessTest` (desktop) holds byte-identity on the JVM.
- Mutants, each against the committed port and restored with `git checkout`: FNV-1a's prime (5 tests fail), mulberry32's `61` (4), the lattice hash's `43758.5453` (2), smooth minimum's `.25` (1), the march's interpolation (1), all by name. Replacing JS rounding by `kotlin.math.round` fails the exactness test (162 of 243 paths). Two survive both tests and are recorded rather than chased: the JVM's `Math.hypot` in place of V8's, and a 1e-6 shift of the field — below the 0.1 dp the paths are written in, they change no output today.
- The hot frames' flow lines follow the engine (boil'), so the vendored golden's three known entries stay as `scripts/design-golden.mjs` lists them; a second-route test reproduces the vendored button's wave and its 25 primitives at t = 5000.
- Not covered here: venom, pour and error blocks ([B-04](B-04-transition-geometry.md)); the cost of all this per frame ([B-06](B-06-frame-cost.md)).
