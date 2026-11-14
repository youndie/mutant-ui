---
id: B-52
title: "Display text against Chrome: Handjet labels 2 px low, and a floor for text"
status: wip
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
