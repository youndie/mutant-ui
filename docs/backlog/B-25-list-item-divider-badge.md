---
id: B-25
title: "ListItem, Divider and Badge"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10]
---

# B-25 — ListItem, Divider and Badge

MVP. ListItem: height 64, a 40 dp `icon` tile with `over` 6 (the list's overhang limit is 8). Divider: tracking strokes 2 dp. Badge: a clean OSD plate, no body.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantListItem(headline, modifier, supporting, leadingIcon, trailingText, trailing, onClick, enabled, key, interactionSource)`: a row 64 dp tall (taller only when its text needs it), its parts 12 dp apart — the icon on a 40 dp `icon` tile with the page's `data-over="6"`, the headline (Rubik 15/500) 2 dp over the supporting line (Rubik 13, secondary), and at the end a slot (a badge) or the OSD text (Handjet 20, amber). Given `onClick`, the whole row is the button, fired on release; its press boils the tile. Disabled: the tile loses its signal and the words grey.
- `MutantDivider(modifier, key)`: the 2 dp tracking divider across what it is given, seeded `<cassette>/<stage>/div<key>`, scaled by the density; the top app bar now draws it.
- `MutantBadge(text, modifier, compact)`: a clean OSD plate, no body — Handjet 16 on a 20 dp line, 6 dp in (the list's `НОВОЕ`), or compact Handjet 14 on 16, 5 dp in (the navigation count, which the navigation bar now draws through it); amber, ringed 1.5 dp, black in the page's `#05060c` in both themes. Inside a body its text is kept clean.
- Designed here (D15): a disabled row; the press boiling the tile; keyboard focus drawing the corner brackets around the row; a headline or supporting line too long for the row cut with an ellipsis before the trailing part, which stays whole.
- References (8): the list stage's two rows (time and badge), each from 3 dp above the row to the bottom of the divider under it, whole and bare, both themes.
- Parity: the tile's body 0 px, its icon 0–1; the headline, supporting line and trailing part each where the page's ink lies and in its colour; the dividers 0 px; the bare twins beside the tile against each other (the badge's plate and ring) 0 px. Listed: «Эфир»'s first headline a row high (the ascender of «б» on the light-on-dark threshold, B-24's cause). The tile's measured clean zone is the engine's record.
- Mutants (31 over three rounds), each failing a named test: the row 60; the gap 10 and the lines' 3; the tile 36, its icon 20, its overhang 10; the three sizes and the headline's weight; the supporting and trailing colours; disabled words and tile; the press not reaching the tile; a disabled row clickable; no focus look; the headline clipped; the icon's clean mark; the badge's paddings 5 and 4, its line 18, its ring 1 and in «Копия»'s ink, its word in «Копия»'s ink; the divider 3 tall, unscaled, one seed for every key. Equivalent: the overhang 8 (the tile draws the same as at 6). First survived and now held: the press (a touch moves the clock, so two tiles of one key are compared at one moment), the clean mark (a probe against the engine's `tz`), the tile's disabled look, the badge's word colour (read inside the plate).
- Tests: `ListItemParityTest` (1), `ListItemBehaviourTest` (10).
- Not covered: the snackbar and the progress on the same stage — B-26 and B-27.
