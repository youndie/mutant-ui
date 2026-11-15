---
id: B-31
title: "SegmentedButton: the selection moves by venom"
status: done
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-12]
---

# B-31 — SegmentedButton: the selection moves by venom

v1. Body `btn` (fill = surf2, `over` 14) and the selection window `pill` (`calm` 0.2, r 9). States: rest, selection (venom), disabled.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`

## Findings (2026-11-15)

**Built.** `MutantSegmentedButton(options, selectedIndex, onSelect, …)`: the page's `btn` body in the second surface (drips of 14, a share of 0.4, no marks), and the window, a lime `pill` (r 9, calm 0.2) over the chosen segment with its label in black. The window moves by venom, the transition B-12 ported into the generator (`venomShapes`) and the surface (`drawVenom`) with no component drawing it until now. Its parts:
- the ink under the window and the fill over it, each through the goo;
- the new window hidden until the generator says the mass has filled its place (`full`, p ≈ 0.81), then springing (`venomSpring`);
- 720 ms at 24 frames a second;
- the labels changing colour 340 ms in, as the page's `setTimeout`.

`drawVenom` is split into `drawVenomInk` and `drawVenomFill` so that the window, a body of its own, stands between them, and both scale to the density.

**The page, as data.** The disabled button sets nothing but its fill: it keeps the `btn`'s drips of 24, its marks and its mosaic, and has no padding, so its two labels are centred in the whole 48 dp row.

**Designed here (D15).**
- A segment is 104 dp where the button is given no width (the page's 312 over three).
- The whole button is one focus stop and the arrows choose; the design's page only clicks.
- The press point, from the segment's middle, is the venom's `px, py`, as the page's click is; a key gives none.
- Under reduce motion, or a clock that stands, the window moves at once.
- At font scale 2 the body grows with the label's line, keeping its 31 dp of room around it.

**Parity** (`SegmentedParityTest`, references `MutantSegmented_*`). Without its labels:
- the disabled cells are 0 px off;
- the resting cells are 17 px («Эфир») and 31 px («Копия») off, single pixels on the speckle's 40 dp grid. Chrome draws the speckle as a pattern rasterised once and resampled; the kit draws each square. It is the button's cause (research §1.15), here over the window in «Эфир» and the body in «Копия». Kept as measured residuals.

Every label lies where the page's does (sides within a pixel, the top row exactly), colour 0. Found on the way:
- Blink floors the half-leading of the 40 dp line;
- a grid centres the disabled labels at 15.5 px and Chrome paints them on the next pixel, so the kit rounds that one up.

**A defect found and fixed in the run.** The window and the venom layers read the clock's frame only during a run, so a still button asks for no frames. Their lambdas captured nothing that changes when a run starts, so Compose kept them, and the new window showed at full opacity from the first frame. They now capture the selected index.

**Tests.** `SegmentedBehaviourTest`:
- a click chooses, and a reader sees it;
- the window flows (the old place still lime and the new one not at 48 ms; the reverse at 800 ms);
- the labels switch between 240 and 400 ms;
- 24 frames a second;
- reduce motion moves the window at once;
- the arrows choose, and focus shows;
- disabled chooses nothing;
- no width gives 104 dp a segment, and nothing is cut at font scale 2.

The button is also in the QA subjects and the keyboard sweep.

**Mutants.** Killed:
- the window shown before it is full;
- the labels switching at once;
- 12 frames a second;
- each label's vertical rounding;
- an arrow stepping two;
- the window's radius.

Survived:
- *reduce motion moving at once*, as an equivalent: the kit's reduce motion pauses the clock, which already moves it at once;
- *the label at its fraction*. Centred at a whole pixel, every label still lies within the check's pixel and its colour within 16. The fractional place stays, as Chrome's model, but nothing here holds it.
