---
id: B-01
title: "Skeleton: mutant-core on desktop, Android, iOS and wasm, the sample kit, both gates"
status: done
priority: P0
size: M
stage: stage-0-risks
---

# B-01 — Skeleton: mutant-core on desktop, Android, iOS and wasm, the sample kit, both gates

Nothing is built. The line's kits share one shape (research D1, D12) and every later item assumes it.

- **The decision and its reason.** One module `mutant-core` on sborka's conventions (explicit API, warnings as errors, the catalogue's Kotlin, CMP and AGP), targets desktop JVM, Android (`minSdk 24`), `iosArm64`, `iosSimulatorArm64` and `wasmJs`; `foundation` only, no Material artefact; `abiValidation` with the dump in `mutant-core/api/`; a `sample` module that runs as `./gradlew :sample:run`. The generator's package `io.github.youndie.mutant.gen` imports nothing from Compose, held by a test (D1).
- The code gate joins `.github/workflows/check.yaml` as a `gradle` job on `macos-latest`: `./gradlew check`.
- Rejected: the handoff's two modules (D1). Not covered: publishing ([B-41](B-41-public-repository-and-publishing.md)), the sample's content.

- AC: `./gradlew check` is green on the Mac and in CI; `:sample:run` opens a window; the ABI dump is recorded; a Compose import added to `io.github.youndie.mutant.gen` fails the test; `make check` stays green.
- Anchors: `docs/research/research-architecture.md`

## Result (2026-11-14)

- `mutant-core` on sborka 0.5.0.135 with desktop, Android, `iosArm64`, `iosSimulatorArm64` and `wasmJs`; `foundation` only; the ABI dump in `mutant-core/api/`; the `sample` on desktop and wasm. Service document: [mutant-core](../services/mutant-core.md).
- `./gradlew check` green on the Mac: `ComposeSmokeTest` 1, `GeneratorBoundaryTest` 2, `SampleAppTest` 1, ktlint, ABI. `make check` green.
- `GeneratorBoundaryTest` was checked by mutation: an internal function returning `androidx.compose.ui.graphics.Path` in `gen/MutantGen.kt` fails it by name, naming the import line; restored, green.
- CI: a `gradle` job on `macos-latest` running `./gradlew check`. Not yet run there: the repository has no remote until [B-41](B-41-public-repository-and-publishing.md).
- Not run: `:sample:run` was not opened in a window this iteration (the smoke test composes the same root off-screen).
