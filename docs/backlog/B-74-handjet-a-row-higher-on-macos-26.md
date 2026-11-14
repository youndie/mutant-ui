---
id: B-74
title: "Handjet's ink a row higher on the CI's macOS 26 than on macOS 27 and in Chrome"
status: done
priority: P1
size: S
stage: stage-5-product
blocked_by: [B-71]
---

# B-74 — Handjet's ink a row higher on the CI's macOS 26 than on macOS 27 and in Chrome

Found once [B-71](B-71-text-ink-varies-across-ci-macs.md) let the CI run its tests. On `macos-latest` (macOS 26.7.2, `Apple M1 (Virtual)`) three text checks fail every time they run, by one row of Handjet's ink: `TextFloorTest` (`Text_Handjet400_18_Efir`: top 3 against Chrome's 4), `MutantTextStylesTest` (`Demo_OsdLine_Efir`: 4 against 5) and `DialogParityTest` (`MutantDialog_Open_Kopiya`: the title's top 76 against 77, the band's 182 against 181). On this Mac (macOS 27.0.1), where the references and every gate before were run, all three are exact. The kit's arithmetic is the same on both (`cssLine`); what differs is where the system's rasteriser puts the glyphs, as plakat-ui saw its goldens move between the same two macOS versions.

## Decision

The owner chose, on 2026-11-14: **exact on this Mac, a row's allowance on CI.** Of the three options put (a row on CI named by the OS version; a self-hosted runner on this Mac; exact everywhere with the CI red), the allowance keeps a hosted gate that can fail without depending on this machine being on.

- `HANDJET_ROWS_ABOVE` (`DesignParity.kt`) is 1 on macOS 26 and 0 on every other host, macOS 27 and later included. `Extent.near(above = …)` lets the top of the ink lie that many rows **higher** than Chrome's, never lower; a residual a check already had stays two-sided.
- Applied by the three checks that failed and only to their Handjet text: `TextFloorTest`'s Handjet labels, the Demo page's OSD line in `MutantTextStylesTest`, the dialog's title in `DialogParityTest` (its text and actions are Rubik and stay exact on CI).
- Rejected: re-rendering the references to match macOS 26, which would move the target instead of the ink.
- AC: on this Mac (macOS 27.0.1) the allowance is 0 and the three checks pass exactly; on CI they pass with it; `HandjetRowsAboveTest` holds the version rule and that the allowance goes up one row, not down and not two.
- Anchors: `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/DesignParity.kt`, `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/HandjetRowsAboveTest.kt`
