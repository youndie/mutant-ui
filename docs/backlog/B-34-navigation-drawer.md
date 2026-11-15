---
id: B-34
title: "NavigationDrawer: 42 dp items, slip 3, a shadow, the selection by venom"
status: open
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
