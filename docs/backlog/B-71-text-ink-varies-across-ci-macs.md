---
id: B-71
title: "The code gate: three labels' ink a row off on one macos-latest run, not on its rerun"
status: question
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

## Iteration 1 (2026-11-14)

Every `gradle` job so far, with what each printed (the print arrived with #2, after the first three):

| Run / attempt | Event | Result | Time | Mac |
|---|---|---|---|---|
| 43595928923 / 1 | push `main` | 3 text checks red | 847 s | not printed |
| 43595970469 / 1 | push `renovate/configure` | the same 3, the same numbers | 854 s | not printed |
| 43595987084 / 1 | pull request #1 | the same 3, the same numbers | 810 s | not printed |
| 43595928923 / 2 | push `main` | green | 424 s | not printed |
| 43598004050, 43598008731 | push, pull request #2 | green | 375, 386 s | Apple M1 (Virtual), VirtualMac2,1, 7 GB |
| 43608109518 | push `main` | green | 404 s | the same |
| 43609517107, 43609523647 | push, pull request #4 | green | 303, 481 s | the same |

Three red runs, identical to the pixel (`Text_Handjet400_18_Efir` top 3 against 4, `Demo_OsdLine_Efir` 4 against 5, `MutantDialog_Open_Kopiya`'s title 76 against 77), all about twice as slow; six green, all of them that were printed on an M1. So the pool hands out two kinds of Mac and the slower one rasterises Handjet a row higher. Inferred, not shown: no red run has printed its machine yet. Every run from now on prints it, so the next red one names the other kind without spending minutes on purpose.

## Question for the owner

The text checks hold Handjet's top row exactly against Chrome (B-52); on one kind of hosted Mac it lands a row higher, and nobody chooses which kind a run gets. Which way?

1. **Hold the exact text checks on the Mac the references were rendered on** (this one) and run them there before every merge; on CI they would check the same things one row loose. CI stays green on either kind; a row's offset would be caught here, not on CI.
2. **Run the code gate on a self-hosted runner** (this Mac): exact everywhere, at the cost of the gate depending on this machine being on.
3. **Wait for a red run that prints its Mac**, then choose with the machine named.

Rejected, as the decision above says: one row of tolerance everywhere, which hides the offset these checks exist for.
