---
id: B-36
title: "PullToRefresh: the tongue, refreshing by boil, done by a clean pill"
status: done
priority: P2
size: M
stage: stage-4-v1
epic: feature-components
blocked_by: [B-10, B-11, B-43]
---

# B-36 — PullToRefresh: the tongue, refreshing by boil, done by a clean pill

v1. Bodies `btn` (the tongue: r 5–7, `over` 14–24, `cf` 0–1) and `pill` (done, clean). States: pulling, refreshing (boil), done (clean). The design shows three frames; the gesture is [B-43](B-43-undesigned-behaviour.md)'s and must work with a mouse drag.

- AC: every state the page draws, in both themes, within the parity floor of [B-05](B-05-pixel-references-and-floor.md); the body's frames at the engine's timestamps equal the generator goldens; reduce motion is the pause (D7); the touch target is at least 48 dp by the slot; a mutant in its geometry or motion is caught; a constraint sweep (no width, long text, font scale 2) neither throws nor clips the content.
- Anchors: `reference/design-system/mutant-ui Компоненты v1.dc.html`

## Findings (2026-11-15)

**Built.** `MutantPullToRefresh(refreshing, onRefresh, …, divider = true) { content }`. The component draws the header's tear line (the tracking divider) at its top and a `btn` tongue hanging 2 dp up into it.
- **The tongue's shapes.** It grows with the pull: the page's short tongue (64 × 10, r 5, drips of 14) at a 40 dp gap, its long one (124 × 34, r 7, drips of 24 that let drops go) at 74 dp, each number lerped between, in whole dp.
- **Refreshing.** The long tongue boils (a press on its body, as the page's `data-heat` forces one) with its word in it.
- **Done.** A clean `pill` with its word stands in the middle of a 52 dp gap for a second, then the content goes back up.

**The page, as data.** The line is drawn over the tongue, as the engine draws a stage's dividers after its bodies. With the line drawn by the consumer, the tongue lay over it and the line's dashes differed on the whole tongue's width, so the component draws the line itself.

**Designed here (D15).** The design draws three frames.
- **The pull.** A finger through nested scroll, past the content's top, takes half the distance dragged, up to 120 dp. A mouse, which scrolls nothing in Compose, does the same from a press made while the content stood at its top; the press's own click is cancelled. Let go past 74 dp, it refreshes; short, the content goes back up in the kit's transition steps (200 ms, stepped).
- **The tongue's lower end.** It shows from a 16 dp gap and is never under 4 dp tall.
- **The pill's second.** It is the wall clock's, not the screen's, which stands under reduce motion; under reduce motion the content goes back at once.
- **Readers.** A custom action refreshes.
- **Font scale 2.** The long tongue keeps its 7 dp of room around its word and grows with it.

**Parity** (`PullToRefreshParityTest`, references `MutantPullToRefresh_*`). Each frame is the page's screen: header, line, tongue or pill, cocoons or the list.
- Without words: «Эфир» 0, 4 and 0 px; «Копия» 6, 19 and 0 px, the speckle on its 40 dp grid over the tongue (research §1.15), kept as measured residuals.
- Every word lies where the page's does. The «Эфир» list's first line sits a top row up, light on dark on the ink threshold (B-24's cause, as the dialog's title).

Found on the way:
- **The pill's key.** The page calls the pill `ptr-done`; a kit key of `ptr/done` missed the engine's slot, and the body was built at the kit's 104 dp instead of 103.
- **The pill's width.** It is the label's unrounded width plus its padding, rounded once, as Chrome's is.
- **The QA sweep found three defects, now fixed:**
  - the tear line measured to an unbounded width (a horizontal scroll) threw;
  - an unbounded height threw the same way;
  - the long tongue cut its word at font scale 2.

**Tests.** `PullToRefreshBehaviourTest`:
- a finger past 74 dp refreshes and the list moved down;
- a mouse pulls and its press clicks nothing;
- let go short, the list goes back without a refresh;
- refreshing, done for a second, back;
- under reduce motion the pill stays its second and the list goes back at once;
- the reader's action refreshes.

The component is also in the QA subjects.

**Mutants.** Killed:
- the threshold;
- the resistance;
- the pill's second;
- the line under the tongue;
- the tongue not hanging;
- a narrower short tongue;
- the long tongue's drips holding their drops;
- the done gap;
- the mouse pull not consuming. Killed only once the test's target was tall enough that the pull stays inside it; a drag that leaves a target cancels its click by itself.
