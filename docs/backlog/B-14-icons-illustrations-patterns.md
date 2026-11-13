---
id: B-14
title: "Icons, the six illustrations and the four patterns"
status: open
priority: P1
size: S
stage: stage-1-materials
blocked_by: [B-10]
---

# B-14 — Icons, the six illustrations and the four patterns

Sixteen icons on a 24 grid, line 2, square caps, each one SVG path in the page's `ICONS` array; six illustrations built from bodies; four patterns.

- **The decision and its reason.** A script extracts the icon paths into a generated Kotlin file (`ImageVector`s), checked with `--check`; illustrations are compositions of `MutantSurface` bodies with fixed keys; patterns are drawn, not bundled.
- Icons are never damaged (the brief's «порча смысла»).

- AC: the 16 icons match the page within the floor; the generated file is checked; each illustration matches its reference; no icon passes through the generator.
- Anchors: `reference/design-system/mutant-ui Иконки и заготовки.dc.html`
