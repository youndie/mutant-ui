---
id: B-97
title: "A risen bottom sheet tears a wedge out of its body"
status: done
priority: P2
size: S
stage: stage-6-later
---

# B-97 — A risen bottom sheet tears a wedge out of its body

Found while building [B-96](B-96-a-richer-demo.md), 2026-11-16. Opened over the demo's recording, the share sheet
showed a wedge-shaped hole through its body, two straight edges crossing the sheet from side to side. The rows under
the sheet showed through the hole, dimmed by the scrim. The hole stood from 200 ms after the press to at least 3 s.

The shape is that of a twisted outline, a body whose foot was built for another height than its top.

A bare fixture reproduces it:
- a 360 × 720 screen under `MutantScreenClock`;
- ten list rows;
- a `MutantSnackbar` shown;
- a `MutantBottomSheet` (key `demo-share`, an OSD line and three clickable rows) set visible 800 ms after the start.

The same sheet shown from the first frame, or risen with no snackbar, is whole. The demo's share sheet tears with no
snackbar at all, so the snackbar is one trigger, not the cause. The fixture tears on `88a355f` and `f6fea25` too, the
commits before the 2026-11-16 round. There it tears in more cases (shown from the first frame as well).

- **The decision and its reason.** To be taken after a frame strip of the body's outline during the rise. The suspects
  are the raster built ahead for the moving body (B-82/B-86's band, here under a translation by `graphicsLayer`) and the
  body's height as measured while the sheet rises.
- AC:
  - the demo's share sheet and the bare fixture draw a whole body on every frame of the rise and after it;
  - a test that reads the pixels inside the body fails with the fix reverted.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantBottomSheet.kt`

## Findings, 2026-11-16

- **The snackbar was a wrong lead.** Variants with no snackbar, with no clickable rows, and with the sheet shown from
  its first frame all tore the same 2 143 px. The fault is the body's height and wobble, not its neighbours: the
  sheet's other contents simply gave it other heights. The same count came out on `eb5f2b0`, the sheet's first commit.
- **Cause.** The contour of the sheet's body (blob 376 × 271, wobble 1.6) reached the field's grid edge at x = −7 and
  383 dp. The march left open chains, and their joins crossed the body (stitches of 195 dp). Under the even-odd fill
  the crossing was a hole.
- **Decision, research D21.**
  - The grid grows by whole cells past the engine's, keeping its cells at their positions, only for a body found at
    the edge, on the frame's thread.
  - A frame built ahead never grows the grid, and a cut one is not taken.
  - The goldens move only where the engine's contour was cut too: three frames of `fab-b`, listed as `B97_CUT_FRAMES`.
- **Held by:**
  - `TallBodyContourTest`: no stitch over 20 dp, four seeds at heights 120 to 440 dp; the worst is now 2.4.
  - `SheetWholeBodyTest`: no pixel of the body's right half darker than its fill, on twelve ticks, shown at once and
    risen.
  - `AheadCutFrameTest`: a cut frame built ahead is not taken, a whole one is.
- **Mutants.** No grid growth fails all three body tests. Taking cut frames fails `AheadCutFrameTest`.

