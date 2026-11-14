---
id: B-71
title: "The code gate: three labels' ink a row off on one macos-latest run, not on its rerun"
status: open
priority: P1
size: S
stage: stage-5-product
blocked_by: [B-41]
---

# B-71 — The code gate: three labels' ink a row off on one macos-latest run, not on its rerun

Found in [B-41](B-41-public-repository-and-publishing.md). The first public run of `./gradlew check` (run 43595928923, attempt 1, macOS 26.7.2, image `macos-26-arm64` 20261102.0473.1) failed three text checks, each by one row of Handjet's ink: `TextFloorTest` (`Text_Handjet400_18_Efir`: top 3 against Chrome's 4), `DialogParityTest` (`MutantDialog_Open_Kopiya`: the title's top 76 against 77, the band's top 182 against 181) and `MutantTextStylesTest` (`Demo_OsdLine_Efir`: top 4 against 5). The rerun on the same image and OS passed, in 5 min 55 s against 12 min 56 s. On this Mac (macOS 27.0.1) all three are exact on every run. A gate that is red on some runners and green on others blocks publishes at random, and tells nothing when it is red.

- **The decision.** Find which machine decides it before deciding anything about the checks: the gate now prints the runner's macOS, CPU and model (`check.yaml`, "the runner"), and runs are compared until a red one and a green one are told apart by them. Then the owner chooses between holding the text checks only on a stated machine and pinning the gate's runner. The rejected alternative is a tolerance of one row on CI: it would hide the very offset the checks were written to catch (B-52).
- AC: two or more runs on each kind of runner the pool hands out, with the printed hardware beside each result; the cause named, or a stated "not determined" with what was tried; the owner's choice recorded and applied.
- Anchors: `.github/workflows/check.yaml`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/type/TextFloorTest.kt`
