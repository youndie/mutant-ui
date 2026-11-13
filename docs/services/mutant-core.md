---
id: mutant-core
title: mutant-core
type: service
module: mutant-core
tech_stack: [Kotlin Multiplatform, Compose Multiplatform]
owner: unassigned
depends_on:
  - sborka (build conventions and the `wip` catalog)
publishes: []
---

# mutant-core

## 1. Responsibility

The library module: the generator port, the tokens, the theme, the body renderer and the
components — everything a consumer of mutant-ui depends on (research D1). It owns no data and has no
API beyond its Kotlin surface. It deliberately does **not** hold the sample, the design references,
or any Material artefact; and its generator package does not depend on Compose.

## 2a. Code anchors

| File | What is there |
|---|---|
| `mutant-core/build.gradle.kts` | targets, dependencies, ABI validation, the test inputs, the iOS metadata exception |
| `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/` | the library's root package |
| `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/gen/` | the generator, Compose-free ([B-03](../backlog/B-03-generator-port.md)) |
| `mutant-core/api/` | the committed public API dump |
| `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/GeneratorBoundaryTest.kt` | holds the generator package free of Compose imports |
| `mutant-core/karma.config.d/mocha-timeout.js` | the browser tests' limits |
| `sample/` | the sample kit: `./gradlew :sample:run`, `:sample:wasmJsBrowserDistribution` |

## 3. How it is built

- sborka 0.5.0.135 conventions (`io.github.youndie.sborka.kmp`, `.lint`): explicit API, warnings as
  errors, the `wip` catalogue's Kotlin, Compose Multiplatform and AGP; JVM floor 25.
- Targets: `desktop` (JVM), `android` (`minSdk 24`, `compileSdk 37`, resources enabled), `iosArm64`,
  `iosSimulatorArm64`, `wasmJs` (browser, with an executable binary).
- `./gradlew check` is the code gate: tests on every target, ktlint, the ABI dump. A change to the
  public API is recorded with `./gradlew updateKotlinAbi` in the same commit.
- Built on the Mac (research D12); CI's `gradle` job runs the same `check` on `macos-latest`.
- `local.properties` with `sdk.dir` is needed in every checkout (git-ignored); without it Gradle
  stops at `compileAndroidMain` with "SDK location not found".

## 5. Quirks

- The shared iOS metadata compilation runs without warnings-as-errors: CMP 1.12 puts two lifecycle
  klibs with one `unique_name` on it. The per-target iOS compilations keep `-Werror`.
- A test that reads files (the generator sources, later the vendored design) declares them as test
  inputs in `mutant-core/build.gradle.kts`, or Gradle reports the previous run's verdict.
- The browser tests run under Mocha and Karma limits raised to 30 s and 60 s
  (`mutant-core/karma.config.d/mocha-timeout.js`): Mocha's 2 s turned plakat-ui's `main` red on a
  hosted runner.
