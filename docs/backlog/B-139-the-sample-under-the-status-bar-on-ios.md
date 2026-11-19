---
id: B-139
title: "The sample under the status bar on iOS"
status: open
priority: P3
size: S
stage: stage-6-later
blocked_by: []
---

# B-139 — The sample under the status bar on iOS

Found in [B-138](B-138-a-round-on-every-platform.md), 2026-11-19, the first time the sample ran on iOS (the simulator's
iPhone 17): the Components page's top bar and the Demo page's screens start at the window's top edge, under the status
bar's clock and battery. The sample's pages pass `WindowInsets(0)` to every bar and sheet, which is right on the desktop
and in the browser, where the insets are zero, and the iOS host (`sample/src/iosMain/…/Main.kt`) adds none.

- **The decision.** The host, not the pages: the iOS host pads the sample by the safe drawing insets over the page's
  colour, so the pages stay as the desktop and the browser show them and their goldens do not move.
- Rejected: the pages reading `WindowInsets.safeDrawing` themselves (every page and sheet changes, for a host that is a
  thin wrapper; the kit's own bars already take `windowInsets` and are held by their fixtures).
- AC: a simulator screenshot of the Components and the Demo page with nothing under the status bar or the home
  indicator; the bench mode unchanged (it measures the whole window).
- Anchors: `sample/src/iosMain/kotlin/io/github/youndie/mutant/sample/Main.kt`
