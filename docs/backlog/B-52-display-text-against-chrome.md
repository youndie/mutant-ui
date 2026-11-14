---
id: B-52
title: "Display text against Chrome: Handjet labels 2 px low, and a floor for text"
status: done
priority: P0
size: S
stage: stage-1-materials
blocked_by: [B-14]
---

# B-52 — Display text against Chrome: Handjet labels 2 px low, and a floor for text

The illustrations' labels in Handjet (20 sp, `line-height: 1` on the page) are drawn 2 px lower than Chrome
draws them and read heavier: the test card's «CH 03 · НАСТРОЙКА» spans rows 143–155 against Chrome's
141–153, with about twice the pixels near its colour per row (research §1.13). Neither
`LineHeightStyle(Center, Trim.None)` nor an unspecified line height moves the kit's text by a pixel.

- **The decision and its reason.** Find the cause before the first component with a label is built on it
  (B-15 and on), and measure a floor for text, as the body has one: a label alone, in each face and size the
  components use, against Chrome — the cmp-ui-parity catalogue's text causes (baselines, half-leading,
  rounded widths, font smoothing) checked one by one.
- Rejected: holding components' text by a percentage before its floor is known.
- **P0** because every MVP component after it (B-15 and on) carries a label: as P1 it would sort after them
  all by number, and each would meet the same 2 px.

- AC: the cause of the 2 px named and removed, or recorded with its number; a text floor per face in the
  research; the illustrations' whole pictures held to it instead of printed.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/illustration/MutantIllustration.kt`, `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/type/MutantFonts.kt`, `scripts/design-references.mjs`

## Result (2026-11-14)

- Cause: a line height equal to the font size is ignored by Compose, which lays out Handjet's own 22.35 px line with the baseline at the ascent (18); Blink rounds the metrics, floors the half-leading and puts it at 16. `cssLine(face, lineHeight)` (`type/MutantLine.kt`) lays a style on Blink's line box from `MutantFaceMetrics` (held to the files by `MutantFaceMetricsTest`) and nudges an equal line height by 0.01 %. The illustrations' labels use it.
- Floor: eight probes in Chrome (`Text_*`: Handjet 16, 18, 20, 24 at line-height 1; Rubik 13, 16 at 1.4, 14 at 1.2, 500 at 14) — Handjet 18 and Rubik 14 at 1.2 are sizes where Blink's rounding moves the baseline. The glyph masks differ on 65–87 % of the ink (a half-covered stroke 186 against 102), which no Compose setting reaches; fixtures still rasterise as Chrome does (`LocalMutantTextRaster`, no hinting, fractional x), 3–6 points better.
- Held: text by where its ink lies — the top row exactly, the bottom and sides within a pixel (`TextFloorTest`); the illustrations' labels, kept parts and reels likewise, as what the whole picture adds to its bodies (`IllustrationParityTest`).
- Mutants, each fails a named test: no nudge; the half-leading a pixel up; the content box unrounded (first survived: no probe size was one where it mattered, so Handjet 18 and Rubik 14 at 1.2 were added and the top row made exact); Handjet's ascender 7400; Rubik's descender 0.3; the labels without `cssLine`.
- Not covered: multi-line text with a fractional line height (the catalogue's case; the illustrations' only two-line label has a whole 22 px line); «Копия» (dark-on-light masks may differ the other way); Android and iOS, whose text stacks are their own.
