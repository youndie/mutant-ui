---
id: B-74
title: "Handjet's ink a row higher on the CI's macOS 26 than on macOS 27 and in Chrome"
status: question
priority: P1
size: S
stage: stage-5-product
blocked_by: [B-71]
---

# B-74 — Handjet's ink a row higher on the CI's macOS 26 than on macOS 27 and in Chrome

Found once [B-71](B-71-text-ink-varies-across-ci-macs.md) let the CI run its tests. On `macos-latest` (macOS 26.7.2, `Apple M1 (Virtual)`) three text checks fail every time they run, by one row of Handjet's ink: `TextFloorTest` (`Text_Handjet400_18_Efir`: top 3 against Chrome's 4), `MutantTextStylesTest` (`Demo_OsdLine_Efir`: 4 against 5) and `DialogParityTest` (`MutantDialog_Open_Kopiya`: the title's top 76 against 77, the band's 182 against 181). On this Mac (macOS 27.0.1), where the references and every gate before were run, all three are exact. The kit's arithmetic is the same on both (`cssLine`); what differs is where the system's rasteriser puts the glyphs, as plakat-ui saw its goldens move between the same two macOS versions.

## Question for the owner

1. **Exact on this Mac, a row's allowance on CI** for these text-position checks only, the allowance named by the OS version it is for; the exact checks run here before every merge.
2. **A self-hosted runner on this Mac** for the code gate: exact everywhere, the gate depending on this machine being on.
3. **Exact everywhere and the CI red** until a hosted macOS 27 exists, merging on the local gate.

Rejected: re-rendering the references to match macOS 26, which would move the target instead of the ink.
