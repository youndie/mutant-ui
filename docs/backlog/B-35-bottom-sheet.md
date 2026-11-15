---
id: B-35
title: "BottomSheet: a blob of r 22 with a divider above"
status: done
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-43]
---

# B-35 — BottomSheet: a blob of r 22 with a divider above

v1. Body `blob` (fill = surf, r 22, `over` 0, `wob` 1.6) and `btn`; a divider above the sheet. State: open. Sliding in and dragging are [B-43](B-43-undesigned-behaviour.md)'s.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`

## Findings (2026-11-15)

**Built.** `MutantBottomSheet(visible, onDismissRequest, …) { content }`, a modal sheet:
- a `blob` of the surface's colour with the page's slot (r 22, no drips, wobble 1.6, no marks, its mosaic kept), 8 dp wider than the screen on each side and running 26 dp past its foot;
- a 44 × 5 grabber 12 dp in, the content 30 dp from the body's sides with 8 dp between parts, and 12 dp under the content (designed here);
- the tear line, the tracking divider, 2 dp under the window's top, drawn over the body;
- behind it a scrim (`rgba(5, 6, 12, .5)`, the page's) that holds the screen's clock, as the dialog's does. The sheet runs on a clock of its own.

**Designed here (D15).**
- It rises from below over 330 ms (ease-out², in the kit's 83 ms transition steps) with the scrim, and sinks in 200 ms; under reduce motion it is there at once.
- A click on the scrim, Escape and the back gesture dismiss.
- Focus moves into it and goes back to `returnFocusTo` on close.
- Its content stays above `windowInsets` (the navigation bars by default) while the body runs on under them.

**Parity** (`SheetParityTest`, references `MutantSheet_*`). The page's screen, reproduced by its fixture: the still card under the scrim and the page's own sheet content (a title, two rows and a `btn` button of 40 dp with drips of 8, drawn as the body it is).
- Without its words: «Эфир» 0 px; «Копия» 40 px, the speckle under the print slip on its 40 dp grid (research §1.15), a measured residual.
- Every word lies where the page's does, colour 0.

Found on the way:
- **The tear line is drawn over the body.** The engine draws a stage's dividers after its bodies, and the sheet's mosaic rises past its top edge and under the line.
- **The page draws the tear line twice.** Its screen stage finds the sheet stage's divider too (`querySelectorAll` reaches into the nested stage) and draws it again with its own seed, under the scrim, so half its blue shows in the sheet line's gaps. That copy belongs to the page's screen, so the fixture draws it there; the sheet does not.
- **The page's title** (Handjet 26, `line-height: 1.1`) has a line under the face's own, a half-leading Blink floors to −1. `cssLine` clamps a leading that negative (its ratio would be 2.1), so the fixture lays it. No kit component sets such a line yet; one that does will need `cssLine` to place a baseline above the line box.
- **The page's rows centre a 23 dp line in 32** (4.5 dp) and Chrome paints it on the pixel above; Compose's centring rounds up.

**Tests.** `SheetBehaviourTest`:
- the scrim and Escape dismiss, a click on the sheet does not;
- it rises (part way at 140 ms) and under reduce motion is there at once;
- the screen's clock stands while it shows and runs once it has sunk;
- focus goes back to the opener;
- the content keeps above a 40 dp inset.

`OverlayAndMouseTest` holds that nothing under it takes the wheel or a key, and that a second click while it sinks reaches nothing.

**Mutants.** Killed:
- the line under the body;
- the dialog's scrim;
- no side bleed;
- a 40 dp grabber;
- a 30 ms rise (by the halfway check);
- a scrim that does not dismiss;
- the screen's clock not held;
- the inset ignored.
