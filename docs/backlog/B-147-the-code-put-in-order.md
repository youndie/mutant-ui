---
id: B-147
title: "The code put in order"
status: wip
priority: P2
size: L
stage: stage-6-later
blocked_by: []
---

# B-147 — The code put in order

The owner, 2026-11-20: «нужно делать cleanup и рефакторинг по кодовой базе». The kit grew through 145 items, the
last forty of them a performance loop (B-72 … B-145) whose arms, probes and switches were left in the main code beside
what they measured. `mutant-core` is 255 files, 50 k lines (about 6 k of them generated golden tables);
`MutantSurface.kt` is 1 329 lines, the sample's `ComponentsPage.kt` 1 159 and `DemoApp.kt` 782.

- **The decision.** Refactoring only: what the kit draws, how it behaves and its public API do not change. A change
  that would move a pixel, a number or a signature is not made here; it is written down as an item of its own.
- **In scope:**
  - dead code: internal declarations nothing uses, parameters no caller passes, branches no path reaches, files left
    by a removed experiment;
  - the switches and probes the performance loop left in main code (`LocalMutantSkipBodyParts`, `LocalMutantTapeSkip`,
    `LocalMutantFrameProbe`, `LocalMutantRasterProbe`, `LocalMutantTapeProbe`, `MoshScene.probe`, …): each one still
    used by a test or a bench is kept and named as such in one place; one used by nothing is removed;
  - oversized files split along the seams they already have (`MutantSurface.kt`, `ComponentsPage.kt`, `DemoApp.kt`
    first), code moved, not rewritten;
  - duplication between the platform source sets;
  - comments and KDoc that describe what the code no longer does.
- **Out of scope:** the generator's port (`io.github.youndie.mutant.gen`) beyond dead code: its streams, order of
  calls and rounding are the contract (research D3); the vendored design, goldens, references, logs; the public API.
- AC:
  - `make check` and `./gradlew check` green on the Mac on every pushed commit; every golden, parity reference and
    numeric golden unchanged, none re-recorded; `mutant-core/api/` unchanged;
  - iOS (`compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64`), Android (`compileAndroidMain`, `:bench-android`)
    and wasm (`:sample:wasmJsBrowserDistribution`) compile;
  - the anchors of open items and of `docs/services/mutant-core.md` still resolve (`make check`'s report);
  - the findings list each change with the lines it removed, and what was left in place and why.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`,
  `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/ComponentsPage.kt`
