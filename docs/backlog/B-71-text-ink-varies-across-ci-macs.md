---
id: B-71
title: "The code gate replayed failed tests from the build cache as a pass"
status: done
priority: P0
size: S
stage: stage-5-product
blocked_by: [B-41]
---

# B-71 — The code gate replayed failed tests from the build cache as a pass

Found in [B-41](B-41-public-repository-and-publishing.md). The first public `./gradlew check` failed three text checks, its rerun passed, and so did most runs after it, the red ones taking about twice as long. The first reading — two kinds of runner, one rasterising Handjet a row higher — was refuted on 2026-11-14: red and green runs alike ran on an `Apple M1 (Virtual)`, macOS 26.7.2.

## What it was

Every `gradle` job whose `desktopTest` actually ran was red, six of six, with the same three checks off by the same row. Every green one, eight of eight, read `> Task :mutant-core:desktopTest FROM-CACHE`: no test ran. The first run's green second attempt restored the build cache its failing first attempt had stored.

Reproduced on this Mac with a deliberately failing test: `:mutant-core:allTests` fails; after `:mutant-core:clean` (a fresh checkout, as every CI run is) the same command loads `desktopTest` from the build cache and reports `BUILD SUCCESSFUL`. Under `allTests` and `check`, Kotlin lets a test task with failing tests succeed and fails the aggregate after it; the succeeded task's outputs are cached like any other, and loading them reports nothing.

So the gate never held these three checks on CI after its first run, and it held nothing that a pull request did not change the inputs of.

- **The decision.** No test task is ever taken from the build cache (`outputs.cacheIf { false }` on every `AbstractTestTask`, in each module): a CI run runs every test. It costs the tests' time on every run (about 5 minutes on the hosted Mac). Rejected: `--no-build-cache` on CI, which would rebuild everything else too; and leaving it, which leaves a gate that cannot fail.
- What the three checks then show on CI is [B-74](B-74-handjet-a-row-higher-on-macos-26.md).
- AC: the failing-test experiment fails after `clean` with the change and passed without it; the next CI run executes `desktopTest` (no `FROM-CACHE`).
- Anchors: `mutant-core/build.gradle.kts`, `sample/build.gradle.kts`, `.github/workflows/check.yaml`
