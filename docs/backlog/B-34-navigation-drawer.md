---
id: B-34
title: "NavigationDrawer: 42 dp items, slip 3, a shadow, the selection by venom"
status: done
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-12, B-43]
---

# B-34 — NavigationDrawer: 42 dp items, slip 3, a shadow, the selection by venom

v1. Body `bar` (fill = surf, r 0, `slip` 3, shadow), items 42 dp, the selection window `pill` r 8. States: open, selection (venom). Sliding in is [B-43](B-43-undesigned-behaviour.md)'s.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`

## Question (2026-11-15): rows shorter than the 48 dp touch target

The page stacks the drawer items at 42 dp, edge to edge. The brief and the handoff ask for a touch target of at least 48 dp by the slot («Зона нажатия … ≥ 48 dp», README: «Зоны нажатия не меньше 48 dp»). A chip meets it with its 32 dp body in a 48 dp box (B-17). Rows stacked edge to edge cannot: each row can only own its own 42 dp band, and a 48 dp target per row would overlap its neighbours, so a press between two rows would belong to either. The same question stands for B-33 (36 dp menu items) and B-34 (42 dp drawer items). Both items wait for it; the loop goes on with the next item.

Choices, for the owner:
1. **Keep the design's rows** (42 dp) as an owner-named exception to the 48 dp rule, pinned by an accessibility test, as plakat-ui's 44 dp menu item.
2. **Rows of 48 dp**, the drawing kept at 42 dp inside with the space between rows grown. This departs from the page's spacing, and the parity of the column moves.
3. **Rows of 42 dp, each target reaching half into its neighbours' bands.** The column is one target and a press goes to the nearest row: every row's target is still 42 dp tall, so this is choice 1 under another name.

**Answered 2026-11-15 by the owner: choice 1.** The design's 42 dp rows stay, as an owner-named exception to the 48 dp rule (research D19). An accessibility test pins it: every row's target is the whole row, 42 dp tall and as wide as the drawer, the rows tile the column with no gap, and nothing else in the kit is under 48 dp without being on D19's list.

## Findings (2026-11-15)

**Built.** `MutantNavigationDrawer(visible, onDismissRequest, items, selectedIndex, onSelect, …)` and `MutantDrawerItem(code, label)`, as the v1 page draws its channel drawer (`v1-drawer-bg` with `v1-drawer-win` nested).
- **The body.** It is a `bar` of the surface's colour, 212 dp wide, its fill slipped 3 dp over a misregistered shadow (`data-slip="3" data-shadow="mis"`). Its corners are square, there is no edge mosaic (`data-edge="0"`, the profile's `mosaic`), and it runs 10 dp off the screen's start and top and past its foot.
- **The words.** 26 dp down, the title (`КАНАЛЫ`, Handjet 20, tracked 1, amber). Then the rows: 180 × 42, 4 dp apart, 22 dp in, each with its code (Handjet 20, tracked 1) and its label (Rubik 15) 12 dp after it.
- **The selection.** It stands in a clean window (`pill`, radius 8, calm .2), its words in black, and moves by venom on the shared `VenomRun`, as the menu's does.

**Designed here (D15).**
- The drawer slides in from the start over 330 ms (ease-out², in the transition steps) with the scrim, and out in 200 ms, as the sheet rises and sinks. Under reduce motion it is there at once.
- The scrim holds the screen's clock, as the sheet's and the dialog's do.
- A click on the scrim, Escape or the system back gesture dismisses. Focus moves in as it opens and goes to `returnFocusTo` as it closes.
- Up and Down move the focus between rows, wrapping. Every row draws the focus brackets.

**The 42 dp rows (research D19).** Each row is its whole target, 180 × 42. The drawer's rows stand 4 dp apart, as the page's do, so D19 now says so: the menu's rows and the calendar's cells tile with no gap, the drawer's do not. `DrawerBehaviourTest`'s D19 test pins it.

**Parity** (`DrawerParityTest`, both themes; one reference whose slots now include the nested drawer stage's).
- The title, codes and labels lie on the page's, within 1 px.
- The cards' words beyond the drawer are read from x 210: at the drawer's edge the first card's title ends a pixel later in the kit than on the page, its last pixel at x 207.
- Bare: 47 px in «Эфир» and 9 px in «Копия». The speckle on its 40 dp grid over the window (research §1.15), recorded and never to be raised.

**Behaviour** (`DrawerBehaviourTest`):
- a pick reaches `onSelect`;
- the scrim and Escape dismiss;
- Down from the last row wraps to the first;
- D19: the rows are 42 × 180, 46 dp from top to top.

**QA.** The kit gained a drawer for the constraint, long-content and font-scale sweeps, and the hover test gained it. It has no disabled state, and the keyboard sweep leaves it out, as the dialog has a test of its own.

**Sample.** A «Navigation drawer» section on the components page.

**Mutants.** All eight killed:
- `DrawerParityTest`:
  - a slip of 1.5;
  - no shadow;
  - the body not run off the edges;
  - the edge mosaic back.
- `DrawerBehaviourTest`:
  - 48 dp rows;
  - Escape ignored;
  - the scrim not answering;
  - Down going nowhere.

The scrim's mutant first did not apply (the line was reformatted) and was run by hand.
