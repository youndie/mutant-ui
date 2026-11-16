---
id: B-96
title: "The demo is thin and plain"
status: done
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

## Findings, 2026-11-16

- **What was built.** Each of the page's three phones runs one demo app (`sample/.../DemoApp.kt`), opened where the
  design's three screens stand: the library, the recording of «Без подписи №3», and settings under the erase dialog.
  - The navigation bar and the drawer move between four tabs, On air, Queue, Search and Settings. The bar moves between
    tabs whole (`movableContentWithReceiverOf`), so its pill slides from the tab left to the tab taken. Each tab draws the
    bar under its own sheets and dialogs. A tab keeps its scroll and fields while another is shown
    (`rememberSaveableStateHolder`).
  - **On air**: the field and the chips filter an archive of seven tapes, with an empty state when nothing matches. A card
    opens its recording. Below the design's first screen are a carousel of tapes recently on air and the rest of the
    shelf. The FAB, with a tooltip, opens a sheet for a new recording with a segmented speed.
  - **Queue**: the copy in progress, a progress bar that advances, and the queue in a pull-to-refresh. A pull brings in a
    tape marked NEW, and the bar's badge counts it.
  - **Search**: a search bar with suggestions and highlighted results, and the archive in a data table with a sort menu
    (the bare `MutantMenu`). Selected rows are erased through a dialog.
  - **Settings**: the design's form, then a tracking slider, a picture dropdown, a segmented sleep timer and a timer
    recording whose sheet holds the date and time pickers.
  - **Recording**: back and rename (a sheet) with tooltips. Listen plays the progress, a share sheet, tabs that switch
    tracks, notes and copies, and a volume slider. The snackbar is the design's.
- **Numbers the design does not state (D15), all the sample's own.**
  - Spacing: 32 dp before a section's OSD heading, 12 dp after it. 112 dp at the end of a tab for the bar.
  - Timing: the queue's copy advances 1 % every 400 ms. Playback advances 0.4 % every 250 ms. A pull refreshes for 2 s.
  - Table columns: 70 dp for length and 60 dp for channel.
- **The tracking slider stays enabled.** Its label disabled reads 4.48:1 on «Копия» against the demo's 4.5 for active
  words (`AccessibilityTest`).
- **Found on the way: [B-97](B-97-a-risen-sheet-tears-a-wedge.md).** The recording's share sheet, risen over the
  screen, shows a wedge-shaped hole through its body. It was reproduced in a bare fixture and on `main` before the
  2026-11-16 round. It is a defect of the sheet, not of the demo, and is left visible here until B-97 is fixed.
- **Held by** `DemoNavigationTest`:
  - the bar, the drawer, a card and its back arrow navigate;
  - every public `Mutant…` function of `mutant-core` is called by the sample. The Components page covered all of them but
    `MutantMenu`; the demo adds it.

- **The owner's look** is in the running window and on Pages after the merge, since the demo is the thing looked at. A
  remark from it is taken as a new item.
