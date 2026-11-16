---
id: B-97
title: "A risen bottom sheet tears a wedge out of its body"
status: wip
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
