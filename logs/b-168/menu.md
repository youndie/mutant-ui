# B-168 — menu: `MutantMenu`, `MutantDropdown` and their rows

Group 18 of [B-168](../../docs/backlog/B-168-look-and-motion-pass.md), on `fix/b-168-menu` from `73dcdd9`, 2026-11-22, on
this Mac (the desktop JVM, `runComposeUiTest` with the clock driven by hand, 2 ms of real time between frames so the
bodies built ahead are ready).

## What was looked at

Three throwaway tests, deleted before committing; the frames stay in `/private/tmp/b168-menu/` (not in the repository):
`before/` as found, `after/` with the fixes. A sheet is a strip of frames 48 ms apart unless its labels say otherwise.

- **The menu's body and words through a run** (`MenuBody`, as the popup composes it, beside a bare twin
  `MenuBody(hidden = true)` with the same keys on the same clock). Each `.txt` has one line per 16 ms frame: the pixels
  changed since the last frame, and per row the ink its glyph pixels are drawn in (`on` black, `tx` the text's colour,
  read per pixel by the ink that best explains it over the twin's ground), the tenth percentile of their contrast against
  that ground and the share under 3:1 (`LN=<ink>:p10=…:<3=…%`).
  - A pick across (0 → 2) in «Эфир», «Копия», reduce motion, «Чистый сигнал», `steppedMotion`, density 2 in both
    palettes, and the browser's way of drawing in both palettes (`LocalMutantFirstFrameWaits` false,
    `LocalMutantPoolBodies` true, `LocalMutantBodyOnGpu` true, `LocalMutantBodyAsImage` false), `pick-0-2-*`;
  - to the next row (`pick-0-1`), back (`pick-3-0`), eight rows (`eight-0-6`), every frame of the spring in the
    browser's way (`tail-*`);
  - a pick during a run (`repick-0-2-then-0-at-304`);
  - indexes outside the rows: −1 → 2, 2 → −1, 2 → 7 (past the end), 7 → 1 (`none-to-2`, `2-to-none`, `2-to-past-end`,
    `past-end-to-1`).
- **The dropdown in its popup**, the window composed from both roots as they stand (`popup-*`):
  - by the mouse: open, a pick on the last row, a press outside, the anchor reopening and closing, a double click on the
    anchor, Escape and Down with no row focused (`popup-mouse.png`, `popup-mouse.log`);
  - by the keys: Tab to the anchor, Enter, Down, Tab, Down, Enter, Up, Escape, Enter (`popup-keys.png`,
    `popup-keys.log`);
  - near the window's right and bottom edges, a long item, 24 items (`edges-0…3.png`, `edges.log`); density 2
    (`popup-d2.png`);
  - a menu opened over a screen while its keyframe loss runs, and a pick during the loss (`loss-open-mid.png`).
- **The sample** (`before/sample/`): the Components page's Navigation group scrolled to the Menu section, «Setting»
  opened and «SHARPNESS» picked by the mouse in «Эфир» and «Копия» (`components-*.png`); the demo's Library phone taken
  to Search, the sort menu opened and «LONGEST» picked (`search-efir.png`).

## Findings

### F-menu-01 — the words change colour on a timer, not with the slime under them: 1.06:1 for half a second — local

- **What.** As the segmented button's (F-segmented-01): `MenuRow` colours its word by `VenomRun.labelOn`, the old row's
  black until 340 ms, the new one's from then, while the lime goes where the venom takes it. In «Эфир» a pale word on
  the passing lime reads 1.06:1 and a black one on the dark body 1.46:1:
  - a pick across (`before/pick-0-2-efir.txt`): «ЦВЕТНОСТЬ», passed over, pale on lime 128–528 ms (up to 41 % of its
    glyph pixels under 3:1, tenth percentile 1.06:1); «ЧЁТКОСТЬ», the new row, pale on the arriving lime 160–352 ms (up
    to 95 %); «ТРЕКИНГ», the row left, black on the dark body at 336–352 ms (1.46:1) and pale on the lime still there at
    368–384 ms;
  - word-frames with more than a fifth of the glyph under 3:1: 39 of a run's 56 frames across, 17 to the next row, 54
    back, 76 over eight rows, 35 under `steppedMotion`, 38 at density 2, 39 in the browser's way, 53 with a pick during
    the run; «Копия» 1 (both inks dark); reduce motion 0;
  - the sample: `before/sample/components-efir.png` 144–432 ms.
- **The design.** The page's `bindPicks` recolours the words in a `setTimeout(…, 340)`. The kit's rule since B-91/B-94
  and `cmp-ui-component` §4: a word's colour follows the fill as drawn; the navigation bar (F-nav-bar-01) and the
  segmented button (F-segmented-01) were fixed the same way in their own files.

### F-menu-02 — the venom runs where no window shows: to an index outside the rows — local

- **What.** `MenuBody` follows `selectedIndex` whatever it is, and `VenomRun` (shared) answers any slot:
  - 2 → 7 (past the end): the lime leaves the window down off the body's bottom edge for 500 ms, tentacles hanging out
    over whatever stands below the menu (`before/2-to-past-end-efir.png` 32–528 ms);
  - 7 → 1: a real index after it runs in from 140 dp below the menu (`before/past-end-to-1-efir.png` 96–576 ms);
  - 2 → −1: the lime runs up out of the body's top edge (`before/2-to-none-efir.png` 48–624 ms), 27 word-frames under
    3:1 on the way.
  - −1 → 2 is right: no run, the window arrives at once.
- **The decided look.** The KDoc: an index outside `items` shows no window; `cmp-ui-component` §9: an index of −1 is
  nothing chosen, the mark arriving from nothing with no run. F-segmented-02 is the same case.

### F-menu-03 — opened, no row has the keys: Escape, Up and Down do nothing until Tab — local

- **What.** The popup takes focus, but none of its rows: the keys land on the popup's root, above the rows' handlers.
  - Opened by the mouse, Escape leaves the menu open (`before/popup-mouse.log`: `Escape (no row focused): roots=2`) and
    Down moves nothing; only Tab reaches the first row.
  - Opened by the keys (Tab to the anchor, Enter), Down does nothing (`before/popup-keys.log`, `popup-keys.png` «down»);
    Tab then lands on the first row, not the chosen one.
- **The decided behaviour.** B-33: «Up and Down move the keyboard's focus from row to row … Enter or Space picks; Escape
  and a press outside dismiss». `MenuBehaviourTest` holds it only after `requestFocus()` on a row, which no user does.

### F-menu-04 — the anchor keeps its focus brackets while a row in the open menu shows its own — local

- **What.** Opened from the keyboard, the anchor stays the focused node of its window, so it draws its brackets while
  the row that takes the keys draws its own: two focus marks on screen, one of them answering nothing
  (`before/popup-keys.png`, «tab2» to «run+384»).
- **The decided look.** B-33: every row and the anchor draw the focus brackets; B-57 and the kit's practice: what has
  the keys shows it. F-text-field-09 is the sample's face of two fields looking focused.

### F-menu-05 — a pick during a run jumps the window to the row picked before — reported (`VenomRun.kt`, = F-segmented-05)

- The menu's face of F-segmented-05, vertical: 0 → 2, then 0 at 304 ms; at 320 ms 17 140 pixels change in one frame
  (300–6 800 a frame through a run) as the window stands whole on «ЧЁТКОСТЬ» and flows back from there
  (`before/repick-0-2-then-0-at-304-efir.txt`, `.png`). The cause and the fix are F-segmented-05's.

### F-menu-06 — near the window's bottom the menu slides up over its own anchor — reported (owner)

- **What.** The popup stands `offset` below its parent, and Compose's popup keeps it inside the window by moving it. A
  dropdown with less than ~200 dp below it gets its menu moved up until it covers the anchor and its drips
  (`before/edges-1.png`: anchor at 690 dp, menu over it); the sample does it on the Components page whenever the Menu
  section is scrolled near the window's bottom (`before/sample/components-efir.png`: the menu stands on the anchor from
  the first frame, so «the anchor closes it» cannot be pressed; a press outside still closes it). At the right edge the
  menu moves left to fit, which reads well (`before/edges-0.png`).
- **Why reported.** The design draws the menu only below its anchor (`top: 50px`) and B-33 decided no other place; a
  flip above the anchor (where the drips would hang onto it) or staying below and cut is the owner's look.

### F-menu-07 — a menu taller than the window loses its first rows: no scroll — reported (owner)

- **What.** 24 items under an anchor 20 dp from the top: the popup moved up to fit its bottom, its top 30 dp above the
  window, the first row and a half out of reach (`before/edges-2.png`). The keys still focus a row out of view. The
  design draws four rows and B-33 decided no limit; a scrolling column (and where its scroll stands on opening) is the
  owner's.

### F-menu-08 — a press on the chosen row hands nothing to `onSelect`, and the KDoc does not say so — local (KDoc), behaviour reported (owner)

- **What.** `MenuRow` passes a press to `onSelect` only for another row (as the engine's `bindPicks` ignores the chosen
  one). A caller that closes its menu on a pick (the demo's sort menu) cannot be closed by picking the order already
  chosen: the press does nothing and the menu stays (`before/sample/search-efir.png` would show it; looked at in code).
  Material's menu item always calls back. Whether to call back is the owner's; the KDoc now says what it does.

### F-menu-09 — the demo's sort menu runs 123 dp past the phone onto the page — reported (sample)

- **What.** The sort menu stands at the text button's start, 267 dp into the window, 216 dp wide, so it ends at 483 dp
  against the phone's 360: the menu hangs off the phone's right edge onto the page (`before/sample/search-efir.png`,
  «open+16»). A real phone is the window, and the popup would move left to fit (F-menu-06's right edge), so only the
  demo's mock phone shows it; the demo could stand the menu at the button's end (`offset`). `DemoSearch.kt`.

### F-menu-10 — the Tape does not reach an open menu — reported (owner, `surface/MutantTape.kt`)

- **What.** The Tape is a layer over the screen (D10); a popup is a window of its own over it, so an open menu stands
  clean while its scanlines and noise run on every side of it (`before/sample/components-efir.png`: the white streaks
  beside the menu stop at its edge). The tooltip, a popup too, does the same. Whether the Tape covers what floats over
  the screen is the owner's look.

### Looked at, nothing wrong

- **Open and close** at once, as B-33/D15 decided: the menu is whole on its first frame (`popup-mouse.png` «open+16»,
  also in the browser's way), and gone in the frame of the press outside, the anchor or Escape. Closed, the popup leaves
  the composition: nothing in it answers a click or a key (`popup-mouse.log`, roots = 1). A pick leaves the dropdown
  open (B-92), the venom in view; the anchor closes it, also by the mouse, and a double click on the anchor opens and
  closes it.
- **Focus return.** After Escape from a row the anchor has focus again and Enter reopens (`popup-keys.log`).
- **The venom** at density 2, in the browser's way and under `steppedMotion`: the same frames as at density 1 (diffs
  peak together; `tail-browser-efir.png` frame by frame through the spring: no stale or flicked-back frame).
- **Reduce motion**: the window and the words move in the change's frame. «Копия»: no word under 3:1 through a run.
- **Long items** end in an ellipsis inside the row (`edges-0.png`, «ПЕРЕЗАПИСЬ ПОВЕРХ …»).
- **The keyframe loss over an open menu**: the menu, in its own root, draws whole over every frame of the loss and a
  pick during it runs its venom; the band cuts only the screen under it (`loss-open-mid.png`), as B-98 decided.
