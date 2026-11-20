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

## Findings

Refactoring only, in six commits before the one that adds these findings. Every moved block was checked to be the same lines before and after:
the old file's lines against the new files' together, import and package lines and `private`/`internal` set aside,
sorted and compared, with no difference. No golden, reference, numeric golden or test expectation changed, and no test
file was edited; `mutant-core/api/` is unchanged (`checkKotlinAbi` green on each of the six commits, with both modules'
tests compiled and ktlint clean on each).

**Dead code removed.**

- `mutant-core/.../MutantCore.kt` (8 lines, `MUTANT_PACKAGE`) and `gen/MutantGen.kt` (8 lines, `GEN_PACKAGE`): the
  placeholders of [B-01](B-01-repository-skeleton.md), read by nothing in any source set of the three modules; their
  packages have long had real code.
- ~~`HeldInstance.builds`~~ (`surface/MutantSurface.kt`, 4 lines): its KDoc said a test reads it; none does
  (`FirstFrameOffTheFrameTest` counts its own `builds`).
- `outlineWidth`'s `trembleSeed` (`surface/BodyDrawing.kt`) and `CarouselBody`'s `viewStage`
  (`components/MutantCarousel.kt`): parameters no caller passed, the tests included. The body takes `trembleSeedOf(g)`
  itself; the carousel reads its view's stage once at its top, where the default was read.
- ~~`SampleStrings.languageName`~~ and ~~`SampleStrings.playAll`~~ (2 lines): the sample shows neither
  (`playAllWide` is the one it uses).

**The performance loop's seams.** Every switch and probe it left is still read by a test or a bench, so none was
removed. The fourteen of the body and the Tape stand in one file, `surface/TestSeams.kt`, grouped as parity overrides,
switches and probes under one note (for tests, benches and parity fixtures only, never for a consumer), each KDoc
naming what reads it. Moved there: `LocalMutantOutlineOverride`, `LocalMutantSlotOrigin`, `LocalMutantTextZone`,
`LocalMutantSlotSize`, `LocalMutantBuildOffFrame`, `LocalMutantFrameProbe`, `LocalMutantBodyAsImage`,
`LocalMutantRasterProbe`, `LocalMutantStillLayersOnce` (from `MutantSurface.kt`, 60 lines), `BodyPart` and
`LocalMutantSkipBodyParts` (from `BodyDrawing.kt`, 10), `TapePart`, `LocalMutantTapeSkip` and `LocalMutantTapeProbe`
(from `MutantTape.kt`, 9). The `probe` parameters of `MutantSurface`, `KeyframeLossBody`, `DialogBody`, `ListItemBody`,
`SnackbarBody` and `NavigationBarBody` (with its `pillProbe`), and `MoshScene.probe`, say the same in their KDoc.

**Files split along the seams they had**, code moved, not rewritten:

| File | Lines before | After | Moved to |
|---|---|---|---|
| `mutant-core/.../surface/MutantSurface.kt` | 1 329 | 746 | `CleanZone.kt`, `BodyAhead.kt` (the held instance, the frames built ahead), `RasterisedBody.kt`, `BodyView.kt` (where a body stands against the window), `BodyHeat.kt`, `BodyMoves.kt`, `AheadDispatcher.kt` (the `expect`, beside its actuals), and the seams to `TestSeams.kt` |
| `sample/.../ComponentsPage.kt` | 1 159 | 357 | `ComponentsMvp.kt`, `ComponentsV1.kt`, `ComponentsLater.kt`, `ComponentsBase.kt`: one per group of `SpecCatalogue` |
| `sample/.../DemoApp.kt` | 782 | 188 | `DemoLibrary.kt`, `DemoQueue.kt`, `DemoSearch.kt`, `DemoRecording.kt`, `DemoSettings.kt` |

In `mutant-core` nothing changed visibility. In the sample the section and screen functions, and the helpers and widths
they share (`Variants`, `States`, `TryIt`, `Cell`, `held`, `Stage`, `FIELD`, `CARD`, `BAR`, `STAGE_W`, `TABLE`,
`ICON_CELL`; `BarAction`, `TabBar`, `TabScroll`), became `internal`: the sample is an application and nothing outside it
sees them. `docs/services/mutant-core.md` anchors the new files.

**Left in place, and why.**

- `MutantSurface`'s `pourAt` and `slideAt`: no caller passes either, but dropping them renames the content lambdas of
  four components in the ABI dump (`ComposableSingletons$MutantDatePickerKt`, `…NavigationDrawerKt`,
  `…SegmentedButtonKt`, `…SwitchKt`: a lambda's key follows the parameters of the function it is passed to). Kept, the
  reason in their KDoc; an item that may move the dump can drop them.
- `MutantTimePicker` out of `MutantDatePicker.kt` (653 lines): moving a public function to another file changes its JVM
  facade class (`MutantDatePickerKt`), which is the ABI. `MutantPullToRefresh.kt` (760): its parts share some twenty
  private constants. Neither split.
- `MutantSurface`'s draw cache, about 500 of its 746 lines: its local functions close over the frame's locals, and taking
  them out would be a rewrite, not a move.
- `gen/Instances.kt` (697 lines): the generator's port, out of scope but for dead code, and it has none.
- `LocalMutantBodyOnGpu` stays in `BodyShader.kt`: not a test seam but the platform's choice (`bodyOnGpuByDefault`, on in
  the browser), which tests also provide.
- The platform copies. `BodyShader.desktop.kt` and `BodyShader.wasmJs.kt` are the same 132 lines but
  `bodyOnGpuByDefault`; `ImageFreeze.desktop.kt`, `.ios.kt` and `.wasmJs.kt` the same 8; the Android and iOS shader stubs
  the same 18. There is no shared Skiko source set (the default hierarchy joins desktop, iOS and wasm only in
  `commonMain`), and adding one changes the build and its metadata compilations, so the copies stay.
- The generated `MutantTokens.kt` carries motion constants nothing reads (`CREEP_MIN_MS`, `KEYFRAME_LOSS_EVERY_MS` and
  others): it is `scripts/tokens.mjs`'s output, written from the design's tokens and checked by `make check`.

**Line counts** (Kotlin under `src/*Main`): `mutant-core` 17 263 lines in 82 files before, 17 312 in 88 after; the
sample 3 473 in 11 before, 3 703 in 20 after. The totals grow by the import blocks of the new files. The largest main
file of `mutant-core` is now `MutantPullToRefresh.kt` (760), of the sample `SpecCatalogue.kt` (327).

**Gates**, on the Mac, on the last code commit (`b01d75d`): `make check` green, every anchor resolving (175 anchors,
166 found, the rest artefacts and patterns; the three members struck above named as removed). `./gradlew check` green:
472 tests, none failed or skipped, by the JUnit XML (`mutant-core` desktop 412, iOS simulator 20, wasm 20; the sample
20), the same count as on `1422c27` before the item; `checkKotlinAbi` green, `mutant-core/api/` untouched. iOS
(`compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64`), Android (`compileAndroidMain`, `:bench-android`'s
`compileReleaseKotlin` and `compileDebugKotlin`) and wasm (`:sample:wasmJsBrowserDistribution`) compile.
