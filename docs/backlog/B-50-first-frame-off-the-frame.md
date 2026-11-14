---
id: B-50
title: "Build a body's instance and first frame off the frame"
status: done
priority: P1
size: M
stage: stage-1-materials
blocked_by: [B-10]
---

# B-50 — Build a body's instance and first frame off the frame

`MutantSurface` builds the instance and the field's static layers in its draw cache, on the main thread:
B-06 measured that first frame at about ten times a warm one (5.8 ms for a 328 × 80 card on the M1), so
ten bodies appearing together cost about 58 ms there and more on a phone (research §1.9).

- **The decision and its reason.** Build the instance and the first frame on a background dispatcher when
  the slot's size and zone are known, and draw the body from the first frame that has them; until then
  draw nothing, or the plain rounded rectangle of the slot (decided here, with the owner's review round).
  Warm frames stay on the main thread: 0.5 ms for a card.
- Tests and references must stay deterministic: a frozen clock builds synchronously.
- Rejected: building every visible body's static layers inside one frame (today's behaviour).

- AC: ten cards appearing in one frame take no frame longer than a warm frame's budget plus the layout;
  the first drawn frame is pixel-identical to today's; a frozen clock draws in the first frame as before.
- Anchors: `mutant-core/src/commonMain/kotlin/io/github/youndie/mutant/surface/MutantSurface.kt`, `logs/b-06/2026-11-14-desktop-2.summary.txt`

## Result (2026-11-14)

`MutantSurface` builds a body's **first** instance, with its static layers warmed by one frame of the field, on
`Dispatchers.Default` from the draw that first knows the slot, and draws nothing of the body in that frame (the
content draws). The next frame takes the result, or builds it then if the thread is late, so the body always draws
from that frame and the same picture either way. Decided here, to be seen in the owner's review round: nothing rather
than the slot's plain rectangle for that one frame. A frozen clock (fixtures, references, goldens) and an IDE preview
still build in their first frame; later rebuilds (a new size, a focus's wear) stay in their frame, so a resizing
window never shows a body missing. `LocalMutantBuildOffFrame` turns it off for a comparison.

Measured on this M1 (JDK 25, 8 cpus) by `AppearBench`, ten 328 × 80 cards appearing in one frame, arms interleaved,
rounds 2–6 counted (`logs/b-50/2026-11-14-desktop.log`, commit 7271ad6; same host, same run):

| Arm | Frame N | its draw phase | Frame N+1 (16 ms later) | A warm frame |
|---|---|---|---|---|
| built in the frame (before) | 85.8 ms | 68.0 ms | 15.5 ms | 13.1 ms |
| built off the frame | 12.8 ms (worst 16.0) | 1.5 ms | 1.5 ms (worst 5.9) | 13.6 ms (12.9–17.8) |

The frames are the test runtime's, its render included, so a warm frame reads 13 ms where B-06 put a card's body at
0.5 ms; what the item asks is the comparison: off the frame, neither frame N nor N+1 stands above a warm frame. The
draw phase of later frames is not timed: a body redraws its own node, which a parent's `drawContent` does not see.
Not measured: a phone ([B-49](B-49-frame-cost-on-a-pixel-6a.md)); the browser, where `Dispatchers.Default` is the
page's one thread and the build runs as a task between frames, not beside them.

Held by `FirstFrameOffTheFrameTest`: under a running clock nothing is built in the first frame and the body is
there in the next, built once; frames 1–11 are pixel-identical to a body built in its frame; a frozen clock draws in
its first frame; a new size is drawn in its own frame. Mutants, each killed by the aimed test: off-frame never taken,
the frozen clock ignored, every rebuild sent off the frame, the arrival not invalidating the draw. The warm-up frame
removed changes only the cost (an equivalent mutant for the tests, held by the bench). Four existing tests that read
an instance's probe after one frame advance one more (dialog, list item, navigation bar, snackbar).
