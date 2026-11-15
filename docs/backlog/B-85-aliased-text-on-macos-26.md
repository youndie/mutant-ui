---
id: B-85
title: "On macOS 26 the slider's disabled value is drawn without antialiasing"
status: done
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

## Findings (2026-11-15, the trigger)

**The lead was wrong, and the slider keeps the platform's raster.** Given the fixture's `ChromeText`, the slider failed parity on this Mac: `MutantSlider_Disabled_Efir`'s value drew 3 px wide against the page's 10, its inkiest pixel #2c2e31 against #14161b. `ChromePlacedText` already shifts the line by the fraction Chrome would place it at, and `ChromeText`'s subpixel positioning shifts it a second time. The slider keeps the platform's raster, and a comment beside its styles now says why.

**A probe on the runner (PR #29, macOS 26.7.2) named the trigger.** It drew `−4` in Handjet 18 sp in six raster settings, over four grounds:
- the settings: platform default; antialiased with no hinting and subpixel positioning; antialiased or LCD with slight, normal or no hinting;
- the grounds: plain grey, plain amber, the disabled body, the resting body.

It also drew the slider's own value, enabled and disabled. Each against its transparent twin, it counted the glyph's pixels and those within 2 levels of the ink (`logs/b-85/`):

| host | glyph pixels | at full ink |
|---|---|---|
| macOS 27.0.1 (this Mac), every case | 52–60 | 0–3 |
| macOS 26.7.2 (runner), every case | 52–60 | 43–49 |
| the slider's value, disabled, this Mac | 54 | 3 |
| the slider's value, disabled, runner | 54 | 49 |

Neither the ground nor the raster settings move the count, and the enabled handle's value is drawn the same. The crops (`logs/b-85/*.png`) show strokes a pixel heavier with soft edges, not missing antialiasing. **The trigger is the host's glyph rasteriser:** CoreText on macOS 26 draws Handjet heavier, likely the same cause as the row above that B-74 allows.

**Why only that cell.** On the page `−4` over the disabled body never reaches the full ink (#14161b at its inkiest), so the runner's full ink (#05060c) leaves the colour check's 16 levels. The amber handle's `+3` reaches the full ink on the page too, so its heavier strokes pass.

**Outcome.** The second AC branch: the kit's text does nothing a consumer could change, as every app's Handjet on that host is drawn so. The allowance stays and is renamed for what it allows (`ALIASED_DISABLED_VALUE` → `BOLDER_DISABLED_VALUE`), and the research's row names the cause. The probe was scratch and is removed; its output, the four crops and the commit that ran it stay in `logs/b-85/`.
