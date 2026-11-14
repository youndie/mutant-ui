---
id: B-28
title: "Dialog: a clean window over a paused background"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-28 — Dialog: a clean window over a paused background

MVP. Bodies `bar` (the window, clean, r 12) and `btn` (actions). The background pauses with a band. Focus moves into the dialog and returns on dismissal.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content; focus returns to the opener on dismissal.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantDialog(visible, title, onDismissRequest, modifier, text, returnFocusTo, actions)` and `MutantDialogAction(label, onClick, modifier, primary, key)`, placed last in a screen-filling box, after plakat-ui's. While it shows, the screen's clock is held — every body behind it stands still — under the page's 62 % black scrim and its pause band (20 dp with a light edge above and a dark one below, shifted 5 dp, 56 dp above the screen's foot); the window, the `bar` body as the engine's clean frame with 12 dp corners, 280 dp at most and 24 from the screen's sides, holds the title (Handjet 22, a heading), the text (Rubik 14 on a 1.45 line) and the actions at its end. The primary action is the engine's `btn` in the acid, at least 104 × 40 with `data-over="8"`, in a 48 dp target; a plain one is the accent word 8 dp in, at least 48 × 48. The window runs on a clock of its own, so its button boils on a held screen.
- Designed here (D15): it opens (`pause`) in 320 ms and closes (`play`) in 200, scrim, band and window together, stepped at 12 fps, at once under reduce motion; closed, it is not composed; its actions stop answering as it starts to close; a click on the scrim, Escape, the back gesture (`navigationevent-compose` 1.1.0, as plakat-ui) or a reader's dismiss call `onDismissRequest`; focus moves into it and stays, and returns to `returnFocusTo` on closing; the window is centred (the page draws it at 52 in a 262 dp frame, which the fixture gives); keyboard focus draws the corner brackets on an action.
- References (12): the dialog demo's frame, whole and with every word hidden, both themes; the window's and the cards' stage cells, whose records give the fixture where the engine stood each body.
- Parity: the frame's bodies — cards, scrim, band, window, button — against the bare twin's 7 px («Эфир») and 11 («Копия»), listed (a highlight pixel and the band's edge over a card's drip; «Копия»'s speckle under its button and the outline over its fill); the title, the text and both actions where the page's ink lies and in its colour. Listed: the actions' words a row low (the engine paints the button at its slot's rounded 113 dp, the DOM's words at 112.6) and «Эфир»'s title top (the mask). The kit's own window measures the engine's 280 × 171, its clean zone within a pixel of the engine's.
- Found and fixed by its tests: the dialog's own clock ran under a frozen fixture (it now freezes with the screen); two lines of 20.3 laid 40 tall, the button a pixel high (the text's box takes the lines' CSS height); the slot origins of a nested stage, which the fixture now reads from the stage cells.
- Mutants (39 over two rounds), each failing a named test: the open 500 and close 400, the scrim at 50 %, no pause, no hold, a dead scrim, a window that lets clicks through, no Escape, no reader's dismiss, no heading, no pane title, no focus in, no focus trap, no focus back, actions answering while closing, the button 96 wide, 36 tall, its drips 24, the paddings 16 and 18 at the foot (first survived: the fixture gives the engine's size; now read by a probe), the gap 8, the actions' top 8 and gap 8, the title 20, the line 1.3, the paragraph's box left to Compose, the corner 8, the band 4 dp lower, unshifted and brighter, the sides 16, the width 300 (first survived; now read on a wide screen), the plain word's 4 dp, the button's word in the text colour, the clean state dropped. Removed as dead: an explicit slip of 0, which the clean body never reads.
- Tests: `DialogParityTest` (1), `DialogBehaviourTest` (8).
