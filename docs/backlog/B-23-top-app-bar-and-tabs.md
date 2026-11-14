---
id: B-23
title: "TopAppBar and Tabs: the 56 dp bar with a tracking divider, the 4 dp indicator with an echo"
status: done
priority: P1
size: M
stage: stage-2-components
epic: feature-components
blocked_by: [B-10, B-07]
---

# B-23 — TopAppBar and Tabs: the 56 dp bar with a tracking divider, the 4 dp indicator with an echo

MVP. TopAppBar: no body, height 56, a tracking divider below. Tabs: no body, an indicator 4 dp tall with an echo.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты.dc.html`

## Result (2026-11-14)

- `MutantTopAppBar(title, modifier, navigationIcon, status, windowInsets, key, actions)`: no body; a 56 dp row — the navigation icon, the title in Handjet 26 taking what is left (one line, an ellipsis), the on-screen-display status in Handjet 20 with 1 sp tracking in the OSD amber, then the actions — 12 dp apart, over a 2 dp tracking divider seeded `<cassette>/<stage>/div<key>`. It stands below `windowInsets` (the status bar by default); no side margins, as the page's column has none.
- `MutantTabs(tabs, selectedIndex, onSelect, modifier)` with `MutantTab(label, enabled)`: the labels in Rubik 14/500, 24 dp apart, the selected in the accent, the others secondary, a disabled one grey; under the selected label a 4 dp acid bar 6 dp below the 36 dp row, ringed 1.5 dp in the page's literal `#05060c` in both themes, with its echo of three strokes (10, 8, 6 dp at 4, 18, 30 past it; 60, 30, 15 %). -1 shows no bar.
- Designed here (D15), each number chosen: the bar is as wide as its label (the page draws a literal 62 under its 65 dp label); a new selection runs it across in 330 ms (the body slide's), stepped at 12 fps, eased out by the cube, at once under reduce motion; on a run to the right the echo trails on the left and crosses back as it lands. Each tab is a target 48 tall reaching half the gap (12 dp) past its label and at least 48 wide, so the row stands 12 dp wider each side and 1 dp taller each way than the page's drawing; keyboard focus draws the field's corner brackets on a tab's target. The bar's title is cut with an ellipsis before the status, which stays whole.
- References (8): the nav stage's bar and tabs, whole and with text hidden, both themes.
- Parity: under the words 0 px in all four cells (the divider; the bar with its ring and echo); every word where the page's ink lies and in its colour. Listed: «Эфир»'s third tab label 2 px right (whole-pixel advances add up along the row) and «Копия»'s title top row 1 px (the dark-on-light mask keeps the top of «Э» under the threshold) — research rows of B-23.
- Mutants (30), each fails a named test: the target's side 10, its least width 40, its height 44; the label at 13 and at weight 400; the selected label secondary, a disabled one secondary; the bar 3 tall, a dp higher, ringed 1 dp, ringed in «Копия»'s ink (first survived: 15 levels from the page's black, under the pixel threshold; now read directly); the echo's gap and opacity; the curve squared; the run 400 ms; no pause; no trailing echo (first survived: the test read the bar's own half-covered edge); the next frame never asked; every tab placed at 0 (first survived: aimed at parity, whose cell selects the first tab); no focus look; the tab labels clipped (first survived; the ellipsis is now read); the bar's row 52, its gap 8, the title 24, the status 18 and untracked and in the text colour, no insets, one divider seed for every key, the title clipped.
- Tests: `BarTabsParityTest` (2), `BarTabsBehaviourTest` (8).
- Found on the way: the text button's divider is drawn unscaled at a density above 1 — [B-53](B-53-text-button-divider-at-density.md).
