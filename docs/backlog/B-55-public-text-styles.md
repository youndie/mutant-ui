---
id: B-55
title: "Public text styles: a consumer cannot set its own words in Handjet or Rubik"
status: done
priority: P1
size: S
stage: stage-3-screens
epic: feature-components
blocked_by: [B-37]
---

# B-55 — Public text styles: a consumer cannot set its own words in Handjet or Rubik

Found in B-37. The kit's faces reach its components through `LocalMutantFonts`, which is internal, and no text style is public: a screen built on the kit cannot write a heading in Handjet, an on-screen-display line (the Demo page's «46 МИН · SP · КОПИЯ 4-ГО ПОКОЛЕНИЯ», «СКОРОСТЬ ЗАПИСИ», the error's «НЕТ СИГНАЛА НА КАНАЛЕ 99») or body text in Rubik. The sample's demo screens set those lines in the platform's face, which wraps where Handjet would not.

- AC: the theme exposes the design's type roles (`mutant-tokens.json`'s `mutant.type`: display, OSD with its tracking, text) as public `TextStyle`s with `cssLine` applied, in the faces the kit bundles; the sample's demo screens use them; a test reads a style's face and size, and a parity cell of a Demo page line (the OSD line under the cassette) holds its ink.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/type/MutantFonts.kt`, `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoPage.kt`

## Result (2026-11-14)

`MutantTheme.textStyles` is a `MutantTextStyles` with `display(size = 26.sp)`, `osd(size = 20.sp)` and
`text(size = 15.sp, weight = Normal)`: Handjet, Handjet tracked 1 in the theme's OSD amber, and Rubik, each through
`cssLine` for the size given. The size is the role function's argument rather than a property to copy, because
`cssLine` lays the line for one size: a style copied to another size would keep the first's line. Outside a
`MutantTheme` the faces fall back to the platform's, as the components' do. The public API dump gains the class and
the getter, nothing else.

The sample's demo screens set their screen titles (display 28), «Сторона A» (display 24), the OSD line under the
cassette and «СКОРОСТЬ ЗАПИСИ» (osd 20) and the error line (osd 18, tracked .5, as the page sets it) in them; the
platform's face stood in before.

Held by `MutantTextStylesTest`: each role's face, size, tracking, weight and colour, and its line height as Blink's
(Handjet 20 → 23 px, Handjet 14 → 15, Rubik 15 → 18); and a new reference cell, `Demo_OsdLine_{Efir,Kopiya}` (the
first tracked span of the Demo page's recording screen, 328 × 23), against which the osd style's ink lies within
1 px on every side and on the page's top row, in both themes. The references re-rendered byte-identically but for the
stems already listed as unstable within the floor. Mutants, each killed by the aimed test: the OSD untracked, the OSD
in Rubik (the cell), the text role without its CSS line (the roles' test).
