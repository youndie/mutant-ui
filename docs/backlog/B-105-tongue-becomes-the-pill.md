---
id: B-105
title: "Refreshing ends with a cut from the tongue to the pill"
status: done
priority: P2
size: M
stage: stage-6-later
---

# B-105 — Refreshing ends with a cut from the tongue to the pill

The owner, 2026-11-17: «pull to refresh заканчивает анимацию резко (Refreshed) — надо, чтобы морфилось».

- **Seen before.** A frame strip from the end of refreshing, every 32 ms, then 96 ms: on the frame after refreshing
  ends the long tongue, 124 × 34 with its drips, is replaced whole by the clean pill (`REFRESHED`) in the middle of the
  gap. After its second the pill rode up with the list and went under the line in about 100 ms, its whole width
  standing until the line cut it.
- **Decision** (the moves are designed here, D15; every number below is this item's):
  - **The tongue becomes the pill** over the 300 ms the content glides from 74 dp to the pill's 52, eased out with it
    (`MORPH_MS = GLIDE_MS`). One shape for both bodies: from the long tongue's 124 × 34 to the pill's width × 28, its
    corners from the tongue's 7 dp to the pill's 14; its top from where the tongue hangs (2 dp up into the line) to the
    middle of the gap.
  - The pill's own clean body stands in that shape under the tongue; the tongue over it, its drips drawn back up
    (overhang 24 → 0 dp), fades out draw by draw. The tongue's word fades out over the first half and the pill's comes
    in over the second, each at its size: words never scale.
  - **The pill goes back into the line**: after its second it shrinks about its top, staying in the middle of the
    closing gap, and meets the line as the gap closes; its word fades over the first 40 % of the way.
  - The standing pill is the design's: its body, seed and word as the page draws them; the moves only lead into it and
    out of it, so no parity cell moves. Under reduce motion both moves are at once, as every glide is.
  - What hangs from the line is one structure in every phase (the pill's body under, the tongue over, the moves' words
    over both), so neither body is built anew as the phases hand over. Built anew, the pill's first instance came a
    frame late (B-50) and the pill blinked out for a frame at the end of the move.
  - The drips step on the hot tick as they draw back up, as every drip of the kit does (D22); the body's size moves on
    every frame.
- **Rejected.** One body keyed as the tongue through to the pill: it moved the standing pill off the design (58 and 123
  px bare in the two themes, past the parity line), a departure for a move. A cross-fade of two whole bodies: the
  page shows through both at the middle.
- AC:
  - from the end of refreshing the body's size never changes by more than 8 dp from one frame to the next, and it
    narrows from the tongue's width to the pill's (`PullToRefreshMorphTest`);
  - after its second the pill narrows to nothing over several frames, never widening and never above the component's
    top (`PullToRefreshMorphTest`);
  - the page's three frames hold their parity unchanged (`PullToRefreshParityTest`);
  - each test fails with its move taken out.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/components/MutantPullToRefresh.kt`
