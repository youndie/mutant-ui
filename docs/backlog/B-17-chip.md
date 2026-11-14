---
id: B-17
title: "Chip: unselected with slip, selected by pour"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11, B-12]
---

# B-17 — Chip: unselected with slip, selected by pour

MVP. Body `chip`. Unselected: `slip` 2; selected: fill = accent, reached by pour. States: rest, pressed, selected, disabled.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantChip(text, selected, onClick, …, enabled)`: body `chip`, 32 dp in a 48 dp touch target, as wide as its label and 16 dp padding unless the caller gives a width, the label starting 16 dp in (the page's flex row). Unselected the fill slips 2 dp; selected it pours the acid fill in (B-12) and the label turns to the ink 200 ms after the change — the engine's toggle — or in the same frame under reduce motion. Role checkbox, its selection in the semantics.
- References (16): the Chip stage's four cells, whole and bare, both themes. The page stretches the first two chips to their captions (170, 148 dp), the others are label-sized (103 dp).
- Fixture hooks added for every component (research §1.15): a body is generated at the engine's recorded size and text zone, not the kit's. Chrome keeps a label's width fractional (103.4, 83.29) where Compose rounds it up (104, 84): at 104 the body is another shape, and a flow line starting between 83.29 and 84 is kept by the page and dropped by the kit.
- Parity: bodies 0–3 px, «Копия»'s selected chips 10 (the speckle under the print slip), listed; labels where the page puts them and in its colour.
- Tests: `ChipParityTest` (1, the chip placed by its 48 dp target so its body lands on the slot only if centred), `ChipBehaviourTest` (6: the toggle and its semantics; the label's lag, and none when paused; the 48 dp target; its natural width; disabled; the sweep).
- Mutants, each fails a named test: no slip; no lag; no acid fill; the label centred; the body 36 tall and the chip without its intrinsic width (both first survived — the fixture fixed the chip's box to the slot; it is now placed by its target, and a test holds the width it takes given none); padding 12; the label at 14; no disabled body; the label lagging when paused.
