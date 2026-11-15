---
id: B-32
title: "Tooltip: a blob plate of 30 or 44 dp"
status: done
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-43]
---

# B-32 — Tooltip: a blob plate of 30 or 44 dp

v1. Body `blob` (fill = blue or OSD, r 6, `over` 20, `cf` 0), height 30 or 44. The design shows it shown, not appearing; when and how it appears is [B-43](B-43-undesigned-behaviour.md)'s.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`

## Findings (2026-11-15)

**Built.** `MutantTooltipBox(text, …, supportingText = null) { anchor }`: over its anchor stands a `blob` plate with the page's slot (r 6, drips of 20, no drops, outline 2.5, no marks, no mosaic):
- one line of OSD type on the blue plate, 30 dp, in the blue's own text colour;
- with a `supportingText`, two lines (Handjet 18 and 16, `line-height: 1`) on the amber plate, 44 dp, in black.

**Designed here (D15).** The design draws the plates shown, not appearing.
- **Where:** the plate is centred over its anchor. On the page the plain plate is within 0.5 dp of that and the two-line plate 2 dp left of it; the anchors there stand at fixed offsets. Its bottom stands 28 dp above the anchor (22 for the two-line plate), the page's distances.
- **When:** at once, with no entrance:
  - while a mouse hovers the anchor;
  - while the anchor holds focus taken from the keyboard;
  - after a finger's long press (the platform's timeout), until 1.5 s after it lifts.

  Focus that came with a press shows nothing. In a test window the input mode is Keyboard, and a touch moves focus to the anchor, so without this a tap would leave the plate up.
- **No press:** the plate stands in a popup of no size and draws past it, so it takes no press: a click on the plate reaches the button under it (plakat-ui's B-72).
- **Font scale 2:** the plate grows with its lines; at font scale 1 it is the page's 30 or 44 dp.

**Parity** (`TooltipParityTest`, references `MutantTooltip_*`). The plates, cut 16 px under them: at 20 px the cut reached the two-line plate's FAB, whose outline rises 6 px over its slot.
- Bodies: 0, 0, 4 and 1 px off.
- Every word where the page puts it, colour within the threshold, with B-74's row allowed on macOS 26 (Handjet).

**Tests.** `TooltipBehaviourTest`:
- hover shows and leaving hides;
- keyboard focus shows, and a click's focus does not;
- a long press shows, stays 1.2 s after lifting and is gone at 1.7 s;
- the plate is centred and 28 dp above;
- a click through the plate reaches the button under it;
- reduce motion: the plate stands.

The plates are also in the QA subjects.

**Mutants.** Killed:
- the gap;
- the plate not centred;
- the linger after a lift;
- a popup with a size (it took the press);
- drops on the plate;
- the radius;
- the second line's `line-height: 1`;
- focus by a press counted as the keyboard's. Killed by the long-press test: a mouse click on desktop does not move focus, a touch does.
