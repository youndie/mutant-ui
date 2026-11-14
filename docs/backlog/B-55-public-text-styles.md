---
id: B-55
title: "Public text styles: a consumer cannot set its own words in Handjet or Rubik"
status: wip
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
