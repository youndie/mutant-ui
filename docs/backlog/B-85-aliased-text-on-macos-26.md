---
id: B-85
title: "On macOS 26 the slider's disabled value is drawn without antialiasing"
status: open
priority: P2
size: S
stage: stage-5-product
blocked_by: [B-30]
---

# B-85 — Text without antialiasing on macOS 26

Found by [B-30](B-30-slider.md) on 2026-11-15. On the hosted `macos-latest` runner (macOS 26), `SliderParityTest`'s fixture draws the disabled handle's value `−4` without antialiasing. Every pixel of its glyphs is either the ink itself or the body behind it. On macOS 27 the same frame is smoothed and lies on the page's pixels. On the runner, the header's words (Handjet 20, allowed a row higher by B-74) and `+3` on the amber handle stay smooth. The owner allowed it for that value on that host alone (`SliderParityTest.ALIASED_DISABLED_VALUE`) and asked for the cause to be found.

- **The decision and its reason.** Find the trigger before any fix. A scratch fixture pushed through CI draws `−4` and `+3` over the amber handle and over the disabled one, once with the fixtures' `ChromeText` settings and once with the platform's. Two things are open:
  - whether the glyphs or the disabled body turn antialiasing off;
  - whether the kit outside a fixture does it too. If it does, a consumer on macOS 26 sees jagged text over a disabled body: a defect of the kit, fixed here, with the allowance then removed.
- AC:
  - the trigger named, with the runner's images;
  - either the kit fixed and `ALIASED_DISABLED_VALUE` removed, or the cause shown to be the fixture's alone and recorded in the research.
- Anchors: `mutant-core/src/desktopTest/kotlin/io/github/youndie/mutant/components/SliderParityTest.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/BodyStates.kt`

## Findings (2026-11-15, from B-31)

**A lead.** Every component's text style passes the fixture's raster (`platformStyle = LocalMutantTextRaster.current`, `ChromeText` in a parity fixture), except the slider's: its header and its value lay out with the platform's settings even in the fixture. The first thing to try is to give the slider the same, then see whether the runner still drops the antialiasing.
