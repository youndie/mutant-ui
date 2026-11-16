---
id: B-96
title: "The demo is thin and plain"
status: wip
priority: P3
size: M
stage: stage-6-later
---

# B-96 — The demo is thin and plain

The owner, 2026-11-16: «в целом демо надо богаче и красивее».

The three demo screens show:
- Library: a top bar, search, chips, two cards, a cocoon, a button, a FAB and a navigation bar that does not navigate;
- Recording: a cassette, a progress, buttons, tabs, two list items and a snackbar;
- Settings: fields, radios, switches, checkboxes and a dialog.

Kit components the demo never shows include the slider, the segmented button, the carousel, the data table, the date picker, the menu, the bottom sheet, the drawer, pull-to-refresh and the tooltip.

- **The decision and its reason.** Designed here (D15), as the owner asked, in the brief's world (a VHS archive, no brands, no real test cards):
  - the navigation bar navigates;
  - each screen has a reason to scroll;
  - the components above are shown where a user of such an app would meet them;
  - motion is visible on entry.

  The proposal is shown to the owner in the running sample before it is closed.
- AC:
  - Every public component of the kit appears in the demo or on the Components page.
  - The demo's screens navigate, from the bar and the drawer.
  - The owner looks at it in the window.
- Anchors: `sample/src/commonMain/kotlin/io/github/youndie/mutant/sample/DemoPage.kt`
