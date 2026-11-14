---
id: B-51
title: "A size change moves every flow line: accept, or hold them?"
status: question
priority: P2
size: S
stage: stage-1-materials
blocked_by: [B-10]
---

# B-51 — A size change moves every flow line: accept, or hold them?

A body is generated from its slot's size, so a resize regenerates it (handoff README, risks). Measured
(research §1.10): a field growing from 260 to 268 dp over 200 keys keeps every drip and 80 % of the mosaic,
and moves every flow line, because the lines are spaced `w / n` with `n` from the width.

- **The question for the owner.** Which of these, for components whose size follows their content (a chip
  or a button whose label changes, a field that grows):
  1. **Accept the jump** (recommended): a size change is a discrete event — a new label, a new value — and
     the engine does the same; animated size changes are not passed to the generator (a component animating
     its size draws its body at the target size).
  2. **Hold the flow lines**: generate them for a width rounded up to 32 dp and clip them to the body, so
     small changes keep them; a departure from the generator (cmp-ui-parity §7).
  3. **Bucket the whole body**: generate at a width rounded to 16 dp and stretch; the outline's noise and the
     drips' spacing then scale with the stretch, also a departure.
- Not blocking: components can be built under 1 and switched later.

- AC: the owner's choice recorded in research as a decision; under 2 or 3, the departure listed and a test
  holding the flow lines across a small resize.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `reference/design-system/mutant-generator.js`
