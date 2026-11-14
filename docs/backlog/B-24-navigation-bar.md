---
id: B-24
title: "NavigationBar: bar body, the active item a 56 × 30 pill"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-11]
---

# B-24 — NavigationBar: bar body, the active item a 56 × 30 pill

MVP. Bodies `bar` (height 62–68) and `pill` for the active item, 56 × 30. The label is always visible.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantNavigationBar(items, selectedIndex, onSelect, modifier, windowInsets, key, pillKey)` with `MutantNavigationItem(label, icon, badge, enabled)`: the `bar` body 68 dp tall across what it is given, with the page's `data-marks="0"` (no flow lines), its items in equal cells, each the whole cell's target; an item is a 22 dp icon in a 56 × 30 box, 3 dp over its label (Rubik 11/500, always shown, secondary; the selected one in the text colour), the icon box and the label each centred on the cell. The selected item stands on the `pill`, its icon the page's literal `#05060c`. A badge is the page's count: Handjet 14 on a 16 dp amber plate, 5 dp in, ringed 1.5 dp in `#05060c`, 6 dp in from the icon box's right and 2 dp above it. The bar stands above `windowInsets` (the navigation bar by default). The clean zone is what the engine's is: the text runs, the icons, the badge's text and the pill.
- Designed here (D15): a new selection slides the pill over from the cell it left with the body's slide (330 ms, its stretch), at once under reduce motion; -1 shows no pill; a disabled item greys and takes no click; a label too long for its cell keeps 4 dp from the cell's sides, then an ellipsis; keyboard focus draws the corner brackets on an item's cell.
- References (4): the nav stage's bar with its pill and badge, whole and bare (text and `currentColor` icons hidden, the badge's plate kept), both themes.
- Parity: the body 0 px («Эфир») and 1 («Копия»); each item's icon box and label where the page's ink lies and in its colour; the icons pixel by pixel; the bare twins against each other inside the zone (1 px; «Копия» 14 — the speckle on its 40 dp grid and the pill's outline over the bar's fill, listed). Listed besides: the settings icon's knobs, 11 and 13 px (arcs as cubics, `IconParityTest`'s cause); in «Эфир» the top row of «ф» and «й» a pixel high (light-on-dark mask on the threshold); the badge's «3», whose thin strokes are covered whole by neither renderer, its strongest pixel 44 levels apart. The bar's and the pill's measured clean zones within a pixel of the engine's record, and a plain bar's (no pill, no badge) at the icons' top.
- Found and fixed by the tests: the badge's line height given as 16 where `cssLine` takes a multiple of the size (a 224 dp plate); the clean zone marked on the item columns (2 dp low, 3 wide of the engine's).
- Mutants (39 over three rounds), each now failing a named test: the bar 64; the icon box 52 wide and 28 tall; the icon 20; the gap 4; the label at 12 and at 400; the selected label secondary; a disabled one secondary; the pill's icon in the label's colour; flow lines on; the badge 4 and 0 in, its padding 4, its ring 1 and in «Копия»'s ink; no slide; the pill at the top; no focus look; the labels clipped; no insets; a disabled item clickable; the label margin 0; each clean-zone mark removed; the icon centred on its column. Ten first survived — the pill's width, the icon's size, the plate and the ring, the clean zones, the flags no test read — and are held by the checks above.
- Tests: `NavigationBarParityTest` (2), `NavigationBarBehaviourTest` (9).
