---
id: B-212
title: "The place key compiles in common metadata"
status: done
priority: P0
size: S
stage: stage-6-later
blocked_by: []
---

# B-212 — The place key compiles in common metadata

`main` has not published since [B-206](B-206-default-keys-by-place.md) (#154, `dc97cf8`): `publish-wip.yaml` stops
at `:mutant-core:compileCommonMainKotlinMetadata` on `MutantPlaceKey.kt:23` — `currentCompositeKeyHashCode.toString(36)`.
In common code the runtime's `CompositeKeyHashCode` is an `expect` type with no `toString(radix)`; every platform's own
compilation sees it as a number and has one, so the desktop, Android, iOS and wasm builds and every test passed, and
`./gradlew check` never compiles the common metadata. The pull request was green; the publish of `main` was red twice
(`45524665468`, `45599725267`).

- **The decision and its reason.** The key's text is made per platform (`expect`/`actual`), each actual the same
  `toString(36)` the platform compiled before, so no body's key, and no golden, moves. Hashing it to an `Int` in common
  code would compile too, and change every unkeyed body's seed on the JVM.
- AC:
  - `:mutant-core:compileCommonMainKotlinMetadata` compiles; it failed on `main` before the change
  - `./gradlew check` runs it, so a pull request that breaks it is red before it merges
  - the unkeyed bodies' keys are the same text as before on desktop: goldens and parity cells unchanged
  - `main`'s publish is green after the merge
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantPlaceKey.kt`, `mutant-core/build.gradle.kts`

## Result, 2026-11-25

- `placeKeyText` is an `expect` function in `MutantPlaceKey.kt`, with one `actual` per platform (`androidMain`,
  `desktopMain`, `iosMain`, `wasmJsMain`), each the `hash.toString(36)` that platform compiled before: the keys' text is
  what it was, so no body moved. `mutant-core`'s `check` depends on `compileCommonMainKotlinMetadata`.
- **Before**: `./gradlew :mutant-core:compileCommonMainKotlinMetadata` on `main` (`fab501e`) failed at
  `MutantPlaceKey.kt:23:94`, "None of the following candidates is applicable" for `toString(36)`, as both publish runs
  did. **After**: it compiles, as do `compileKotlinIosArm64` and `compileKotlinWasmJs`.
- **Mutant**: with the `dependsOn` line removed, `./gradlew :mutant-core:check --dry-run` lists no
  `compileCommonMainKotlinMetadata`; restored, it lists it.
- Goldens and parity cells: none moved; `./gradlew check` (desktop, wasm, iOS simulator suites, goldens and parity) is
  green on this Mac, and runs the common metadata compilation now.
- The last AC, `main`'s publish green, is read on the merge commit's publish run, named in the pull request.
